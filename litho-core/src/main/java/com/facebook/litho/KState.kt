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

import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.state.ComponentState
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateProvider
import com.facebook.litho.state.StateReadRecorder
import com.facebook.rendercore.utils.NoDeps
import com.facebook.rendercore.utils.areObjectsEquivalent
import java.util.Objects

internal fun <T> ComponentScope.getUpdatedState(
    vararg deps: Any? = NoDeps,
    initializer: () -> T
): T {
  val key = context.globalKey
  val index = useStateIndex++
  val treeState: TreeState = resolveContext.treeState
  val isLayoutState = context.isContextForLayout
  val existing = treeState.getState(key, isLayoutState) as ComponentState<KStateContainer>?
  val deps: Array<Any?> = deps as Array<Any?> /* TODO: Can this cast be avoided? */

  val current: ComponentState<KStateContainer> =
      if (existing == null || existing.value.states.size <= index) {
        // Create the initial state for index since it hasn't been computed yet
        treeState
            .createOrGetInitialHookState(
                key,
                index,
                deps,
                initializer,
                isLayoutState,
                context.scopedComponentInfo.component.simpleName,
            )
            .apply {
              // Put it in the current tree state from the InitialState
              treeState.addState(key, this, isLayoutState)
            }
      } else {
        existing
      }

  val updated: ComponentState<KStateContainer> =
      if (areObjectsEquivalent(current.value.states[index].deps, deps)) {
        current
      } else {
        current.copy(value = current.value.copyAndMutate(index, initializer(), deps)).apply {
          // Put the updated state in the current tree state
          treeState.addState(key, this, isLayoutState)
        }
      }

  context.scopedComponentInfo.state = updated

  // Only need to mark this global key as seen once
  if (index == 0) {
    treeState.markStateInUse(key, isLayoutState)
  }

  @Suppress("UNCHECKED_CAST")
  return updated.value.states[index].value as T
}

/**
 * Declares a state within a Component. The [initializer] will be invoked to provide the initial
 * value of state. The initializer will be invoked again if the [deps] change when the Component is
 * rendered again.
 *
 * Note: The component does not re-render if the [initializer] is invoked. The updated value of the
 * state is visible to currently rendering component.
 *
 * The [initializer] must be free from side effects because it can invoked multiple times from any
 * Thread during the lifecycle of the Component.
 *
 * @see [State]
 */
@ExperimentalLithoApi
@Hook
fun <T> ComponentScope.useStateWithDeps(
    vararg deps: Any? = NoDeps,
    initializer: () -> T
): State<T> {
  val globalKey = context.globalKey
  val hookIndex = useStateIndex
  val lithoTree = context.lithoTree ?: error("LithoTree is null")
  val isLayoutState = context.isContextForLayout

  val result = getUpdatedState(deps = deps, initializer = initializer)

  return State(
      lithoTree.stateProvider,
      lithoTree.stateUpdater,
      hookIndex,
      globalKey,
      isLayoutState,
      context.componentScope,
      lithoTree.isReadTrackingEnabled,
      result,
  )
}

/**
 * Declares a state variable within a Component. The initializer will provide the initial value if
 * it hasn't already been initialized in a previous render of the Component.
 *
 * Assignments to the state variables are allowed only in [updateState] block to batch updates and
 * trigger a UI layout only once per batch.
 */
@Hook
fun <T> ComponentScope.useState(initializer: () -> T): State<T> =
    useStateWithDeps(deps = NoDeps, initializer = initializer)

/** Interface with which a component gets the value from a state or updates it. */
class State<T>
internal constructor(
    private val stateProvider: StateProvider,
    private val stateUpdater: StateUpdater,
    private val hookStateIndex: Int,
    private val globalKey: String,
    internal val isLayoutContext: Boolean,
    private val componentScope: Component?,
    private val isReadTrackingEnabled: Boolean,
    internal val fallback: T
) {

  internal val stateId: StateId =
      StateId(stateProvider.treeId, globalKey, hookStateIndex).also {
        it.ownerName = { componentScope?.simpleName }
      }

  val value: T
    get() {
      if (!isReadTrackingEnabled) return fallback
      return when {
        StateReadRecorder.read(stateId) -> stateProvider.getValue(this)
        else -> fallback
      }
    }

  /**
   * Updates this state value and enqueues a new layout calculation reflecting it to execute in the
   * background.
   */
  fun update(newValue: T) {
    if (canSkip(newValue)) {
      return
    }

    stateUpdater?.updateHookStateAsync(
        stateId, HookUpdaterValue(newValue), componentScope?.simpleName ?: "hook", isLayoutContext)
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
    if (canSkip(newValueFunction)) {
      return
    }

    stateUpdater.updateHookStateAsync(
        stateId,
        HookUpdaterLambda(newValueFunction),
        componentScope?.simpleName ?: "hook",
        isLayoutContext)
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
    if (canSkip(newValue)) {
      return
    }

    stateUpdater.updateHookStateSync(
        stateId, HookUpdaterValue(newValue), componentScope?.simpleName ?: "hook", isLayoutContext)
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
    if (canSkip(newValueFunction)) {
      return
    }

    stateUpdater.updateHookStateSync(
        stateId,
        HookUpdaterLambda(newValueFunction),
        componentScope?.simpleName ?: "hook",
        isLayoutContext)
  }

  inner class HookUpdaterValue(val newValue: T) : HookUpdater {
    override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer {
      return currentState.copyAndMutate(hookStateIndex, newValue)
    }
  }

  inner class HookUpdaterLambda(val newValueFunction: (T) -> T) : HookUpdater {
    override fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer {
      return currentState.copyAndMutate(
          hookStateIndex,
          newValueFunction(currentState.states[hookStateIndex].value as T),
      )
    }
  }

  private fun canSkip(newValue: T): Boolean {
    return stateUpdater.canSkipStateUpdate(stateId, newValue, isLayoutContext)
  }

  private fun canSkip(newValueFunction: (T) -> T): Boolean {
    return stateUpdater.canSkipStateUpdate(newValueFunction, stateId, isLayoutContext)
  }

  /**
   * We consider two state objects equal if they 1) belong to the same Tree, 2) have the same global
   * key and hook index, and 3) have the same value (according to its own .equals check)
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (other !is State<*>) {
      return false
    }

    return globalKey == other.globalKey &&
        hookStateIndex == other.hookStateIndex &&
        fallback == other.fallback
  }

  override fun hashCode(): Int {
    return Objects.hash(globalKey, hookStateIndex, fallback)
  }
}
