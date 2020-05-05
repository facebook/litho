// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.litho.animation;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.choreographercompat.ChoreographerCompatImpl;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.dataflow.springs.SpringConfig;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TransitionEndCallbackTestComponent;
import com.facebook.litho.widget.TransitionEndCallbackTestComponentSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class TransitionEndEventTest {
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();
  private boolean mIsAnimationDisabled;
  private MockTimingSource mFakeTimingSource;

  @Before
  public void setUp() {
    mIsAnimationDisabled = ComponentsConfiguration.isAnimationDisabled;
    ComponentsConfiguration.isAnimationDisabled = false;
    mFakeTimingSource = new MockTimingSource();
    mFakeTimingSource.start();
    DataFlowGraph.setInstance(DataFlowGraph.create(mFakeTimingSource));
    SpringConfig.defaultConfig = new SpringConfig(20, 10);
    ChoreographerCompatImpl.setInstance(mFakeTimingSource);
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.isAnimationDisabled = mIsAnimationDisabled;
  }

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
    mFakeTimingSource.step(10);
    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Message changed after first transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_ALPHA);
    mFakeTimingSource.step(20);
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
    mFakeTimingSource.step(1000);
    stateUpdater.toggle();
    mFakeTimingSource.step(1000);
    assertThat(stateUpdater.getTransitionEndMessage())
        .describedAs("Message changed after disappear transition ended")
        .isEqualTo(TransitionEndCallbackTestComponentSpec.ANIM_DISAPPEAR);
  }
}
