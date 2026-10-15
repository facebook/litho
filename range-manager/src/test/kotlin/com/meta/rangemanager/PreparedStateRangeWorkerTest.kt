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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test

class PreparedStateRangeWorkerTest {

  private lateinit var dispatcher: FakeRangeWorkDispatcher
  private lateinit var policy: TestRangeAdapter
  private lateinit var worker: PreparedStateRangeWorker<Unit, List<Int>, Int>
  private val events = mutableListOf<String>()

  @Before
  fun setup() {
    dispatcher = FakeRangeWorkDispatcher()
    policy = TestRangeAdapter(dispatcher, events)
    worker = createWorker()
  }

  @Test
  fun `worker rejects an adapter without exact eviction`() {
    policy.supportsExactEviction = false

    val failure = runCatching {
      PreparedStateRangeWorker(
          adapter = policy,
          scope = PrepareScope(),
          dispatcher = FakeRangeWorkDispatcher(),
      )
    }
        .exceptionOrNull()

    assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `partial disjoint schedule converges from completed work`() {
    policy.setItems(0..4)
    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()

    worker.schedule(Unit, listOf(2, 3, 4))
    dispatcher.runOne()
    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
    assertThat(policy.item(2).prepareCount).isEqualTo(1)
    assertThat(policy.item(2).unprepareCount).isEqualTo(1)
    assertThat(policy.item(3).prepareCount).isZero()
    assertThat(policy.item(4).prepareCount).isZero()
  }

  @Test
  fun `overlap is not prepared again`() {
    policy.setItems(0..3)
    worker.schedule(Unit, listOf(0, 1, 2))
    dispatcher.drain()

    worker.schedule(Unit, listOf(1, 2, 3))
    dispatcher.drain()

    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
    assertThat(policy.item(2).prepareCount).isEqualTo(1)
    assertThat(policy.item(3).prepareCount).isEqualTo(1)
  }

  @Test
  fun `one callback performs one item and all prepares precede releases`() {
    policy.setItems(0..4)
    worker.schedule(Unit, listOf(0, 1, 2))
    dispatcher.drain()
    events.clear()

    worker.schedule(Unit, listOf(3, 4))

    assertThat(dispatcher.runOne()).isTrue()
    assertThat(events).containsExactly("prepare:3")
    assertThat(dispatcher.queuedCount).isEqualTo(1)

    dispatcher.drain()

    assertThat(events)
        .containsExactly(
            "prepare:3",
            "prepare:4",
            "unprepare:0",
            "evict:0",
            "unprepare:1",
            "evict:1",
            "unprepare:2",
            "evict:2",
        )
  }

  @Test
  fun `large disjoint plans use bounded selection work`() {
    val counters = SelectionCounters()
    val firstKeys =
        (0 until LARGE_PLAN_SIZE).map { value -> CountingKey(value = value, counters = counters) }
    val secondKeys =
        (LARGE_PLAN_SIZE until LARGE_PLAN_SIZE * 2).map { value ->
          CountingKey(value = value, counters = counters)
        }
    val items = (firstKeys + secondKeys).associateWith { CountingItem() }
    val countingWorker = PreparedStateRangeWorker(
        adapter = CountingRangeAdapter(items, counters),
        scope = PrepareScope(),
        dispatcher = dispatcher,
    )
    countingWorker.schedule(Unit, firstKeys)
    dispatcher.drain()

    countingWorker.schedule(Unit, secondKeys)
    counters.reset()
    var selectionCallbacks = 0
    while (dispatcher.runOne()) {
      selectionCallbacks++
    }
    val membershipChecks = counters.membershipChecks
    val comparatorCalls = counters.comparatorCalls

    assertThat(selectionCallbacks).isEqualTo(LARGE_PLAN_SIZE * 2)
    assertThat(membershipChecks)
        .isLessThanOrEqualTo(LARGE_PLAN_SIZE * MAX_MEMBERSHIP_CHECKS_PER_KEY)
    assertThat(comparatorCalls).isLessThanOrEqualTo(LARGE_PLAN_SIZE * MAX_COMPARATOR_CALLS_PER_KEY)
    assertThat(firstKeys.map { key -> items.getValue(key).unprepareCount }).containsOnly(1)
    assertThat(secondKeys.map { key -> items.getValue(key).prepareCount }).containsOnly(1)
  }

  @Test
  fun `first key occurrence wins`() {
    policy.setItems(0..1)

    worker.schedule(Unit, listOf(0, 0, 1, 0))
    dispatcher.drain()

    assertThat(policy.lookupKeys).containsExactly(0, 1)
    assertThat(events).containsExactly("prepare:0", "prepare:1")
  }

  @Test
  fun `cancel before first run drops queued work`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0, 1))

    worker.cancel()
    dispatcher.drain()

    assertThat(events).isEmpty()
    assertThat(dispatcher.queuedCount).isZero()
  }

  @Test
  fun `dequeued callback cannot start after it becomes stale`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0))
    assertThat(dispatcher.dequeueOne()).isTrue()

    worker.schedule(Unit, listOf(1))
    assertThat(dispatcher.runDequeued()).isTrue()
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `schedule supersession while item callback is in flight reconciles latest plan`() {
    policy.setItems(0..2)
    val prepareStarted = CountDownLatch(1)
    val releasePrepare = CountDownLatch(1)
    policy.item(0).onPrepare = {
      prepareStarted.countDown()
      releasePrepare.await()
    }
    worker.schedule(Unit, listOf(0, 2))
    val workerThread = Thread { dispatcher.runOne() }.apply { start() }
    try {
      assertThat(prepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      worker.schedule(Unit, listOf(1))
    } finally {
      releasePrepare.countDown()
      workerThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
    }
    assertThat(workerThread.isAlive).isFalse()
    dispatcher.drain()

    assertThat(events).containsExactly("prepare:0", "prepare:1", "unprepare:0", "evict:0")
    assertThat(policy.item(2).prepareCount).isZero()
    assertThat(policy.lookupDuringDispatch).isFalse()
  }

  @Test
  fun `stale release completion is reconsidered by superseding prepare cursor`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()
    policy.item(0).onUnprepare = { worker.schedule(Unit, listOf(0)) }

    worker.schedule(Unit, listOf(1))
    dispatcher.runOne()
    policy.item(0).onUnprepare = null
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(2)
    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).unprepareCount).isEqualTo(1)
  }

  @Test
  fun `cancel during prepare keeps successful same epoch item represented`() {
    policy.setItems(0..1)
    policy.item(0).onPrepare = { worker.cancel() }

    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()
    policy.item(0).onPrepare = null
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(0).unprepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isZero()
  }

  @Test
  fun `reset compensates late prepare and next schedule is fresh`() {
    policy.setItems(0..0)
    val oldItem = policy.item(0)
    oldItem.onPrepare = { worker.reset() }

    worker.schedule(Unit, listOf(0))
    dispatcher.runOne()

    assertThat(oldItem.prepareCount).isEqualTo(1)
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(dispatcher.queuedCount).isZero()

    val newItem = TestItem(0, events)
    policy.replace(0, newItem)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(newItem.prepareCount).isEqualTo(1)
    assertThat(newItem.unprepareCount).isZero()
  }

  @Test
  fun `reset after prepare commit transfers the active item for later reconciliation`() {
    policy.setItems(0..0)
    worker =
        createWorker(
            observer =
                object : NoOpPreparationHealthObserver() {
                  override fun onItemPrepared(key: Any, source: PreparationSource) {
                    worker.reset()
                  }
                },
        )

    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(0).unprepareCount).isZero()
    assertThat(events).containsExactly("prepare:0")

    worker.schedule(Unit, emptyList())
    dispatcher.drain()

    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(events).containsExactly("prepare:0", "unprepare:0", "evict:0")
  }

  @Test
  fun `reset during same item refresh retains successful ownership`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    item.onPrepare = { worker.reset() }

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(2)
    assertThat(item.unprepareCount).isZero()

    item.onPrepare = null
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(2)
    assertThat(item.unprepareCount).isZero()
  }

  @Test
  fun `reset retains completed rows for reconciliation`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()

    worker.reset()
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(0).unprepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).unprepareCount).isEqualTo(1)
  }

  @Test
  fun `reset does not release a completed item already released by the provider`() {
    policy.setItems(0..0)
    val oldItem = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    PrepareScope().unprepare(0, oldItem)
    policy.replace(0, TestItem(0, events))

    worker.reset()
    dispatcher.drain()

    assertThat(oldItem.unprepareCount).isEqualTo(1)
  }

  @Test
  fun `destroy drops queued work`() {
    policy.setItems(0..0)
    worker.schedule(Unit, listOf(0))

    worker.destroy()
    dispatcher.drain()

    assertThat(events).isEmpty()
    assertThat(dispatcher.quitCalled).isTrue()
  }

  /**
   * A dispatcher that can't take the cleanup must not leave prepared items or saved state behind.
   */
  @Test
  fun `destroy cleans up on the calling thread when the dispatcher refuses the cleanup`() {
    policy.setItems(0..0)
    policy.removeItemsOnEvict = true
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    events.clear()
    dispatcher.acceptsWork = false

    worker.destroy()

    assertThat(events).containsExactly("evict:0", "unprepare:0")
    assertThat(dispatcher.quitCalled).isTrue()
  }

  /** A refused pulse must not leave the worker believing one is still on its way. */
  @Test
  fun `a refused pulse is posted again on the next request`() {
    policy.setItems(0..1)
    dispatcher.acceptsWork = false
    worker.schedule(Unit, listOf(0))
    dispatcher.acceptsWork = true

    worker.prepareKey(1, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `destroy drains completed ledger before quitting`() {
    policy.setItems(0..0)
    policy.removeItemsOnEvict = true
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    events.clear()

    worker.destroy()

    assertThat(events).containsExactly("evict:0")
    assertThat(dispatcher.quitCalled).isFalse()

    dispatcher.drain()

    assertThat(events).containsExactly("evict:0", "unprepare:0")
    assertThat(dispatcher.quitCalled).isTrue()
  }

  @Test
  fun `destroy detaches exact items before a replacement worker resolves`() {
    policy.setItems(0..0)
    policy.removeItemsOnEvict = true
    policy.createMissingItems = true
    val oldItem = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    worker.destroy()
    val replacementDispatcher = FakeRangeWorkDispatcher()
    val replacementWorker = PreparedStateRangeWorker(
        adapter = policy,
        scope = PrepareScope(),
        dispatcher = replacementDispatcher,
    )
    replacementWorker.schedule(Unit, listOf(0))
    val replacement = policy.item(0)
    replacementDispatcher.drain()
    dispatcher.drain()

    assertThat(replacement).isNotSameAs(oldItem)
    assertThat(oldItem.prepareCount).isEqualTo(1)
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(replacement.prepareCount).isEqualTo(1)
    assertThat(replacement.unprepareCount).isZero()
  }

  @Test
  fun `destroyed dequeued callback cannot start`() {
    policy.setItems(0..0)
    worker.schedule(Unit, listOf(0))
    assertThat(dispatcher.dequeueOne()).isTrue()

    worker.destroy()
    assertThat(dispatcher.runDequeued()).isTrue()

    assertThat(events).isEmpty()
  }

  @Test
  fun `destroy after prepare commit releases the active item once`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker =
        createWorker(
            observer =
                object : NoOpPreparationHealthObserver() {
                  override fun onItemPrepared(key: Any, source: PreparationSource) {
                    worker.destroy()
                  }
                },
        )

    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(item.unprepareCount).isEqualTo(1)
    assertThat(dispatcher.quitCalled).isTrue()
  }

  @Test
  fun `destroy compensates in flight prepare and posts only teardown`() {
    policy.setItems(0..1)
    val item = policy.item(0)
    item.onPrepare = { worker.destroy() }

    worker.schedule(Unit, listOf(0, 1))
    dispatcher.runOne()

    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(item.unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isZero()
    assertThat(dispatcher.queuedCount).isEqualTo(1)

    dispatcher.drain()

    assertThat(dispatcher.quitCalled).isTrue()
  }

  @Test
  fun `destroy retains active release ownership when the callback fails`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    val failure = IllegalStateException("unprepare failed")
    var firstUnprepare = true
    item.onUnprepare = {
      if (firstUnprepare) {
        firstUnprepare = false
        worker.destroy()
        item.unprepareFailure = failure
      } else {
        item.unprepareFailure = null
      }
    }

    worker.schedule(Unit, emptyList())

    assertThat(runCatching { dispatcher.runOne() }.exceptionOrNull()).isSameAs(failure)
    dispatcher.drain()

    assertThat(item.unprepareCount).isEqualTo(2)
    assertThat(dispatcher.quitCalled).isTrue()
  }

  @Test
  fun `reset during a non retaining cache release does not release the item twice`() {
    policy.setItems(0..0)
    policy.retainWithoutCache = true
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    item.onUnprepare = { worker.reset() }

    worker.schedule(Unit, emptyList())
    dispatcher.drain()

    assertThat(item.unprepareCount).isEqualTo(1)
  }

  @Test
  fun `operation failure does not commit or post a tail`() {
    policy.setItems(0..1)
    val item = policy.item(0)
    item.prepareFailure = IllegalStateException("prepare failed")
    worker.schedule(Unit, listOf(0, 1))

    assertThatThrownBy { dispatcher.runOne() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("prepare failed")
    assertThat(dispatcher.queuedCount).isZero()
    assertThat(policy.item(1).prepareCount).isZero()

    item.prepareFailure = null
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(2)
  }

  @Test
  fun `old prepare failure does not discard a newer request`() {
    policy.setItems(0..1)
    val failure = IllegalStateException("prepare failed")
    policy.item(0).onPrepare = { worker.schedule(Unit, listOf(1)) }
    policy.item(0).prepareFailure = failure
    worker.schedule(Unit, listOf(0))

    assertThat(runCatching { dispatcher.runOne() }.exceptionOrNull()).isSameAs(failure)
    assertThat(dispatcher.queuedCount).isEqualTo(1)

    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `old eviction failure does not discard a newer request`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    val failure = IllegalStateException("eviction failed")
    policy.onEvict = { worker.schedule(Unit, listOf(1)) }
    policy.evictFailure = failure
    worker.schedule(Unit, emptyList())

    assertThat(runCatching { dispatcher.runOne() }.exceptionOrNull()).isSameAs(failure)
    assertThat(dispatcher.queuedCount).isEqualTo(1)
    policy.onEvict = null
    policy.evictFailure = null

    dispatcher.drain()

    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `committed release notifies observer before eviction failure`() {
    policy.setItems(0..0)
    val unpreparedKeys = mutableListOf<Any>()
    worker =
        createWorker(
            observer =
                object : NoOpPreparationHealthObserver() {
                  override fun onItemUnprepared(key: Any, reason: UnprepareReason) {
                    unpreparedKeys.add(key)
                  }
                },
        )
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    policy.evictFailure = IllegalStateException("eviction failed")
    worker.schedule(Unit, emptyList())

    assertThat(runCatching { dispatcher.runOne() }.exceptionOrNull()).isSameAs(policy.evictFailure)
    assertThat(unpreparedKeys).containsExactly(0)
  }

  @Test
  fun `eviction failure leaves physically unprepared item out of ledger`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    policy.evictFailure = IllegalStateException("eviction failed")
    worker.schedule(Unit, emptyList())

    assertThatThrownBy { dispatcher.runOne() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("eviction failed")
    assertThat(item.unprepareCount).isEqualTo(1)
    assertThat(dispatcher.queuedCount).isZero()

    policy.evictFailure = null
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(2)
  }

  @Test
  fun `on demand prepare resolves now and executes later`() {
    policy.setItems(0..0)

    val accepted =
        worker.prepareKey(
            key = 0,
            source = PreparationSource.PREPARE_KEY,
            requireCurrentRange = false,
            itemForKey = policy::resolveItem,
        )

    assertThat(accepted).isTrue()
    assertThat(policy.lookupKeys).containsExactly(0)
    assertThat(policy.item(0).prepareCount).isZero()

    dispatcher.runOne()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.lookupDuringDispatch).isFalse()
  }

  @Test
  fun `on demand prepare shares the ledger and repeats explicit preparation`() {
    policy.setItems(0..0)

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(2)
    assertThat(policy.item(0).unprepareCount).isZero()
  }

  @Test
  fun `pending requests for the same key coalesce to the latest item`() {
    policy.setItems(0..0)
    val supersededItem = policy.item(0)
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    val latestItem = TestItem(0, events)
    policy.replace(0, latestItem)

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(supersededItem.prepareCount).isZero()
    assertThat(latestItem.prepareCount).isEqualTo(1)
  }

  @Test
  fun `active key retains only its latest successor`() {
    policy.setItems(0..0)
    val activeItem = policy.item(0)
    val supersededSuccessor = TestItem(0, events)
    val latestSuccessor = TestItem(0, events)
    activeItem.onPrepare = {
      policy.replace(0, supersededSuccessor)
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
      policy.replace(0, latestSuccessor)
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(activeItem.prepareCount).isEqualTo(1)
    assertThat(supersededSuccessor.prepareCount).isZero()
    assertThat(latestSuccessor.prepareCount).isEqualTo(1)
  }

  @Test
  fun `coalescing one key retains other pending keys`() {
    policy.setItems(0..1)

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    worker.prepareKey(1, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `on demand refresh runs before unrelated range releases`() {
    policy.setItems(0..3)
    worker.schedule(Unit, listOf(0, 1, 2, 3))
    dispatcher.drain()
    events.clear()

    worker.schedule(Unit, emptyList())
    worker.prepareKey(3, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(events.first()).isEqualTo("prepare:3")
    assertThat(policy.item(3).prepareCount).isEqualTo(2)
    assertThat(policy.item(3).unprepareCount).isZero()
    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).unprepareCount).isEqualTo(1)
    assertThat(policy.item(2).unprepareCount).isEqualTo(1)
  }

  @Test
  fun `on demand replacement releases before preparing the new item`() {
    policy.setItems(0..0)
    val oldItem = policy.item(0)
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()
    events.clear()
    val newItem = TestItem(0, events)
    policy.replace(0, newItem)

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)

    dispatcher.runOne()
    assertThat(events).containsExactly("unprepare:0", "evict:0")
    assertThat(dispatcher.queuedCount).isEqualTo(1)

    dispatcher.runOne()
    assertThat(events).containsExactly("unprepare:0", "evict:0", "prepare:0")
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(newItem.prepareCount).isEqualTo(1)
  }

  @Test
  fun `conditional on demand prepare requires the latest exact range capture`() {
    policy.setItems(0..1)
    val capturedItem = policy.item(0)
    worker.schedule(Unit, listOf(0))

    assertThat(
        worker.prepareKey(
            0,
            PreparationSource.PREPARE_KEY_IN_RANGE,
            true,
            policy::resolveItem,
        ),
    )
        .isTrue()

    val replacement = TestItem(0, events)
    policy.replace(0, replacement)
    assertThat(
        worker.prepareKey(
            0,
            PreparationSource.PREPARE_KEY_IN_RANGE,
            true,
            policy::resolveItem,
        ),
    )
        .isFalse()
    assertThat(
        worker.prepareKey(
            1,
            PreparationSource.PREPARE_KEY_IN_RANGE,
            true,
            policy::resolveItem,
        ),
    )
        .isFalse()

    dispatcher.drain()

    assertThat(capturedItem.prepareCount).isEqualTo(2)
    assertThat(replacement.prepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isZero()
  }

  @Test
  fun `cancel invalidates an already dequeued on demand request`() {
    policy.setItems(0..0)
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    assertThat(dispatcher.dequeueOne()).isTrue()

    worker.cancel()
    assertThat(dispatcher.runDequeued()).isTrue()

    assertThat(policy.item(0).prepareCount).isZero()
  }

  @Test
  fun `accepted on demand capture prevents eviction during in flight release`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    events.clear()
    item.onUnprepare = {
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }

    worker.schedule(Unit, emptyList())
    dispatcher.drain()

    assertThat(events).containsExactly("unprepare:0", "prepare:0")
    assertThat(item.prepareCount).isEqualTo(2)
  }

  @Test
  fun `newer on demand request completes after the older range target`() {
    policy.setItems(0..1)
    val oldItem = policy.item(0)
    worker.schedule(Unit, listOf(1, 0))
    val newItem = TestItem(0, events)
    policy.replace(0, newItem)

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    dispatcher.drain()

    assertThat(oldItem.prepareCount).isEqualTo(1)
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(newItem.prepareCount).isEqualTo(1)
    assertThat(newItem.unprepareCount).isZero()
  }

  @Test
  fun `cancelling a request that owns deferred eviction evicts its captured item`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    events.clear()
    item.onUnprepare = {
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }

    worker.schedule(Unit, emptyList())
    dispatcher.runOne()
    worker.cancel()
    dispatcher.drain()

    assertThat(events).containsExactly("unprepare:0", "evict:0")
    assertThat(item.prepareCount).isEqualTo(1)
  }

  /**
   * A deferred eviction that becomes ready inside prepareKey is reported to the provider after the
   * worker lets go of its provider lock, so a hook that waits on another thread calling the worker
   * cannot deadlock it.
   */
  @Test
  fun `a deferred eviction settled by prepareKey is reported outside the provider lock`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    item.onUnprepare = {
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }
    worker.schedule(Unit, emptyList())
    dispatcher.runOne()
    var otherThreadBlocked: Boolean? = null
    var other: Thread? = null
    policy.onNotifyRemoved = {
      val thread = Thread { worker.reset() }.also { other = it }
      thread.start()
      thread.join(BLOCK_CHECK_MS)
      otherThreadBlocked = thread.isAlive
    }
    policy.replace(0, TestItem(0, events))

    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    other?.join(AWAIT_SECONDS * 1000)

    assertThat(otherThreadBlocked).isFalse()
  }

  @Test
  fun `destroy ignores a failed deferred eviction and quits`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    item.onUnprepare = {
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }
    worker.schedule(Unit, emptyList())
    dispatcher.runOne()
    policy.evictFailure = IllegalStateException("eviction failed")

    val destroyFailure = runCatching { worker.destroy() }.exceptionOrNull()
    dispatcher.drain()

    assertThat(destroyFailure).isNull()
    assertThat(dispatcher.quitCalled).isTrue()
    assertThat(events.count { event -> event == "evict:0" }).isEqualTo(1)
  }

  @Test
  fun `on demand prepare rejects while a range is resolving`() {
    policy.setItems(0..1)
    var accepted: Boolean? = null
    policy.onResolve = { key ->
      if (key == 1) {
        accepted =
            worker.prepareKey(
                0,
                PreparationSource.PREPARE_KEY,
                false,
                policy::resolveItem,
            )
      }
    }

    worker.schedule(Unit, listOf(1))
    policy.onResolve = null
    dispatcher.drain()

    assertThat(accepted).isFalse()
    assertThat(policy.item(0).prepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `later range schedule invalidates an accepted unconditional request`() {
    policy.setItems(0..0)
    val accepted =
        worker.prepareKey(
            0,
            PreparationSource.PREPARE_KEY,
            false,
            policy::resolveItem,
        )

    worker.schedule(Unit, emptyList())
    dispatcher.drain()

    assertThat(accepted).isTrue()
    assertThat(policy.item(0).prepareCount).isZero()
  }

  @Test
  fun `generation change during key lookup rejects the request`() {
    policy.setItems(0..1)

    val accepted =
        worker.prepareKey(
            0,
            PreparationSource.PREPARE_KEY,
            false,
            itemForKey = { key ->
              worker.schedule(Unit, listOf(1))
              policy.resolveItem(key)
            },
        )
    dispatcher.drain()

    assertThat(accepted).isFalse()
    assertThat(policy.item(0).prepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `conditional on demand prepare rejects reentrant lookup during range resolution`() {
    policy.setItems(0..1)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    var accepted: Boolean? = null
    policy.onResolve = { key ->
      if (key == 1) {
        accepted =
            worker.prepareKey(
                0,
                PreparationSource.PREPARE_KEY_IN_RANGE,
                true,
                policy::resolveItem,
            )
      }
    }

    worker.schedule(Unit, listOf(1))
    policy.onResolve = null
    dispatcher.drain()

    assertThat(accepted).isFalse()
    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(0).unprepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `failed on demand request does not discard the next key request`() {
    policy.setItems(0..1)
    val failure = IllegalStateException("prepare failed")
    policy.item(0).prepareFailure = failure
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    worker.prepareKey(1, PreparationSource.PREPARE_KEY, false, policy::resolveItem)

    assertThat(runCatching { dispatcher.runOne() }.exceptionOrNull()).isSameAs(failure)
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `new schedule invalidates an already dequeued on demand request`() {
    policy.setItems(0..1)
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    assertThat(dispatcher.dequeueOne()).isTrue()

    worker.schedule(Unit, listOf(1))
    assertThat(dispatcher.runDequeued()).isTrue()
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isZero()
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  @Test
  fun `reset compensates an in flight on demand prepare`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    item.onPrepare = { worker.reset() }
    worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)

    dispatcher.runOne()

    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(item.unprepareCount).isEqualTo(1)
    assertThat(events).containsExactly("prepare:0", "unprepare:0", "evict:0")
    assertThat(dispatcher.queuedCount).isZero()
  }

  @Test
  fun `reset waits for an active prepare before settling deferred eviction`() {
    policy.setItems(0..0)
    val item = policy.item(0)
    worker.schedule(Unit, listOf(0))
    dispatcher.drain()
    events.clear()
    item.onUnprepare = {
      item.onUnprepare = null
      worker.prepareKey(0, PreparationSource.PREPARE_KEY, false, policy::resolveItem)
    }
    item.onPrepare = { worker.reset() }

    worker.schedule(Unit, emptyList())
    dispatcher.drain()

    assertThat(events).containsExactly("unprepare:0", "prepare:0", "unprepare:0", "evict:0")
    assertThat(item.prepareCount).isEqualTo(2)
    assertThat(item.unprepareCount).isEqualTo(2)
  }

  @Test
  fun `provider lookup occurs while resolving the range not while dispatching`() {
    policy.setItems(0..2)

    worker.schedule(Unit, listOf(0, 1, 2))

    assertThat(policy.lookupKeys).containsExactly(0, 1, 2)
    assertThat(policy.lookupDuringDispatch).isFalse()
    dispatcher.drain()
    assertThat(policy.lookupDuringDispatch).isFalse()
  }

  @Test
  fun `observer failure cannot wedge reconciliation after commit`() {
    policy.setItems(0..1)
    worker =
        createWorker(
            observer =
                object : NoOpPreparationHealthObserver() {
                  override fun onItemPrepared(key: Any, source: PreparationSource) {
                    throw AssertionError("observer failed")
                  }
                },
        )

    worker.schedule(Unit, listOf(0, 1))
    dispatcher.drain()

    assertThat(policy.item(0).prepareCount).isEqualTo(1)
    assertThat(policy.item(1).prepareCount).isEqualTo(1)
  }

  private fun createWorker(
      observer: PreparationHealthObserver? = null,
  ): PreparedStateRangeWorker<Unit, List<Int>, Int> = PreparedStateRangeWorker(
      adapter = policy,
      scope = PrepareScope(),
      dispatcher = dispatcher,
      observer = observer,
  )

  private class TestRangeAdapter(
      private val dispatcher: FakeRangeWorkDispatcher,
      private val events: MutableList<String>,
  ) : RangeAdapter<Unit, List<Int>, Int> {
    private val items = mutableMapOf<Int, TestItem>()

    val lookupKeys = mutableListOf<Int>()
    var lookupDuringDispatch = false
      private set

    var evictFailure: RuntimeException? = null
    var onEvict: (() -> Unit)? = null
    var onNotifyRemoved: (() -> Unit)? = null
    var onResolve: ((Int) -> Unit)? = null
    var createMissingItems = false
    var removeItemsOnEvict = false
    var retainWithoutCache = false

    fun setItems(keys: IntRange) {
      keys.forEach { key -> items[key] = TestItem(key, events) }
    }

    fun item(key: Int): TestItem = items.getValue(key)

    fun resolveItem(key: Int): TestItem {
      lookupKeys.add(key)
      lookupDuringDispatch = lookupDuringDispatch || dispatcher.isRunningOnCurrentThread
      onResolve?.invoke(key)
      items[key]?.let { item ->
        return item
      }
      check(createMissingItems) { "Missing test item for key $key" }
      return TestItem(key, events).also { item -> items[key] = item }
    }

    fun replace(key: Int, item: TestItem) {
      items[key] = item
    }

    override fun resolve(position: Unit, range: List<Int>): ResolvedRange<Int> =
        ResolvedRange.fromKeys(
            orderedKeys = range,
            releaseComparator = compareBy { it },
            itemForKey = ::resolveItem,
        )

    override var supportsExactEviction: Boolean = true

    override fun isCachedSame(key: Int, item: PreparableCollectionItem): Boolean =
        retainWithoutCache || items[key] === item

    override fun claimReleaseIfSame(key: Int, item: PreparableCollectionItem): Boolean {
      if (retainWithoutCache || !removeItemsOnEvict) {
        return true
      }
      if (items[key] !== item) {
        return false
      }
      evictIfSame(key, item)
      return true
    }

    override fun evictIfSame(key: Int, item: PreparableCollectionItem): Boolean {
      onEvict?.invoke()
      if (removeItemsOnEvict && items[key] !== item) {
        return false
      }
      events.add("evict:$key")
      evictFailure?.let { failure -> throw failure }
      if (removeItemsOnEvict) {
        items.remove(key)
      }
      return true
    }

    override fun notifyRemoved(key: Int, item: PreparableCollectionItem) {
      onNotifyRemoved?.invoke()
    }
  }

  private class TestItem(
      private val key: Int,
      private val events: MutableList<String>,
  ) : PreparableCollectionItem {
    var prepareCount = 0
    var unprepareCount = 0
    var prepareFailure: RuntimeException? = null
    var unprepareFailure: RuntimeException? = null
    var onPrepare: (() -> Unit)? = null
    var onUnprepare: (() -> Unit)? = null

    override fun PrepareScope.prepare() {
      prepareCount++
      events.add("prepare:$key")
      onPrepare?.invoke()
      prepareFailure?.let { throw it }
    }

    override fun PrepareScope.unprepare() {
      unprepareCount++
      events.add("unprepare:$key")
      onUnprepare?.invoke()
      unprepareFailure?.let { failure -> throw failure }
    }
  }

  private class SelectionCounters {
    var membershipChecks = 0
    var comparatorCalls = 0

    fun reset() {
      membershipChecks = 0
      comparatorCalls = 0
    }
  }

  private class CountingKey(
      val value: Int,
      private val counters: SelectionCounters,
  ) {
    override fun equals(other: Any?): Boolean = other is CountingKey && value == other.value

    override fun hashCode(): Int {
      counters.membershipChecks++
      return value
    }
  }

  private class CountingItem : PreparableCollectionItem {
    var prepareCount = 0
    var unprepareCount = 0

    override fun PrepareScope.prepare() {
      prepareCount++
    }

    override fun PrepareScope.unprepare() {
      unprepareCount++
    }
  }

  private class CountingRangeAdapter(
      private val items: Map<CountingKey, CountingItem>,
      private val counters: SelectionCounters,
  ) : RangeAdapter<Unit, List<CountingKey>, CountingKey> {
    override fun resolve(
        position: Unit,
        range: List<CountingKey>,
    ): ResolvedRange<CountingKey> =
        ResolvedRange.fromKeys(
            orderedKeys = range,
            releaseComparator =
                Comparator { left, right ->
                  counters.comparatorCalls++
                  left.value.compareTo(right.value)
                },
            itemForKey = { key -> items.getValue(key) },
        )

    override val supportsExactEviction: Boolean = true

    override fun isCachedSame(key: CountingKey, item: PreparableCollectionItem): Boolean =
        items[key] === item

    override fun claimReleaseIfSame(
        key: CountingKey,
        item: PreparableCollectionItem,
    ): Boolean = items[key] === item

    override fun evictIfSame(key: CountingKey, item: PreparableCollectionItem): Boolean = false
  }

  private open class NoOpPreparationHealthObserver : PreparationHealthObserver {
    override fun onItemRegistered(key: Any): Unit = Unit

    override fun onItemPrepared(key: Any, source: PreparationSource): Unit = Unit

    override fun onItemBound(key: Any, wasPrepared: Boolean): Unit = Unit

    override fun onItemUnprepared(key: Any, reason: UnprepareReason): Unit = Unit

    override fun reportEvent(key: Any, event: PreparationHealthEvent): Unit = Unit

    override fun onFirstScroll(): Unit = Unit
  }

  private companion object {
    private const val AWAIT_SECONDS = 5L
    private const val BLOCK_CHECK_MS = 500L
    private const val LARGE_PLAN_SIZE = 256
    private const val MAX_MEMBERSHIP_CHECKS_PER_KEY = 20
    private const val MAX_COMPARATOR_CALLS_PER_KEY = 32
  }
}
