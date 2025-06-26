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
import com.facebook.litho.state.StateId
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Lists
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the hooks state (useState) integration with StateHandler. */
@RunWith(LithoTestRunner::class)
class HooksStateHandlerTest {

  private val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())

  @Test
  fun copyHandler_copyingEmptyStateHandler_createsEmptyStateHandler() {
    val first = StateHandler()
    val second = StateHandler(first)
    assertThat(second.hasUncommittedUpdates()).isFalse
    assertThat(second.isEmpty).isTrue
  }

  @Test
  fun `when copying StateHandler and applying updates on new StateHandler then state updates should be applied`() {
    val bazState = Any()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    val first = StateHandler()
    first.addState(GLOBAL_KEY, ComponentState(value = kStateContainer))
    first.queueHookStateUpdate(
        StateId(-1, GLOBAL_KEY, 0),
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue")
        })
    assertThat(first.hasUncommittedUpdates()).isTrue
    val second = StateHandler(first)
    second.applyStateUpdatesEarly(context = c, component = null, prevTreeRootNode = null)
    assertThat(first.hasUncommittedUpdates()).isTrue
    assertThat(second.hasUncommittedUpdates()).isTrue
    val secondKstate = second.getState(GLOBAL_KEY)?.value as KStateContainer?
    second.markStateInUse(GLOBAL_KEY)
    assertThat(secondKstate?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val firstState = first.getState(GLOBAL_KEY)?.value as KStateContainer?
    assertThat(firstState?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"))
  }

  @Test
  fun `when copying StateHandler and applying multiple updates on new StateHandler then all state updates should be applied`() {
    val bazState = Any()
    val first = StateHandler()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    first.addState(GLOBAL_KEY, ComponentState(value = kStateContainer))
    val stateId = StateId(-1, GLOBAL_KEY, 0)
    first.queueHookStateUpdate(
        stateId,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5)
        })
    first.queueHookStateUpdate(
        stateId,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, 1 + currentState.states.map { it.value }[1] as Int)
        })
    assertThat(first.hasUncommittedUpdates()).isTrue
    val second = StateHandler(first)
    second.applyStateUpdatesEarly(context = c, component = null, prevTreeRootNode = null)
    assertThat(first.hasUncommittedUpdates()).isTrue
    assertThat(second.hasUncommittedUpdates()).isTrue
    assertThat(second.state).hasSize(1)
    val secondKstate = second.getState(GLOBAL_KEY)?.value as KStateContainer?
    second.markStateInUse(GLOBAL_KEY)
    assertThat(secondKstate?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val kState = first.getState(GLOBAL_KEY)?.value as KStateContainer?
    assertThat(kState?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
  }

  @Test
  fun `when copying StateHandler multiple times with multiple updates then all state updates should be applied`() {
    val bazState = Any()
    val first = StateHandler()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    first.addState(GLOBAL_KEY, ComponentState(value = kStateContainer))
    val stateId = StateId(-1, GLOBAL_KEY, 0)
    first.queueHookStateUpdate(
        stateId,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5)
        })
    first.queueHookStateUpdate(
        stateId,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, currentState.states.map { it.value }[1] as Int + 1)
        })
    val second = StateHandler(first)
    second.applyStateUpdatesEarly(context = c, component = null, prevTreeRootNode = null)
    first.queueHookStateUpdate(
        stateId,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, currentState.states.map { it.value }[1] as Int + 1)
        })
    val third = StateHandler(first)
    third.applyStateUpdatesEarly(context = c, component = null, prevTreeRootNode = null)
    third.markStateInUse(GLOBAL_KEY)
    val secondKstate = second.getState(GLOBAL_KEY)?.value as KStateContainer?
    second.markStateInUse(GLOBAL_KEY)
    assertThat(secondKstate?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isTrue
    val kState = first.getState(GLOBAL_KEY)?.value as KStateContainer?
    assertThat(kState?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(third)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val firstStateUpdated = first.getState(GLOBAL_KEY)?.value as KStateContainer?
    assertThat(firstStateUpdated?.states?.map { it.value })
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 7, bazState, "newValue"))
  }

  @Test
  fun `when input changes and new cache value is evaluated then previous data is discarded`() {
    val handler = StateHandler()
    val input = "input"
    val newInput = "new-input"
    val value = "value"
    val newValue = "new-value"
    handler.putCachedValue(GLOBAL_KEY, 0, input, value)
    assertThat(handler.getCachedValue(GLOBAL_KEY, 0, input)).isEqualTo(value)
    handler.putCachedValue(GLOBAL_KEY, 0, newInput, newValue)
    assertThat(handler.getCachedValue(GLOBAL_KEY, 0, input)).isNull()
    assertThat(handler.getCachedValue(GLOBAL_KEY, 0, newInput)).isEqualTo(newValue)
  }

  companion object {
    private const val GLOBAL_KEY = "globalKey"
  }
}
