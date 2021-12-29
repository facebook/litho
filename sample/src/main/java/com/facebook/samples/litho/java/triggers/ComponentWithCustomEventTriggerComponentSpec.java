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

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class ComponentWithCustomEventTriggerComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Integer> exampleState) {
    exampleState.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop String titleText, @State Integer exampleState) {
    return Column.create(c)
        .child(Text.create(c).marginDip(YogaEdge.LEFT, 5).text(titleText + exampleState))
        .build();
  }

  @OnUpdateState
  static void increaseExampleState(StateValue<Integer> exampleState, @Param int increaseBy) {
    exampleState.set(exampleState.get() + increaseBy);
  }

  @OnTrigger(CustomEvent.class)
  static void triggerCustomEvent(ComponentContext c, @FromTrigger int increaseBy) {
    ComponentWithCustomEventTriggerComponent.increaseExampleState(c, increaseBy);
  }
}
