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
import static com.facebook.litho.ComponentTree.STATE_UPDATES_IN_LOOP_THRESHOLD;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.StateContainer.StateUpdate;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.facebook.litho.StateUpdateTestComponent.TestStateContainer;
import com.facebook.litho.components.StateUpdateTestLayout;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class StateUpdatesTest {

  private int mWidthSpec;
  private int mHeightSpec;

  private static final String LOG_TAG = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private StateUpdateTestComponent mTestComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;

  @Before
  public void setup() {
    setup(false);
  }

  public void setup(boolean enableComponentTreeSpy) {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), LOG_TAG, mComponentsLogger);
    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);

    mLayoutThreadShadowLooper = ComponentTestHelper.getDefaultLayoutThreadShadowLooper();
    mTestComponent = new StateUpdateTestComponent();

    mComponentTree = ComponentTree.create(mContext, mTestComponent).build();

    if (enableComponentTreeSpy) {
      mComponentTree = spy(mComponentTree);
    }

    mLithoView = new LithoView(mContext);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @After
  public void tearDown() {
    // Empty all pending runnables.
    mLayoutThreadShadowLooper.runToEndOfTasks();
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
    final LithoView lithoView = new LithoView(mContext);
    lithoView.setComponent(component);
    lithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(lithoView);
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
    final LithoView lithoView = new LithoView(mContext);
    lithoView.setComponent(component);
    lithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(lithoView);
  }

  @Test
  public void testKeepInitialStateValues() {
    TestStateContainer previousStateContainer =
        (TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey());
    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);
  }

  @Test
  public void testKeepUpdatedStateValue() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    TestStateContainer previousStateContainer =
        (TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey());
    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testClearUnusedStateContainers() {
    mComponentTree.updateStateSync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains(mTestComponent.getGlobalKey())).isTrue();

    final Component child1 = new StateUpdateTestComponent();
    child1.setKey("key");

    mLithoView.setComponent(child1);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
    mComponentTree.updateStateSync(
        child1.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains("key")).isTrue();
  }

  @Test
  public void testClearAppliedStateUpdates() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(getPendingStateUpdatesForComponent(mTestComponent.getComponentForStateUpdate()))
        .isNull();
  }

  @Test
  public void testEnqueueStateUpdate() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    assertThat(
            ((TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey()))
                .mCount)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent.getComponentForStateUpdate()))
        .hasSize(1);
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
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(
            mTestComponent
                .getComponentForStateUpdate()
                .getScopedContext()
                .getStateHandler()
                .getAppliedStateUpdates())
        .hasSize(1);
  }

  @Test
  public void testSetInitialStateValue() {
    assertThat(mTestComponent.getCount())
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);
  }

  @Test
  public void testUpdateState() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
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
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createIncrementStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testLazyUpdateState_isCommittedOnlyOnRelayout() {
    mComponentTree.updateStateLazy(
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createIncrementStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE);

    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createNoopStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testLazyUpdateState_isCommittedInCorrectOrder() {
    mComponentTree.updateStateLazy(
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createIncrementStateUpdate());
    mComponentTree.updateStateLazy(
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createMultiplyStateUpdate());
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void
      testLazyUpdateState_everyLazyUpdateThatManagesToBeEnqueuedBeforeActualRelayoutGetsCommitted() {
    mComponentTree.updateStateLazy(
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createIncrementStateUpdate());
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createMultiplyStateUpdate(),
        "test",
        false);

    mComponentTree.updateStateLazy(
        mTestComponent.getGlobalKey(), StateUpdateTestComponent.createIncrementStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void testTransferState() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testTransferAndUpdateState() {
    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    mComponentTree.updateStateAsync(
        mTestComponent.getGlobalKey(),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
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
            componentTree.setRootAndSizeSpec(
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
            try {
              waitForFirstThreadToStart.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            componentTree.setRootAndSizeSpec(
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

    try {
      check.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(componentTree.getInitialStateContainer().mInitialStates.isEmpty()).isTrue();
    assertThat(componentTree.getInitialStateContainer().mPendingStateHandlers.isEmpty()).isTrue();
    assertThat(stateUpdateCalled.intValue()).isEqualTo(1);
    assertThat(stateValue.intValue()).isEqualTo(secondStateValue.intValue());
    assertThat(stateValue.intValue()).isEqualTo(10);
  }

  private StateHandler getStateHandler() {
    return Whitebox.getInternalState(mComponentTree, "mStateHandler");
  }

  private Map<String, StateContainer> getStateContainersMap() {
    return getStateHandler().getStateContainers();
  }

  private Map<String, List<StateUpdate>> getPendingStateUpdates() {
    return getStateHandler().getPendingStateUpdates();
  }

  private List<StateUpdate> getPendingStateUpdatesForComponent(Component component) {
    return getPendingStateUpdates().get(component.getGlobalKey());
  }
}
