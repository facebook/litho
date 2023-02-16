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

import android.content.Context
import com.facebook.rendercore.ContentAllocator

class TrackingMountContentPool(name: String, maxSize: Int, sync: Boolean) :
    DefaultMountContentPool(name, maxSize, sync) {
  var acquireCount: Int = 0
    private set
  var releaseCount: Int = 0
    private set

  override fun acquire(c: Context, component: ContentAllocator<*>?): Any {
    val item = super.acquire(c, component)
    acquireCount++
    return item
  }

  override fun release(item: Any): Boolean {
    releaseCount++
    return super.release(item)
  }
}
