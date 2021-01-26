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

package com.facebook.litho.animation;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateCaller;
import com.facebook.litho.Transition;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TestAnimationsComponent;
import com.facebook.litho.widget.TestAnimationsComponentSpec;
import com.facebook.yoga.YogaAlign;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/**
 * We test the different kind of Transitions and how they compose different animations.
 *
 * <p>All transitions are timed to 144 ms which translates to 9 frames (we actually step 10 because
 * * the first one does not count as it only initializes the values in the {@link *
 * com.facebook.litho.dataflow.TimingNode}) based on {@link MockTimingSource}.FRAME_TIME_MS) which *
 * is 16ms per frame.
 */
@SuppressLint("ColorConstantUsageIssue")
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class TransitionsTest {
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();
  private final StateCaller mStateCaller = new StateCaller();

  public static final String RED_TRANSITION_KEY = "red";
  public static final String GREEN_TRANSITION_KEY = "green";
  public static final String BLUE_TRANSITION_KEY = "blue";

  // This could be a lambda but it fails ci.
  TestAnimationsComponentSpec.TestComponent mTestComponent =
      new TestAnimationsComponentSpec.TestComponent() {
        @Override
        public Component getComponent(ComponentContext componentContext, boolean state) {
          return Column.create(componentContext)
              .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
              .child(
                  Row.create(componentContext)
                      .heightDip(40)
                      .widthDip(40)
                      .backgroundColor(Color.parseColor("#ee1111"))
                      .transitionKey(RED_TRANSITION_KEY)
                      .viewTag(RED_TRANSITION_KEY)
                      .build())
              .child(
                  Row.create(componentContext)
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
      };

  @Test
  public void
      transitionAnimations_runTransitionsInSequence_elementsShouldAnimateOneAfterTheOther() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.sequence(
                    Transition.create(RED_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X),
                    Transition.create(GREEN_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X),
                    Transition.create(BLUE_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X)))
            .testComponent(mTestComponent)
            .build();

    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(RED_TRANSITION_KEY);
    View greenView = mLithoViewRule.findViewWithTag(GREEN_TRANSITION_KEY);
    View blueView = mLithoViewRule.findViewWithTag(BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    mStateCaller.update();

    mTransitionTestRule.step(10);

    assertThat(redView.getX()).describedAs("redView after 30 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 30 frames").isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView after 30 frames").isEqualTo(1040);

    mTransitionTestRule.step(10);

    assertThat(redView.getX()).describedAs("redView after 30 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 30 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 30 frames").isEqualTo(1040);

    mTransitionTestRule.step(10);

    assertThat(redView.getX()).describedAs("redView after 30 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 30 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 30 frames").isEqualTo(0);
  }

  @Test
  public void transitionAnimations_runTransitionsInParallel_elementsShouldAnimateAtTheSameTime() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.parallel(
                    Transition.create(RED_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X),
                    Transition.create(GREEN_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X),
                    Transition.create(BLUE_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X)))
            .testComponent(mTestComponent)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(RED_TRANSITION_KEY);
    View greenView = mLithoViewRule.findViewWithTag(GREEN_TRANSITION_KEY);
    View blueView = mLithoViewRule.findViewWithTag(BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    mStateCaller.update();

    mTransitionTestRule.step(10);

    assertThat(redView.getX()).describedAs("redView after 10 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 10 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 10 frames").isEqualTo(0);
  }

  @Test
  public void transitionAnimations_runTransitionsInStagger_elementsShouldAnimateStaggered() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
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
                        .animate(AnimatedProperties.X)))
            .testComponent(mTestComponent)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(RED_TRANSITION_KEY);
    View greenView = mLithoViewRule.findViewWithTag(GREEN_TRANSITION_KEY);
    View blueView = mLithoViewRule.findViewWithTag(BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    mStateCaller.update();

    mTransitionTestRule.step(5);

    assertThat(redView.getX()).describedAs("redView after 5 frames").isEqualTo(610.2971f);
    assertThat(greenView.getX()).describedAs("greenView after 5 frames").isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView after 5 frames").isEqualTo(1040);

    mTransitionTestRule.step(5);

    assertThat(redView.getX()).describedAs("redView after 10 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 10 frames").isEqualTo(429.70294f);
    assertThat(blueView.getX()).describedAs("blueView after 10 frames").isEqualTo(1008.64014f);

    mTransitionTestRule.step(5);

    assertThat(redView.getX()).describedAs("redView after 15 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 15 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 15 frames").isEqualTo(259.99988f);

    mTransitionTestRule.step(5);

    assertThat(redView.getX()).describedAs("redView after 20 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 20 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 20 frames").isEqualTo(0);
  }

  @Test
  public void transitionAnimations_runDelayedTransition_elementsShouldAnimateAfterDelay() {
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.delay(
                    144,
                    Transition.create(RED_TRANSITION_KEY)
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X)))
            .testComponent(mTestComponent)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(RED_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    mStateCaller.update();
    mTransitionTestRule.step(10);
    assertThat(redView.getX()).describedAs("redView after 10 frames").isEqualTo(1040);
    mTransitionTestRule.step(10);
    assertThat(redView.getX()).describedAs("redView after 20 frames").isEqualTo(0);
  }
}
