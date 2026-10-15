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

class RangeManagerTest {

  private lateinit var rangeManager: RangeManager<TestPosition, IntRange>
  private lateinit var worker: SynchronousRangeWorker<TestPosition, IntRange>
  private lateinit var workQueueBuilder: TestWorkQueueBuilder
  private lateinit var itemProvider: TestCollectionItemProvider
  private lateinit var calculator: TestRangeCalculator
  private lateinit var scope: TestPrepareScope
  private val tracer = NoOpSystracer()

  @Before
  fun setup() {
    itemProvider = TestCollectionItemProvider()
    workQueueBuilder = TestWorkQueueBuilder(itemProvider)
    scope = TestPrepareScope()
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer)
    calculator = TestRangeCalculator()
    rangeManager = RangeManager(calculator, worker, itemProvider)
  }

  @After
  fun tearDown() {
    rangeManager.destroy()
  }

  @Test
  fun `recalculateRange calculates range and schedules worker`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))

    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }
  }

  @Test
  fun `recalculateRange cancels previous work before scheduling new`() {
    val items = (0..99).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Set up first schedule to cancel when preparing item 10
    calculator.rangeToReturn = 0..99
    items[10].onPrepare = { worker.cancel() }
    rangeManager.recalculateRange(TestPosition(50))

    // Items after 10 should NOT be prepared
    for (i in 11..99) {
      assertThat(items[i].prepareCount).isEqualTo(0)
    }
  }

  @Test
  fun `onDataSetChanged evicts stale items from provider`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    // Prepare all items
    rangeManager.recalculateRange(TestPosition(2))
    items.forEach { item -> assertThat(item.prepareCount).isEqualTo(1) }

    // Access all items through the provider to populate the cache
    for (i in 0..4) {
      itemProvider[i]
    }

    // Remove items 3-4 from the data source
    val reducedItems = items.take(3)
    itemProvider.setItems(reducedItems)
    calculator.rangeToReturn = 0..2

    // onDataSetChanged should evict items 3-4 and recalculate
    rangeManager.onDataSetChanged(TestPosition(1))

    // Items 0-2 should be prepared again
    for (i in 0..2) {
      assertThat(items[i].prepareCount).isGreaterThanOrEqualTo(2)
    }
  }

  @Test
  fun `onDataSetChanged resets worker state before recalculating`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    // First: prepare range 0-4
    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // Now simulate a data set change — items may have been removed/reordered
    // After reset, the worker should not diff against the old range
    calculator.rangeToReturn = 0..4
    rangeManager.onDataSetChanged(TestPosition(2))

    // The worker was reset, so previousRange should equal newRange (fresh start)
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
    assertThat(call.newRange).isEqualTo(0..4)
  }

  @Test
  fun `onDataSetChanged coordinates eviction reset and recalculation in order`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    // Prepare all items and populate cache
    rangeManager.recalculateRange(TestPosition(2))
    for (i in 0..4) {
      itemProvider[i]
    }

    // Remove item 4
    val reducedItems = items.take(4)
    itemProvider.setItems(reducedItems)
    calculator.rangeToReturn = 0..3
    workQueueBuilder.clearCalls()

    rangeManager.onDataSetChanged(TestPosition(2))

    // Worker should have been reset (previousRange = newRange since currentRange was cleared)
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..3)
    assertThat(call.newRange).isEqualTo(0..3)

    // Items 0-3 should be re-prepared
    for (i in 0..3) {
      assertThat(items[i].prepareCount).isGreaterThanOrEqualTo(2)
    }
  }

  @Test
  fun `onDataSetChanged prevents stale range diff after item removal`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..9

    // Prepare range 0-9
    rangeManager.recalculateRange(TestPosition(5))
    for (i in 0..9) {
      itemProvider[i]
    }

    // Remove items 5-9 from data source
    val reducedItems = items.take(5)
    itemProvider.setItems(reducedItems)
    calculator.rangeToReturn = 0..4
    workQueueBuilder.clearCalls()

    // Without onDataSetChanged, the worker would diff from 0..9 → 0..4
    // and try to unprepare items 5-9 through the provider, which would fail
    // because those items no longer exist.
    // With onDataSetChanged, the worker is reset and starts fresh.
    rangeManager.onDataSetChanged(TestPosition(2))

    // Should be a fresh start (no diff against stale 0..9 range)
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    val call = workQueueBuilder.buildWorkQueueCalls[0]
    assertThat(call.previousRange).isEqualTo(0..4)
    assertThat(call.newRange).isEqualTo(0..4)
  }

  @Test
  fun `destroy cancels worker and cleans up`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    rangeManager.destroy()

    // Create a new worker and manager to verify the old one was destroyed
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer)
    rangeManager = RangeManager(calculator, worker, itemProvider)
    workQueueBuilder.clearCalls()

    rangeManager.recalculateRange(TestPosition(2))

    // New worker should treat this as first schedule
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
    assertThat(workQueueBuilder.buildWorkQueueCalls[0].previousRange).isEqualTo(0..4)
  }

  @Test
  fun `recalculateRange skips work when range unchanged`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // Same position, same range → worker should NOT be scheduled again
    rangeManager.recalculateRange(TestPosition(2))

    assertThat(workQueueBuilder.buildWorkQueueCalls).isEmpty()
  }

  @Test
  fun `recalculateRange recalculates when range changes`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // Different range → worker should be scheduled
    calculator.rangeToReturn = 3..7
    rangeManager.recalculateRange(TestPosition(5))

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
  }

  @Test
  fun `onDataSetChanged resets range cache`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // Same range but after onDataSetChanged → should still recalculate
    rangeManager.onDataSetChanged(TestPosition(2))

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
  }

  @Test
  fun `onDataSetChanged uses last position when no position provided`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // No position provided → should use the last position from recalculateRange
    rangeManager.onDataSetChanged()

    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
  }

  @Test
  fun `onDataSetChanged is no-op when no position available`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    // No prior recalculateRange call → no lastPosition
    rangeManager.onDataSetChanged()

    assertThat(workQueueBuilder.buildWorkQueueCalls).isEmpty()
  }

  @Test
  fun `destroy resets range cache`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    calculator.rangeToReturn = 0..4

    rangeManager.recalculateRange(TestPosition(2))
    workQueueBuilder.clearCalls()

    // Same range is cached → should be skipped
    rangeManager.recalculateRange(TestPosition(2))
    assertThat(workQueueBuilder.buildWorkQueueCalls).isEmpty()

    rangeManager.destroy()

    // Recreate worker since destroy() destroys it, but reuse the same calculator/provider
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer)
    rangeManager = RangeManager(calculator, worker, itemProvider)

    // Same range as before destroy → should schedule work (cache was reset by destroy)
    rangeManager.recalculateRange(TestPosition(2))
    assertThat(workQueueBuilder.buildWorkQueueCalls).hasSize(1)
  }
}

class TestRangeCalculator : RangeCalculator<TestPosition, IntRange> {
  var rangeToReturn: IntRange = IntRange.EMPTY

  override fun calculateRange(position: TestPosition): IntRange {
    return rangeToReturn
  }
}
