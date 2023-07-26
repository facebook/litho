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

abstract class ResourceCache protected constructor(private val configuration: Configuration) {

  abstract operator fun <T> get(key: Int): T?

  abstract operator fun set(key: Int, value: Any)

  companion object {
    private var latest: ResourceCache? = null

    @JvmStatic
    @Synchronized
    fun getLatest(configuration: Configuration): ResourceCache? {
      if (latest?.configuration != configuration) {
        latest = LruResourceCache(Configuration(configuration))
      }

      return latest
    }
  }
}
