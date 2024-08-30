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

import kotlin.collections.HashMap

/**
 * A Cache that can be used to reuse LayoutResults or parts of them across layout calculations. It's
 * responsibility of the implementer of the Layout function to put values in the cache for a given
 * node. Values put in the LayoutCache (WriteCache) will be available for read in the next layout
 * pass as ReadCache.
 */
class LayoutCache(oldWriteCache: Map<Any, Any?>? = null) {
  private val writeCache: MutableMap<Any, Any?> = HashMap()
  private val readCache: Map<Any, Any?> = oldWriteCache ?: emptyMap()

  class CacheItem(val layoutResult: LayoutResult, val widthSpec: Int, val heightSpec: Int)

  fun put(key: Any, cacheItem: Any?) {
    writeCache[key] = cacheItem
  }

  operator fun <T> get(uniqueId: Any): T? {
    @Suppress("UNCHECKED_CAST")
    return readCache.get(uniqueId) as? T?
  }

  val writeCacheData: Map<Any, Any?>
    get() = writeCache
}
