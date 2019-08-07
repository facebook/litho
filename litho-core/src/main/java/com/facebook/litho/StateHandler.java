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

import static com.facebook.litho.StateContainer.StateUpdate;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.stats.LithoStats;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;

/** Holds information about the current State of the components in a Component Tree. */
public class StateHandler {

  private static final int INITIAL_STATE_UPDATE_LIST_CAPACITY = 4;
  private static final int INITIAL_MAP_CAPACITY = 4;

  /** List of state updates that will be applied during the next layout pass. */
  @GuardedBy("this")
  private Map<String, List<StateUpdate>> mPendingStateUpdates;

  /** List of lazy state updates. */
  @GuardedBy("this")
  @Nullable
  private Map<String, List<StateUpdate>> mPendingLazyStateUpdates;

  /** List of transitions from state update that will be applied on next mount. */
  @GuardedBy("this")
  @Nullable
  private Map<String, List<Transition>> mPendingStateUpdateTransitions;

  /** List of transitions from state update that have been applied on next mount. */
  @GuardedBy("this")
  private Map<String, List<StateUpdate>> mAppliedStateUpdates;

  /**
   * Maps a component key to a component object that retains the current state values for that key.
   */
  @GuardedBy("this")
  public Map<String, StateContainer> mStateContainers;

  /**
   * Contains all keys of components that were present in the current ComponentTree and therefore
   * their StateContainer needs to be kept around.
   */
  @GuardedBy("this")
  public HashSet<String> mNeededStateContainers;

  /** Map of all cached values that are stored for the current ComponentTree. */
  @GuardedBy("this")
  @Nullable
  private Map<Object, Object> mCachedValues;

  private Map<String, Object> mCreateInitialStateLock;

  public StateHandler() {
    this(null);
  }

  public StateHandler(final @Nullable StateHandler stateHandler) {
    if (stateHandler == null) {
      return;
    }

    synchronized (this) {
      copyStateUpdatesMap(
          stateHandler.getPendingStateUpdates(),
          stateHandler.getPendingLazyStateUpdates(),
          stateHandler.getAppliedStateUpdates());
      copyCurrentStateContainers(stateHandler.getStateContainers());
      copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
    }
  }

  public static StateHandler createNewInstance(@Nullable StateHandler stateHandler) {
    return new StateHandler(stateHandler);
  }

  public static StateHandler createShallowCopyForLazyStateUpdates(final StateHandler stateHandler) {
    final StateHandler copy = new StateHandler();
    synchronized (stateHandler) {
      copy.copyPendingLazyStateUpdates(stateHandler.mPendingLazyStateUpdates);
    }
    return copy;
  }

  public synchronized boolean isEmpty() {
    return mStateContainers == null || mStateContainers.isEmpty();
  }

