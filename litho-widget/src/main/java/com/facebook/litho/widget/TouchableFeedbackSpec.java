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

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

/**
 * A Component that can wrap another component to add touch feedback via a RippleDrawable
 * background.
 */
@LayoutSpec(simpleNameDelegate = "content")
class TouchableFeedbackSpec {

  @PropDefault static final int color = Color.WHITE;
  @PropDefault static final int highlightColor = Color.LTGRAY;

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop Component content, @CachedValue Drawable rippleDrawable) {
    return Wrapper.create(c).delegate(content).background(rippleDrawable).build();
  }

  // We use a cached value to make sure we don't replace the RippleDrawable mid-animation.
  @OnCalculateCachedValue(name = "rippleDrawable")
  static Drawable onCalculateCachedValue(
      ComponentContext c,
      @Prop(optional = true, resType = ResType.COLOR) int color,
      @Prop(optional = true, resType = ResType.COLOR) int highlightColor) {
    return getRippleDrawable(color, highlightColor);
  }

  public static Drawable getRippleDrawable(int normalColor, int pressedColor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new RippleDrawable(
          ColorStateList.valueOf(pressedColor),
          new ColorDrawable(normalColor),
          getRippleMask(pressedColor));
    } else {
      return getStateListDrawable(normalColor, pressedColor);
    }
  }

  private static Drawable getRippleMask(int color) {
    final ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
    shapeDrawable.getPaint().setColor(color);
    return shapeDrawable;
  }

  public static StateListDrawable getStateListDrawable(int normalColor, int pressedColor) {
    final StateListDrawable states = new StateListDrawable();
    states.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
    states.addState(new int[] {android.R.attr.state_focused}, new ColorDrawable(pressedColor));
    states.addState(new int[] {android.R.attr.state_activated}, new ColorDrawable(pressedColor));
    states.addState(new int[] {}, new ColorDrawable(normalColor));

    return states;
  }
}
