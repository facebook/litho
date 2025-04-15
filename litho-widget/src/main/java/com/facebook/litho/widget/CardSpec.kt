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

/**
 * A component that renders a given component into a card border with shadow.
 *
 * @prop cardBackgroundTransitionKey TransitionKey for the Background. If set, enables adding a
 *   global transition for animating the card background.
 * @prop cardBackgroundColor Background color for the card.
 * @prop clippingColor Color for corner clipping.
 * @prop shadowStartColor Start color for shadow drawn underneath the card.
 * @prop shadowEndColor End color for shadow drawn underneath the card.
 * @prop cornerRadius Corner radius for the card.
 * @prop elevation Elevation of the card.
 * @prop shadowBottomOverride Override of size of shadow at bottom of card.
 * @prop shadowLeftOverride Override of size of shadow at left of card.
 * @prop shadowRightOverride Override of size of shadow at right of card.
 * @prop disableClipTopLeft If set, opt out of clipping the top-left corner, elevation will force to
 *   0 in this case.
 * @prop disableClipTopRight If set, opt out of clipping the top-right corner, elevation will force
 *   to 0 in this case.
 * @prop disableClipBottomLeft If set, opt out of clipping the bottom-left corner, elevation will
 *   force to 0 in this case.
 * @prop disableClipBottomRight If set, opt out of clipping the bottom-right corner, elevation will
 *   force to 0 in this case.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
internal object CardSpec {

  // Colors are clamped between 0x00000000 and 0xffffffff so this value is safe
  private const val UNSET_CLIPPING = Int.MIN_VALUE

  @PropDefault const val cardBackgroundColor: Int = Color.WHITE
  @PropDefault const val clippingColor: Int = UNSET_CLIPPING
  @PropDefault const val shadowStartColor: Int = 0x37000000
  @PropDefault const val shadowEndColor: Int = 0x03000000
  @PropDefault const val cornerRadius: Float = -1f
  @PropDefault const val elevation: Float = -1f
  @PropDefault const val shadowTopOverride: Int = -1
  @PropDefault const val shadowBottomOverride: Int = -1
  @PropDefault const val shadowLeftOverride: Float = CardShadowDrawable.UNDEFINED
  @PropDefault const val shadowRightOverride: Float = CardShadowDrawable.UNDEFINED
  @PropDefault const val transparencyEnabled: Boolean = false

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop content: Component?,
      @Prop(optional = true) cardBackgroundDrawable: Drawable?,
      @Prop(optional = true) cardBackgroundTransitionKey: String?,
      @Prop(optional = true, resType = ResType.COLOR) cardBackgroundColor: Int,
      @Prop(optional = true) cardBackgroundColorDv: DynamicValue<Int>?,
      @Prop(optional = true, resType = ResType.COLOR) clippingColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) shadowStartColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) shadowEndColor: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) cornerRadius: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) elevation: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowTopOverride: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowBottomOverride: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowLeftOverride: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowRightOverride: Float,
      @Prop(
          optional = true,
          docString =
              "[UNPERFORMANT WARNING] if you do not need to render your corners transparently " +
                  "please set to false. It is more expensive to perform rounded corners with " +
                  "transparent\nclipping due to antialiasing operations.\n\n<p>A component that " +
                  "renders a given component into a card border with shadow, and allows for\n" +
                  "transparent corners. " +
                  "With transparencyEnabled(false) {@link * com.facebook.litho.widget.Card} uses " +
                  "imitation clipped corners that\ndraw in a solid color to mimic the background. " +
                  "transparencyEnabled(true) is useful if you are\nrendering your pill over a " +
                  "gradient or dynamic background.\n")
      transparencyEnabled: Boolean,
      @Prop(optional = true) disableClipTopLeft: Boolean,
      @Prop(optional = true) disableClipTopRight: Boolean,
      @Prop(optional = true) disableClipBottomLeft: Boolean,
      @Prop(optional = true) disableClipBottomRight: Boolean
  ): Component =
      ExperimentalCard(
          requireNotNull(content) { "CardSpec requires a content" },
          cardBackgroundDrawable,
          cardBackgroundTransitionKey,
          cardBackgroundColor,
          cardBackgroundColorDv,
          clippingColor,
          shadowStartColor,
          shadowEndColor,
          cornerRadius,
          elevation,
          shadowTopOverride.toFloat(),
          shadowBottomOverride.toFloat(),
          shadowLeftOverride,
          shadowRightOverride,
          transparencyEnabled,
          disableClipTopLeft,
          disableClipTopRight,
          disableClipBottomLeft,
          disableClipBottomRight)
}
