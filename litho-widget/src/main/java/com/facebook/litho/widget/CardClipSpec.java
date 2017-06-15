/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;

import static com.facebook.litho.widget.CardClipDrawable.BOTTOM_LEFT;
import static com.facebook.litho.widget.CardClipDrawable.BOTTOM_RIGHT;
import static com.facebook.litho.widget.CardClipDrawable.NONE;
import static com.facebook.litho.widget.CardClipDrawable.TOP_LEFT;
import static com.facebook.litho.widget.CardClipDrawable.TOP_RIGHT;

/**
 * A component that paints rounded edges to mimic a clipping operation on the
 * component being rendered below it. Used in {@link CardSpec}.
 *
 * @prop clippingColor Color for corner clipping.
 * @prop cornerRadius Radius for corner clipping.
 * @prop disableClipTopLeft If set, opt out of clipping the top-left corner
 * @prop disableClipTopRight If set, opt out of clipping the top-right corner
 * @prop disableClipBottomLeft If set, opt out of clipping the bottom-left corner
 * @prop disableClipBottomRight If set, opt out of clipping the bottom-right corner
 */
@MountSpec(isPureRender = true)
class CardClipSpec {

  @OnCreateMountContent
  static CardClipDrawable onCreateMountContent(ComponentContext c) {
    return new CardClipDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      CardClipDrawable cardClipDrawable,
      @Prop(optional = true, resType = ResType.COLOR) int clippingColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius,
      @Prop(optional = true) boolean disableClipTopLeft,
      @Prop(optional = true) boolean disableClipTopRight,
      @Prop(optional = true) boolean disableClipBottomLeft,
      @Prop(optional = true) boolean disableClipBottomRight) {

    cardClipDrawable.setClippingColor(clippingColor);
    cardClipDrawable.setCornerRadius(cornerRadius);
    int clipEdge =
        (disableClipTopLeft ? TOP_LEFT : NONE) |
        (disableClipTopRight ? TOP_RIGHT : NONE) |
        (disableClipBottomLeft ? BOTTOM_LEFT : NONE) |
        (disableClipBottomRight ? BOTTOM_RIGHT : NONE);
    cardClipDrawable.setDisableClip(clipEdge);
  }
}
