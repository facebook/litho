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
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

// start_parent_mediator
@LayoutSpec
class ParentComponentMediatorSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int selectedPosition) {

    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 30)
        .child(Text.create(c).text("ParentComponent").textSizeDip(30))
        .child(
            ChildComponentSiblingCommunication.create(c)
                .id(0)
                .isSelected(selectedPosition == 0)
                .selectedRadioButtonEventHandler(
                    ParentComponentMediator.onSelectedRadioButtonEvent(c)))
        .child(
            ChildComponentSiblingCommunication.create(c)
                .id(1)
                .isSelected(selectedPosition == 1)
                .selectedRadioButtonEventHandler(
                    ParentComponentMediator.onSelectedRadioButtonEvent(c)))
        .build();
  }

  @OnEvent(SelectedRadioButtonEvent.class)
  static void onSelectedRadioButtonEvent(ComponentContext c, @FromEvent int selectedId) {
    ParentComponentMediator.onUpdateSelectedRadioButtonId(c, selectedId);
  }

  @OnUpdateState
  static void onUpdateSelectedRadioButtonId(
      StateValue<Integer> selectedPosition, @Param int selectedId) {
    selectedPosition.set(selectedId);
  }
}
// end_parent_mediator
