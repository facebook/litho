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

package com.facebook.litho

import com.facebook.litho.annotations.Hook

/**
 * Declares a state variable within a Component. The initializer will provide the initial value if
 * it hasn't already been initialized in a previous render of the Component.
 *
 * Assignments to the state variables are allowed only in [updateState] block to batch updates and
 * trigger a UI layout only once per batch.
 */
@Hook
fun <T> ComponentScope.useState(initializer: () -> T): State<T> {
  val globalKey = context.globalKey
  val hookIndex = useStateIndex++
  val stateHandler = layoutStateContext?.stateHandler!!
  val kState = stateHandler.getStateContainer(globalKey) as KStateContainer?

  if (kState == null || kState.mStates.size <= hookIndex) {
    // The initial state was not computed yet. let's create it and put it in the state
    val state =
        context.componentTree.initialStateContainer.createOrGetInitialHookState(
            globalKey, hookIndex, initializer)
    stateHandler.stateContainers[globalKey] = state

    return State(context, hookIndex, state.mStates[hookIndex] as T)
  }

  return State(context, hookIndex, kState.mStates[hookIndex] as T)
}

/** Interface with which a component gets the value from a state or updates it. */
class State<T>(
    private val context: ComponentContext,
    private val hookStateIndex: Int,
    val value: T
) {

  /**
   * Updates this state value and enqueues a new layout calculation reflecting it to execute in the
   * background.
   */
  fun update(newValue: T) {
    context.updateHookStateAsync(context.globalKey) { stateHandler ->
      val currentState = stateHandler.mStateContainers[context.globalKey] as KStateContainer?
      // currentState could be null if the state is removed from the StateHandler before the update
      // runs
      if (currentState != null) {
        stateHandler.mStateContainers[context.globalKey] =
            currentState.copyAndMutate(hookStateIndex, newValue)
      }
    }
  }

  /**
   * Uses [newValueFunction] to update this state value using the previous state value, and enqueues
   * a new layout calculation reflecting it to execute in the background.
   *
   * [newValueFunction] receives the current state value and can use it to compute the update: this
   * is useful when there could be other enqueued updates that may not have been applied yet.
   *
   * For example, if your state update should increment a counter, using the function version of
   * [update] with `count -> count + 1` will allow you to account for updates that are in flight but
   * not yet applied (e.g. if the user has tapped a button triggering the update multiple times in
   * succession).
   */
  fun update(newValueFunction: (T) -> T) {
    context.updateHookStateAsync(context.globalKey) { stateHandler ->
      val currentState = stateHandler.mStateContainers[context.globalKey] as KStateContainer?
      // currentState could be null if the state is removed from the StateHandler before the update
      // runs
      if (currentState != null) {
        stateHandler.mStateContainers[context.globalKey] =
            currentState.copyAndMutate(
                hookStateIndex, newValueFunction(currentState.mStates[hookStateIndex] as T))
      }
    }
  }

  /**
   * Updates this state value and enqueues a new layout calculation reflecting it to execute on the
   * current thread. If called on the main thread, this means that the UI will be updated for the
   * current frame.
   *
   * Note: If [updateSync] is used on the main thread, it can easily cause dropped frames and
   * degrade user experience. Therefore it should only be used in exceptional circumstances or when
   * it's known to be executed off the main thread.
   */
  fun updateSync(newValue: T) {
    context.updateHookStateSync(context.globalKey) { stateHandler ->
      val currentState = stateHandler.mStateContainers[context.globalKey] as KStateContainer?
      // currentState could be null if the state is removed from the StateHandler before the update
      // runs
      if (currentState != null) {
        stateHandler.mStateContainers[context.globalKey] =
            currentState.copyAndMutate(hookStateIndex, newValue)
      }
    }
  }

  /**
   * Uses [newValueFunction] to update this state value using the previous state value, and enqueues
   * a new layout calculation reflecting it to execute on the current thread.
   *
   * [newValueFunction] receives the current state value and can use it to compute the update: this
   * is useful when there could be other enqueued updates that may not have been applied yet.
   *
   * For example, if your state update should increment a counter, using the function version of
   * [update] with `count -> count + 1` will allow you to account for updates that are in flight but
   * not yet applied (e.g. if the user has tapped a button triggering the update multiple times in
   * succession).
   *
   * Note: If [updateSync] is used on the main thread, it can easily cause dropped frames and
   * degrade user experience. Therefore it should only be used in exceptional circumstances or when
   * it's known to be executed off the main thread.
   */
  fun updateSync(newValueFunction: (T) -> T) {
    context.updateHookStateSync(context.globalKey) { stateHandler ->
      val currentState = stateHandler.mStateContainers[context.globalKey] as KStateContainer?
      // currentState could be null if the state is removed from the StateHandler before the update
      // runs
      if (currentState != null) {
        stateHandler.mStateContainers[context.globalKey] =
            currentState.copyAndMutate(
                hookStateIndex, newValueFunction(currentState.mStates[hookStateIndex] as T))
      }
    }
  }
}
