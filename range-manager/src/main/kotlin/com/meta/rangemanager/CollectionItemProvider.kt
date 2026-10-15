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
 * Abstract base class for item providers in a collection.
 *
 * Extend this class to create a provider where caching is automatic. Items are cached on first
 * access and reused until explicitly invalidated. This is the required base class for providers
 * used with [RangeManager.Companion.KeyBased].
 *
 * ## Implementation
 *
 * Subclasses must implement two methods:
 * - [create]: Creates a new item for a given key (called on cache miss)
 * - [contains]: Returns whether an item exists for a given key (used for eviction)
 *
 * ## Usage
 *
 * ```kotlin
 * class MyProvider(private val repo: Repository) : CollectionItemProvider<MyKey>() {
 *     override fun create(key: MyKey): PreparableCollectionItem = createItem(key)
 *     override fun contains(key: MyKey): Boolean = repo.contains(key)
 * }
 *
 * // Use directly - caching is built-in
 * val provider = MyProvider(repo)
 * val item = provider[key]   // Creates and caches
 * val same = provider[key]   // Returns cached instance
 *
 * // Use with RangeManager
 * val rangeManager = RangeManager.KeyBased(
 *     itemProvider = provider,
 *     calculator = myCalculator,
 * )
 *
 * // Invalidation options
 * provider.onDataSetChanged()  // Evicts items where contains() returns false
 * provider.invalidate(key)     // Remove specific item
 * provider.invalidateAll()     // Clear entire cache
 * ```
 *
 * ## Caching Behavior
 * - **Creation**: Items are created via [create] on first access
 * - **Reuse**: Subsequent [get] calls return the cached instance
 * - **Eviction**: [onDataSetChanged] evicts items where [contains] returns false
 *
 * ## Lifecycle Management
 * All eviction methods call [PreparableCollectionItem.unprepare] on removed items to release
 * resources.
 *
 * ## Thread Safety
 * All cache operations are thread-safe via [SynchronizedItemCache] by default.
 *
 * @param K The type of keys used to identify cached items
 * @param cache The cache implementation. Defaults to [SynchronizedItemCache].
 * @param scope The [PrepareScope] for state persistence during prepare/unprepare. Defaults to
 *   [PrepareScope].
 * @see RangeManager.Companion.KeyBased
 */
