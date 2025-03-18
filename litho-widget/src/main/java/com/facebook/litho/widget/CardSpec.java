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

package com.facebook.litho.widget;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

/**
 * A component that renders a given component into a card border with shadow.
 *
 * @prop cardBackgroundTransitionKey TransitionKey for the Background. If set, enables adding a
 *     global transition for animating the card background.
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
 *     0 in this case.
 * @prop disableClipTopRight If set, opt out of clipping the top-right corner, elevation will force
 *     to 0 in this case.
 * @prop disableClipBottomLeft If set, opt out of clipping the bottom-left corner, elevation will
 *     force to 0 in this case.
 * @prop disableClipBottomRight If set, opt out of clipping the bottom-right corner, elevation will
 *     force to 0 in this case.
 */
@LayoutSpec
class CardSpec {

  // Colors are clamped between 0x00000000 and 0xffffffff so this value is safe
  private static final int UNSET_CLIPPING = Integer.MIN_VALUE;

  @PropDefault static final int cardBackgroundColor = Color.WHITE;
  @PropDefault static final int clippingColor = UNSET_CLIPPING;
  @PropDefault static final int shadowStartColor = 0x37000000;
  @PropDefault static final int shadowEndColor = 0x03000000;
  @PropDefault static final float cornerRadius = -1;
  @PropDefault static final float elevation = -1;
  @PropDefault static final int shadowTopOverride = -1;
  @PropDefault static final int shadowBottomOverride = -1;
  @PropDefault static final float shadowLeftOverride = CardShadowDrawable.UNDEFINED;
  @PropDefault static final float shadowRightOverride = CardShadowDrawable.UNDEFINED;
  @PropDefault static final boolean transparencyEnabled = false;

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Component content,
      @Prop(optional = true) @Nullable Drawable cardBackgroundDrawable,
      @Prop(optional = true) @Nullable String cardBackgroundTransitionKey,
      @Prop(optional = true, resType = ResType.COLOR) int cardBackgroundColor,
      @Prop(optional = true) @Nullable DynamicValue<Integer> cardBackgroundColorDv,
      @Prop(optional = true, resType = ResType.COLOR) int clippingColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowStartColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowEndColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float elevation,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowTopOverride,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowBottomOverride,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowLeftOverride,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRightOverride,
      @Prop(
              optional = true,
              docString =
                  "[UNPERFORMANT WARNING] if you do not need to render your corners transparently"
                      + " please set to false. It is more expensive to perform rounded corners with"
                      + " transparent\n"
                      + "clipping due to antialiasing operations.\n\n"
                      + "<p>A component that renders a given component into a card border with"
                      + " shadow, and allows for\n"
                      + "transparent corners. With transparencyEnabled(false) {@link *"
                      + " com.facebook.litho.widget.Card} uses imitation clipped corners that\n"
                      + "draw in a solid color to mimic the background. transparencyEnabled(true)"
                      + " is useful if you are\n"
                      + "rendering your pill over a gradient or dynamic background.\n")
          boolean transparencyEnabled,
      @Prop(optional = true) boolean disableClipTopLeft,
      @Prop(optional = true) boolean disableClipTopRight,
      @Prop(optional = true) boolean disableClipBottomLeft,
      @Prop(optional = true) boolean disableClipBottomRight) {
    return new ExperimentalCard(
        content,
        cardBackgroundDrawable,
        cardBackgroundTransitionKey,
        cardBackgroundColor,
        cardBackgroundColorDv,
        clippingColor,
        shadowStartColor,
        shadowEndColor,
        cornerRadius,
        elevation,
        shadowTopOverride,
        shadowBottomOverride,
        shadowLeftOverride,
        shadowRightOverride,
        transparencyEnabled,
        disableClipTopLeft,
        disableClipTopRight,
        disableClipBottomLeft,
        disableClipBottomRight);
  }
}
