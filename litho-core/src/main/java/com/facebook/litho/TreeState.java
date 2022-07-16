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
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class TreeState {

  private final StateHandler mRenderStateHandler;

  private final StateHandler mLayoutStateHandler;

  public TreeState() {
    mRenderStateHandler = StateHandler.createNewInstance(null);
    mLayoutStateHandler = StateHandler.createNewInstance(null);
  }

  public TreeState(TreeState treeState) {
    mRenderStateHandler = StateHandler.createNewInstance(treeState.mRenderStateHandler);
    mLayoutStateHandler = StateHandler.createNewInstance(treeState.mLayoutStateHandler);
  }

  // TODO: Remove this method
  StateHandler getRenderStateHandler() {
    return mRenderStateHandler;
  }

  private StateHandler getStateHandler(boolean isNestedTree) {
    return isNestedTree ? mLayoutStateHandler : mRenderStateHandler;
  }

  void registerRenderState() {
    mRenderStateHandler.getInitialStateContainer().registerStateHandler(mRenderStateHandler);
  }

  void registerLayoutState() {
    mLayoutStateHandler.getInitialStateContainer().registerStateHandler(mLayoutStateHandler);
  }

  void unregisterRenderState(TreeState treeState) {
    mRenderStateHandler
        .getInitialStateContainer()
        .unregisterStateHandler(treeState.mRenderStateHandler);
  }

  void unregisterLayoutState(TreeState treeState) {
    mLayoutStateHandler
        .getInitialStateContainer()
        .unregisterStateHandler(treeState.mLayoutStateHandler);
  }

  void commitRenderState(TreeState localTreeState) {
    mRenderStateHandler.commit(localTreeState.mRenderStateHandler);
  }

  void commitLayoutState(TreeState localTreeState) {
    mLayoutStateHandler.commit(localTreeState.mLayoutStateHandler);
  }

  void queueStateUpdate(
      String key,
      StateContainer.StateUpdate stateUpdate,
      boolean isLazyStateUpdate,
      boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.queueStateUpdate(key, stateUpdate, isLazyStateUpdate);
  }

  void queueHookStateUpdate(String key, HookUpdater updater, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.queueHookStateUpdate(key, updater);
  }

  void applyLazyStateUpdatesForContainer(
      String componentKey, StateContainer container, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.applyLazyStateUpdatesForContainer(componentKey, container);
  }

  boolean hasUncommittedUpdates() {
    return mRenderStateHandler.hasUncommittedUpdates()
        || mLayoutStateHandler.hasUncommittedUpdates();
  }

  public boolean isEmpty() {
    return mRenderStateHandler.isEmpty() && mLayoutStateHandler.isEmpty();
  }

  void applyStateUpdatesEarly(
      final ComponentContext context,
      final Component component,
      final @Nullable LithoNode prevTreeRootNode,
      final boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.applyStateUpdatesEarly(context, component, prevTreeRootNode);
  }

  void applyStateUpdatesForComponent(
      final ComponentContext scopedContext, final Component component, final String key) {
    final StateHandler stateHandler =
        scopedContext.isNestedTreeContext() ? mLayoutStateHandler : mRenderStateHandler;
    stateHandler.applyStateUpdatesForComponent(
        scopedContext, (SpecGeneratedComponent) component, key);
  }

  private static Set<String> getKeysForPendingStateUpdates(final StateHandler stateHandler) {
    return stateHandler.getKeysForPendingUpdates();
  }

  Set<String> getKeysForPendingRenderStateUpdates() {
    return getKeysForPendingStateUpdates(mRenderStateHandler);
  }

  Set<String> getKeysForPendingLayoutStateUpdates() {
    return getKeysForPendingStateUpdates(mLayoutStateHandler);
  }

  void addStateContainer(String key, StateContainer stateContainer, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.addStateContainer(key, stateContainer);
  }

  StateContainer getStateContainer(String key, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    return stateHandler.getStateContainer(key);
  }

  void removePendingStateUpdate(String key, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.removePendingStateUpdate(key);
  }

  @Nullable
  List<Transition> getStateUpdateTransitions() {
    final List<Transition> updateStateTransitions = new ArrayList<>();

    if (mRenderStateHandler.getPendingStateUpdateTransitions() != null) {
      final Map<String, List<Transition>> pendingStateUpdateTransitions =
          mRenderStateHandler.getPendingStateUpdateTransitions();
      for (List<Transition> pendingTransitions : pendingStateUpdateTransitions.values()) {
        updateStateTransitions.addAll(pendingTransitions);
      }
    }

    if (mLayoutStateHandler.getPendingStateUpdateTransitions() != null) {
      final Map<String, List<Transition>> pendingStateUpdateTransitions =
          mLayoutStateHandler.getPendingStateUpdateTransitions();
      for (List<Transition> pendingTransitions : pendingStateUpdateTransitions.values()) {
        updateStateTransitions.addAll(pendingTransitions);
      }
    }

    return updateStateTransitions.isEmpty() ? null : updateStateTransitions;
  }

  void putCachedValue(Object cachedValueInputs, Object cachedValue, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.putCachedValue(cachedValueInputs, cachedValue);
  }

  @Nullable
  Object getCachedValue(Object cachedValueInputs, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    return stateHandler.getCachedValue(cachedValueInputs);
  }
}
