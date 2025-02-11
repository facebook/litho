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

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.config.ComponentsConfiguration

/**
 * A component that paints rounded edges to mimic a clipping operation on the component being
 * rendered below it. Used in [CardSpec].
 *
 * @prop clippingColor Color for corner clipping.
 * @prop cornerRadius Radius for corner clipping.
 * @prop disableClipTopLeft If set, opt out of clipping the top-left corner
 * @prop disableClipTopRight If set, opt out of clipping the top-right corner
 * @prop disableClipBottomLeft If set, opt out of clipping the bottom-left corner
 * @prop disableClipBottomRight If set, opt out of clipping the bottom-right corner
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
internal object CardClipSpec {

  @PropDefault val clippingColor: Int = Color.WHITE

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true, resType = ResType.COLOR) clippingColor: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) cornerRadius: Float,
      @Prop(optional = true) disableClipTopLeft: Boolean,
      @Prop(optional = true) disableClipTopRight: Boolean,
      @Prop(optional = true) disableClipBottomLeft: Boolean,
      @Prop(optional = true) disableClipBottomRight: Boolean
  ): Component {
    return if (ComponentsConfiguration.usePrimitiveCardClip) {
      ExperimentalCardClip(
          clippingColor = clippingColor,
          cornerRadius = cornerRadius,
          disableClipTopLeft = disableClipTopLeft,
          disableClipTopRight = disableClipTopRight,
          disableClipBottomLeft = disableClipBottomLeft,
          disableClipBottomRight = disableClipBottomRight)
    } else {
      CardClipComponent.create(c)
          .clippingColor(clippingColor)
          .cornerRadiusPx(cornerRadius)
          .disableClipTopLeft(disableClipTopLeft)
          .disableClipTopRight(disableClipTopRight)
          .disableClipBottomLeft(disableClipBottomLeft)
          .disableClipBottomRight(disableClipBottomRight)
          .build()
    }
  }
}
