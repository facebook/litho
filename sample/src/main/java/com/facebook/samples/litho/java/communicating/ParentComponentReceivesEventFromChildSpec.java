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

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.java.communicating.CommunicatingFromChildToParent.ComponentEventObserver;
import com.facebook.yoga.YogaEdge;

// start_demo
@LayoutSpec
class ParentComponentReceivesEventFromChildSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<String> infoText) {
    infoText.set("No event received from ChildComponent");
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop ComponentEventObserver observer, @State String infoText) {

    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 30)
        .child(Text.create(c).text("ParentComponent").textSizeDip(30))
        .child(Text.create(c).text(infoText).textSizeDip(15))
        .child(
            ChildComponentSendsEventToParent.create(c)
                .observer(observer)
                .notifyParentEventHandler(
                    ParentComponentReceivesEventFromChild.onNotifyParentEvent(c)))
        .build();
  }

  @OnEvent(NotifyParentEvent.class)
  static void onNotifyParentEvent(ComponentContext c) {
    ParentComponentReceivesEventFromChild.onUpdateInfoText(c);
  }

  @OnUpdateState
  static void onUpdateInfoText(StateValue<String> infoText) {
    infoText.set("Received event from ChildComponent!");
  }
}
// end_demo
