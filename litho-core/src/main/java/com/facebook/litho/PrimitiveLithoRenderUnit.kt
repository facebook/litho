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
import androidx.collection.ScatterSet
import com.facebook.rendercore.BindData
import com.facebook.rendercore.BinderId
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
    debugKey: String?,
    excludesFromIncrementalMount: Boolean,
) :
    LithoRenderUnit(
        primitiveRenderUnit.id,
        component,
        commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        context,
        debugKey,
        excludesFromIncrementalMount,
    ) {

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
      bindData: BindData,
  ) {
    try {
      primitiveRenderUnit.mountBinders(
          context,
          content,
          (layoutData as? LithoLayoutData)?.layoutData,
          bindData,
      )
    } catch (e: Exception) {
      // Error boundary will handle component removal; partial binder state is acceptable
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun unmountBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData,
  ) {
    try {
      primitiveRenderUnit.unmountBinders(
          context,
          content,
          (layoutData as? LithoLayoutData)?.layoutData,
          bindData,
      )
    } catch (e: Exception) {
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun attachBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData,
  ) {
    try {
      primitiveRenderUnit.attachBinders(
          context,
          content,
          (layoutData as? LithoLayoutData)?.layoutData,
          bindData,
      )
    } catch (e: Exception) {
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun detachBinders(
      context: MountContext,
      content: Any,
      layoutData: Any?,
      bindData: BindData,
  ) {
    try {
      primitiveRenderUnit.detachBinders(
          context,
          content,
          (layoutData as? LithoLayoutData)?.layoutData,
          bindData,
      )
    } catch (e: Exception) {
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun rebindBinders(
      context: MountContext,
      binderIds: ScatterSet<BinderId>,
      content: Any,
      layoutData: Any?,
      bindData: BindData,
  ) {
    try {
      primitiveRenderUnit.rebindBinders(
          context,
          binderIds,
          content,
          (layoutData as? LithoLayoutData)?.layoutData,
          bindData,
      )
    } catch (e: Exception) {
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun updateBinders(
      context: MountContext,
      content: Any,
      currentRenderUnit: RenderUnit<Any>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      bindData: BindData,
      isAttached: Boolean,
      extraBindersToUpdate: ScatterSet<BinderId>,
  ) {
    try {
      primitiveRenderUnit.updateBinders(
          context,
          content,
          (currentRenderUnit as? PrimitiveLithoRenderUnit
                  ?: error("Expected PrimitiveLithoRenderUnit for currentRenderUnit"))
              .primitiveRenderUnit,
          (currentLayoutData as? LithoLayoutData)?.layoutData,
          (newLayoutData as? LithoLayoutData)?.layoutData,
          mountDelegate,
          bindData,
          isAttached,
          extraBindersToUpdate,
      )
    } catch (e: Exception) {
      ComponentUtils.handle(componentContext, e)
    }
  }

  override fun <T : Binder<*, *, *>?> findAttachBinderByKey(key: BinderKey): T? {
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
        debugKey: String?,
        excludesFromIncrementalMount: Boolean,
    ): PrimitiveLithoRenderUnit {
      return PrimitiveLithoRenderUnit(
          component,
          commonDynamicProps,
          nodeInfo,
          flags,
          importantForAccessibility,
          primitiveRenderUnit,
          context,
          debugKey,
          excludesFromIncrementalMount,
      )
    }
  }
}
