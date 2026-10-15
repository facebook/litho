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

package com.facebook.samples.litho.java.animations.transitions;

import android.graphics.Color;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class OnUpdateStateWithTransitionExampleSpec {

  public static final String RED_TRANSITION_KEY = "red";
  public static final String GREEN_TRANSITION_KEY = "green";
  public static final String BLUE_TRANSITION_KEY = "blue";

  @OnCreateInitialState
  static void onCreateInitialState(final ComponentContext c, StateValue<Boolean> state) {
    state.set(false);
  }

  @OnEvent(ClickEvent.class)
  static void onClickRed(ComponentContext c) {
    OnUpdateStateWithTransitionExample.updateStateWithTransition(c);
  }

  @OnEvent(ClickEvent.class)
  static void onClickGreen(ComponentContext c) {
    OnUpdateStateWithTransitionExample.onUpdateStateSync(c);
  }

  @OnCreateLayout
  static Component onCreateLayout(final ComponentContext componentContext, @State boolean state) {
    return Column.create(componentContext)
        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(componentContext)
                .clickHandler(OnUpdateStateWithTransitionExample.onClickRed(componentContext))
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey(RED_TRANSITION_KEY)
                .viewTag(RED_TRANSITION_KEY)
                .build())
        .child(
            Row.create(componentContext)
                .clickHandler(OnUpdateStateWithTransitionExample.onClickGreen(componentContext))
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey(GREEN_TRANSITION_KEY)
                .viewTag(GREEN_TRANSITION_KEY)
                .build())
        .child(
            Row.create(componentContext)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey(BLUE_TRANSITION_KEY)
                .viewTag(BLUE_TRANSITION_KEY)
                .build())
        .build();
  }

  @OnUpdateStateWithTransition
  static Transition updateState(StateValue<Boolean> state) {
    state.set(!state.get());
    return Transition.sequence(
        Transition.create(RED_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X),
        Transition.create(GREEN_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X),
        Transition.create(BLUE_TRANSITION_KEY)
            .animator(Transition.timing(144))
            .animate(AnimatedProperties.X));
  }

  @OnUpdateState
  static void onUpdateState(StateValue<Boolean> state) {
    state.set(!state.get());
  }
}
