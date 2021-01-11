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
import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Looper;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ImmediateLazyStateUpdateDispatchingComponent;
import com.facebook.litho.widget.LayoutSpecLifecycleTester;
import com.facebook.litho.widget.SameManualKeyRootComponentSpec;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.litho.widget.TestWrapperComponent;
import com.facebook.litho.widget.TextDrawable;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.match.MatchNode;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class StateUpdatesWithReconciliationTest {

  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  public @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

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
    verify(layout, times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_new_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(new DummyComponent());

    verify(layout, times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_same_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent);

    verify(layout, times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_same_instance_internal() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(current.getRootComponent());

    verify(layout, times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_shallowCopy() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent.makeShallowCopy());

    verify(layout, times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(getRootGlobalKey(current), createStateUpdate(), "test", false);

    verify(layout, times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void update_state_async() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateAsync(getRootGlobalKey(current), createStateUpdate(), "test", false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(layout, times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void simple_update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(getRootGlobalKey(current), createStateUpdate(), "test", false);

    verify(layout, times(1)).reconcile(any(), any(), any(), any());
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
    verify(layout, times(1)).reconcile(any(), any(), any(), any());
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
    verify(layout, times(0)).reconcile(any(), any(), any(), any());
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
                            .prefix("First: "))
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdater2)
                            .widthPx(100)
                            .heightPx(100)
                            .prefix("Second: ")))
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

    ViewAssertions.assertThat(lithoView)
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .prop(
                    "drawables",
                    MatchNode.list(
                        MatchNode.forType(TextDrawable.class).prop("text", "First: 2"),
                        MatchNode.forType(TextDrawable.class).prop("text", "Second: 2"))));
  }

  @Test
  public void whenSetRootWithDifferentPropsWithPendingUpdate_shouldNotReconcile() {
    final ComponentContext c = mLithoViewRule.getContext();
    final ImmediateLazyStateUpdateDispatchingComponent child =
        ImmediateLazyStateUpdateDispatchingComponent.create(c).build();

    final Component initial = Column.create(c).child(child).build();

    mLithoViewRule.attachToWindow().setRoot(initial).measure().layout();

    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component updated =
        Column.create(c)
            .child(child)
            .child(LayoutSpecLifecycleTester.create(c).steps(info))
            .build();

    mLithoViewRule.setRoot(updated);

    // If the new component resolves then the root component (column) was not reconciled.
    assertThat(getSteps(info)).contains(LifecycleStep.ON_CREATE_LAYOUT);
  }

  @Test
  public void whenSetRootWithDelegatingComponentWithPendingUpdate_shouldReconcile() {
    final ComponentContext c = mLithoViewRule.getContext();
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component initial =
        TestWrapperComponent.create(c)
            .delegate(
                Column.create(c)
                    .child(ImmediateLazyStateUpdateDispatchingComponent.create(c))
                    .child(LayoutSpecLifecycleTester.create(c).steps(info)))
            .build();

    mLithoViewRule.attachToWindow().setRoot(initial).measure().layout();

    final Component updated =
        TestWrapperComponent.create(c)
            .delegate(
                Column.create(c)
                    .child(ImmediateLazyStateUpdateDispatchingComponent.create(c))
                    .child(LayoutSpecLifecycleTester.create(c).steps(info)))
            .build();

    info.clear();
    mLithoViewRule.setRoot(updated);

    // If the new component's on create layout is not called then it was reconciled.
    assertThat(getSteps(info)).doesNotContain(LifecycleStep.ON_CREATE_LAYOUT);
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
    protected StateContainer getStateContainer(ComponentContext scopedContext) {
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

  private static String getRootGlobalKey(LayoutState layoutState) {
    return layoutState.getRootComponent().getKey();
  }
}
