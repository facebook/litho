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

package com.facebook.litho.binders

import android.view.View
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.rendercore.RenderUnit

/**
 * Sets the given [binder] as a *Mount* binder for [View]. The usage of this binder will guarantee
 * that there will be an associated View to it (either because it is wrapping a Mounting View or by
 * creating a wrapping [ComponentHost].
 *
 * Once associated, the binder [RenderUnit.Binder#bind] can be called at every *mount* of the render
 * unit associated with this [Style], while the [RenderUnit.Binder#unbind] can be called at every
 * unmount.
 *
 * The result of the [RenderUnit.Binder.shouldUpdate] will determine if the [bind] and [unbind]
 * methods will be called for any iteration of the mount phase.
 *
 * For example, if you want your binder to be called at first time the content is mount, and finally
 * the last time it is unmounted before going offscreen, you should set the
 * [RenderUnit.Binder.shouldUpdate] to `false`.
 */
fun Style.viewBinder(binder: RenderUnit.Binder<Any?, View, Any?>): Style =
    this + ObjectStyleItem(BinderObjectField.MOUNT_VIEW_BINDER, binder)

@PublishedApi
internal enum class BinderObjectField : StyleItemField {
  MOUNT_VIEW_BINDER,
}

@PublishedApi
@DataClassGenerate
internal data class ObjectStyleItem(
    override val field: BinderObjectField,
    override val value: Any?
) : StyleItem<Any?> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      BinderObjectField.MOUNT_VIEW_BINDER ->
          commonProps.mountViewBinder(value as RenderUnit.Binder<Any?, Any, Any?>)
    }
  }
}
