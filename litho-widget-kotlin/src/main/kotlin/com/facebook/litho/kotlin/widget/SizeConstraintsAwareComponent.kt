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

package com.facebook.litho.kotlin.widget

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec
import com.facebook.litho.annotations.Prop
import com.facebook.litho.kotlinStyle
import com.facebook.rendercore.SizeConstraints

/**
 * Utility for creating a layout tree that depends on its own and/or its children's size.
 *
 * For example, Component A is returned if it fits within the specified width otherwise Component B
 * is returned.
 *
 * Example:
 * ```
 * SizeConstraintsAwareComponent { sizeConstraints ->
 *     val textComponent = Text(textSize = 16.sp, text = "Some text to measure")
 *
 *     val textOutputSize = Size()
 *
 *     textComponent.measure(
 *     context,
 *     SizeSpec.makeSizeSpec(0, UNSPECIFIED),
 *     SizeSpec.makeSizeSpec(0, UNSPECIFIED),
 *     textOutputSize)
 *
 *     // Small component to use in case textComponent doesn’t fit within
 *     // the current layout.
 *     val imageComponent = Image(drawable = drawableRes(R.drawable.ic_launcher))
 *
 *     // Assuming sizeConstraints.hasBoundedWidth == true
 *     val doesTextFit = textOutputSize.width <= sizeConstraints.maxWidth
 *
 *     if (doesTextFit) textComponent else imageComponent
 * }
 * ```
 */
@Suppress("FunctionName")
fun ResourcesScope.SizeConstraintsAwareComponent(
    style: Style? = null,
    content: ComponentScope.(SizeConstraints) -> Component
): Component {
  return SizeSpecsWrapperComponent.create(context).content(content).kotlinStyle(style).build()
}

@LayoutSpec
private object SizeSpecsWrapperComponentSpec {
  @JvmStatic
  @OnCreateLayoutWithSizeSpec
  fun onCreateLayoutWithSizeSpec(
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      @Prop content: ComponentScope.(SizeConstraints) -> Component
  ): Component {
    val scope = ComponentScope(c, c.renderStateContext)
    return scope.content(SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
  }
}
