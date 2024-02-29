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

package com.facebook.rendercore

import androidx.collection.LongSparseArray
import java.util.Collections
import java.util.HashMap

/**
 * A Cache that can be used to reuse LayoutResults or parts of them across layout calculations. It's
 * responsibility of the implementer of the Layout function to put values in the cache for a given
 * node. Values put in the LayoutCache (WriteCache) will be available for read in the next layout
 * pass as ReadCache.
 */
class LayoutCache(oldWriteCache: CachedData? = null) {
  private val writeCache: CachedData = CachedData()
  private val readCache: CachedData? = oldWriteCache

  class CacheItem(val layoutResult: LayoutResult, val widthSpec: Int, val heightSpec: Int)

  class CachedData {
    internal val cacheByNode: MutableMap<Node<*>, CacheItem> = HashMap<Node<*>, CacheItem>()
    internal val cacheById: LongSparseArray<Any?> = LongSparseArray()

    fun getCacheByNode(): Map<Node<*>, CacheItem> {
      return Collections.unmodifiableMap(cacheByNode)
    }
  }

  operator fun get(node: Node<*>): CacheItem? {
    return readCache?.cacheByNode?.get(node)
  }

  fun <T> put(node: Node<T>, cacheItem: CacheItem) {
    writeCache.cacheByNode[node] = cacheItem
  }

  fun <T> get(uniqueId: Long): T? {
    @Suppress("UNCHECKED_CAST") return readCache?.cacheById?.get(uniqueId) as? T?
  }

  fun put(uniqueId: Long, value: Any?) {
    writeCache.cacheById.put(uniqueId, value)
  }

  val writeCacheData: CachedData
    get() = writeCache
}
