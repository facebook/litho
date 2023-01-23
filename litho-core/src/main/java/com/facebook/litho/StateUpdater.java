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

package com.facebook.litho;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

/**
 * StateUpdater lets a Component rendered with a scoped ComponentContext interact with Litho's state
 * An implementation of StateUpdater is responsible for collecting state update operations and
 * schedule a new Resolve/Layout step to occur. The default implementation of StateUpdater is
 * ComponentTree but it might be useful to implement this interface when integrating Litho in
 * different rendering frameworks where it's not desirable for Litho to control the
 * resolve/layout/commit process.
 */
public interface StateUpdater {

  /**
   * Enqueues a state update that will schedule a new render on the calling thread at the end of its
   * current run-loop. It is expected that the calling thread has an active Looper.
   */
  void updateStateSync(
      String globalKey,
      StateContainer.StateUpdate stateUpdate,
      String attribution,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  /**
   * Enqueues a state update that will schedule a new render on a Thread controlled by the Litho
   * infrastructure.
   */
  void updateStateAsync(
      String globalKey,
      StateContainer.StateUpdate stateUpdate,
      String attribution,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  /**
   * Enqueues a state update that will not schedule a new render. The new state will immediately be
   * visible in Event Handlers and it will be visible in the next render phase.
   */
  void updateStateLazy(
      String globalKey, StateContainer.StateUpdate stateUpdate, boolean nestedTreeContext);

  /** Same as updateStateAsync but for Hook State. */
  void updateHookStateAsync(
      String globalKey,
      HookUpdater updateBlock,
      String s,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  /** Same as updateStateSync but for Hook State. */
  void updateHookStateSync(
      String globalKey,
      HookUpdater updateBlock,
      String s,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  StateContainer applyLazyStateUpdatesForContainer(
      String globalKey, StateContainer container, boolean nestedTreeContext);

  /** Returns a Cached value that is accessible across all re-render operations. */
  Object getCachedValue(Object cachedValueInputs, boolean nestedTreeContext);

  /** Stores a Cached value that will be accessible across all re-render operations. */
  void putCachedValue(Object cachedValueInputs, Object cachedValue, boolean nestedTreeContext);

  /**
   * Removes a state update that was previously enqueued if the state update has not been processed
   * yet.
   */
  void removePendingStateUpdate(String key, boolean nestedTreeContext);

  /** @return whether this tree has ever been mounted before */
  boolean isFirstMount();

  /** sets whether this tree has ever been mounted before */
  void setIsFirstMount(boolean needsToRerunTransitions);

  <T> boolean canSkipStateUpdate(
      final String globalKey,
      final int hookStateIndex,
      final @Nullable T newValue,
      final boolean isNestedTree);

  <T> boolean canSkipStateUpdate(
      final Function<T, T> newValueFunction,
      final String globalKey,
      final int hookStateIndex,
      final boolean isNestedTree);
}
