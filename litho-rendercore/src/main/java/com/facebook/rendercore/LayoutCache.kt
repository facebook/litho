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

import androidx.annotation.VisibleForTesting
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf

/**
 * A Cache that can be used to reuse LayoutResults or parts of them across layout calculations. It's
 * responsibility of the implementer of the Layout function to put values in the cache for a given
 * node. Values put in the LayoutCache (WriteCache) will be available for read in the next layout
 * pass as ReadCache.
 */
class LayoutCache(@JvmField val readCache: LayoutCacheData? = null) {
  private val writeCache: MutableMap<Any, Any?> = HashMap()
  private var continuationCache: MutableScatterMap<Any, Any?>? = null

  open class CacheItem(
      open val layoutResult: LayoutResult,
      open val widthSpec: Int,
      open val heightSpec: Int,
  )

  fun put(key: Any, cacheItem: Any?) {
    writeCache[key] = cacheItem
  }

  operator fun <T> get(uniqueId: Any): T? = readCache?.get(uniqueId)

  fun writeContinuationValue(key: Any, value: Any?) {
    val cache = continuationCache ?: mutableScatterMapOf()
    continuationCache = cache
    cache.put(key, value)
  }

  fun <T> readContinuationValue(key: Any): T? = readCache?.continuationValueFor(key)

  val writeCacheData: Map<Any, Any?>
    get() = writeCache

  val continuationValues: ScatterMap<Any, Any?>
    get() = continuationCache ?: emptyScatterMap()

  fun toCacheData(): LayoutCacheData = LayoutCacheData(writeCacheData, continuationValues)
}

class LayoutCacheData(
    @JvmField val cache: Map<Any, Any?>,
    @JvmField val continuationValues: ScatterMap<Any, Any?>,
) {
  @Suppress("UNCHECKED_CAST") operator fun <T> get(key: Any): T? = cache[key] as? T?

  @Suppress("UNCHECKED_CAST")
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun <T> continuationValueFor(key: Any): T? = continuationValues[key] as? T?
}
