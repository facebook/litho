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

import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.annotations.UIState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class TreeState {

  private final StateHandler mRenderStateHandler;
  private final StateHandler mLayoutStateHandler;
  @UIState private final TreeMountInfo mTreeMountInfo;
  @UIState private final RenderState mRenderState;
  private final EventTriggersContainer mEventTriggersContainer;
  private final EventHandlersController mEventHandlersController;
  /**
   * This class represents whether this Litho tree has been mounted before. The usage is a bit
   * convoluted and will need to be cleaned out properly in the future.
   */
  public static class TreeMountInfo {
    public volatile boolean mHasMounted = false;
    public volatile boolean mIsFirstMount = false;
  }

  public TreeState() {
    mRenderStateHandler = new StateHandler(null);
    mLayoutStateHandler = new StateHandler(null);
    mTreeMountInfo = new TreeMountInfo();
    mRenderState = new RenderState();
    mEventTriggersContainer = new EventTriggersContainer();
    mEventHandlersController = new EventHandlersController();
  }

  public TreeState(TreeState treeState) {
    mRenderStateHandler = new StateHandler(treeState.mRenderStateHandler);
    mLayoutStateHandler = new StateHandler(treeState.mLayoutStateHandler);
    mTreeMountInfo = treeState.mTreeMountInfo;
    mRenderState = treeState.mRenderState;
    mEventTriggersContainer = treeState.mEventTriggersContainer;
    mEventHandlersController = treeState.mEventHandlersController;
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

  StateContainer applyLazyStateUpdatesForContainer(
      String componentKey, StateContainer container, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    return stateHandler.applyLazyStateUpdatesForContainer(componentKey, container);
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

  private static Set<String> getKeysForPendingStateUpdates(final StateHandler stateHandler) {
    return stateHandler.getKeysForPendingUpdates();
  }

  Set<String> getKeysForPendingRenderStateUpdates() {
    return getKeysForPendingStateUpdates(mRenderStateHandler);
  }

  Set<String> getKeysForPendingLayoutStateUpdates() {
    return getKeysForPendingStateUpdates(mLayoutStateHandler);
  }

  Set<String> getKeysForPendingStateUpdates() {
    Set<String> keys = getKeysForPendingStateUpdates(mRenderStateHandler);
    keys.addAll(getKeysForPendingStateUpdates(mLayoutStateHandler));
    return keys;
  }

  void addStateContainer(String key, StateContainer stateContainer, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.addStateContainer(key, stateContainer);
  }

  void keepStateContainerForGlobalKey(String key, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.keepStateContainerForGlobalKey(key);
  }

  @Nullable
  StateContainer getStateContainer(String key, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    return stateHandler.getStateContainer(key);
  }

  StateContainer createOrGetStateContainerForComponent(
      final ComponentContext scopedContext, final Component component, final String key) {
    final StateHandler stateHandler = getStateHandler(scopedContext.isNestedTreeContext());
    return stateHandler.createOrGetStateContainerForComponent(scopedContext, component, key);
  }

  void removePendingStateUpdate(String key, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    stateHandler.removePendingStateUpdate(key);
  }

  <T> boolean canSkipStateUpdate(
      final String globalKey,
      final int hookStateIndex,
      final @Nullable T newValue,
      final boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    final KStateContainer committedStateContainer =
        (KStateContainer) stateHandler.getStateContainer(globalKey);

    if (committedStateContainer != null
        && committedStateContainer.mStates != null
        && committedStateContainer.mStates.get(hookStateIndex) != null) {
      final KStateContainer committedStateContainerWithAppliedPendingHooks =
          stateHandler.getStateContainerWithHookUpdates(globalKey);

      if (committedStateContainerWithAppliedPendingHooks != null) {
        final T committedUpdatedValue =
            (T) committedStateContainerWithAppliedPendingHooks.mStates.get(hookStateIndex);

        if (committedUpdatedValue == null && newValue == null) {
          return true;
        }

        return committedUpdatedValue != null && committedUpdatedValue.equals(newValue);
      }
    }

    return false;
  }

  <T> boolean canSkipStateUpdate(
      final Function<T, T> newValueFunction,
      final String globalKey,
      final int hookStateIndex,
      final boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    final KStateContainer committedStateContainer =
        (KStateContainer) stateHandler.getStateContainer(globalKey);

    if (committedStateContainer != null
        && committedStateContainer.mStates != null
        && committedStateContainer.mStates.get(hookStateIndex) != null) {
      final KStateContainer committedStateContainerWithAppliedPendingHooks =
          stateHandler.getStateContainerWithHookUpdates(globalKey);

      if (committedStateContainerWithAppliedPendingHooks != null) {
        final T committedUpdatedValue =
            (T) committedStateContainerWithAppliedPendingHooks.mStates.get(hookStateIndex);
        final T newValueAfterPendingUpdate = newValueFunction.apply(committedUpdatedValue);

        if (committedUpdatedValue == null && newValueAfterPendingUpdate == null) {
          return true;
        }

        return committedUpdatedValue != null
            && committedUpdatedValue.equals(newValueAfterPendingUpdate);
      }
    }

    return false;
  }

  @Nullable
  List<Transition> getPendingStateUpdateTransitions() {
    List<Transition> updateStateTransitions = null;

    if (mRenderStateHandler.getPendingStateUpdateTransitions() != null) {
      final Map<String, List<Transition>> pendingStateUpdateTransitions =
          mRenderStateHandler.getPendingStateUpdateTransitions();
      updateStateTransitions = new ArrayList<>();
      for (List<Transition> pendingTransitions : pendingStateUpdateTransitions.values()) {
        updateStateTransitions.addAll(pendingTransitions);
      }
    }

    if (mLayoutStateHandler.getPendingStateUpdateTransitions() != null) {
      final Map<String, List<Transition>> pendingStateUpdateTransitions =
          mLayoutStateHandler.getPendingStateUpdateTransitions();

      if (updateStateTransitions == null) {
        updateStateTransitions = new ArrayList<>();
      }

      for (List<Transition> pendingTransitions : pendingStateUpdateTransitions.values()) {
        updateStateTransitions.addAll(pendingTransitions);
      }
    }

    return updateStateTransitions;
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

  <T> KStateContainer createOrGetInitialHookState(
      String key, int hookStateIndex, HookInitializer<T> initializer, boolean isNestedTree) {
    final StateHandler stateHandler = getStateHandler(isNestedTree);
    return stateHandler
        .getInitialStateContainer()
        .createOrGetInitialHookState(key, hookStateIndex, initializer);
  }

  public TreeMountInfo getMountInfo() {
    return mTreeMountInfo;
  }

  void applyPreviousRenderData(@Nullable List<ScopedComponentInfo> scopedComponentInfos) {
    if (CollectionsUtils.isNullOrEmpty(scopedComponentInfos)) {
      return;
    }

    if (mRenderState == null) {
      return;
    }

    mRenderState.applyPreviousRenderData(scopedComponentInfos);
  }

  void applyPreviousRenderData(LayoutState layoutState) {
    final List<ScopedComponentInfo> scopedComponentInfos =
        layoutState.getScopedComponentInfosNeedingPreviousRenderData();
    applyPreviousRenderData(scopedComponentInfos);
  }

  void recordRenderData(LayoutState layoutState) {
    final List<ScopedComponentInfo> scopedComponentInfos =
        layoutState.getScopedComponentInfosNeedingPreviousRenderData();
    if (CollectionsUtils.isNullOrEmpty(scopedComponentInfos)) {
      return;
    }
    mRenderState.recordRenderData(scopedComponentInfos);
  }

  @Nullable
  EventTrigger getEventTrigger(String triggerKey) {
    synchronized (mEventTriggersContainer) {
      return mEventTriggersContainer.getEventTrigger(triggerKey);
    }
  }

  @Nullable
  EventTrigger getEventTrigger(Handle handle, int methodId) {
    synchronized (mEventTriggersContainer) {
      return mEventTriggersContainer.getEventTrigger(handle, methodId);
    }
  }

  void clearUnusedTriggerHandlers() {
    synchronized (mEventTriggersContainer) {
      mEventTriggersContainer.clear();
    }
  }

  void bindEventAndTriggerHandlers(
      final @Nullable List<Pair<String, EventHandler>> createdEventHandlers,
      final @Nullable List<ScopedComponentInfo> scopedSpecComponentInfos) {
    synchronized (mEventTriggersContainer) {
      clearUnusedTriggerHandlers();
      if (createdEventHandlers != null) {
        mEventHandlersController.canonicalizeEventDispatchInfos(createdEventHandlers);
      }
      if (scopedSpecComponentInfos != null) {
        for (ScopedComponentInfo scopedSpecComponentInfo : scopedSpecComponentInfos) {
          final SpecGeneratedComponent component =
              (SpecGeneratedComponent) scopedSpecComponentInfo.getComponent();
          final ComponentContext scopedContext = scopedSpecComponentInfo.getContext();
          mEventHandlersController.updateEventDispatchInfoForGlobalKey(
              scopedContext, component, scopedContext.getGlobalKey());
          component.recordEventTrigger(scopedContext, mEventTriggersContainer);
        }
      }
    }

    mEventHandlersController.clearUnusedEventDispatchInfos();
  }

  @VisibleForTesting
  EventHandlersController getEventHandlersController() {
    return mEventHandlersController;
  }
}
