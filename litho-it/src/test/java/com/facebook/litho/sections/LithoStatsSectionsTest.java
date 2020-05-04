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

package com.facebook.litho.sections;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.os.Looper;
import com.facebook.litho.Component;
import com.facebook.litho.StateContainer;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.sections.TestSectionCreator;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class LithoStatsSectionsTest {

  private SectionContext mSectionContext;
  private ShadowLooper mChangeSetThreadShadowLooper;

  @Before
  public void setup() {
    mSectionContext = new SectionContext(getApplicationContext());
    mChangeSetThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(SectionTree.class, "getDefaultChangeSetThreadLooper"));
  }

  @After
  public void tearDown() {
    // If a test fails, make sure the shadow looper gets cleared out anyway so it doesn't impact
    // other tests.
    mChangeSetThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void updateStateSync_incrementsSyncAndTotalCount() {
    final long beforeSync = LithoStats.getSectionTriggeredSyncStateUpdateCount();
    final long beforeAsync = LithoStats.getSectionTriggeredAsyncStateUpdateCount();
    final long beforeTotal = LithoStats.getSectionAppliedStateUpdateCount();

    final Section section =
        TestSectionCreator.createChangeSetComponent("leaf", Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();

    tree.setRoot(section);

    final StateContainer.StateUpdate stateUpdate = new StateContainer.StateUpdate(0);
    changeSetHandler.clear();
    tree.updateState("key", stateUpdate, "test");

    final long afterSync = LithoStats.getSectionTriggeredSyncStateUpdateCount();
    final long afterAsync = LithoStats.getSectionTriggeredAsyncStateUpdateCount();
    final long afterTotal = LithoStats.getSectionAppliedStateUpdateCount();

    assertThat(afterSync - beforeSync).isEqualTo(1);
    assertThat(afterAsync - beforeAsync).isEqualTo(0);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void updateStateAsync_incrementsAsyncAndTotalCount() {
    final long beforeSync = LithoStats.getSectionTriggeredSyncStateUpdateCount();
    final long beforeAsync = LithoStats.getSectionTriggeredAsyncStateUpdateCount();
    final long beforeTotal = LithoStats.getSectionAppliedStateUpdateCount();

    final Section section =
        TestSectionCreator.createChangeSetComponent("leaf", Change.insert(0, makeComponentInfo()));
    section.setKey("key");

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();

    tree.setRoot(section);

    final StateContainer.StateUpdate stateUpdate = new StateContainer.StateUpdate(0);
    changeSetHandler.clear();
    tree.updateStateAsync("key", stateUpdate, "test");

    mChangeSetThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getSectionTriggeredSyncStateUpdateCount();
    final long afterAsync = LithoStats.getSectionTriggeredAsyncStateUpdateCount();
    final long afterTotal = LithoStats.getSectionAppliedStateUpdateCount();

    assertThat(afterSync - beforeSync).isEqualTo(0);
    assertThat(afterAsync - beforeAsync).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void setRoot_incrementsCalculateChangesetOnUIAndTotalCount() {
    final long beforeChangesetCalculationCount = LithoStats.getSectionCalculateNewChangesetCount();
    final long beforeChangesetCalculationOnUICount =
        LithoStats.getSectionCalculateNewChangesetOnUICount();

    final Section section =
        TestSectionCreator.createChangeSetComponent("leaf", Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();

    tree.setRoot(section);

    final long afterChangesetCalculationCount = LithoStats.getSectionCalculateNewChangesetCount();
    final long afterChangesetCalculationOnUICount =
        LithoStats.getSectionCalculateNewChangesetOnUICount();

    assertThat(afterChangesetCalculationCount - beforeChangesetCalculationCount).isEqualTo(1);
    assertThat(afterChangesetCalculationOnUICount - beforeChangesetCalculationOnUICount)
        .isEqualTo(1);
  }

  @Test
  public void setRootAsync_incrementsCalculateChangesetTotalCount() throws InterruptedException {
    final long beforeChangesetCalculationCount = LithoStats.getSectionCalculateNewChangesetCount();
    final long beforeChangesetCalculationOnUICount =
        LithoStats.getSectionCalculateNewChangesetOnUICount();

    final Section section =
        TestSectionCreator.createChangeSetComponent("leaf", Change.insert(0, makeComponentInfo()));

    final TestTarget changeSetHandler = new TestTarget();
    SectionTree tree = SectionTree.create(mSectionContext, changeSetHandler).build();

    tree.setRootAsync(section);

    CountDownLatch latch = new CountDownLatch(1);

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                // We have to do this inside another thread otherwise the execution of
                // mChangeSetThreadShadowLooper will happen on Main Thread
                mChangeSetThreadShadowLooper.runToEndOfTasks();
                latch.countDown();
              }
            })
        .start();
    latch.await();

    final long afterChangesetCalculationCount = LithoStats.getSectionCalculateNewChangesetCount();
    final long afterChangesetCalculationOnUICount =
        LithoStats.getSectionCalculateNewChangesetOnUICount();

    assertThat(afterChangesetCalculationCount - beforeChangesetCalculationCount).isEqualTo(1);
    assertThat(afterChangesetCalculationOnUICount - beforeChangesetCalculationOnUICount)
        .isEqualTo(0);
  }

  private static RenderInfo makeComponentInfo() {
    return ComponentRenderInfo.create().component(mock(Component.class)).build();
  }
}
