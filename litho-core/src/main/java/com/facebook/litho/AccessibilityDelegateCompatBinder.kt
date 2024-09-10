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
import androidx.core.view.ViewCompat
import com.facebook.rendercore.RenderUnit

internal object AccessibilityDelegateCompatBinder : RenderUnit.Binder<Int, View, Any> {

  fun create(
      model: Int,
  ): RenderUnit.DelegateBinder<Any?, Any, Any> {
    return RenderUnit.DelegateBinder.createDelegateBinder(
        model = model, binder = AccessibilityDelegateCompatBinder)
        as RenderUnit.DelegateBinder<Any?, Any, Any>
  }

  override fun shouldUpdate(
      currentModel: Int,
      newModel: Int,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = false

  override fun bind(context: Context, content: View, model: Int, layoutData: Any?) {
    ViewCompat.setAccessibilityDelegate(
        content, ComponentAccessibilityDelegate(content, content.isFocusable, model))
  }

  override fun unbind(
      context: Context,
      content: View,
      model: Int,
      layoutData: Any?,
      bindData: Any?
  ) {
    ViewCompat.setAccessibilityDelegate(content, null)
  }
}
