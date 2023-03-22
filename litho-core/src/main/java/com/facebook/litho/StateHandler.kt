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

import androidx.annotation.VisibleForTesting
import com.facebook.infer.annotation.ThreadSafe
import com.facebook.litho.SpecGeneratedComponent.TransitionContainer
import com.facebook.litho.StateContainer.StateUpdate
import com.facebook.litho.stats.LithoStats
import com.facebook.rendercore.transitions.TransitionUtils
import java.lang.Exception
import javax.annotation.concurrent.GuardedBy
import kotlin.collections.HashSet

/** Holds information about the current State of the components in a Component Tree. */
class StateHandler @VisibleForTesting constructor(stateHandler: StateHandler? = null) {

  /** List of state updates that will be applied during the next layout pass. */
  @GuardedBy("this")
  private val _pendingStateUpdates: MutableMap<String, MutableList<StateUpdate>> =
      HashMap(INITIAL_MAP_CAPACITY)

  /** List of lazy state updates. */
  @GuardedBy("this")
  private val _pendingLazyStateUpdates: MutableMap<String, MutableList<StateUpdate>> =
      HashMap(INITIAL_MAP_CAPACITY)

  /** List of transitions from state update that will be applied on next mount. */
  @GuardedBy("this")
  private val _pendingStateUpdateTransitions: MutableMap<String, List<Transition>> = mutableMapOf()

  /** List of transitions from state update that have been applied on next mount. */
  @GuardedBy("this")
  private val _appliedStateUpdates: MutableMap<String, List<StateUpdate>> =
      HashMap(INITIAL_MAP_CAPACITY)

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   */
  @GuardedBy("this") private val _stateContainers: MutableMap<String, StateContainer> = HashMap()

  /**
   * Contains all keys of components that were present in the current ComponentTree and therefore
   * their StateContainer needs to be kept around.
   */
  @GuardedBy("this") private val neededStateContainers = HashSet<String>()

  /** Map of all cached values that are stored for the current ComponentTree. */
  @GuardedBy("this") private var cachedValues: MutableMap<Any, Any?>? = null

  // These are both lists of (globalKey, updateMethod) pairs, where globalKey is the global key
  // of the component the update applies to
  @GuardedBy("this")
  private val pendingHookUpdates: MutableMap<String, MutableList<HookUpdater>> = mutableMapOf()
  private var appliedHookUpdates: Map<String, List<HookUpdater>> = emptyMap()

  var initialStateContainer: InitialStateContainer
    private set

  private var stateContainerNotFoundForKeys: HashSet<String>? = null

  @get:Synchronized
  val isEmpty: Boolean
    get() = _stateContainers.isEmpty()

  /**
   * @return whether this StateHandler has updates that haven't been committed to the
   *   source-of-truth StateHandler on the ComponentTree.
   */
  @Synchronized
  fun hasUncommittedUpdates(): Boolean =
      // Because we immediately apply Kotlin state updates at the  beginning of layout, we need to
      // also check applied state updates to see if this StateHandler has uncommitted updates.
      _pendingStateUpdates.isNotEmpty() ||
          pendingHookUpdates.isNotEmpty() ||
          appliedHookUpdates.isNotEmpty()

  /**
   * Adds a state update to the list of the state updates that will be applied for the given
   * component key during the next layout pass.
   *
   * @param key the global key of the component
   * @param stateUpdate the state update to apply to the component
   * @param isLazyStateUpdate the flag to indicate if it's a lazy state update
   */
  @Synchronized
  fun queueStateUpdate(key: String, stateUpdate: StateUpdate, isLazyStateUpdate: Boolean) {
    addStateUpdateForKey(key, stateUpdate, _pendingStateUpdates)
    if (isLazyStateUpdate) {
      addStateUpdateForKey(key, stateUpdate, _pendingLazyStateUpdates)
    }
  }

  fun keepStateContainerForGlobalKey(key: String) {
    neededStateContainers.add(key)
  }

  /**
   * StateContainer in this StateHandler should be accessed using this method as it will also ensure
   * that the state is marked as needed
   */
  fun getStateContainer(key: String): StateContainer? = _stateContainers[key]

