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
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.SpecGeneratedComponent.TransitionContainer
import com.facebook.litho.StateContainer.StateUpdate
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.state.ComponentState
import com.facebook.litho.stats.LithoStats
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute.Description
import com.facebook.rendercore.debug.DebugEventAttribute.Name
import com.facebook.rendercore.debug.DebugEventAttribute.Source
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.transitions.TransitionUtils
import com.facebook.rendercore.utils.areObjectsEquivalent
import javax.annotation.concurrent.GuardedBy

/** Holds information about the current State of the components in a Component Tree. */
class StateHandler {

  constructor(initialState: InitialState, stateHandler: StateHandler? = null) {
    this.initialState = initialState
    if (stateHandler != null) {
      copyStateUpdatesMap(
          stateHandler.pendingStateUpdates,
          stateHandler.pendingHookUpdates,
          stateHandler.pendingLazyStateUpdates,
          stateHandler.appliedStateUpdates,
          stateHandler.appliedHookUpdates,
      )
      copyCurrentStateContainers(stateHandler.stateContainers)
      copyPendingStateTransitions(stateHandler.pendingStateUpdateTransitions)
      stateHandler.cachedValues?.let { cachedValues = HashMap(it) }
    }
  }

  constructor(
      stateHandler: StateHandler? = null
  ) : this(
      initialState = stateHandler?.initialState ?: InitialState(),
      stateHandler = stateHandler,
  )

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
  private val _pendingStateUpdateTransitions: MutableMap<String, List<Transition>> = HashMap()

  /** List of transitions from state update that have been applied on next mount. */
  @GuardedBy("this")
  private val _appliedStateUpdates: MutableMap<String, List<StateUpdate>> =
      HashMap(INITIAL_MAP_CAPACITY)

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   */
  @GuardedBy("this")
  private val _stateContainers: MutableMap<String, ComponentState<out StateContainer>> = HashMap()

  /**
   * Contains all keys of components that were present in the current ComponentTree and therefore
   * their StateContainer needs to be kept around.
   */
  @GuardedBy("this") private val neededStateContainers = HashSet<String>()

  /** Map of all cached values that are stored for the current ComponentTree. */
  @GuardedBy("this") private var cachedValues: MutableMap<CacheKey, CacheValue>? = null

  // These are both lists of (globalKey, updateMethod) pairs, where globalKey is the global key
  // of the component the update applies to
  @GuardedBy("this")
  private val pendingHookUpdates: MutableMap<String, MutableList<HookUpdater>> = HashMap()
  private var appliedHookUpdates: MutableMap<String, List<HookUpdater>> = HashMap()

  var initialState: InitialState
    private set

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

  fun markStateInUse(key: String) {
    neededStateContainers.add(key)
  }

  /**
   * StateContainer in this StateHandler should be accessed using this method as it will also ensure
   * that the state is marked as needed
   */
  fun getState(key: String): ComponentState<out StateContainer>? = _stateContainers[key]

