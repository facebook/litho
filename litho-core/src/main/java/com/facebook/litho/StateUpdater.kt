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

import com.facebook.litho.animation.AnimationFrameInvalidation
import com.facebook.litho.state.StateId

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
      globalKey: GlobalKey,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean,
  )

  /**
   * Enqueues a state update that will schedule a new render on a Thread controlled by the Litho
   * infrastructure.
   */
  fun updateStateAsync(
      globalKey: GlobalKey,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean,
  )

  /**
   * Enqueues a state update that will not schedule a new render. The new state will immediately be
   * visible in Event Handlers and it will be visible in the next render phase.
   */
  fun updateStateLazy(
      globalKey: GlobalKey,
      stateUpdate: StateContainer.StateUpdate,
      isLayoutState: Boolean,
  )

  /** Same as updateStateAsync but for Hook State. */
  fun updateHookStateAsync(
      stateId: StateId,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean,
  )

  /** Same as updateStateSync but for Hook State. */
  fun updateHookStateSync(
      stateId: StateId,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean,
  )

  /**
   * Applies a batch of animation-frame invalidations produced during a single `AnimationController`
   * frame tick. Each invalidation re-renders the read-tracked consumers of its `stateId`; the
   * animated value itself stays owned by its `AnimatedValue`.
   *
   * The default implementation preserves correctness by routing every invalidation through the
   * existing, frame-deferred [updateHookStateSync] path, so integrations that do not implement a
   * same-frame fast path keep working unchanged. [ComponentTree] overrides this with the same-frame
   * path.
   */
  fun applyAnimationFrameUpdates(invalidations: List<AnimationFrameInvalidation>) {
    for (invalidation in invalidations) {
      updateHookStateSync(
          invalidation.stateId,
          HookUpdater.NoOp,
          invalidation.attribution,
          invalidation.isLayoutState,
      )
    }
  }

  /**
   * Whether this updater implements the same-frame animation invalidation path.
   *
   * When false (the default), AnimatedValue keeps its pre-feature behavior — signaling re-renders
   * directly through [updateHookStateSync] without routing frames through the invalidation
   * dispatcher — so integrations that do not opt in pay no extra per-frame cost. [ComponentTree]
   * enables it when its animation-frame-invalidation config is set.
   */
  val isAnimationFrameInvalidationEnabled: Boolean
    get() = false

  fun applyLazyStateUpdatesForContainer(
      globalKey: GlobalKey,
      container: StateContainer,
      isLayoutState: Boolean,
  ): StateContainer

  /** Returns a Cached value that is accessible across all re-render operations. */
  fun getCachedValue(
      cacheKey: HookGlobalKey,
      cachedValueInputs: Any,
      isLayoutState: Boolean,
  ): Any?

  /** Stores a Cached value that will be accessible across all re-render operations. */
  fun putCachedValue(
      cacheKey: HookGlobalKey,
      cachedValueInputs: Any,
      cachedValue: Any?,
      isLayoutState: Boolean,
  )

  /**
   * Removes a state update that was previously enqueued if the state update has not been processed
   * yet.
   */
  fun removePendingStateUpdate(key: GlobalKey, isLayoutState: Boolean)

  fun <T> canSkipStateUpdate(stateId: StateId, newValue: T?, isLayoutState: Boolean): Boolean

  fun <T> canSkipStateUpdate(
      newValueFunction: (T) -> T,
      stateId: StateId,
      isLayoutState: Boolean,
  ): Boolean

  fun getEventTrigger(key: Any): EventTrigger<*>?

  fun getEventTrigger(handle: Handle, id: Int): EventTrigger<*>?
}
