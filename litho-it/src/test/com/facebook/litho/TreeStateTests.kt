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

package com.facebook.litho

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.state.ComponentState
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class TreeStateTests {

  private lateinit var context: ComponentContext
  private lateinit var testComponent: StateUpdateTestComponent
  private lateinit var testComponentKey: String

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    testComponent = StateUpdateTestComponent()
    testComponentKey = testComponent.key
  }

  @Test
  fun testQueueStateUpdate() {
    val treeState = TreeState()
    val stateContainer = ComponentState(value = StateUpdateTestComponent.TestStateContainer())

    // Add state container
    treeState.addState(testComponentKey, stateContainer, false)

    // Queue state update
    treeState.queueStateUpdate(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, false)

    // Apply state updates
    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(1)
  }

  private fun testRegisterUnregisterInitialStateContainer(isNestedTree: Boolean) {
    val componentContext: ComponentContext = mock()
    whenever(componentContext.isNestedTreeContext).thenReturn(isNestedTree)
    whenever(componentContext.scopedComponentInfo)
        .thenReturn(ScopedComponentInfo(testComponent, context, null))
    val treeState = TreeState()
    val localTreeState = TreeState(treeState)

    // Register local tree state to ISC
    localTreeState.registerResolveState()
    localTreeState.registerLayoutState()

    // Add state containers in ISC
    localTreeState.createOrGetState(componentContext, testComponent, testComponentKey)
    val resolveState = treeState.resolveState
    val layoutStateHandler = treeState.layoutState
    if (isNestedTree) {
      assertThat(resolveState.initialState.states).isEmpty()
      assertThat(layoutStateHandler.initialState.states).isNotEmpty
    } else {
      assertThat(resolveState.initialState.states).isNotEmpty
      assertThat(layoutStateHandler.initialState.states).isEmpty()
    }

    // Unregister local tree state from ISC
    localTreeState.unregisterResolveInitialState()
    localTreeState.unregisterLayoutInitialState()

    // State containers in ISC should be cleared
    assertThat(resolveState.initialState).isNotNull
    assertThat(resolveState.initialState.states).isEmpty()
    assertThat(layoutStateHandler.initialState).isNotNull
    assertThat(layoutStateHandler.initialState.states).isEmpty()
  }

  @Test
  fun testRegisterUnregisterInitialStateContainerRenderState() {
    testRegisterUnregisterInitialStateContainer(false)
  }

  @Test
  fun testRegisterUnregisterInitialStateContainerLayoutState() {
    testRegisterUnregisterInitialStateContainer(true)
  }

  @Test
  fun testCommitRenderState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        false,
    )
    val treeState = TreeState()
    treeState.commitResolveState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCommitLayoutState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        true,
    )
    val treeState = TreeState()
    treeState.commitLayoutState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, true)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCopyPreviousState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        false,
    )
    val treeState = TreeState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testKeysForPendingUpdates() {
    val treeState = TreeState()
    val anotherTestComponentKey = "anotherTestComponentKey"

    // State Update on main tree component
    treeState.queueStateUpdate(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, false)

    // State Update on nested tree component
    treeState.queueStateUpdate(
        anotherTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, true)
    val renderKeysForPendingUpdates = treeState.keysForPendingResolveStateUpdates
    assertThat(renderKeysForPendingUpdates).contains(testComponentKey)
    val layoutKeysForPendingUpdates = treeState.keysForPendingLayoutStateUpdates
    assertThat(layoutKeysForPendingUpdates).contains(anotherTestComponentKey)
  }

  @Test
  fun `queueDuplicateStateUpdates enabled - StateUpdate is enqueued if it is a duplicate`() {
    val treeState = TreeState(fromState = null)

    treeState.createOrGetState(context, testComponent, testComponentKey)

    treeState.assertThatTestStateContainerHasValue(4)

    // increment
    treeState.queueStateUpdate(
        key = testComponentKey,
        stateUpdate = StateUpdateTestComponent.createIncrementStateUpdate(),
        isLazyStateUpdate = false,
        isNestedTree = false)

    assertThat(treeState.hasUncommittedUpdates()).isTrue

    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    treeState.assertThatTestStateContainerHasValue(5)

    // update with the same value (duplicate)
    val duplicateUpdateEnqueued =
        treeState.queueStateUpdate(
            key = testComponentKey,
            stateUpdate = StateUpdateTestComponent.createValueStateUpdate(5),
            isLazyStateUpdate = false,
            isNestedTree = false)

    assertThat(treeState.hasUncommittedUpdates()).isTrue
    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    treeState.assertThatTestStateContainerHasValue(5)
  }

  private fun TreeState.assertThatTestStateContainerHasValue(value: Int) {
    assertThat(
            (getState(testComponentKey, false)?.value
                    as StateUpdateTestComponent.TestStateContainer)
                .count)
        .isEqualTo(value)
  }
}
