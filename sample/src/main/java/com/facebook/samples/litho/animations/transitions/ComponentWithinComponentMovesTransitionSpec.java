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

package com.facebook.samples.litho.animations.transitions;

import android.graphics.Color;
import androidx.annotation.Dimension;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class ComponentWithinComponentMovesTransitionSpec {

  private static final @Dimension(unit = Dimension.DP) float SIZE_DP = 50;

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Boolean> shouldAlignStart) {
    shouldAlignStart.set(true);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean shouldAlignStart) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20)
        .alignItems(shouldAlignStart ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .alignItems(shouldAlignStart ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .heightDip(SIZE_DP)
                .widthDip(SIZE_DP)
                .backgroundColor(Color.RED)
                .child(Row.create(c).heightDip(10).widthDip(10).backgroundColor(Color.BLUE)))
        .clickHandler(ComponentWithinComponentMovesTransition.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    ComponentWithinComponentMovesTransition.updateState(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> shouldAlignStart) {
    shouldAlignStart.set(!shouldAlignStart.get());
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.allLayout();
  }
}
