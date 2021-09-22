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
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.ComponentTree.LayoutStateFuture;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, the LayoutState reference is nullified. Using a wrapper instead of passing
 * the instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutStateContext {

  private final @Nullable ComponentTree mComponentTree;

  private @Nullable LayoutState mLayoutStateRef;
  private @Nullable StateHandler mStateHandler;
  private @Nullable LayoutStateFuture mLayoutStateFuture;
  private @Nullable Map<Integer, InternalNode> mComponentIdToWillRenderLayout;
  private @Nullable DiffNode mCurrentDiffTree;

  private @Nullable DiffNode mCurrentNestedTreeDiffNode;
  private boolean mHasNestedTreeDiffNodeSet = false;

  private boolean mIsLayoutStarted = false;
  private volatile boolean mIsFrozen = false;

  @Deprecated
  public static LayoutStateContext getTestInstance(ComponentContext c) {
    final LayoutState layoutState = new LayoutState(c);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, new StateHandler(), c.getComponentTree(), null, null);
    layoutState.setLayoutStateContextForTest(layoutStateContext);
    return layoutStateContext;
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
    this(layoutState, new StateHandler(), componentTree, null, null);
  }

  LayoutStateContext(
      final LayoutState layoutState,
      final StateHandler stateHandler,
      final @Nullable ComponentTree componentTree,
      final @Nullable LayoutStateFuture layoutStateFuture,
      final @Nullable DiffNode currentDiffTree) {
    mLayoutStateRef = layoutState;
    mLayoutStateFuture = layoutStateFuture;
    mComponentTree = componentTree;
    mCurrentDiffTree = currentDiffTree;
    mStateHandler = stateHandler;
  }

  ScopedComponentInfo createScopedComponentInfo(
      final Component component,
      final ComponentContext scopedContext,
      final ComponentContext parentContext) {
    checkIfFrozen();

    final EventHandler<ErrorEvent> errorEventHandler =
        ComponentUtils.createOrGetErrorEventHandler(component, parentContext, scopedContext);

    return new ScopedComponentInfo(component, scopedContext, errorEventHandler);
  }

  @Nullable
  InternalNode consumeLayoutCreatedInWillRender(int componentId) {
    if (mComponentIdToWillRenderLayout != null) {
      return mComponentIdToWillRenderLayout.remove(componentId);
    } else {
      return null;
    }
  }

  @Nullable
  InternalNode getLayoutCreatedInWillRender(int componentId) {
    if (mComponentIdToWillRenderLayout != null) {
      return mComponentIdToWillRenderLayout.get(componentId);
    } else {
      return null;
    }
  }

  void setLayoutCreatedInWillRender(int componentId, final @Nullable InternalNode node) {
    if (mComponentIdToWillRenderLayout == null) {
      mComponentIdToWillRenderLayout = new HashMap<>();
    }
    mComponentIdToWillRenderLayout.put(componentId, node);
  }

  void releaseReference() {
    mLayoutStateRef = null;
    mStateHandler = null;
    mLayoutStateFuture = null;
    mCurrentDiffTree = null;
    mComponentIdToWillRenderLayout = null;
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

  private void checkIfFrozen() {
    if (mIsFrozen) {
      throw new IllegalStateException(
          "Cannot modify this LayoutStateContext, it's already been committed.");
    }
  }

  boolean isFrozen() {
    return mIsFrozen;
  }

  void freeze() {
    mIsFrozen = true;
  }

  StateHandler getStateHandler() {
    return Preconditions.checkNotNull(mStateHandler);
  }

  boolean useStatelessComponent() {
    return mComponentTree != null && mComponentTree.useStatelessComponent();
  }
}
