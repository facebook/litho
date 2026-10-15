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
import org.junit.After
import org.junit.Before
import org.junit.Test

class AsynchronousKeyBasedRangeWorkerTest {

  private lateinit var dispatcher: FakeRangeWorkDispatcher
  private lateinit var worker: AsynchronousKeyBasedRangeWorker<String>
  private lateinit var itemProvider: TestStringKeyItemProvider
  private lateinit var scope: PrepareScope

  @Before
  fun setup() {
    dispatcher = FakeRangeWorkDispatcher()
    itemProvider = TestStringKeyItemProvider()
    scope = PrepareScope()
    worker =
        AsynchronousKeyBasedRangeWorker(
            itemProvider = itemProvider,
            scope = scope,
            dispatcher = dispatcher,
        )
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `schedule resolves on the caller and prepares later in range order`() {
    val order = mutableListOf<String>()
    val items =
        listOf("A", "B", "C").map { key ->
          TestStringKeyItem(key, onPrepare = { order.add(key) })
        }
    itemProvider.setItems(items.associateBy { it.key })

    worker.schedule(Unit, TestStringKeyRange(listOf("C", "A", "C", "B")))

    assertThat(itemProvider.createdKeys).containsExactly("C", "A", "B")
    assertThat(order).isEmpty()

    dispatcher.drain()

    assertThat(order).containsExactly("C", "A", "B")
  }

  @Test
  fun `overlap is retained and releases preserve prepared insertion order`() {
    val releaseOrder = mutableListOf<String>()
    val items =
        listOf("A", "B", "C", "D").map { key ->
          TestStringKeyItem(key, onUnprepare = { releaseOrder.add(key) })
        }
    itemProvider.setItems(items.associateBy { it.key })
    worker.schedule(Unit, TestStringKeyRange(listOf("C", "A", "B")))
    dispatcher.drain()

    worker.schedule(Unit, TestStringKeyRange(listOf("B", "D")))
    dispatcher.drain()

    assertThat(items.first { it.key == "B" }.prepareCount).isEqualTo(1)
    assertThat(items.first { it.key == "D" }.prepareCount).isEqualTo(1)
    assertThat(releaseOrder).containsExactly("C", "A")
  }

  @Test
  fun `prepareKey coalesces pending work and refreshes again after completion`() {
    val item = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to item))

    worker.prepareKey("A")
    worker.prepareKey("A")

    assertThat(itemProvider.createdKeys).containsExactly("A")
    assertThat(item.prepareCount).isZero()

    dispatcher.drain()
    assertThat(item.prepareCount).isEqualTo(1)

    worker.prepareKey("A")
    dispatcher.drain()

    assertThat(item.prepareCount).isEqualTo(2)
    assertThat(item.unprepareCount).isZero()
  }

  @Test
  fun `prepareKeyIfInRange uses the latest accepted target`() {
    val itemA = TestStringKeyItem("A")
    val itemB = TestStringKeyItem("B")
    itemProvider.setItems(mapOf("A" to itemA, "B" to itemB))

    assertThat(worker.prepareKeyIfInRange("A")).isFalse()
    worker.schedule(Unit, TestStringKeyRange(listOf("A")))

    assertThat(worker.prepareKeyIfInRange("A")).isTrue()
    assertThat(worker.prepareKeyIfInRange("B")).isFalse()
    assertThat(itemA.prepareCount).isZero()

    dispatcher.drain()

    assertThat(itemA.prepareCount).isEqualTo(2)
    assertThat(itemB.prepareCount).isZero()
  }

  @Test
  fun `cancel invalidates queued and already dequeued key work`() {
    val item = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to item))
    worker.prepareKey("A")
    assertThat(dispatcher.dequeueOne()).isTrue()

    worker.cancel()
    assertThat(dispatcher.runDequeued()).isTrue()

    worker.prepareKey("A")
    worker.cancel()
    dispatcher.drain()

    assertThat(item.prepareCount).isZero()
  }

  @Test
  fun `replacement releases the prepared instance before preparing the new instance`() {
    itemProvider = TestStringKeyItemProvider(cache = NoCache())
    worker.destroy()
    dispatcher = FakeRangeWorkDispatcher()
    worker =
        AsynchronousKeyBasedRangeWorker(
            itemProvider = itemProvider,
            scope = scope,
            dispatcher = dispatcher,
        )
    val oldItem = TestStringKeyItem("A")
    val newItem = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to oldItem))
    worker.prepareKey("A")
    dispatcher.drain()
    itemProvider.setItems(mapOf("A" to newItem))

    worker.prepareKey("A")
    assertThat(dispatcher.runOne()).isTrue()
    assertThat(oldItem.unprepareCount).isEqualTo(1)
    assertThat(newItem.prepareCount).isZero()

    assertThat(dispatcher.runOne()).isTrue()
    assertThat(newItem.prepareCount).isEqualTo(1)
  }

  @Test
  fun `observer receives the source of committed key preparation`() {
    val sources = mutableListOf<PreparationSource>()
    val observer =
        object : NoOpTestPreparationHealthObserver() {
          override fun onItemPrepared(key: Any, source: PreparationSource) {
            sources.add(source)
          }
        }
    worker.destroy()
    dispatcher = FakeRangeWorkDispatcher()
    worker =
        AsynchronousKeyBasedRangeWorker(
            itemProvider = itemProvider,
            scope = scope,
            observer = observer,
            dispatcher = dispatcher,
        )
    val item = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to item))
    worker.schedule(Unit, TestStringKeyRange(listOf("A")))
    dispatcher.drain()
    worker.prepareKey("A")
    dispatcher.drain()
    worker.prepareKeyIfInRange("A")
    dispatcher.drain()

    assertThat(sources)
        .containsExactly(
            PreparationSource.RANGE_SCHEDULE,
            PreparationSource.PREPARE_KEY,
            PreparationSource.PREPARE_KEY_IN_RANGE,
        )
  }

  private open class NoOpTestPreparationHealthObserver : PreparationHealthObserver {
    override fun onItemRegistered(key: Any): Unit = Unit

    override fun onItemPrepared(key: Any, source: PreparationSource): Unit = Unit

    override fun onItemBound(key: Any, wasPrepared: Boolean): Unit = Unit

    override fun onItemUnprepared(key: Any, reason: UnprepareReason): Unit = Unit

    override fun reportEvent(key: Any, event: PreparationHealthEvent): Unit = Unit

    override fun onFirstScroll(): Unit = Unit
  }
}
