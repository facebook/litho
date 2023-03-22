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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Lists
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the hooks state (useState) integration with StateHandler. */
@RunWith(LithoTestRunner::class)
class HooksStateHandlerTest {

  @Test
  fun copyHandler_copyingEmptyStateHandler_createsEmptyStateHandler() {
    val first = StateHandler()
    val second = StateHandler(first)
    assertThat(second.hasUncommittedUpdates()).isFalse
    assertThat(second.isEmpty).isTrue
  }

  @Test
  fun commit_copyingHandlerWithPendingStateUpdateAndCommittingIt_copiesAllOldStateValuesAndAppliesStateUpdate() {
    val bazState = Any()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    val first = StateHandler()
    first.addStateContainer(GLOBAL_KEY, kStateContainer)
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue")
        })
    assertThat(first.hasUncommittedUpdates()).isTrue
    val second = StateHandler(first)
    assertThat(first.hasUncommittedUpdates()).isTrue
    assertThat(second.hasUncommittedUpdates()).isTrue
    val secondKstate = second.getStateContainer(GLOBAL_KEY) as KStateContainer?
    second.keepStateContainerForGlobalKey(GLOBAL_KEY)
    assertThat(secondKstate?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val firstState = first.getStateContainer(GLOBAL_KEY) as KStateContainer?
    assertThat(firstState?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 4, bazState, "newValue"))
  }

  @Test
  fun commit_copyingHandlerWithMultiplePendingStateUpdatesAndCommittingIt_copiesAllOldStateValuesAndAppliesAllStateUpdates() {
    val bazState = Any()
    val first = StateHandler()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    first.addStateContainer(GLOBAL_KEY, kStateContainer)
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5)
        })
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, 1 + currentState.states[1] as Int)
        })
    assertThat(first.hasUncommittedUpdates()).isTrue
    val second = StateHandler(first)
    assertThat(first.hasUncommittedUpdates()).isTrue
    assertThat(second.hasUncommittedUpdates()).isTrue
    assertThat(second.stateContainers).hasSize(1)
    val secondKstate = second.getStateContainer(GLOBAL_KEY) as KStateContainer?
    second.keepStateContainerForGlobalKey(GLOBAL_KEY)
    assertThat(secondKstate?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val kState = first.getStateContainer(GLOBAL_KEY) as KStateContainer?
    assertThat(kState?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
  }

  @Test
  fun commit_committingHandlerFirstWithPartOfStateUpdatesAndSecondTimeWithAllStateUpdates_atFirstAppliesOnlyStateUpdatesFromFirstCommitAndKeepOthersPendingAndThenAppliesAllStateUpdates() {
    val bazState = Any()
    val first = StateHandler()
    var kStateContainer = KStateContainer.withNewState(null, "test")
    kStateContainer = KStateContainer.withNewState(kStateContainer, 4)
    kStateContainer = KStateContainer.withNewState(kStateContainer, bazState)
    first.addStateContainer(GLOBAL_KEY, kStateContainer)
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              KStateContainer.withNewState(currentState, "newValue").copyAndMutate(1, 5)
        })
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, currentState.states[1] as Int + 1)
        })
    val second = StateHandler(first)
    first.queueHookStateUpdate(
        GLOBAL_KEY,
        object : HookUpdater {
          override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer =
              currentState.copyAndMutate(1, currentState.states[1] as Int + 1)
        })
    val third = StateHandler(first)
    third.keepStateContainerForGlobalKey(GLOBAL_KEY)
    val secondKstate = second.getStateContainer(GLOBAL_KEY) as KStateContainer?
    second.keepStateContainerForGlobalKey(GLOBAL_KEY)
    assertThat(secondKstate?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(second)
    assertThat(first.hasUncommittedUpdates()).isTrue
    val kState = first.getStateContainer(GLOBAL_KEY) as KStateContainer?
    assertThat(kState?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 6, bazState, "newValue"))
    first.commit(third)
    assertThat(first.hasUncommittedUpdates()).isFalse
    val firstStateUpdated = first.getStateContainer(GLOBAL_KEY) as KStateContainer?
    assertThat(firstStateUpdated?.states)
        .hasSize(4)
        .isEqualTo(Lists.newArrayList("test", 7, bazState, "newValue"))
  }

  companion object {
    private const val GLOBAL_KEY = "globalKey"
  }
}
