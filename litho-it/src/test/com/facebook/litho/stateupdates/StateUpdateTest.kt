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

package com.facebook.litho.stateupdates

import android.content.Context
import android.os.HandlerThread
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Column
import com.facebook.litho.Column.Companion.create
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.ErrorEventHandler
import com.facebook.litho.NestedTreeHolderResult
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.StateContainer
import com.facebook.litho.StateHandler
import com.facebook.litho.TreeState
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.state.ComponentState
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.LithoTestRuleResizeMode
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.assertj.LithoViewAssert.Companion.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class StateUpdateTest {
  private lateinit var context: ComponentContext
  private var widthSpec = 0
  private var heightSpec = 0

  @Rule @JvmField val mLithoTestRule = LithoTestRule(resizeMode = LithoTestRuleResizeMode.MANUAL)

  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    widthSpec = makeSizeSpec(100, EXACTLY)
    heightSpec = makeSizeSpec(100, EXACTLY)
  }

  private fun createTestLithoView(component: Component): TestLithoView {
    return mLithoTestRule
        .createTestLithoView()
        .setRoot(component)
        .setSizeSpecs(widthSpec, heightSpec)
        .attachToWindow()
        .measure()
        .layout()
  }

  private fun getResolveState(componentTree: ComponentTree): StateHandler {
    val treeState = Whitebox.getInternalState<TreeState>(componentTree, "mTreeState")
    return Whitebox.getInternalState(treeState, "resolveState")
  }

  private fun getLayoutState(componentTree: ComponentTree): StateHandler {
    val treeState = Whitebox.getInternalState<TreeState>(componentTree, "mTreeState")
    return Whitebox.getInternalState(treeState, "layoutState")
  }

  private fun getTreeState(componentTree: ComponentTree): TreeState {
    return Whitebox.getInternalState(componentTree, "mTreeState")
  }

  private fun <T : StateContainer> getLayoutStateHandleStateContainer(
      componentTree: ComponentTree,
      globalKey: String
  ): T {
    val treeState = getTreeState(componentTree)
    val state =
        Whitebox.invokeMethod<Any>(
            treeState,
            "getState",
            globalKey,
            true,
        ) as ComponentState<T>
    return state.value
  }

  private fun <T : StateContainer> getStateHandleStateContainer(
      componentTree: ComponentTree,
      globalKey: String,
  ): T {
    val treeState = getTreeState(componentTree)
    val state =
        Whitebox.invokeMethod<Any>(
            treeState,
            "getState",
            globalKey,
            false,
        ) as ComponentState<T>
    return state.value
  }

  private fun <T> getInitialStateContainer(componentTree: ComponentTree, globalKey: String): T? {
    val stateHandler = getResolveState(componentTree)
    return stateHandler.initialState.states[globalKey] as T?
  }

  private fun <T> getLayoutInitialStateContainer(
      componentTree: ComponentTree,
      globalKey: String
  ): T? {
    val stateHandler = getLayoutState(componentTree)
    return stateHandler.initialState.states[globalKey] as T?
  }

  private fun getInitialStates(componentTree: ComponentTree): Map<String, StateContainer> {
    val stateHandler = getResolveState(componentTree)
    return stateHandler.initialState.states.mapValues { it.value.value }
  }

  @Test
  fun testKeepInitialStateValuesDeepHierarchy() {
    val component: Component =
        create(context).child(ComponentWithCounterStateLayout.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val context = testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    assertThatStateContainerIsInStateHandler(componentTree, context, 0)
    Assertions.assertThat(getInitialStates(componentTree).isEmpty()).isTrue
  }

  @Test
  fun testKeepInitialStateValuesNestedTree() {
    val component: Component =
        create(context).child(ComponentWithCounterStateNestedParent.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val context =
        (testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .node!!
            .getChildAt(0)
            .getComponentContextAt(1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, context, 0)
  }

  @Test
  fun testKeepInitialStateValuesNestedTreeWithNestedChild() {
    val component: Component =
        create(context).child(ComponentWithCounterStateNestedGrandParent.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val context =
        ((testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
                .nestedResult!!
                .getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .getChildAt(0)
            .node
            .getComponentContextAt(1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, context, 0)
  }

  @Test
  fun testKeepUpdatedStateValueDeepHierarchy() {
    val component: Component =
        create(context).child(ComponentWithCounterStateLayout.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val componentContext =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountSync(componentContext)
    assertThatStateContainerIsInStateHandler(componentTree, componentContext, 1)
    Assertions.assertThat(getInitialStates(componentTree).isEmpty()).isTrue
  }

  @Test
  fun testKeepUpdatedStateValueLazyStateUpdateDeepHierarchy() {
    val stateUpdater = ComponentWithCounterLazyStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithCounterLazyStateLayout.create(context)
                    .caller(stateUpdater)
                    .listener(
                        object : TestEventListener() {
                          override fun onTestEventCalled(actualCount: Int, expectedValue: Int) {
                            Assertions.assertThat(actualCount).isEqualTo(expectedValue)
                          }
                        }))
            .build()
    val testLithoView = createTestLithoView(component)

    // Lazy State Update
    stateUpdater.lazyUpdate(2)

    // Layout should not be triggered by lazy state update and text counter should still be 0
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 0")

    // Dispatch a test event to check whether we get the right state value in event or not
    stateUpdater.dispatchTestEvent(TestEvent(2))

    // Normal State Update which triggers the layout
    stateUpdater.increment()

    // Layout should be triggered by state update and text counter should be 3 now (Lazy State
    // Update updated it to 2 then we did increment with normal state update)
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 3")

    // Dispatch a test event to check whether we get the right state value in event or not
    stateUpdater.dispatchTestEvent(TestEvent(3))
  }

  @Test
  fun testKeepUpdatedStateValueNestedTree() {
    val component: Component =
        create(context).child(ComponentWithCounterStateNestedParent.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val componentContext =
        (testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .node
            .getChildAt(0)
            .getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountSync(componentContext)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1)
    Assertions.assertThat(getInitialStates(componentTree).isEmpty()).isTrue
  }

  @Test
  fun testKeepUpdatedStateValueNestedTreeWithNestedChild() {
    val component: Component =
        create(context).child(ComponentWithCounterStateNestedGrandParent.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val componentContext =
        ((testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
                .nestedResult!!
                .getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .getChildAt(0)
            .node
            .getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountSync(componentContext)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1)
    Assertions.assertThat(getInitialStates(componentTree).isEmpty()).isTrue
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsDeepHierarchy() {
    val component: Component =
        create(context)
            .child(ComponentWithCounterStateLayout.create(context))
            .child(ComponentWithCounterStateLayout.create(context))
            .child(ComponentWithCounterStateLayout.create(context))
            .build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val componentContextChild1 =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    val componentContextChild2 =
        testLithoView.currentRootNode!!.node.getChildAt(1).getComponentContextAt(1)
    val componentContextChild3 =
        testLithoView.currentRootNode!!.node.getChildAt(2).getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild2, 1)
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild3, 0)
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsNestedTree() {
    val component: Component =
        create(context).child(NestedParentWithMultipleStateChild.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val lithoNode =
        (testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .node
    val componentContextChild1 = lithoNode.getChildAt(0).getComponentContextAt(1)
    val componentContextChild2 = lithoNode.getChildAt(1).getComponentContextAt(1)
    val componentContextChild3 = lithoNode.getChildAt(2).getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild3, 0)
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesInSiblingsNestedTreeWithNestedChild() {
    val component: Component =
        create(context).child(NestedGrandParentWithMultipleStateChild.create(context)).build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val layoutResult =
        ((testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
                .nestedResult!!
                .getChildAt(0) as NestedTreeHolderResult)
            .nestedResult
    val componentContextChild1 = layoutResult!!.getChildAt(0).node.getComponentContextAt(1)
    val componentContextChild2 = layoutResult.getChildAt(1).node.getComponentContextAt(1)
    val componentContextChild3 = layoutResult.getChildAt(2).node.getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild2)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild3, 0)
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsDeepHierarchy() {
    val component: Component =
        create(context)
            .child(ComponentWithCounterStateLayout.create(context))
            .child(ComponentWithStateAndChildWithState.create(context))
            .build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val parentSibling = testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    val parent = testLithoView.currentRootNode!!.node.getChildAt(1).getComponentContextAt(1)
    val componentContextChild1 =
        testLithoView.currentRootNode!!.node.getChildAt(1).getChildAt(0).getComponentContextAt(1)
    val componentContextChild2 =
        testLithoView.currentRootNode!!.node.getChildAt(1).getChildAt(1).getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInStateHandler(componentTree, parentSibling, 0)
    assertThatParentStateContainerIsInStateHandler(componentTree, parent, 2)
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInStateHandler(componentTree, componentContextChild2, 0)
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsNestedTree() {
    val component: Component =
        create(context)
            .child(ComponentWithCounterStateLayout.create(context))
            .child(ComponentWithStateAndChildWithStateNestedParent.create(context))
            .build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val parentSibling = testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    val parent = testLithoView.currentRootNode!!.node.getChildAt(1).getComponentContextAt(0)
    val lithoNode =
        (testLithoView.currentRootNode!!.getChildAt(1) as NestedTreeHolderResult)
            .nestedResult!!
            .node
    val componentContextChild1 = lithoNode.getChildAt(0).getComponentContextAt(1)
    val componentContextChild2 = lithoNode.getChildAt(1).getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInStateHandler(componentTree, parentSibling, 0)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 0)
    assertThatNestedParentStateContainerIsInLayoutStateHandler(componentTree, parent, 2)
  }

  @Test
  fun testKeepUpdatedStateValueAfterMultipleStateUpdatesOnParentChildComponentsNestedTreeWithNestedChild() {
    val component: Component =
        create(context)
            .child(ComponentWithCounterStateLayout.create(context))
            .child(ComponentWithStateAndChildWithStateNestedGrandParent.create(context))
            .build()
    val testLithoView = createTestLithoView(component)
    val componentTree = testLithoView.componentTree
    val grandParentSibling =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    val grandParent = testLithoView.currentRootNode!!.node.getChildAt(1).getComponentContextAt(0)
    val lithoNode =
        (testLithoView.currentRootNode!!.getChildAt(1) as NestedTreeHolderResult)
            .nestedResult!!
            .node
    val parentSibling = lithoNode.getChildAt(0).getComponentContextAt(1)
    val parent = lithoNode.getChildAt(1).getComponentContextAt(0)
    val lithoLayoutResult =
        ((testLithoView.currentRootNode!!.getChildAt(1) as NestedTreeHolderResult)
                .nestedResult!!
                .getChildAt(1) as NestedTreeHolderResult)
            .nestedResult
    val componentContextChild1 = lithoLayoutResult!!.getChildAt(0).node.getComponentContextAt(1)
    val componentContextChild2 = lithoLayoutResult.getChildAt(1).node.getComponentContextAt(1)
    ComponentWithCounterStateLayout.incrementCountAsync(grandParent)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContextChild1)
    ComponentWithCounterStateLayout.incrementCountAsync(parent)
    ComponentWithCounterStateLayout.incrementCountAsync(grandParent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInStateHandler(componentTree, grandParentSibling, 0)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, parentSibling, 0)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild1, 1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContextChild2, 0)
    assertThatNestedGrandParentStateContainerIsInLayoutStateHandler(componentTree, grandParent, 2)
    assertThatNestedParentStateContainerIsInLayoutStateHandler(componentTree, parent, 2)
  }

  @Test
  fun testNestedTreeStateContainersPreservedCorrectly() {
    val component: Component =
        create(context).child(ComponentWithCounterStateNestedParent.create(context)).build()
    val looper = looper
    var componentTree = ComponentTree.create(context, component).layoutThreadLooper(looper).build()
    val testLithoView =
        mLithoTestRule
            .createTestLithoView(null, componentTree)
            .setSizeSpecs(widthSpec, heightSpec)
            .attachToWindow()
            .measure()
            .layout()
    val componentContext =
        (testLithoView.currentRootNode!!.getChildAt(0) as NestedTreeHolderResult)
            .nestedResult!!
            .node
            .getChildAt(0)
            .getComponentContextAt(1)
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 0)
    ComponentWithCounterStateLayout.incrementCountAsync(componentContext)

    // Simulate Activity destroyed so that we have pending state update which should be restored
    // correctly when we acquire state handler and again process the pending state update.
    looper.quit()
    val treeState = componentTree.acquireTreeState()
    componentTree = ComponentTree.create(context, component).treeState(treeState).build()
    mLithoTestRule
        .createTestLithoView(null, componentTree)
        .setSizeSpecs(widthSpec, heightSpec)
        .attachToWindow()
        .measure()
        .layout()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThatStateContainerIsInLayoutStateHandler(componentTree, componentContext, 1)
    Assertions.assertThat(getInitialStates(componentTree).isEmpty()).isTrue
  }

  @Test
  fun testCrashOnUpdateState() {
    val numOfTimesOnErrorWasCalled = AtomicInteger(0)
    val errorEventHandler: ErrorEventHandler =
        object : ErrorEventHandler() {
          override fun onError(cc: ComponentContext, e: Exception): Component? {
            numOfTimesOnErrorWasCalled.getAndIncrement()
            if (numOfTimesOnErrorWasCalled.get() > 1) {
              throw RuntimeException("onError should be called only once")
            }
            return create(context).build()
          }
        }

    val componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                context.lithoConfiguration.componentsConfig.copy(
                    errorEventHandler = errorEventHandler))
            .build()

    mLithoTestRule.render(componentTree = componentTree) {
      Column { child(ComponentWithStateCrashOnUpdateState.create(context).build()) }
    }

    val testLithoView =
        mLithoTestRule
            .createTestLithoView(null, componentTree)
            .setSizeSpecs(widthSpec, heightSpec)
            .attachToWindow()
            .measure()
            .layout()
    val componentContext =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(1)
    ComponentWithStateCrashOnUpdateState.incrementCountSync(componentContext)
    Assertions.assertThat(numOfTimesOnErrorWasCalled.get()).isEqualTo(1)
  }

  @Test
  fun testStateUpdateComponentMeasureDoNotCacheResult() {
    testStateUpdateComponentMeasure(false)
  }

  @Test
  fun testStateUpdateComponentMeasureCacheResult() {
    testStateUpdateComponentMeasure(true)
  }

  /**
   * We are using a component hierarchy which calls [Component.measure] in onCreateLayout. The
   * component being measured is simple component with state.
   *
   * We want to verify if StateUpdate happens on a component then it should get updated correctly.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private fun testStateUpdateComponentMeasure(shouldCacheResult: Boolean) {
    val stateUpdater = ComponentWithCounterStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithMeasureCall.create(context)
                    .component(ComponentWithCounterStateLayout.create(context).caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build()
    val testLithoView = createTestLithoView(component)
    stateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTree(false)
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeCacheResult() {
    testStateUpdateComponentMeasureNestedTree(true)
  }

  /**
   * We are using a component hierarchy which calls [Component.measure] in onCreateLayout. The
   * component being measured is NestedTree and has a child component with state.
   *
   * We want to verify if StateUpdate happens on a component (NestedTree -> component) then it
   * should get updated correctly.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private fun testStateUpdateComponentMeasureNestedTree(shouldCacheResult: Boolean) {
    val stateUpdater = ComponentWithCounterStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithMeasureCall.create(context)
                    .component(
                        ComponentWithCounterStateNestedParent.create(context).caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build()
    val testLithoView = createTestLithoView(component)
    stateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeWithNestedChildDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChild(false)
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeWithNestedChildCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChild(true)
  }

  /**
   * We are using a component hierarchy which calls [Component.measure] in onCreateLayout. The
   * component being measured is NestedTree -> NestedTree -> component with state
   *
   * We want to verify if StateUpdate happens on a component (NestedTree -> NestedTree -> component)
   * then it should get updated corectly
   *
   * In this test, component is passed as a child of Column.
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private fun testStateUpdateComponentMeasureNestedTreeWithNestedChild(shouldCacheResult: Boolean) {
    val stateUpdater = ComponentWithCounterStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithMeasureCall.create(context)
                    .component(
                        create(context)
                            .child(
                                ComponentWithCounterStateNestedGrandParent.create(context)
                                    .caller(stateUpdater)))
                    .shouldCacheResult(shouldCacheResult))
            .build()
    val testLithoView = createTestLithoView(component)
    stateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumnDoNotCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(false)
  }

  @Test
  fun testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumnCacheResult() {
    testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(true)
  }

  /**
   * We are using a component hierarchy which calls [Component.measure] in onCreateLayout. The
   * component being measured is NestedTree -> NestedTree -> component with state.
   *
   * We want to verify if StateUpdate happens on a component (NestedTree -> NestedTree -> component)
   * then it should get updated correctly.
   *
   * In this test, component is passed as it is to be measured i.e not a child of Column or any
   * other component
   *
   * @param shouldCacheResult whether to cache the result measured or not
   */
  private fun testStateUpdateComponentMeasureNestedTreeWithNestedChildWithoutColumn(
      shouldCacheResult: Boolean
  ) {
    val stateUpdater = ComponentWithCounterStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithMeasureCall.create(context)
                    .component(
                        ComponentWithCounterStateNestedGrandParent.create(context)
                            .caller(stateUpdater))
                    .shouldCacheResult(shouldCacheResult))
            .build()
    val testLithoView = createTestLithoView(component)
    stateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun testStateUpdateComponentAfterComponentIsRemovedFromHierarchy() {
    val stateUpdater = ComponentWithConditionalChildLayoutSpec.Caller()
    val childStateUpdater = ComponentWithCounterStateLayoutSpec.Caller()
    val component: Component =
        create(context)
            .child(
                ComponentWithConditionalChildLayout.create(context)
                    .caller(stateUpdater)
                    .childCaller(childStateUpdater))
            .build()
    val testLithoView = createTestLithoView(component)
    childStateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
    stateUpdater.hideChildComponent()
    childStateUpdater.increment()
    assertThat(testLithoView.lithoView).hasVisibleText("Empty")
    assertThat(testLithoView.lithoView).doesNotHaveVisibleTextContaining("Count:")
  }

  private fun assertThatNestedGrandParentStateContainerIsInLayoutStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getLayoutStateHandleStateContainer<
            ComponentWithStateAndChildWithStateNestedGrandParent.ComponentWithStateAndChildWithStateNestedGrandParentStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getLayoutInitialStateContainer<
            ComponentWithStateAndChildWithStateNestedGrandParent.ComponentWithStateAndChildWithStateNestedGrandParentStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatNestedGrandParentStateContainerIsInStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getStateHandleStateContainer<
            ComponentWithStateAndChildWithStateNestedGrandParent.ComponentWithStateAndChildWithStateNestedGrandParentStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getInitialStateContainer<
            ComponentWithStateAndChildWithStateNestedGrandParent.ComponentWithStateAndChildWithStateNestedGrandParentStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatNestedParentStateContainerIsInLayoutStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getLayoutStateHandleStateContainer<
            ComponentWithStateAndChildWithStateNestedParent.ComponentWithStateAndChildWithStateNestedParentStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getLayoutInitialStateContainer<
            ComponentWithStateAndChildWithStateNestedParent.ComponentWithStateAndChildWithStateNestedParentStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatNestedParentStateContainerIsInStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getStateHandleStateContainer<
            ComponentWithStateAndChildWithStateNestedParent.ComponentWithStateAndChildWithStateNestedParentStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getInitialStateContainer<
            ComponentWithStateAndChildWithStateNestedParent.ComponentWithStateAndChildWithStateNestedParentStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatParentStateContainerIsInStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getStateHandleStateContainer<
            ComponentWithStateAndChildWithState.ComponentWithStateAndChildWithStateStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getInitialStateContainer<
            ComponentWithStateAndChildWithState.ComponentWithStateAndChildWithStateStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatStateContainerIsInLayoutStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getLayoutStateHandleStateContainer<
            ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getLayoutInitialStateContainer<
            ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private fun assertThatStateContainerIsInStateHandler(
      componentTree: ComponentTree,
      context: ComponentContext,
      expectedStateValue: Int
  ) {
    val stateHandlerStateContainerChild1 =
        getStateHandleStateContainer<
            ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer>(
            componentTree, context.globalKey)
    val initialStateStateContainerChild1 =
        getInitialStateContainer<
            ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer>(
            componentTree, context.globalKey)
    Assertions.assertThat(stateHandlerStateContainerChild1).isNotNull
    Assertions.assertThat(stateHandlerStateContainerChild1.count).isEqualTo(expectedStateValue)
    Assertions.assertThat(initialStateStateContainerChild1).isNull()
  }

  private val looper: Looper
    private get() {
      val defaultThread =
          HandlerThread("testThread", ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY)
      defaultThread.start()
      return defaultThread.looper
    }
}
