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

package com.facebook.samples.litho.dynamicprops;

import android.content.Context;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBindDynamicValue;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.utils.MeasureUtils;

@MountSpec
class ClockComponentSpec {

  @OnMeasure
  static void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    final ClockView clockView = new ClockView(c.getAndroidContext());
    clockView.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec));
    size.width = clockView.getMeasuredWidth();
    size.height = clockView.getMeasuredHeight();
  }

  @OnCreateMountContent
  static ClockView onCreateMountContent(Context c) {
    return new ClockView(c);
  }

  @OnBindDynamicValue
  static void onBindTime(ClockView clockView, @Prop(dynamic = true) long time) {
    clockView.setTime(time % ClockView.TWELVE_HOURS);
  }
}
