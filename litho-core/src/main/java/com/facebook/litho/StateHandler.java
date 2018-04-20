/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentLifecycle.StateUpdate;

import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.ComponentLifecycle.StateContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;

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

  /**
   * List of state updates that will be applied during the next layout pass.
   */
  @GuardedBy("this")
  private Map<String, List<StateUpdate>> mPendingStateUpdates;

  /** List of transitions from state update that will be applied on next mount. */
  @GuardedBy("this")
  @Nullable
  private Map<String, List<Transition>> mPendingStateUpdateTransitions;

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   */
  @GuardedBy("this")
  public Map<String, StateContainer> mStateContainers;

  void init(@Nullable StateHandler stateHandler) {
    if (stateHandler == null) {
      return;
    }

    synchronized (this) {
      copyPendingStateUpdatesMap(stateHandler.getPendingStateUpdates());
      copyCurrentStateContainers(stateHandler.getStateContainers());
      copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
    }
  }

  public static StateHandler acquireNewInstance(@Nullable StateHandler stateHandler) {
    return ComponentsPools.acquireStateHandler(stateHandler);
  }

  public synchronized boolean isEmpty() {
    return mStateContainers == null || mStateContainers.isEmpty();
  }

  /**
   * Adds a state update to the list of the state updates that will be applied for the given
   * component key during the next layout pass.
   * @param key the global key of the component
   * @param stateUpdate the state update to apply to the component
   */
  synchronized void queueStateUpdate(String key, StateUpdate stateUpdate) {
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

    if (!component.hasState()) {
      return;
    }

    final StateContainer previousStateContainer;
    final String key = component.getGlobalKey();
    final StateContainer currentStateContainer;

    synchronized (this) {
      currentStateContainer = mStateContainers.get(key);
    }

    if (currentStateContainer != null) {
      component.transferState(
          component.getScopedContext(),
          currentStateContainer);
      previousStateContainer = currentStateContainer;
    } else {
      component.createInitialState(component.getScopedContext());
      previousStateContainer = component.getStateContainer();
    }

    final List<StateUpdate> stateUpdatesForKey;

    synchronized (this) {
      stateUpdatesForKey = mPendingStateUpdates == null
          ? null
          : mPendingStateUpdates.get(key);
    }

    // If there are no state updates pending for this component, simply store its current state.
    if (stateUpdatesForKey != null) {
      for (StateUpdate update : stateUpdatesForKey) {
        update.updateState(previousStateContainer, component);
      }
    }

    synchronized (this) {
      final StateContainer stateContainer = component.getStateContainer();
      mStateContainers.put(key, stateContainer);
      if (stateContainer instanceof ComponentLifecycle.TransitionContainer) {
        final List<Transition> transitions =
            ((ComponentLifecycle.TransitionContainer) stateContainer).consumeTransitions();
        if (!transitions.isEmpty()) {
          maybeInitPendingStateUpdateTransitions();
          mPendingStateUpdateTransitions.put(key, transitions);
        }
      }
    }
  }

  /**
   * Removes a list of state updates that have been applied from the pending state updates list and
   *  updates the map of current components with the given components.
   * @param stateHandler state handler that was used to apply state updates in a layout pass
   */
  void commit(StateHandler stateHandler) {
    clearStateUpdates(stateHandler.getPendingStateUpdates());
    copyCurrentStateContainers(stateHandler.getStateContainers());
    copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
  }

  private void clearStateUpdates(Map<String, List<StateUpdate>> appliedStateUpdates) {
    synchronized (this) {
      if (appliedStateUpdates == null ||
          mPendingStateUpdates == null ||
          mPendingStateUpdates.isEmpty()) {
        return;
      }
    }

    for (String key : appliedStateUpdates.keySet()) {
      final List<StateUpdate> pendingStateUpdatesForKey;
      synchronized (this) {
        pendingStateUpdatesForKey = mPendingStateUpdates.get(key);
      }

      if (pendingStateUpdatesForKey == null) {
        continue;
      }

      final List<StateUpdate> appliedStateUpdatesForKey = appliedStateUpdates.get(key);
      if (pendingStateUpdatesForKey.size() == appliedStateUpdatesForKey.size()) {
        synchronized (this) {
          mPendingStateUpdates.remove(key);
        }
        releaseStateUpdatesList(pendingStateUpdatesForKey);
      } else {
        pendingStateUpdatesForKey.removeAll(appliedStateUpdatesForKey);
      }
    }
  }

  synchronized void release() {
    if (mPendingStateUpdates != null) {
      mPendingStateUpdates.clear();
      sPendingStateUpdatesMapPool.release(mPendingStateUpdates);
      mPendingStateUpdates = null;
    }

    mPendingStateUpdateTransitions = null;

    if (mStateContainers != null) {
      mStateContainers.clear();
      sStateContainersMapPool.release(mStateContainers);
      mStateContainers = null;
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

  synchronized Map<String, StateContainer> getStateContainers() {
    return mStateContainers;
  }

  synchronized Map<String, List<StateUpdate>> getPendingStateUpdates() {
    return mPendingStateUpdates;
  }

  @Nullable
  synchronized Map<String, List<Transition>> getPendingStateUpdateTransitions() {
    return mPendingStateUpdateTransitions;
  }

  @Nullable
  synchronized void consumePendingStateUpdateTransitions(List<Transition> outList) {
    if (mPendingStateUpdateTransitions == null) {
      return;
    }

    for (List<Transition> pendingTransitions : mPendingStateUpdateTransitions.values()) {
      for (int i = 0, size = pendingTransitions.size(); i < size; i++) {
        TransitionUtils.addTransitions(pendingTransitions.get(i), outList);
      }
    }
    mPendingStateUpdateTransitions = null;
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
      synchronized (this) {
        mPendingStateUpdates.put(key, acquireStateUpdatesList(pendingStateUpdates.get(key)));
      }
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

    synchronized (this) {
      maybeInitStateContainers();
      mStateContainers.putAll(stateContainers);
    }
  }

  private void copyPendingStateTransitions(
      @Nullable Map<String, List<Transition>> pendingStateUpdateTransitions) {
    if (pendingStateUpdateTransitions == null || pendingStateUpdateTransitions.isEmpty()) {
      return;
    }

    synchronized (this) {
      maybeInitPendingStateUpdateTransitions();
      mPendingStateUpdateTransitions.putAll(pendingStateUpdateTransitions);
    }
  }

  private synchronized void maybeInitStateContainers() {
    if (mStateContainers == null) {
      mStateContainers = sStateContainersMapPool.acquire();
      if (mStateContainers == null) {
        mStateContainers = new HashMap<>(INITIAL_MAP_CAPACITY);
      }
    }
  }

  private synchronized void maybeInitPendingStateUpdateTransitions() {
    if (mPendingStateUpdateTransitions == null) {
      mPendingStateUpdateTransitions = new HashMap<>();
    }
  }

  private synchronized void maybeInitPendingUpdates() {
    if (mPendingStateUpdates == null) {
      mPendingStateUpdates = sPendingStateUpdatesMapPool.acquire();
      if (mPendingStateUpdates == null) {
        mPendingStateUpdates = new HashMap<>(INITIAL_MAP_CAPACITY);
      }
    }
  }

}
