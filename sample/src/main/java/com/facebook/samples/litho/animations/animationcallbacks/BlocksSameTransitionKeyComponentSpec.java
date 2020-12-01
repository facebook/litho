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
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import java.util.HashSet;
import java.util.Set;

@LayoutSpec
public class BlocksSameTransitionKeyComponentSpec {

  private static final String TRANSITION_KEY = "TRANSITION_KEY";
  private static final String ANIM_ALPHA = "ALPHA";
  private static final String ANIM_X = "X";

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Set<String>> runningAnimations) {
    runningAnimations.set(new HashSet<String>());
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean state,
      @State boolean running,
      @State Set<String> runningAnimations) {
    return Column.create(c)
        .child(Row.create(c).child(Text.create(c).text(running ? "RUNNING" : "STOPPED")))
        .child(
            Column.create(c)
                .alignItems(!state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Text.create(c)
                        .heightDip(50)
                        .widthDip(50)
                        .text(runningAnimations.toString())
                        .backgroundColor(Color.parseColor("#ee1111"))
                        .alpha(!state ? 1.0f : 0.2f)
                        .transitionKey(TRANSITION_KEY)
                        .build()))
        .clickHandler(BlocksSameTransitionKeyComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @State boolean running) {
    BlocksSameTransitionKeyComponent.updateStateSync(c, true);
  }

  @OnEvent(TransitionEndEvent.class)
  static void onTransitionEndEvent(
      ComponentContext c,
      @State Set<String> runningAnimations,
      @FromEvent String transitionKey,
      @FromEvent AnimatedProperty property) {
    if (property == AnimatedProperties.X) {
      BlocksSameTransitionKeyComponent.updateRunningAnimations(c, ANIM_X);
      BlocksSameTransitionKeyComponent.updateStateSync(c, false);
    } else {
      BlocksSameTransitionKeyComponent.updateRunningAnimations(c, ANIM_ALPHA);
    }
  }

  @OnUpdateState
  static void updateRunningAnimations(
      @Param String animatedProperty, StateValue<Set<String>> runningAnimations) {
    runningAnimations.get().remove(animatedProperty);
  }

  @OnUpdateState
  static void updateState(
      StateValue<Boolean> state,
      StateValue<Boolean> running,
      StateValue<Set<String>> runningAnimations,
      @Param boolean toggleRunning) {
    if (toggleRunning && !running.get()) {
      running.set(true);
      runningAnimations.get().add(ANIM_ALPHA);
      runningAnimations.get().add(ANIM_X);
      state.set(!state.get());
    } else if (!toggleRunning) {
      running.set(false);
    }
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create(TRANSITION_KEY)
            .animate(AnimatedProperties.ALPHA)
            .animator(Transition.timing(2000))
            .transitionEndHandler(BlocksSameTransitionKeyComponent.onTransitionEndEvent(c)),
        Transition.create(TRANSITION_KEY)
            .animate(AnimatedProperties.X)
            .animator(Transition.timing(4000))
            .transitionEndHandler(BlocksSameTransitionKeyComponent.onTransitionEndEvent(c)));
  }
}
