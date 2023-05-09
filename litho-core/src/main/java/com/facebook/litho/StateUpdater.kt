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

import androidx.arch.core.util.Function

/**
 * StateUpdater lets a [Component] rendered with a scoped [ComponentContext] interact with Litho's
 * state. An implementation of StateUpdater is responsible for collecting state update operations
 * and schedule a new Resolve/Layout step to occur. The default implementation of StateUpdater is
 * [ComponentTree], but it might be useful to implement this interface when integrating Litho in
 * different rendering frameworks where it's not desirable for Litho to control the
 * resolve/layout/commit process.
 */
interface StateUpdater {

  /** @return whether this tree has never been mounted before */
  var isFirstMount: Boolean

  /**
   * Enqueues a state update that will schedule a new render on the calling thread at the end of its
   * current run-loop. It is expected that the calling thread has an active Looper.
   */
  fun updateStateSync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isNestedTreeContext: Boolean
  )

  /**
   * Enqueues a state update that will schedule a new render on a Thread controlled by the Litho
   * infrastructure.
   */
  fun updateStateAsync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isNestedTreeContext: Boolean
  )

  /**
   * Enqueues a state update that will not schedule a new render. The new state will immediately be
   * visible in Event Handlers and it will be visible in the next render phase.
   */
  fun updateStateLazy(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      isNestedTreeContext: Boolean
  )

  /** Same as updateStateAsync but for Hook State. */
  fun updateHookStateAsync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isNestedTreeContext: Boolean
  )

  /** Same as updateStateSync but for Hook State. */
  fun updateHookStateSync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isCreateLayoutInProgress: Boolean,
      isNestedTreeContext: Boolean
  )

  fun applyLazyStateUpdatesForContainer(
      globalKey: String,
      container: StateContainer,
      isNestedTreeContext: Boolean
  ): StateContainer?

  /** Returns a Cached value that is accessible across all re-render operations. */
  fun getCachedValue(cachedValueInputs: Any, isNestedTreeContext: Boolean): Any?

  /** Stores a Cached value that will be accessible across all re-render operations. */
  fun putCachedValue(cachedValueInputs: Any, cachedValue: Any?, isNestedTreeContext: Boolean)

  /**
   * Removes a state update that was previously enqueued if the state update has not been processed
   * yet.
   */
  fun removePendingStateUpdate(key: String, isNestedTreeContext: Boolean)

  fun <T> canSkipStateUpdate(
      globalKey: String,
      hookStateIndex: Int,
      newValue: T?,
      isNestedTreeContext: Boolean
  ): Boolean

  fun <T> canSkipStateUpdate(
      newValueFunction: Function<T, T>,
      globalKey: String,
      hookStateIndex: Int,
      isNestedTreeContext: Boolean
  ): Boolean

  fun getEventTrigger(key: String): EventTrigger<*>?

  fun getEventTrigger(handle: Handle, id: Int): EventTrigger<*>?
}
