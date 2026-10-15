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

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf

/**
 * Thread-safe [ItemCache] implementation using synchronized methods.
 *
 * Uses a [MutableScatterMap] for efficient key-based lookups with synchronized access to ensure
 * thread safety. All operations are atomic with respect to other cache operations.
 *
 * ## Thread Safety
 *
 * All methods are synchronized to prevent race conditions. The [getOrPut] method is atomic,
 * ensuring that the factory function is only called once per key even under concurrent access.
 *
 * ## Memory Management
 *
 * This implementation does not limit cache size. Items remain in cache until explicitly removed via
 * [remove] or [clear]. For memory-sensitive use cases, consider implementing a custom [ItemCache]
 * with LRU eviction.
 *
 * @param K The type of keys used to identify cached items
 * @see ItemCache
 * @see CollectionItemProvider
 */
class SynchronizedItemCache<K : Any> : ItemCache<K> {

  private val cache: MutableScatterMap<K, PreparableCollectionItem> = mutableScatterMapOf()

  @Synchronized
  override fun getOrPut(
      key: K,
      create: () -> PreparableCollectionItem,
  ): PreparableCollectionItem {
    return cache[key] ?: create().also { cache[key] = it }
  }

  @Synchronized override fun get(key: K): PreparableCollectionItem? = cache[key]

  @Synchronized
  override fun put(key: K, item: PreparableCollectionItem) {
    cache[key] = item
  }

  @Synchronized override fun remove(key: K): PreparableCollectionItem? = cache.remove(key)

  override val supportsExactRemoval: Boolean = true

  @Synchronized
  override fun removeIfSame(key: K, expectedItem: PreparableCollectionItem): Boolean {
    if (cache[key] !== expectedItem) {
      return false
    }
    cache.remove(key)
    return true
  }

  @Synchronized
  override fun keys(): Set<K> {
    val result = mutableSetOf<K>()
    cache.forEachKey { key -> result.add(key) }
    return result
  }

  @Synchronized override fun clear(): Unit = cache.clear()
}
