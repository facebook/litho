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

/**
 * Cache interface for storing and retrieving [PreparableCollectionItem] instances by key.
 *
 * This interface defines the contract for item caching used by [CollectionItemProvider].
 * Implementations should be thread-safe as cache operations may be called from multiple threads.
 *
 * ## Usage
 *
 * The cache is primarily used through [getOrPut] which atomically retrieves an existing item or
 * creates and stores a new one:
 * ```kotlin
 * val cache: ItemCache<String> = SynchronizedItemCache()
 * val item = cache.getOrPut("key") { createItem() }
 * ```
 *
 * ## Thread Safety
 *
 * All methods should be thread-safe.
 *
 * @param K The type of keys used to identify cached items
 */
interface ItemCache<K : Any> {

  /**
   * Retrieves the item for the given key, or creates and stores it if not present.
   *
   * This is the primary method for cache access. It atomically checks for an existing item and
   * creates a new one if needed, avoiding race conditions where multiple threads might create
   * duplicate items.
   *
   * @param key The key identifying the item
   * @param create Factory function to create the item if not in cache
   * @return The cached or newly created item
   */
  fun getOrPut(key: K, create: () -> PreparableCollectionItem): PreparableCollectionItem

  /**
   * Retrieves the item for the given key without creating it.
   *
   * @param key The key identifying the item
   * @return The cached item, or null if not present
   */
  fun get(key: K): PreparableCollectionItem?

  /**
   * Stores an item in the cache.
   *
   * @param key The key to associate with the item
   * @param item The item to store
   */
  fun put(key: K, item: PreparableCollectionItem)

  /**
   * Removes and returns the item for the given key.
   *
   * @param key The key of the item to remove
   * @return The removed item, or null if not present
   */
  fun remove(key: K): PreparableCollectionItem?

  /**
   * Atomically removes the item for [key] only when it is the exact [expectedItem] instance.
   *
   * The default fails closed so existing custom caches remain source and binary compatible without
   * risking removal of a replacement. Custom caches should override this atomically to participate
   * in identity-aware worker eviction; composing [get] and [remove] is not atomic.
   *
   * @param key The key of the item to remove
   * @param expectedItem The exact item instance expected to remain cached
   * @return true if [expectedItem] was cached for [key] and removed, false otherwise
   */
  fun removeIfSame(key: K, expectedItem: PreparableCollectionItem): Boolean = false

  /**
   * Whether [removeIfSame] is implemented atomically, or removal is unnecessary as in [NoCache].
   */
  val supportsExactRemoval: Boolean
    get() = false

  /**
   * Returns all keys currently in the cache.
   *
   * Note: The returned set is a snapshot and may not reflect concurrent modifications.
   *
   * @return Set of all cached keys
   */
  fun keys(): Set<K>

  /** Removes all items from the cache. */
  fun clear()
}

/**
 * A no-op [ItemCache] implementation that disables caching.
 *
 * This implementation never stores items. Every call to [getOrPut] invokes the factory function to
 * create a new item. Use this when caching is not desired, such as when items should always be
 * freshly created or when the caller manages item lifecycle externally.
 *
 * ## Thread Safety
 *
 * This implementation is inherently thread-safe as it maintains no mutable state.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyProvider : CollectionItemProvider<MyKey>(cache = NoCache()) {
 *     override fun create(key: MyKey): PreparableCollectionItem = createItem(key)
 *     override fun contains(key: MyKey): Boolean = true
 * }
 * ```
 *
 * @param K The type of keys used to identify items
 * @see ItemCache
 * @see SynchronizedItemCache
 * @see CollectionItemProvider
 */
class NoCache<K : Any> : ItemCache<K> {

  override fun getOrPut(key: K, create: () -> PreparableCollectionItem): PreparableCollectionItem =
      create()

  override fun get(key: K): PreparableCollectionItem? = null

  override fun put(key: K, item: PreparableCollectionItem) {
    // No-op: caching is disabled
  }

  override fun remove(key: K): PreparableCollectionItem? = null

  override fun removeIfSame(key: K, expectedItem: PreparableCollectionItem): Boolean = false

  override val supportsExactRemoval: Boolean = true

  override fun keys(): Set<K> = emptySet()

  override fun clear() {
    // No-op: nothing to clear
  }
}
