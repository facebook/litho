// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho

import android.content.Context
import android.view.View
import com.facebook.rendercore.RenderUnit

internal object ViewAttributesViewBinder : RenderUnit.Binder<ViewAttributes, View, Any> {

  fun create(viewAttributes: ViewAttributes): RenderUnit.DelegateBinder<Any?, Any, Any> {
    return RenderUnit.DelegateBinder.createDelegateBinder(viewAttributes, ViewAttributesViewBinder)
        as RenderUnit.DelegateBinder<Any?, Any, Any>
  }

  override fun shouldUpdate(
      currentModel: ViewAttributes,
      newModel: ViewAttributes,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    return currentModel != newModel
  }

  override fun bind(
      context: Context,
      content: View,
      model: ViewAttributes,
      layoutData: Any?
  ): Any? {
    // TODO: Set View Attributes
    return null
  }

  override fun unbind(
      context: Context,
      content: View,
      model: ViewAttributes,
      layoutData: Any?,
      bindData: Any?
  ) {
    // TODO: Unset View Attributes
  }
}
