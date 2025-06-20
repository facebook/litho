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

package com.facebook.litho.state

import com.facebook.litho.KStateContainer
import com.facebook.litho.State
import com.facebook.litho.TreeState
import com.facebook.litho.utils.LithoThreadLocal

/**
 * StateProvider is an abstraction used to supply the latest (and most accurate) value of a given
 * [State] in a given scope, when read tracking is enabled.
 *
 * This makes it possible for the same [State] object to return different [State.value] in various
 * scopes, unlocking new possibilities. For example, a state that's read in post-mount will have the
 * correct value without having to recreate that state object when its value gets updated. This
 * works even if the previous phases were completely skipped.
 */
interface StateProvider {
  /**
   * An identifier for the state provider's tree.
   *
   * It is especially useful in a setting where state may be read across tree boundaries. In such
   * case, it may be desirable to adjust the access/tracking logic accordingly.
   */
  val treeId: Int

  /**
   * Registers a [TreeState] that the StateProvider should use to supply the appropriate [State]
   * value.
   *
   * This function should typically be used together with [exitScope] in a try...finally block.
   *
   * @param [source] [TreeState] that should be used to supply [State] value
   */
  fun enterScope(source: TreeState)

  /**
   * Unregisters the [TreeState] from the StateProvider.
   *
   * @param [source] [TreeState] that is being unregistered from the state provider
   */
  fun exitScope(source: TreeState)

  /**
   * Retrieves the current value of the given [state] from the active scope.
   *
   * @param [state] state whose value is being requested
   */
  fun <T> getValue(state: State<T>): T
}

/**
 * Internal abstraction used to provide the default [TreeState] for use by the [StateProvider].
 *
 * The [TreeState] supplied here is typically used whenever there's no active scope in flight.
 */
internal interface TreeStateProvider {
  /**
   * The default [TreeState] needed by the [StateProvider].
   *
   * This value can also be null in some rare cases, which typically indicates that the tree state
   * may have been disposed.
   */
  val treeState: TreeState?
}

internal class StateProviderImpl(
    override val treeId: Int,
    private val isReadTrackingEnabled: Boolean,
    private val treeStateProvider: TreeStateProvider
) : StateProvider {

  private val currentSource = LithoThreadLocal<TreeState>()

  override fun enterScope(source: TreeState) {
    if (!isReadTrackingEnabled) return // Noop if read tracking is disabled
    check(currentSource.get() == null)
    currentSource.set(source)
  }

  override fun exitScope(source: TreeState) {
    if (!isReadTrackingEnabled) return // Noop if read tracking is disabled
    check(source === currentSource.get())
    currentSource.set(null)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> getValue(state: State<T>): T {
    val stateId = state.stateId
    require(stateId.treeId == treeId) {
      "State tree (id=${stateId.treeId}) does not match StateProvider tree (id=$treeId)"
    }
    val componentState =
        when (val source = currentSource.get()) {
          null ->
              // The component state for a given key may not exist on the main tree state which
              // may imply that the corresponding component was removed from the tree
              treeStateProvider.treeState?.getState(stateId.globalKey, state.isLayoutContext)
                  ?: return state.fallback
          else ->
              // The component state must be present when accessing the overridden tree state
              // since access requires prior creation
              checkNotNull(source.getState(stateId.globalKey, state.isLayoutContext))
        }
    check(componentState.value is KStateContainer)
    return componentState.value.states[stateId.index].value as T
  }
}
