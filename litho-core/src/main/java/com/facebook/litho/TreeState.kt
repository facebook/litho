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
import com.facebook.litho.Component.RenderData
import com.facebook.litho.internal.HookKey
import com.facebook.litho.state.ComponentState
import com.facebook.rendercore.annotations.UIState

class TreeState {
  val resolveState: StateHandler
  val layoutState: StateHandler
  val effectsHandler: AttachDetachHandler

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

  private constructor(
      resolveState: StateHandler,
      layoutState: StateHandler,
      mountInfo: TreeMountInfo,
      renderState: RenderState,
      effectsHandler: AttachDetachHandler,
      eventTriggersContainer: EventTriggersContainer,
      eventHandlersController: EventHandlersController,
  ) {

    if (resolveState.initialState === layoutState.initialState) {
      throw IllegalArgumentException(
          "The same InitialState cannot be used for both resolve and layout states")
    }

    this.resolveState = resolveState
    this.layoutState = layoutState
    this.mountInfo = mountInfo
    this.renderState = renderState
    this.effectsHandler = effectsHandler
    this.eventTriggersContainer = eventTriggersContainer
    this.eventHandlersController = eventHandlersController
  }

  constructor(
      fromState: TreeState?
  ) : this(
      resolveState = StateHandler(fromState?.resolveState),
      layoutState = StateHandler(fromState?.layoutState),
      mountInfo = fromState?.mountInfo ?: TreeMountInfo(),
      renderState = RenderState(fromState?.renderState),
      effectsHandler = fromState?.effectsHandler ?: AttachDetachHandler(),
      eventTriggersContainer = fromState?.eventTriggersContainer ?: EventTriggersContainer(),
      eventHandlersController = fromState?.eventHandlersController ?: EventHandlersController(),
  )

  constructor(
      initialResolveState: InitialState,
      initialLayoutState: InitialState,
  ) : this(
      resolveState = StateHandler(initialResolveState),
      layoutState = StateHandler(initialLayoutState),
      mountInfo = TreeMountInfo(),
      renderState = RenderState(),
      effectsHandler = AttachDetachHandler(),
      eventTriggersContainer = EventTriggersContainer(),
      eventHandlersController = EventHandlersController(),
  )

  constructor() : this(fromState = null)

  private fun getStateHandler(isNestedTree: Boolean): StateHandler {
    return if (isNestedTree) {
      layoutState
    } else {
      resolveState
    }
  }

  fun registerResolveState() {
    resolveState.initialState.registerStateHandler(resolveState)
  }

  fun registerLayoutState() {
    layoutState.initialState.registerStateHandler(layoutState)
  }

  fun unregisterResolveInitialState() {
    resolveState.initialState.unregisterStateHandler(resolveState)
  }

  fun unregisterLayoutInitialState() {
    layoutState.initialState.unregisterStateHandler(layoutState)
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
  ) {
    val stateHandler = getStateHandler(isNestedTree)
    stateHandler.queueStateUpdate(key, stateUpdate, isLazyStateUpdate)
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

  fun addState(key: String, state: ComponentState<*>, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).addState(key, state)
  }

  fun markStateInUse(key: String, isNestedTree: Boolean) {
    getStateHandler(isNestedTree).markStateInUse(key)
  }

  fun getState(key: String, isNestedTree: Boolean): ComponentState<out StateContainer>? {
    return getStateHandler(isNestedTree).getState(key)
  }

  fun createOrGetState(
      scopedContext: ComponentContext,
      component: Component,
      key: String
  ): ComponentState<out StateContainer> {
    return getStateHandler(scopedContext.isNestedTreeContext)
        .createOrGetState(scopedContext, component, key)
  }

  fun createOrGetEventDispatchInfoForComponent(
      scopedContext: ComponentContext,
      component: Component,
      key: String,
  ): EventDispatchInfo {
    return checkNotNull(
        getStateHandler(scopedContext.isNestedTreeContext)
            .createOrGetState(scopedContext, component, key)
            .eventDispatchInfo)
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
    val committedState = stateHandler.getState(globalKey)
    if (committedState != null) {
      val committedStateWithUpdatesApplied = stateHandler.getKStateWithUpdates(globalKey)
      if (committedStateWithUpdatesApplied != null) {
        val committedUpdatedValue: T =
            committedStateWithUpdatesApplied.states.getOrNull(hookStateIndex) as T
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

  @Suppress("UNCHECKED_CAST")
  internal fun <T> getHookStateValue(
      globalKey: String,
      hookStateIndex: Int,
      isNestedTree: Boolean
  ): T {
    val stateContainer = getState(globalKey, isNestedTree)?.value as KStateContainer
    return stateContainer.states[hookStateIndex] as T
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
      kStateIndex: Int,
      initializer: HookInitializer<T>,
      isNestedTree: Boolean,
      componentName: String
  ): ComponentState<KStateContainer> {

    return getStateHandler(isNestedTree)
        .initialState
        .getOrCreateInitialKState(
            key,
            kStateIndex,
            initializer,
            componentName,
        )
  }

  internal fun getPreviousLayoutStateId(): Int = renderState.getPreviousLayoutStateId()

  internal fun getPreviousRenderData(hookKey: HookKey): RenderData? {
    return renderState.getPreviousRenderData(hookKey)
  }

  fun recordRenderData(layoutState: LayoutState) {
    renderState.recordRenderData(layoutState)
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

  fun clearEventHandlersAndTriggers() {
    synchronized(eventTriggersContainer) { eventTriggersContainer.clear() }
    eventHandlersController.clear()
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

  fun updateEventDispatchers(toUpdate: List<ScopedComponentInfo>) {
    toUpdate.forEach { scope ->
      val context = scope.context
      val key = context.globalKey
      val component = context.componentScope

      if (component is SpecGeneratedComponent) {
        component.recordEventTrigger(context, eventTriggersContainer)
      }

      val info =
          resolveState.getState(key)?.eventDispatchInfo
              ?: layoutState.getState(key)?.eventDispatchInfo
              ?: return@forEach

      info.componentContext = context
      info.hasEventDispatcher = context.componentScope as HasEventDispatcher
    }
  }

  companion object {
    private fun getKeysForPendingStateUpdates(stateHandler: StateHandler): Set<String> {
      return stateHandler.keysForPendingUpdates
    }
  }
}
