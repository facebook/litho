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
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.RenderUnit.DelegateBinder.Companion.createDelegateBinder

/**
 * Creates a [Style] which will apply the given [RenderUnit.Binder] to the [View] rendered by the
 * Component the Style is added to. This abstraction can be used to create higher-level Styles which
 * can apply some property generically to any type of Component.
 *
 * The usage of this binder will guarantee that there will be an associated View to it by calling
 * [Style.wrapInView].
 *
 * Notes for implementing a [RenderUnit.Binder]:
 *
 * The binder's [RenderUnit.Binder.bind] will be called by the framework with the current model to
 * bind the model to the View content, and [RenderUnit.Binder.unbind] will be called with the
 * current model to unbind the model from the View content. When the model changes in a new render
 * pass, [RenderUnit.Binder.shouldUpdate] will be called with the old and new models before
 * unbind/bind: if it returns true, unbind(oldModel)+bind(newModel) will be called. Otherwise, the
 * update will be skipped.
 *
 * In some cases, you may want your binder's bind to be called only the first time the content is
 * mounted, and unbind called the last time it is unmounted before going offscreen. In that case,
 * you can unconditionally return false from [RenderUnit.Binder.shouldUpdate].
 *
 * Generally speaking, a [RenderUnit.Binder] should be static and unchanging, while the model may
 * change between renders.
 *
 * @param binder the [RenderUnit.Binder] to be used to bind the model to the View content.
 * @param model the current model to bind.
 */
inline fun <ModelT, BindDataT : Any> Style.viewBinder(
    binder: RenderUnit.Binder<ModelT, View, BindDataT>,
    model: ModelT
): Style =
    this +
        ObjectStyleItem(
            BinderObjectField.DELEGATE_MOUNT_VIEW_BINDER, createDelegateBinder(model, binder))

/**
 * An overload of [Style.viewBinder] which takes a [RenderUnit.Binder] that does not require a
 * model.
 *
 * @param binder the [RenderUnit.Binder] to be used to bind the model to the View content.
 */
inline fun <BindDataT : Any> Style.viewBinder(
    binder: RenderUnit.Binder<Unit, View, BindDataT>
): Style = this.viewBinder(binder, model = Unit)

@Deprecated("use parameterized viewBinder methods above")
fun Style.viewBinder(binder: DelegateBinder<*, View, Any>): Style =
    this + ObjectStyleItem(BinderObjectField.DELEGATE_MOUNT_VIEW_BINDER, binder)

@PublishedApi
internal enum class BinderObjectField : StyleItemField {
  DELEGATE_MOUNT_VIEW_BINDER
}

@PublishedApi
@DataClassGenerate
internal data class ObjectStyleItem(
    override val field: BinderObjectField,
    override val value: Any?
) : StyleItem<Any?> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      BinderObjectField.DELEGATE_MOUNT_VIEW_BINDER ->
          commonProps.delegateMountViewBinder(value as DelegateBinder<Any, Any, Any>)
    }
  }
}
