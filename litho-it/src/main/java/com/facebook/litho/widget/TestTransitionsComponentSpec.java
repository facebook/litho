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

package com.facebook.litho.widget;

import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaAlign;

@LayoutSpec
public class TestTransitionsComponentSpec {
  public static final String RED_TRANSITION_KEY = "red";
  public static final String GREEN_TRANSITION_KEY = "green";
  public static final String BLUE_TRANSITION_KEY = "blue";

  public enum TestType {
    PARALLEL_TRANSITION,
    SEQUENCE_TRANSITION,
    STAGGER_TRANSITION,
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Caller caller, @State boolean state) {
    caller.set(c);
    return Column.create(c)
        .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#ee1111"))
                .transitionKey(RED_TRANSITION_KEY)
                .viewTag(RED_TRANSITION_KEY)
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#11ee11"))
                .transitionKey(GREEN_TRANSITION_KEY)
                .viewTag(GREEN_TRANSITION_KEY)
                .build())
        .child(
            Row.create(c)
                .heightDip(40)
                .widthDip(40)
                .backgroundColor(Color.parseColor("#1111ee"))
                .transitionKey(BLUE_TRANSITION_KEY)
                .viewTag(BLUE_TRANSITION_KEY)
                .build())
        .build();
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> state) {
    state.set(!state.get());
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c, @Prop Caller caller) {
    Transition transition;
    switch (caller.testType) {
      case SEQUENCE_TRANSITION:
        transition =
            Transition.sequence(
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X));
        break;
      case PARALLEL_TRANSITION:
        transition =
            Transition.parallel(
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X));
        break;
      case STAGGER_TRANSITION:
        transition =
            Transition.stagger(
                50,
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X));
        break;
      default:
        throw new IllegalStateException("Should never happen");
    }
    return transition;
  }

  public static class Caller {
    ComponentContext c;
    TestType testType;

    void set(ComponentContext c) {
      this.c = c;
    }

    public void setTestType(TestType testType) {
      this.testType = testType;
    }

    public void toggle() {
      TestTransitionsComponent.updateStateSync(c);
    }
  }
}
