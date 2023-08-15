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

package com.facebook.litho.testing

import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountItemsPool

class TrackedItemPool(lifecycle: Any, size: Int) :
    MountItemsPool.DefaultItemPool(lifecycle::class.java, size, false) {

  var currentSize: Int = 0
    private set

  override fun acquire(contentAllocator: ContentAllocator<*>?): Any? {
    return super.acquire(contentAllocator).apply {
      if (this != null) {
        currentSize--
      }
    }
  }

  override fun release(item: Any): Boolean {
    return super.release(item).apply {
      if (this) {
        currentSize++
      }
    }
  }
}
