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

package com.facebook.samples.lithocodelab.examples.modules;

import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.TOP;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class LearningStateComponentSpec {

  @PropDefault static final boolean canClick = true;

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Integer> count) {
    count.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop(optional = true) boolean canClick, @State Integer count) {
    return Text.create(c)
        .text("Clicked " + count + " times.")
        .textSizeDip(50)
        .clickHandler(canClick ? LearningStateComponent.onClick(c) : null)
        .backgroundRes(android.R.color.holo_blue_light)
        .alignSelf(STRETCH)
        .paddingDip(BOTTOM, 20)
        .paddingDip(TOP, 40)
        .build();
  }

  @OnUpdateState
  static void incrementClickCount(StateValue<Integer> count) {
    count.set(count.get() + 1);
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    LearningStateComponent.incrementClickCountSync(c);
  }
}
