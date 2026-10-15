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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreparationHealthTrackerTest {

  @Test
  fun `concurrent callbacks retain every event`() {
    val tracker = PreparationHealthTracker("Test")
    val start = java.util.concurrent.CountDownLatch(1)
    val threads =
        (0 until 8).map { threadIndex ->
          Thread {
            start.await()
            repeat(500) { eventIndex ->
              tracker.onItemPrepared(
                  "key-$threadIndex-$eventIndex",
                  PreparationSource.RANGE_SCHEDULE,
              )
            }
          }
              .apply { start() }
        }

    start.countDown()
    threads.forEach(Thread::join)

    assertThat(tracker.dumpSummary()).contains("Prepared: 4000")
  }

  @Test
  fun `registers item and increments counter`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemRegistered("key1")
    tracker.onItemRegistered("key2")

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Registered: 2")
    assertThat(summary).contains("Currently registered keys: 2")
  }

  @Test
  fun `registers item with empty key still increments counter`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemRegistered("")

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Registered: 1")
    assertThat(summary).contains("Currently registered keys: 0")
  }

  @Test
  fun `prepares item and increments counter`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Prepared: 1")
    assertThat(summary).contains("RANGE_SCHEDULE: 1")
  }

  @Test
  fun `tracks prepare source breakdown`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemPrepared("key2", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemPrepared("key3", PreparationSource.PREPARE_KEY)

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("RANGE_SCHEDULE: 2")
    assertThat(summary).contains("PREPARE_KEY: 1")
  }

  @Test
  fun `binding with preparation records no failure`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemBound("key1", wasPrepared = true)

    assertThat(tracker.preparationFailures).isEmpty()
    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Bound: 1")
    assertThat(summary).doesNotContain("Preparation failures")
  }

  @Test
  fun `binding without preparation records MISS failure`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemBound("key1", wasPrepared = false)

    assertThat(tracker.preparationFailures).hasSize(1)
    val failure = tracker.preparationFailures[0]
    assertThat(failure.type).isEqualTo(PreparationFailureType.MISS)
    assertThat(failure.key).isEqualTo("key1")
    assertThat(failure.wasEvicted).isFalse()
  }

  @Test
  fun `eviction followed by bind records MISS with wasEvicted true`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemUnprepared("key1", UnprepareReason.RANGE_EVICTION)
    tracker.onItemBound("key1", wasPrepared = false)

    assertThat(tracker.preparationFailures).hasSize(1)
    val failure = tracker.preparationFailures[0]
    assertThat(failure.type).isEqualTo(PreparationFailureType.MISS)
    assertThat(failure.wasEvicted).isTrue()
  }

  @Test
  fun `re-preparation after eviction clears evicted state`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemUnprepared("key1", UnprepareReason.RANGE_EVICTION)
    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemBound("key1", wasPrepared = false)

    assertThat(tracker.preparationFailures).hasSize(1)
    val failure = tracker.preparationFailures[0]
    assertThat(failure.wasEvicted).isFalse()
  }

  @Test
  fun `unprepare tracks reason breakdown`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemUnprepared("key1", UnprepareReason.RANGE_EVICTION)
    tracker.onItemUnprepared("key2", UnprepareReason.RANGE_EVICTION)
    tracker.onItemUnprepared("key3", UnprepareReason.DATA_SET_CHANGED)

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Unprepared: 3")
    assertThat(summary).contains("RANGE_EVICTION: 2")
    assertThat(summary).contains("DATA_SET_CHANGED: 1")
  }

  @Test
  fun `tracks preps before first bind`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemPrepared("key2", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemBound("key1", wasPrepared = true)
    tracker.onItemPrepared("key3", PreparationSource.RANGE_SCHEDULE)

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Preps before first bind: 2")
  }

  @Test
  fun `tracks preps before first scroll`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onItemPrepared("key2", PreparationSource.RANGE_SCHEDULE)
    tracker.onFirstScroll()
    tracker.onItemPrepared("key3", PreparationSource.RANGE_SCHEDULE)

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Preps before first scroll: 2")
    assertThat(summary).contains("Initial load prep count: 2")
  }

  @Test
  fun `onFirstScroll is idempotent`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemPrepared("key1", PreparationSource.RANGE_SCHEDULE)
    tracker.onFirstScroll()
    tracker.onItemPrepared("key2", PreparationSource.RANGE_SCHEDULE)
    tracker.onFirstScroll()

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("Initial load prep count: 1")
  }

  @Test
  fun `reportEvent adds failure with correct type and details`() {
    val tracker = PreparationHealthTracker("Test")
    val event =
        object : PreparationHealthEvent {
          override val failureType = PreparationFailureType.LATE
          override val details = "NO_LAYOUT_STATE"
        }

    tracker.reportEvent("key1", event)

    assertThat(tracker.preparationFailures).hasSize(1)
    val failure = tracker.preparationFailures[0]
    assertThat(failure.type).isEqualTo(PreparationFailureType.LATE)
    assertThat(failure.key).isEqualTo("key1")
    assertThat(failure.details).isEqualTo("NO_LAYOUT_STATE")
  }

  @Test
  fun `dumpSummary uses surface name`() {
    val tracker = PreparationHealthTracker("Stories")
    val summary = tracker.dumpSummary()
    assertThat(summary).contains("=== Stories Preparation Health ===")
  }

  @Test
  fun `dumpSummary includes failure breakdown by type`() {
    val tracker = PreparationHealthTracker("Test")

    tracker.onItemBound("key1", wasPrepared = false) // MISS
    tracker.onItemBound("key2", wasPrepared = false) // MISS
    tracker.reportEvent(
        "key3",
        object : PreparationHealthEvent {
          override val failureType = PreparationFailureType.LATE
          override val details = null
        },
    )
    tracker.reportEvent(
        "key4",
        object : PreparationHealthEvent {
          override val failureType = PreparationFailureType.WRONG
          override val details = "SIZE_SPEC_MISMATCH"
        },
    )

    val summary = tracker.dumpSummary()
    assertThat(summary).contains("2 MISS")
    assertThat(summary).contains("1 LATE")
    assertThat(summary).contains("1 WRONG")
  }
}
