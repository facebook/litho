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

package com.facebook.samples.litho.java.communicating;

import android.graphics.Color;
import com.facebook.litho.Border;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class ParentComponentSendsEventToChildSpec {

  // start_define_handle
  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, final StateValue<Handle> childHandle) {
    childHandle.set(new Handle());
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @State int counterForChildComponentText, @State Handle childHandle) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 30)
        .child(Text.create(c).text("ParentComponent").textSizeDip(30))
        .child(
            Text.create(c)
                .paddingDip(YogaEdge.ALL, 5)
                .text("Click to trigger show toast event on ChildComponent with handle")
                .marginDip(YogaEdge.TOP, 15)
                .border(
                    Border.create(c)
                        .color(YogaEdge.ALL, Color.BLACK)
                        .radiusDip(2f)
                        .widthDip(YogaEdge.ALL, 1)
                        .build())
                .textSizeDip(15)
                .clickHandler(ParentComponentSendsEventToChild.onClickShowToast(c, childHandle)))
        .child(
            ChildComponentReceivesEventFromParent.create(c)
                .textFromParent("Child with handle")
                .handle(childHandle))
        // end_define_handle
        // start_update_prop
        .child(
            Text.create(c)
                .paddingDip(YogaEdge.ALL, 5)
                .text("Click to send new text to ChildComponent")
                .border(
                    Border.create(c)
                        .color(YogaEdge.ALL, Color.BLACK)
                        .radiusDip(2f)
                        .widthDip(YogaEdge.ALL, 1)
                        .build())
                .textSizeDip(15)
                .clickHandler(ParentComponentSendsEventToChild.onClickCounter(c)))
        .child(
            ChildComponentReceivesEventFromParent.create(c)
                .textFromParent("Version " + counterForChildComponentText))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickCounter(ComponentContext c) {
    ParentComponentSendsEventToChild.onUpdateCounterForChildComponent(c);
  }

  @OnUpdateState
  static void onUpdateCounterForChildComponent(StateValue<Integer> counterForChildComponentText) {
    counterForChildComponentText.set(counterForChildComponentText.get() + 1);
  }
  // end_update_prop

  // start_trigger
  @OnEvent(ClickEvent.class)
  static void onClickShowToast(ComponentContext c, @Param Handle childHandle) {
    ChildComponentReceivesEventFromParent.triggerOnShowToastEvent(
        c, childHandle, "ChildComponent received event from parent!");
  }
  // end_trigger
}
