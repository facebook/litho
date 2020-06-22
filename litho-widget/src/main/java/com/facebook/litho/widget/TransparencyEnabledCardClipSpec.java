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
 * A component that paints a card with rounded edges to perform a clipping operation on the
 * component being rendered below it. Used in {@link CardSpec} when transparencyEnabled(true).
 *
 * @prop clippingColor Color for corner clipping.
 * @prop cornerRadius Radius for corner clipping.
 */
@MountSpec(isPureRender = true)
class TransparencyEnabledCardClipSpec {

  @PropDefault static final int cardBackgroundColor = Color.WHITE;

  @OnCreateMountContent
  static TransparencyEnabledCardClipDrawable onCreateMountContent(Context c) {
    return new TransparencyEnabledCardClipDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      TransparencyEnabledCardClipDrawable cardClipDrawable,
      @Prop(optional = true, resType = ResType.COLOR) int cardBackgroundColor,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float cornerRadius) {
    cardClipDrawable.setBackgroundColor(cardBackgroundColor);
    cardClipDrawable.setCornerRadius(cornerRadius);
  }

  @OnUnmount
  static void onUnmount(ComponentContext c, TransparencyEnabledCardClipDrawable cardClipDrawable) {
    cardClipDrawable.setCornerRadius(0);
    cardClipDrawable.setBackgroundColor(Color.WHITE);
  }
}
