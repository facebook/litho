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

/**
 * [CollectionItemProvider.onItemRemoved]: a provider that keeps per-key data learns when an item
 * leaves its cache, on every path that removes one, so it can drop that data.
 */
class CollectionItemProviderRemovalTest {

  private val items = (0 until 10).associate { "k$it" to TestStringKeyItem("k$it") }
  private val provider = RecordingProvider(items)

  @Test
  fun `the worker releasing a row that left the range reports it`() {
    val worker = KeyBasedRangeWorker(provider)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0", "k1")))

    worker.schedule(Unit, TestStringKeyRange(listOf("k1", "k2")))

    assertThat(provider.removed).containsExactly("k0" to items.getValue("k0"))
    worker.destroy()
  }

  @Test
  fun `a data change reports each item it evicts`() {
    cache("k0", "k1", "k2")
    provider.present = setOf("k1")

    provider.onDataSetChanged()

    assertThat(provider.removed.map { it.first }).containsExactlyInAnyOrder("k0", "k2")
  }

  /** A provider whose hook throws for one item still has every other item released and reported. */
  @Test
  fun `a throwing hook during a data change still releases and reports the other items`() {
    val throwing = RecordingProvider(items) { key -> if (key == "k0") error("hook failed") }
    listOf("k0", "k1", "k2").forEach { throwing[it] }
    throwing.present = emptySet()

    val failure = runCatching { throwing.onDataSetChanged() }.exceptionOrNull()

    assertThat(failure).hasMessage("hook failed")
    assertThat(throwing.removed.map { it.first }).containsExactlyInAnyOrder("k0", "k1", "k2")
    assertThat(listOf("k0", "k1", "k2").map { items.getValue(it).unprepareCount }).containsOnly(1)
  }

  @Test
  fun `a throwing hook during invalidateAll still reports the other items`() {
    val throwing = RecordingProvider(items) { key -> if (key == "k0") error("hook failed") }
    listOf("k0", "k1", "k2").forEach { throwing[it] }

    val failure = runCatching { throwing.invalidateAll() }.exceptionOrNull()

    assertThat(failure).hasMessage("hook failed")
    assertThat(throwing.removed.map { it.first }).containsExactlyInAnyOrder("k0", "k1", "k2")
  }

  @Test
  fun `invalidating a key reports its item`() {
    cache("k0")

    provider.invalidate("k0")

    assertThat(provider.removed).containsExactly("k0" to items.getValue("k0"))
  }

  @Test
  fun `invalidating everything reports each item`() {
    cache("k0", "k1")

    provider.invalidateAll()

    assertThat(provider.removed.map { it.first }).containsExactlyInAnyOrder("k0", "k1")
  }

  @Test
  fun `evicting a key reports its item`() {
    cache("k0")

    provider.evict("k0")

    assertThat(provider.removed).containsExactly("k0" to items.getValue("k0"))
  }

  /**
   * The async worker evicts only the exact item it released, so a replacement stays. The eviction
   * itself reports nothing: the worker reports it through notifyRemoved, outside its locks.
   */
  @Test
  fun `an exact eviction removes only that item and leaves reporting to the worker`() {
    cache("k0")

    val removedOther = provider.evictIfSame("k0", TestStringKeyItem("other"))
    val removedSame = provider.evictIfSame("k0", items.getValue("k0"))

    assertThat(removedOther).isFalse()
    assertThat(removedSame).isTrue()
    assertThat(provider.removed).isEmpty()
    provider.notifyRemoved("k0", items.getValue("k0"))
    assertThat(provider.removed).containsExactly("k0" to items.getValue("k0"))
  }

