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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Unit tests for [SynchronizedItemCache]. */
class SynchronizedItemCacheTest {

  private data class DataClassKey(val id: String)

  private class TestItem(val id: String) : PreparableCollectionItem {
    override fun PrepareScope.prepare() = Unit

    override fun PrepareScope.unprepare() = Unit
  }

  private data class EqualTestItem(val id: String) : PreparableCollectionItem {
    override fun PrepareScope.prepare() = Unit

    override fun PrepareScope.unprepare() = Unit
  }

  private class CustomItemCache : ItemCache<String> {
    private val items = mutableMapOf<String, PreparableCollectionItem>()
    var unconditionalRemoveCalls = 0
      private set

    override fun getOrPut(
        key: String,
        create: () -> PreparableCollectionItem,
    ): PreparableCollectionItem = items.getOrPut(key, create)

    override fun get(key: String): PreparableCollectionItem? = items[key]

    override fun put(key: String, item: PreparableCollectionItem) {
      items[key] = item
    }

    override fun remove(key: String): PreparableCollectionItem? {
      unconditionalRemoveCalls++
      return items.remove(key)
    }

    override fun keys(): Set<String> = items.keys.toSet()

    override fun clear(): Unit = items.clear()
  }

  @Test
  fun `getOrPut returns cached item on second call`() {
    val cache = SynchronizedItemCache<String>()
    var createCount = 0

    val item1 =
        cache.getOrPut("key1") {
          createCount++
          TestItem("1")
        }

    val item2 =
        cache.getOrPut("key1") {
          createCount++
          TestItem("should-not-be-created")
        }

    assertThat(createCount).isEqualTo(1)
    assertThat(item2).isSameAs(item1)
    assertThat((item1 as TestItem).id).isEqualTo("1")
  }

  @Test
  fun `get returns null for missing key`() {
    val cache = SynchronizedItemCache<String>()

    assertThat(cache.get("missing")).isNull()
  }

  @Test
  fun `get returns item after put`() {
    val cache = SynchronizedItemCache<String>()
    val item = TestItem("1")

    cache.put("key1", item)

    assertThat(cache.get("key1")).isSameAs(item)
  }

  @Test
  fun `remove returns and removes item`() {
    val cache = SynchronizedItemCache<String>()
    val item = TestItem("1")

    cache.put("key1", item)

    val removed = cache.remove("key1")

    assertThat(removed).isSameAs(item)
    assertThat(cache.get("key1")).isNull()
  }

  @Test
  fun `remove returns null for missing key`() {
    val cache = SynchronizedItemCache<String>()

    assertThat(cache.remove("missing")).isNull()
  }

  @Test
  fun `removeIfSame removes expected exact instance and reports success`() {
    val cache = SynchronizedItemCache<String>()
    val expectedItem = TestItem("expected")
    cache.put("key", expectedItem)

    assertThat(cache.removeIfSame("key", expectedItem)).isTrue()
    assertThat(cache.get("key")).isNull()
  }

  @Test
  fun `removeIfSame retains equal or different replacement instances and reports false`() {
    val cache = SynchronizedItemCache<String>()
    val expectedEqualItem = EqualTestItem("equal")
    val cachedEqualItem = EqualTestItem("equal")
    cache.put("equal", cachedEqualItem)

    assertThat(cache.removeIfSame("equal", expectedEqualItem)).isFalse()
    assertThat(cache.get("equal")).isSameAs(cachedEqualItem)

    val expectedDifferentItem = TestItem("expected")
    val cachedDifferentItem = TestItem("replacement")
    cache.put("different", cachedDifferentItem)

    assertThat(cache.removeIfSame("different", expectedDifferentItem)).isFalse()
    assertThat(cache.get("different")).isSameAs(cachedDifferentItem)
  }

  @Test
  fun `removeIfSame cannot evict a racing replacement in either linearization order`() {
    repeat(100) { iteration ->
      val cache = SynchronizedItemCache<String>()
      val expectedItem = TestItem("expected-$iteration")
      val replacementItem = TestItem("replacement-$iteration")
      cache.put("key", expectedItem)

      cache.put("key", replacementItem)
      assertThat(cache.removeIfSame("key", expectedItem)).isFalse()
      assertThat(cache.get("key")).isSameAs(replacementItem)

      cache.put("key", expectedItem)
      assertThat(cache.removeIfSame("key", expectedItem)).isTrue()
      cache.put("key", replacementItem)
      assertThat(cache.get("key")).isSameAs(replacementItem)
    }
  }

