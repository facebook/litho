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

import static com.facebook.litho.widget.CardClipDrawable.BOTTOM_LEFT;
import static com.facebook.litho.widget.CardClipDrawable.BOTTOM_RIGHT;
import static com.facebook.litho.widget.CardClipDrawable.NONE;
import static com.facebook.litho.widget.CardClipDrawable.TOP_LEFT;
import static com.facebook.litho.widget.CardClipDrawable.TOP_RIGHT;

import android.content.Context;
import android.graphics.Color;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

/**
 * A component that paints rounded edges to mimic a clipping operation on the component being
 * rendered below it. Used in {@link CardSpec}.
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

  @PropDefault static final int clippingColor = Color.WHITE;

  @OnCreateMountContent
  static CardClipDrawable onCreateMountContent(Context c) {
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
        (disableClipTopLeft ? TOP_LEFT : NONE)
            | (disableClipTopRight ? TOP_RIGHT : NONE)
            | (disableClipBottomLeft ? BOTTOM_LEFT : NONE)
            | (disableClipBottomRight ? BOTTOM_RIGHT : NONE);
    cardClipDrawable.setDisableClip(clipEdge);
  }

  @OnUnmount
  static void onUnmount(ComponentContext c, CardClipDrawable cardClipDrawable) {
    cardClipDrawable.setCornerRadius(0);
    cardClipDrawable.setClippingColor(Color.WHITE);
    cardClipDrawable.setDisableClip(NONE);
  }
}
