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

package com.facebook.samples.litho.java.events;

import android.util.Log;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.FocusedVisibleEvent;
import com.facebook.litho.FullImpressionVisibleEvent;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.VisibilityChangedEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;

// start_example
@LayoutSpec
class VisibilityEventExampleSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .alignItems(YogaAlign.STRETCH)
        .child(
            Text.create(c)
                .text("This is a layout spec")
                .visibleHandler(VisibilityEventExample.onTitleVisible(c))
                .invisibleHandler(VisibilityEventExample.onTitleInvisible(c))
                .focusedHandler(VisibilityEventExample.onComponentFocused(c, "someStringParam"))
                .fullImpressionHandler(VisibilityEventExample.onComponentFullImpression(c)))
        .visibilityChangedHandler(VisibilityEventExample.onComponentVisibilityChanged(c))
        .visibleHeightRatio(0.1f) // fire only when at least 10% of the heighta is visible
        .visibleHeightRatio(0.1f) // fire only when at least 10% of the width is visible
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onTitleVisible(ComponentContext c) {
    Log.d("VisibilityEvent", "The title entered the Visible Range");
  }

  @OnEvent(InvisibleEvent.class)
  static void onTitleInvisible(ComponentContext c) {
    Log.d("VisibilityEvent", "The title is no longer visible");
  }

  @OnEvent(FocusedVisibleEvent.class)
  static void onComponentFocused(ComponentContext c, @Param String stringParam) {
    Log.d("VisibilityEvent", "The component is focused with param: " + stringParam);
  }

  @OnEvent(FullImpressionVisibleEvent.class)
  static void onComponentFullImpression(ComponentContext c) {
    Log.d("VisibilityEvent", "The component has logged a full impression");
  }

  @OnEvent(VisibilityChangedEvent.class)
  static void onComponentVisibilityChanged(
      ComponentContext c,
      @FromEvent int visibleTop,
      @FromEvent int visibleLeft,
      @FromEvent int visibleHeight,
      @FromEvent int visibleWidth,
      @FromEvent float percentVisibleHeight,
      @FromEvent float percentVisibleWidth) {
    Log.d(
        "VisibilityEvent",
        "The component's visible size is " + visibleHeight + "h" + visibleWidth + "w");
  }
}
// end_example
