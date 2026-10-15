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

package com.meta.rangemanager.list

import com.meta.rangemanager.FakeRangeWorkDispatcher
import com.meta.rangemanager.NoCache
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.SynchronizedItemCache
import com.meta.systrace.NoOpSystracer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class AsynchronousListRangeWorkerTest {

  private lateinit var worker: AsynchronousListRangeWorker
  private lateinit var itemProvider: TestCollectionItemProvider
  private lateinit var scope: TestPrepareScope
  private lateinit var dispatcher: FakeRangeWorkDispatcher
  private val tracer = NoOpSystracer()

  @Before
  fun setup() {
    itemProvider = TestCollectionItemProvider()
    scope = TestPrepareScope()
    dispatcher = FakeRangeWorkDispatcher()
    worker = AsynchronousListRangeWorker(itemProvider, scope, tracer, dispatcher)
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `schedule prepares every item in range`() {
    // Create items for indices 0-9
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Position is at 5-7, range is 0-9
    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 7)

    worker.schedule(position, 0..9)

    dispatcher.drain()

    // All items should be prepared
    items.forEach { item ->
      assertThat(item.prepareCount).isEqualTo(1)
      assertThat(item.unprepareCount).isEqualTo(0)
    }
  }

  @Test
  fun `rescheduling a fully prepared range only unprepares completed work from partial range`() {
    val rangeA = 0..1
    val rangeB = 2..6
    val items = (rangeA.first..rangeB.last).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    worker.schedule(
        ListPosition(
            currentFirstVisible = rangeA.first,
            currentLastVisible = rangeA.first,
        ),
        rangeA,
    )
    dispatcher.drain()

    val prepareStarted = CountDownLatch(1)
    val releasePrepare = CountDownLatch(1)
    items[4].onPrepare = {
      prepareStarted.countDown()
      releasePrepare.await()
    }
    worker.schedule(
        ListPosition(
            currentFirstVisible = rangeB.first,
            currentLastVisible = rangeB.first,
        ),
        rangeB,
    )
    dispatcher.runOne()
    dispatcher.runOne()
    val workerThread = Thread { dispatcher.runOne() }.apply { start() }
    try {
      assertThat(prepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      worker.schedule(
          ListPosition(
              currentFirstVisible = rangeA.first,
              currentLastVisible = rangeA.first,
          ),
          rangeA,
      )
    } finally {
      releasePrepare.countDown()
      workerThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
    }
    assertThat(workerThread.isAlive).isFalse()
    dispatcher.drain()

    rangeA.forEach { index ->
      assertThat(items[index].prepareCount).describedAs("A[$index] prepare count").isEqualTo(1)
      assertThat(items[index].unprepareCount).describedAs("A[$index] unprepare count").isEqualTo(0)
    }
    (2..4).forEach { index ->
      assertThat(items[index].prepareCount).describedAs("B[$index] prepare count").isEqualTo(1)
      assertThat(items[index].unprepareCount).describedAs("B[$index] unprepare count").isEqualTo(1)
    }
    (5..6).forEach { index ->
      assertThat(items[index].prepareCount).describedAs("B[$index] prepare count").isEqualTo(0)
      assertThat(items[index].unprepareCount).describedAs("B[$index] unprepare count").isEqualTo(0)
    }
  }

  @Test
  fun `schedule prepares in viewport priority order`() {
    // Create items for indices 0-9
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Track the execution order
    val preparationOrder = mutableListOf<Int>()
    items.forEach { item -> item.onPrepare = { preparationOrder.add(item.index) } }

    // Position is at 5-5, range is 0-9
    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)

    worker.schedule(position, 0..9)

    dispatcher.drain()

    // Items should be prepared starting from first visible, moving ahead first then behind
    // Expected order based on code: 5, 6, 7, 8, 9, 4, 3, 2, 1, 0
    assertThat(preparationOrder).hasSize(10)
    assertThat(preparationOrder[0]).isEqualTo(5) // Start at first visible
    // Verify ahead items come first
    assertThat(preparationOrder[1]).isEqualTo(6) // Ahead
    assertThat(preparationOrder[2]).isEqualTo(7) // Ahead
    assertThat(preparationOrder[3]).isEqualTo(8) // Ahead
    assertThat(preparationOrder[4]).isEqualTo(9) // Ahead (end of ahead)
    // Then behind items
    assertThat(preparationOrder[5]).isEqualTo(4) // Behind
  }

  @Test
  fun `new schedule releases items outside its range`() {
    // Create items for indices 0-19
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First, prepare range 0-9
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)
    dispatcher.drain()

    // Verify all items in first range are prepared
    items.take(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }

    // Now change to range 10-19
    val position2 = ListPosition(currentFirstVisible = 15, currentLastVisible = 15)
    worker.schedule(position2, 10..19)
    dispatcher.drain()

    // Items 0-9 should be unprepared
    items.take(10).forEach { item -> assertThat(item.unprepareCount).isEqualTo(1) }

    // Items 10-19 should be prepared
    items.drop(10).forEach { item ->
      assertThat(item.prepareCount).isEqualTo(1)
      assertThat(item.unprepareCount).isEqualTo(0)
    }
  }

  @Test
  fun `overlapping schedule only prepares missing items`() {
    // Create items for indices 0-14
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First, prepare range 0-9 starting from position 5
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)
    dispatcher.drain()

    // Verify first range prepared - all items should be prepared exactly once
    items.take(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }

    // Now expand to range 0-14 (adds 10-14), starting from position 7.
    val position2 = ListPosition(currentFirstVisible = 7, currentLastVisible = 7)
    worker.schedule(position2, 0..14)
    dispatcher.drain()

    // Overlap remains represented in the prepared ledger and is not prepared again.
    items.take(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
    // Items 10-14 should be prepared for the first time
    items.drop(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
    // No items should be unprepared (range only expanded)
    items.forEach { item -> assertThat(item.unprepareCount).isEqualTo(0) }
  }

  @Test
  fun `shifted schedule releases old and prepares new items`() {
    // Create items for indices 0-19
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First, prepare range 0-9
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)
    dispatcher.drain()

    // Now shift to range 5-14 (unprepare 0-4, prepare 10-14, keep 5-9)
    val position2 = ListPosition(currentFirstVisible = 10, currentLastVisible = 10)
    worker.schedule(position2, 5..14)
    dispatcher.drain()

    // Items 0-4 should be unprepared
    items.take(5).forEach { item -> assertThat(item.unprepareCount).isEqualTo(1) }

    // Items 5-9 should not be unprepared
    items.subList(5, 10).forEach { item -> assertThat(item.unprepareCount).isEqualTo(0) }

    // Items 10-14 should be prepared
    items.subList(10, 15).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `cancel stops pending preparation`() {
    // Create many items to ensure there's pending work
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val position = ListPosition(currentFirstVisible = 50, currentLastVisible = 50)
    worker.schedule(position, 0..99)

    repeat(5) { dispatcher.runOne() }

    // Count how many items are prepared so far
    val preparedBeforeStop = items.count { it.prepareCount > 0 }

    // Stop the worker
    worker.cancel()

    dispatcher.drain()

    // No additional items should be prepared after stop
    val preparedAfterStop = items.count { it.prepareCount > 0 }
    assertThat(preparedAfterStop).isEqualTo(preparedBeforeStop)
  }

  @Test
  fun `new schedule supersedes pending work`() {
    // Create items
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Start first range
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)

    // Immediately start second range before first completes
    val position2 = ListPosition(currentFirstVisible = 15, currentLastVisible = 15)
    worker.schedule(position2, 10..19)

    dispatcher.drain()

    // Items 0-9 should be unprepared (they were in first range but not second)
    items.take(10).forEach { item -> assertThat(item.unprepareCount).isGreaterThanOrEqualTo(0) }

    // Items 10-19 should be prepared
    items.drop(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `destroy stops thread and clears state`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position, 0..9)

    worker.destroy()
    dispatcher.drain()

    assertThat(dispatcher.quitCalled).isTrue()
    assertThat(dispatcher.queuedCount).isZero()
  }

  @Test
  fun `empty range does not prepare any items`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First prepare a range
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)
    dispatcher.drain()

    // Verify all items are prepared
    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }

    // Now use an empty range - the worker should unprepare all items
    val position2 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position2, IntRange.EMPTY)
    dispatcher.drain()

    // All items should be unprepared, no new items should be prepared
    items.forEach { item ->
      assertThat(item.prepareCount).isEqualTo(1) // Still 1 from before
      assertThat(item.unprepareCount).isEqualTo(1) // Now unprepared
    }
  }

  @Test
  fun `single item range prepares only that item`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position, 5..5)

    dispatcher.drain()

    // Only item 5 should be prepared
    items.forEachIndexed { index, item ->
      if (index == 5) {
        assertThat(item.prepareCount).isEqualTo(1)
      } else {
        assertThat(item.prepareCount).isEqualTo(0)
      }
    }
  }

  @Test
  fun `schedule prepares in forward then backward order`() {
    val items = (0..10).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Capture prepare calls
    val prepareOrder = mutableListOf<Int>()
    items.forEach { item -> item.onPrepare = { prepareOrder.add(item.index) } }

    // Position at 5-6, range 3-7
    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 6)
    worker.schedule(position, 3..7)

    dispatcher.drain()

    // Should start from first visible (5), move ahead to end, then move behind
    // Expected: 5, 6, 7, 4, 3
    assertThat(prepareOrder.size).isEqualTo(5)
    assertThat(prepareOrder[0]).isEqualTo(5) // Start at first visible
    assertThat(prepareOrder[1]).isEqualTo(6) // Ahead
    assertThat(prepareOrder[2]).isEqualTo(7) // Ahead (end of range)
    assertThat(prepareOrder[3]).isEqualTo(4) // Behind
    assertThat(prepareOrder[4]).isEqualTo(3) // Behind (start of range)
  }

  @Test
  fun `schedule prepares all ahead items before behind items`() {
    val items = (0..20).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val executionOrder = mutableListOf<Int>()
    items.forEach { item -> item.onPrepare = { executionOrder.add(item.index) } }

    // Position at 10-10, range 5-15
    val position = ListPosition(currentFirstVisible = 10, currentLastVisible = 10)

    worker.schedule(position, 5..15)
    dispatcher.drain()

    // Verify work ordering: starts at first visible, goes ahead to end, then behind to start
    // Pattern: 10, 11, 12, 13, 14, 15, 9, 8, 7, 6, 5
    assertThat(executionOrder.size).isEqualTo(11) // Items 5-15
    assertThat(executionOrder[0]).isEqualTo(10) // Start at first visible
    // All ahead items come first
    assertThat(executionOrder[1]).isEqualTo(11) // Ahead
    assertThat(executionOrder[2]).isEqualTo(12) // Ahead
    assertThat(executionOrder[3]).isEqualTo(13) // Ahead
    assertThat(executionOrder[4]).isEqualTo(14) // Ahead
    assertThat(executionOrder[5]).isEqualTo(15) // Ahead (end of range)
    // Then all behind items
    assertThat(executionOrder[6]).isEqualTo(9) // Behind
  }

  @Test
  fun `reset retains prepared items for the next reconciliation`() {
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Prepare range 0-9
    val position1 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position1, 0..9)
    dispatcher.drain()

    // Verify all items in first range are prepared
    items.take(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }

    // Reset the worker
    worker.reset()

    // Schedule range 0-9 again. Reset clears the stale target but retains exact prepared ownership.
    val position2 = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position2, 0..9)
    dispatcher.drain()

    // The same exact items remain represented, so the replacement target does not prepare again.
    items.take(10).forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
    // The target still contains every prepared item, so reconciliation has nothing to release.
    items.take(10).forEach { item -> assertThat(item.unprepareCount).isEqualTo(0) }
  }

  @Test
  fun `reset cancels ongoing preparation work`() {
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val position = ListPosition(currentFirstVisible = 50, currentLastVisible = 50)
    worker.schedule(position, 0..99)

    repeat(5) { dispatcher.runOne() }

    // Count how many items are prepared so far
    val preparedBeforeReset = items.count { it.prepareCount > 0 }

    // Reset the worker
    worker.reset()

    dispatcher.drain()

    // No additional items should be prepared after reset
    val preparedAfterReset = items.count { it.prepareCount > 0 }
    assertThat(preparedAfterReset).isEqualTo(preparedBeforeReset)
  }

  @Test
  fun `reset is safe to call before any schedule`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Reset before any schedule should not throw
    worker.reset()

    // Worker should still be usable
    val position = ListPosition(currentFirstVisible = 5, currentLastVisible = 5)
    worker.schedule(position, 0..9)
    dispatcher.drain()

    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `superseding same-key plan releases old in-flight item and prepares cached replacement`() {
    val provider = ReplaceableCacheTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    val prepareStarted = CountDownLatch(1)
    val releasePrepare = CountDownLatch(1)
    try {
      oldItem.onPrepare = {
        prepareStarted.countDown()
        releasePrepare.await()
      }
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      val workerThread = Thread { localDispatcher.runOne() }.apply { start() }
      val replacementItem: TestPreparableItem
      try {
        assertThat(prepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        replacementItem = provider.replaceCachedItem()
        localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      } finally {
        releasePrepare.countDown()
        workerThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      }
      assertThat(workerThread.isAlive).isFalse()
      localDispatcher.drain()

      assertThat(oldItem.prepareCount).isEqualTo(1)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(replacementItem.prepareCount).isEqualTo(1)
      assertThat(replacementItem.unprepareCount).isZero()
      assertThat(provider[0]).isSameAs(replacementItem)
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `NoCache later same-key plan releases old item and prepares fresh item`() {
    val provider = NoCacheTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      val oldItem = provider.createdItems.single()

      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      val replacementItem = provider.createdItems.last()

      assertThat(provider.createdItems).containsExactly(oldItem, replacementItem)
      assertThat(oldItem.prepareCount).isEqualTo(1)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(replacementItem.prepareCount).isEqualTo(1)
      assertThat(replacementItem.unprepareCount).isZero()
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `stale unprepare completion cannot evict replacement stored under the same key`() {
    val provider = ReplaceableCacheTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    val unprepareStarted = CountDownLatch(1)
    val releaseUnprepare = CountDownLatch(1)
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      oldItem.onUnprepare = {
        unprepareStarted.countDown()
        releaseUnprepare.await()
      }
      localWorker.schedule(
          ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
          IntRange.EMPTY,
      )
      val workerThread = Thread { localDispatcher.runOne() }.apply { start() }
      val replacementItem: TestPreparableItem
      try {
        assertThat(unprepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        replacementItem = provider.replaceCachedItem()
      } finally {
        releaseUnprepare.countDown()
        workerThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      }
      assertThat(workerThread.isAlive).isFalse()
      localDispatcher.drain()

      assertThat(provider[0]).isSameAs(replacementItem)
      assertThat(provider.createCount).isEqualTo(2)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(replacementItem.unprepareCount).isZero()
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `provider invalidation overlapping prepare completion owns the release`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val item = provider[0] as TestPreparableItem
    val providerUnprepareStarted = CountDownLatch(1)
    val releaseProviderUnprepare = CountDownLatch(1)
    val unprepareCalls = java.util.concurrent.atomic.AtomicInteger()
    val invalidationThread = Thread(
        {
          provider.invalidate(0)
        },
        "provider-invalidate",
    )
    try {
      item.onUnprepare = {
        unprepareCalls.incrementAndGet()
        if (Thread.currentThread() === invalidationThread) {
          providerUnprepareStarted.countDown()
          releaseProviderUnprepare.await()
        }
      }
      item.onPrepare = {
        invalidationThread.start()
        assertThat(providerUnprepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      }

      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.runOne()
    } finally {
      releaseProviderUnprepare.countDown()
      invalidationThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      localWorker.destroy()
      localDispatcher.drain()
    }

    assertThat(invalidationThread.isAlive).isFalse()
    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(unprepareCalls.get()).isEqualTo(1)
  }

  @Test
  fun `provider invalidation before range release owns the settled release`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val item = provider[0] as TestPreparableItem
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      provider.invalidate(0)

      localWorker.schedule(
          ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
          IntRange.EMPTY,
      )
      localDispatcher.drain()

      assertThat(item.prepareCount).isEqualTo(1)
      assertThat(item.unprepareCount).isEqualTo(1)
    } finally {
      localWorker.destroy()
      localDispatcher.drain()
    }
  }

  @Test
  fun `provider invalidation before destroy owns the settled release`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val item = provider[0] as TestPreparableItem

    localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
    localDispatcher.drain()
    provider.invalidate(0)

    localWorker.destroy()
    localDispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(item.unprepareCount).isEqualTo(1)
  }

  @Test
  fun `destroy detaches an already unprepared deferred item before provider invalidation`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val item = provider[0] as TestPreparableItem
    item.onUnprepare = {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
    }

    localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
    localDispatcher.drain()
    localWorker.schedule(
        ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
        IntRange.EMPTY,
    )
    localDispatcher.runOne()

    localWorker.destroy()
    provider.invalidateAll()
    localDispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(1)
    assertThat(item.unprepareCount).isEqualTo(1)
  }

  @Test
  fun `reset during a NoCache release does not replay the release`() {
    val provider = NoCacheTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      val item = provider.createdItems.single()
      item.onUnprepare = { localWorker.reset() }

      localWorker.schedule(
          ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
          IntRange.EMPTY,
      )
      localDispatcher.drain()

      assertThat(item.prepareCount).isEqualTo(1)
      assertThat(item.unprepareCount).isEqualTo(1)
    } finally {
      localWorker.destroy()
      localDispatcher.drain()
    }
  }

  @Test
  fun `reset compensation unprepares and evicts the exact late prepared item`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    try {
      oldItem.onPrepare = { localWorker.reset() }

      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.runOne()

      assertThat(oldItem.prepareCount).isEqualTo(1)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(provider[0]).isNotSameAs(oldItem)
      assertThat(provider.createCount).isEqualTo(2)
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `reset compensation failure still evicts exact late prepared item and rethrows failure`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    val compensationFailure = IllegalStateException("compensation failed")
    try {
      oldItem.onPrepare = { localWorker.reset() }
      oldItem.unprepareFailure = compensationFailure

      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      val thrown = runCatching { localDispatcher.runOne() }.exceptionOrNull()

      assertThat(thrown).isSameAs(compensationFailure)
      assertThat(oldItem.prepareCount).isEqualTo(1)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(provider[0]).isNotSameAs(oldItem)
      assertThat(provider.createCount).isEqualTo(2)
      assertThat(localDispatcher.queuedCount).isZero()
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `destroy compensation unprepares and evicts the exact late prepared item`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    oldItem.onPrepare = { localWorker.destroy() }

    localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
    localDispatcher.runOne()

    assertThat(oldItem.prepareCount).isEqualTo(1)
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(provider[0]).isNotSameAs(oldItem)
    assertThat(provider.createCount).isEqualTo(2)
  }

  @Test
  fun `NoCache unprepares the exact instance that successfully prepared`() {
    val provider = NoCacheTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      val preparedItem = provider.createdItems.single()

      localWorker.schedule(
          ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
          IntRange.EMPTY,
      )
      localDispatcher.drain()

      assertThat(provider.createdItems).containsExactly(preparedItem)
      assertThat(preparedItem.prepareCount).isEqualTo(1)
      assertThat(preparedItem.unprepareCount).isEqualTo(1)
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `reincluded in flight unprepare prepares a new provider owned item`() {
    val provider = RecreatingTestCollectionItemProvider()
    val localDispatcher = FakeRangeWorkDispatcher()
    val localWorker =
        AsynchronousListRangeWorker(provider, TestPrepareScope(), tracer, localDispatcher)
    val oldItem = provider[0] as TestPreparableItem
    val unprepareStarted = CountDownLatch(1)
    val releaseUnprepare = CountDownLatch(1)
    try {
      localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      localDispatcher.drain()
      oldItem.onUnprepare = {
        unprepareStarted.countDown()
        releaseUnprepare.await()
      }
      localWorker.schedule(
          ListPosition(currentFirstVisible = 0, currentLastVisible = 0),
          IntRange.EMPTY,
      )
      val workerThread = Thread { localDispatcher.runOne() }.apply { start() }
      try {
        assertThat(unprepareStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        localWorker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..0)
      } finally {
        releaseUnprepare.countDown()
        workerThread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      }
      assertThat(workerThread.isAlive).isFalse()
      localDispatcher.drain()

      val replacementItem = provider[0] as TestPreparableItem
      assertThat(provider.createCount).isEqualTo(2)
      assertThat(replacementItem).isNotSameAs(oldItem)
      assertThat(oldItem.prepareCount).isEqualTo(1)
      assertThat(oldItem.unprepareCount).isEqualTo(1)
      assertThat(replacementItem.prepareCount).isEqualTo(1)
      assertThat(replacementItem.unprepareCount).isZero()
    } finally {
      localWorker.destroy()
    }
  }

  @Test
  fun `releases above ascending then below descending and empty ascending`() {
    val items = (0..9).map { TestPreparableItem(it) }
    val releaseOrder = mutableListOf<Int>()
    items.forEach { item -> item.onUnprepare = { releaseOrder.add(item.index) } }
    itemProvider.setItems(items)
    worker.schedule(ListPosition(currentFirstVisible = 0, currentLastVisible = 0), 0..9)
    dispatcher.drain()

    worker.schedule(ListPosition(currentFirstVisible = 3, currentLastVisible = 3), 3..6)
    dispatcher.drain()

    assertThat(releaseOrder).containsExactly(7, 8, 9, 2, 1, 0)

    releaseOrder.clear()
    worker.schedule(ListPosition(currentFirstVisible = 3, currentLastVisible = 3), IntRange.EMPTY)
    dispatcher.drain()

    assertThat(releaseOrder).containsExactly(3, 4, 5, 6)
  }

  private companion object {
    private const val AWAIT_SECONDS = 5L
  }
}

private class ReplaceableCacheTestCollectionItemProvider(
    cache: SynchronizedItemCache<Int> = SynchronizedItemCache(),
) : ListCollectionItemProvider(cache) {
  var createCount = 0
    private set

  override val size: Int = 1

  override fun create(key: Int): PreparableCollectionItem =
      TestPreparableItem(index = createCount).also { createCount++ }

  fun replaceCachedItem(): TestPreparableItem {
    invalidate(0)
    return this[0] as TestPreparableItem
  }
}

private class NoCacheTestCollectionItemProvider : ListCollectionItemProvider(NoCache()) {
  val createdItems = mutableListOf<TestPreparableItem>()

  override val size: Int = 1

  override fun create(key: Int): PreparableCollectionItem =
      TestPreparableItem(index = createdItems.size).also { item -> createdItems.add(item) }
}

private class RecreatingTestCollectionItemProvider : ListCollectionItemProvider() {
  var createCount = 0
    private set

  override val size: Int = 1

  override fun create(key: Int): PreparableCollectionItem =
      TestPreparableItem(index = createCount).also { createCount++ }
}

class TestCollectionItemProvider : ListCollectionItemProvider() {
  private var items: List<TestPreparableItem> = emptyList()

  fun setItems(items: List<TestPreparableItem>) {
    this.items = items
    invalidateAll()
  }

  override fun create(key: Int): PreparableCollectionItem {
    return items[key]
  }

  override val size: Int
    get() = items.size
}

class TestPreparableItem(val index: Int) : PreparableCollectionItem {
  var prepareCount: Int = 0
  var unprepareCount: Int = 0
  var unprepareFailure: RuntimeException? = null
  var onPrepare: (() -> Unit)? = null
  var onUnprepare: (() -> Unit)? = null

  override fun PrepareScope.prepare() {
    prepareCount++
    onPrepare?.invoke()
  }

  override fun PrepareScope.unprepare() {
    unprepareCount++
    onUnprepare?.invoke()
    unprepareFailure?.let { failure -> throw failure }
  }
}

class TestPrepareScope : PrepareScope()