  /**
   * The async worker claims an item (taking it out of the cache) before it unprepares it. The hook
   * fires once the release is done, after `unprepare` and outside the worker's locks, like every
   * other path.
   */
  @Test
  fun `the async worker reports a released row after unpreparing it`() {
    val events = mutableListOf<String>()
    val ordered =
        (0 until 2).associate { key ->
          "k$key" to TestStringKeyItem("k$key", onUnprepare = { events += "unprepare:k$key" })
        }
    val recording = RecordingProvider(ordered) { key -> events += "removed:$key" }
    val dispatcher = FakeRangeWorkDispatcher()
    val worker = AsynchronousKeyBasedRangeWorker(recording, dispatcher = dispatcher)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0", "k1")))
    dispatcher.drain()

    worker.schedule(Unit, TestStringKeyRange(listOf("k1")))
    dispatcher.drain()

    assertThat(events).containsExactly("unprepare:k0", "removed:k0")
    worker.destroy()
  }

  /**
   * Destroy took the item out of the cache, so the provider hears about it even if unprepare fails.
   */
  @Test
  fun `the async worker reports a row destroyed even when its unprepare fails`() {
    val failing = mapOf(
        "k0" to TestStringKeyItem("k0", onUnprepare = { throw IllegalStateException("boom") }),
    )
    val recording = RecordingProvider(failing)
    val dispatcher = FakeRangeWorkDispatcher()
    val worker = AsynchronousKeyBasedRangeWorker(recording, dispatcher = dispatcher)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0")))
    dispatcher.drain()

    worker.destroy()
    dispatcher.drain()

    assertThat(recording.removed.map { it.first }).containsExactly("k0")
  }

  /** A provider whose hook throws must not stop the worker releasing the rest of the range. */
  @Test
  fun `a throwing removal hook does not stop the async worker's other releases`() {
    var thrown = false
    val recording =
        RecordingProvider(items) { _ ->
          if (!thrown) {
            thrown = true
            throw IllegalStateException("hook failed")
          }
        }
    val dispatcher = FakeRangeWorkDispatcher()
    val worker = AsynchronousKeyBasedRangeWorker(recording, dispatcher = dispatcher)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0", "k1", "k2")))
    dispatcher.drain()

    worker.schedule(Unit, TestStringKeyRange(listOf("k3")))
    dispatcher.drain()

    assertThat((0 until 3).map { items.getValue("k$it").unprepareCount }).containsOnly(1)
    assertThat(items.getValue("k3").prepareCount).isEqualTo(1)
    worker.destroy()
  }

  /** The hook runs without the worker's locks, so another thread can still cancel meanwhile. */
  @Test
  fun `the async worker's hook does not block a cancel from another thread`() {
    val cancelled = java.util.concurrent.CountDownLatch(1)
    lateinit var worker: AsynchronousKeyBasedRangeWorker<String>
    val recording =
        RecordingProvider(items) { _ ->
          Thread {
            worker.cancel()
            cancelled.countDown()
          }
              .start()
          assertThat(cancelled.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue()
        }
    val dispatcher = FakeRangeWorkDispatcher()
    worker = AsynchronousKeyBasedRangeWorker(recording, dispatcher = dispatcher)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0", "k1")))
    dispatcher.drain()

    worker.schedule(Unit, TestStringKeyRange(listOf("k1")))
    dispatcher.drain()

    assertThat(cancelled.count).isEqualTo(0)
    worker.destroy()
  }

  @Test
  fun `removing a key that is not cached reports nothing`() {
    provider.invalidate("k0")
    provider.evict("k1")

    assertThat(provider.removed).isEmpty()
  }

  private fun cache(vararg keys: String) = keys.forEach { provider[it] }

  private class RecordingProvider(
      private val items: Map<String, TestStringKeyItem>,
      private val onRemoved: (String) -> Unit = {},
  ) : CollectionItemProvider<String>() {
    var present: Set<String> = items.keys
    val removed = mutableListOf<Pair<String, PreparableCollectionItem>>()

    override fun create(key: String): PreparableCollectionItem = items.getValue(key)

    override fun contains(key: String): Boolean = key in present

    override fun onItemRemoved(key: String, item: PreparableCollectionItem) {
      removed += key to item
      onRemoved(key)
    }
  }
}
