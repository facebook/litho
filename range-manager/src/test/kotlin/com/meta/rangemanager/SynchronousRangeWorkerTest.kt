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

import com.meta.systrace.NoOpSystracer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class SynchronousRangeWorkerTest {

  private lateinit var worker: SynchronousRangeWorker<TestPosition, IntRange>
  private lateinit var workQueueBuilder: TestWorkQueueBuilder
  private lateinit var itemProvider: TestCollectionItemProvider
  private lateinit var scope: TestPrepareScope
  private val tracer = NoOpSystracer()

  @Before
  fun setup() {
    itemProvider = TestCollectionItemProvider()
    workQueueBuilder = TestWorkQueueBuilder(itemProvider)
    scope = TestPrepareScope()
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer)
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `schedules work by building a queue from position and range`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    worker.schedule(TestPosition(2), 0..4)

    // All items should be prepared
    items.forEach { item ->
      assertThat(item.prepareCount).isEqualTo(1)
      assertThat(item.unprepareCount).isEqualTo(0)
    }
  }

  @Test
  fun `schedule executes work synchronously`() {
    val items = (0..2).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // schedule should block until all work is complete
    worker.schedule(TestPosition(1), 0..2)

    // Immediately after schedule returns, all work should be done
    assertThat(items.map { it.prepareCount }).containsExactly(1, 1, 1)
  }

  @Test
  fun `schedule uses previousRange for diff calculation`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First schedule: range 0-4
    worker.schedule(TestPosition(2), 0..4)

    // Verify items 0-4 are prepared, items 5-9 are not
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }
    for (i in 5..9) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }

    // Clear the builder's calls history
    workQueueBuilder.clearCalls()

    // Second schedule: range 5-9 (no overlap - items 0-4 should be unprepared)
    worker.schedule(TestPosition(7), 5..9)

    // Verify buildWorkQueue was called with previousRange=0..4, newRange=5..9
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
    assertThat(call.newRange).isEqualTo(5..9)

    // Verify items 0-4 are now UNPREPARED (were in previousRange, not in newRange)
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared in first schedule
      assertThat(items[i].unprepareCount).isEqualTo(1) // Unprepared in second schedule
    }

    // Verify items 5-9 are now prepared
    for (i in 5..9) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared in second schedule
      assertThat(items[i].unprepareCount).isEqualTo(0) // Never unprepared
    }
  }

  @Test
  fun `first schedule uses newRange as previousRange`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    worker.schedule(TestPosition(2), 0..4)

    // On first call, previousRange should equal newRange (since currentRange is null)
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
    assertThat(call.newRange).isEqualTo(0..4)
  }

  @Test
  fun `cancel stops work execution midway`() {
    // Create many items with slow preparation
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Set up a callback to cancel after a few items
    val cancelAfter = 10
    items[cancelAfter].onPrepare = { worker.cancel() }

    worker.schedule(TestPosition(50), 0..99)

    // Verify exactly which items were prepared
    // Items 0 through cancelAfter should be prepared (cancel happens during item[cancelAfter])
    for (i in 0..cancelAfter) {
      assertThat(items[i].prepareCount).withFailMessage("Item $i should be prepared").isEqualTo(1)
    }

    // Items after cancelAfter should NOT be prepared
    for (i in (cancelAfter + 1)..99) {
      assertThat(items[i].prepareCount)
          .withFailMessage("Item $i should NOT be prepared")
          .isEqualTo(0)
    }

    // Verify currentRange was NOT updated
    workQueueBuilder.clearCalls()
    worker.schedule(TestPosition(50), 0..99)

    // Should use the default previousRange (same as newRange since no previous complete)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..99)
  }

  @Test
  fun `cancel during schedule does not update currentRange`() {
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First schedule completes successfully
    worker.schedule(TestPosition(2), 0..4)

    // Second schedule gets cancelled midway - cancel when preparing item at index 7 (which is at
    // position 2 in range 5-9)
    items[7].onPrepare = { worker.cancel() }
    worker.schedule(TestPosition(7), 5..9)

    // Clear calls history and cancel callback
    workQueueBuilder.clearCalls()
    items[7].onPrepare = null

    // Third schedule should use previousRange=0..4 (from first schedule, not second)
    worker.schedule(TestPosition(12), 10..14)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4) // Not 5..9!
  }

  @Test
  fun `WITHOUT FIX data would be lost - demonstrates the bug`() {
    // This test demonstrates EXACTLY what data loss would occur without the fix
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Step 1: Schedule range 0-4, completes successfully
    // Result: Items 0-4 are PREPARED
    worker.schedule(TestPosition(2), 0..4)

    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }

    // Step 2: Schedule range 8-12, but CANCEL at item 10
    // WITHOUT FIX: currentRange would be set to 8-12 (wrong!)
    // WITH FIX: currentRange remains 0-4 (correct!)
    items[10].onPrepare = { worker.cancel() }
    worker.schedule(TestPosition(10), 8..12)

    // Actual state after cancel:
    // - Items 0-4 were UNPREPARED (when switching from 0-4 to 8-12)
    // - Items 8-10 were PREPARED (up to cancel point)
    // - Items 11-12 were NOT prepared (after cancel)
    for (i in 0..4) {
      assertThat(items[i].unprepareCount).isEqualTo(1) // Unprepared
    }
    for (i in 8..10) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared up to cancel
    }
    for (i in 11..12) {
      assertThat(items[i].prepareCount).isEqualTo(0) // Not prepared (after cancel)
    }

    items[10].onPrepare = null
    workQueueBuilder.clearCalls()

    // Step 3: Schedule range 0-4 again
    // WITHOUT FIX: Would diff from 8-12 → 0-4
    //   - Would unprepare items 8-12 (but 11-12 were never prepared!)
    //   - Would prepare items 0-4 (correct, but they were unprepared in step 2!)
    // WITH FIX: Diffs from 0-4 → 0-4
    //   - No diff! But wait - items 0-4 were actually unprepared in step 2!
    //   - So they should be RE-PREPARED

    worker.schedule(TestPosition(2), 0..4)

    // WITH FIX: previousRange=0..4 (last completed), newRange=0..4
    // Since ranges are the same, items 0-4 are RE-PREPARED (which is correct!)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..4)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].newRange).isEqualTo(0..4)

    // Items 0-4 should be prepared AGAIN (since they were unprepared in step 2)
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(2) // Prepared twice!
    }

    // WITHOUT FIX: If currentRange was incorrectly set to 8-12 in step 2:
    // - previousRange would be 8-12, newRange would be 0-4
    // - Items 8-10 would be unprepared (correct - they were prepared)
    // - Items 11-12 would be unprepared (BUG! They were never prepared!)
    // - Items 0-4 would be prepared (correct - they need to be prepared)
    // But the state tracking would be WRONG going forward!
  }

  @Test
  fun `complete schedule updates currentRange`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule completes successfully
    worker.schedule(TestPosition(2), 0..4)

    // Clear calls history
    workQueueBuilder.clearCalls()

    // Next schedule should use previousRange=0..4
    worker.schedule(TestPosition(7), 5..9)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
  }

  @Test
  fun `cancel then schedule preserves last completed range`() {
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule 1: completes successfully, range 0-4
    worker.schedule(TestPosition(2), 0..4)
    assertThat(items.take(5).map { it.prepareCount }).containsExactly(1, 1, 1, 1, 1)

    // Schedule 2: cancelled midway, range 5-9
    items[7].onPrepare = { worker.cancel() }
    worker.schedule(TestPosition(7), 5..9)

    // Verify items 5-7 were prepared (up to cancel point), items 8-9 were NOT
    for (i in 5..7) {
      assertThat(items[i].prepareCount).isEqualTo(1)
    }
    for (i in 8..9) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }

    // Clear callback
    items[7].onPrepare = null
    workQueueBuilder.clearCalls()

    // Schedule 3: range 10-14
    worker.schedule(TestPosition(12), 10..14)

    // Should diff from 0..4 (last completed), not 5..9 (cancelled)
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
    assertThat(call.newRange).isEqualTo(10..14)

    // Verify items 10-14 are prepared
    for (i in 10..14) {
      assertThat(items[i].prepareCount).isEqualTo(1)
    }
  }

  @Test
  fun `multiple schedules with cancellations maintain correct state`() {
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule 1: completes, range 0-4
    worker.schedule(TestPosition(2), 0..4)

    // Verify items 0-4 are prepared
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }

    // Schedule 2: cancelled at item 7, range 5-9
    items[7].onPrepare = { worker.cancel() }
    worker.schedule(TestPosition(7), 5..9)

    // Verify items 0-4 were unprepared (not in new range), items 5-7 prepared, items 8-9 not
    // prepared
    for (i in 0..4) {
      assertThat(items[i].unprepareCount).isEqualTo(1) // Unprepared when switching to 5-9
    }
    for (i in 5..7) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared up to cancel point
    }
    for (i in 8..9) {
      assertThat(items[i].prepareCount).isEqualTo(0) // Not prepared (after cancel)
    }

    // Schedule 3: completes, range 10-14
    // Clear cancel callbacks
    items.forEach { it.onPrepare = null }
    workQueueBuilder.clearCalls()
    worker.schedule(TestPosition(12), 10..14)

    // Should diff from 0..4 (not 5..9), since 5..9 was cancelled
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..4)

    // Verify items 10-14 are prepared
    for (i in 10..14) {
      assertThat(items[i].prepareCount).isEqualTo(1)
    }

    // Schedule 4: range 15-19
    workQueueBuilder.clearCalls()
    worker.schedule(TestPosition(17), 15..19)

    // Should diff from 10..14, since that completed successfully
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(10..14)

    // Verify items 10-14 are unprepared (not in new range), items 15-19 are prepared
    for (i in 10..14) {
      assertThat(items[i].unprepareCount).isEqualTo(1) // Unprepared when switching to 15-19
    }
    for (i in 15..19) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared in fourth schedule
    }
  }

  @Test
  fun `cancel from callback interrupts remaining work`() {
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Cancel after preparing item 10
    items[10].onPrepare = { worker.cancel() }

    worker.schedule(TestPosition(50), 0..99)

    // Verify items 0-10 were prepared (inclusive, since cancel happens during item 10's prepare)
    for (i in 0..10) {
      assertThat(items[i].prepareCount).isEqualTo(1)
    }

    // Verify items 11-99 were NOT prepared
    for (i in 11..99) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }

    // Verify currentRange was NOT updated (since work was cancelled)
    workQueueBuilder.clearCalls()
    worker.schedule(TestPosition(50), 0..99)

    // Should use default previousRange (newRange, since no completed range exists)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..99)
  }

  @Test
  fun `destroy clears current range state`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule and complete work
    worker.schedule(TestPosition(2), 0..4)

    // Destroy worker
    worker.destroy()

    // Create new worker with same builder
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer)

    workQueueBuilder.clearCalls()

    // New worker should treat this as first schedule (previousRange = newRange)
    worker.schedule(TestPosition(7), 5..9)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(5..9)
    assertThat(call.newRange).isEqualTo(5..9)
  }

  @Test
  fun `unprepare work executes correctly`() {
    val items = (0..4).map { TestPreparableItem(it) }
    // Set up builder to create unprepare work
    itemProvider.setItems(items)
    workQueueBuilder.forceUnprepareWork = true

    worker.schedule(TestPosition(2), 0..4)

    // All items should be unprepared (not prepared, since we forced unprepare work)
    items.forEach { item ->
      assertThat(item.prepareCount).isEqualTo(0)
      assertThat(item.unprepareCount).isEqualTo(1)
    }
  }

  @Test
  fun `empty work queue executes without error`() {
    itemProvider.setItems(emptyList())

    // Should not throw
    worker.schedule(TestPosition(0), 0..0)
  }

  @Test
  fun `work executes in queue order`() {
    val items = (0..4).map { TestPreparableItem(it) }
    val executionOrder = mutableListOf<Int>()

    items.forEach { item -> item.onPrepare = { executionOrder.add(item.index) } }

    itemProvider.setItems(items)

    worker.schedule(TestPosition(2), 0..4)

    // Items should be prepared in the order provided by the queue builder
    assertThat(executionOrder).containsExactly(0, 1, 2, 3, 4)
  }

  @Test
  fun `cancel called multiple times is safe`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    items[3].onPrepare = {
      worker.cancel()
      worker.cancel() // Second cancel should be safe
      worker.cancel() // Third cancel should be safe
    }

    worker.schedule(TestPosition(5), 0..9)

    // Should have stopped at item 3
    for (i in 0..3) {
      assertThat(items[i].prepareCount).isEqualTo(1)
    }
    for (i in 4..9) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }
  }

  @Test
  fun `cancel before schedule is called has no effect`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Cancel before any schedule
    worker.cancel()

    // Schedule should still work normally
    worker.schedule(TestPosition(2), 0..4)

    // All items should be prepared
    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `overlapping ranges are handled correctly`() {
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First schedule: range 0-9
    worker.schedule(TestPosition(5), 0..9)

    // Verify first batch prepared, nothing unprepared
    for (i in 0..9) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }
    for (i in 10..14) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }

    workQueueBuilder.clearCalls()

    // Second schedule: overlapping range 5-14
    // Items 0-4 should be unprepared (not in new range)
    // Items 5-9 should be re-prepared (in both ranges)
    // Items 10-14 should be prepared (new in range)
    worker.schedule(TestPosition(10), 5..14)

    // Verify buildWorkQueue was called with correct previousRange
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..9)
    assertThat(call.newRange).isEqualTo(5..14)

    // Verify items 0-4 are now unprepared (were in previousRange, not in newRange)
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared in first schedule
      assertThat(items[i].unprepareCount).isEqualTo(1) // Unprepared in second schedule
    }

    // Verify items 5-9 are re-prepared (in both ranges, so prepared twice)
    for (i in 5..9) {
      assertThat(items[i].prepareCount).isEqualTo(2) // Prepared in both schedules
      assertThat(items[i].unprepareCount).isEqualTo(0) // Never unprepared
    }

    // Verify items 10-14 are prepared (new items in range)
    for (i in 10..14) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Prepared only in second schedule
      assertThat(items[i].unprepareCount).isEqualTo(0) // Never unprepared
    }
  }

  @Test
  fun `cancel during overlapping range preserves correct state`() {
    val items = (0..19).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule 1: completes successfully, range 0-9
    worker.schedule(TestPosition(5), 0..9)

    // Verify items 0-9 are prepared
    for (i in 0..9) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }

    // Schedule 2: cancelled during overlapping range 5-14
    // When switching to 5-14, items 0-4 would be unprepared first, then 5-14 prepared
    // Cancel happens at item 12
    items[12].onPrepare = { worker.cancel() }
    worker.schedule(TestPosition(10), 5..14)

    // Verify items 0-4 were unprepared (not in new range 5-14)
    for (i in 0..4) {
      assertThat(items[i].unprepareCount).isEqualTo(1)
    }

    // Verify items 5-12 were prepared (up to and including cancel point)
    for (i in 5..12) {
      assertThat(items[i].prepareCount).isGreaterThanOrEqualTo(1)
    }

    // Verify items 13-14 were NOT prepared (after cancel point)
    for (i in 13..14) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }

    items[12].onPrepare = null
    workQueueBuilder.clearCalls()

    // Schedule 3: range 10-19
    worker.schedule(TestPosition(15), 10..19)

    // Should diff from 0..9 (last completed), not 5..14 (cancelled)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..9)

    // Verify items 10-19 are prepared
    for (i in 10..19) {
      assertThat(items[i].prepareCount).isGreaterThanOrEqualTo(1)
    }
  }

  @Test
  fun `reset clears currentRange so next schedule treats it as first`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule completes successfully, range 0-4
    worker.schedule(TestPosition(2), 0..4)

    // Reset the worker
    worker.reset()
    workQueueBuilder.clearCalls()

    // Next schedule should treat this as first schedule (previousRange = newRange)
    worker.schedule(TestPosition(7), 5..9)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(5..9)
    assertThat(call.newRange).isEqualTo(5..9)
  }

  @Test
  fun `reset cancels ongoing work`() {
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Set up a callback to reset after a few items
    val resetAfter = 10
    items[resetAfter].onPrepare = { worker.reset() }

    worker.schedule(TestPosition(50), 0..99)

    // Verify items after resetAfter were NOT prepared
    for (i in (resetAfter + 1)..99) {
      assertThat(items[i].prepareCount)
          .withFailMessage("Item $i should NOT be prepared")
          .isEqualTo(0)
    }

    // Verify currentRange was cleared (next schedule is treated as first)
    workQueueBuilder.clearCalls()
    items[resetAfter].onPrepare = null
    worker.schedule(TestPosition(50), 0..99)

    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..99)
  }

  @Test
  fun `reset after completed schedule clears range state`() {
    val items = (0..14).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Schedule 1: completes, range 0-4
    worker.schedule(TestPosition(2), 0..4)

    // Schedule 2: completes, range 5-9
    worker.schedule(TestPosition(7), 5..9)

    // Reset
    worker.reset()
    workQueueBuilder.clearCalls()

    // Schedule 3: should treat as first schedule, not diff from 5..9
    worker.schedule(TestPosition(12), 10..14)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(10..14)
    assertThat(call.newRange).isEqualTo(10..14)
  }

  @Test
  fun `reset is safe to call multiple times`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    worker.schedule(TestPosition(2), 0..4)

    // Multiple resets should not throw
    worker.reset()
    worker.reset()
    worker.reset()

    workQueueBuilder.clearCalls()

    // Worker should still be usable
    worker.schedule(TestPosition(2), 0..4)

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..4)
  }

  @Test
  fun `reset before any schedule is safe`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Reset before any schedule
    worker.reset()

    // Schedule should work normally
    worker.schedule(TestPosition(2), 0..4)

    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `empty range unprepares previously prepared items`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // First schedule with non-empty range - items should be prepared
    worker.schedule(TestPosition(2), 0..4)

    // Verify all items are prepared
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1)
      assertThat(items[i].unprepareCount).isEqualTo(0)
    }

    // Schedule with empty range - should unprepare all previously prepared items
    // Configure builder to unprepare items from previous range when newRange is empty
    workQueueBuilder.unprepareOnEmptyRange = true
    worker.schedule(TestPosition(0), IntRange.EMPTY)

    // Verify all items are now unprepared
    for (i in 0..4) {
      assertThat(items[i].prepareCount).isEqualTo(1) // Still 1 from before
      assertThat(items[i].unprepareCount).isEqualTo(1) // Now unprepared
    }

    // Verify currentRange is updated to empty (work completed successfully)
    workQueueBuilder.clearCalls()
    workQueueBuilder.unprepareOnEmptyRange = false

    // Next schedule should use empty range as previousRange
    worker.schedule(TestPosition(2), 0..4)

    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(IntRange.EMPTY)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].newRange).isEqualTo(0..4)
  }
}

