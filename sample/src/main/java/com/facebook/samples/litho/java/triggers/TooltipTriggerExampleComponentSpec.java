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

package com.facebook.samples.litho.java.triggers;

import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import androidx.annotation.ColorInt;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.LithoTooltipController;
import com.facebook.litho.LithoView;
import com.facebook.litho.StateValue;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class TooltipTriggerExampleComponentSpec {

  private static final @ColorInt int LITHO_PINK = 0xfff36b7f;

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, final StateValue<Handle> anchorHandle) {
    anchorHandle.set(new Handle());
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State Handle anchorHandle) {
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .child(
            Text.create(c, android.R.attr.buttonStyle, 0)
                .marginDip(YogaEdge.BOTTOM, 50)
                .text("Click to Trigger show tooltip")
                .clickHandler(TooltipTriggerExampleComponent.onClick(c, anchorHandle)))
        .child(Text.create(c).text("Tooltip anchor").handle(anchorHandle))
        .visibleHandler(TooltipTriggerExampleComponent.onVisible(c, anchorHandle))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @Param Handle anchorHandle) {
    TooltipTriggerExampleComponentSpec.showToolTip(c, anchorHandle);
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @Param Handle anchorHandle) {
    // Show a tooltip when the component becomes visible.
    // NB: Incremental mount must be enabled for the component to receive visibility callbacks.
    TooltipTriggerExampleComponentSpec.showToolTip(c, anchorHandle);
  }

  static void showToolTip(ComponentContext c, Handle anchorHandle) {
    LithoTooltipController.showTooltipOnHandle(
        c, createTooltip(c, "Example Tooltip"), anchorHandle, 0, 0);
  }

  private static PopupWindow createTooltip(final ComponentContext c, final String text) {
    final LithoView tooltip =
        LithoView.create(
            c,
            Column.create(c)
                .paddingDip(YogaEdge.ALL, 15)
                .backgroundColor(LITHO_PINK)
                .child(Text.create(c).text(text).textColor(Color.WHITE))
                .build());
    return new PopupWindow(
        tooltip,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        true);
  }
}
