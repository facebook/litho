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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.SameManualKeyRootComponentSpec;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class StateUpdatesWithReconciliationTest {

  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  private static final String mLogTag = "logTag";

  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private Component mRootComponent;
  private ComponentTree mComponentTree;
  private ComponentsLogger mComponentsLogger;
  private LithoView mLithoView;

  private static final int STATE_VALUE_INITIAL_COUNT = 4;

  @Before
  public void before() {
    before(null);
  }

  public void before(Component component) {
    ComponentsConfiguration.isEndToEndTestRun = true;

    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            return spy(new DefaultInternalNode(c));
          }
        };
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), mLogTag, mComponentsLogger);
    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));
    mRootComponent = component != null ? component : new DummyComponent();
    mLithoView = new LithoView(mContext);
    mComponentTree =
        spy(ComponentTree.create(mContext, mRootComponent).isReconciliationEnabled(true).build());

    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    ComponentTestHelper.measureAndLayout(mLithoView);
  }

  @After
  public void after() {
    ComponentsConfiguration.isEndToEndTestRun = false;
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
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test", false);

    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void update_state_async() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateAsync(
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test", false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void simple_update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(
        current.getRootComponent().getGlobalKey(), createStateUpdate(), "test", false);

    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void testStateUpdateWithComponentsWhichHaveSameManualKeyWithNewLayoutCreation() {
    after();

    // Set custom root component
    before(SameManualKeyRootComponentSpec.create(mContext));

    mComponentTree.updateStateAsync(
        SameManualKeyRootComponentSpec.getGlobalKeyForStateUpdate(),
        createStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testStateUpdateWithComponentsWhichHaveSameManualKey() {
    after();

    // Set custom root component
    before(SameManualKeyRootComponentSpec.create(mContext));

    mComponentTree.updateStateAsync(
        SameManualKeyRootComponentSpec.getGlobalKeyForStateUpdate(),
        createStateUpdate(),
        "test",
        false);
    mLayoutThreadShadowLooper.runToEndOfTasks();
  }

  @Test
  public void testIsReconcilableForRootComponentWithUpdatedState() {
    after();

    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    before(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    // trigger a state update
    caller.increment();

    mComponentTree.setRoot(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    // reconciliation should be invoked
    verify(layout, times(1)).reconcile(any(), any());
  }

  @Test
  public void testIsReconcilableForRootComponentWithDifferentPropValues() {
    after();

    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    before(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    // trigger a state update
    caller.increment();

    // Passing a new caller
    mComponentTree.setRoot(
        SimpleStateUpdateEmulator.create(mContext)
            .caller(new SimpleStateUpdateEmulatorSpec.Caller())
            .build());

    // reconciliation should not be invoked
    verify(layout, times(0)).reconcile(any(), any());
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that the
   * final result includes both state updates.
   */
  @Test
  public void testMultipleBackgroundStateUpdates() {
    after();

    ComponentContext c = new ComponentContext(getApplicationContext());
    LithoView lithoView = new LithoView(c);
    ComponentTree componentTree = ComponentTree.create(c).build();
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    lithoView.layout(0, 0, 100, 100);
    lithoView.onAttachedToWindow();

    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater1 =
        new SimpleStateUpdateEmulatorSpec.Caller();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater2 =
        new SimpleStateUpdateEmulatorSpec.Caller();

    componentTree.setRootAsync(
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater1)
                            .widthPx(100)
                            .heightPx(100)
                            .tagPrefix("First:")
                            .background(new ColorDrawable(Color.RED)))
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater2)
                            .widthPx(100)
                            .heightPx(100)
                            .tagPrefix("Second:")
                            .background(new ColorDrawable(Color.RED))))
            .build());

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    ShadowLooper.idleMainLooper();
    lithoView.layout(0, 0, 100, 100);

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater1.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    stateUpdater2.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    lithoView.layout(0, 0, 100, 100);

    // We're using the fact that SimpleStateUpdateEmulator will write the state to a view tag
    // TODO(t64502673): This is a really ugly way to do this, fix after we upgrade to robolectric 4
    // by just checking against mounted text
    assertThat(findViewWithTag(lithoView, "First:" + 2))
        .describedAs("Expected first state update to be applied in final result.")
        .isNotNull();
    assertThat(findViewWithTag(lithoView, "Second:" + 2))
        .describedAs("Expected second state update to be applied in final result.")
        .isNotNull();
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

  private static View findViewWithTag(View root, String tag) {
    if (tag.equals(root.getTag())) {
      return root;
    }

    if (root instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) root;
      for (int i = 0; i < vg.getChildCount(); i++) {
        View v = findViewWithTag(vg.getChildAt(i), tag);
        if (v != null) {
          return v;
        }
      }
    }

    return null;
  }
}
