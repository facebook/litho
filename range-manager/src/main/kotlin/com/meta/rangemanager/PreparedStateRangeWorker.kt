/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meta.rangemanager

import android.util.Log
import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer
import com.meta.systrace.trace

/**
 * Reconciles the latest resolved range and on-demand key intents against exact prepared items.
 *
 * Range and key resolution run synchronously under [providerAccessLock]. Lifecycle callbacks run
 * later through [dispatcher], never while [stateLock] is held. One pulse selects at most one item
 * operation: ordered range prepares first, then the latest pending intent for each key, then
 * unrelated range releases.
 *
 * A generation invalidates queued work when the target or lifecycle changes. A separate ledger
 * identity changes only on reset or destroy, allowing work that finishes after schedule or cancel
 * to remain represented while compensating work that finishes after state was discarded. Pending
 * key intents coalesce by key; callback reentrancy is kept deadlock-safe but has no ordering
 * contract beyond these identities.
 */
internal class PreparedStateRangeWorker<Position : Any, Range : Any, K : Any>(
    private val adapter: RangeAdapter<Position, Range, K>,
    private val scope: PrepareScope,
    private val dispatcher: RangeWorkDispatcher,
    private val tracer: Systracer = NoOpSystracer(),
    private val observer: PreparationHealthObserver? = null,
) : RangeWorker<Position, Range> {
  init {
    require(adapter.supportsExactEviction) {
      "Asynchronous range workers require a cache with atomic exact-item eviction"
    }
  }

  private val stateLock = Any()
  private val providerAccessLock = Any()

  private var generation = Generation()
  private var ledger = PreparedLedger<K>()
  private var resolving: ResolveAttempt? = null
  private var currentRange: RangeRequest<K>? = null

  private val pendingByKey = LinkedHashMap<K, KeyIntent<K>>()
  private val claimedReleases = LinkedHashMap<ItemIdentity<K>, ReleaseItem<K>>()
  private val deferredEvictions = LinkedHashMap<ItemIdentity<K>, DeferredEviction<K>>()

  private var activeOperation: Operation<K>? = null
  private var destroyCleanup: DestroyCleanup<K>? = null
  private var postedPulse: Pulse? = null
  private var destroyed = false

  override fun schedule(position: Position, range: Range) {
    val start =
        synchronized(stateLock) {
          if (destroyed) {
            return
          }
          val evictions = invalidateGenerationLocked()
          val attempt = ResolveAttempt(generation)
          resolving = attempt
          ScheduleStart(attempt, evictions)
        }
    evictBestEffort(start.evictions)

    // Evicted after the provider lock is released, so the provider's removal hook never runs
    // under it.
    var evictions: List<DeferredEviction<K>> = emptyList()
    try {
      synchronized(providerAccessLock) {
        val resolvedRange =
            try {
              tracer.trace({ "range-worker:resolve" }) { adapter.resolve(position, range) }
            } catch (failure: Throwable) {
              evictions =
                  synchronized(stateLock) {
                    if (resolving === start.attempt) {
                      resolving = null
                      settleDeferredEvictionsLocked()
                    } else {
                      emptyList()
                    }
                  }
              throw failure
            }

        evictions =
            synchronized(stateLock) {
              if (
                  destroyed ||
                      resolving !== start.attempt ||
                      generation !== start.attempt.generation
              ) {
                return
              }
              resolving = null
              currentRange = RangeRequest(resolvedRange, generation)
              val ready = settleDeferredEvictionsLocked()
              ensurePulseLocked()
              ready
            }
      }
    } finally {
      evictBestEffort(evictions)
    }
  }

  /**
   * Resolves [key] synchronously and accepts the latest intent for that key when its snapshot is
   * still current. Acceptance does not guarantee a distinct callback: pending same-key intents
   * coalesce, and a later schedule or lifecycle transition may invalidate them before execution.
   */
  internal fun prepareKey(
      key: K,
      source: PreparationSource,
      requireCurrentRange: Boolean,
      itemForKey: (K) -> PreparableCollectionItem,
  ): Boolean {
    // Evicted after the provider lock is released (see schedule).
    var evictions: List<DeferredEviction<K>> = emptyList()
    try {
      synchronized(providerAccessLock) {
        val snapshot =
            synchronized(stateLock) {
              if (destroyed || resolving != null) {
                return false
              }
              val requiredRange =
                  if (requireCurrentRange) {
                    currentRange?.takeIf { request ->
                      !request.failed && request.resolvedRange.contains(key)
                    } ?: return false
                  } else {
                    null
                  }
              KeyResolutionSnapshot(generation, requiredRange)
            }

        val item = tracer.trace({ "range-worker:resolve-key" }) { itemForKey(key) }
        evictions =
            synchronized(stateLock) {
              if (destroyed || generation !== snapshot.generation) {
                return false
              }
              val requiredRange = snapshot.requiredRange
              if (
                  requiredRange != null &&
                      (currentRange !== requiredRange ||
                          requiredRange.failed ||
                          !requiredRange.resolvedRange.captures(key, item))
              ) {
                return false
              }

              pendingByKey[key] =
                  KeyIntent(
                      key = key,
                      item = item,
                      source = source,
                      generation = generation,
                      requiredRange = requiredRange,
                  )
              val ready = settleDeferredEvictionsLocked()
              ensurePulseLocked()
              ready
            }
      }
    } finally {
      evictBestEffort(evictions)
    }
    return true
  }

  override fun cancel() {
    val evictions =
        synchronized(stateLock) {
          if (destroyed) {
            return
          }
          invalidateGenerationLocked()
        }
    evictBestEffort(evictions)
  }

  override fun reset() {
    val evictions =
        synchronized(providerAccessLock) {
          synchronized(stateLock) {
            if (destroyed) {
              return
            }
            val ready = invalidateGenerationLocked()
            val previousItems = ledger.byKey.toMap()
            ledger = PreparedLedger()
            previousItems.forEach { (key, item) ->
              if (
                  adapter.isCachedSame(key, item) ||
                      claimedReleases.containsKey(ItemIdentity(key, item))
              ) {
                ledger.byKey[key] = item
              }
            }
            val settled = ready + settleDeferredEvictionsLocked()
            ensurePulseLocked()
            settled
          }
        }
    evictBestEffort(evictions)
  }

  override fun destroy() {
    val cleanup =
        synchronized(providerAccessLock) {
          synchronized(stateLock) {
            if (destroyed) {
              return
            }
            destroyed = true
            val ready = invalidateGenerationLocked() + settleDeferredEvictionsLocked()
            val active = activeOperation
            val activeIdentity = active?.let { operation ->
              ItemIdentity(operation.key, operation.item)
            }
            val candidates = LinkedHashMap<ItemIdentity<K>, ReleaseItem<K>>()
            ledger.byKey.forEach { (key, item) ->
              candidates[ItemIdentity(key, item)] = ReleaseItem(key, item)
            }
            claimedReleases.forEach { (identity, release) -> candidates[identity] = release }
            active?.let { operation ->
              candidates[ItemIdentity(operation.key, operation.item)] =
                  ReleaseItem(operation.key, operation.item)
            }
            val releases =
                candidates
                    .filterNot { (identity, _) ->
                      active is Operation.Release &&
                          active.lifecycleCallbackCompleted &&
                          identity == activeIdentity
                    }
                    .values
                    .filter { release ->
                      try {
                        claimReleaseLocked(release.key, release.item)
                      } catch (failure: Throwable) {
                        reportFailure("Destroy detach failed", failure)
                        false
                      }
                    }
            val evicted = ready.filter { eviction ->
              try {
                adapter.evictIfSame(eviction.key, eviction.item)
              } catch (failure: Throwable) {
                reportFailure("Destroy deferred detach failed", failure)
                false
              }
            }
            ledger = PreparedLedger()
            DestroyCleanup(releases).also { destroyCleanup = it } to evicted
          }
        }
    notifyRemovedBestEffort(cleanup.second.map { it.key to it.item })
    // A dispatcher that can't take the cleanup (its executor refused it) would leave the prepared
    // items and saved state behind for good, so release them here instead.
    if (!dispatcher.post { runDestroyCleanup(cleanup.first) }) {
      runDestroyCleanup(cleanup.first)
    }
  }

  private fun invalidateGenerationLocked(): List<DeferredEviction<K>> {
    generation = Generation()
    resolving = null
    currentRange = null
    pendingByKey.clear()
    postedPulse = null
    dispatcher.removePending()
    return settleDeferredEvictionsLocked()
  }

  private fun ensurePulseLocked() {
    if (
        destroyed ||
            resolving != null ||
            activeOperation != null ||
            postedPulse != null ||
            !hasSelectableWorkLocked()
    ) {
      return
    }
    val pulse = Pulse()
    postedPulse = pulse
    // Refused: forget it, so the next request posts a pulse again instead of waiting for this one.
    if (!dispatcher.post { runPulse(pulse) }) {
      postedPulse = null
    }
  }

  private fun hasSelectableWorkLocked(): Boolean {
    val range = currentRange
    return pendingByKey.isNotEmpty() ||
        (range != null &&
            !range.failed &&
            (range.nextPrepareIndex < range.resolvedRange.items.size ||
                range.releaseItems == null ||
                range.nextReleaseIndex < range.releaseItems.orEmpty().size))
  }

  private fun runPulse(pulse: Pulse) {
    val operation =
        synchronized(stateLock) {
          if (destroyed || postedPulse !== pulse) {
            return
          }
          postedPulse = null
          val selected = selectOperationLocked()
          if (selected != null) {
            activeOperation = selected
          }
          selected
        } ?: return

    val completion =
        try {
          execute(operation)
          val committed = commit(operation)
          if (committed.committed && operation.lifecycleCallbackExecuted) {
            notifyObserver(operation)
          }
          runCleanup(operation, committed.cleanup)?.let { failure -> throw failure }
          committed
        } catch (failure: Throwable) {
          val evictions = fail(operation)
          evictBestEffort(evictions)
          throw failure
        }

    val finalization = finish(operation, completion.committed)
    evictBestEffort(finalization.evictions)
    finalization.failure?.let { failure -> throw failure }
  }

  private fun selectOperationLocked(): Operation<K>? {
    if (destroyed || resolving != null || activeOperation != null) {
      return null
    }

    val range = currentRange
    if (range != null && !range.failed) {
      nextRangePrepareOperationLocked(range)?.let {
        return it
      }
      ensureReleaseItemsLocked(range)
    }

    nextKeyOperationLocked()?.let {
      return it
    }

    if (range != null && !range.failed) {
      return nextRangeReleaseOperationLocked(range)
    }
    return null
  }

  private fun nextRangePrepareOperationLocked(request: RangeRequest<K>): Operation<K>? {
    while (request.nextPrepareIndex < request.resolvedRange.items.size) {
      val resolvedItem = request.resolvedRange.items[request.nextPrepareIndex]
      val preparedItem = ledger.byKey[resolvedItem.key]
      if (preparedItem == null) {
        request.nextPrepareIndex++
        return Operation.Prepare(
            owner = RequestOwner.Range(request),
            ledger = ledger,
            key = resolvedItem.key,
            item = resolvedItem.item,
            source = PreparationSource.RANGE_SCHEDULE,
        )
      }
      if (preparedItem === resolvedItem.item) {
        request.nextPrepareIndex++
        continue
      }
      return Operation.Release(
          owner = RequestOwner.Range(request),
          ledger = ledger,
          key = resolvedItem.key,
          item = preparedItem,
      )
    }
    return null
  }

  private fun nextKeyOperationLocked(): Operation<K>? {
    val iterator = pendingByKey.entries.iterator()
    while (iterator.hasNext()) {
      val intent = iterator.next().value
      if (!isIntentCurrentLocked(intent)) {
        iterator.remove()
        continue
      }

      val preparedItem = ledger.byKey[intent.key]
      if (preparedItem != null && preparedItem !== intent.item) {
        return Operation.Release(
            owner = RequestOwner.Key(intent),
            ledger = ledger,
            key = intent.key,
            item = preparedItem,
        )
      }

      iterator.remove()
      return Operation.Prepare(
          owner = RequestOwner.Key(intent),
          ledger = ledger,
          key = intent.key,
          item = intent.item,
          source = intent.source,
      )
    }
    return null
  }

  private fun isIntentCurrentLocked(intent: KeyIntent<K>): Boolean {
    if (intent.generation !== generation) {
      return false
    }
    val requiredRange = intent.requiredRange ?: return true
    return currentRange === requiredRange &&
        !requiredRange.failed &&
        requiredRange.resolvedRange.captures(intent.key, intent.item)
  }

  private fun ensureReleaseItemsLocked(request: RangeRequest<K>) {
    if (request.releaseItems != null) {
      return
    }
    request.releaseItems =
        request.resolvedRange.orderedReleaseKeys(ledger.byKey.keys).mapNotNull { key ->
          ledger.byKey[key]?.let { item -> ReleaseItem(key, item) }
        }
  }

  private fun nextRangeReleaseOperationLocked(request: RangeRequest<K>): Operation<K>? {
    val releaseItems = checkNotNull(request.releaseItems)
    while (request.nextReleaseIndex < releaseItems.size) {
      val releaseItem = releaseItems[request.nextReleaseIndex++]
      if (
          releaseItem.key !in request.retainedKeys &&
              ledger.byKey[releaseItem.key] === releaseItem.item
      ) {
        return Operation.Release(
            owner = RequestOwner.Range(request),
            ledger = ledger,
            key = releaseItem.key,
            item = releaseItem.item,
        )
      }
    }
    return null
  }

  private fun execute(operation: Operation<K>) {
    val callbackExecuted =
        when (operation) {
          is Operation.Prepare -> {
            tracer.trace({ "work:Prepare" }) { scope.prepare(operation.key, operation.item) }
            true
          }
          is Operation.Release -> {
            if (claimRelease(operation.key, operation.item)) {
              tracer.trace({ "work:Unprepare" }) {
                scope.unprepare(operation.key, operation.item)
              }
              completeRelease(operation.key, operation.item)
              notifyRemovedBestEffort(listOf(operation.key to operation.item))
              true
            } else {
              false
            }
          }
        }
    synchronized(stateLock) {
      operation.lifecycleCallbackExecuted = callbackExecuted
      operation.lifecycleCallbackCompleted = true
    }
  }

  private fun claimRelease(key: K, item: PreparableCollectionItem): Boolean =
      synchronized(providerAccessLock) {
        synchronized(stateLock) {
          claimReleaseLocked(key, item)
        }
      }

  private fun claimReleaseLocked(key: K, item: PreparableCollectionItem): Boolean {
    val identity = ItemIdentity(key, item)
    if (claimedReleases.containsKey(identity)) {
      return true
    }
    if (!adapter.claimReleaseIfSame(key, item)) {
      return false
    }
    claimedReleases[identity] = ReleaseItem(key, item)
    return true
  }

  private fun completeRelease(key: K, item: PreparableCollectionItem) {
    synchronized(stateLock) {
      claimedReleases.remove(ItemIdentity(key, item))
      destroyCleanup?.removeRelease(key, item)
    }
  }

  private fun commit(operation: Operation<K>): Completion =
      synchronized(stateLock) {
        if (activeOperation !== operation) {
          return@synchronized Completion(committed = false, cleanup = Cleanup.None)
        }

        when (operation) {
          is Operation.Prepare -> {
            if (
                destroyed ||
                    (operation.ledger !== ledger && ledger.byKey[operation.key] !== operation.item)
            ) {
              Completion(committed = false, cleanup = Cleanup.UnprepareAndEvict)
            } else {
              ledger.byKey[operation.key] = operation.item
              if (operation.owner is RequestOwner.Key) {
                currentRange
                    ?.takeIf { request -> request.generation === operation.owner.intent.generation }
                    ?.retainedKeys
                    ?.add(operation.key)
              }
              removeDeferredEvictionLocked(operation.key, operation.item)
              Completion(committed = true, cleanup = Cleanup.None)
            }
          }
          is Operation.Release -> {
            val committed = ledger.byKey[operation.key] === operation.item
            if (committed) {
              ledger.byKey.remove(operation.key)
            }
            Completion(committed = committed, cleanup = Cleanup.Evict)
          }
        }
      }

  private fun runCleanup(operation: Operation<K>, cleanup: Cleanup): Throwable? {
    var firstFailure: Throwable? = null
    if (cleanup == Cleanup.UnprepareAndEvict) {
      try {
        if (claimRelease(operation.key, operation.item)) {
          tracer.trace({ "work:CompensatePrepare" }) {
            scope.unprepare(operation.key, operation.item)
          }
          completeRelease(operation.key, operation.item)
          notifyRemovedBestEffort(listOf(operation.key to operation.item))
        }
      } catch (failure: Throwable) {
        firstFailure = failure
      }
    }

    if (cleanup != Cleanup.None) {
      try {
        evictIfStillUncaptured(operation.key, operation.item)
      } catch (failure: Throwable) {
        if (firstFailure == null) {
          firstFailure = failure
        } else {
          reportFailure("Secondary cleanup failure", failure)
        }
      }
    }
    return firstFailure
  }

  private fun fail(operation: Operation<K>): List<DeferredEviction<K>> =
      synchronized(stateLock) {
        if (activeOperation === operation) {
          activeOperation = null
        }
        when (val owner = operation.owner) {
          is RequestOwner.Range -> {
            if (currentRange === owner.request) {
              owner.request.failed = true
            }
          }
          is RequestOwner.Key -> {
            if (pendingByKey[owner.intent.key] === owner.intent) {
              pendingByKey.remove(owner.intent.key)
            }
          }
        }
        discardInvalidIntentsLocked()
        val evictions = settleDeferredEvictionsLocked()
        ensurePulseLocked()
        evictions
      }

  private fun finish(operation: Operation<K>, committed: Boolean): Finalization<K> {
    synchronized(stateLock) {
      if (activeOperation !== operation) {
        return Finalization(failure = null, evictions = emptyList())
      }
      val needsCompensation =
          committed &&
              operation is Operation.Prepare &&
              ledger.byKey[operation.key] !== operation.item
      if (!needsCompensation) {
        activeOperation = null
        discardInvalidIntentsLocked()
        val evictions = settleDeferredEvictionsLocked()
        ensurePulseLocked()
        return Finalization(failure = null, evictions = evictions)
      }
    }

    val failure = runCleanup(operation, Cleanup.UnprepareAndEvict)
    val evictions =
        synchronized(stateLock) {
          if (activeOperation === operation) {
            activeOperation = null
          }
          discardInvalidIntentsLocked()
          val ready = settleDeferredEvictionsLocked()
          ensurePulseLocked()
          ready
        }
    return Finalization(failure = failure, evictions = evictions)
  }

  private fun discardInvalidIntentsLocked() {
    val iterator = pendingByKey.entries.iterator()
    while (iterator.hasNext()) {
      if (!isIntentCurrentLocked(iterator.next().value)) {
        iterator.remove()
      }
    }
  }

  private fun evictIfStillUncaptured(key: K, item: PreparableCollectionItem) {
    val evicted =
        synchronized(providerAccessLock) {
          val captured =
              synchronized(stateLock) {
                if (ledger.byKey[key] === item) {
                  true
                } else if (hasFutureCaptureLocked(key, item)) {
                  addDeferredEvictionLocked(key, item)
                  true
                } else {
                  false
                }
              }
          if (captured) {
            false
          } else {
            val removed = tracer.trace({ "work:Evict" }) { adapter.evictIfSame(key, item) }
            synchronized(stateLock) { removeDeferredEvictionLocked(key, item) }
            removed
          }
        }
    if (evicted) {
      notifyRemovedBestEffort(listOf(key to item))
    }
  }

  private fun notifyRemovedBestEffort(removed: List<Pair<K, PreparableCollectionItem>>) {
    removed.forEach { (key, item) ->
      try {
        adapter.notifyRemoved(key, item)
      } catch (failure: Throwable) {
        reportFailure("Removal notification failed", failure)
      }
    }
  }

  private fun hasFutureCaptureLocked(key: K, item: PreparableCollectionItem): Boolean {
    val rangeWillPrepareItem =
        currentRange?.let { request ->
          if (request.failed) {
            false
          } else {
            (request.nextPrepareIndex until request.resolvedRange.items.size).any { index ->
              val resolvedItem = request.resolvedRange.items[index]
              resolvedItem.key == key && resolvedItem.item === item
            }
          }
        } == true
    val keyIntentWillPrepareItem =
        pendingByKey[key]?.let { intent ->
          isIntentCurrentLocked(intent) && intent.item === item
        } == true
    val activePrepareOwnsItem =
        (activeOperation as? Operation.Prepare)?.let { operation ->
          operation.key == key && operation.item === item
        } == true
    return rangeWillPrepareItem || keyIntentWillPrepareItem || activePrepareOwnsItem
  }

  private fun addDeferredEvictionLocked(key: K, item: PreparableCollectionItem) {
    val identity = ItemIdentity(key, item)
    if (!deferredEvictions.containsKey(identity)) {
      deferredEvictions[identity] = DeferredEviction(key, item)
    }
  }

  private fun removeDeferredEvictionLocked(key: K, item: PreparableCollectionItem) {
    deferredEvictions.remove(ItemIdentity(key, item))
  }

  private fun settleDeferredEvictionsLocked(): List<DeferredEviction<K>> {
    val ready = mutableListOf<DeferredEviction<K>>()
    val iterator = deferredEvictions.entries.iterator()
    while (iterator.hasNext()) {
      val eviction = iterator.next().value
      if (ledger.byKey[eviction.key] === eviction.item) {
        iterator.remove()
      } else if (!hasFutureCaptureLocked(eviction.key, eviction.item)) {
        iterator.remove()
        ready.add(eviction)
      }
    }
    return ready
  }

  private fun runDestroyCleanup(cleanup: DestroyCleanup<K>) {
    try {
      cleanup.releases().forEach { release ->
        try {
          scope.unprepare(release.key, release.item)
          completeRelease(release.key, release.item)
        } catch (failure: Throwable) {
          reportFailure("Destroy unprepare failed", failure)
        }
        // Destroy already took the item out of the cache, so the provider is told either way.
        notifyRemovedBestEffort(listOf(release.key to release.item))
      }
    } finally {
      scope.clearAll()
      dispatcher.quit()
    }
  }

  private fun evictBestEffort(evictions: List<DeferredEviction<K>>) {
    evictions.forEach { eviction ->
      try {
        evictIfStillUncaptured(eviction.key, eviction.item)
      } catch (failure: Throwable) {
        reportFailure("Deferred eviction failed", failure)
      }
    }
  }

  private fun reportFailure(message: String, failure: Throwable) {
    if (failure is VirtualMachineError || failure is ThreadDeath) {
      throw failure
    }
    try {
      Log.e(TAG, message, failure)
    } catch (_: RuntimeException) {
      // android.util.Log is a throwing stub in host-side unit tests.
    }
  }

  /** Observer failures are isolated after commit so diagnostics cannot stop reconciliation. */
  private fun notifyObserver(operation: Operation<K>) {
    try {
      when (operation) {
        is Operation.Prepare -> observer?.onItemPrepared(operation.key, operation.source)
        is Operation.Release -> observer?.onItemUnprepared(operation.key, operation.reason)
      }
    } catch (failure: Throwable) {
      reportFailure("Preparation health observer failed", failure)
    }
  }

  private class Generation

  private class Pulse

  private class ResolveAttempt(val generation: Generation)

  private class ScheduleStart<K : Any>(
      val attempt: ResolveAttempt,
      val evictions: List<DeferredEviction<K>>,
  )

  private class KeyResolutionSnapshot<K : Any>(
      val generation: Generation,
      val requiredRange: RangeRequest<K>?,
  )

  private class PreparedLedger<K : Any>(
      preparedItems: Map<K, PreparableCollectionItem> = emptyMap(),
  ) {
    val byKey = LinkedHashMap(preparedItems)
  }

  private class RangeRequest<K : Any>(
      val resolvedRange: ResolvedRange<K>,
      val generation: Generation,
      var nextPrepareIndex: Int = 0,
      var releaseItems: List<ReleaseItem<K>>? = null,
      var nextReleaseIndex: Int = 0,
      var failed: Boolean = false,
      val retainedKeys: MutableSet<K> = mutableSetOf(),
  )

  private class ReleaseItem<K : Any>(
      val key: K,
      val item: PreparableCollectionItem,
  )

  private class KeyIntent<K : Any>(
      val key: K,
      val item: PreparableCollectionItem,
      val source: PreparationSource,
      val generation: Generation,
      val requiredRange: RangeRequest<K>?,
  )

  private sealed class RequestOwner<K : Any> {
    class Range<K : Any>(val request: RangeRequest<K>) : RequestOwner<K>()

    class Key<K : Any>(val intent: KeyIntent<K>) : RequestOwner<K>()
  }

  private sealed class Operation<K : Any>(
      val owner: RequestOwner<K>,
      val ledger: PreparedLedger<K>,
      val key: K,
      val item: PreparableCollectionItem,
  ) {
    var lifecycleCallbackExecuted: Boolean = false
    var lifecycleCallbackCompleted: Boolean = false

    class Prepare<K : Any>(
        owner: RequestOwner<K>,
        ledger: PreparedLedger<K>,
        key: K,
        item: PreparableCollectionItem,
        val source: PreparationSource,
    ) : Operation<K>(owner, ledger, key, item)

    class Release<K : Any>(
        owner: RequestOwner<K>,
        ledger: PreparedLedger<K>,
        key: K,
        item: PreparableCollectionItem,
        val reason: UnprepareReason = UnprepareReason.RANGE_EVICTION,
    ) : Operation<K>(owner, ledger, key, item)
  }

  private class ItemIdentity<K : Any>(
      val key: K,
      val item: PreparableCollectionItem,
  ) {
    override fun equals(other: Any?): Boolean =
        other is ItemIdentity<*> && key == other.key && item === other.item

    override fun hashCode(): Int = 31 * key.hashCode() + System.identityHashCode(item)
  }

  private class DeferredEviction<K : Any>(
      val key: K,
      val item: PreparableCollectionItem,
  )

  private class DestroyCleanup<K : Any>(releases: List<ReleaseItem<K>>) {
    private val releasesByIdentity =
        LinkedHashMap<ItemIdentity<K>, ReleaseItem<K>>().apply {
          releases.forEach { release -> put(ItemIdentity(release.key, release.item), release) }
        }

    @Synchronized
    fun removeRelease(key: K, item: PreparableCollectionItem) {
      releasesByIdentity.remove(ItemIdentity(key, item))
    }

    @Synchronized fun releases(): List<ReleaseItem<K>> = releasesByIdentity.values.toList()
  }

  private class Completion(
      val committed: Boolean,
      val cleanup: Cleanup,
  )

  private class Finalization<K : Any>(
      val failure: Throwable?,
      val evictions: List<DeferredEviction<K>>,
  )

  private enum class Cleanup {
    None,
    Evict,
    UnprepareAndEvict,
  }

  private companion object {
    private const val TAG = "PreparedStateRangeWorker"
  }
}
