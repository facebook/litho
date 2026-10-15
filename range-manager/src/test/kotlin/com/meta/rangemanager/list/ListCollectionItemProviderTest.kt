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

import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.PrepareScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Unit tests for [ListCollectionItemProvider]. */
class ListCollectionItemProviderTest {

  // Test item that tracks prepare/unprepare calls
  class TestItem(val index: Int) : PreparableCollectionItem {
    var prepareCount = 0
    var unprepareCount = 0

    override fun PrepareScope.prepare() {
      prepareCount++
    }

    override fun PrepareScope.unprepare() {
      unprepareCount++
    }
  }

  // Test provider that extends abstract ListCollectionItemProvider
  class TestCollectionListProvider(private var items: List<TestItem>) :
      ListCollectionItemProvider() {
    var createCount = 0

    override val size: Int
      get() = items.size

    override fun create(key: Int): PreparableCollectionItem {
      createCount++
      return items[key]
    }

    fun setItems(newItems: List<TestItem>) {
      items = newItems
    }
  }

  // Factory-based provider that creates new items on each create call
  class FactoryCollectionListProvider(private var itemCount: Int) : ListCollectionItemProvider() {
    val createdItems = mutableListOf<TestItem>()

    override val size: Int
      get() = itemCount

    override fun create(key: Int): PreparableCollectionItem {
      val item = TestItem(key)
      createdItems.add(item)
      return item
    }

    fun setSize(newSize: Int) {
      itemCount = newSize
    }
  }

  // =====================================================================================
  // CACHING TESTS (inherited from CollectionItemProvider)
  // =====================================================================================

  @Test
  fun `get returns cached item on subsequent calls`() {
    // Use FactoryCollectionListProvider which creates new items on each create()
    // This properly tests that caching returns the same instance
    val provider = FactoryCollectionListProvider(3)

    val item1 = provider[0]
    val item2 = provider[0]

    assertThat(item2).isSameAs(item1)
    // Verify only one item was created by the factory
    assertThat(provider.createdItems.size).isEqualTo(1)
  }

  @Test
  fun `get creates new item only on first access`() {
    val provider = FactoryCollectionListProvider(2)

    provider[0]
    provider[0]
    provider[0]

    // Factory should only create one item despite 3 get calls
    assertThat(provider.createdItems.size).isEqualTo(1)
  }

  @Test
  fun `invalidate removes item and calls unprepare`() {
    val provider = FactoryCollectionListProvider(2)

    val item = provider[0] as TestItem
    with(item) { PrepareScope().prepare() }

    provider.invalidate(0)

    assertThat(item.unprepareCount).isEqualTo(1)

    // Next get creates new item
    val newItem = provider[0]
    assertThat(newItem).isNotSameAs(item)
    assertThat(provider.createdItems.size).isEqualTo(2) // First item + new item
  }

  @Test
  fun `invalidateAll removes all items and calls unprepare on each`() {
    val provider = FactoryCollectionListProvider(3)

    val cachedItems = (0..2).map { provider[it] as TestItem }
    cachedItems.forEach { with(it) { PrepareScope().prepare() } }

    provider.invalidateAll()

    cachedItems.forEach { item -> assertThat(item.unprepareCount).isEqualTo(1) }
  }

  @Test
  fun `onDataSetChanged evicts items beyond new size`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    // Cache all items
    items.forEach { provider[it.index] }

    // Shrink the list
    provider.setItems(items.take(2))
    provider.onDataSetChanged()

    // Items 0-1 should still be cached
    assertThat(items[0].unprepareCount).isEqualTo(0)
    assertThat(items[1].unprepareCount).isEqualTo(0)

    // Items 2-4 should be evicted and unprepared
    assertThat(items[2].unprepareCount).isEqualTo(1)
    assertThat(items[3].unprepareCount).isEqualTo(1)
    assertThat(items[4].unprepareCount).isEqualTo(1)
  }

  @Test
  fun `onDataSetChanged on empty cache does not throw`() {
    val provider = TestCollectionListProvider(emptyList())

    // Should not throw
    provider.onDataSetChanged()
  }

  @Test
  fun `invalidateAll on empty cache does not throw`() {
    val provider = TestCollectionListProvider(emptyList())

    // Should not throw
    provider.invalidateAll()
  }

  @Test
  fun `invalidate on missing key does not throw`() {
    val provider = TestCollectionListProvider(listOf(TestItem(0)))

    // Should not throw
    provider.invalidate(99)
  }

  // =====================================================================================
  // LIST-SPECIFIC TESTS
  // =====================================================================================

  @Test
  fun `size delegates to underlying provider`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    assertThat(provider.size).isEqualTo(5)

    // Shrink the list
    provider.setItems(items.take(2))
    assertThat(provider.size).isEqualTo(2)
  }

  @Test
  fun `lastIndex returns size minus one`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    assertThat(provider.lastIndex).isEqualTo(4)
  }

  @Test
  fun `lastIndex returns negative one for empty collection`() {
    val provider = TestCollectionListProvider(emptyList())

    assertThat(provider.lastIndex).isEqualTo(-1)
  }

  @Test
  fun `contains returns true for valid indices`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    assertThat(provider.contains(0)).isTrue()
    assertThat(provider.contains(2)).isTrue()
    assertThat(provider.contains(4)).isTrue()
  }

  @Test
  fun `contains returns false for out of bounds indices`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    assertThat(provider.contains(-1)).isFalse()
    assertThat(provider.contains(5)).isFalse()
    assertThat(provider.contains(100)).isFalse()
  }

  @Test
  fun `contains reflects dynamic size changes`() {
    val items = (0..4).map { TestItem(it) }
    val provider = TestCollectionListProvider(items)

    assertThat(provider.contains(4)).isTrue()

    // Shrink the list
    provider.setItems(items.take(2))

    assertThat(provider.contains(4)).isFalse()
    assertThat(provider.contains(1)).isTrue()
  }

  @Test
  fun `ListCollectionItemProvider preserves list functionality`() {
    val items = (0..9).map { TestItem(it) }
    val cached = TestCollectionListProvider(items)

    // Should have list properties
    assertThat(cached.size).isEqualTo(10)
    assertThat(cached.lastIndex).isEqualTo(9)
    assertThat(cached.contains(5)).isTrue()

    // Should have caching
    val item = cached[5]
    assertThat(cached[5]).isSameAs(item)
  }
}
