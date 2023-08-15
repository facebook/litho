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

package com.facebook.rendercore.primitives

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.ContentAllocator.DEFAULT_MAX_PREALLOCATION
import com.facebook.rendercore.RenderUnit

/**
 * A [ContentAllocator] that allocates an instance of a [View].
 *
 * @property poolSize The size of the content pool. The default is [DEFAULT_MAX_PREALLOCATION].
 *   Setting this to 0 disables pooling of the content [View].
 * @property canPreallocate The flag indicating if the content pool for this Primitive should be
 *   filled ahead of time. See [ContentAllocator.canPreallocate]. The default is false.
 * @property allocator The lambda callback that returns the actual [View].
 */
class ViewAllocator<Content : View>(
    private val poolSize: Int = DEFAULT_MAX_PREALLOCATION,
    private val canPreallocate: Boolean = false,
    private val allocator: Allocator<Content>
) : ContentAllocator<Content> {
  override fun createContent(context: Context): Content = allocator.allocate(context)

  override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.VIEW

  override fun poolSize(): Int = poolSize

  override fun canPreallocate(): Boolean = canPreallocate

  override fun getPoolableContentType(): Class<*> = allocator.javaClass
}

/**
 * A [ContentAllocator] that allocates an instance of a [Drawable].
 *
 * @property poolSize The size of the content pool. The default is [DEFAULT_MAX_PREALLOCATION].
 *   Setting this to 0 disables pooling of the content [Drawable].
 * @property canPreallocate The flag indicating if the content pool for this Primitive should be
 *   filled ahead of time. See [ContentAllocator.canPreallocate]. The default is false.
 * @property allocator The lambda callback that returns the actual [Drawable].
 */
class DrawableAllocator<Content : Drawable>(
    private val poolSize: Int = DEFAULT_MAX_PREALLOCATION,
    private val canPreallocate: Boolean = false,
    private val allocator: Allocator<Content>
) : ContentAllocator<Content> {
  override fun createContent(context: Context): Content = allocator.allocate(context)

  override fun getRenderType(): RenderUnit.RenderType = RenderUnit.RenderType.DRAWABLE

  override fun poolSize(): Int = poolSize

  override fun canPreallocate(): Boolean = canPreallocate

  override fun getPoolableContentType(): Class<*> = allocator.javaClass
}

/**
 * An interface used to mark the allocator lambdas, so that they're not optimized. Each [Allocator]
 * lambda should be compiled to a separate class without being merged with others.
 */
fun interface Allocator<ContentType> {
  fun allocate(context: Context): ContentType
}
