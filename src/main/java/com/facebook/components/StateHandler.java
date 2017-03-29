/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.support.v4.util.Pools;

import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.infer.annotation.ThreadSafe;

import static com.facebook.litho.ComponentLifecycle.StateUpdate;

/**
 * Holds information about the current State of the components in a Component Tree.
 */
public class StateHandler {

  private static final int INITIAL_STATE_UPDATE_LIST_CAPACITY = 4;
  private static final int INITIAL_MAP_CAPACITY = 4;
  private static final int POOL_CAPACITY = 10;

  private static final Pools.SynchronizedPool<List<StateUpdate>> sStateUpdatesListPool =
      new Pools.SynchronizedPool<>(POOL_CAPACITY);
  private static final
  Pools.SynchronizedPool<Map<String, List<StateUpdate>>> sPendingStateUpdatesMapPool =
      new Pools.SynchronizedPool<>(POOL_CAPACITY);
  private static final
  Pools.SynchronizedPool<Map<String, StateContainer>> sStateContainersMapPool =
      new Pools.SynchronizedPool<>(POOL_CAPACITY);
  private static final Pools.SynchronizedPool<Set<String>> sKnownGlobalKeysSetPool =
      new Pools.SynchronizedPool<>(POOL_CAPACITY);

  /**
   * List of state updates that will be applied during the next layout pass.
   */
  private Map<String, List<StateUpdate>> mPendingStateUpdates;

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   */
  public Map<String, StateContainer> mStateContainers;

  private Set<String> mKnownGlobalKeys;

  void init(StateHandler stateHandler) {
    if (stateHandler == null) {
      return;
    }
    copyPendingStateUpdatesMap(stateHandler.getPendingStateUpdates());
    copyCurrentStateContainers(stateHandler.getStateContainers());
  }

  public static StateHandler acquireNewInstance(StateHandler stateHandler) {
    return ComponentsPools.acquireStateHandler(stateHandler);
  }

  public boolean isEmpty() {
    return mStateContainers == null || mStateContainers.isEmpty();
  }

  /**
   * Adds a state update to the list of the state updates that will be applied for the given
   * component key during the next layout pass.
   * @param key the global key of the component
   * @param stateUpdate the state update to apply to the component
   */
  void queueStateUpdate(String key, StateUpdate stateUpdate) {
    maybeInitPendingUpdates();

    List<StateUpdate> pendingStateUpdatesForKey = mPendingStateUpdates.get(key);

    if (pendingStateUpdatesForKey == null) {
      pendingStateUpdatesForKey = StateHandler.acquireStateUpdatesList();
      mPendingStateUpdates.put(key, pendingStateUpdatesForKey);
    }

    pendingStateUpdatesForKey.add(stateUpdate);
  }

  /**
   * Sets the initial value for a state or transfers the previous state value to the new component,
   * then applies all the states updates that have been enqueued for the new component's global key.
   * Assumed thread-safe because the one write is before all the reads.
