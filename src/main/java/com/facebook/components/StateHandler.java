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
   * @param component the new component
   */
  @ThreadSafe(enableChecks = false)
  void applyStateUpdatesForComponent(Component component) {
    maybeInitStateContainers();
    maybeInitKnownGlobalKeys();

    final ComponentLifecycle lifecycle = component.getLifecycle();
    if (!lifecycle.hasState()) {
      return;
    }

    final StateContainer previousStateContainer;
    final String key = component.getGlobalKey();
    final StateContainer currentStateContainer =
        mStateContainers.get(key);

    if (mKnownGlobalKeys.contains(key)) {
      // We found two components with the same global key.
      throw new RuntimeException(
          "Cannot set State for " +
              component.getSimpleName() +
              ", found another Component with the same key");
    }
    mKnownGlobalKeys.add(key);

    if (currentStateContainer != null) {
      lifecycle.transferState(
          component.getScopedContext(),
          currentStateContainer,
          component);
      previousStateContainer = currentStateContainer;
    } else {
      lifecycle.createInitialState(component.getScopedContext(), component);
      previousStateContainer = component.getStateContainer();
    }

    final List<StateUpdate> stateUpdatesForKey = mPendingStateUpdates == null
        ? null
        : mPendingStateUpdates.get(key);

    // If there are no state updates pending for this component, simply store its current state.
    if (stateUpdatesForKey != null) {
      for (StateUpdate update : stateUpdatesForKey) {
        update.updateState(previousStateContainer, component);
      }
    }

    mStateContainers.put(key, component.getStateContainer());
  }

  /**
   * Removes a list of state updates that have been applied from the pending state updates list and
   *  updates the map of current components with the given components.
   * @param stateHandler state handler that was used to apply state updates in a layout pass
   */
  void commit(StateHandler stateHandler) {
    clearStateUpdates(stateHandler.getPendingStateUpdates());
    updateCurrentComponentsWithState(stateHandler.getStateContainers());
  }

  private void clearStateUpdates(Map<String, List<StateUpdate>> appliedStateUpdates) {
    if (appliedStateUpdates == null ||
        mPendingStateUpdates == null ||
        mPendingStateUpdates.isEmpty()) {
      return;
    }

    for (String key : appliedStateUpdates.keySet()) {
      final List<StateUpdate> pendingStateUpdatesForKey = mPendingStateUpdates.get(key);
      if (pendingStateUpdatesForKey == null) {
        continue;
      }

      final List<StateUpdate> appliedStateUpdatesForKey = appliedStateUpdates.get(key);
      if (pendingStateUpdatesForKey.size() == appliedStateUpdatesForKey.size()) {
        mPendingStateUpdates.remove(key);
        releaseStateUpdatesList(pendingStateUpdatesForKey);
      } else {
        pendingStateUpdatesForKey.removeAll(appliedStateUpdatesForKey);
      }
    }
  }

  private void updateCurrentComponentsWithState(
      Map<String, StateContainer> updatedStateContainers) {
    if (updatedStateContainers == null || updatedStateContainers.isEmpty()) {
      return;
    }

    maybeInitStateContainers();
    mStateContainers.putAll(updatedStateContainers);
  }

  void release() {
    if (mPendingStateUpdates != null) {
      mPendingStateUpdates.clear();
      sPendingStateUpdatesMapPool.release(mPendingStateUpdates);
      mPendingStateUpdates = null;
    }

    if (mStateContainers != null) {
      mStateContainers.clear();
      sStateContainersMapPool.release(mStateContainers);
      mStateContainers = null;
    }

    if (mKnownGlobalKeys != null) {
      mKnownGlobalKeys.clear();
      sKnownGlobalKeysSetPool.release(mKnownGlobalKeys);
      mKnownGlobalKeys = null;
    }
  }

  private static List<StateUpdate> acquireStateUpdatesList() {
    return acquireStateUpdatesList(null);
  }

  private static List<StateUpdate> acquireStateUpdatesList(List<StateUpdate> copyFrom) {
    List<StateUpdate> list = sStateUpdatesListPool.acquire();
    if (list == null) {
      list = new ArrayList<>(
          copyFrom == null ? INITIAL_STATE_UPDATE_LIST_CAPACITY : copyFrom.size());
    }
    if (copyFrom != null) {
      list.addAll(copyFrom);
    }

    return list;
  }

  private static void releaseStateUpdatesList(List<StateUpdate> list) {
    list.clear();
    sStateUpdatesListPool.release(list);
  }

  Map<String, StateContainer> getStateContainers() {
    return mStateContainers;
  }

  Map<String, List<StateUpdate>> getPendingStateUpdates() {
    return mPendingStateUpdates;
  }

  /**
   * @return copy the information from the given map of state updates into the map of pending state
   * updates.
   */
  private void copyPendingStateUpdatesMap(
      Map<String, List<StateUpdate>> pendingStateUpdates) {
    if (pendingStateUpdates == null || pendingStateUpdates.isEmpty()) {
      return;
    }

    maybeInitPendingUpdates();
    for (String key : pendingStateUpdates.keySet()) {
      mPendingStateUpdates.put(key, acquireStateUpdatesList(pendingStateUpdates.get(key)));
    }
  }

  /**
   * @return copy the list of given state containers into the map that holds the current
   * state containers of components.
   */
  private void copyCurrentStateContainers(Map<String, StateContainer> stateContainers) {
    if (stateContainers == null || stateContainers.isEmpty()) {
      return;
    }

    maybeInitStateContainers();
    for (String key : stateContainers.keySet()) {
      mStateContainers.put(key, stateContainers.get(key));
    }
  }

  private void maybeInitStateContainers() {
    if (mStateContainers == null) {
      mStateContainers = sStateContainersMapPool.acquire();
      if (mStateContainers == null) {
        mStateContainers = new HashMap<>(INITIAL_MAP_CAPACITY);
      }
    }
  }

  private void maybeInitPendingUpdates() {
    if (mPendingStateUpdates == null) {
      mPendingStateUpdates = sPendingStateUpdatesMapPool.acquire();
      if (mPendingStateUpdates == null) {
        mPendingStateUpdates = new HashMap<>(INITIAL_MAP_CAPACITY);
      }
    }
  }

  private void maybeInitKnownGlobalKeys() {
    if (mKnownGlobalKeys == null) {
      mKnownGlobalKeys = sKnownGlobalKeysSetPool.acquire();
      if (mKnownGlobalKeys == null) {
        mKnownGlobalKeys = new HashSet<>(INITIAL_MAP_CAPACITY);
      }
    }
  }
}