  fun createOrGetComponentState(
      scopedContext: ComponentContext,
      component: Component,
      key: String
  ): ComponentState<out StateContainer> {
    val current: ComponentState<out StateContainer>? = synchronized(this) { _stateContainers[key] }

    return if (current != null) {
      neededStateContainers.add(key)
      current
    } else {
      val state =
          initialState.createOrGetComponentState(
              component,
              scopedContext,
              key,
          )
      this.addState(key, state)
      state
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
        val current: ComponentState<StateContainer> =
            (_stateContainers[key] ?: initialState.getInitialStateForComponent(key))
                as ComponentState<StateContainer>? ?: continue

        val currentValue: StateContainer = current.value

        val newValue = currentValue.clone()
        _stateContainers[key] = current.copy(value = newValue)
        applyStateUpdates(key, newValue)
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

    runHooks()
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
  fun addState(key: String, state: ComponentState<*>) {
    neededStateContainers.add(key)
    _stateContainers[key] = state
  }

  fun applyLazyStateUpdatesForContainer(
      componentKey: String,
      container: StateContainer
  ): StateContainer {
    val stateUpdatesForKey = synchronized(this) { pendingLazyStateUpdates[componentKey] }

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
  }

  fun commit() {
    synchronized(this) {
      clearStateUpdates(appliedStateUpdates)
      clearUnusedStateContainers(this)
      commitHookState(appliedHookUpdates)
      _appliedStateUpdates.clear()
      appliedHookUpdates.clear()
    }
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

  @get:Synchronized
  val keysForAppliedUpdates: Set<String>
    get() =
        HashSet<String>().apply {
          addAll(_appliedStateUpdates.keys)
          addAll(appliedHookUpdates.keys)
        }

  private fun clearStateUpdates(appliedStateUpdates: Map<String, List<StateUpdate>>) {
    synchronized(this) {
      if (_pendingStateUpdates.isEmpty()) {
        return
      }
    }

    for ((appliedStateUpdateKey, appliedStateUpdatesForKey) in appliedStateUpdates) {
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
  val stateContainers: Map<String, ComponentState<out StateContainer>>
    get() = _stateContainers

  @get:Synchronized
  val pendingStateUpdates: Map<String, MutableList<StateUpdate>>
    get() = _pendingStateUpdates

  @get:Synchronized
  val pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>>
    get() = _pendingLazyStateUpdates

  @get:Synchronized
  val pendingStateUpdateTransitions: Map<String, List<Transition>>
    get() = _pendingStateUpdateTransitions

  @get:Synchronized
  @get:VisibleForTesting
  val appliedStateUpdates: Map<String, List<StateUpdate>>
    get() = _appliedStateUpdates

  @Synchronized
  fun getCachedValue(globalKey: String, index: Int, cachedValueInputs: Any): Any? {
    val cacheKey = CacheKey(globalKey, index)
    val cacheValue = cachedValues?.get(cacheKey) ?: return null

    if (areObjectsEquivalent(cacheValue.inputs, cachedValueInputs)) {
      if (cacheValue.inputsHash != cachedValueInputs.hashCode()) {
        // Log mutable input detection
        DebugEventDispatcher.dispatch(LithoDebugEvent.DebugInfo, DebugEvent.NoId, LogLevel.ERROR) {
            attribute ->
          attribute[Name] = "StateHandler:MutableTypeUsedAsCachedValueDep"
          attribute[Description] = "Unexpected mutable value used as CachedValue dep"
          attribute[Source] = cachedValueInputs.javaClass.name
        }
        return null
      }
      return cacheValue.value
    }
    return null
  }

  @Synchronized
  fun putCachedValue(globalKey: String, index: Int, cachedValueInputs: Any, cachedValue: Any?) {
    val cacheKey = CacheKey(globalKey, index)
    if (cachedValue == null) {
      cachedValues?.remove(cacheKey)
      return
    }
    if (cachedValues == null) {
      cachedValues = LinkedHashMap()
    }
    val value = CacheValue(cachedValueInputs, cachedValueInputs.hashCode(), cachedValue)
    cachedValues?.put(cacheKey, value)
  }

  /**
   * Copies the information from the given map of state updates into the map of pending state
   * updates.
   */
  private fun copyStateUpdatesMap(
      pendingStateUpdates: Map<String, MutableList<StateUpdate>>,
      pendingHookStateUpdates: Map<String, MutableList<HookUpdater>>,
      pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>>,
      appliedStateUpdates: Map<String, List<StateUpdate>>,
      appliedHookStateUpdates: Map<String, List<HookUpdater>>,
  ) {
    if (pendingStateUpdates.isEmpty() &&
        appliedStateUpdates.isEmpty() &&
        pendingHookStateUpdates.isEmpty() &&
        appliedHookStateUpdates.isEmpty()) {
      return
    }
    synchronized(this) {
      for (key in pendingStateUpdates.keys) {
        _pendingStateUpdates[key] = createStateUpdatesList(pendingStateUpdates[key])
      }

      for ((key, value) in appliedStateUpdates) {
        _appliedStateUpdates[key] = createStateUpdatesList(value)
      }

      for ((key, value) in pendingHookStateUpdates) {
        pendingHookUpdates[key] = createHookStateUpdatesList(value)
      }

      for ((key, value) in appliedHookStateUpdates) {
        appliedHookUpdates[key] = createHookStateUpdatesList(value)
      }

      copyPendingLazyStateUpdates(pendingLazyStateUpdates)
    }
  }

  private fun copyPendingLazyStateUpdates(
      pendingLazyStateUpdates: Map<String, MutableList<StateUpdate>>
  ) {
    if (pendingLazyStateUpdates.isEmpty()) {
      return
    }
    for ((key, value) in pendingLazyStateUpdates) {
      _pendingLazyStateUpdates[key] = createStateUpdatesList(value)
    }
  }

  /**
   * Copies the list of given state containers into the map that holds the current state containers
   * of components.
   */
  private fun copyCurrentStateContainers(
      stateContainers: Map<String, ComponentState<out StateContainer>>
  ) {

    synchronized(this) {
      _stateContainers.clear()
      _stateContainers.putAll(stateContainers)
    }
  }

  private fun copyPendingStateTransitions(
      pendingStateUpdateTransitions: Map<String, List<Transition>>
  ) {
    if (pendingStateUpdateTransitions.isEmpty()) {
      return
    }

    synchronized(this) { _pendingStateUpdateTransitions.putAll(pendingStateUpdateTransitions) }
  }

  //
  // Hooks - see KState.kt
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

  /**
   * Called when creating a new StateHandler for a layout calculation. It copies the source of truth
   * state, and then the current list of HookUpdater blocks that need to be applied. Unlike normal
   * state, these blocks are run immediately to update this StateHandlers hook state before we start
   * creating components.
   *
   * @param other the ComponentTree's source-of-truth StateHandler where pending state updates are
   *   collected
   */
  private fun runHooks() {
    for ((key, value) in pendingHookUpdates) {
      val current = _stateContainers[key] as ComponentState<KStateContainer>?
      val stateContainer = current?.value
      /* currentState could be null if the state is removed from the StateHandler before the update runs */
      if (stateContainer is KStateContainer) {
        var kStateContainer: KStateContainer = stateContainer

        for (hookUpdate in value) {
          kStateContainer = hookUpdate.getUpdatedStateContainer(kStateContainer)
        }

        _stateContainers[key] = current.copy(value = kStateContainer)
      }
    }
    appliedHookUpdates.putAll(pendingHookUpdates)
    pendingHookUpdates.clear()
  }

  /**
   * Gets a state container with all applied updates for the given key without committing the
   * updates to a state handler.
   */
  fun getStateContainerWithHookUpdates(globalKey: String): KStateContainer? {
    val stateContainer: StateContainer?
    val updaters: List<HookUpdater>?
    synchronized(this) {
      stateContainer = _stateContainers[globalKey]?.value ?: return null
      updaters = pendingHookUpdates[globalKey]?.let { ArrayList(it) }
    }

    if (updaters == null) {
      return stateContainer as KStateContainer
    }

    var updatedState = stateContainer as KStateContainer
    for (updater in updaters) {
      updatedState = updater.getUpdatedStateContainer(updatedState)
    }

    return updatedState
  }

  /**
   * Called on the ComponentTree's source-of-truth StateHandler when a layout has completed and new
   * state needs to be committed. In this case, we want to remove any pending state updates that
   * this StateHandler applied, while leaving new ones that have accumulated in the interim. We also
   * copy over the new mapping from hook state keys to values.
   */
  private fun commitHookState(appliedHookUpdates: Map<String, List<HookUpdater>>) {
    if (appliedHookUpdates.isEmpty() || pendingHookUpdates.isEmpty()) {
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

    private fun addStateUpdateForKey(
        key: String,
        stateUpdate: StateUpdate,
        map: MutableMap<String, MutableList<StateUpdate>>
    ) {
      var pendingStateUpdatesForKey = map[key]
      if (pendingStateUpdatesForKey == null) {
        pendingStateUpdatesForKey = createStateUpdatesList()
        map[key] = pendingStateUpdatesForKey
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

    private fun createHookStateUpdatesList(
        copyFrom: List<HookUpdater>? = null
    ): MutableList<HookUpdater> {
      val list: MutableList<HookUpdater> =
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

sealed interface StateUpdateApplier

@DataClassGenerate private data class CacheKey(val globalKey: String, val index: Int)

private class CacheValue(val inputs: Any, val inputsHash: Int, val value: Any)