  fun createOrGetStateContainerForComponent(
      scopedContext: ComponentContext,
      component: Component,
      key: String
  ): StateContainer {
    val currentStateContainer: StateContainer? = synchronized(this) { _stateContainers[key] }

    return if (currentStateContainer != null) {
      neededStateContainers.add(key)
      currentStateContainer
    } else {
      val initialState =
          initialStateContainer.createOrGetInitialStateForComponent(component, scopedContext, key)
      addStateContainer(key, initialState)
      initialState
    }
  }

  private fun applyStateUpdates(key: String, newStateContainer: StateContainer) {
    // If there are no state updates pending for this component, simply store its current state.
    val stateUpdatesForKey = synchronized(this) { _pendingStateUpdates[key] } ?: return

    var transitionsFromStateUpdate: MutableList<Transition>? = null
    val asTransitionContainer = newStateContainer as? TransitionContainer

    for (update in stateUpdatesForKey) {
      if (asTransitionContainer != null) {
        val transition = asTransitionContainer.applyStateUpdateWithTransition(update)
        if (transition != null) {
          if (transitionsFromStateUpdate == null) {
            transitionsFromStateUpdate = ArrayList()
          }
          TransitionUtils.setOwnerKey(transition, key)
          transitionsFromStateUpdate.add(transition)
        }
      } else {
        newStateContainer.applyStateUpdate(update)
      }
    }

    LithoStats.incrementComponentAppliedStateUpdateCountBy(stateUpdatesForKey.size.toLong())

    synchronized(this) {
      _pendingLazyStateUpdates.remove(key)
      _appliedStateUpdates[key] = stateUpdatesForKey // add to applied

      if (!transitionsFromStateUpdate.isNullOrEmpty()) {
        _pendingStateUpdateTransitions[key] = transitionsFromStateUpdate
      }
    }
  }

  @ThreadSafe(enableChecks = false)
  @Synchronized
  fun applyStateUpdatesEarly(
      context: ComponentContext,
      component: Component?,
      prevTreeRootNode: LithoNode?
  ) {
    for ((key, _) in _pendingStateUpdates) {
      try {
        var stateContainer = _stateContainers[key]
        if (stateContainer == null) {
          stateContainer = initialStateContainer.getInitialStateForComponent(key)
        }
        if (stateContainer == null) {
          if (stateContainerNotFoundForKeys == null) {
            stateContainerNotFoundForKeys = HashSet()
          }
          stateContainerNotFoundForKeys?.add(key)
          continue
        }
        val newStateContainer = stateContainer.clone()
        _stateContainers[key] = newStateContainer
        applyStateUpdates(key, newStateContainer)
      } catch (ex: Exception) {

        // Remove pending state update from ComponentTree's state handler since we don't want to
        // process this pending state update again. If we don't remove it and someone is using
        // setRoot in onError api then we can end up in an infinite loop
        context.removePendingStateUpdate(key, context.isNestedTreeContext)
        if (prevTreeRootNode != null) {
          handleExceptionDuringApplyStateUpdate(key, prevTreeRootNode, ex)
        } else {
          ComponentUtils.handleWithHierarchy(context, component, ex)
        }
      }
    }
    _pendingStateUpdates.clear()
  }

  @Synchronized
  fun removePendingStateUpdate(key: String) {
    _pendingStateUpdates.remove(key)
  }

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   *
   * @param key the key of component
   * @param state the state value that needs to be retained
   */
  @Synchronized
  fun addStateContainer(key: String, state: StateContainer) {
    neededStateContainers.add(key)
    _stateContainers[key] = state
  }

  fun applyLazyStateUpdatesForContainer(
      componentKey: String,
      container: StateContainer
  ): StateContainer {
    val stateUpdatesForKey = synchronized(this) { pendingLazyStateUpdates?.get(componentKey) }

    if (stateUpdatesForKey.isNullOrEmpty()) {
      return container
    }

    val containerWithUpdatesApplied = container.clone()
    for (update in stateUpdatesForKey) {
      containerWithUpdatesApplied.applyStateUpdate(update)
    }

    return containerWithUpdatesApplied
  }

