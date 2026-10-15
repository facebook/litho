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

class SynchronousRangeWorkerObserverTest {

  private lateinit var worker: SynchronousRangeWorker<TestPosition, IntRange>
  private lateinit var workQueueBuilder: TestWorkQueueBuilder
  private lateinit var itemProvider: TestCollectionItemProvider
  private lateinit var scope: TestPrepareScope
  private lateinit var observer: RecordingObserver
  private val tracer = NoOpSystracer()

  @Before
  fun setup() {
    itemProvider = TestCollectionItemProvider()
    workQueueBuilder = TestWorkQueueBuilder(itemProvider)
    scope = TestPrepareScope()
    observer = RecordingObserver()
    worker = SynchronousRangeWorker(workQueueBuilder, scope, tracer, observer)
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `observer receives onItemPrepared for each prepare work item`() {
    val items = (0..2).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    worker.schedule(TestPosition(1), 0..2)

    val prepareEvents = observer.events.filter { it.startsWith("PREPARED") }
    assertThat(prepareEvents).hasSize(3)
    assertThat(prepareEvents[0]).isEqualTo("PREPARED:0:RANGE_SCHEDULE")
    assertThat(prepareEvents[1]).isEqualTo("PREPARED:1:RANGE_SCHEDULE")
    assertThat(prepareEvents[2]).isEqualTo("PREPARED:2:RANGE_SCHEDULE")
  }

  @Test
  fun `observer receives onItemUnprepared for each unprepare work item`() {
    val items = (0..4).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Prepare range 0-2
    worker.schedule(TestPosition(1), 0..2)
    observer.events.clear()

    // Shift to range 3-4 → items 0-2 should be unprepared
    worker.schedule(TestPosition(3), 3..4)

    val unprepareEvents = observer.events.filter { it.startsWith("UNPREPARED") }
    assertThat(unprepareEvents).hasSize(3)
    assertThat(unprepareEvents[0]).isEqualTo("UNPREPARED:0:RANGE_EVICTION")
    assertThat(unprepareEvents[1]).isEqualTo("UNPREPARED:1:RANGE_EVICTION")
    assertThat(unprepareEvents[2]).isEqualTo("UNPREPARED:2:RANGE_EVICTION")
  }

  @Test
  fun `no observer calls when observer is null`() {
    val workerWithoutObserver =
        SynchronousRangeWorker(workQueueBuilder, scope, tracer, observer = null)
    val items = (0..2).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Should not throw
    workerWithoutObserver.schedule(TestPosition(1), 0..2)

    // Items should still be prepared (observer doesn't affect work execution)
    items.forEach { assertThat(it.prepareCount).isEqualTo(1) }

    workerWithoutObserver.destroy()
  }

  @Test
  fun `cancelled work does not call observer for unprocessed items`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    // Cancel after item 3
    items[3].onPrepare = { worker.cancel() }

    worker.schedule(TestPosition(5), 0..9)

    // Observer should only have events for items 0-3 (inclusive — cancel happens during 3)
    val prepareEvents = observer.events.filter { it.startsWith("PREPARED") }
    assertThat(prepareEvents).hasSize(4)
    assertThat(prepareEvents.map { it.split(":")[1] }).containsExactly("0", "1", "2", "3")
  }

  @Test
  fun `observer calls happen after work execution`() {
    val items = (0..2).map { TestPreparableItem(it) }
    itemProvider.setItems(items)

    val executionOrder = mutableListOf<String>()
    items[0].onPrepare = { executionOrder.add("ITEM_0_PREPARED") }
    observer.onEvent = { executionOrder.add(it) }

    worker.schedule(TestPosition(1), 0..2)

    // Item prepare should happen before observer call
    assertThat(executionOrder[0]).isEqualTo("ITEM_0_PREPARED")
    assertThat(executionOrder[1]).isEqualTo("PREPARED:0:RANGE_SCHEDULE")
  }

  /** Recording observer for test assertions. */
  private class RecordingObserver : PreparationHealthObserver {
    val events = mutableListOf<String>()
    var onEvent: ((String) -> Unit)? = null

    override fun onItemRegistered(key: Any) {
      val event = "REGISTERED:$key"
      events.add(event)
      onEvent?.invoke(event)
    }

    override fun onItemPrepared(key: Any, source: PreparationSource) {
      val event = "PREPARED:$key:$source"
      events.add(event)
      onEvent?.invoke(event)
    }

    override fun onItemBound(key: Any, wasPrepared: Boolean) {
      val event = "BOUND:$key:$wasPrepared"
      events.add(event)
      onEvent?.invoke(event)
    }

    override fun onItemUnprepared(key: Any, reason: UnprepareReason) {
      val event = "UNPREPARED:$key:$reason"
      events.add(event)
      onEvent?.invoke(event)
    }

    override fun reportEvent(key: Any, event: PreparationHealthEvent) {
      val eventStr = "EVENT:$key:${event.failureType}"
      events.add(eventStr)
      onEvent?.invoke(eventStr)
    }

    override fun onFirstScroll() {
      val event = "FIRST_SCROLL"
      events.add(event)
      onEvent?.invoke(event)
    }
  }
}
