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
import androidx.annotation.IdRes

/**
 * This wrapper allows to add binders to the original [renderUnit] without modifying it directly.
 * [addOptionalMountBinder] and [addAttachBinder] methods gather additional Binders in the
 * [WrapperRenderUnit] instance and all other methods are delegated to the original [renderUnit].
 */
// Don't add support for fixedMountBinders in WrapperRenderUnit unless you add support for them in
// BindData handling logic.
class WrapperRenderUnit<ContentType>(private val renderUnit: RenderUnit<ContentType>) :
    RenderUnit<ContentType>(renderUnit.renderType) {

  override fun getId(): Long = renderUnit.id

  override fun getContentAllocator(): ContentAllocator<ContentType> = renderUnit.contentAllocator

  override fun doesMountRenderTreeHosts(): Boolean = renderUnit.doesMountRenderTreeHosts()

  override fun getRenderContentType(): Class<*> = renderUnit.renderContentType

  override fun getDescription(): String = renderUnit.description

  override fun <T> getExtra(@IdRes key: Int): T? = renderUnit.getExtra(key)

  override fun addOptionalMountBinder(binder: DelegateBinder<*, in ContentType, *>) {
    if (renderUnit.containsOptionalMountBinder(binder)) {
      throw IllegalArgumentException(
          "Binder ${binder.getDescription()} already exists in the wrapped ${renderUnit.getDescription()}")
    }
    super.addOptionalMountBinder(binder)
  }

  override fun addAttachBinder(binder: DelegateBinder<*, in ContentType, *>) {
    if (renderUnit.containsAttachBinder(binder)) {
      throw IllegalArgumentException(
          "Binder ${binder.getDescription()} already exists in the wrapped ${renderUnit.getDescription()}")
    }
    super.addAttachBinder(binder)
  }

  override fun <T : Binder<*, *, *>> findAttachBinderByClass(klass: Class<T>): T? =
      renderUnit.findAttachBinderByClass(klass) ?: super.findAttachBinderByClass(klass)

  override fun mountBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) {
    renderUnit.mountBinders(context, content, layoutData, bindData, tracer)
    super.mountBinders(context, content, layoutData, bindData, tracer)
  }

  override fun unmountBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) {
    super.unmountBinders(context, content, layoutData, bindData, tracer)
    renderUnit.unmountBinders(context, content, layoutData, bindData, tracer)
  }

  override fun attachBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) {
    renderUnit.attachBinders(context, content, layoutData, bindData, tracer)
    super.attachBinders(context, content, layoutData, bindData, tracer)
  }

  override fun detachBinders(
      context: Context,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
      tracer: Systracer
  ) {
    super.detachBinders(context, content, layoutData, bindData, tracer)
    renderUnit.detachBinders(context, content, layoutData, bindData, tracer)
  }

  override fun updateBinders(
      context: Context,
      content: ContentType,
      currentRenderUnit: RenderUnit<ContentType>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      bindData: BindData,
      isAttached: Boolean,
      tracer: Systracer,
  ) {
    renderUnit.updateBinders(
        context,
        content,
        (currentRenderUnit as WrapperRenderUnit).renderUnit,
        currentLayoutData,
        newLayoutData,
        mountDelegate,
        bindData,
        isAttached,
        tracer,
    )
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
}
