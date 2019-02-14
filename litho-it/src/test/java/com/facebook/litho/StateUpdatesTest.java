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

import static com.facebook.litho.ComponentLifecycle.StateUpdate;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.os.Looper;
import com.facebook.litho.stats.LithoStats;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.List;
import java.util.Map;
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

  private static class TestStateUpdate implements StateUpdate {

    @Override
    public void updateState(StateContainer stateContainer) {
      TestStateContainer stateContainerImpl = (TestStateContainer) stateContainer;
      stateContainerImpl.mCount = stateContainerImpl.mCount + 1;
    }
  }

  static class TestComponent extends Component {

    private final TestStateContainer mStateContainer;
    private TestComponent shallowCopy;
    private int mId;
    private static final AtomicInteger sIdGenerator = new AtomicInteger(0);

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
      if (getScopedContext().isNestedTreeResolutionExperimentEnabled()) {
        shallowCopy.setGlobalKey(getGlobalKey());
      }
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

  static class TestStateContainer implements StateContainer {
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
  public void setup() throws Exception {
    setup(false);
  }

  public void setup(boolean enableNestedTreeResolutionExeperiment) throws Exception {
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
            .enableNestedTreeResolutionExeperiment(enableNestedTreeResolutionExeperiment)
            .build();
    mLithoView = new LithoView(mContext);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mLithoView);
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
  public void testEnqueueStateUpdate_withExperiment() throws Exception {
    setup(true);
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
  public void testEnqueueStateUpdate_withExperiment_checkAppliedStateUpdate() throws Exception {
    setup(true);
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
  public void testStateStatsTest() {
    final long before = LithoStats.getStateUpdates();

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long after = LithoStats.getStateUpdates();

    assertThat(after - before).isEqualTo(1);
  }

  @Test
  public void testSyncStateStatsWhenAsyncTest() {
    final long before = LithoStats.getStateUpdatesSync();

    mComponentTree.updateStateAsync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long after = LithoStats.getStateUpdatesSync();

    assertThat(after - before).isEqualTo(0);
  }

  @Test
  public void testSyncStateStatsWhenSyncTest() {
    final long before = LithoStats.getStateUpdatesSync();

    mComponentTree.updateStateSync(mTestComponent.getGlobalKey(), new TestStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    final long after = LithoStats.getStateUpdatesSync();

    assertThat(after - before).isEqualTo(1);
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
