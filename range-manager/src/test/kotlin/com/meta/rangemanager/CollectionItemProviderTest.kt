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

/** Unit tests for [CollectionItemProvider]. */
class CollectionItemProviderTest {

  // Simulated repository
  class FakeRepository {
    private val items = mutableMapOf<String, String>()

    fun add(id: String, value: String) {
      items[id] = value
    }

    fun remove(id: String) {
      items.remove(id)
    }

    fun contains(id: String): Boolean = items.containsKey(id)

    fun get(id: String): String = items.getValue(id)
  }

  // Test item that tracks prepare/unprepare calls
  class TestItem(val id: String, private val dataProvider: () -> String) :
      PreparableCollectionItem {
    var prepareCount = 0
    var unprepareCount = 0
    var lastData: String? = null
    var stateToSave: String? = null
    var restoreState = false
    var restoredState: String? = null

    override fun PrepareScope.prepare() {
      prepareCount++
      lastData = dataProvider()
      if (restoreState) {
        restoredState = getSavedState() as? String
      }
    }

    override fun PrepareScope.unprepare() {
      unprepareCount++
      stateToSave?.let { state -> saveState(state) }
      lastData = null
    }
  }

  // Test provider that extends CollectionItemProvider
  class TestCollectionProvider(
      private val repo: FakeRepository,
      cache: ItemCache<String> = SynchronizedItemCache(),
      scope: PrepareScope = PrepareScope(),
  ) : CollectionItemProvider<String>(cache, scope) {
    var createCount = 0

    override fun create(key: String): PreparableCollectionItem {
      createCount++
      return TestItem(key) { repo.get(key) }
    }

    override fun contains(key: String): Boolean = repo.contains(key)
  }

  @Test
  fun `get returns cached item on subsequent calls`() {
    val repo = FakeRepository()
    repo.add("1", "value1")

    val provider = TestCollectionProvider(repo)

    val item1 = provider.get("1")
    val item2 = provider.get("1")

    assertThat(item2).isSameAs(item1)
    // Verify create was only called once (item was cached)
    assertThat(provider.createCount).isEqualTo(1)
  }

  @Test
  fun `items fetch data lazily during prepare`() {
    val repo = FakeRepository()
    repo.add("1", "original")

    val provider = TestCollectionProvider(repo)
    val item = provider.get("1") as TestItem

    // Data not fetched yet
    assertThat(item.lastData).isNull()

    // Prepare fetches data
    with(item) { PrepareScope().prepare() }
    assertThat(item.lastData).isEqualTo("original")

    // Update data in repository
    repo.add("1", "updated")

    // Next prepare fetches fresh data
    with(item) { PrepareScope().prepare() }
    assertThat(item.lastData).isEqualTo("updated")
  }

  @Test
  fun `onDataSetChanged evicts removed items and calls unprepare`() {
    val repo = FakeRepository()
    repo.add("1", "value1")
    repo.add("2", "value2")

    val provider = TestCollectionProvider(repo)

    val item1 = provider.get("1") as TestItem
    val item2 = provider.get("2") as TestItem

    // Prepare both
    with(item1) { PrepareScope().prepare() }
    with(item2) { PrepareScope().prepare() }

    // Remove item 2 from repository
    repo.remove("2")

    // onDataSetChanged evicts item2
    provider.onDataSetChanged()

    // item2 should be unprepared
    assertThat(item2.unprepareCount).isEqualTo(1)

    // item1 should NOT be evicted
    assertThat(provider.get("1")).isSameAs(item1)
    assertThat(item1.unprepareCount).isEqualTo(0)
  }

  @Test
  fun `invalidate clears state saved during unprepare`() {
    val repo = FakeRepository()
    repo.add("1", "value1")
    val scope = PrepareScope()
    val provider = TestCollectionProvider(repo, scope = scope)
    val item = provider.get("1") as TestItem
    item.stateToSave = "stale"

    provider.invalidate("1")
    val replacement = provider.get("1") as TestItem
    replacement.restoreState = true
    scope.prepare("1", replacement)

    assertThat(replacement.restoredState).isNull()
  }

  @Test
  fun `invalidate removes item and calls unprepare`() {
    val repo = FakeRepository()
    repo.add("1", "value1")

    val provider = TestCollectionProvider(repo)
    val item = provider.get("1") as TestItem
    with(item) { PrepareScope().prepare() }

    provider.invalidate("1")

    assertThat(item.unprepareCount).isEqualTo(1)

    // Next get creates new item
    val newItem = provider.get("1")
    assertThat(newItem).isNotSameAs(item)
  }

  @Test
  fun `evictIfSame removes the expected cached item`() {
    val repo = FakeRepository()
    repo.add("1", "value1")
    val provider = TestCollectionProvider(repo)
    val expectedItem = provider["1"]

    assertThat(provider.evictIfSame("1", expectedItem)).isTrue()
    assertThat(provider["1"]).isNotSameAs(expectedItem)
  }

  @Test
  fun `evictIfSame retains a replacement stored after the expected item`() {
    val repo = FakeRepository()
    repo.add("1", "value1")
    val cache = SynchronizedItemCache<String>()
    val provider = TestCollectionProvider(repo, cache)
    val oldItem = provider["1"]
    val replacementItem = TestItem("1") { repo.get("1") }
    cache.put("1", replacementItem)

    assertThat(provider.evictIfSame("1", oldItem)).isFalse()
    assertThat(provider["1"]).isSameAs(replacementItem)
  }

  @Test
  fun `invalidateAll removes all items and calls unprepare on each`() {
    val repo = FakeRepository()
    repo.add("1", "value1")
    repo.add("2", "value2")

    val provider = TestCollectionProvider(repo)

    val item1 = provider.get("1") as TestItem
    val item2 = provider.get("2") as TestItem

    with(item1) { PrepareScope().prepare() }
    with(item2) { PrepareScope().prepare() }

    provider.invalidateAll()

    assertThat(item1.unprepareCount).isEqualTo(1)
    assertThat(item2.unprepareCount).isEqualTo(1)

    // New items created on next get
    assertThat(provider.get("1")).isNotSameAs(item1)
    assertThat(provider.get("2")).isNotSameAs(item2)
  }

  @Test
  fun `onDataSetChanged on empty cache does not throw`() {
    val repo = FakeRepository()
    val provider = TestCollectionProvider(repo)

    // Should not throw
    provider.onDataSetChanged()
  }

  @Test
  fun `invalidateAll on empty cache does not throw`() {
    val repo = FakeRepository()
    val provider = TestCollectionProvider(repo)

    // Should not throw
    provider.invalidateAll()
  }

  @Test
  fun `invalidate on missing key does not throw`() {
    val repo = FakeRepository()
    val provider = TestCollectionProvider(repo)

    // Should not throw
    provider.invalidate("nonexistent")
  }

  @Test
  fun `contains delegates to create implementation`() {
    val repo = FakeRepository()
    repo.add("1", "value1")

    val provider = TestCollectionProvider(repo)

    assertThat(provider.contains("1")).isTrue()
    assertThat(provider.contains("nonexistent")).isFalse()
  }
}
