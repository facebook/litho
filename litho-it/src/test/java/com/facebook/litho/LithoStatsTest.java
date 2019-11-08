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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class LithoStatsTest {

  private static final String LOG_TAG = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private StateUpdateTestComponent mTestComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;

  @Before
  public void setup() {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(RuntimeEnvironment.application, LOG_TAG, mComponentsLogger);

    mLayoutThreadShadowLooper = ComponentTestHelper.getDefaultLayoutThreadShadowLooper();
    mTestComponent = new StateUpdateTestComponent();

    mComponentTree = ComponentTree.create(mContext, mTestComponent).build();

    mLithoView = new LithoView(mContext);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @Test
  public void updateStateAsync_incrementsAsyncCountAndTotalCount() {
    final long beforeSync = LithoStats.getStateUpdatesSync();
    final long beforeAsync = LithoStats.getStateUpdatesAsync();
    final long beforeTotal = LithoStats.getAppliedStateUpdates();

    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getStateUpdatesSync();
    final long afterAsync = LithoStats.getStateUpdatesAsync();
    final long afterTotal = LithoStats.getAppliedStateUpdates();

    assertThat(afterSync - beforeSync).isEqualTo(0);
    assertThat(afterAsync - beforeAsync).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void updateStateSync_incrementsSyncAndTotalCount() {
    final long beforeSync = LithoStats.getStateUpdatesSync();
    final long beforeAsync = LithoStats.getStateUpdatesAsync();
    final long beforeTotal = LithoStats.getAppliedStateUpdates();

    mComponentTree.updateStateSync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getStateUpdatesSync();
    final long afterAsync = LithoStats.getStateUpdatesAsync();
    final long afterTotal = LithoStats.getAppliedStateUpdates();

    assertThat(afterSync - beforeSync).isEqualTo(1);
    assertThat(afterAsync - beforeAsync).isEqualTo(0);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }
}
