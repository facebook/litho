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

package com.facebook.litho.animation

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.Value
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.CoreDimenField
import com.facebook.rendercore.Dimen

/**
 * Style item for layout properties that accept [Value] for animated transitions.
 *
 * This style item allows layout properties like height and width to be driven by transition values,
 * enabling smooth animated transitions when the value changes.
 */
@PublishedApi
@DataClassGenerate
internal data class DimenValueStyleItem(
    override val field: CoreDimenField,
    override val value: Value<Dimen>,
) : StyleItem<Value<Dimen>> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    val layoutValues = commonProps.getOrCreateLayoutValues()
    when (field) {
      CoreDimenField.HEIGHT -> layoutValues.height = value
      CoreDimenField.WIDTH -> layoutValues.width = value
      else -> error("Unsupported Value<Dimen> field: $field")
    }
  }
}

/**
 * Sets a specific preferred height for this component using a [Value].
 *
 * This allows the height to be driven by a transition value, enabling smooth animated transitions
 * when the height changes. The component will re-render when the transition value updates.
 *
 * @param height the [Value] containing the height dimension.
 */
@ExperimentalLithoApi
fun Style.height(height: Value<Dimen>): Style =
    this + DimenValueStyleItem(CoreDimenField.HEIGHT, height)

/**
 * Sets a specific preferred width for this component using a [Value].
 *
 * This allows the width to be driven by a transition value, enabling smooth animated transitions
 * when the width changes. The component will re-render when the transition value updates.
 *
 * @param width the [Value] containing the width dimension.
 */
@ExperimentalLithoApi
fun Style.width(width: Value<Dimen>): Style =
    this + DimenValueStyleItem(CoreDimenField.WIDTH, width)
