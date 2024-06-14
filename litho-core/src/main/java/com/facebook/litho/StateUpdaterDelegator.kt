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

/**
 * StateUpdaterDelegator is responsible for holding reference to the right [StateUpdater] that will
 * be used during creation of [ComponentTree]
 */
class StateUpdaterDelegator : StateUpdater {
  var delegate: StateUpdater? = null
    private set

  fun attachStateUpdater(stateUpdater: StateUpdater) {
    this.delegate = stateUpdater
  }

  override var isFirstMount: Boolean
    get() = applyOnStateUpdater { isFirstMount }
    set(value) {
      applyOnStateUpdater { isFirstMount = value }
    }

  override fun updateStateSync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) = applyOnStateUpdater {
    updateStateSync(globalKey, stateUpdate, attribution, isCreateLayoutInProgress, isLayoutState)
  }

  override fun updateStateAsync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) = applyOnStateUpdater {
    updateStateAsync(globalKey, stateUpdate, attribution, isCreateLayoutInProgress, isLayoutState)
  }

  override fun updateStateLazy(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      isLayoutState: Boolean
  ) = applyOnStateUpdater { updateStateLazy(globalKey, stateUpdate, isLayoutState) }

  override fun updateHookStateAsync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) = applyOnStateUpdater {
    updateHookStateAsync(
        globalKey, updateBlock, attribution, isCreateLayoutInProgress, isLayoutState)
  }

  override fun updateHookStateSync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isLayoutState: Boolean
  ) = applyOnStateUpdater {
    updateHookStateSync(
        globalKey, updateBlock, attribution, isCreateLayoutInProgress, isLayoutState)
  }

  override fun applyLazyStateUpdatesForContainer(
      globalKey: String,
      container: StateContainer,
      isLayoutState: Boolean
  ): StateContainer = applyOnStateUpdater {
    applyLazyStateUpdatesForContainer(globalKey, container, isLayoutState)
  }

  override fun getCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      isLayoutState: Boolean
  ): Any? = applyOnStateUpdater {
    getCachedValue(globalKey, index, cachedValueInputs, isLayoutState)
  }

  override fun putCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      cachedValue: Any?,
      isLayoutState: Boolean
  ) = applyOnStateUpdater {
    putCachedValue(globalKey, index, cachedValueInputs, cachedValue, isLayoutState)
  }

  override fun removePendingStateUpdate(key: String, isLayoutState: Boolean) = applyOnStateUpdater {
    removePendingStateUpdate(key, isLayoutState)
  }

  override fun <T> canSkipStateUpdate(
      globalKey: String,
      hookStateIndex: Int,
      newValue: T?,
      isLayoutState: Boolean
  ): Boolean = applyOnStateUpdater {
    canSkipStateUpdate(globalKey, hookStateIndex, newValue, isLayoutState)
  }

  override fun <T> canSkipStateUpdate(
      newValueFunction: (T) -> T,
      globalKey: String,
      hookStateIndex: Int,
      isLayoutState: Boolean
  ): Boolean = applyOnStateUpdater {
    canSkipStateUpdate(newValueFunction, globalKey, hookStateIndex, isLayoutState)
  }

  override fun getEventTrigger(key: String): EventTrigger<*>? = applyOnStateUpdater {
    getEventTrigger(key)
  }

  override fun getEventTrigger(handle: Handle, id: Int): EventTrigger<*>? = applyOnStateUpdater {
    getEventTrigger(handle, id)
  }

  private fun <T> applyOnStateUpdater(body: StateUpdater.() -> T): T {
    return body(delegate ?: error("Delegate StateUpdater not set"))
  }
}
