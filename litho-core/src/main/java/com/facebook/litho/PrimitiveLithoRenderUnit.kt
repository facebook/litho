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

import android.util.SparseArray
import androidx.annotation.IdRes
import com.facebook.rendercore.BindData
import com.facebook.rendercore.BinderKey
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountContext
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.primitives.PrimitiveRenderUnit

class PrimitiveLithoRenderUnit
private constructor(
    component: Component,
    commonDynamicProps: SparseArray<DynamicValue<Any?>>?,
    nodeInfo: NodeInfo?,
    flags: Int,
    importantForAccessibility: Int,
    val primitiveRenderUnit: PrimitiveRenderUnit<Any>,
    context: ComponentContext,
    debugKey: String?
) :
    LithoRenderUnit(
        primitiveRenderUnit.id,
        component,
        commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        primitiveRenderUnit.renderType,
        context,
        debugKey) {

  override val contentAllocator: ContentAllocator<Any> = primitiveRenderUnit.contentAllocator

  override fun doesMountRenderTreeHosts(): Boolean {
    return primitiveRenderUnit.doesMountRenderTreeHosts()
  }

  override fun <T> getExtra(@IdRes key: Int): T? {
    return primitiveRenderUnit.getExtra(key)
  }

  override fun mountBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData
  ) {
    primitiveRenderUnit.mountBinders(
        context, content, (layoutData as? LithoLayoutData)?.layoutData, bindData)
  }

  override fun unmountBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData
  ) {
    primitiveRenderUnit.unmountBinders(
        context, content, (layoutData as? LithoLayoutData)?.layoutData, bindData)
  }

  override fun attachBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData
  ) {
    primitiveRenderUnit.attachBinders(
        context, content, (layoutData as? LithoLayoutData)?.layoutData, bindData)
  }

  override fun detachBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData
  ) {
    primitiveRenderUnit.detachBinders(
        context, content, (layoutData as? LithoLayoutData)?.layoutData, bindData)
  }

  override fun updateBinders(
      context: MountContext,
      content: Any,
      currentRenderUnit: RenderUnit<Any>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      bindData: BindData,
      isAttached: Boolean
  ) {
    primitiveRenderUnit.updateBinders(
        context,
        content,
        (currentRenderUnit as PrimitiveLithoRenderUnit).primitiveRenderUnit,
        (currentLayoutData as? LithoLayoutData)?.layoutData,
        (newLayoutData as? LithoLayoutData)?.layoutData,
        mountDelegate,
        bindData,
        isAttached)
  }

  override fun <T : BinderWithContext<*, *, *>?> findAttachBinderByKey(key: BinderKey): T? {
    return primitiveRenderUnit.findAttachBinderByKey(key)
  }

  override fun containsAttachBinder(delegateBinder: DelegateBinder<*, *, *>): Boolean {
    return primitiveRenderUnit.containsAttachBinder(delegateBinder)
  }

  override fun containsOptionalMountBinder(delegateBinder: DelegateBinder<*, *, *>): Boolean {
    return primitiveRenderUnit.containsOptionalMountBinder(delegateBinder)
  }

  override val description: String = primitiveRenderUnit.description

  override fun addOptionalMountBinder(binder: DelegateBinder<*, in Any, *>) {
    primitiveRenderUnit.addOptionalMountBinder(binder)
  }

  companion object {
    fun create(
        component: Component,
        commonDynamicProps: SparseArray<DynamicValue<Any?>>?,
        context: ComponentContext,
        nodeInfo: NodeInfo?,
        flags: Int,
        importantForAccessibility: Int,
        primitiveRenderUnit: PrimitiveRenderUnit<Any>,
        debugKey: String?
    ): PrimitiveLithoRenderUnit {
      return PrimitiveLithoRenderUnit(
          component,
          commonDynamicProps,
          nodeInfo,
          flags,
          importantForAccessibility,
          primitiveRenderUnit,
          context,
          debugKey)
    }
  }
}
