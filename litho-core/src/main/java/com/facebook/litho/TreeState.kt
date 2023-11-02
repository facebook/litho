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
import androidx.arch.core.util.Function
import com.facebook.rendercore.annotations.UIState

class TreeState {
  val resolveState: StateHandler
  val layoutState: StateHandler

  @UIState private val renderState: RenderState
  private val eventTriggersContainer: EventTriggersContainer

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

  constructor() {
    resolveState = StateHandler()
    layoutState = StateHandler()
    mountInfo = TreeMountInfo()
    renderState = RenderState()
    eventTriggersContainer = EventTriggersContainer()
    eventHandlersController = EventHandlersController()
  }

  constructor(treeState: TreeState) {
    resolveState = StateHandler(treeState.resolveState)
    layoutState = StateHandler(treeState.layoutState)
    mountInfo = treeState.mountInfo
    renderState = treeState.renderState
    eventTriggersContainer = treeState.eventTriggersContainer
    eventHandlersController = treeState.eventHandlersController
  }

  private fun getStateHandler(isNestedTree: Boolean): StateHandler {
    return if (isNestedTree) layoutState else resolveState
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

  fun queueStateUpdate(
      key: String,
      stateUpdate: StateContainer.StateUpdate,
      isLazyStateUpdate: Boolean,
      isNestedTree: Boolean
  ) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.queueStateUpdate(key, stateUpdate, isLazyStateUpdate)
  }

