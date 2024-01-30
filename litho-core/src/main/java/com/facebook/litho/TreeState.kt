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

import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.facebook.litho.SpecGeneratedComponent.TransitionContainer
import com.facebook.rendercore.annotations.UIState
import com.facebook.rendercore.utils.isEqualOrEquivalentTo

class TreeState {
  val resolveState: StateHandler
  val layoutState: StateHandler

  private val eventTriggersContainer: EventTriggersContainer
  @UIState private val renderState: RenderState

  @UIState val mountInfo: TreeMountInfo
  @get:VisibleForTesting val eventHandlersController: EventHandlersController

  /**
   * This class represents whether this Litho tree has been mounted before. The usage is a bit
   * convoluted and will need to be cleaned out properly in the future.
   */
  class TreeMountInfo {
    @JvmField @Volatile var hasMounted: Boolean = false
    @JvmField @Volatile var isFirstMount: Boolean = false
  }

  constructor() : this(fromState = null)

  @JvmOverloads
  constructor(fromState: TreeState?) {
    resolveState = StateHandler(fromState?.resolveState)
    layoutState = StateHandler(fromState?.layoutState)
    mountInfo = fromState?.mountInfo ?: TreeMountInfo()
    renderState = fromState?.renderState ?: RenderState()
    eventTriggersContainer = fromState?.eventTriggersContainer ?: EventTriggersContainer()
    eventHandlersController = fromState?.eventHandlersController ?: EventHandlersController()
  }

  private fun getStateHandler(isNestedTree: Boolean): StateHandler {
    return if (isNestedTree) {
      layoutState
    } else {
      resolveState
    }
  }

  fun registerResolveState() {
    resolveState.initialStateContainer.registerStateHandler(resolveState)
  }

  fun registerLayoutState() {
    layoutState.initialStateContainer.registerStateHandler(layoutState)
  }

  fun unregisterResolveInitialState() {
    resolveState.initialStateContainer.unregisterStateHandler(resolveState)
  }

  fun unregisterLayoutInitialState() {
    layoutState.initialStateContainer.unregisterStateHandler(layoutState)
  }

  fun commitResolveState(localTreeState: TreeState) {
    resolveState.commit(localTreeState.resolveState)
  }

  fun commitLayoutState(localTreeState: TreeState) {
    layoutState.commit(localTreeState.layoutState)
  }

  fun commit() {
    resolveState.commit()
    layoutState.commit()
  }

  fun queueStateUpdate(
      key: String,
      stateUpdate: StateContainer.StateUpdate,
      isLazyStateUpdate: Boolean,
      isNestedTree: Boolean
  ): Boolean = queueStateUpdate(key, stateUpdate, isLazyStateUpdate, isNestedTree, true)

  fun queueStateUpdate(
      key: String,
      stateUpdate: StateContainer.StateUpdate,
      isLazyStateUpdate: Boolean,
      isNestedTree: Boolean,
      queueDuplicateStateUpdates: Boolean = true,
  ): Boolean {
    val stateHandler = getStateHandler(isNestedTree)
    val stateContainer = stateHandler.getStateContainer(key)
    return if (queueDuplicateStateUpdates ||
        isLazyStateUpdate ||
        stateContainer == null ||
        stateContainer is TransitionContainer) {
      stateHandler.queueStateUpdate(key, stateUpdate, isLazyStateUpdate)
      true
    } else {
      /**
       * Ideally we would apply the state update only once if it is not a duplicate. We should
       * improve this further if this experiment is successful.
       */
      val containerClone = stateContainer.clone()
      containerClone.applyStateUpdate(stateUpdate)

      val isDuplicate = isEqualOrEquivalentTo(stateContainer, containerClone)
      if (isDuplicate) {
        false
      } else {
        stateHandler.queueStateUpdate(key, stateUpdate, false)
        true
      }
    }
  }

