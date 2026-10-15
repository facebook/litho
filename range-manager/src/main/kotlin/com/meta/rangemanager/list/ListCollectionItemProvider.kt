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

import com.meta.rangemanager.CollectionItemProvider
import com.meta.rangemanager.ItemCache
import com.meta.rangemanager.PrepareScope
import com.meta.rangemanager.RangeManager
import com.meta.rangemanager.SynchronizedItemCache

/**
 * Abstract base class for list item providers with built-in caching.
 *
 * Extend this class to create a list provider where caching is automatic. Items are cached on first
 * access and reused until explicitly invalidated. This is the required base class for providers
 * used with [RangeManager.ListBased].
 *
 * ## Implementation
 *
 * Subclasses must implement:
 * - [size]: Returns the total number of items in the collection
 * - [create]: Creates a new item for a given index (called on cache miss)
 *
 * ## Usage
 *
 * ```kotlin
 * class MyListProvider(private val items: List<Data>) : ListCollectionItemProvider() {
 *     override val size: Int get() = items.size
 *     override fun create(key: Int): PreparableCollectionItem = createItem(items[key])
 * }
 *
 * // Use directly - caching is built-in
 * val provider = MyListProvider(items)
 * val item = provider[0]    // Creates and caches
 * val same = provider[0]    // Returns cached instance
 *
 * // Use with RangeManager
 * val rangeManager = RangeManager.ListBased(
 *     itemProvider = provider,
 *     config = ListRangeConfig(behind = 10, ahead = 20),
 *     scope = myPrepareScope,
 * )
 *
 * // Invalidation options
 * provider.onDataSetChanged()  // Evicts items where index >= size
 * provider.invalidate(0)       // Remove specific item
 * provider.invalidateAll()     // Clear entire cache
 * ```
 *
 * ## Caching Behavior
 * - **Creation**: Items are created via [create] on first access
 * - **Reuse**: Subsequent [get] calls return the cached instance
 * - **Eviction**: [onDataSetChanged] evicts items where index >= [size]
 *
 * @param cache The cache implementation. Defaults to [SynchronizedItemCache].
 * @param scope The [PrepareScope] for state persistence. Defaults to [PrepareScope].
 * @see CollectionItemProvider
 * @see RangeManager.ListBased
 */
abstract class ListCollectionItemProvider(
    cache: ItemCache<Int> = SynchronizedItemCache(),
    scope: PrepareScope = PrepareScope(),
) : CollectionItemProvider<Int>(cache, scope) {

  /** The total number of items in the collection. */
  abstract val size: Int

  /**
   * The index of the last item in the collection.
   *
   * This is computed as size - 1. For an empty collection, this will be -1.
   */
  val lastIndex: Int
    get() = size - 1

  /**
   * Checks whether the given index is valid for this collection.
   *
   * Used by [onDataSetChanged] to evict cached items that are no longer valid (index >= size).
   *
   * @param key The index to check
   * @return true if the index is within bounds (0 <= index < size)
   */
  final override fun contains(key: Int): Boolean = key >= 0 && key < size
}
