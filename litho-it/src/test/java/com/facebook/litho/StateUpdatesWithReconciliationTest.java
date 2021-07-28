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
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner.class)
public class StateUpdatesWithReconciliationTest {

  private final boolean mReuseInternalNodes;
  private final boolean mOriginalValueOfReuseInternalNodes;
  private final boolean mOriginalValueOfUseStatelessComponent;
  private final boolean originalE2ETestRun;

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

  @ParameterizedRobolectricTestRunner.Parameters(name = "reuseInternalNodes={0}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false}, {true},
        });
  }

  public StateUpdatesWithReconciliationTest(boolean reuseInternalNodes) {
    originalE2ETestRun = ComponentsConfiguration.isEndToEndTestRun;
    mReuseInternalNodes = reuseInternalNodes;
    mOriginalValueOfReuseInternalNodes = ComponentsConfiguration.reuseInternalNodes;
    mOriginalValueOfUseStatelessComponent = ComponentsConfiguration.useStatelessComponent;
  }

  @Before
  public void setup() {
    ComponentsConfiguration.reuseInternalNodes = mReuseInternalNodes;
    ComponentsConfiguration.useStatelessComponent = mReuseInternalNodes;
  }

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
            return spy(new InputOnlyInternalNode(c));
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            return spy(new InputOnlyNestedTreeHolder(c, props));
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
    ComponentsConfiguration.isEndToEndTestRun = originalE2ETestRun;
    ComponentsConfiguration.reuseInternalNodes = mOriginalValueOfReuseInternalNodes;
    ComponentsConfiguration.useStatelessComponent = mOriginalValueOfUseStatelessComponent;
    NodeConfig.sInternalNodeFactory = null;
  }

  @Test
  public void initial_condition() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    assertThat(layout).isNotNull();
    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_new_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.setRoot(new DummyComponent());

    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_same_instance() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent);

    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_same_instance_internal() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.setRoot(current.getRootComponent());

    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void set_root_shallowCopy() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.setRoot(mRootComponent.makeShallowCopy());

    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(getRootGlobalKey(current), createStateUpdate(), "test", false);

    verify(layout.getInternalNode(), times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void update_state_async() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.updateStateAsync(getRootGlobalKey(current), createStateUpdate(), "test", false);
    mLayoutThreadShadowLooper.runToEndOfTasks();

    verify(layout.getInternalNode(), times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void simple_update_state_sync() {
    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    mComponentTree.updateStateSync(getRootGlobalKey(current), createStateUpdate(), "test", false);

    verify(layout.getInternalNode(), times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void testStateUpdateWithComponentsWhichHaveSameManualKeyWithNewLayoutCreation() {
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
    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    before(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    // trigger a state update
    caller.increment();

    mComponentTree.setRoot(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    // reconciliation should be invoked
    verify(layout.getInternalNode(), times(1)).reconcile(any(), any(), any(), any());
  }

  @Test
  public void testIsReconcilableForRootComponentWithDifferentPropValues() {
    SimpleStateUpdateEmulatorSpec.Caller caller = new SimpleStateUpdateEmulatorSpec.Caller();
    before(SimpleStateUpdateEmulator.create(mContext).caller(caller).build());

    LayoutState current = mComponentTree.getMainThreadLayoutState();
    LithoLayoutResult layout = current.getLayoutRoot();

    // trigger a state update
    caller.increment();

    // Passing a new caller
    mComponentTree.setRoot(
        SimpleStateUpdateEmulator.create(mContext)
            .caller(new SimpleStateUpdateEmulatorSpec.Caller())
            .build());

    // reconciliation should not be invoked
    verify(layout.getInternalNode(), times(0)).reconcile(any(), any(), any(), any());
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that the
   * final result includes both state updates.
   */
  @Test
  public void testMultipleBackgroundStateUpdates() {
    final boolean defaultReuseInternalNode = ComponentsConfiguration.reuseInternalNodes;
    ComponentsConfiguration.reuseInternalNodes = false;

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

    ComponentsConfiguration.reuseInternalNodes = defaultReuseInternalNode;
  }

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that the
   * final result includes both state updates.
   */
  @Test
  public void testMultipleBackgroundStateUpdates_with_reuse() {

    final boolean defaultReuseInternalNode = ComponentsConfiguration.reuseInternalNodes;
    final boolean defaultStatelessness = ComponentsConfiguration.useStatelessComponent;

    ComponentsConfiguration.reuseInternalNodes = true;
    ComponentsConfiguration.useStatelessComponent = true;

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

    ComponentsConfiguration.reuseInternalNodes = defaultReuseInternalNode;
    ComponentsConfiguration.useStatelessComponent = defaultStatelessness;
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

  @Test
  public void onStateUpdateWithStatelessComponents_shouldEnsureNoStateContainersAreLost() {
    final ComponentContext c = mLithoView.getComponentContext();

    final SimpleStateUpdateEmulatorSpec.Caller caller_1 =
        new SimpleStateUpdateEmulatorSpec.Caller();
    final SimpleStateUpdateEmulatorSpec.Caller caller_2 =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller_1))
            .child(SimpleStateUpdateEmulator.create(c).caller(caller_2))
            .build();

    mLithoViewRule.attachToWindow().setRoot(root).measure().layout();

    // trigger a state update
    caller_1.increment();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .prop(
                    "drawables",
                    MatchNode.list(
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 2"),
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 1"))));

    // trigger a state update
    caller_2.increment();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .prop(
                    "drawables",
                    MatchNode.list(
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 2"),
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 2"))));

    // trigger a state update
    caller_1.increment();

    ViewAssertions.assertThat(mLithoViewRule.getLithoView())
        .matches(
            ViewMatchNode.forType(LithoView.class)
                .prop(
                    "drawables",
                    MatchNode.list(
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 3"),
                        MatchNode.forType(TextDrawable.class).prop("text", "Text: 2"))));
  }

  static class DummyComponent extends Component {

    public DummyComponent() {
      setStateContainer(new DummyStateContainer());
    }

    @Override
    protected boolean hasState() {
      return true;
    }

    @Override
    protected void createInitialState(ComponentContext c) {
      getStateContainerImpl(c).mCount = STATE_VALUE_INITIAL_COUNT;
    }

    @Nullable
    @Override
    protected StateContainer createStateContainer() {
      return new DummyStateContainer();
    }

    @Nullable
    protected DummyStateContainer getStateContainerImpl(ComponentContext c) {
      return (DummyStateContainer) Component.getStateContainer(c, this);
    }

    @Override
    protected void transferState(
        StateContainer prevStateContainer, StateContainer nextStateContainer) {
      DummyStateContainer prevStateContainerImpl = (DummyStateContainer) prevStateContainer;
      DummyStateContainer nextStateContainerImpl = (DummyStateContainer) nextStateContainer;
      nextStateContainerImpl.mCount = prevStateContainerImpl.mCount;
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