  /**
   * Removes a list of state updates that have been applied from the pending state updates list and
   * updates the map of current components with the given components.
   *
   * @param stateHandler state handler that was used to apply state updates in a layout pass
   */
  fun commit(stateHandler: StateHandler) {
    clearStateUpdates(stateHandler.appliedStateUpdates)
    clearUnusedStateContainers(stateHandler)
    copyCurrentStateContainers(stateHandler.stateContainers)
    copyPendingStateTransitions(stateHandler.pendingStateUpdateTransitions)
    commitHookState(stateHandler.appliedHookUpdates)
    stateContainerNotFoundForKeys?.clear()
  }

  @get:Synchronized
  val keysForPendingUpdates: Set<String>
    get() =
        HashSet<String>().apply {
          addAll(_appliedStateUpdates.keys)
          addAll(_pendingStateUpdates.keys)
          addAll(pendingHookUpdates.keys)
          addAll(appliedHookUpdates.keys)
        }

  private fun clearStateUpdates(appliedStateUpdates: Map<String, List<StateUpdate>>?) {
    synchronized(this) {
      if (appliedStateUpdates == null || _pendingStateUpdates.isEmpty()) {
        return
      }
    }

    for ((appliedStateUpdateKey, appliedStateUpdatesForKey) in appliedStateUpdates!!) {
      val (pendingStateUpdatesForKey, pendingLazyStateUpdatesForKey) =
          synchronized(this) {
            _pendingStateUpdates[appliedStateUpdateKey] to
                _pendingLazyStateUpdates[appliedStateUpdateKey]
          }

      if (pendingStateUpdatesForKey == null) {
        continue
      }

      if (pendingStateUpdatesForKey.size == appliedStateUpdatesForKey.size) {
        synchronized(this) {
          _pendingStateUpdates.remove(appliedStateUpdateKey)
          _pendingLazyStateUpdates.remove(appliedStateUpdateKey)
        }
      } else {
        pendingStateUpdatesForKey.removeAll(appliedStateUpdatesForKey)
        pendingLazyStateUpdatesForKey?.removeAll(appliedStateUpdatesForKey)
      }
    }
  }

  @get:Synchronized
  val stateContainers: Map<String, StateContainer>
    get() = _stateContainers

  @get:Synchronized
  val pendingStateUpdates: Map<String, MutableList<StateUpdate>>?
    get() = _pendingStateUpdates

  @get:Synchronized
  val pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>>?
    get() = _pendingLazyStateUpdates

  @get:Synchronized
  val pendingStateUpdateTransitions: Map<String, List<Transition>>?
    get() = _pendingStateUpdateTransitions

  @get:Synchronized
  @get:VisibleForTesting
  val appliedStateUpdates: Map<String, List<StateUpdate>>?
    get() = _appliedStateUpdates

  @Synchronized
  fun getCachedValue(cachedValueInputs: Any): Any? {
    return cachedValues?.get(cachedValueInputs)
  }

  @Synchronized
  fun putCachedValue(cachedValueInputs: Any, cachedValue: Any?) {
    if (cachedValues == null) {
      cachedValues = HashMap()
    }

    cachedValues?.set(cachedValueInputs, cachedValue)
  }

  /**
   * Copies the information from the given map of state updates into the map of pending state
   * updates.
   */
  private fun copyStateUpdatesMap(
      pendingStateUpdates: Map<String, MutableList<StateUpdate>?>?,
      pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>?>?,
      appliedStateUpdates: Map<String, List<StateUpdate>>?
  ) {
    if (CollectionsUtils.isNullOrEmpty(pendingStateUpdates) &&
        CollectionsUtils.isNullOrEmpty(appliedStateUpdates)) {
      return
    }
    synchronized(this) {
      if (pendingStateUpdates != null) {
        for (key in pendingStateUpdates.keys) {
          _pendingStateUpdates[key] = createStateUpdatesList(pendingStateUpdates[key])
        }
      }
      copyPendingLazyStateUpdates(pendingLazyStateUpdates)
      if (appliedStateUpdates != null) {
        for ((key, value) in appliedStateUpdates) {
          _appliedStateUpdates[key] = createStateUpdatesList(value)
        }
      }
    }
  }

