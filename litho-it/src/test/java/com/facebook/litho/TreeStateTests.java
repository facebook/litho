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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class TreeStateTests {

  private ComponentContext mContext;

  private Component mTestComponent;

  private String mTestComponentKey;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mTestComponent = new StateUpdateTestComponent();
    mTestComponentKey = mTestComponent.getKey();
  }

  @Test
  public void testQueueStateUpdate() {
    final TreeState treeState = new TreeState();
    final StateUpdateTestComponent.TestStateContainer stateContainer =
        new StateUpdateTestComponent.TestStateContainer();

    // Add state container
    treeState.addStateContainer(mTestComponentKey, stateContainer, false);

    // Queue state update
    treeState.queueStateUpdate(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, false);

    // Apply state updates
    treeState.applyStateUpdatesEarly(mContext, mTestComponent, null, false);

    StateUpdateTestComponent.TestStateContainer previousStateContainer =
        (StateUpdateTestComponent.TestStateContainer)
            treeState.getStateContainer(mTestComponentKey, false);

    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount).isEqualTo(1);
  }

  private void testRegisterUnregisterInitialStateContainer(boolean isNestedTree) {
    final ComponentContext mComponentContext = mock(ComponentContext.class);

    when(mComponentContext.isNestedTreeContext()).thenReturn(isNestedTree);
    when(mComponentContext.getScopedComponentInfo())
        .thenReturn(new ScopedComponentInfo(mTestComponent, mContext, null));

    final TreeState treeState = new TreeState();
    final TreeState localTreeState = new TreeState(treeState);

    // Register local tree state to ISC
    localTreeState.registerRenderState();
    localTreeState.registerLayoutState();

    // Add state containers in ISC
    localTreeState.createOrGetStateContainerForComponent(
        mComponentContext, mTestComponent, mTestComponentKey);

    final StateHandler renderStateHandler =
        Whitebox.getInternalState(treeState, "mRenderStateHandler");

    final StateHandler layoutStateHandler =
        Whitebox.getInternalState(treeState, "mLayoutStateHandler");

    if (isNestedTree) {
      assertThat(renderStateHandler.getInitialStateContainer().initialStates).isEmpty();
      assertThat(layoutStateHandler.getInitialStateContainer().initialStates).isNotEmpty();
    } else {
      assertThat(renderStateHandler.getInitialStateContainer().initialStates).isNotEmpty();
      assertThat(layoutStateHandler.getInitialStateContainer().initialStates).isEmpty();
    }

    // Unregister local tree state from ISC
    treeState.unregisterRenderState(localTreeState);
    treeState.unregisterLayoutState(localTreeState);

    // State containers in ISC should be cleared
    assertThat(renderStateHandler.getInitialStateContainer()).isNotNull();
    assertThat(renderStateHandler.getInitialStateContainer().initialStates).isEmpty();

    assertThat(layoutStateHandler.getInitialStateContainer()).isNotNull();
    assertThat(layoutStateHandler.getInitialStateContainer().initialStates).isEmpty();
  }

  @Test
  public void testRegisterUnregisterInitialStateContainerRenderState() {
    testRegisterUnregisterInitialStateContainer(false);
  }

  @Test
  public void testRegisterUnregisterInitialStateContainerLayoutState() {
    testRegisterUnregisterInitialStateContainer(true);
  }

  @Test
  public void testCommitRenderState() {
    final TreeState previousTreeState = new TreeState();
    previousTreeState.addStateContainer(
        mTestComponentKey, new StateUpdateTestComponent.TestStateContainer(), false);

    final TreeState treeState = new TreeState();
    treeState.commitRenderState(previousTreeState);

    StateUpdateTestComponent.TestStateContainer previousStateContainer =
        (StateUpdateTestComponent.TestStateContainer)
            treeState.getStateContainer(mTestComponentKey, false);

    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount).isEqualTo(0);
  }

  @Test
  public void testCommitLayoutState() {
    final TreeState previousTreeState = new TreeState();
    previousTreeState.addStateContainer(
        mTestComponentKey, new StateUpdateTestComponent.TestStateContainer(), true);

    final TreeState treeState = new TreeState();
    treeState.commitLayoutState(previousTreeState);

    StateUpdateTestComponent.TestStateContainer previousStateContainer =
        (StateUpdateTestComponent.TestStateContainer)
            treeState.getStateContainer(mTestComponentKey, true);

    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount).isEqualTo(0);
  }

  @Test
  public void testCopyPreviousState() {
    final TreeState previousTreeState = new TreeState();
    previousTreeState.addStateContainer(
        mTestComponentKey, new StateUpdateTestComponent.TestStateContainer(), false);

    final TreeState treeState = new TreeState(previousTreeState);

    StateUpdateTestComponent.TestStateContainer previousStateContainer =
        (StateUpdateTestComponent.TestStateContainer)
            treeState.getStateContainer(mTestComponentKey, false);

    assertThat(previousStateContainer).isNotNull();
    assertThat(previousStateContainer.mCount).isEqualTo(0);
  }

  @Test
  public void testKeysForPendingUpdates() {
    final TreeState treeState = new TreeState();
    final String anotherTestComponentKey = "anotherTestComponentKey";

    // State Update on main tree component
    treeState.queueStateUpdate(
        mTestComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), false, false);

    // State Update on nested tree component
    treeState.queueStateUpdate(
        anotherTestComponentKey,
        StateUpdateTestComponent.createIncrementStateUpdate(),
        false,
        true);

    final Set<String> renderKeysForPendingUpdates = treeState.getKeysForPendingRenderStateUpdates();
    assertThat(renderKeysForPendingUpdates).contains(mTestComponentKey);

    final Set<String> layoutKeysForPendingUpdates = treeState.getKeysForPendingLayoutStateUpdates();
    assertThat(layoutKeysForPendingUpdates).contains(anotherTestComponentKey);
  }
}