  synchronized boolean hasPendingUpdates() {
    if (mPendingStateUpdates != null && !mPendingStateUpdates.isEmpty()) {
      for (List<StateUpdate> entry : mPendingStateUpdates.values()) {
        if (!entry.isEmpty()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Adds a state update to the list of the state updates that will be applied for the given
   * component key during the next layout pass.
   *
   * @param key the global key of the component
   * @param stateUpdate the state update to apply to the component
   * @param isLazyStateUpdate the flag to indicate if it's a lazy state update
   */
  synchronized void queueStateUpdate(
      String key, StateUpdate stateUpdate, boolean isLazyStateUpdate) {
    maybeInitStateUpdatesMap();

    addStateUpdateForKey(key, stateUpdate, mPendingStateUpdates);

    if (isLazyStateUpdate) {
      maybeInitLazyStateUpdatesMap();
      addStateUpdateForKey(key, stateUpdate, mPendingLazyStateUpdates);
    }
  }

  private static void addStateUpdateForKey(
      String key, StateUpdate stateUpdate, Map<String, List<StateUpdate>> map) {
    List<StateUpdate> pendingStateUpdatesForKey = map.get(key);

    if (pendingStateUpdatesForKey == null) {
      pendingStateUpdatesForKey = StateHandler.createStateUpdatesList();
      map.put(key, pendingStateUpdatesForKey);
    }

    pendingStateUpdatesForKey.add(stateUpdate);
  }

  /**
   * Sets the initial value for a state or transfers the previous state value to the new component,
   * then applies all the states updates that have been enqueued for the new component's global key.
   * Assumed thread-safe because the one write is before all the reads.
   *
   * @param component the new component
   */
  @ThreadSafe(enableChecks = false)
  void applyStateUpdatesForComponent(Component component) {
    maybeInitStateContainers();
    maybeInitNeededStateContainers();

    if (!component.hasState()) {
      return;
    }

    final String key = component.getGlobalKey();
    final StateContainer currentStateContainer;

    synchronized (this) {
      currentStateContainer = mStateContainers.get(key);
      mNeededStateContainers.add(key);
    }

    if (currentStateContainer != null) {
      component.transferState(currentStateContainer, component.getStateContainer());
    } else {
      final ComponentTree componentTree = component.getScopedContext().getComponentTree();

      // It's possible that Component.measure is called from outside a ComponentTree LayoutState
      // calculation. In that case, we can't reuse or store the StateContainer in the ComponentTree,
      // so we call createInitialState immediately and exit.
      if (componentTree == null || !componentTree.shouldCreateInitialStateOncePerThread()) {
        component.createInitialState(component.getScopedContext());
      } else {
        maybeCreateInitialStateAndCommitResult(component, componentTree);
      }
    }

    final List<StateUpdate> stateUpdatesForKey;

    synchronized (this) {
      stateUpdatesForKey = mPendingStateUpdates == null ? null : mPendingStateUpdates.get(key);
    }

    // If there are no state updates pending for this component, simply store its current state.
    if (stateUpdatesForKey != null) {
      for (StateUpdate update : stateUpdatesForKey) {
        component.getStateContainer().applyStateUpdate(update);
      }

      LithoStats.incStateUpdate(stateUpdatesForKey.size());

      if (component.getScopedContext().isReconciliationEnabled()) {
        synchronized (this) {
          mPendingStateUpdates.remove(key); // remove from pending
          if (mPendingLazyStateUpdates != null) {
            mPendingLazyStateUpdates.remove(key); // remove from pending lazy
          }
          mAppliedStateUpdates.put(key, stateUpdatesForKey); // add to applied
        }
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

  static void maybeCreateInitialStateAndCommitResult(
      Component component, ComponentTree componentTree) {
    final StateHandler stateHandler = componentTree.getStateHandler();

    // Tree has been released, we can exit immediately.
    if (stateHandler == null) {
      return;
    }

    final String key = component.getGlobalKey();

    // Each createInitialState call creates a lock. The ComponentTree will store the first lock
    // that is passed to it to use for sync with other createInitialState calls, so that it only
    // gets executed once.
    final Object initLock = new Object();
    synchronized (initLock) {
      final Object lock = stateHandler.getCreateInitialStateLock(key, initLock);

      synchronized (lock) {
        if (initLock == lock) {
          // createInitialState has not been called for this component. Call it and immediately
          // commit the result to this state handler.
          component.createInitialState(component.getScopedContext());
          stateHandler.maybeInitStateContainers();
          stateHandler.mStateContainers.put(key, component.getStateContainer());
        } else {
          // This means that createInitialState was already called for this component. Transfer the
          // result without calling it again.
          component.transferState(
              stateHandler.mStateContainers.get(key), component.getStateContainer());
        }
      }
    }
  }

  private Object getCreateInitialStateLock(String globalKey, Object lock) {
    synchronized (this) {
      if (mCreateInitialStateLock == null) {
        mCreateInitialStateLock = new HashMap<>();
      }
    }

    synchronized (mCreateInitialStateLock) {
      Object existingLock = mCreateInitialStateLock.get(globalKey);

      if (existingLock != null) {
        return existingLock;
      }

      mCreateInitialStateLock.put(globalKey, lock);
    }

    return lock;
  }

  void applyLazyStateUpdatesForContainer(String componentKey, StateContainer container) {
    final List<StateUpdate> stateUpdatesForKey;

    synchronized (this) {
      stateUpdatesForKey =
          mPendingLazyStateUpdates == null ? null : mPendingLazyStateUpdates.get(componentKey);
    }

    if (stateUpdatesForKey != null) {
      for (StateUpdate update : stateUpdatesForKey) {
        container.applyStateUpdate(update);
      }
    }
  }

  /**
   * Removes a list of state updates that have been applied from the pending state updates list and
   * updates the map of current components with the given components.
   *
   * @param stateHandler state handler that was used to apply state updates in a layout pass
   * @param isReconciliationEnabled is the NestedTree resolution experiment enabled
   */
  void commit(StateHandler stateHandler, boolean isReconciliationEnabled) {
    clearStateUpdates(
        isReconciliationEnabled
            ? stateHandler.getAppliedStateUpdates()
            : stateHandler.getPendingStateUpdates());
    clearUnusedStateContainers(this, stateHandler);
    copyCurrentStateContainers(stateHandler.getStateContainers());
    copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
  }

  synchronized Set<String> getKeysForPendingUpdates() {
    final Set<String> keys = new HashSet<>();
    if (mAppliedStateUpdates != null) {
      keys.addAll(mAppliedStateUpdates.keySet());
    }
    if (mPendingStateUpdates != null) {
      keys.addAll(mPendingStateUpdates.keySet());
    }

    return keys;
  }

  private void clearStateUpdates(@Nullable Map<String, List<StateUpdate>> appliedStateUpdates) {
    synchronized (this) {
      if (appliedStateUpdates == null
          || mPendingStateUpdates == null
          || mPendingStateUpdates.isEmpty()) {
        return;
      }
    }

    for (Map.Entry<String, List<StateUpdate>> appliedStateUpdate : appliedStateUpdates.entrySet()) {
      String appliedStateUpdateKey = appliedStateUpdate.getKey();
      final List<StateUpdate> pendingStateUpdatesForKey;
      final List<StateUpdate> pendingLazyStateUpdatesForKey;
      synchronized (this) {
        pendingStateUpdatesForKey = mPendingStateUpdates.get(appliedStateUpdateKey);
        pendingLazyStateUpdatesForKey =
            mPendingLazyStateUpdates == null
                ? null
                : mPendingLazyStateUpdates.get(appliedStateUpdateKey);
      }

      if (pendingStateUpdatesForKey == null) {
        continue;
      }

      final List<StateUpdate> appliedStateUpdatesForKey = appliedStateUpdate.getValue();
      if (pendingStateUpdatesForKey.size() == appliedStateUpdatesForKey.size()) {
        synchronized (this) {
          mPendingStateUpdates.remove(appliedStateUpdateKey);
          if (mPendingLazyStateUpdates != null) {
            mPendingLazyStateUpdates.remove(appliedStateUpdateKey);
          }
        }
      } else {
        pendingStateUpdatesForKey.removeAll(appliedStateUpdatesForKey);
        if (pendingLazyStateUpdatesForKey != null) {
          pendingLazyStateUpdatesForKey.removeAll(appliedStateUpdatesForKey);
        }
      }
    }
  }

  private static List<StateUpdate> createStateUpdatesList() {
    return createStateUpdatesList(null);
  }

  private static List<StateUpdate> createStateUpdatesList(@Nullable List<StateUpdate> copyFrom) {
    List<StateUpdate> list =
        new ArrayList<>(copyFrom == null ? INITIAL_STATE_UPDATE_LIST_CAPACITY : copyFrom.size());
    if (copyFrom != null) {
      list.addAll(copyFrom);
    }

    return list;
  }

  synchronized Map<String, StateContainer> getStateContainers() {
    return mStateContainers;
  }

  @Nullable
  synchronized Map<String, List<StateUpdate>> getPendingStateUpdates() {
    return mPendingStateUpdates;
  }

  @Nullable
  synchronized Map<String, List<StateUpdate>> getPendingLazyStateUpdates() {
    return mPendingLazyStateUpdates;
  }

  @Nullable
  synchronized Map<String, List<Transition>> getPendingStateUpdateTransitions() {
    return mPendingStateUpdateTransitions;
  }

  @VisibleForTesting
  synchronized Map<String, List<StateUpdate>> getAppliedStateUpdates() {
    return mAppliedStateUpdates;
  }

  synchronized void consumePendingStateUpdateTransitions(
      List<Transition> outList, @Nullable String logContext) {
    if (mPendingStateUpdateTransitions == null) {
      return;
    }

    for (List<Transition> pendingTransitions : mPendingStateUpdateTransitions.values()) {
      for (int i = 0, size = pendingTransitions.size(); i < size; i++) {
        TransitionUtils.addTransitions(pendingTransitions.get(i), outList, logContext);
      }
    }
    mPendingStateUpdateTransitions = null;
  }

  @Nullable
  synchronized Object getCachedValue(Object cachedValueInputs) {
    if (mCachedValues == null) {
      mCachedValues = new HashMap<>();
    }

    return mCachedValues.get(cachedValueInputs);
  }

  synchronized void putCachedValue(Object cachedValueInputs, Object cachedValue) {
    if (mCachedValues == null) {
      mCachedValues = new HashMap<>();
    }

    mCachedValues.put(cachedValueInputs, cachedValue);
  }

  /**
   * Copies the information from the given map of state updates into the map of pending state
   * updates.
   */
  private void copyStateUpdatesMap(
      @Nullable Map<String, List<StateUpdate>> pendingStateUpdates,
      @Nullable Map<String, List<StateUpdate>> pendingLazyStateUpdates,
      @Nullable Map<String, List<StateUpdate>> appliedStateUpdates) {

    if ((pendingStateUpdates == null || pendingStateUpdates.isEmpty())
        && (appliedStateUpdates == null || appliedStateUpdates.isEmpty())) {
      return;
    }

    maybeInitStateUpdatesMap();
    synchronized (this) {
      if (pendingStateUpdates != null) {
        for (String key : pendingStateUpdates.keySet()) {
          mPendingStateUpdates.put(key, createStateUpdatesList(pendingStateUpdates.get(key)));
        }
      }

      copyPendingLazyStateUpdates(pendingLazyStateUpdates);

      if (appliedStateUpdates != null) {
        for (Map.Entry<String, List<StateUpdate>> appliedStateUpdate :
            appliedStateUpdates.entrySet()) {
          mAppliedStateUpdates.put(
              appliedStateUpdate.getKey(), createStateUpdatesList(appliedStateUpdate.getValue()));
        }
      }
    }
  }

  private void copyPendingLazyStateUpdates(
      @Nullable Map<String, List<StateUpdate>> pendingLazyStateUpdates) {

    if (pendingLazyStateUpdates == null || pendingLazyStateUpdates.isEmpty()) {
      return;
    }

    maybeInitLazyStateUpdatesMap();
    for (Map.Entry<String, List<StateUpdate>> pendingLazyStateUpdate :
        pendingLazyStateUpdates.entrySet()) {
      mPendingLazyStateUpdates.put(
          pendingLazyStateUpdate.getKey(),
          createStateUpdatesList(pendingLazyStateUpdate.getValue()));
    }
  }

  /**
   * Copies the list of given state containers into the map that holds the current state containers
   * of components.
   */
  private void copyCurrentStateContainers(Map<String, StateContainer> stateContainers) {
    if (stateContainers == null || stateContainers.isEmpty()) {
      return;
    }

    synchronized (this) {
      maybeInitStateContainers();
      mStateContainers.clear();
      mStateContainers.putAll(stateContainers);
    }
  }

  private static void clearUnusedStateContainers(
      StateHandler createInitialStateLockingStateHandler, StateHandler currentStateHandler) {
    final HashSet<String> neededStateContainers = currentStateHandler.mNeededStateContainers;
    final List<String> stateContainerKeys = new ArrayList<>();
    if (neededStateContainers == null || currentStateHandler.mStateContainers == null) {
      return;
    }

    final boolean shouldCleanLocks =
        createInitialStateLockingStateHandler.mCreateInitialStateLock != null
            && !createInitialStateLockingStateHandler.mCreateInitialStateLock.isEmpty();

    stateContainerKeys.addAll(currentStateHandler.mStateContainers.keySet());

    for (String key : stateContainerKeys) {
      if (!neededStateContainers.contains(key)) {
        currentStateHandler.mStateContainers.remove(key);
        if (shouldCleanLocks) {
          createInitialStateLockingStateHandler.mCreateInitialStateLock.remove(key);
        }
      }
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
      mStateContainers = new HashMap<>(INITIAL_MAP_CAPACITY);
    }
  }

  private synchronized void maybeInitNeededStateContainers() {
    if (mNeededStateContainers == null) {
      mNeededStateContainers = new HashSet<>();
    }
  }

  private synchronized void maybeInitPendingStateUpdateTransitions() {
    if (mPendingStateUpdateTransitions == null) {
      mPendingStateUpdateTransitions = new HashMap<>();
    }
  }

  private synchronized void maybeInitStateUpdatesMap() {
    if (mPendingStateUpdates == null) {
      mPendingStateUpdates = new HashMap<>(INITIAL_MAP_CAPACITY);
    }

    if (mAppliedStateUpdates == null) {
      mAppliedStateUpdates = new HashMap<>(INITIAL_MAP_CAPACITY);
    }
  }

  private synchronized void maybeInitLazyStateUpdatesMap() {
    if (mPendingLazyStateUpdates == null) {
      mPendingLazyStateUpdates = new HashMap<>(INITIAL_MAP_CAPACITY);
    }
  }
}
