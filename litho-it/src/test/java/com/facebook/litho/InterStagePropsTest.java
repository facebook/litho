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

import static com.facebook.litho.LifecycleStep.ON_BIND;
import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_UNBIND;
import static com.facebook.litho.LifecycleStep.ON_UNMOUNT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecInterStagePropsTester;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class InterStagePropsTest {

  private ComponentContext mContext;
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setUp() {
    mContext = mLegacyLithoViewRule.getContext();
    mLegacyLithoViewRule.useLithoView(new LithoView(mContext));
  }

  @Test
  public void interStageProp_FromPrepare_usedIn_OnBind() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root = createComponent(lifecycleTracker, stateUpdater);
    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    lifecycleTracker.reset();

    stateUpdater.increment();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("On Bind should be called")
        .contains(ON_BIND);
  }

  @Test
  public void interStageProp_FromBind_usedIn_OnUnbind() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root = createComponent(lifecycleTracker, stateUpdater);
    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.detachFromWindow();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("On Unbind should be called")
        .contains(ON_UNBIND);
  }

  @Test
  public void interStageProp_FromMeasure_usedIn_OnMount() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root = createComponent(lifecycleTracker, stateUpdater);
    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("On Mount should be called")
        .contains(ON_MOUNT);
  }

  @Test
  public void interStageProp_FromBoundsDefined_usedIn_OnUnMount() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root = createComponent(lifecycleTracker, stateUpdater);
    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.getLithoView().unmountAllItems();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("On Unmount should be called")
        .contains(ON_UNMOUNT);
  }

  private Component createComponent(
      LifecycleTracker lifecycleTracker, SimpleStateUpdateEmulatorSpec.Caller stateUpdater) {
    return Column.create(mLegacyLithoViewRule.getContext())
        .child(
            MountSpecInterStagePropsTester.create(mLegacyLithoViewRule.getContext())
                .lifecycleTracker(lifecycleTracker))
        .child(
            SimpleStateUpdateEmulator.create(mLegacyLithoViewRule.getContext())
                .caller(stateUpdater))
        .build();
  }
}