/** Test data classes and helpers */
data class TestPosition(val value: Int)

data class BuildWorkQueueCall(
    val previousRange: IntRange,
    val newPosition: TestPosition,
    val newRange: IntRange,
)

class TestWorkQueueBuilder(private val itemProvider: TestCollectionItemProvider) :
    WorkQueueBuilder<TestPosition, IntRange> {
  var forceUnprepareWork: Boolean = false
  var unprepareOnEmptyRange: Boolean = false
  val buildWorkQueueCalls = mutableListOf<BuildWorkQueueCall>()

  fun clearCalls() {
    buildWorkQueueCalls.clear()
  }

  override fun buildWorkQueue(
      previousRange: IntRange,
      newPosition: TestPosition,
      newRange: IntRange,
  ): List<ItemWork> {
    buildWorkQueueCalls.add(BuildWorkQueueCall(previousRange, newPosition, newRange))

    // Handle empty range - unprepare all items from previous range
    if (newRange.isEmpty() && unprepareOnEmptyRange) {
      return buildList {
        for (index in previousRange) {
          if (index < itemProvider.size) {
            val item = itemProvider[index]
            add(
                ItemWork(
                    key = index,
                    item = item,
                    type = WorkType.Unprepare,
                    execute = { scope, workItem, _ -> with(workItem) { scope.unprepare() } },
                ),
            )
          }
        }
      }
    }

    // Calculate diff between previousRange and newRange (realistic behavior)
    return buildList {
      // First, unprepare items that are in previousRange but NOT in newRange
      if (!forceUnprepareWork) {
        for (index in previousRange) {
          if (index !in newRange && index < itemProvider.size) {
            val item = itemProvider[index]
            add(
                ItemWork(
                    key = index,
                    item = item,
                    type = WorkType.Unprepare,
                    execute = { scope, workItem, _ -> with(workItem) { scope.unprepare() } },
                ),
            )
          }
        }
      }

      // Then, prepare items in newRange
      for (index in newRange) {
        if (index < itemProvider.size) {
          val item = itemProvider[index]
          val workType = if (forceUnprepareWork) WorkType.Unprepare else WorkType.Prepare
          add(
              ItemWork(
                  key = index,
                  item = item,
                  type = workType,
                  execute = { scope, workItem, _ ->
                    if (workType == WorkType.Prepare) {
                      with(workItem) { scope.prepare() }
                    } else {
                      with(workItem) { scope.unprepare() }
                    }
                  },
              ),
          )
        }
      }
    }
  }
}

class TestPreparableItem(val index: Int) : PreparableCollectionItem {
  var prepareCount: Int = 0
  var unprepareCount: Int = 0
  var onPrepare: (() -> Unit)? = null
  var onUnprepare: (() -> Unit)? = null

  override fun PrepareScope.prepare() {
    prepareCount++
    onPrepare?.invoke()
  }

  override fun PrepareScope.unprepare() {
    unprepareCount++
    onUnprepare?.invoke()
  }
}

class TestPrepareScope : PrepareScope()

class TestCollectionItemProvider : CollectionItemProvider<Int>() {
  private var items: List<TestPreparableItem> = emptyList()

  val size: Int
    get() = items.size

  fun setItems(items: List<TestPreparableItem>) {
    this.items = items
    invalidateAll()
  }

  override fun create(key: Int): PreparableCollectionItem {
    return if (key < items.size) items[key]
    else
        throw IndexOutOfBoundsException(
            "Index $key is out of bounds for items list of size ${items.size}",
        )
  }

  override fun contains(key: Int): Boolean {
    return key in items.indices
  }
}
