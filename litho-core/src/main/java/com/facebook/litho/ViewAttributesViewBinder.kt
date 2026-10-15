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
import android.view.View
import com.facebook.rendercore.RenderUnit

object ViewAttributesViewBinder : RenderUnit.Binder<ViewAttributesViewBinder.Model, View, Int> {

  data class Model(
      val nodeInfo: NodeInfo?,
      val viewAttributes: ViewAttributes,
      val isRootHost: Boolean,
  )

  fun create(
      model: Model,
  ): RenderUnit.DelegateBinder<Any?, Any, Any> {
    return RenderUnit.DelegateBinder.createDelegateBinder(
        model = model,
        binder = ViewAttributesViewBinder,
    ) as RenderUnit.DelegateBinder<Any?, Any, Any>
  }

  override fun shouldUpdate(
      currentModel: Model,
      newModel: Model,
      currentLayoutData: Any?,
      nextLayoutData: Any?,
  ): Boolean {
    // The bind also installs the render unit's NodeInfo, which the accessibility delegate reads and
    // ViewAttributes does not hold: a heading, a state description or a focus order that changed on
    // its own must rebind as well, or the delegate keeps describing the old one.
    return currentModel.viewAttributes != newModel.viewAttributes ||
        !isNodeInfoEquivalent(currentModel.nodeInfo, newModel.nodeInfo)
  }

  private fun isNodeInfoEquivalent(current: NodeInfo?, next: NodeInfo?): Boolean =
      if (current == null || next == null) current === next else current.isEquivalentTo(next)

  override fun bind(context: Context, content: View, model: Model, layoutData: Any?): Int {
    val flags =
        if (model.isRootHost) {
          (content as BaseMountingView).viewAttributeFlags
        } else {
          LithoMountData.getViewAttributeFlags(content)
        }

    ViewAttributes.setViewAttributesWithNodeInfo(
        content = content,
        attributes = model.viewAttributes,
        nodeInfo = model.nodeInfo,
    )

    return flags
  }

  override fun unbind(
      context: Context,
      content: View,
      model: Model,
      layoutData: Any?,
      bindData: Int?,
  ) {
    if (bindData == null) {
      throw IllegalStateException("Bind data should not be null")
    }

    ViewAttributes.unsetViewAttributes(content, model.viewAttributes, bindData)
  }
}
