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

package com.facebook.samples.litho.animations.animationcallbacks;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionEndEvent;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class ToggleMoveBlocksExampleComponentSpec {

  private static final String TRANSITION_KEY_RED = "red";
  private static final String TRANSITION_KEY_BLUE = "blue";
  private static final String TRANSITION_KEY_GREEN = "green";
  private static final String[] ALL_TRANSITION_KEYS = {
    TRANSITION_KEY_RED, TRANSITION_KEY_BLUE, TRANSITION_KEY_GREEN
  };

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int state, @State boolean running) {
    final boolean redLeft = state == 0 || state == 4 || state == 5;
    final boolean blueLeft = state == 0 || state == 1 || state == 5;
    final boolean greenLeft = state == 0 || state == 1 || state == 2;
    return Column.create(c)
        .child(Row.create(c).child(Text.create(c).text(running ? "RUNNING" : "STOPPED")))
        .child(
            Column.create(c)
                .alignItems(redLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#ee1111"))
                        .transitionKey(TRANSITION_KEY_RED)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(blueLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#1111ee"))
                        .transitionKey(TRANSITION_KEY_BLUE)
                        .build()))
        .child(
            Column.create(c)
                .alignItems(greenLeft ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Row.create(c)
                        .heightDip(40)
                        .widthDip(40)
                        .backgroundColor(Color.parseColor("#11ee11"))
                        .transitionKey(TRANSITION_KEY_GREEN)
                        .build()))
        .clickHandler(ToggleMoveBlocksExampleComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @State boolean running) {
    ToggleMoveBlocksExampleComponent.updateStateSync(c, true);
  }

  @OnEvent(TransitionEndEvent.class)
  static void onTransitionEndEvent(ComponentContext c, @FromEvent String transitionKey) {
    // handler
    ToggleMoveBlocksExampleComponent.updateStateSync(c, false);
  }

  @OnUpdateState
  static void updateState(
      StateValue<Integer> state, StateValue<Boolean> running, @Param boolean toggleRunning) {
    running.set(toggleRunning ? !running.get() : running.get());
    if (running.get()) {
      state.set((state.get() + 1) % 6);
    }
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.create(ALL_TRANSITION_KEYS)
        .animate(AnimatedProperties.X, AnimatedProperties.Y, AnimatedProperties.ALPHA)
        .transitionEndHandler(ToggleMoveBlocksExampleComponent.onTransitionEndEvent(c));
  }
}
