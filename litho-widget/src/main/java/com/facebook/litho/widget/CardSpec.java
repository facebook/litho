/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
import com.facebook.litho.ComponentLayout;
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
 */
@LayoutSpec (isPureRender = true)
class CardSpec {

  private static final int DEFAULT_CORNER_RADIUS_DP = 2;
  private static final int DEFAULT_SHADOW_SIZE_DP = 2;

  @PropDefault static final int cardBackgroundColor = Color.WHITE;
  @PropDefault static final int clippingColor = Color.WHITE;
  @PropDefault static final int shadowStartColor = 0x37000000;
  @PropDefault static final int shadowEndColor = 0x03000000;
  @PropDefault static final float cornerRadius = -1;
  @PropDefault static final float elevation = -1;

  private static float pixels(Resources resources, int dips) {
    final float scale = resources.getDisplayMetrics().density;
    return dips * scale + 0.5f;
  }

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop Component content,
      @Prop(optional = true, resType = ResType.COLOR) int cardBackgroundColor,
      @Prop(optional = true, resType = ResType.COLOR) int clippingColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowStartColor,
      @Prop(optional = true, resType = ResType.COLOR) int shadowEndColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float elevation) {

    final Resources resources = c.getResources();

    if (cornerRadius == -1) {
      cornerRadius = pixels(resources, DEFAULT_CORNER_RADIUS_DP);
    }

    if (elevation == -1) {
      elevation = pixels(resources, DEFAULT_SHADOW_SIZE_DP);
    }

    final int shadowTop = getShadowTop(elevation);
    final int shadowBottom = getShadowBottom(elevation);
    final int shadowHorizontal = getShadowHorizontal(elevation);

    return Column.create(c)
        .child(
            Column.create(c)
                .marginPx(HORIZONTAL, shadowHorizontal)
                .marginPx(TOP, shadowTop)
                .marginPx(BOTTOM, shadowBottom)
                .backgroundColor(cardBackgroundColor)
                .child(content)
                .child(
                    CardClip.create(c)
                        .clippingColor(clippingColor)
                        .cornerRadiusPx(cornerRadius)
                        .positionType(ABSOLUTE)
                        .positionPx(ALL, 0)))
        .child(
            CardShadow.create(c)
                .shadowStartColor(shadowStartColor)
                .shadowEndColor(shadowEndColor)
                .cornerRadiusPx(cornerRadius)
                .shadowSizePx(elevation)
                .positionType(ABSOLUTE)
                .positionPx(ALL, 0))
        .build();
  }
}
