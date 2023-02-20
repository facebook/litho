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

import static com.facebook.litho.StateContainer.StateUpdate;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.transitions.TransitionUtils;
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
  private final Map<String, StateContainer> mStateContainers = new HashMap<>();

  /**
   * Contains all keys of components that were present in the current ComponentTree and therefore
   * their StateContainer needs to be kept around.
   */
  @GuardedBy("this")
  private final HashSet<String> mNeededStateContainers = new HashSet<>();

  /** Map of all cached values that are stored for the current ComponentTree. */
  @GuardedBy("this")
  @Nullable
  private Map<Object, Object> mCachedValues;

  // These are both lists of (globalKey, updateMethod) pairs, where globalKey is the global key
  // of the component the update applies to
  @GuardedBy("this")
  private Map<String, List<HookUpdater>> mPendingHookUpdates;

  private Map<String, List<HookUpdater>> mAppliedHookUpdates;

  private final InitialStateContainer mInitialStateContainer;

  private @Nullable HashSet<String> mStateContainerNotFoundForKeys;

  static final String ERROR_STATE_CONTAINER_NOT_FOUND_APPLY_STATE_UPDATE_EARLY =
      "StateHandler:StateContainerNotFoundApplyStateUpdateEarly";

  @VisibleForTesting
  public StateHandler() {
    this(null);
  }

  @VisibleForTesting
  public StateHandler(final @Nullable StateHandler stateHandler) {
    if (stateHandler == null) {
      this.mInitialStateContainer = new InitialStateContainer();
      return;
    }

    synchronized (this) {
      this.mInitialStateContainer = stateHandler.mInitialStateContainer;
      copyStateUpdatesMap(
          stateHandler.getPendingStateUpdates(),
          stateHandler.getPendingLazyStateUpdates(),
          stateHandler.getAppliedStateUpdates());
      copyCurrentStateContainers(stateHandler.getStateContainers());
      copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
      runHooks(stateHandler);
    }
  }

  public InitialStateContainer getInitialStateContainer() {
    return mInitialStateContainer;
  }

  public synchronized boolean isEmpty() {
    return mStateContainers.isEmpty();
  }

  /**
   * @return whether this StateHandler has updates that haven't been committed to the
   *     source-of-truth StateHandler on the ComponentTree.
   */
  synchronized boolean hasUncommittedUpdates() {
    return (CollectionsUtils.isNotNullOrEmpty(mPendingStateUpdates))
        || (CollectionsUtils.isNotNullOrEmpty(mPendingHookUpdates))
        // Because we immediately apply Kotlin state updates at the beginning of layout, we need to
        // also check applied state updates to see if this StateHandler has uncommitted updates.
        || (CollectionsUtils.isNotNullOrEmpty(mAppliedHookUpdates));
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

  void keepStateContainerForGlobalKey(String key) {
    mNeededStateContainers.add(key);
  }

  /**
   * StateContainer in this StateHandler should be accessed using this method as it will also ensure
   * that the state is marked as needed
   */
  @Nullable
  StateContainer getStateContainer(String key) {
    return mStateContainers.get(key);
  }

  StateContainer createOrGetStateContainerForComponent(
      final ComponentContext scopedContext, final Component component, final String key) {
    final StateContainer currentStateContainer;

    synchronized (this) {
      currentStateContainer = mStateContainers.get(key);
    }

    if (currentStateContainer != null) {
      mNeededStateContainers.add(key);
      return currentStateContainer;
    } else {
      final StateContainer initialState =
          mInitialStateContainer.createOrGetInitialStateForComponent(component, scopedContext, key);
      addStateContainer(key, initialState);
      return initialState;
    }
  }

  private void applyStateUpdates(final String key, final StateContainer newStateContainer) {
    final List<StateUpdate> stateUpdatesForKey;

    synchronized (this) {
      stateUpdatesForKey = mPendingStateUpdates == null ? null : mPendingStateUpdates.get(key);
    }

    List<Transition> transitionsFromStateUpdate = null;

    final SpecGeneratedComponent.TransitionContainer asTransitionContainer =
        newStateContainer instanceof SpecGeneratedComponent.TransitionContainer
            ? (SpecGeneratedComponent.TransitionContainer) newStateContainer
            : null;

    // If there are no state updates pending for this component, simply store its current state.
    if (stateUpdatesForKey != null) {
      for (StateUpdate update : stateUpdatesForKey) {
        if (asTransitionContainer != null) {
          final Transition transition =
              asTransitionContainer.applyStateUpdateWithTransition(update);
          if (transition != null) {
            if (transitionsFromStateUpdate == null) {
              transitionsFromStateUpdate = new ArrayList<>();
            }
            TransitionUtils.setOwnerKey(transition, key);
            transitionsFromStateUpdate.add(transition);
          }
        } else {
          newStateContainer.applyStateUpdate(update);
        }
      }

      LithoStats.incrementComponentAppliedStateUpdateCountBy(stateUpdatesForKey.size());

      synchronized (this) {
        if (mPendingLazyStateUpdates != null) {
          mPendingLazyStateUpdates.remove(key); // remove from pending lazy
        }
        mAppliedStateUpdates.put(key, stateUpdatesForKey); // add to applied

        if (CollectionsUtils.isNotNullOrEmpty(transitionsFromStateUpdate)) {
          maybeInitPendingStateUpdateTransitions();
          mPendingStateUpdateTransitions.put(key, transitionsFromStateUpdate);
        }
      }
    }
  }

  @ThreadSafe(enableChecks = false)
  synchronized void applyStateUpdatesEarly(
      final ComponentContext context,
      final Component component,
      final @Nullable LithoNode prevTreeRootNode) {
    if (mPendingStateUpdates != null) {
      for (Map.Entry<String, List<StateUpdate>> entry : mPendingStateUpdates.entrySet()) {
        final String key = entry.getKey();
        try {
          StateContainer stateContainer = mStateContainers.get(key);
          if (stateContainer == null) {
            stateContainer = mInitialStateContainer.getInitialStateForComponent(key);
          }

          if (stateContainer == null) {
            if (mStateContainerNotFoundForKeys == null) {
              mStateContainerNotFoundForKeys = new HashSet<>();
            }
            mStateContainerNotFoundForKeys.add(key);
            continue;
          }

          final StateContainer newStateContainer = stateContainer.clone();
          mNeededStateContainers.add(key);
          mStateContainers.put(key, newStateContainer);
          applyStateUpdates(key, newStateContainer);
        } catch (Exception ex) {

          // Remove pending state update from ComponentTree's state handler since we don't want to
          // process this pending state update again. If we don't remove it and someone is using
          // setRoot in onError api then we can end up in an infinite loop
          context.removePendingStateUpdate(key, context.isNestedTreeContext());

          if (prevTreeRootNode != null) {
            handleExceptionDuringApplyStateUpdate(key, prevTreeRootNode, ex);
          } else {
            ComponentUtils.handleWithHierarchy(context, component, ex);
          }
        }
      }

      mPendingStateUpdates.clear();
    }
  }

  public void thowSoftErrorIfStateContainerWasNotFound(String key, Component component) {
    if (mStateContainerNotFoundForKeys != null && mStateContainerNotFoundForKeys.contains(key)) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          ERROR_STATE_CONTAINER_NOT_FOUND_APPLY_STATE_UPDATE_EARLY
              + ":"
              + component.getSimpleName(),
          "StateContainer not found for component for which we have pending state update");
    }
  }

  synchronized void removePendingStateUpdate(String key) {
    mPendingStateUpdates.remove(key);
  }

  private static void handleExceptionDuringApplyStateUpdate(
      final String key, final LithoNode current, final Exception exception) {
    List<ScopedComponentInfo> scopedComponentInfos = current.getScopedComponentInfos();
    for (ScopedComponentInfo scopedComponentInfo : scopedComponentInfos) {
      ComponentContext context = scopedComponentInfo.getContext();
      if (context.getGlobalKey().equals(key)) {
        ComponentUtils.handleWithHierarchy(context, scopedComponentInfo.getComponent(), exception);
        break;
      }
    }

    for (int index = 0; index < current.getChildCount(); ++index) {
      LithoNode childLithoNode = current.getChildAt(index);
      if (key.startsWith(childLithoNode.getHeadComponentKey())) {
        handleExceptionDuringApplyStateUpdate(key, childLithoNode, exception);
      }
    }
  }

  public synchronized void addStateContainer(String key, StateContainer state) {
    mNeededStateContainers.add(key);
    mStateContainers.put(key, state);
  }

  StateContainer applyLazyStateUpdatesForContainer(String componentKey, StateContainer container) {
    final List<StateUpdate> stateUpdatesForKey;

    synchronized (this) {
      stateUpdatesForKey =
          mPendingLazyStateUpdates == null ? null : mPendingLazyStateUpdates.get(componentKey);
    }

    if (CollectionsUtils.isNullOrEmpty(stateUpdatesForKey)) {
      return container;
    }

    final StateContainer containerWithUpdatesApplied = container.clone();
    for (StateUpdate update : stateUpdatesForKey) {
      containerWithUpdatesApplied.applyStateUpdate(update);
    }
    return containerWithUpdatesApplied;
  }

  /**
   * Removes a list of state updates that have been applied from the pending state updates list and
   * updates the map of current components with the given components.
   *
   * @param stateHandler state handler that was used to apply state updates in a layout pass
   */
  void commit(StateHandler stateHandler) {
    clearStateUpdates(stateHandler.getAppliedStateUpdates());
    clearUnusedStateContainers(stateHandler);
    copyCurrentStateContainers(stateHandler.getStateContainers());
    copyPendingStateTransitions(stateHandler.getPendingStateUpdateTransitions());
    commitHookState(stateHandler.mAppliedHookUpdates);

    if (mStateContainerNotFoundForKeys != null) {
      mStateContainerNotFoundForKeys.clear();
    }
  }

  synchronized Set<String> getKeysForPendingUpdates() {
    final Set<String> keys = new HashSet<>();
    if (mAppliedStateUpdates != null) {
      keys.addAll(mAppliedStateUpdates.keySet());
    }
    if (mPendingStateUpdates != null) {
      keys.addAll(mPendingStateUpdates.keySet());
    }
    if (mPendingHookUpdates != null) {
      keys.addAll(mPendingHookUpdates.keySet());
    }
    if (mAppliedHookUpdates != null) {
      keys.addAll(mAppliedHookUpdates.keySet());
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

  @Nullable
  @VisibleForTesting
  synchronized Map<String, List<StateUpdate>> getAppliedStateUpdates() {
    return mAppliedStateUpdates;
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

    if (CollectionsUtils.isNullOrEmpty(pendingStateUpdates)
        && CollectionsUtils.isNullOrEmpty(appliedStateUpdates)) {
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
  private void copyCurrentStateContainers(@Nullable Map<String, StateContainer> stateContainers) {
    if (stateContainers == null) {
      return;
    }

    synchronized (this) {
      mStateContainers.clear();
      mStateContainers.putAll(stateContainers);
    }
  }

  private static void clearUnusedStateContainers(StateHandler currentStateHandler) {
    if (currentStateHandler.mStateContainers.isEmpty()) {
      return;
    }

    final HashSet<String> neededStateContainers = currentStateHandler.mNeededStateContainers;
    final List<String> stateContainerKeys = new ArrayList<>();

    stateContainerKeys.addAll(currentStateHandler.mStateContainers.keySet());

    for (String key : stateContainerKeys) {
      if (!neededStateContainers.contains(key)) {
        currentStateHandler.mStateContainers.remove(key);
      }
    }
  }

  private void copyPendingStateTransitions(
      @Nullable Map<String, List<Transition>> pendingStateUpdateTransitions) {
    if (CollectionsUtils.isNullOrEmpty(pendingStateUpdateTransitions)) {
      return;
    }

    synchronized (this) {
      maybeInitPendingStateUpdateTransitions();
      mPendingStateUpdateTransitions.putAll(pendingStateUpdateTransitions);
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

  //
  // Hooks - Experimental - see KState.kt
  //

  /**
   * Registers the given block to be run before the next layout calculation to update hook state.
   */
  synchronized void queueHookStateUpdate(String key, HookUpdater updater) {
    if (mPendingHookUpdates == null) {
      mPendingHookUpdates = new HashMap<>();
    }
    List<HookUpdater> hookUpdaters = mPendingHookUpdates.get(key);
    if (hookUpdaters == null) {
      hookUpdaters = new ArrayList<>();
      mPendingHookUpdates.put(key, hookUpdaters);
    }
    hookUpdaters.add(updater);
  }

  @VisibleForTesting
  int getPendingHookUpdatesCount() {
    if (CollectionsUtils.isNullOrEmpty(mPendingHookUpdates)) {
      return 0;
    }

    int count = 0;
    for (List<HookUpdater> hookUpdaters : mPendingHookUpdates.values()) {
      count += hookUpdaters.size();
    }

    return count;
  }

  /**
   * Called when creating a new StateHandler for a layout calculation. It copies the source of truth
   * state, and then the current list of HookUpdater blocks that need to be applied. Unlike normal
   * state, these blocks are run immediately to update this StateHandlers hook state before we start
   * creating components.
   *
   * @param other the ComponentTree's source-of-truth StateHandler where pending state updates are
   *     collected
   */
  private void runHooks(StateHandler other) {
    if (other.mPendingHookUpdates != null) {
      Map<String, List<HookUpdater>> updates = getHookUpdatesCopy(other.mPendingHookUpdates);
      for (Map.Entry<String, List<HookUpdater>> hookUpdates : updates.entrySet()) {
        final String key = hookUpdates.getKey();
        final StateContainer stateContainer = mStateContainers.get(key);
        // currentState could be null if the state is removed from the StateHandler before the
        // update runs
        if (stateContainer != null) {
          KStateContainer kStateContainer = (KStateContainer) stateContainer;
          for (HookUpdater hookUpdate : hookUpdates.getValue()) {
            kStateContainer = hookUpdate.getUpdatedStateContainer(kStateContainer);
          }

          mStateContainers.put(key, kStateContainer);
        }
      }
      mAppliedHookUpdates = updates;
    }
  }

  /**
   * Gets a state container with all applied updates for the given key without committing the
   * updates to a state handler.
   */
  public @Nullable KStateContainer getStateContainerWithHookUpdates(String globalKey) {
    final @Nullable StateContainer stateContainer;

    synchronized (this) {
      stateContainer = mStateContainers.get(globalKey);

      if (stateContainer == null) {
        return null;
      }
    }
    KStateContainer stateContainerWithUpdatesApplied = (KStateContainer) stateContainer;

    List<HookUpdater> hookUpdaters = null;

    synchronized (this) {
      if (mPendingHookUpdates != null) {
        final List<HookUpdater> pendingHookUpdatersForKey = mPendingHookUpdates.get(globalKey);
        if (pendingHookUpdatersForKey != null) {
          hookUpdaters = new ArrayList<>(pendingHookUpdatersForKey);
        }
      }
    }

    if (hookUpdaters == null) {
      return stateContainerWithUpdatesApplied;
    }

    for (HookUpdater hookUpdater : hookUpdaters) {
      stateContainerWithUpdatesApplied =
          hookUpdater.getUpdatedStateContainer(stateContainerWithUpdatesApplied);
    }

    return stateContainerWithUpdatesApplied;
  }

  /**
   * Called on the ComponentTree's source-of-truth StateHandler when a layout has completed and new
   * state needs to be committed. In this case, we want to remove any pending state updates that
   * this StateHandler applied, while leaving new ones that have accumulated in the interim. We also
   * copy over the new mapping from hook state keys to values.
   */
  private void commitHookState(@Nullable Map<String, List<HookUpdater>> appliedHookUpdates) {
    if (appliedHookUpdates == null
        || mPendingHookUpdates == null
        || mPendingHookUpdates.isEmpty()) {
      return;
    }

    for (Map.Entry<String, List<HookUpdater>> appliedHookUpdatesForKey :
        appliedHookUpdates.entrySet()) {
      String globalKey = appliedHookUpdatesForKey.getKey();
      final List<HookUpdater> pendingHookUpdatersForKey;
      synchronized (this) {
        pendingHookUpdatersForKey = mPendingHookUpdates.get(globalKey);

        if (pendingHookUpdatersForKey == null) {
          continue;
        }

        final List<HookUpdater> appliedHookUpdatersForKey = appliedHookUpdatesForKey.getValue();
        pendingHookUpdatersForKey.removeAll(appliedHookUpdatersForKey);

        if (pendingHookUpdatersForKey.isEmpty()) {
          mPendingHookUpdates.remove(globalKey);
        }
      }
    }
  }

  private Map<String, List<HookUpdater>> getHookUpdatesCopy(
      Map<String, List<HookUpdater>> copyFrom) {
    final Map<String, List<HookUpdater>> copyInto = new HashMap<>(copyFrom.size());

    for (Map.Entry<String, List<HookUpdater>> entry : copyFrom.entrySet()) {
      copyInto.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }

    return copyInto;
  }
}