  fun queueHookStateUpdate(key: String, updater: HookUpdater, isNestedTree: Boolean) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.queueHookStateUpdate(key, updater)
  }

  fun applyLazyStateUpdatesForContainer(
      componentKey: String,
      container: StateContainer,
      isNestedTree: Boolean
  ): StateContainer {
    val stateHandler = getStateHandler(isNestedTree)
    return stateHandler.applyLazyStateUpdatesForContainer(componentKey, container)
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
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.applyStateUpdatesEarly(context, component, prevTreeRootNode)
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

  fun addStateContainer(key: String, stateContainer: StateContainer, isNestedTree: Boolean) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.addStateContainer(key, stateContainer)
  }

  fun keepStateContainerForGlobalKey(key: String, isNestedTree: Boolean) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.keepStateContainerForGlobalKey(key)
  }

  fun getStateContainer(key: String, isNestedTree: Boolean): StateContainer? {
    val stateHandler = getStateHandler(isNestedTree)
    return stateHandler.getStateContainer(key)
  }

  fun createOrGetStateContainerForComponent(
      scopedContext: ComponentContext,
      component: Component,
      key: String
  ): StateContainer {
    val stateHandler = getStateHandler(scopedContext.isNestedTreeContext)
    return stateHandler.createOrGetStateContainerForComponent(scopedContext, component, key)
  }

  fun removePendingStateUpdate(key: String, isNestedTree: Boolean) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.removePendingStateUpdate(key)
  }

  fun <T> canSkipStateUpdate(
      globalKey: String,
      hookStateIndex: Int,
      newValue: T?,
      isNestedTree: Boolean
  ): Boolean {
    val stateHandler = getStateHandler(isNestedTree)
    val committedStateContainer = stateHandler.getStateContainer(globalKey) as KStateContainer?
    if (committedStateContainer != null && committedStateContainer.states[hookStateIndex] != null) {
      val committedStateContainerWithAppliedPendingHooks =
          stateHandler.getStateContainerWithHookUpdates(globalKey)
      if (committedStateContainerWithAppliedPendingHooks != null) {
        val committedUpdatedValue: T? =
            committedStateContainerWithAppliedPendingHooks.states[hookStateIndex] as T
        return if (committedUpdatedValue == null && newValue == null) {
          true
        } else committedUpdatedValue != null && committedUpdatedValue == newValue
      }
    }
    return false
  }

  fun <T> canSkipStateUpdate(
      newValueFunction: Function<T?, T>,
      globalKey: String,
      hookStateIndex: Int,
      isNestedTree: Boolean
  ): Boolean {
    val stateHandler = getStateHandler(isNestedTree)
    val committedStateContainer = stateHandler.getStateContainer(globalKey) as KStateContainer?
    if (committedStateContainer != null && committedStateContainer.states[hookStateIndex] != null) {
      val committedStateContainerWithAppliedPendingHooks =
          stateHandler.getStateContainerWithHookUpdates(globalKey)
      if (committedStateContainerWithAppliedPendingHooks != null) {
        val committedUpdatedValue: T? =
            committedStateContainerWithAppliedPendingHooks.states[hookStateIndex] as T
        val newValueAfterPendingUpdate = newValueFunction.apply(committedUpdatedValue)
        return if (committedUpdatedValue == null && newValueAfterPendingUpdate == null) {
          true
        } else committedUpdatedValue != null && committedUpdatedValue == newValueAfterPendingUpdate
      }
    }
    return false
  }

  val pendingStateUpdateTransitions: List<Transition?>?
    get() {
      var updateStateTransitions: MutableList<Transition>? = null

      val pendingResolveStateUpdateTransitions: Map<String, List<Transition>>? =
          resolveState.pendingStateUpdateTransitions
      if (pendingResolveStateUpdateTransitions != null) {
        updateStateTransitions = ArrayList()
        for (pendingTransitions in pendingResolveStateUpdateTransitions.values) {
          updateStateTransitions.addAll(pendingTransitions)
        }
      }

      val pendingLayoutStateUpdateTransitions: Map<String, List<Transition>>? =
          layoutState.pendingStateUpdateTransitions
      if (pendingLayoutStateUpdateTransitions != null) {
        if (updateStateTransitions == null) {
          updateStateTransitions = ArrayList()
        }
        for (pendingTransitions in pendingLayoutStateUpdateTransitions.values) {
          updateStateTransitions.addAll(pendingTransitions)
        }
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
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.putCachedValue(globalKey, index, cachedValueInputs, cachedValue)
  }

  fun getCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      isNestedTree: Boolean
  ): Any? {
    val stateHandler = getStateHandler(isNestedTree)
    return stateHandler.getCachedValue(globalKey, index, cachedValueInputs)
  }

  fun <T> createOrGetInitialHookState(
      key: String,
      hookStateIndex: Int,
      initializer: HookInitializer<T>,
      isNestedTree: Boolean,
      componentName: String
  ): KStateContainer {
    val stateHandler = getStateHandler(isNestedTree)
    return stateHandler.initialStateContainer.createOrGetInitialHookState(
        key, hookStateIndex, initializer, componentName)
  }

  fun applyPreviousRenderData(scopedComponentInfos: List<ScopedComponentInfo>?) {
    if (CollectionsUtils.isNullOrEmpty(scopedComponentInfos)) {
      return
    }
    if (renderState == null) {
      return
    }
    renderState.applyPreviousRenderData(scopedComponentInfos)
  }

  fun applyPreviousRenderData(layoutState: LayoutState) {
    val scopedComponentInfos = layoutState.scopedComponentInfosNeedingPreviousRenderData
    applyPreviousRenderData(scopedComponentInfos)
  }

  fun recordRenderData(layoutState: LayoutState) {
    val scopedComponentInfos = layoutState.scopedComponentInfosNeedingPreviousRenderData
    if (CollectionsUtils.isNullOrEmpty(scopedComponentInfos)) {
      return
    }
    renderState?.recordRenderData(scopedComponentInfos)
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
      scopedSpecComponentInfos: List<ScopedComponentInfo>?
  ) {
    synchronized(eventTriggersContainer) {
      clearUnusedTriggerHandlers()
      if (createdEventHandlers != null) {
        eventHandlersController.canonicalizeEventDispatchInfos(createdEventHandlers)
      }
      if (scopedSpecComponentInfos != null) {
        for (scopedSpecComponentInfo in scopedSpecComponentInfos) {
          val component = scopedSpecComponentInfo.component as SpecGeneratedComponent
          val scopedContext = scopedSpecComponentInfo.context
          eventHandlersController.updateEventDispatchInfoForGlobalKey(
              scopedContext, component, scopedContext.globalKey)
          component.recordEventTrigger(scopedContext, eventTriggersContainer)
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