abstract class CollectionItemProvider<K : Any>(
    private val cache: ItemCache<K> = SynchronizedItemCache(),
    private val scope: PrepareScope = PrepareScope(),
) {

  /**
   * Creates a new item for the specified key.
   *
   * This method is called when an item is not in the cache. Implement this to create your items.
   * The returned item will be automatically cached.
   *
   * @param key The key identifying the item to create
   * @return A new [PreparableCollectionItem] for the given key
   */
  protected abstract fun create(key: K): PreparableCollectionItem

  /**
   * Checks whether the collection contains an item with the specified key.
   *
   * Used by [onDataSetChanged] to determine which cached items should be evicted.
   *
   * @param key The identifier to check for
   * @return true if an item with the given key exists, false otherwise
   */
  abstract fun contains(key: K): Boolean

  /**
   * Called once RangeManager is done with [item] for [key], on every path that removes one: a
   * worker releasing a row that left the range, [onDataSetChanged], [invalidate], [invalidateAll]
   * and [evict]. For a provider that keeps per-key data next to its items, so it can drop that
   * data.
   *
   * A released item has been unprepared by then, and no worker lock is held. Runs on the thread
   * that removed the item, which is a worker thread for an asynchronous worker, so implementations
   * must be thread-safe. [item] is the instance removed; a later [get] for [key] creates a new one.
   */
  protected open fun onItemRemoved(key: K, item: PreparableCollectionItem): Unit = Unit

  /**
   * Retrieves the item for the specified key, using cache when possible.
   *
   * If the item is in cache, it's returned directly. Otherwise, calls [create] to make a new item
   * which is then cached.
   *
   * Thread-safety: This method is thread-safe due to the cache's synchronized operations.
   *
   * @param key The key identifying the item
   * @return The cached or newly created item
   */
  operator fun get(key: K): PreparableCollectionItem {
    return cache.getOrPut(key) { create(key) }
  }

  /**
   * Called when the underlying data source changes.
   *
   * Evicts cached items where [contains] returns false and calls [unprepare] on each. Items that
   * remain in cache continue to be served until explicitly invalidated.
   */
  fun onDataSetChanged() {
    val allKeys = cache.keys()
    val keysToRemove = allKeys.filter { !contains(it) }
    val removed = keysToRemove.mapNotNull { key ->
      cache.remove(key)?.let { item ->
        scope.unprepare(key, item)
        scope.clearSavedState(key)
        key to item
      }
    }
    notifyEachRemoved(removed)
  }

  /**
   * Removes the specified key from cache and calls [unprepare] on the item.
   *
   * @param key The key to remove from cache
   */
  fun invalidate(key: K) {
    val item = cache.remove(key)
    if (item != null) {
      scope.unprepare(key, item)
    }
    scope.clearSavedState(key)
    if (item != null) {
      onItemRemoved(key, item)
    }
  }

  /** Clears all items from cache and calls [unprepare] on each. */
  fun invalidateAll() {
    val removed =
        cache.keys().mapNotNull { key ->
          cache.remove(key)?.let { item ->
            scope.unprepare(key, item)
            key to item
          }
        }
    scope.clearAll()
    notifyEachRemoved(removed)
  }

  /**
   * Calls [onItemRemoved] for every item, even if one throws, and then rethrows the first failure:
   * the items are already gone, so a provider that fails on one still hears about the rest.
   */
  private fun notifyEachRemoved(removed: List<Pair<K, PreparableCollectionItem>>) {
    var failure: Throwable? = null
    removed.forEach { (key, item) ->
      try {
        onItemRemoved(key, item)
      } catch (thrown: Throwable) {
        failure?.addSuppressed(thrown) ?: run { failure = thrown }
      }
    }
    failure?.let { throw it }
  }

  /**
   * Removes the specified key from cache without calling [unprepare].
   *
   * This is used by workers that have already called [unprepare] on the item and only need to evict
   * it from the cache. Unlike [invalidate], this method does not call [unprepare] on the item.
   *
   * @param key The key to remove from cache
   */
  fun evict(key: K) {
    cache.remove(key)?.let { item -> onItemRemoved(key, item) }
  }

  /**
   * Removes [expectedItem] without unpreparing it, but only if that exact instance remains cached.
   *
   * Workers use this after unpreparing a captured item so stale work cannot evict a replacement.
   */
  internal val supportsExactEviction: Boolean
    get() = cache.supportsExactRemoval

  /**
   * Whether the provider may still hold an item for [key], so a worker can release it. A provider
   * without a cache ([NoCache]) creates items on demand and can't say, so it counts as holding
   * every key, like [isCachedSame].
   */
  internal fun isCached(key: K): Boolean = cache is NoCache || cache.get(key) != null

  internal fun isCachedSame(key: K, expectedItem: PreparableCollectionItem): Boolean =
      cache is NoCache || cache.get(key) === expectedItem

  /**
   * Atomically transfers exact-item cleanup ownership to the worker.
   *
   * Does not call [onItemRemoved]: the asynchronous worker claims an item under its own locks and
   * before it unprepares it, so it reports the removal through [notifyRemoved] once it is done.
   */
  internal fun claimReleaseIfSame(key: K, expectedItem: PreparableCollectionItem): Boolean =
      cache is NoCache || cache.removeIfSame(key, expectedItem)

  /** Removes [expectedItem] if still cached. Like [claimReleaseIfSame], reported later. */
  internal fun evictIfSame(key: K, expectedItem: PreparableCollectionItem): Boolean =
      cache.removeIfSame(key, expectedItem)

  /**
   * Reports that a worker finished removing [item] it took out with [claimReleaseIfSame] (after
   * unpreparing it) or [evictIfSame]. Called outside the worker's locks.
   */
  internal fun notifyRemoved(key: K, item: PreparableCollectionItem) {
    if (cache !is NoCache) {
      onItemRemoved(key, item)
    }
  }
}
