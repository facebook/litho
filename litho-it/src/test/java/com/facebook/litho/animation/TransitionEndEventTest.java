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

import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TransitionEndCallbackTestComponent;
import com.facebook.litho.widget.TransitionEndCallbackTestComponentSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class TransitionEndEventTest {
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();

  @Test
  public void transitionEnCallback_parallelCallback_shouldShowDifferentMessages() {
    final TransitionEndCallbackTestComponentSpec.Caller stateUpdater =
        new TransitionEndCallbackTestComponentSpec.Caller();
    stateUpdater.setTestType(TransitionEndCallbackTestComponentSpec.TestType.SAME_KEY);
    final TransitionEndCallbackTestComponent component =
        TransitionEndCallbackTestComponent.create(mLithoViewRule.getContext())
            .caller(stateUpdater)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Before starting animations")
        .isNullOrEmpty();
    stateUpdater.toggle();
    mTransitionTestRule.step(10);
    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Message changed after first transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_ALPHA);
    mTransitionTestRule.step(20);
    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Message changed after second transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_X);
  }

  @Test
  public void transitionEnCallback_disappearAnimation_shouldDisappearMessage() {
    final TransitionEndCallbackTestComponentSpec.Caller stateUpdater =
        new TransitionEndCallbackTestComponentSpec.Caller();
    stateUpdater.setTestType(TransitionEndCallbackTestComponentSpec.TestType.DISAPPEAR);
    final TransitionEndCallbackTestComponent component =
        TransitionEndCallbackTestComponent.create(mLithoViewRule.getContext())
            .caller(stateUpdater)
            .build();
    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Before starting animations")
        .isNullOrEmpty();
    mTransitionTestRule.step(1000);
    stateUpdater.toggle();
    mTransitionTestRule.step(1000);
    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Message changed after disappear transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_DISAPPEAR);
  }
}
