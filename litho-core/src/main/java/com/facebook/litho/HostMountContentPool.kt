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
import com.facebook.rendercore.MountContentPools.DefaultItemPool
import com.facebook.rendercore.MountContentPools.ItemPool

/**
 * A specific MountContentPool for HostComponent - needed to do correct recycling with things like
 * duplicateParentState.
 */
class HostMountContentPool(maxSize: Int, isEnabled: Boolean) : ItemPool {

  private val pool: DefaultItemPool? =
      if (isEnabled) {
        DefaultItemPool(ComponentHost::class.java, maxSize)
      } else {
        null
      }

  override fun acquire(contentAllocator: ContentAllocator<*>): Any? =
      pool?.acquire(contentAllocator)

  override fun release(item: Any): Boolean {
    if (pool == null) {
      return false
    }

    // See ComponentHost#hadChildWithDuplicateParentState() for reason for this check
    if ((item as ComponentHost).hadChildWithDuplicateParentState()) {
      return false
    }

    return pool.release(item)
  }

  override fun maybePreallocateContent(c: Context, contentAllocator: ContentAllocator<*>): Boolean =
      pool?.maybePreallocateContent(c, contentAllocator) ?: false
}
