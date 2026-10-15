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

import androidx.annotation.IdRes
import androidx.collection.ScatterSet

/**
 * This wrapper allows to add binders to the original [renderUnit] without modifying it directly.
 * [addOptionalMountBinder] and [addAttachBinder] methods gather additional Binders in the
 * [WrapperRenderUnit] instance and all other methods are delegated to the original [renderUnit].
 */
// Don't add support for fixedMountBinders in WrapperRenderUnit unless you add support for them in
// BindData handling logic.
class WrapperRenderUnit<ContentType : Any>
constructor(private val renderUnit: RenderUnit<ContentType>) : RenderUnit<ContentType>() {

  override val id: Long = renderUnit.id

  override val contentAllocator: ContentAllocator<ContentType> = renderUnit.contentAllocator

  override fun doesMountRenderTreeHosts(): Boolean = renderUnit.doesMountRenderTreeHosts()

  override val description: String = renderUnit.description

  override fun <T> getExtra(@IdRes key: Int): T? = renderUnit.getExtra(key)

  override fun addOptionalMountBinder(binder: DelegateBinder<*, in ContentType, *>) {
    if (renderUnit.containsOptionalMountBinder(binder)) {
      throw IllegalArgumentException(
          "Binder ${binder.binder.description} already exists in the wrapped ${renderUnit.description}",
      )
    }
    super.addOptionalMountBinder(binder)
  }

  override fun addAttachBinder(binder: DelegateBinder<*, in ContentType, *>) {
    if (renderUnit.containsAttachBinder(binder)) {
      throw IllegalArgumentException(
          "Binder ${binder.binder.description} already exists in the wrapped ${renderUnit.description}",
      )
    }
    super.addAttachBinder(binder)
  }

  override fun <T : Binder<*, *, *>?> findAttachBinderByKey(key: BinderKey): T? =
      renderUnit.findAttachBinderByKey(key) ?: super.findAttachBinderByKey(key)

  override fun mountBinders(
      context: MountContext,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
  ) {
    renderUnit.mountBinders(context, content, layoutData, bindData)
    super.mountBinders(context, content, layoutData, bindData)
  }

  override fun unmountBinders(
      context: MountContext,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
  ) {
    super.unmountBinders(context, content, layoutData, bindData)
    renderUnit.unmountBinders(context, content, layoutData, bindData)
  }

  override fun attachBinders(
      context: MountContext,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
  ) {
    renderUnit.attachBinders(context, content, layoutData, bindData)
    super.attachBinders(context, content, layoutData, bindData)
  }

  override fun detachBinders(
      context: MountContext,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
  ) {
    super.detachBinders(context, content, layoutData, bindData)
    renderUnit.detachBinders(context, content, layoutData, bindData)
  }

  override fun rebindBinders(
      context: MountContext,
      binderIds: ScatterSet<BinderId>,
      content: ContentType,
      layoutData: Any?,
      bindData: BindData,
  ) {
    renderUnit.rebindBinders(context, binderIds, content, layoutData, bindData)
    super.rebindBinders(context, binderIds, content, layoutData, bindData)
  }

  override fun updateBinders(
      context: MountContext,
      content: ContentType,
      currentRenderUnit: RenderUnit<ContentType>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      bindData: BindData,
      isAttached: Boolean,
      extraBindersToUpdate: ScatterSet<BinderId>,
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
        extraBindersToUpdate,
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
        extraBindersToUpdate,
    )
  }
}