  @Test
  fun `NoCache removeIfSame reports false`() {
    val cache = NoCache<String>()

    assertThat(cache.removeIfSame("key", TestItem("expected"))).isFalse()
    assertThat(cache.supportsExactRemoval).isTrue()
  }

  @Test
  fun `custom cache without removeIfSame override fails closed`() {
    val cache = CustomItemCache()
    val expectedItem = TestItem("expected")
    cache.put("key", expectedItem)

    assertThat(cache.removeIfSame("key", expectedItem)).isFalse()
    assertThat(cache.get("key")).isSameAs(expectedItem)
    assertThat(cache.unconditionalRemoveCalls).isZero()
    assertThat(cache.supportsExactRemoval).isFalse()
  }

  @Test
  fun `keys returns all cached keys`() {
    val cache = SynchronizedItemCache<String>()

    cache.put("key1", TestItem("1"))
    cache.put("key2", TestItem("2"))
    cache.put("key3", TestItem("3"))

    assertThat(cache.keys()).containsExactlyInAnyOrder("key1", "key2", "key3")
  }

  @Test
  fun `keys returns empty set for empty cache`() {
    val cache = SynchronizedItemCache<String>()

    assertThat(cache.keys()).isEmpty()
  }

  @Test
  fun `clear removes all items`() {
    val cache = SynchronizedItemCache<String>()

    cache.put("key1", TestItem("1"))
    cache.put("key2", TestItem("2"))

    cache.clear()

    assertThat(cache.keys()).isEmpty()
    assertThat(cache.get("key1")).isNull()
    assertThat(cache.get("key2")).isNull()
  }

  @Test
  fun `put overwrites existing item`() {
    val cache = SynchronizedItemCache<String>()
    val item1 = TestItem("1")
    val item2 = TestItem("2")

    cache.put("key", item1)
    cache.put("key", item2)

    assertThat(cache.get("key")).isSameAs(item2)
  }

  @Test
  fun `getOrPut is thread-safe and creates item only once under concurrent access`() {
    val cache = SynchronizedItemCache<String>()
    val createCount = AtomicInteger(0)
    val threads = 10
    val startLatch = CountDownLatch(1)
    val doneLatch = CountDownLatch(threads)
    val results = ConcurrentHashMap<Int, PreparableCollectionItem>()

    repeat(threads) { i ->
      Thread {
        startLatch.await() // Wait for all threads to be ready
        val item =
            cache.getOrPut("key") {
              createCount.incrementAndGet()
              Thread.sleep(10) // Simulate slow creation
              TestItem("1")
            }
        results[i] = item
        doneLatch.countDown()
      }
          .start()
    }

    startLatch.countDown() // Release all threads at once
    doneLatch.await()

    assertThat(createCount.get()).isEqualTo(1)
    assertThat(results.values.distinct()).hasSize(1) // All threads got the same instance
  }

  @Test
  fun `uses key equality not identity`() {
    val cache = SynchronizedItemCache<DataClassKey>()
    val item = TestItem("1")

    cache.put(DataClassKey("a"), item)

    // Different DataClassKey instance with same value should retrieve the same item
    assertThat(cache.get(DataClassKey("a"))).isSameAs(item)
  }

  @Test
  fun `getOrPut does not cache if factory throws`() {
    val cache = SynchronizedItemCache<String>()

    runCatching { cache.getOrPut("key") { error("boom") } }

    assertThat(cache.get("key")).isNull()
  }

  @Test
  fun `keys returns defensive copy that does not affect cache`() {
    val cache = SynchronizedItemCache<String>()
    cache.put("key1", TestItem("1"))

    val keys = cache.keys().toMutableSet()
    keys.clear()

    assertThat(cache.get("key1")).isNotNull()
    assertThat(cache.keys()).containsExactly("key1")
  }
}
