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
  private lateinit var testComponent: Component
  private lateinit var testComponentKey: String

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    testComponent = StateUpdateTestComponent()
    testComponentKey = testComponent.getKey()
  }

  @Test
  fun testQueueStateUpdate() {
    val treeState = TreeState()
    val stateContainer = StateUpdateTestComponent.TestStateContainer()

    // Add state container
    treeState.addStateContainer(testComponentKey, stateContainer, false)

    // Queue state update
    treeState.queueStateUpdate(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, false)

    // Apply state updates
    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    val previousStateContainer =
        treeState.getStateContainer(testComponentKey, false)
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
    localTreeState.createOrGetStateContainerForComponent(
        componentContext, testComponent, testComponentKey)
    val resolveState = treeState.resolveState
    val layoutStateHandler = treeState.layoutState
    if (isNestedTree) {
      assertThat(resolveState.initialStateContainer.initialStates).isEmpty()
      assertThat(layoutStateHandler.initialStateContainer.initialStates).isNotEmpty
    } else {
      assertThat(resolveState.initialStateContainer.initialStates).isNotEmpty
      assertThat(layoutStateHandler.initialStateContainer.initialStates).isEmpty()
    }

    // Unregister local tree state from ISC
    localTreeState.unregisterResolveInitialState()
    localTreeState.unregisterLayoutInitialState()

    // State containers in ISC should be cleared
    assertThat(resolveState.initialStateContainer).isNotNull
    assertThat(resolveState.initialStateContainer.initialStates).isEmpty()
    assertThat(layoutStateHandler.initialStateContainer).isNotNull
    assertThat(layoutStateHandler.initialStateContainer.initialStates).isEmpty()
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
    previousTreeState.addStateContainer(
        testComponentKey, StateUpdateTestComponent.TestStateContainer(), false)
    val treeState = TreeState()
    treeState.commitResolveState(previousTreeState)
    val previousStateContainer =
        treeState.getStateContainer(testComponentKey, false)
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCommitLayoutState() {
    val previousTreeState = TreeState()
    previousTreeState.addStateContainer(
        testComponentKey, StateUpdateTestComponent.TestStateContainer(), true)
    val treeState = TreeState()
    treeState.commitLayoutState(previousTreeState)
    val previousStateContainer =
        treeState.getStateContainer(testComponentKey, true)
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCopyPreviousState() {
    val previousTreeState = TreeState()
    previousTreeState.addStateContainer(
        testComponentKey, StateUpdateTestComponent.TestStateContainer(), false)
    val treeState = TreeState(previousTreeState)
    val previousStateContainer =
        treeState.getStateContainer(testComponentKey, false)
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
}
