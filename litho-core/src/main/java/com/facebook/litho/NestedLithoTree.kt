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

import android.view.View
import androidx.annotation.UiThread
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.utils.ThreadUtils.assertMainThread

/**
 * The [NestedLithoTree] should be used to render Litho components in a container, say a Primitive
 * like a ScrollView or when embedded Litho hierarchies in other RC framework hierarchies.
 *
 * The [NestedLithoTree] lets the client to manually request resolutions, and layouts. It also lets
 * the client intercept state updates so that updates can be synchronised with their render
 * lifecycle.
 */
object NestedLithoTree {

  fun resolve(
      id: Int,
      context: ComponentContext,
      root: Component,
      treeProps: TreePropContainer?,
      state: TreeState,
      current: ResolveResult?,
  ): ResolveResult {
    return if (current == null ||
        !ComponentUtils.isEquivalent(root, current.component, true) ||
        state.keysForPendingStateUpdates.isNotEmpty() ||
        treeProps != current.context.treePropContainer) {
      ResolveTreeFuture.resolve(
          context,
          root,
          state,
          -1,
          id,
          current?.node,
          "nested-resolve",
          null, // tree future is null; effectively cannot be cancelled
          null, // no logger passed; perhaps can inherit from parent
      )
    } else {
      current
    }
  }

  fun layout(
      result: ResolveResult,
      sizeConstraints: SizeConstraints,
      current: LayoutState?,
  ): LayoutState {
    val layoutState =
        if (result != current?.resolveResult || sizeConstraints != current.sizeConstraints) {
          LayoutTreeFuture.layout(
              result,
              sizeConstraints,
              -1,
              checkNotNull(result.context.lithoTree).id,
              current,
              current?.diffTree,
              null, // tree future is null; task cannot be cancelled
              null, // no logger passed; perhaps can inherit from parent
          )
        } else {
          current
        }

    layoutState.toRenderTree()

    return layoutState
  }

  fun LayoutState.commit() {
    val components = consumeComponentScopes()
    val eventHandlers = consumeCreatedEventHandlers()
    val stateUpdater = checkNotNull(componentContext.stateUpdater)
    val mountedView = checkNotNull(componentContext.lithoTree).mountedViewReference

    // clear state updates
    treeState.commit()

    // bind event handlers
    treeState.bindEventAndTriggerHandlers(eventHandlers, components)

    // bind handles
    for (handle in componentHandles) {
      handle.setStateUpdaterAndRootViewReference(stateUpdater, mountedView)
    }
  }

  fun LayoutState.runEffects() {
    treeState.effectsHandler.onAttached(attachables)
  }

  fun LayoutState.cleanup() {
    treeState.effectsHandler.onDetached()
    treeState.clearEventHandlersAndTriggers()
  }

  fun TreeState.enqueue(updates: List<PendingStateUpdate>): TreeState {
    for (update in updates) {
      this.enqueue(update)
    }
    return this
  }

  fun TreeState.enqueue(update: PendingStateUpdate): TreeState {
    when (update.updater) {
      is HookUpdater -> {
        queueHookStateUpdate(
            key = update.key,
            updater = update.updater,
            isNestedTree = update.isLayoutState,
        )
      }
      is StateContainer.StateUpdate -> {
        queueStateUpdate(
            key = update.key,
            stateUpdate = update.updater,
            update.isLazy,
            isNestedTree = update.isLayoutState,
        )
      }
    }
    return this
  }
}

// region NestedStateUpdater
class NestedStateUpdater(
    private val state: TreeState,
    private val updater: StateUpdateRequester,
) : StateUpdater {

  override var isFirstMount: Boolean
    get() = state.mountInfo.isFirstMount
    set(value) {
      state.mountInfo.isFirstMount = value
    }

  override fun updateHookStateAsync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    updater.request(
        PendingStateUpdate(
            key = globalKey,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        ))
  }

  override fun updateHookStateSync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    updater.request(
        PendingStateUpdate(
            key = globalKey,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        ))
  }

  override fun updateStateAsync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    updater.request(
        PendingStateUpdate(
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        ))
  }

  override fun updateStateSync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    updater.request(
        PendingStateUpdate(
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        ))
  }

  override fun updateStateLazy(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      isLayoutState: Boolean
  ) {
    updater.request(
        PendingStateUpdate(
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            isLazy = true,
        ))
  }

  override fun applyLazyStateUpdatesForContainer(
      globalKey: String,
      container: StateContainer,
      isLayoutState: Boolean
  ): StateContainer {
    return state.applyLazyStateUpdatesForContainer(globalKey, container, isLayoutState)
  }

  override fun <T> canSkipStateUpdate(
      globalKey: String,
      hookStateIndex: Int,
      newValue: T?,
      isLayoutState: Boolean
  ): Boolean {
    return state.canSkipStateUpdate(globalKey, hookStateIndex, newValue, isLayoutState)
  }

  override fun <T> canSkipStateUpdate(
      newValueFunction: (T) -> T,
      globalKey: String,
      hookStateIndex: Int,
      isLayoutState: Boolean
  ): Boolean {
    return state.canSkipStateUpdate(
        newValueFunction,
        globalKey,
        hookStateIndex,
        isLayoutState,
    )
  }

  override fun removePendingStateUpdate(key: String, isLayoutState: Boolean) {
    throw UnsupportedOperationException(
        """This API should not be invoked. Nested Litho Tree updates will
          |be cleared when nested layout state is committed."""
            .trimMargin())
  }

  override fun getCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      isLayoutState: Boolean
  ): Any? {
    return state.getCachedValue(globalKey, index, cachedValueInputs, isLayoutState)
  }

  override fun putCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      cachedValue: Any?,
      isLayoutState: Boolean
  ) {
    state.putCachedValue(globalKey, index, cachedValueInputs, cachedValue, isLayoutState)
  }

  override fun getEventTrigger(key: String): EventTrigger<*>? {
    return state.getEventTrigger(triggerKey = key)
  }

  override fun getEventTrigger(handle: Handle, id: Int): EventTrigger<*>? {
    return state.getEventTrigger(handle = handle, methodId = id)
  }
}

// endregion NestedStateUpdater

class NestedMountedViewReference : MountedViewReference {

  private var view: View? = null

  override var mountedView: View?
    @UiThread
    get() {
      return view
    }
    @UiThread
    set(value) {
      view = value
    }
}

// region NestedLithoTreeLifecycleProvider
class NestedLithoTreeLifecycleProvider : LithoTreeLifecycleProvider {

  private val listeners: MutableList<LithoTreeLifecycleProvider.OnReleaseListener> = mutableListOf()

  @Volatile private var _isReleased: Boolean = false

  override val isReleased: Boolean
    get() = _isReleased

  override fun addOnReleaseListener(listener: LithoTreeLifecycleProvider.OnReleaseListener) {
    synchronized(this) {
      if (!isReleased) {
        listeners.add(listener)
      }
    }
  }

  @UiThread
  fun release() {
    assertMainThread()
    _isReleased = true
    listeners.forEach { it.onReleased() }
    listeners.clear()
  }
}

// endregion

@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class PendingStateUpdate(
    val key: String,
    val updater: StateUpdateApplier,
    val isLayoutState: Boolean, // state created during the layout phase
    val isAsync: Boolean,
    val isLazy: Boolean = false,
    val attribution: String? = null,
)
