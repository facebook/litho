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

import android.content.Context
import com.facebook.rendercore.MountItemsPool.DefaultItemPool
import com.facebook.rendercore.MountItemsPool.ItemPool
import com.facebook.rendercore.RenderUnit.RenderType

/**
 * Defines a class that can provide mountable content and have it be pooled.
 *
 * Instances must implement at least createContent and getRenderType methods to allocate the
 * RenderUnit content (View or Drawable) and to make it possible to query the type of the content.
 */
interface ContentAllocator<Content : Any> {

  /** Allocates the mountable content (View or Drawable). */
  fun createContent(context: Context): Content

  /** Returns the [RenderUnit.RenderType] of the mountable content. */
  fun getRenderType(): RenderType

  /** Creates a mount-content that can be pooled. This is typically a View or Drawable subclass. */
  fun createPoolableContent(context: Context): Content = createContent(context)

  /** Returns the class of the content of the mountable content. */
  fun getPoolableContentType(): Class<*> {
    return javaClass
  }

  val isRecyclingDisabled: Boolean
    get() = false

  /**
   * Creates an ItemPool for this mountable content. Returning null will generate a default pool.
   */
  fun createRecyclingPool(): ItemPool? = onCreateMountContentPool()

  /**
   * This API informs the framework to fill the content pool for this Mountable ahead of time. The
   * default value is `false`, i.e. content is not pre-allocated. Pre-allocation of the content can
   * improve performance in some circumstances where creating the content is expensive.
   *
   * @return `true` to preallocate the content, otherwise `false`
   */
  fun canPreallocate(): Boolean = false

  /** This API informs the framework about the size of the content pool. The default is 3. */
  fun poolSize(): Int = DEFAULT_MAX_PREALLOCATION

  /** Creates the content pool the framework should use for this Mountable. */
  fun onCreateMountContentPool(): ItemPool = DefaultItemPool(javaClass, poolSize())

  companion object {
    /** Default size of the content pool. */
    const val DEFAULT_MAX_PREALLOCATION: Int = 3
  }
}
