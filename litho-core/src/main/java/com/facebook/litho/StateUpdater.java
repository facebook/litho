// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho;

public interface StateUpdater {

  void updateStateSync(
      String globalKey,
      StateContainer.StateUpdate stateUpdate,
      String attribution,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  void updateStateAsync(
      String globalKey,
      StateContainer.StateUpdate stateUpdate,
      String attribution,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  void updateStateLazy(
      String globalKey, StateContainer.StateUpdate stateUpdate, boolean nestedTreeContext);

  void updateHookStateAsync(
      String globalKey,
      HookUpdater updateBlock,
      String s,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  void updateHookStateSync(
      String globalKey,
      HookUpdater updateBlock,
      String s,
      boolean createLayoutInProgress,
      boolean nestedTreeContext);

  StateContainer applyLazyStateUpdatesForContainer(
      String globalKey, StateContainer container, boolean nestedTreeContext);

  Object getCachedValue(Object cachedValueInputs, boolean nestedTreeContext);

  void putCachedValue(Object cachedValueInputs, Object cachedValue, boolean nestedTreeContext);

  void removePendingStateUpdate(String key, boolean nestedTreeContext);
}