  private fun copyPendingLazyStateUpdates(
      pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>?>?
  ) {
    if (pendingLazyStateUpdates == null || pendingLazyStateUpdates.isEmpty()) {
      return
    }
    for ((key, value) in pendingLazyStateUpdates) {
      _pendingLazyStateUpdates?.set(key, createStateUpdatesList(value))
    }
  }

  /**
   * Copies the list of given state containers into the map that holds the current state containers
   * of components.
   */
  private fun copyCurrentStateContainers(stateContainers: Map<String, StateContainer>?) {
    if (stateContainers == null) {
      return
    }

    synchronized(this) {
      _stateContainers.clear()
      _stateContainers.putAll(stateContainers)
    }
  }

  private fun copyPendingStateTransitions(
      pendingStateUpdateTransitions: Map<String, List<Transition>>?
  ) {
    if (pendingStateUpdateTransitions.isNullOrEmpty()) {
      return
    }

    synchronized(this) { _pendingStateUpdateTransitions.putAll(pendingStateUpdateTransitions) }
  }

  //
  // Hooks - Experimental - see KState.kt
  //

  /**
   * Registers the given block to be run before the next layout calculation to update hook state.
   */
  @Synchronized
  fun queueHookStateUpdate(key: String, updater: HookUpdater) {
    var hookUpdaters = pendingHookUpdates[key]
    if (hookUpdaters == null) {
      hookUpdaters = ArrayList()
      pendingHookUpdates[key] = hookUpdaters
    }

    hookUpdaters.add(updater)
  }

  @get:VisibleForTesting
  val pendingHookUpdatesCount: Int
    get() = pendingHookUpdates.asSequence().sumOf { it.value.size }

  init {
    if (stateHandler == null) {
      initialStateContainer = InitialStateContainer()
    } else {
      synchronized(this) {
        initialStateContainer = stateHandler.initialStateContainer
        copyStateUpdatesMap(
            stateHandler.pendingStateUpdates,
            stateHandler.pendingLazyStateUpdates,
            stateHandler.appliedStateUpdates)
        copyCurrentStateContainers(stateHandler.stateContainers)
        copyPendingStateTransitions(stateHandler.pendingStateUpdateTransitions)
        runHooks(stateHandler)
      }
    }
  }

  /**
   * Called when creating a new StateHandler for a layout calculation. It copies the source of truth
   * state, and then the current list of HookUpdater blocks that need to be applied. Unlike normal
   * state, these blocks are run immediately to update this StateHandlers hook state before we start
   * creating components.
   *
   * @param other the ComponentTree's source-of-truth StateHandler where pending state updates are
   *   collected
   */
  private fun runHooks(other: StateHandler) {
    val updates = getHookUpdatesCopy(other.pendingHookUpdates)
    for ((key, value) in updates) {
      val stateContainer = _stateContainers[key]
      /* currentState could be null if the state is removed from the StateHandler before the update runs */
      if (stateContainer is KStateContainer) {
        var kStateContainer: KStateContainer = stateContainer

        for (hookUpdate in value) {
          kStateContainer = hookUpdate.getUpdatedStateContainer(kStateContainer)
        }
        _stateContainers[key] = kStateContainer
      }
    }
    appliedHookUpdates = updates
  }

  /**
   * Gets a state container with all applied updates for the given key without committing the
   * updates to a state handler.
   */
  fun getStateContainerWithHookUpdates(globalKey: String): KStateContainer? {
    val stateContainer = synchronized(this) { _stateContainers[globalKey] } ?: return null

    val hookUpdaters =
        synchronized(this) { pendingHookUpdates?.get(globalKey)?.let { ArrayList(it) } }

    if (hookUpdaters == null) {
      return stateContainer as KStateContainer
    }

    var stateContainerWithUpdatesApplied = stateContainer as KStateContainer
    for (hookUpdater in hookUpdaters) {
      stateContainerWithUpdatesApplied =
          hookUpdater.getUpdatedStateContainer(stateContainerWithUpdatesApplied)
    }

    return stateContainerWithUpdatesApplied
  }

