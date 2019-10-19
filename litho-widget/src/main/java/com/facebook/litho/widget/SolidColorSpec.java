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

import static android.widget.ImageView.ScaleType.FIT_XY;
import static com.facebook.litho.annotations.ResType.COLOR;

import android.graphics.drawable.ColorDrawable;
import androidx.core.graphics.ColorUtils;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;

/**
 * A component that renders a solid color.
 *
 * @uidocs https://fburl.com/SolidColor:b0df
 * @prop color Color to be shown.
 * @prop alpha The alpha of the color, in the range [0.0, 1.0]
 */
@LayoutSpec
class SolidColorSpec {
  @PropDefault static final float alpha = -1.0f;

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop(resType = COLOR) int color,
      @Prop(optional = true, isCommonProp = true, overrideCommonPropBehavior = true) float alpha) {
    if (alpha >= 0f) {
      alpha = Math.min(1f, alpha);
      color = ColorUtils.setAlphaComponent(color, (int) (alpha * 255f));
    }
    return Image.create(c).scaleType(FIT_XY).drawable(new ColorDrawable(color)).build();
  }
}
