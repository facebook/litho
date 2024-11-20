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

package com.facebook.litho.widget

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType

/**
 * A component that renders a solid color.
 *
 * @uidocs
 * @prop color Color to be shown.
 * @prop alpha The alpha of the color, in the range [0.0, 1.0]
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
internal object SolidColorSpec {

  @PropDefault val alpha: Float = -1.0f

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext?,
      @Prop(resType = ResType.COLOR) color: Int,
      @Prop(optional = true, isCommonProp = true, overrideCommonPropBehavior = true) alpha: Float
  ): Component {
    return SolidColorPrimitiveComponent(color = color, alpha = alpha)
  }
}
