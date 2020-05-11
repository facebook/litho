// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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
