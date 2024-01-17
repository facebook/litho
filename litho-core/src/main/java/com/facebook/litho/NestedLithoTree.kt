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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.SizeConstraints

/**
 * The [NestedLithoTree] should be used to render Litho components in a container, say a Primitive
 * like a ScrollView or when embedded Litho hierarchies in other RC framework hierarchies.
 *
 * The [NestedLithoTree] lets the client to manually request resolutions, and layouts. It also lets
 * the client intercept state updates so that updates can be synchronised with their render
 * lifecycle.
 */
class NestedLithoTree(
    context: ComponentContext,
    config: ComponentsConfiguration = context.lithoConfiguration.componentsConfig,
    private val requestUpdate: (isAsync: Boolean) -> Unit,
) : StateUpdater, MountedViewReference, ErrorComponentReceiver, LithoTreeLifecycleProvider {

  private val delegate: LithoTree = checkNotNull(context.lithoTree)

  private val id: Int = LithoTree.generateComponentTreeId()

  val context: ComponentContext =
      ComponentContext(
          context.androidContext,
          context.treeProps,
          context.lithoConfiguration.copy(componentsConfig = config),
          LithoTree(this, this, this, this, id),
          "nested-tree-root",
          context.lifecycleProvider,
          null,
          context.parentTreeProps,
      )

  val state: TreeState = TreeState()

  @Volatile
  var layoutState: LayoutState? = null
    private set

  private val pendingUpdates: MutableMap<String, PendingStateUpdate> = LinkedHashMap()

  fun resolve(root: Component, treeProps: TreeProps?): ResolveResult {

    val shouldResolve: Boolean
    val currentResult: ResolveResult?
    val newState: TreeState
    val pendingUpdatesSnapShot: HashMap<String, PendingStateUpdate>

    synchronized(this) {
      currentResult = layoutState?.resolveResult

      shouldResolve =
          currentResult == null ||
              pendingUpdates.isNotEmpty() ||
              treeProps != currentResult.context.treeProps

      pendingUpdatesSnapShot = HashMap(pendingUpdates)

      newState =
          if (shouldResolve) {
            TreeState(state).enqueue(pendingUpdatesSnapShot)
          } else {
            state // doesn't matter because the current result will be returned
          }
    }

    return if (currentResult == null || shouldResolve) {
      ResolveTreeFuture.resolve(
          ComponentContext(context, treeProps),
          root,
          newState,
          -1,
          id,
          currentResult?.node,
          "nested-resolve",
          null, // tree future is null; effectively cannot be cancelled
          null, // no logger passed; perhaps can inherit from parent
      )
    } else {
      currentResult
    }
  }

  fun layout(
      result: ResolveResult,
      sizeConstraints: SizeConstraints,
  ): LayoutState {

    val current: LayoutState? = layoutState

    return if (result != current?.resolveResult || sizeConstraints != current.sizeConstraints) {
      LayoutTreeFuture.layout(
          result,
          sizeConstraints,
          -1,
          id,
          current,
          current?.diffTree,
          null, // tree future is null; task cannot be cancelled
          null, // no logger passed; perhaps can inherit from parent
      )
    } else {
      current
    }
  }

  @UiThread
  fun commit(newLayoutState: LayoutState?) {
    synchronized(this) {
      newLayoutState?.treeState?.let { newState ->
        state.commitResolveState(newState)
        state.commitLayoutState(newState)
        pendingUpdates.keys.removeAll(newState.keysForPendingStateUpdates)
      }
    }
    layoutState = newLayoutState
  }

  override var isFirstMount: Boolean
    get() = state.mountInfo.isFirstMount
    set(value) {
      state.mountInfo.isFirstMount = value
    }

  override fun updateHookStateAsync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) {
    pendingUpdates[globalKey] =
        PendingStateUpdate(
            key = globalKey,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        )
    updateAsync()
  }

  override fun updateHookStateSync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) {

    pendingUpdates[globalKey] =
        PendingStateUpdate(
            key = globalKey,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        )

    updateSync()
  }

  override fun updateStateAsync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) {

    pendingUpdates[globalKey] =
        PendingStateUpdate(
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        )
    updateAsync()
  }

  override fun updateStateSync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) {
    pendingUpdates[globalKey] =
        PendingStateUpdate(
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        )
    updateSync()
  }

  override fun updateStateLazy(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      isLayoutState: Boolean
  ) {
    state.queueStateUpdate(globalKey, stateUpdate, true, isLayoutState)
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

  override val isReleased: Boolean
    get() = delegate.lithoTreeLifecycleProvider.isReleased

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

  override fun addOnReleaseListener(
      onReleaseListener: LithoTreeLifecycleProvider.OnReleaseListener
  ) {
    delegate.lithoTreeLifecycleProvider.addOnReleaseListener(onReleaseListener = onReleaseListener)
  }

  override fun onErrorComponent(component: Component?) {
    delegate.errorComponentReceiver.onErrorComponent(component)
  }

  override val mountedView: View?
    @UiThread get() = delegate.mountedViewReference.mountedView

  private fun updateAsync() {
    requestUpdate(true)
  }

  private fun updateSync() {
    requestUpdate(false)
  }

  private fun TreeState.enqueue(updates: Map<String, PendingStateUpdate>): TreeState {
    for ((key, update) in updates) {
      when (update.updater) {
        is HookUpdater -> {
          queueHookStateUpdate(
              key = key,
              updater = update.updater,
              isNestedTree = update.isLayoutState,
          )
        }
        is StateContainer.StateUpdate -> {
          queueStateUpdate(
              key = key,
              stateUpdate = update.updater,
              update.isLazy,
              isNestedTree = update.isLayoutState,
          )
        }
      }
    }
    return this
  }
}

@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
internal data class PendingStateUpdate(
    val key: String,
    val updater: StateUpdateApplier,
    val isLayoutState: Boolean, // state created during the layout phase
    val isAsync: Boolean,
    val isLazy: Boolean = false,
    val attribution: String?,
)
