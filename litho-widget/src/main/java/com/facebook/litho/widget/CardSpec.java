/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static com.facebook.litho.widget.CardShadowDrawable.getShadowBottom;
import static com.facebook.litho.widget.CardShadowDrawable.getShadowHorizontal;
import static com.facebook.litho.widget.CardShadowDrawable.getShadowTop;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;

import android.content.res.Resources;
import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

/**
 * A component that renders a given component into a card border with shadow.
 *
 * @prop cardBackgroundColor Background color for the card.
 * @prop clippingColor Color for corner clipping.
 * @prop shadowStartColor Start color for shadow drawn underneath the card.
 * @prop shadowEndColor End color for shadow drawn underneath the card.
 * @prop cornerRadius Corner radius for the card.
 * @prop elevation Elevation of the card.
 * @prop shadowBottomOverride Override of size of shadow at bottom of card.
 * @prop disableClipTopLeft If set, opt out of clipping the top-left corner, elevation will force to
 *     0 in this case.
 * @prop disableClipTopRight If set, opt out of clipping the top-right corner, elevation will force
 *     to 0 in this case.
 * @prop disableClipBottomLeft If set, opt out of clipping the bottom-left corner, elevation will
 *     force to 0 in this case.
 * @prop disableClipBottomRight If set, opt out of clipping the bottom-right corner, elevation will
 *     force to 0 in this case.
 */
@LayoutSpec(isPureRender = true)
class CardSpec {

  private static final int DEFAULT_CORNER_RADIUS_DP = 2;
  private static final int DEFAULT_SHADOW_SIZE_DP = 2;

  @PropDefault static final int cardBackgroundColor = Color.WHITE;
  @PropDefault static final int clippingColor = Color.WHITE;
  @PropDefault static final int shadowStartColor = 0x37000000;
  @PropDefault static final int shadowEndColor = 0x03000000;
  @PropDefault static final float cornerRadius = -1;
  @PropDefault static final float elevation = -1;
  @PropDefault static final int shadowBottomOverride = -1;

  private static float pixels(Resources resources, int dips) {
    final float scale = resources.getDisplayMetrics().density;
    return dips * scale + 0.5f;
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Component content,
      @Prop(optional = true, resType = ResType.COLOR) int cardBackgroundColor,
      @Prop(optional = true, resType = ResType.COLOR) int clippingColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowStartColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowEndColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float elevation,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowBottomOverride,
      @Prop(optional = true) boolean disableClipTopLeft,
      @Prop(optional = true) boolean disableClipTopRight,
      @Prop(optional = true) boolean disableClipBottomLeft,
      @Prop(optional = true) boolean disableClipBottomRight) {

    final Resources resources = c.getAndroidContext().getResources();

    if (cornerRadius == -1) {
      cornerRadius = pixels(resources, DEFAULT_CORNER_RADIUS_DP);
    }

    if (elevation == -1) {
      elevation = pixels(resources, DEFAULT_SHADOW_SIZE_DP);
    }

    final int shadowTop = getShadowTop(elevation);
    final int shadowBottom =
        shadowBottomOverride == -1 ? getShadowBottom(elevation) : shadowBottomOverride;
    final int shadowHorizontal = getShadowHorizontal(elevation);

    return Column.create(c)
        .child(
            Column.create(c)
                .marginPx(HORIZONTAL, shadowHorizontal)
                .marginPx(TOP, disableClipTopLeft && disableClipTopRight ? 0 : shadowTop)
                .marginPx(
                    BOTTOM, disableClipBottomLeft && disableClipBottomRight ? 0 : shadowBottom)
                .backgroundColor(cardBackgroundColor)
                .child(content)
                .child(
                    CardClip.create(c)
                        .clippingColor(clippingColor)
                        .cornerRadiusPx(cornerRadius)
                        .positionType(ABSOLUTE)
                        .positionPx(ALL, 0)
                        .disableClipTopLeft(disableClipTopLeft)
                        .disableClipTopRight(disableClipTopRight)
                        .disableClipBottomLeft(disableClipBottomLeft)
                        .disableClipBottomRight(disableClipBottomRight)))
        .child(
            elevation > 0
                ? CardShadow.create(c)
                    .shadowStartColor(shadowStartColor)
                    .shadowEndColor(shadowEndColor)
                    .cornerRadiusPx(cornerRadius)
                    .shadowSizePx(elevation)
                    .hideTopShadow(disableClipTopLeft && disableClipTopRight)
                    .hideBottomShadow(disableClipBottomLeft && disableClipBottomRight)
                    .positionType(ABSOLUTE)
                    .positionPx(ALL, 0)
                : null)
        .build();
  }
}
