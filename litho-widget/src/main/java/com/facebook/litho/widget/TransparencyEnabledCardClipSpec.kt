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
import android.graphics.drawable.Drawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.DynamicValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.config.ComponentsConfiguration

/**
 * A component that paints a card with rounded edges to perform a clipping operation on the
 * component being rendered below it. Used in {@link CardSpec} when transparencyEnabled(true).
 *
 * @prop clippingColor Color for corner clipping.
 * @prop cornerRadius Radius for corner clipping.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
internal object TransparencyEnabledCardClipSpec {

  @PropDefault val cardBackgroundColor: Int = Color.WHITE
  @PropDefault val clippingColor: Int = Color.TRANSPARENT

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true) backgroundDrawable: Drawable?,
      @Prop(optional = true, resType = ResType.COLOR) cardBackgroundColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) clippingColor: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) cornerRadius: Float,
      @Prop(optional = true) disableClipTopLeft: Boolean,
      @Prop(optional = true) disableClipTopRight: Boolean,
      @Prop(optional = true) disableClipBottomLeft: Boolean,
      @Prop(optional = true) disableClipBottomRight: Boolean,
      @Prop(optional = true) cardBackgroundColorDv: DynamicValue<Int>?,
  ): Component {
    return if (ComponentsConfiguration.usePrimitiveTransparencyEnabledCardClip) {
      ExperimentalTransparencyEnabledCardClip(
          backgroundDrawable = backgroundDrawable,
          cardBackgroundColor = cardBackgroundColor,
          clippingColor = clippingColor,
          cornerRadius = cornerRadius,
          disableClipTopLeft = disableClipTopLeft,
          disableClipTopRight = disableClipTopRight,
          disableClipBottomLeft = disableClipBottomLeft,
          disableClipBottomRight = disableClipBottomRight,
          dynamicCardBackgroundColor = cardBackgroundColorDv,
      )
    } else {
      TransparencyEnabledCardClipComponent.create(c)
          .backgroundDrawable(backgroundDrawable)
          .cardBackgroundColor(cardBackgroundColor)
          .clippingColor(clippingColor)
          .cornerRadiusPx(cornerRadius)
          .disableClipTopLeft(disableClipTopLeft)
          .disableClipTopRight(disableClipTopRight)
          .disableClipBottomLeft(disableClipBottomLeft)
          .disableClipBottomRight(disableClipBottomRight)
          .cardBackgroundColorDv(cardBackgroundColorDv)
          .build()
    }
  }
}
