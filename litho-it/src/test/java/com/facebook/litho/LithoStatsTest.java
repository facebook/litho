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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TextInput;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@LooperMode(LEGACY)
@RunWith(LithoTestRunner.class)
public class LithoStatsTest {

  private static final String LOG_TAG = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private StateUpdateTestComponent mTestComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;
  private String mTestComponentKey;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), LOG_TAG, mComponentsLogger);

    mLayoutThreadShadowLooper = ComponentTestHelper.getDefaultLayoutThreadShadowLooper();
    mTestComponent = new StateUpdateTestComponent();
    mTestComponentKey = mTestComponent.getKey();

    mComponentTree = ComponentTree.create(mContext, mTestComponent).build();

    mLithoView = new LithoView(mContext);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @Test
  public void updateStateAsync_incrementsAsyncCountAndTotalCount() {
    final long beforeSync = LithoStats.getComponentTriggeredSyncStateUpdateCount();
    final long beforeAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount();
    final long beforeTotal = LithoStats.getComponentAppliedStateUpdateCount();

    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getComponentTriggeredSyncStateUpdateCount();
    final long afterAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount();
    final long afterTotal = LithoStats.getComponentAppliedStateUpdateCount();

    assertThat(afterSync - beforeSync).isEqualTo(0);
    assertThat(afterAsync - beforeAsync).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void updateStateSync_incrementsSyncAndTotalCount() {
    final long beforeSync = LithoStats.getComponentTriggeredSyncStateUpdateCount();
    final long beforeAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount();
    final long beforeTotal = LithoStats.getComponentAppliedStateUpdateCount();

    mComponentTree.updateStateSync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getComponentTriggeredSyncStateUpdateCount();
    final long afterAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount();
    final long afterTotal = LithoStats.getComponentAppliedStateUpdateCount();

    assertThat(afterSync - beforeSync).isEqualTo(1);
    assertThat(afterAsync - beforeAsync).isEqualTo(0);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void setRoot_incrementsCalculateLayoutOnUIAndTotalCount() {
    final long beforeLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount();
    final long beforeLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount();

    mComponentTree.setRoot(new StateUpdateTestComponent());

    final long afterLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount();
    final long afterLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount();

    assertThat(afterLayoutCalculationCount - beforeLayoutCalculationCount).isEqualTo(1);
    assertThat(afterLayoutCalculationOnUICount - beforeLayoutCalculationOnUICount).isEqualTo(1);
  }

  @Test
  public void setRootAsync_incrementsCalculateLayoutTotalCount() throws InterruptedException {
    final long beforeLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount();
    final long beforeLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount();

    mComponentTree.setRootAsync(new StateUpdateTestComponent());

    CountDownLatch latch = new CountDownLatch(1);

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                // We have to do this inside another thread otherwise the execution of
                // mChangeSetThreadShadowLooper will happen on Main Thread
                mLayoutThreadShadowLooper.runOneTask();
                latch.countDown();
              }
            })
        .start();
    latch.await();

    final long afterLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount();
    final long afterLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount();

    assertThat(afterLayoutCalculationCount - beforeLayoutCalculationCount).isEqualTo(1);
    assertThat(afterLayoutCalculationOnUICount - beforeLayoutCalculationOnUICount).isEqualTo(0);
  }

  @Test
  public void mount_incrementsMountCount() {
    final long beforeMountCount = LithoStats.getComponentMountCount();

    final Component component = TextInput.create(mLithoViewRule.getContext()).build();
    mLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    final long afterMountCount = LithoStats.getComponentMountCount();
    assertThat(afterMountCount - beforeMountCount).isEqualTo(1);
  }
}
