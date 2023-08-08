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

package com.facebook.litho

import androidx.core.util.Pools
import com.facebook.infer.annotation.ThreadSafe
import kotlin.math.max
import kotlin.math.min

/**
 * Used to recycle objects in Litho. Can be configured to be either synchronized or not. A
 * [RecyclePool] will keep track of its own size so that it can be queried to debug pool sizes.
 */
@ThreadSafe(enableChecks = false)
open class RecyclePool<T : Any>(private val maxSize: Int, private val isSync: Boolean) {

  private val pool: Pools.Pool<T> =
      if (isSync) Pools.SynchronizedPool(maxSize) else Pools.SimplePool(maxSize)

  private var _currentSize = 0

  val currentSize: Int
    get() = _currentSize

  val isFull: Boolean
    get() = _currentSize >= maxSize

  open fun acquire(): T? {
    return if (isSync) {
      synchronized(this) {
        return acquireInternal()
      }
    } else {
      acquireInternal()
    }
  }

  open fun release(item: T): Boolean {
    return if (isSync) {
      synchronized(this) {
        return releaseInternal(item)
      }
    } else {
      releaseInternal(item)
    }
  }

  fun clear() {
    if (isSync) {
      synchronized(this) { clearInternal() }
    } else {
      clearInternal()
    }
  }

  private fun acquireInternal(): T? {
    val item = pool.acquire()
    _currentSize = max(0, _currentSize - 1)
    return item
  }

  private fun releaseInternal(item: T): Boolean {
    _currentSize = min(maxSize, _currentSize + 1)
    return pool.release(item)
  }

  private fun clearInternal() {
    while (acquire() != null) {
      // no-op.
    }
  }
}
