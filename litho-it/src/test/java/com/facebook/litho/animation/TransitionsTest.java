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

import android.view.View;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TestTransitionsComponent;
import com.facebook.litho.widget.TestTransitionsComponentSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class TransitionsTest {
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();

  @Test
  public void
      transitionAnimations_runTransitionsInSequence_elementsShouldAnimateOneAfterTheOther() {
    final TestTransitionsComponentSpec.Caller stateUpdater =
        new TestTransitionsComponentSpec.Caller();
    stateUpdater.setTestType(TestTransitionsComponentSpec.TestType.SEQUENCE_TRANSITION);
    final TestTransitionsComponent component =
        TestTransitionsComponent.create(mLithoViewRule.getContext()).caller(stateUpdater).build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.RED_TRANSITION_KEY);
    View greenView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.GREEN_TRANSITION_KEY);
    View blueView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    stateUpdater.toggle();

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
    final TestTransitionsComponentSpec.Caller stateUpdater =
        new TestTransitionsComponentSpec.Caller();
    stateUpdater.setTestType(TestTransitionsComponentSpec.TestType.PARALLEL_TRANSITION);
    final TestTransitionsComponent component =
        TestTransitionsComponent.create(mLithoViewRule.getContext()).caller(stateUpdater).build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.RED_TRANSITION_KEY);
    View greenView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.GREEN_TRANSITION_KEY);
    View blueView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    stateUpdater.toggle();

    mTransitionTestRule.step(10);

    assertThat(redView.getX()).describedAs("redView after 10 frames").isEqualTo(0);
    assertThat(greenView.getX()).describedAs("greenView after 10 frames").isEqualTo(0);
    assertThat(blueView.getX()).describedAs("blueView after 10 frames").isEqualTo(0);
  }

  @Test
  public void transitionAnimations_runTransitionsInStagger_elementsShouldAnimateStaggered() {
    final TestTransitionsComponentSpec.Caller stateUpdater =
        new TestTransitionsComponentSpec.Caller();
    stateUpdater.setTestType(TestTransitionsComponentSpec.TestType.STAGGER_TRANSITION);
    final TestTransitionsComponent component =
        TestTransitionsComponent.create(mLithoViewRule.getContext()).caller(stateUpdater).build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    View redView = mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.RED_TRANSITION_KEY);
    View greenView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.GREEN_TRANSITION_KEY);
    View blueView =
        mLithoViewRule.findViewWithTag(TestTransitionsComponentSpec.BLUE_TRANSITION_KEY);

    assertThat(redView.getX()).describedAs("redView should be at start position").isEqualTo(1040);
    assertThat(greenView.getX())
        .describedAs("greenView should be at start position")
        .isEqualTo(1040);
    assertThat(blueView.getX()).describedAs("blueView should be at start position").isEqualTo(1040);

    stateUpdater.toggle();

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
}
