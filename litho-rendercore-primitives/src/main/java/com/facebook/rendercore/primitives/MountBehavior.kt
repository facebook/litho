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

import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.CommonUtils.getSectionNameForTracing

/**
 * MountBehavior defines how to allocate a [View]/[Drawable] and apply properties to it.
 *
 * @property id Unique id identifying the [RenderUnit] in the tree of Node it is part of.
 * @property description A lambda that returns the description of the underlying [RenderUnit].
 *   Mainly for debugging purposes such as tracing and logs. Maximum description length is 127
 *   characters. Everything above that will be truncated.
 * @property contentAllocator Provides a [View]/[Drawable] content.
 * @property mountConfigurationCall A function that allows for applying properties to the content.
 */
class MountBehavior<ContentType : Any>(
    private val id: Long,
    private val description: () -> String?,
    private val contentAllocator: ContentAllocator<ContentType>,
    private val mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
) {

  constructor(
      id: Long,
      description: String?,
      contentAllocator: ContentAllocator<ContentType>,
      mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
  ) : this(id, { description }, contentAllocator, mountConfigurationCall)

  constructor(
      id: Long,
      contentAllocator: ContentAllocator<ContentType>,
      mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
  ) : this(id, null, contentAllocator, mountConfigurationCall)

  internal val renderUnit: PrimitiveRenderUnit<ContentType>

  init {
    val mountConfigurationScope = MountConfigurationScope<ContentType>()
    mountConfigurationScope.mountConfigurationCall()

    renderUnit =
        object :
            PrimitiveRenderUnit<ContentType>(
                contentAllocator.renderType,
                mountConfigurationScope.fixedBinders,
                mountConfigurationScope.doesMountRenderTreeHosts) {
          override val contentAllocator: ContentAllocator<ContentType>
            get() = this@MountBehavior.contentAllocator

          override val id: Long
            get() = this@MountBehavior.id

          override val description: String
            get() =
                this@MountBehavior.description()?.take(MAX_DESCRIPTION_LENGTH)
                    ?: getSectionNameForTracing(contentAllocator.getPoolableContentType())
        }
  }
}

abstract class PrimitiveRenderUnit<ContentType : Any>(
    renderType: RenderType,
    fixedMountBinders: List<DelegateBinder<*, ContentType, in Any>>,
    private val doesMountRenderTreeHosts: Boolean
) :
    RenderUnit<ContentType>(
        renderType,
        fixedMountBinders,
        emptyList(), // optional binders
        emptyList() // attach binders
        ) {

  override fun doesMountRenderTreeHosts(): Boolean = doesMountRenderTreeHosts
}
