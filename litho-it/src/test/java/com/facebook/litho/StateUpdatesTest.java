/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.ComponentTree.STATE_UPDATES_IN_LOOP_THRESHOLD;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.StateContainer.StateUpdate;
import static com.facebook.litho.testing.MeasureSpecTestingUtilsKt.unspecified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import android.view.View;
import com.facebook.litho.StateUpdateTestComponent.TestStateContainer;
import com.facebook.litho.components.StateUpdateTestLayout;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.ThreadTestingUtils;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class StateUpdatesTest {

  private int mWidthSpec;
  private int mHeightSpec;

  private static final String LOG_TAG = "logTag";

  private ComponentContext mContext;
  private StateUpdateTestComponent mTestComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;
  private String mTestComponentKey;
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    setup(false);
  }

  public void setup(boolean enableComponentTreeSpy) {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), LOG_TAG, mComponentsLogger);
    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);

    mTestComponent = new StateUpdateTestComponent();
    mTestComponentKey = mTestComponent.getKey();

    mLegacyLithoViewRule.setRoot(mTestComponent);
    mComponentTree = mLegacyLithoViewRule.getComponentTree();

    if (enableComponentTreeSpy) {
      mComponentTree = spy(mComponentTree);
    }
    mLegacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(mWidthSpec, mHeightSpec);
  }

  @After
  public void tearDown() {
    // Empty all pending runnables.
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
  }

  @Test
  public void testNoCrashOnSameComponentKey() {
    final Component child1 = new StateUpdateTestComponent();
    child1.setKey("key");
    final Component child2 = new StateUpdateTestComponent();
    child2.setKey("key");
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c).child(child1).child(child2).build();
          }
        };
    mLegacyLithoViewRule.setRoot(component);
    mLegacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), unspecified());
  }

  @Test
  public void testNoCrashOnSameComponentKeyNestedContainers() {
    final Component child1 = new StateUpdateTestComponent();
    child1.setKey("key");
    final Component child2 = new StateUpdateTestComponent();
    child2.setKey("key");
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(child1))
                .child(Column.create(c).child(child2))
                .build();
          }
        };
    mLegacyLithoViewRule.setRoot(component);
    mLegacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), unspecified());
  }

  @Test
  public void testKeepInitialStateValues() {
    TestStateContainer previousStateContainer =
        (TestStateContainer) getStateContainersMap().get(mTestComponentKey);
    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);
    assertThat(getRenderStateHandler().getInitialStateContainer().mInitialStates.isEmpty())
        .isTrue();
  }

  @Test
  public void testKeepUpdatedStateValue() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    TestStateContainer previousStateContainer =
        (TestStateContainer) getStateContainersMap().get(mTestComponentKey);
    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testClearUnusedStateContainers() {
    mComponentTree.updateStateSync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains(mTestComponentKey)).isTrue();

    final Component child1 = new StateUpdateTestComponent();
    child1.setKey("key");

    mLegacyLithoViewRule.setRoot(child1);
    mLegacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), unspecified());
    mComponentTree.updateStateSync(
        "$key", StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains("$key")).isTrue();
  }

  @Test
  public void testClearAppliedStateUpdates() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).hasSize(1);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).isNull();
  }

  @Test
  public void testEnqueueStateUpdate() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).hasSize(1);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    assertThat(((TestStateContainer) getStateContainersMap().get(mTestComponentKey)).mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).hasSize(1);
  }

  @Test(expected = RuntimeException.class)
  public void testUpdateStateFromOnCreateLayout_throwsRuntimeExceptionWhenThresholdExceeds() {
    for (int i = 0; i < STATE_UPDATES_IN_LOOP_THRESHOLD; i++) {
      mComponentTree.updateStateInternal(false, "test", true);
    }
  }

  @Test
  public void testEnqueueStateUpdate_checkAppliedStateUpdate() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).hasSize(1);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(getPendingStateUpdatesForComponent(mTestComponentKey)).isNullOrEmpty();
  }

  @Test
  public void testSetInitialStateValue() {
    assertThat(mTestComponent.getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);
  }

  @Test
  public void testUpdateState() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testLazyUpdateState_doesNotTriggerRelayout() {
    setup(true);

    mComponentTree.addMeasureListener(
        new ComponentTree.MeasureListener() {
          @Override
          public void onSetRootAndSizeSpec(
              int layoutVersion, int width, int height, boolean stateUpdate) {
            throw new RuntimeException("Should not have computed a new layout!");
          }
        });
    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate());
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
  }

  @Test
  public void testLazyUpdateState_isCommittedOnlyOnRelayout() {
    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate());
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);

    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createNoopStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testLazyUpdateState_isCommittedInCorrectOrder() {
    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate());
    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createMultiplyStateUpdate());
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void
      testLazyUpdateState_everyLazyUpdateThatManagesToBeEnqueuedBeforeActualRelayoutGetsCommitted() {
    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate());
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createMultiplyStateUpdate(), "test", false);

    mComponentTree.updateStateLazy(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate());
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void testTransferState() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testTransferAndUpdateState() {
    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mComponentTree.updateStateAsync(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false);
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount(mContext))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 2);
  }

  @Test
  public void testStateContainerDrained() {
    ComponentTree componentTree = ComponentTree.create(mContext).build();

    final CountDownLatch check = new CountDownLatch(2);
    final CountDownLatch waitForFirstLayoutToStart = new CountDownLatch(1);
    final CountDownLatch waitForFirstThreadToStart = new CountDownLatch(1);

    AtomicInteger stateUpdateCalled = new AtomicInteger(0);
    AtomicInteger stateValue = new AtomicInteger(0);
    AtomicInteger secondStateValue = new AtomicInteger(0);

    // The threading here is a bit tricky. The idea is that the first thread unlocks the second
    // thread from its onCreateLayout execution and blocks immediately. At this point the second
    // thread's onCreateLayout unblocks the first thread. We are now in the situation that we want
    // to test where both threads are executing a layout and the state for the inner component
    // has not been created yet.

    Thread thread1 =
        new Thread() {
          @Override
          public void run() {
            componentTree.setRootAndSizeSpecSync(
                StateUpdateTestLayout.create(mContext)
                    .awaitable(waitForFirstLayoutToStart)
                    .countDownLatch(waitForFirstThreadToStart)
                    .createStateCount(stateUpdateCalled)
                    .outStateValue(stateValue)
                    .build(),
                SizeSpec.makeSizeSpec(100, EXACTLY),
                SizeSpec.makeSizeSpec(100, EXACTLY));
            check.countDown();
          }
        };

    Thread thread2 =
        new Thread() {
          @Override
          public void run() {

            ThreadTestingUtils.failSilentlyIfInterrupted(waitForFirstThreadToStart::await);
            componentTree.setRootAndSizeSpecSync(
                StateUpdateTestLayout.create(mContext)
                    .awaitable(null)
                    .countDownLatch(waitForFirstLayoutToStart)
                    .createStateCount(stateUpdateCalled)
                    .outStateValue(secondStateValue)
                    .build(),
                SizeSpec.makeSizeSpec(200, EXACTLY),
                SizeSpec.makeSizeSpec(200, EXACTLY));
            check.countDown();
          }
        };

    thread1.start();
    thread2.start();

    ThreadTestingUtils.failSilentlyIfInterrupted(() -> check.await(5000, TimeUnit.MILLISECONDS));

    assertThat(getRenderStateHandler().getInitialStateContainer().mInitialStates.isEmpty())
        .isTrue();
    assertThat(getRenderStateHandler().getInitialStateContainer().mPendingStateHandlers.isEmpty())
        .isTrue();
    assertThat(stateUpdateCalled.intValue()).isEqualTo(1);
    assertThat(stateValue.intValue()).isEqualTo(secondStateValue.intValue());
    assertThat(stateValue.intValue()).isEqualTo(10);
  }

  private StateHandler getRenderStateHandler() {
    final TreeState treeState = Whitebox.getInternalState(mComponentTree, "mTreeState");
    return Whitebox.getInternalState(treeState, "mRenderStateHandler");
  }

  private Map<String, StateContainer> getStateContainersMap() {
    return getRenderStateHandler().getStateContainers();
  }

  private Map<String, List<StateUpdate>> getPendingStateUpdates() {
    return getRenderStateHandler().getPendingStateUpdates();
  }

  private List<StateUpdate> getPendingStateUpdatesForComponent(String globalKey) {
    return getPendingStateUpdates().get(mTestComponentKey);
  }
}