  /**
   * Called on the ComponentTree's source-of-truth StateHandler when a layout has completed and new
   * state needs to be committed. In this case, we want to remove any pending state updates that
   * this StateHandler applied, while leaving new ones that have accumulated in the interim. We also
   * copy over the new mapping from hook state keys to values.
   */
  private fun commitHookState(appliedHookUpdates: Map<String, List<HookUpdater>>?) {
    if (appliedHookUpdates.isNullOrEmpty() || pendingHookUpdates.isEmpty()) {
      return
    }

    for ((globalKey, appliedHookUpdatersForKey) in appliedHookUpdates) {
      val pendingHookUpdatersForKey: MutableList<HookUpdater>?
      synchronized(this) {
        pendingHookUpdatersForKey = pendingHookUpdates[globalKey]
        if (!pendingHookUpdatersForKey.isNullOrEmpty()) {
          pendingHookUpdatersForKey.removeAll(appliedHookUpdatersForKey)
          if (pendingHookUpdatersForKey.isEmpty()) {
            pendingHookUpdates.remove(globalKey)
          }
        }
      }
    }
  }

  companion object {
    private const val INITIAL_STATE_UPDATE_LIST_CAPACITY = 4
    private const val INITIAL_MAP_CAPACITY = 4
    const val ERROR_STATE_CONTAINER_NOT_FOUND_APPLY_STATE_UPDATE_EARLY =
        "StateHandler:StateContainerNotFoundApplyStateUpdateEarly"

    private fun addStateUpdateForKey(
        key: String,
        stateUpdate: StateUpdate,
        map: MutableMap<String, MutableList<StateUpdate>>?
    ) {
      var pendingStateUpdatesForKey = map?.get(key)
      if (pendingStateUpdatesForKey == null) {
        pendingStateUpdatesForKey = createStateUpdatesList()
        map?.set(key, pendingStateUpdatesForKey)
      }

      pendingStateUpdatesForKey.add(stateUpdate)
    }

    private fun handleExceptionDuringApplyStateUpdate(
        key: String,
        current: LithoNode,
        exception: Exception
    ) {
      val scopedComponentInfos = current.scopedComponentInfos
      for (scopedComponentInfo in scopedComponentInfos) {
        val context = scopedComponentInfo.context
        if (context.globalKey == key) {
          ComponentUtils.handleWithHierarchy(context, scopedComponentInfo.component, exception)
          break
        }
      }
      for (index in 0 until current.childCount) {
        val childLithoNode = current.getChildAt(index)
        if (key.startsWith(childLithoNode.headComponentKey)) {
          handleExceptionDuringApplyStateUpdate(key, childLithoNode, exception)
        }
      }
    }

    private fun createStateUpdatesList(
        copyFrom: List<StateUpdate>? = null
    ): MutableList<StateUpdate> {
      val list: MutableList<StateUpdate> =
          ArrayList(copyFrom?.size ?: INITIAL_STATE_UPDATE_LIST_CAPACITY)
      if (copyFrom != null) {
        list.addAll(copyFrom)
      }
      return list
    }

    private fun clearUnusedStateContainers(currentStateHandler: StateHandler) {
      if (currentStateHandler._stateContainers.isEmpty()) {
        return
      }

      val neededStateContainers: Set<String> = currentStateHandler.neededStateContainers
      val stateContainerKeys: List<String> = ArrayList(currentStateHandler._stateContainers.keys)

      for (key in stateContainerKeys) {
        if (key !in neededStateContainers) {
          currentStateHandler._stateContainers.remove(key)
        }
      }
    }

    private fun getHookUpdatesCopy(
        copyFrom: Map<String, List<HookUpdater>>
    ): Map<String, List<HookUpdater>> {
      val copyInto: MutableMap<String, List<HookUpdater>> = HashMap(copyFrom.size)
      for ((key, value) in copyFrom) {
        copyInto[key] = ArrayList(value)
      }
      return copyInto
    }
  }
}
