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

package com.facebook.samples.litho.playground;

import android.graphics.Color;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.OnPopulateAccessibilityEventEvent;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;

import androidx.core.view.AccessibilityDelegateCompat;

@LayoutSpec
public class PlaygroundComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean isChecked) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .clickHandler(PlaygroundComponent.onClick(c))
        .onPopulateAccessibilityEventHandler(PlaygroundComponent.onPopulateAccessibilityEvent(c))
        .child(Text.create(c).textSizeSp(20).text(isChecked ? "I am clicked" : "Playground sample"))
        .build();
  }

  @OnUpdateState
  static void updateCheckbox(StateValue<Boolean> isChecked) {
    isChecked.set(!isChecked.get());
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @FromEvent View view,
      @Prop View.OnClickListener clickListener) {
    // clickListener.onClick(view);
    PlaygroundComponent.updateCheckboxSync(c);
  }

  @OnEvent(OnPopulateAccessibilityEventEvent.class)
  static void onPopulateAccessibilityEvent(
      ComponentContext c,
      @FromEvent AccessibilityDelegateCompat superDelegate,
      @FromEvent View host,
      @FromEvent AccessibilityEvent event) {
    superDelegate.onPopulateAccessibilityEvent(host, event);
//    host.performAccessibilityAction(
//        AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
  }

}
