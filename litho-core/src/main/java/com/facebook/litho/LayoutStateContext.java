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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.ComponentTree.LayoutStateFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, the LayoutState reference is nullified. Using a wrapper instead of passing
 * the instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
public class LayoutStateContext {

  private @Nullable LayoutState mLayoutStateRef;
  private @Nullable ComponentTree mComponentTree;
  private @Nullable LayoutStateFuture mLayoutStateFuture;
  private final Map<String, ScopedComponentInfo> mGlobalKeyToScopedInfo = new ConcurrentHashMap<>();
  private final Map<Integer, InternalNode> mComponentIdToWillRenderLayout = new HashMap<>();
  private @Nullable DiffNode mCurrentDiffTree;
  private @Nullable DiffNode mCurrentNestedTreeDiffNode;
  private boolean mHasNestedTreeDiffNodeSet = false;
  private boolean wasReconciled = false;

  private boolean mIsLayoutStarted = false;

  private volatile boolean mIsFrozen = false;

  private StateHandler mStateHandler;

  void freeze() {
    mIsFrozen = true;
  }

  public static LayoutStateContext getTestInstance(ComponentContext c) {
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, c.getComponentTree(), null, null, new StateHandler());
    layoutState.setLayoutStateContextForTest(layoutStateContext);
    return layoutStateContext;
  }

  void copyScopedInfoFrom(LayoutStateContext from, StateHandler stateHandler) {
    checkIfFrozen();

    mGlobalKeyToScopedInfo.clear();
    for (Map.Entry<String, ScopedComponentInfo> e : from.mGlobalKeyToScopedInfo.entrySet()) {
      final String key = e.getKey();
      final ScopedComponentInfo info = e.getValue().copy(this, stateHandler);
      mGlobalKeyToScopedInfo.put(key, info);
    }

    mComponentIdToWillRenderLayout.clear();
    mComponentIdToWillRenderLayout.putAll(from.mComponentIdToWillRenderLayout);

    wasReconciled = true;
  }

  /**
   * This is only used in tests and marked as {@link Deprecated}. Use {@link
   * LayoutStateContext(LayoutState, ComponentTree, LayoutStateFuture, DiffNode, StateHandler)}
   * instead.
   *
   * @param layoutState
   * @param componentTree
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @Deprecated
  public LayoutStateContext(
      final LayoutState layoutState, @Nullable final ComponentTree componentTree) {
    this(layoutState, componentTree, null, null, new StateHandler());
  }

  LayoutStateContext(
      final @Nullable LayoutState layoutState,
      final @Nullable ComponentTree componentTree,
      final @Nullable LayoutStateFuture layoutStateFuture,
      final @Nullable DiffNode currentDiffTree,
      final StateHandler stateHandler) {
    mLayoutStateRef = layoutState;
    mLayoutStateFuture = layoutStateFuture;
    mComponentTree = componentTree;
    mCurrentDiffTree = currentDiffTree;
    mStateHandler = stateHandler;
  }

  ScopedComponentInfo addScopedComponentInfo(
      final String globalKey,
      final Component component,
      final ComponentContext scopedContext,
      final ComponentContext parentContext) {
    checkIfFrozen();

    final EventHandler<ErrorEvent> errorEventHandler =
        ComponentUtils.createOrGetErrorEventHandler(component, parentContext, scopedContext);

    final ScopedComponentInfo info =
        new ScopedComponentInfo(component, scopedContext, errorEventHandler);

    final ScopedComponentInfo previous = mGlobalKeyToScopedInfo.put(globalKey, info);
    if (previous != null) {
      previous.transferInto(info);
    }

    return info;
  }

  ScopedComponentInfo getScopedComponentInfo(String globalKey) {
    if (globalKey == null) {
      return null;
    }

    final ScopedComponentInfo scopedComponentInfo = mGlobalKeyToScopedInfo.get(globalKey);
    if (scopedComponentInfo == null) {
      throw new IllegalStateException(
          "ScopedComponentInfo is null for key " + globalKey + getDebugString());
    }

    return scopedComponentInfo;
  }

  ComponentContext getScopedContext(String globalKey) {
    final ScopedComponentInfo info = getScopedComponentInfo(globalKey);
    return info.getContext();
  }

  @Nullable
  InternalNode consumeLayoutCreatedInWillRender(int componentId) {
    return mComponentIdToWillRenderLayout.remove(componentId);
  }

  @Nullable
  InternalNode getLayoutCreatedInWillRender(int componentId) {
    return mComponentIdToWillRenderLayout.get(componentId);
  }

  void setLayoutCreatedInWillRender(int componentId, final @Nullable InternalNode node) {
    mComponentIdToWillRenderLayout.put(componentId, node);
  }

  void releaseReference() {
    mLayoutStateRef = null;
    mLayoutStateFuture = null;
    mCurrentDiffTree = null;
  }

  /** Returns the LayoutState instance or null if the layout state has been released. */
  @Nullable
  LayoutState getLayoutState() {
    return mLayoutStateRef;
  }

  @Nullable
  @VisibleForTesting
  public ComponentTree getComponentTree() {
    return mComponentTree;
  }

  public @Nullable LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  boolean isLayoutInterrupted() {
    boolean isInterruptRequested =
        mLayoutStateFuture != null && mLayoutStateFuture.isInterruptRequested();
    boolean isInterruptible = mLayoutStateRef != null && mLayoutStateRef.isInterruptible();

    return isInterruptible && isInterruptRequested;
  }

  boolean isLayoutReleased() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isReleased();
  }

  public void markLayoutUninterruptible() {
    if (mLayoutStateRef != null) {
      mLayoutStateRef.setInterruptible(false);
    }
  }

  void markLayoutStarted() {
    if (mIsLayoutStarted) {
      throw new IllegalStateException(
          "Duplicate layout of a component: "
              + (mComponentTree != null ? mComponentTree.getRoot() : null));
    }
    mIsLayoutStarted = true;
  }

  public @Nullable DiffNode getCurrentDiffTree() {
    return mCurrentDiffTree;
  }

  void setNestedTreeDiffNode(@Nullable DiffNode diff) {
    mHasNestedTreeDiffNodeSet = true;
    mCurrentNestedTreeDiffNode = diff;
  }

  boolean hasNestedTreeDiffNodeSet() {
    return mHasNestedTreeDiffNodeSet;
  }

  public @Nullable DiffNode consumeNestedTreeDiffNode() {
    final DiffNode node = mCurrentNestedTreeDiffNode;
    mCurrentNestedTreeDiffNode = null;
    mHasNestedTreeDiffNodeSet = false;
    return node;
  }

  boolean isInternalNodeReuseEnabled() {
    return mComponentTree != null && mComponentTree.isInternalNodeReuseEnabled();
  }

  private String getDebugString() {
    final StringBuilder builder = new StringBuilder();
    final List<String> keys = new ArrayList<>(mGlobalKeyToScopedInfo.keySet());

    // Sorts the keys by length so the keys closer to the root are in the beginning.
    Collections.sort(
        keys,
        new Comparator<String>() {
          public int compare(String o1, String o2) {
            return Integer.compare(o1.length(), o2.length());
          }
        });

    for (String key : keys) {
      builder.append("\n  ").append(key);
    }

    // Truncate at 200 characters.
    String string = builder.substring(0, Math.min(builder.length(), 200));

    return String.format("\nsize: %d\nkeys: %s", keys.size(), string);
  }

  /** Remove scoped info of components which are not part of the hierarchy. */
  public void prune() {
    final Iterator<Map.Entry<String, ScopedComponentInfo>> iterator =
        mGlobalKeyToScopedInfo.entrySet().iterator();
    if (wasReconciled) {
      while (iterator.hasNext()) {
        Map.Entry<String, ScopedComponentInfo> info = iterator.next();
        if (!info.getValue().isBeingUsed()) {
          iterator.remove();
        }
      }
    }
  }

  private void checkIfFrozen() {
    if (mIsFrozen) {
      throw new IllegalStateException(
          "Cannot modify this LayoutStateContext, it's already been committed.");
    }
  }

  boolean isFrozen() {
    return mIsFrozen;
  }

  StateHandler getStateHandler() {
    return mStateHandler;
  }
}
