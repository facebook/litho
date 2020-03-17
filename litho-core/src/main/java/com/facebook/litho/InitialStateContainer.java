/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.annotation.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The InitialStateContainer is a lookaside table used by a ComponentTree to create initial states
 * for Components. The idea is that the onCreateInitialState result for each component will be
 * cached and stored here so that we can guarantee it's not called multiple times on multiple
 * threads. We keep the initial states cached as long as there is a layout happening for the
 * ComponentTree. As soon as we detect that all in-flights layout have terminated we can clean up
 * the initial states cache.
 */
@ThreadSafe
public class InitialStateContainer {

  // All the initial states that have been created and can not yet be released. This is a concurrent
  // map as we can access it from multiple threads. The safety is given by the fact that we will
  // only get and set for a key while holding a lock for that specific key.
  @VisibleForTesting
  final Map<String, StateContainer> mInitialStates =
      Collections.synchronizedMap(new HashMap<String, StateContainer>());

  @GuardedBy("this")
  private final Map<String, Object> mCreateInitialStateLocks = new HashMap<>();

  @GuardedBy("this")
  @VisibleForTesting
  Set<StateHandler> mPendingStateHandlers = new HashSet<>();

  /**
   * Called when the ComponentTree creates a new StateHandler for a new layout computation. We keep
   * track of this new StateHandler so that we know that we need to wait for this layout computation
   * to finish before we can clear the initial states map.
   */
  synchronized void registerStateHandler(StateHandler stateHandler) {
    mPendingStateHandlers.add(stateHandler);
  }

  /**
   * If an initial state for this component has already been created just transfers it to it.
   * Otherwise onCreateInitialState gets called for the component and its result cached.
   */
  void createOrGetInitialStateForComponent(Component component) {
    Object stateLock;
    synchronized (this) {
      stateLock = mCreateInitialStateLocks.get(component.getGlobalKey());
      if (stateLock == null) {
        stateLock = new Object();
        mCreateInitialStateLocks.put(component.getGlobalKey(), stateLock);
      }
    }

    synchronized (stateLock) {
      final StateContainer stateContainer = mInitialStates.get(component.getGlobalKey());
      if (stateContainer == null) {
        component.createInitialState(component.getScopedContext());
        mInitialStates.put(component.getGlobalKey(), component.getStateContainer());
      } else {
        component.transferState(stateContainer, component.getStateContainer());
      }
    }
  }

  /**
   * Called when the ComponentTree commits a new StateHandler or discards one for a discarded layout
   * computation.
   */
  synchronized void unregisterStateHandler(StateHandler stateHandler) {
    mPendingStateHandlers.remove(stateHandler);
    if (mPendingStateHandlers.isEmpty()) {
      mCreateInitialStateLocks.clear();
      // This is safe as we have a guarantee that by this point there is no layout happening
      // and therefore we can not be executing createOrGetInitialStateForComponent from any
      // thread.
      mInitialStates.clear();
    }
  }
}
