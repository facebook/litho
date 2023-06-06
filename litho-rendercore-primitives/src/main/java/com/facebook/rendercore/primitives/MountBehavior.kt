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
import com.facebook.rendercore.BindData
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.Systracer

/**
 * MountBehavior defines how to allocate a [View]/[Drawable] and apply properties to it.
 *
 * @property id Unique id identifying the [RenderUnit] in the tree of Node it is part of.
 * @property description A description of the underlying [RenderUnit]. Mainly for debugging purposes
 *   such as tracing and logs. Maximum description length is 127 characters. Everything above that
 *   will be truncated.
 * @property contentAllocator Provides a [View]/[Drawable] content.
 * @property mountConfigurationCall A function that allows for applying properties to the content.
 */
class MountBehavior<ContentType : Any>(
    private val id: Long,
    private val description: String?,
    private val contentAllocator: ContentAllocator<ContentType>,
    private val mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
) {

  constructor(
      id: Long,
      contentAllocator: ContentAllocator<ContentType>,
      mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
  ) : this(id, null, contentAllocator, mountConfigurationCall)

  internal val renderUnit: PrimitiveRenderUnit<ContentType> by
      lazy(LazyThreadSafetyMode.NONE) {
        val mountConfigurationScope = MountConfigurationScope<ContentType>()
        mountConfigurationScope.mountConfigurationCall()

        object :
            PrimitiveRenderUnit<ContentType>(
                contentAllocator.renderType,
                mountConfigurationScope.fixedBinders,
                mountConfigurationScope.doesMountRenderTreeHosts) {
          override fun getContentAllocator(): ContentAllocator<ContentType> {
            return this@MountBehavior.contentAllocator
          }

          override fun getId(): Long {
            return this@MountBehavior.id
          }

          override fun getDescription(): String {
            return this@MountBehavior.description?.take(MAX_DESCRIPTION_LENGTH)
                ?: super.getDescription()
          }
        }
      }
}

abstract class PrimitiveRenderUnit<ContentType>(
    renderType: RenderType,
    fixedMountBinders: List<DelegateBinder<*, ContentType, *>>,
    private val doesMountRenderTreeHosts: Boolean
) :
    RenderUnit<ContentType>(
        renderType,
        fixedMountBinders,
        emptyList(), // optional binders
        emptyList() // attach binders
        ) {

  override fun doesMountRenderTreeHosts(): Boolean = doesMountRenderTreeHosts

  /**
   * This method is an override that calls super impl to make it public on RenderUnit because it
   * needs to be called in PrimitiveLithoRenderUnit.
   */
  public override fun mountBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) = super.mountBinders(context, content, layoutData, bindData, tracer)

  /**
   * This method is an override that calls super impl to make it public on RenderUnit because it
   * needs to be called in PrimitiveLithoRenderUnit.
   */
  public override fun unmountBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) = super.unmountBinders(context, content, layoutData, bindData, tracer)

  /**
   * This method is an override that calls super impl to make it public on RenderUnit because it
   * needs to be called in PrimitiveLithoRenderUnit.
   */
  public override fun attachBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) = super.attachBinders(context, content, layoutData, bindData, tracer)

  /**
   * This method is an override that calls super impl to make it public on RenderUnit because it
   * needs to be called in PrimitiveLithoRenderUnit.
   */
  public override fun detachBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) = super.detachBinders(context, content, layoutData, bindData, tracer)

  /**
   * This method is an override that calls super impl to make it public on RenderUnit because it
   * needs to be called in PrimitiveLithoRenderUnit.
   */
  public override fun updateBinders(
      context: Context,
      content: ContentType,
      currentRenderUnit: RenderUnit<ContentType>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      bindData: BindData,
      isAttached: Boolean,
      tracer: Systracer,
  ) =
      super.updateBinders(
          context,
          content,
          currentRenderUnit,
          currentLayoutData,
          newLayoutData,
          mountDelegate,
          bindData,
          isAttached,
          tracer,
      )
}
