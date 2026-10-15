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

/** Observer coverage for [HandlerRangeWorker], mirroring [SynchronousRangeWorkerObserverTest]. */
class HandlerRangeWorkerObserverTest {

  private lateinit var worker: HandlerRangeWorker<TestPosition, IntRange>
  private lateinit var workQueueBuilder: TestWorkQueueBuilder
  private lateinit var itemProvider: TestCollectionItemProvider
  private lateinit var scope: TestPrepareScope
  private lateinit var observer: RecordingObserver
  private lateinit var dispatcher: FakeRangeWorkDispatcher
  private val tracer = NoOpSystracer()

  @Before
  fun setup() {
    itemProvider = TestCollectionItemProvider()
    workQueueBuilder = TestWorkQueueBuilder(itemProvider)
    scope = TestPrepareScope()
    observer = RecordingObserver()
    dispatcher = FakeRangeWorkDispatcher()
    worker = HandlerRangeWorker(workQueueBuilder, scope, tracer, observer, dispatcher)
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `observer receives onItemPrepared for each prepared item`() {
    itemProvider.setItems((0..2).map { TestPreparableItem(it) })

    worker.schedule(TestPosition(1), 0..2)
    dispatcher.drain()

    assertThat(observer.events)
        .containsExactly(
            "PREPARED:0:RANGE_SCHEDULE",
            "PREPARED:1:RANGE_SCHEDULE",
            "PREPARED:2:RANGE_SCHEDULE",
        )
  }

  @Test
  fun `observer receives onItemUnprepared when items leave the range`() {
    itemProvider.setItems((0..4).map { TestPreparableItem(it) })
    worker.schedule(TestPosition(1), 0..2)
    dispatcher.drain()

    observer.reset()
    worker.schedule(TestPosition(3), 3..4)
    dispatcher.drain()

    assertThat(observer.events.filter { it.startsWith(UNPREPARED) })
        .containsExactly(
            "UNPREPARED:0:RANGE_EVICTION",
            "UNPREPARED:1:RANGE_EVICTION",
            "UNPREPARED:2:RANGE_EVICTION",
        )
  }

  @Test
  fun `work still runs when no observer is supplied`() {
    val items = (0..2).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    val dispatcherWithoutObserver = FakeRangeWorkDispatcher()
    val workerWithoutObserver = HandlerRangeWorker(
        workQueueBuilder,
        scope,
        tracer,
        observer = null,
        dispatcher = dispatcherWithoutObserver,
    )
    try {
      workerWithoutObserver.schedule(TestPosition(1), 0..2)
      dispatcherWithoutObserver.drain()

      items.forEach { assertThat(it.prepareCount).isEqualTo(1) }
    } finally {
      workerWithoutObserver.destroy()
    }
  }

  @Test
  fun `observer is notified after the work it describes has run`() {
    val item = TestPreparableItem(0)
    itemProvider.setItems(listOf(item))
    val order = mutableListOf<String>()
    item.onPrepare = { order.add("ITEM_PREPARED") }
    observer.onEvent = { order.add(it) }

    worker.schedule(TestPosition(0), 0..0)
    dispatcher.drain()

    assertThat(order).containsExactly("ITEM_PREPARED", "PREPARED:0:RANGE_SCHEDULE")
  }

  @Test
  fun `cancelled work stops notifying for items it never processed`() {
    val items = (0..9).map { TestPreparableItem(it) }
    itemProvider.setItems(items)
    items[3].onPrepare = { worker.cancel() }

    worker.schedule(TestPosition(5), 0..9)
    repeat(4) { assertThat(dispatcher.runOne()).isTrue() }
    dispatcher.drain()

    assertThat(observer.events.filter { it.startsWith(PREPARED) })
        .containsExactly(
            "PREPARED:0:RANGE_SCHEDULE",
            "PREPARED:1:RANGE_SCHEDULE",
            "PREPARED:2:RANGE_SCHEDULE",
            "PREPARED:3:RANGE_SCHEDULE",
        )
  }

  private class RecordingObserver : PreparationHealthObserver {
    private val recorded = mutableListOf<String>()
    var onEvent: ((String) -> Unit)? = null

    val events: List<String>
      get() = recorded.toList()

    fun reset() {
      recorded.clear()
    }

    private fun record(event: String) {
      recorded.add(event)
      onEvent?.invoke(event)
    }

    override fun onItemRegistered(key: Any): Unit = record("REGISTERED:$key")

    override fun onItemPrepared(key: Any, source: PreparationSource): Unit =
        record("PREPARED:$key:$source")

    override fun onItemBound(key: Any, wasPrepared: Boolean): Unit =
        record("BOUND:$key:$wasPrepared")

    override fun onItemUnprepared(key: Any, reason: UnprepareReason): Unit =
        record("UNPREPARED:$key:$reason")

    override fun reportEvent(key: Any, event: PreparationHealthEvent): Unit =
        record("EVENT:$key:${event.failureType}")

    override fun onFirstScroll(): Unit = record("FIRST_SCROLL")
  }

  private companion object {
    private const val PREPARED = "PREPARED"
    private const val UNPREPARED = "UNPREPARED"
  }
}
