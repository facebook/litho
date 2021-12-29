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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.DynamicValue
import com.facebook.litho.StateValue
import com.facebook.litho.animated.alpha
import com.facebook.litho.animated.scaleX
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.State
import com.facebook.litho.flexbox.alignSelf
import com.facebook.samples.litho.R
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify

// start_example
@LayoutSpec
object CommonDynamicPropsComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      alpha: StateValue<DynamicValue<Float>>,
      scale: StateValue<DynamicValue<Float>>,
  ) {
    alpha.set(DynamicValue<Float>(1f))
    scale.set(DynamicValue<Float>(1f))
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State alpha: DynamicValue<Float>,
      @State scale: DynamicValue<Float>,
  ): Component {
    val square =
        Column.create(c)
            .widthDip(100f)
            .heightDip(100f)
            .backgroundRes(R.color.primaryColor)
            .alignSelf(YogaAlign.CENTER)
            .scaleX(scale)
            .scaleY(scale)
            .alpha(alpha)
            .build()

    return Column.create(c)
        .justifyContent(YogaJustify.SPACE_BETWEEN)
        .paddingDip(YogaEdge.ALL, 20f)
        .child(
            SeekBar.create(c).heightDip(14f).widthPercent(100f).initialValue(1f).onProgressChanged {
              alpha.set(it)
            })
        .child(square)
        .child(
            SeekBar.create(c).heightDip(14f).widthPercent(100f).initialValue(1f).onProgressChanged {
              scale.set(it)
            })
        .build()
  }
}
// end_example
