/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import static com.facebook.litho.StateContainer.StateUpdate;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class StateUpdatesTest {
  private static final int LIFECYCLE_TEST_ID = 1;
  private static final int INITIAL_COUNT_STATE_VALUE = 4;

  private int mWidthSpec;
  private int mHeightSpec;

  private static class NoopStateUpdate implements StateUpdate {
    @Override
    public void updateState(StateContainer stateContainer) {}
  }

  private static class TestStateUpdate implements StateUpdate {
    @Override
    public void updateState(StateContainer stateContainer) {
      TestStateContainer stateContainerImpl = (TestStateContainer) stateContainer;
      stateContainerImpl.mCount = stateContainerImpl.mCount + 1;
    }
  }

  private static class MultiplyStateUpdate implements StateUpdate {
    @Override
    public void updateState(StateContainer stateContainer) {
      TestStateContainer stateContainerImpl = (TestStateContainer) stateContainer;
      stateContainerImpl.mCount = stateContainerImpl.mCount * 2;
    }
  }

  static class TestComponent extends Component {

    private final TestStateContainer mStateContainer;
    private TestComponent shallowCopy;
    private int mId;
    private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
    private final AtomicInteger createInitialStateCount = new AtomicInteger(0);

    public TestComponent() {
      super("TestComponent");
      mStateContainer = new TestStateContainer();
      mId = sIdGenerator.getAndIncrement();
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return this == other;
    }

    @Override
    int getTypeId() {
      return LIFECYCLE_TEST_ID;
    }

    @Override
    protected boolean hasState() {
      return true;
    }

    @Override
    protected void createInitialState(ComponentContext c) {
      mStateContainer.mCount = INITIAL_COUNT_STATE_VALUE;
      createInitialStateCount.incrementAndGet();
    }

    @Override
    protected void transferState(
        StateContainer prevStateContainer, StateContainer nextStateContainer) {
      TestStateContainer prevStateContainerImpl = (TestStateContainer) prevStateContainer;
      TestStateContainer nextStateContainerImpl = (TestStateContainer) nextStateContainer;
      nextStateContainerImpl.mCount = prevStateContainerImpl.mCount;
    }

    int getCount() {
      return mStateContainer.mCount;
    }

    @Override
    synchronized void markLayoutStarted() {
      // No-op because we override makeShallowCopy below :(
    }

    @Override
    public Component makeShallowCopy() {
      return this;
    }

    @Override
    Component makeShallowCopyWithNewId() {
      shallowCopy = (TestComponent) super.makeShallowCopy();
      shallowCopy.mId = sIdGenerator.getAndIncrement();
      shallowCopy.setGlobalKey(getGlobalKey());
      return shallowCopy;
    }

    TestComponent getComponentForStateUpdate() {
      if (shallowCopy == null) {
        return this;
      }
      return shallowCopy.getComponentForStateUpdate();
    }

    @Override
    protected int getId() {
      return mId;
    }

    @Override
    protected StateContainer getStateContainer() {
      return mStateContainer;
    }
  }

  static class TestStateContainer extends StateContainer {
    protected int mCount;
  }

  private static final String mLogTag = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private TestComponent mTestComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;

  @Before
  public void setup() {
    setup(false, false);
  }

  public void setup(boolean enableNestedTreeResolutionExperiment, boolean enableComponentTreeSpy) {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(RuntimeEnvironment.application, mLogTag, mComponentsLogger);
    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
    mTestComponent = new TestComponent();

    mComponentTree =
        ComponentTree.create(mContext, mTestComponent)
            .enableNestedTreeResolutionExperiment(enableNestedTreeResolutionExperiment)
            .build();

    if (enableComponentTreeSpy) {
      mComponentTree = spy(mComponentTree);
    }

    mLithoView = new LithoView(mContext);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @Test
  public void testCreateInitialStateOnceOnly() {
    boolean enabled = ComponentsConfiguration.createInitialStateOncePerThread;
    ComponentsConfiguration.createInitialStateOncePerThread = true;
    final TestComponent testComponent = new TestComponent();
    testComponent.setKey("initstate");
    final TestComponent layoutTestComponent = spy(testComponent);

    ComponentTree componentTree = ComponentTree.create(mContext, testComponent).build();

    final ComponentContext context = spy(new ComponentContext(mContext));
    when(context.getComponentTree()).thenReturn(componentTree);
    when(layoutTestComponent.getScopedContext()).thenReturn(context);
    when(layoutTestComponent.getGlobalKey()).thenReturn("initstate");

    final CountDownLatch check = new CountDownLatch(2);

    Thread thread1 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                final StateHandler stateHandler1 = componentTree.acquireStateHandler();
                stateHandler1.applyStateUpdatesForComponent(layoutTestComponent);
                check.countDown();
              }
            });

    Thread thread2 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                final StateHandler stateHandler2 = componentTree.acquireStateHandler();
                stateHandler2.applyStateUpdatesForComponent(layoutTestComponent);
                check.countDown();
              }
            });

    thread1.start();
    thread2.start();

    try {
      check.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertEquals(1, testComponent.createInitialStateCount.get());
    ComponentsConfiguration.createInitialStateOncePerThread = enabled;
  }

  @Test
  public void testNoCrashOnSameComponentKey() {
    final Component child1 = new TestComponent();
    child1.setKey("key");
    final Component child2 = new TestComponent();
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
    final Component child1 = new TestComponent();
    child1.setKey("key");
    final Component child2 = new TestComponent();
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
    assertThat(previousStateContainer.mCount).isEqualTo(INITIAL_COUNT_STATE_VALUE);
  }

  @Test
  public void testKeepUpdatedStateValue() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    TestStateContainer previousStateContainer =
        (TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey());
    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount).isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testClearUnusedStateContainers() {
    mComponentTree.updateStateSync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains(mTestComponent.getGlobalKey())).isTrue();

    final Component child1 = new TestComponent();
    child1.setKey("key");

    mLithoView.setComponent(child1);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
    mComponentTree.updateStateSync(child1.getGlobalKey(), new TestStateUpdate(), "test");

    assertThat(getStateContainersMap().keySet().size()).isEqualTo(1);
    assertThat(getStateContainersMap().keySet().contains("key")).isTrue();
  }

  @Test
  public void testClearAppliedStateUpdates() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(getPendingStateUpdatesForComponent(mTestComponent.getComponentForStateUpdate())).isNull();
  }

  @Test
  public void testEnqueueStateUpdate() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    assertThat(((TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey())).mCount).isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent.getComponentForStateUpdate())).hasSize(1);
  }

  @Test
  public void testEnqueueStateUpdate_withExperiment() {
    setup(true, false);
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    assertThat(getPendingStateUpdatesForComponent(mTestComponent)).hasSize(1);
    mLayoutThreadShadowLooper.runToEndOfTasks();
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    assertThat(
            ((TestStateContainer) getStateContainersMap().get(mTestComponent.getGlobalKey()))
                .mCount)
        .isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
    assertThat(getPendingStateUpdatesForComponent(mTestComponent.getComponentForStateUpdate()))
        .hasSize(1);
  }

  @Test
  public void testEnqueueStateUpdate_withExperiment_checkAppliedStateUpdate() {
    setup(true, false);
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
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
    assertThat(mTestComponent.getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE);
  }

  @Test
  public void testUpdateState() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testLazyUpdateState_doesNotTriggerRelayout() {
    setup(false, true);
    reset(mComponentTree);

    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(mComponentTree, never())
        .calculateLayoutState(
            any(), any(), anyInt(), anyInt(), anyBoolean(), any(), any(), anyInt(), any());
  }

  @Test
  public void testLazyUpdateState_isCommittedOnlyOnRelayout() {
    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE);

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new NoopStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testLazyUpdateState_isCommittedInCorrectOrder() {
    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new MultiplyStateUpdate());
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo((INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void testLazyUpdateState_everyLazyUpdateThatManagesToBeEnqueuedBeforeActualRelayoutGetsCommitted() {
    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new MultiplyStateUpdate(), "test");

    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount())
        .isEqualTo((INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1);
  }

  @Test
  public void testTransferState() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    mComponentTree.setSizeSpec(mWidthSpec, mHeightSpec);
    assertThat(mTestComponent.getComponentForStateUpdate().getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE + 1);
  }

  @Test
  public void testTransferAndUpdateState() {
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();
    assertThat(mTestComponent.getComponentForStateUpdate().getCount()).isEqualTo(INITIAL_COUNT_STATE_VALUE + 2);
  }

  @Test
  public void testStateUpdateStats_updateAsyncIncrementsTotalCount() {
    final long before = LithoStats.getStateUpdates();

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long after = LithoStats.getStateUpdates();

    assertThat(after - before).isEqualTo(1);
  }

  @Test
  public void testStateUpdateStats_updateAsyncDoesntIncrementSyncCount() {
    final long beforeSync = LithoStats.getStateUpdatesSync();
    final long beforeTotal = LithoStats.getStateUpdates();

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getStateUpdatesSync();
    final long afterTotal = LithoStats.getStateUpdates();

    assertThat(afterSync - beforeSync).isEqualTo(0);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void testStateUpdateStats_updateSyncIncrementsSyncAndTotalCount() {
    final long beforeSync = LithoStats.getStateUpdatesSync();
    final long beforeTotal = LithoStats.getStateUpdates();

    mComponentTree.updateStateSync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterSync = LithoStats.getStateUpdatesSync();
    final long afterTotal = LithoStats.getStateUpdates();

    assertThat(afterSync - beforeSync).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(1);
  }

  @Test
  public void testStateUpdateStats_updateLazyDoesntIncrementTotalCount() {
    mLayoutThreadShadowLooper.runToEndOfTasks();
    final long beforeLazy = LithoStats.getStateUpdatesLazy();
    final long beforeTotal = LithoStats.getStateUpdates();

    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterLazy = LithoStats.getStateUpdatesLazy();
    final long afterTotal = LithoStats.getStateUpdates();

    assertThat(afterLazy - beforeLazy).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(0);
  }

  @Test
  public void testStateUpdateStats_updateLazyIncrementsTotalCountWhenCommitted() {
    mLayoutThreadShadowLooper.runToEndOfTasks();
    final long beforeLazy = LithoStats.getStateUpdatesLazy();
    final long beforeTotal = LithoStats.getStateUpdates();

    mComponentTree.updateStateLazy(mTestComponent.getGlobalKey(), new TestStateUpdate());
    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long afterLazy = LithoStats.getStateUpdatesLazy();
    final long afterTotal = LithoStats.getStateUpdates();

    assertThat(afterLazy - beforeLazy).isEqualTo(1);
    assertThat(afterTotal - beforeTotal).isEqualTo(2);
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

  private Map<String, List<StateUpdate>> getAppliedStateUpdates() {
    return getStateHandler().getAppliedStateUpdates();
  }
}
