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

package com.facebook.litho.stateupdates;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;
import static com.facebook.litho.stateupdates.ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer;
import static com.facebook.litho.stateupdates.ComponentWithStateAndChildWithState.ComponentWithStateAndChildWithStateStateContainer;
import static com.facebook.litho.stateupdates.ComponentWithStateAndChildWithStateNestedGrandParent.ComponentWithStateAndChildWithStateNestedGrandParentStateContainer;
import static com.facebook.litho.stateupdates.ComponentWithStateAndChildWithStateNestedParent.ComponentWithStateAndChildWithStateNestedParentStateContainer;
import static org.assertj.core.api.Assertions.assertThat;

import android.os.HandlerThread;
import android.os.Looper;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ErrorEventHandler;
import com.facebook.litho.LithoLayoutResult;
import com.facebook.litho.LithoNode;
import com.facebook.litho.NestedTreeHolderResult;
import com.facebook.litho.Size;
import com.facebook.litho.StateContainer;
import com.facebook.litho.StateHandler;
import com.facebook.litho.TreeState;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestLithoView;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.assertj.LithoViewAssert;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class StateUpdatesTest {

  private ComponentContext mContext;
  private int mWidthSpec;
  private int mHeightSpec;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mWidthSpec = makeSizeSpec(100, EXACTLY);
    mHeightSpec = makeSizeSpec(100, EXACTLY);
  }

  private TestLithoView createTestLithoView(Component component) {
    return mLithoViewRule
        .createTestLithoView()
        .setRoot(component)
        .setSizeSpecs(mWidthSpec, mHeightSpec)
        .attachToWindow()
        .measure()
        .layout();
  }

  private StateHandler getResolveState(final ComponentTree componentTree) {
    final TreeState treeState = Whitebox.getInternalState(componentTree, "mTreeState");
    return Whitebox.getInternalState(treeState, "mResolveState");
  }

  private StateHandler getLayoutState(final ComponentTree componentTree) {
    final TreeState treeState = Whitebox.getInternalState(componentTree, "mTreeState");
    return Whitebox.getInternalState(treeState, "mLayoutState");
  }

  private TreeState getTreeState(final ComponentTree componentTree) {
    return Whitebox.getInternalState(componentTree, "mTreeState");
  }

  private <T> T getLayoutStateHandleStateContainer(
      final ComponentTree componentTree, final String globalKey) {
    final TreeState treeState = getTreeState(componentTree);
    return (T) Whitebox.invokeMethod(treeState, "getStateContainer", globalKey, true);
  }

  private <T> T getStateHandleStateContainer(
      final ComponentTree componentTree, final String globalKey) {
    final TreeState treeState = getTreeState(componentTree);
    return (T) Whitebox.invokeMethod(treeState, "getStateContainer", globalKey, false);
  }

  private <T> T getInitialStateContainer(
      final ComponentTree componentTree, final String globalKey) {
    final StateHandler stateHandler = getResolveState(componentTree);
    return (T) stateHandler.getInitialStateContainer().initialStates.get(globalKey);
  }

  private <T> T getLayoutInitialStateContainer(
      final ComponentTree componentTree, final String globalKey) {
    final StateHandler stateHandler = getLayoutState(componentTree);
    return (T) stateHandler.getInitialStateContainer().initialStates.get(globalKey);
  }

  private Map<String, StateContainer> getInitialStates(final ComponentTree componentTree) {
    final StateHandler stateHandler = getResolveState(componentTree);
    return stateHandler.getInitialStateContainer().initialStates;
  }

  @Test
  public void testKeepInitialStateValuesDeepHierarchy() {
    final Component component =
        Column.create(mContext).child(ComponentWithCounterStateLayout.create(mContext)).build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext context =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);

    assertThatStateContainerIsInStateHandler(componentTree, context, 0);
    assertThat(getInitialStates(componentTree).isEmpty()).isTrue();
  }

  @Test
  public void testKeepInitialStateValuesNestedTree() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext context =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
            .getNestedResult()
            .getNode()
            .getChildAt(0)
            .getComponentContextAt(1);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, context, 0);
  }

  @Test
  public void testKeepInitialStateValuesNestedTreeWithNestedChild() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedGrandParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext context =
        ((NestedTreeHolderResult)
                ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
                    .getNestedResult()
                    .getChildAt(0))
            .getNestedResult()
            .getChildAt(0)
            .getNode()
            .getComponentContextAt(1);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, context, 0);
  }

  @Test
  public void testKeepUpdatedStateValueDeepHierarchy() {
    final Component component =
        Column.create(mContext).child(ComponentWithCounterStateLayout.create(mContext)).build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext componentContext =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountSync(componentContext);

    assertThatStateContainerIsInStateHandler(componentTree, componentContext, 1);
    assertThat(getInitialStates(componentTree).isEmpty()).isTrue();
  }

  @Test
  public void testKeepUpdatedStateValueLazyStateUpdateDeepHierarchy() {
    ComponentWithCounterLazyStateLayoutSpec.Caller stateUpdater =
        new ComponentWithCounterLazyStateLayoutSpec.Caller();

    final Component component =
        Column.create(mContext)
            .child(
                ComponentWithCounterLazyStateLayout.create(mContext)
                    .caller(stateUpdater)
                    .listener(
                        new TestEventListener() {
                          @Override
                          public void onTestEventCalled(int actualCount, int expectedValue) {
                            assertThat(actualCount).isEqualTo(expectedValue);
                          }
                        }))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);

    // Lazy State Update
    stateUpdater.lazyUpdate(2);

    // Layout should not be triggered by lazy state update and text counter should still be 0
    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 0");

    // Dispatch a test event to check whether we get the right state value in event or not
    stateUpdater.dispatchTestEvent(new TestEvent(2));

    // Normal State Update which triggers the layout
    stateUpdater.increment();

    // Layout should be triggered by state update and text counter should be 3 now (Lazy State
    // Update updated it to 2 then we did increment with normal state update)
    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 3");

    // Dispatch a test event to check whether we get the right state value in event or not
    stateUpdater.dispatchTestEvent(new TestEvent(3));
  }

  @Test
  public void testKeepUpdatedStateValueNestedTree() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext componentContext =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
            .getNestedResult()
            .getNode()
            .getChildAt(0)
            .getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountSync(componentContext);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1);
    assertThat(getInitialStates(componentTree).isEmpty()).isTrue();
  }

  @Test
  public void testKeepUpdatedStateValueNestedTreeWithNestedChild() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedGrandParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext componentContext =
        ((NestedTreeHolderResult)
                ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
                    .getNestedResult()
                    .getChildAt(0))
            .getNestedResult()
            .getChildAt(0)
            .getNode()
            .getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountSync(componentContext);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1);
    assertThat(getInitialStates(componentTree).isEmpty()).isTrue();
  }

  @Test
  public void testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsDeepHierarchy() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateLayout.create(mContext))
            .child(ComponentWithCounterStateLayout.create(mContext))
            .child(ComponentWithCounterStateLayout.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext componentContextChild1 =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);
    final ComponentContext componentContextChild2 =
        testLithoView.getCurrentRootNode().getNode().getChildAt(1).getComponentContextAt(1);
    final ComponentContext componentContextChild3 =
        testLithoView.getCurrentRootNode().getNode().getChildAt(2).getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild2, 1);
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild3, 0);
  }

  @Test
  public void testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsNestedTree() {
    final Component component =
        Column.create(mContext).child(NestedParentWithMultipleStateChild.create(mContext)).build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final LithoNode lithoNode =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
            .getNestedResult()
            .getNode();

    final ComponentContext componentContextChild1 =
        lithoNode.getChildAt(0).getComponentContextAt(1);
    final ComponentContext componentContextChild2 =
        lithoNode.getChildAt(1).getComponentContextAt(1);
    final ComponentContext componentContextChild3 =
        lithoNode.getChildAt(2).getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild3, 0);
  }

  @Test
  public void
      testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsNestedTreeWithNestedChild() {
    final Component component =
        Column.create(mContext)
            .child(NestedGrandParentWithMultipleStateChild.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final LithoLayoutResult layoutResult =
        ((NestedTreeHolderResult)
                ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
                    .getNestedResult()
                    .getChildAt(0))
            .getNestedResult();

    final ComponentContext componentContextChild1 =
        layoutResult.getChildAt(0).getNode().getComponentContextAt(1);

    final ComponentContext componentContextChild2 =
        layoutResult.getChildAt(1).getNode().getComponentContextAt(1);

    final ComponentContext componentContextChild3 =
        layoutResult.getChildAt(2).getNode().getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild3, 0);
  }

  @Test
  public void
      testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsDeepHierarchy() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateLayout.create(mContext))
            .child(ComponentWithStateAndChildWithState.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext parentSibling =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);
    final ComponentContext parent =
        testLithoView.getCurrentRootNode().getNode().getChildAt(1).getComponentContextAt(1);
    final ComponentContext componentContextChild1 =
        testLithoView
            .getCurrentRootNode()
            .getNode()
            .getChildAt(1)
            .getChildAt(0)
            .getComponentContextAt(1);

    final ComponentContext componentContextChild2 =
        testLithoView
            .getCurrentRootNode()
            .getNode()
            .getChildAt(1)
            .getChildAt(1)
            .getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(parent);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(parent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInStateHandler(componentTree, parentSibling, 0);
    assertThatParentStateContainerIsInStateHandler(componentTree, parent, 2);
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild2, 0);
  }

  @Test
  public void
      testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsNestedTree() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateLayout.create(mContext))
            .child(ComponentWithStateAndChildWithStateNestedParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext parentSibling =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);
    final ComponentContext parent =
        testLithoView.getCurrentRootNode().getNode().getChildAt(1).getComponentContextAt(0);

    final LithoNode lithoNode =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(1))
            .getNestedResult()
            .getNode();

    final ComponentContext componentContextChild1 =
        lithoNode.getChildAt(0).getComponentContextAt(1);
    final ComponentContext componentContextChild2 =
        lithoNode.getChildAt(1).getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(parent);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(parent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInStateHandler(componentTree, parentSibling, 0);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 0);
    assertThatNestedParentStateContainerIsInLayoutStateHandler(componentTree, parent, 2);
  }

  @Test
  public void
      testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsNestedTreeWithNestedChild() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateLayout.create(mContext))
            .child(ComponentWithStateAndChildWithStateNestedGrandParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final ComponentContext grandParentSibling =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);
    final ComponentContext grandParent =
        testLithoView.getCurrentRootNode().getNode().getChildAt(1).getComponentContextAt(0);

    final LithoNode lithoNode =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(1))
            .getNestedResult()
            .getNode();

    final ComponentContext parentSibling = lithoNode.getChildAt(0).getComponentContextAt(1);
    final ComponentContext parent = lithoNode.getChildAt(1).getComponentContextAt(0);

    final LithoLayoutResult lithoLayoutResult =
        ((NestedTreeHolderResult)
                ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(1))
                    .getNestedResult()
                    .getChildAt(1))
            .getNestedResult();

    final ComponentContext componentContextChild1 =
        lithoLayoutResult.getChildAt(0).getNode().getComponentContextAt(1);
    final ComponentContext componentContextChild2 =
        lithoLayoutResult.getChildAt(1).getNode().getComponentContextAt(1);

    ComponentWithCounterStateLayout.incrementCountAsync(grandParent);
    ComponentWithCounterStateLayout.incrementCountAsync(parent);
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1);
    ComponentWithCounterStateLayout.incrementCountAsync(parent);
    ComponentWithCounterStateLayout.incrementCountAsync(grandParent);

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInStateHandler(componentTree, grandParentSibling, 0);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, parentSibling, 0);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1);
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 0);
    assertThatNestedGrandParentStateContainerIsInLayoutStateHandler(componentTree, grandParent, 2);
    assertThatNestedParentStateContainerIsInLayoutStateHandler(componentTree, parent, 2);
  }

  @Test
  public void testNestedTreeStateContainersPreservedCorrectly() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedParent.create(mContext))
            .build();

    final Looper looper = getLooper();
    ComponentTree componentTree =
        ComponentTree.create(mContext, component).layoutThreadLooper(looper).build();

    TestLithoView testLithoView =
        mLithoViewRule
            .createTestLithoView(null, componentTree)
            .setSizeSpecs(mWidthSpec, mHeightSpec)
            .attachToWindow()
            .measure()
            .layout();

    final ComponentContext componentContext =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
            .getNestedResult()
            .getNode()
            .getChildAt(0)
            .getComponentContextAt(1);

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 0);

    ComponentWithCounterStateLayout.incrementCountAsync(componentContext);

    // Simulate Activity destroyed so that we have pending state update which should be restored
    // correctly when we acquire state handler and again process the pending state update.
    looper.quit();
    final TreeState treeState = componentTree.acquireTreeState();
    componentTree = ComponentTree.create(mContext, component).treeState(treeState).build();

    mLithoViewRule
        .createTestLithoView(null, componentTree)
        .setSizeSpecs(mWidthSpec, mHeightSpec)
        .attachToWindow()
        .measure()
        .layout();

    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1);
    assertThat(getInitialStates(componentTree).isEmpty()).isTrue();
  }

  @Test
  public void testCrashOnUpdateState() {
    AtomicInteger numOfTimesOnErrorWasCalled = new AtomicInteger(0);

    final ErrorEventHandler errorEventHandler =
        new ErrorEventHandler() {
          @Override
          public Component onError(ComponentContext cc, Exception e) {
            numOfTimesOnErrorWasCalled.getAndIncrement();
            if (numOfTimesOnErrorWasCalled.get() > 1) {
              throw new RuntimeException("onError should be called only once");
            }
            return Column.create(mContext).build();
          }
        };

    final Component component =
        Column.create(mContext)
            .child(ComponentWithStateCrashOnUpdateState.create(mContext))
            .build();

    ComponentTree componentTree =
        ComponentTree.create(mContext, component).errorHandler(errorEventHandler).build();

    final TestLithoView testLithoView =
        mLithoViewRule
            .createTestLithoView(null, componentTree)
            .setSizeSpecs(mWidthSpec, mHeightSpec)
            .attachToWindow()
            .measure()
            .layout();

    final ComponentContext componentContext =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getComponentContextAt(1);

    ComponentWithStateCrashOnUpdateState.incrementCountSync(componentContext);

    assertThat(numOfTimesOnErrorWasCalled.get()).isEqualTo(1);
  }

  @Test
  public void testStateUpdateComponentMeasureDoNotCacheResult() {
    testStateUpdateComponentMeasure(false);
  }

  @Test
  public void testStateUpdateComponentMeasureCacheResult() {
    testStateUpdateComponentMeasure(true);
  }

  /**
   * We are using a component hierarchy which calls {@link Component#measure(ComponentContext, int,
   * int, Size, boolean)} in onCreateLayout. The component being measured is simple component with
   * state.
   *
   * <p>We want to verify if StateUpdate happens on a component then it should get updated
   * correctly.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private void testStateUpdateComponentMeasure(boolean shouldCacheResult) {
    ComponentWithCounterStateLayoutSpec.Caller stateUpdater =
        new ComponentWithCounterStateLayoutSpec.Caller();

    Component component =
        Column.create(mContext)
            .child(
                ComponentWithMeasureCall.create(mContext)
                    .component(
                        ComponentWithCounterStateLayout.create(mContext).caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build();

    TestLithoView testLithoView = createTestLithoView(component);

    stateUpdater.increment();

    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 1");
  }

  @Test
  public void testStateUpdateComponentMeasureNestedTreeDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTree(false);
  }

  @Test
  public void testStateUpdateComponentMeasureNestedTreeCacheResult() {
    testStateUpdateComponentMeasureNestedTree(true);
  }

  /**
   * We are using a component hierarchy which calls {@link Component#measure(ComponentContext, int,
   * int, Size, boolean)} in onCreateLayout. The component being measured is NestedTree and has a
   * child component with state.
   *
   * <p>We want to verify if StateUpdate happens on a component (NestedTree -> component) then it
   * should get updated correctly.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private void testStateUpdateComponentMeasureNestedTree(boolean shouldCacheResult) {
    ComponentWithCounterStateLayoutSpec.Caller stateUpdater =
        new ComponentWithCounterStateLayoutSpec.Caller();

    Component component =
        Column.create(mContext)
            .child(
                ComponentWithMeasureCall.create(mContext)
                    .component(
                        ComponentWithCounterStateNestedParent.create(mContext).caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build();

    final TestLithoView testLithoView = createTestLithoView(component);

    stateUpdater.increment();

    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 1");
  }

  @Test
  public void testStateUpdateComponentMeasureNestedTreeWithNestedChildDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChild(false);
  }

  @Test
  public void testStateUpdateComponentMeasureNestedTreeWithNestedChildCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChild(true);
  }

  /**
   * We are using a component hierarchy which calls {@link Component#measure(ComponentContext, int,
   * int, Size, boolean)} in onCreateLayout. The component being measured is NestedTree ->
   * NestedTree -> component with state
   *
   * <p>We want to verify if StateUpdate happens on a component (NestedTree -> NestedTree ->
   * component) then it should get updated corectly
   *
   * <p>In this test, component is passed as a child of Column.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private void testStateUpdateComponentMeasureNestedTreeWithNestedChild(boolean shouldCacheResult) {
    ComponentWithCounterStateLayoutSpec.Caller stateUpdater =
        new ComponentWithCounterStateLayoutSpec.Caller();

    Component component =
        Column.create(mContext)
            .child(
                ComponentWithMeasureCall.create(mContext)
                    .component(
                        Column.create(mContext)
                            .child(
                                ComponentWithCounterStateNestedGrandParent.create(mContext)
                                    .caller(stateUpdater)))
                    .shouldCacheResult(shouldCacheResult))
            .build();

    final TestLithoView testLithoView = createTestLithoView(component);

    stateUpdater.increment();

    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 1");
  }

  @Test
  public void
      testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumnDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(false);
  }

  @Test
  public void testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumnCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(true);
  }

  /**
   * We are using a component hierarchy which calls {@link Component#measure(ComponentContext, int,
   * int, Size, boolean)} in onCreateLayout. The component being measured is NestedTree ->
   * NestedTree -> component with state.
   *
   * <p>We want to verify if StateUpdate happens on a component (NestedTree -> NestedTree ->
   * component) then it should get updated correctly.
   *
   * <p>In this test, component is passed as it is to be measured i.e not a child of Column or any
   * other component
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private void testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(
      boolean shouldCacheResult) {
    ComponentWithCounterStateLayoutSpec.Caller stateUpdater =
        new ComponentWithCounterStateLayoutSpec.Caller();

    Component component =
        Column.create(mContext)
            .child(
                ComponentWithMeasureCall.create(mContext)
                    .component(
                        ComponentWithCounterStateNestedGrandParent.create(mContext)
                            .caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build();

    final TestLithoView testLithoView = createTestLithoView(component);

    stateUpdater.increment();

    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 1");
  }

  @Test
  public void testStateUpdateComponentAfterComponentIsRemovedFromHierarchy() {
    ComponentWithConditionalChildLayoutSpec.Caller stateUpdater =
        new ComponentWithConditionalChildLayoutSpec.Caller();

    ComponentWithCounterStateLayoutSpec.Caller childStateUpdater =
        new ComponentWithCounterStateLayoutSpec.Caller();

    Component component =
        Column.create(mContext)
            .child(
                ComponentWithConditionalChildLayout.create(mContext)
                    .caller(stateUpdater)
                    .childCaller(childStateUpdater))
            .build();

    TestLithoView testLithoView = createTestLithoView(component);

    childStateUpdater.increment();
    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Count: 1");

    stateUpdater.hideChildComponent();

    childStateUpdater.increment();

    LithoViewAssert.assertThat(testLithoView.getLithoView()).hasVisibleText("Empty");
    LithoViewAssert.assertThat(testLithoView.getLithoView())
        .doesNotHaveVisibleTextContaining("Count:");
  }

  private void assertThatNestedGrandParentStateContainerIsInLayoutStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithStateAndChildWithStateNestedGrandParentStateContainer
        stateHandlerStateContainerChild1 =
            getLayoutStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithStateAndChildWithStateNestedGrandParentStateContainer
        initialStateStateContainerChild1 =
            getLayoutInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatNestedGrandParentStateContainerIsInStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithStateAndChildWithStateNestedGrandParentStateContainer
        stateHandlerStateContainerChild1 =
            getStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithStateAndChildWithStateNestedGrandParentStateContainer
        initialStateStateContainerChild1 =
            getInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatNestedParentStateContainerIsInLayoutStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithStateAndChildWithStateNestedParentStateContainer stateHandlerStateContainerChild1 =
        getLayoutStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithStateAndChildWithStateNestedParentStateContainer initialStateStateContainerChild1 =
        getLayoutInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatNestedParentStateContainerIsInStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithStateAndChildWithStateNestedParentStateContainer stateHandlerStateContainerChild1 =
        getStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithStateAndChildWithStateNestedParentStateContainer initialStateStateContainerChild1 =
        getInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatParentStateContainerIsInStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithStateAndChildWithStateStateContainer stateHandlerStateContainerChild1 =
        getStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithStateAndChildWithStateStateContainer initialStateStateContainerChild1 =
        getInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatStateContainerIsInLayoutStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithCounterStateLayoutStateContainer stateHandlerStateContainerChild1 =
        getLayoutStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithCounterStateLayoutStateContainer initialStateStateContainerChild1 =
        getLayoutInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private void assertThatStateContainerIsInStateHandler(
      ComponentTree componentTree, ComponentContext context, int expectedStateValue) {
    ComponentWithCounterStateLayoutStateContainer stateHandlerStateContainerChild1 =
        getStateHandleStateContainer(componentTree, context.getGlobalKey());
    ComponentWithCounterStateLayoutStateContainer initialStateStateContainerChild1 =
        getInitialStateContainer(componentTree, context.getGlobalKey());

    assertThat(stateHandlerStateContainerChild1).isNotNull();
    assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue);
    assertThat(initialStateStateContainerChild1).isNull();
  }

  private Looper getLooper() {
    final HandlerThread defaultThread =
        new HandlerThread("testThread", DEFAULT_BACKGROUND_THREAD_PRIORITY);
    defaultThread.start();
    return defaultThread.getLooper();
  }
}
