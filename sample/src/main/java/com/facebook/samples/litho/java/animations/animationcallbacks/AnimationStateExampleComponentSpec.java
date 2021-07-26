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

package com.facebook.samples.litho.java.animations.animationcallbacks;

import android.graphics.Color;
import androidx.annotation.Dimension;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionEndEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextAlignment;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class AnimationStateExampleComponentSpec {
  private static final @Dimension(unit = Dimension.DP) float SIZE_DP = 50;
  private static final String STATE_RUNNING = "R";
  private static final String STATE_START = "S";
  private static final String STATE_END = "E";

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, StateValue<Boolean> shouldAlignStart, StateValue<String> actualPosition) {
    shouldAlignStart.set(true);
    actualPosition.set(STATE_START);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @State boolean shouldAlignStart, @State String actualPosition) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20)
        .alignItems(shouldAlignStart ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Text.create(c)
                .text(actualPosition)
                .textColor(Color.WHITE)
                .textSizeDip(20)
                .alignment(TextAlignment.CENTER)
                .heightDip(SIZE_DP)
                .widthDip(SIZE_DP)
                .backgroundColor(Color.RED))
        .clickHandler(AnimationStateExampleComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    AnimationStateExampleComponent.updateState(c);
  }

  @OnEvent(TransitionEndEvent.class)
  static void onTransitionEnd(ComponentContext c, @State boolean shouldAlignStart) {
    AnimationStateExampleComponent.updatePosition(c, shouldAlignStart);
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> shouldAlignStart, StateValue<String> actualPosition) {
    actualPosition.set(STATE_RUNNING);
    shouldAlignStart.set(!shouldAlignStart.get());
  }

  @OnUpdateState
  static void updatePosition(StateValue<String> actualPosition, @Param boolean alignStart) {
    if (alignStart) {
      actualPosition.set(STATE_START);
    } else {
      actualPosition.set(STATE_END);
    }
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.allLayout()
        .transitionEndHandler(AnimationStateExampleComponent.onTransitionEnd(c));
  }
}
