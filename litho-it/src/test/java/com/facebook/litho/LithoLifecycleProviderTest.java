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

package com.facebook.litho;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.LayoutSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LithoLifecycleProviderTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private LithoView mLithoView;
  private LayoutSpecLifecycleTester mComponent;
  private MountSpecLifecycleTester mMountableComponent;
  private LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate;
  private List<LifecycleStep.StepInfo> mSteps;
  private LifecycleTracker lifecycleTracker;

  @Before
  public void setup() {
    final ComponentContext c = mLithoViewRule.getContext();
    mLithoLifecycleProviderDelegate = new LithoLifecycleProviderDelegate();
    mSteps = new ArrayList<>();
    lifecycleTracker = new LifecycleTracker();
    mComponent = LayoutSpecLifecycleTester.create(c).widthPx(10).heightPx(5).steps(mSteps).build();
    mMountableComponent =
        MountSpecLifecycleTester.create(c)
            .widthPx(10)
            .heightPx(5)
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoView = LithoView.create(c, Column.create(c).build(), mLithoLifecycleProviderDelegate);
    mLithoViewRule.useLithoView(mLithoView);
  }

  @After
  public void resetViews() {
    mSteps.clear();
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.DESTROYED);
  }

  @Test
  public void lithoLifecycleProviderDelegateInvisibleToVisibleTest() {
    mLithoViewRule
        .setRoot(mComponent)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure()
        .layout();
    mLithoViewRule.getLithoView().notifyVisibleBoundsChanged(new Rect(0, 0, 10, 10), true);
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Invisible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE);

    mSteps.clear();

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE);
  }

  @Test
  public void lithoLifecycleProviderDelegateInvisibleToInvisibleTest() {
    mLithoViewRule
        .setRoot(mComponent)
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10);
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Invisible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE);

    mSteps.clear();

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Invisible event is expected to be dispatched")
        .isEmpty();
  }

  @Test
  public void lithoLifecycleProviderDelegateVisibleToVisibleTest() {
    mLithoViewRule
        .setRoot(mComponent)
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10);
    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE);

    mSteps.clear();

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    assertThat(LifecycleStep.getSteps(mSteps))
        .describedAs("Visible event is expected to be dispatched")
        .isEmpty();
  }

  @Test
  public void lithoLifecycleProviderDelegateVisibleToDestroyedTest() {
    mLithoViewRule
        .setRoot(mMountableComponent)
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10);

    ComponentsConfiguration.unmountAllWhenComponentTreeSetToNull = true;

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE);

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_MOUNT);

    lifecycleTracker.reset();

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.DESTROYED);

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_UNMOUNT);
  }

  @Test
  public void lithoLifecycleProviderComponentTreeResetVisibilityFlags() {
    mLithoViewRule
        .setRoot(mComponent)
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10);

    mLithoLifecycleProviderDelegate.moveToLifecycle(
        LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE);
    boolean hasVisibilityHint =
        Whitebox.getInternalState(mLithoViewRule.getLithoView(), "mHasVisibilityHint");
    boolean pauseMountingWhileVisibilityHintFalse =
        Whitebox.getInternalState(
            mLithoViewRule.getLithoView(), "mPauseMountingWhileVisibilityHintFalse");
    assertThat(hasVisibilityHint).isTrue();
    assertThat(pauseMountingWhileVisibilityHintFalse).isTrue();

    mLithoViewRule.useComponentTree(ComponentTree.create(mLithoViewRule.getContext()).build());

    hasVisibilityHint =
        Whitebox.getInternalState(mLithoViewRule.getLithoView(), "mHasVisibilityHint");
    pauseMountingWhileVisibilityHintFalse =
        Whitebox.getInternalState(
            mLithoViewRule.getLithoView(), "mPauseMountingWhileVisibilityHintFalse");

    assertThat(hasVisibilityHint).isFalse();
    assertThat(pauseMountingWhileVisibilityHintFalse).isFalse();
  }
}
