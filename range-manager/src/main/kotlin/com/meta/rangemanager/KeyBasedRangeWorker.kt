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

import androidx.annotation.UiThread
import com.meta.systrace.NoOpSystracer
import com.meta.systrace.Systracer

/**
 * A range representation that uses a list of keys to identify items.
 *
 * This interface provides a type-safe way to represent ranges in collections where items are
 * identified by keys rather than positions. It's particularly useful for collections where:
 * - Items may be reordered or inserted/removed
 * - Stable identifiers are needed across collection changes
 * - O(1) lookups are important for performance
 * - Priority-based ordering is needed for preparation
 *
 * The keys are stored in a list (not a set) to preserve ordering, which allows the range calculator
 * to control preparation priority by ordering keys appropriately.
 *
 * @param K The type of keys used to identify items in the range
 */
interface KeyBasedRange<K : Any> {

  val keys: List<K>

  /**
   * Checks if this range contains the given key.
   *
   * @param key The key to check
   * @return true if the key is in this range, false otherwise
   */
  operator fun contains(key: K): Boolean = key in keys

  /** Returns the number of items in this range. */
  val size: Int
    get() = keys.size

  /** Returns true if this range is empty. */
  fun isEmpty(): Boolean = keys.isEmpty()
}

/**
 * A [RangeWorker] over a key-based collection that can also express preparation intent for one key.
 *
 * Implementations resolve the provider item on the calling thread, where lookup failures are
 * reported. Lifecycle work may complete immediately or later. Callers must not rely on a distinct
 * prepare callback for every call or FIFO ordering between repeated requests: asynchronous pending
 * requests may coalesce to the latest intent per key.
 */
interface KeyPreparingRangeWorker<K : Any> : RangeWorker<Any, KeyBasedRange<K>> {

  /**
   * Requests preparation of [key] without applying a range-membership check.
   *
   * This is intended for consumers such as Feed that already checked their own current range. The
   * item's prepare operation must be idempotent because an explicit refresh may prepare an item
   * that is already represented as prepared. Delayed work may be invalidated by a later [schedule],
   * [cancel], [reset], or [destroy] before it starts.
   */
  @UiThread fun prepareKey(key: K)

  /**
   * Requests preparation of [key] only while the latest scheduled target captures it.
   *
   * This is intended for streaming consumers that need the worker to enforce current-target
   * membership. A no-op before the first [schedule]. Delayed implementations also require the
   * provider item to be the exact item captured by that target, and may coalesce or invalidate the
   * accepted intent before lifecycle work starts.
   *
   * @return `true` when the current target accepted the intent, not when preparation completed.
   */
  @UiThread fun prepareKeyIfInRange(key: K): Boolean
}

/**
 * Key-based range worker that prepares on the calling thread.
 *
 * @param releaseOutOfRangeOnReset whether rows it prepared that fall outside the next range after a
 *   [reset] are released. [RangeManager.onDataSetChanged] resets the worker, and without this the
 *   rows prepared before the reset are forgotten: one that still exists but is outside the new
 *   range stays prepared until the provider releases it. With it, the next [schedule] releases
 *   them, as [AsynchronousKeyBasedRangeWorker] does; a row the provider has already released is
 *   skipped, so none is released twice. Off by default so existing callers keep their behavior
 *   until they opt in.
 */
class KeyBasedRangeWorker<K : Any>(
    private val itemProvider: CollectionItemProvider<K>,
    private val scope: PrepareScope = PrepareScope(),
    tracer: Systracer = NoOpSystracer(),
    private val observer: PreparationHealthObserver? = null,
    // TODO(T291074975): make this the behavior and remove the flag once Feed has tested it.
    private val releaseOutOfRangeOnReset: Boolean = false,
) :
    SynchronousRangeWorker<Any, KeyBasedRange<K>>(
        workQueueBuilder = KeyBasedWorkQueueBuilder(itemProvider),
        scope = scope,
        tracer = tracer,
        observer = observer,
    ),
    KeyPreparingRangeWorker<K> {

  /** With [releaseOutOfRangeOnReset], what was prepared before the last [reset], for [schedule]. */
  private var rangeBeforeReset: KeyBasedRange<K>? = null

  override fun schedule(position: Any, range: KeyBasedRange<K>) {
    if (releaseOutOfRangeOnReset) {
      val previous = currentRange ?: rangeBeforeReset
      rangeBeforeReset = null
      currentRange = previous?.let(::stillCached)
    }
    super.schedule(position, range)
  }

  override fun reset() {
    if (releaseOutOfRangeOnReset) {
      rangeBeforeReset = currentRange ?: rangeBeforeReset
    }
    super.reset()
  }

  /** [range]'s keys whose items the provider still holds; releasing any other would create one. */
  private fun stillCached(range: KeyBasedRange<K>): KeyBasedRange<K>? {
    val keys = range.keys.filter(itemProvider::isCached)
    return if (keys.isEmpty()) null else CachedKeys(keys)
  }

  private class CachedKeys<K : Any>(override val keys: List<K>) : KeyBasedRange<K>

  override fun prepareKey(key: K) {
    val item = itemProvider[key]
    scope.prepare(key, item)
    observer?.onItemPrepared(key, PreparationSource.PREPARE_KEY)
  }

  override fun prepareKeyIfInRange(key: K): Boolean {
    val range = currentRange ?: return false
    if (key !in range) return false

    val item = itemProvider[key]
    scope.prepare(key, item)
    observer?.onItemPrepared(key, PreparationSource.PREPARE_KEY_IN_RANGE)
    return true
  }
}

/**
 * Work queue builder implementation for key-based collections.
 *
 * This implementation creates a work queue that:
 * 1. Prepares items in the new range in priority order (as provided by the range)
 * 2. Releases items from the previous range that are no longer in the new range
 *
 * The ordering of keys in the range is preserved, allowing the range calculator to control
 * preparation priority by ordering keys appropriately.
 *
 * @param K The type of keys used to identify items
 * @param itemProvider The provider for retrieving items by key
 */
class KeyBasedWorkQueueBuilder<K : Any>(
    private val itemProvider: CollectionItemProvider<K>,
) : WorkQueueBuilder<Any, KeyBasedRange<K>> {

  /**
   * Builds a work queue for preparing and releasing items in key-based collections.
   *
   * @param previousRange The previously prepared range
   * @param newPosition The current visible position
   * @param newRange The new range to prepare with keys ordered by priority
   * @return A work queue ordered by priority (items ordered as provided by range)
   */
  override fun buildWorkQueue(
      previousRange: KeyBasedRange<K>,
      newPosition: Any,
      newRange: KeyBasedRange<K>,
  ): List<ItemWork> {
    return buildList {
      // Add prepare work for items in new range (priority-ordered by range calculator)
      newRange.keys.forEach { key ->
        add(
            ItemWork(
                key = key,
                item = itemProvider[key],
                type = WorkType.Prepare,
                execute = { scope, workItem, workKey -> scope.prepare(workKey, workItem) },
            ),
        )
      }

      // Add release work for items no longer in range
      previousRange.keys.forEach { key ->
        if (key !in newRange) {
          add(
              ItemWork(
                  key = key,
                  item = itemProvider[key],
                  type = WorkType.Unprepare,
                  execute = { scope, workItem, workKey ->
                    scope.unprepare(workKey, workItem)
                    @Suppress("UNCHECKED_CAST") itemProvider.evict(workKey as K)
                  },
              ),
          )
        }
      }
    }
  }
}