  fun queueHookStateUpdate(key: String, updater: HookUpdater, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).queueHookStateUpdate(key, updater)
  }

  fun applyLazyStateUpdatesForContainer(
      componentKey: String,
      container: StateContainer,
      isNestedTree: Boolean
  ): StateContainer {
    return getStateHandler(isNestedTree).applyLazyStateUpdatesForContainer(componentKey, container)
  }

  fun hasUncommittedUpdates(): Boolean {
    return resolveState.hasUncommittedUpdates() || layoutState.hasUncommittedUpdates()
  }

  val isEmpty: Boolean
    get() = resolveState.isEmpty && layoutState.isEmpty

  fun applyStateUpdatesEarly(
      context: ComponentContext,
      component: Component?,
      prevTreeRootNode: LithoNode?,
      isNestedTree: Boolean
  ) {
    getStateHandler(isNestedTree).applyStateUpdatesEarly(context, component, prevTreeRootNode)
  }

  val keysForPendingResolveStateUpdates: Set<String>
    get() = getKeysForPendingStateUpdates(resolveState)

  val keysForPendingLayoutStateUpdates: Set<String>
    get() = getKeysForPendingStateUpdates(layoutState)

  val keysForPendingStateUpdates: Set<String>
    get() {
      return HashSet<String>().apply {
        addAll(getKeysForPendingStateUpdates(resolveState))
        addAll(getKeysForPendingStateUpdates(layoutState))
      }
    }

  val keysForAppliedStateUpdates: Set<String>
    get() {
      return HashSet<String>().apply {
        addAll(resolveState.keysForAppliedUpdates)
        addAll(layoutState.keysForAppliedUpdates)
      }
    }

  fun addStateContainer(key: String, stateContainer: StateContainer, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).addStateContainer(key, stateContainer)
  }

  fun keepStateContainerForGlobalKey(key: String, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).keepStateContainerForGlobalKey(key)
  }

  fun getStateContainer(key: String, isNestedTree: Boolean): StateContainer? {
    return getStateHandler(isNestedTree).getStateContainer(key)
  }

  fun createOrGetStateContainerForComponent(
      scopedContext: ComponentContext,
      component: Component,
      key: String
  ): StateContainer {
    return getStateHandler(scopedContext.isNestedTreeContext)
        .createOrGetStateContainerForComponent(
            scopedContext,
            component,
            key,
        )
  }

  fun removePendingStateUpdate(key: String, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).removePendingStateUpdate(key)
  }

  fun <T> canSkipStateUpdate(
      globalKey: String,
      hookStateIndex: Int,
      newValue: T,
      isNestedTree: Boolean
  ): Boolean {
    return canSkipStateUpdate<T>(
        updater = { newValue },
        globalKey = globalKey,
        hookStateIndex = hookStateIndex,
        isNestedTree = isNestedTree,
    )
  }

  fun <T> canSkipStateUpdate(
      updater: (T) -> T,
      globalKey: String,
      hookStateIndex: Int,
      isNestedTree: Boolean
  ): Boolean {
    val stateHandler = getStateHandler(isNestedTree)
    val committedState = stateHandler.getStateContainer(globalKey) as KStateContainer?
    if (committedState != null && committedState.states[hookStateIndex] != null) {
      val committedStateWithUpdatesApplied =
          stateHandler.getStateContainerWithHookUpdates(globalKey)
      if (committedStateWithUpdatesApplied != null) {
        val committedUpdatedValue: T = committedStateWithUpdatesApplied.states[hookStateIndex] as T
        val newValueAfterUpdate = updater.invoke(committedUpdatedValue)
        return if (committedUpdatedValue == null && newValueAfterUpdate == null) {
          true
        } else {
          committedUpdatedValue != null && committedUpdatedValue == newValueAfterUpdate
        }
      }
    }
    return false
  }

  val pendingStateUpdateTransitions: List<Transition>
    get() {
      val updateStateTransitions: MutableList<Transition> = ArrayList()

      for (pendingTransitions in resolveState.pendingStateUpdateTransitions.values) {
        updateStateTransitions.addAll(pendingTransitions)
      }

      for (pendingTransitions in layoutState.pendingStateUpdateTransitions.values) {
        updateStateTransitions.addAll(pendingTransitions)
      }
      return updateStateTransitions
    }

  fun putCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      cachedValue: Any?,
      isNestedTree: Boolean
  ) {
    getStateHandler(isNestedTree).putCachedValue(globalKey, index, cachedValueInputs, cachedValue)
  }

  fun getCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      isNestedTree: Boolean
  ): Any? {
    return getStateHandler(isNestedTree).getCachedValue(globalKey, index, cachedValueInputs)
  }

  fun <T> createOrGetInitialHookState(
      key: String,
      hookStateIndex: Int,
      initializer: HookInitializer<T>,
      isNestedTree: Boolean,
      componentName: String
  ): KStateContainer {

    return getStateHandler(isNestedTree)
        .initialStateContainer
        .createOrGetInitialHookState(
            key,
            hookStateIndex,
            initializer,
            componentName,
        )
  }

  fun applyPreviousRenderData(componentScopes: List<ScopedComponentInfo>?) {
    if (CollectionsUtils.isNullOrEmpty(componentScopes)) {
      return
    }
    renderState.applyPreviousRenderData(componentScopes)
  }

  fun applyPreviousRenderData(layoutState: LayoutState) {
    applyPreviousRenderData(layoutState.scopedComponentInfosNeedingPreviousRenderData)
  }

  fun recordRenderData(layoutState: LayoutState) {
    val componentScopes = layoutState.scopedComponentInfosNeedingPreviousRenderData
    if (CollectionsUtils.isNullOrEmpty(componentScopes)) {
      return
    }
    renderState.recordRenderData(componentScopes)
  }

  fun getEventTrigger(triggerKey: String): EventTrigger<*>? {
    synchronized(eventTriggersContainer) {
      return eventTriggersContainer.getEventTrigger(triggerKey)
    }
  }

  fun getEventTrigger(handle: Handle, methodId: Int): EventTrigger<*>? {
    synchronized(eventTriggersContainer) {
      return eventTriggersContainer.getEventTrigger(handle, methodId)
    }
  }

  fun clearUnusedTriggerHandlers() {
    synchronized(eventTriggersContainer) { eventTriggersContainer.clear() }
  }

  fun bindEventAndTriggerHandlers(
      createdEventHandlers: List<Pair<String, EventHandler<*>>>?,
      componentScopes: List<ScopedComponentInfo>?
  ) {
    synchronized(eventTriggersContainer) {
      clearUnusedTriggerHandlers()
      if (createdEventHandlers != null) {
        eventHandlersController.canonicalizeEventDispatchInfos(createdEventHandlers)
      }
      if (componentScopes != null) {
        for (componentScope in componentScopes) {
          val component = componentScope.component as SpecGeneratedComponent
          val context = componentScope.context
          eventHandlersController.updateEventDispatchInfoForGlobalKey(
              context,
              component,
              context.globalKey,
          )
          component.recordEventTrigger(context, eventTriggersContainer)
        }
      }
    }
    eventHandlersController.clearUnusedEventDispatchInfos()
  }

  companion object {
    private fun getKeysForPendingStateUpdates(stateHandler: StateHandler): Set<String> {
      return stateHandler.keysForPendingUpdates
    }
  }
}
