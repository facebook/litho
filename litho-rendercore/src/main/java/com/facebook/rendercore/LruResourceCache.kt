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

import android.content.res.Configuration
import androidx.collection.LruCache

open class LruResourceCache(configuration: Configuration) : ResourceCache(configuration) {

  private val cache =
      object : LruCache<Int?, Any?>(500) {

        override fun sizeOf(key: Int, value: Any): Int = if (value is String) value.length else 1
      }

  override fun <T> get(key: Int): T? = cache[key] as T?

  override fun set(key: Int, value: Any) {
    cache.put(key, value)
  }
}
