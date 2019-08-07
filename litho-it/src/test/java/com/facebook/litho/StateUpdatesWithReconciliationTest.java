/*
 * Copyright 2019-present Facebook, Inc.
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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class StateUpdatesWithReconciliationTest {

  private static final String mLogTag = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private DummyComponent mRootComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;

  private static final int STATE_VALUE_INITIAL_COUNT = 4;

  @Before
  public void before() {
    ComponentsConfiguration.isEndToEndTestRun = true;
    ComponentsConfiguration.isReconciliationEnabled = true;

    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            return spy(new DefaultInternalNode(c));
          }
        };
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(RuntimeEnvironment.application, mLogTag, mComponentsLogger);
    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
    mRootComponent = new DummyComponent();
    mLithoView = new LithoView(mContext);
    mComponentTree = spy(ComponentTree.create(mContext, mRootComponent).build());

    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @After
  public void after() {
    ComponentsConfiguration.isEndToEndTestRun = false;
    ComponentsConfiguration.isReconciliationEnabled = false;
    NodeConfig.sInternalNodeFactory = null;
  }

  @Test
  public void initial_condition() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    assertThat(layout).isNotNull();
    verify(layout, times(0)).reconcile(any(), any());
  }

  @Test
  public void set_root_new_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(new DummyComponent());

    verify(layout, times(0)).reconcile(any(), any());
  }

  @Test
  public void set_root_same_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent);

    verify(layout, times(0)).reconcile(any(), any());
  }

  @Test
  public void set_root_same_instance_internal() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(current.getRootComponent());

    verify(layout, times(0)).reconcile(any(), any());
  }

  @Test
  public void set_root_shallowCopy() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent.makeShallowCopy());

    verify(layout, times(0)).reconcile(any(), any());
  }

  @Test
  public void update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test");

    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void update_state_async() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateAsync(
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test");
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void simple_update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test");

    verify(layout, times(1)).reconcile(any(), any());
  }

  static class DummyComponent extends Component {

    private final DummyStateContainer mStateContainer;

    public DummyComponent() {
      super("TestComponent");
      mStateContainer = new DummyStateContainer();
    }

    @Override
    protected boolean hasState() {
      return true;
    }

    @Override
    protected void createInitialState(ComponentContext c) {
      mStateContainer.mCount = STATE_VALUE_INITIAL_COUNT;
    }

    @Override
    protected void transferState(
        StateContainer prevStateContainer, StateContainer nextStateContainer) {
      DummyStateContainer prevStateContainerImpl = (DummyStateContainer) prevStateContainer;
      DummyStateContainer nextStateContainerImpl = (DummyStateContainer) nextStateContainer;
      nextStateContainerImpl.mCount = prevStateContainerImpl.mCount;
    }

    @Override
    protected StateContainer getStateContainer() {
      return mStateContainer;
    }
  }

  static class DummyStateContainer extends StateContainer {
    int mCount;

    @Override
    public void applyStateUpdate(StateUpdate stateUpdate) {
      switch (stateUpdate.type) {
        case 0:
          mCount += 1;
          break;
      }
    }
  }

  private StateContainer.StateUpdate createStateUpdate() {
    return new StateContainer.StateUpdate(0);
  }
}
