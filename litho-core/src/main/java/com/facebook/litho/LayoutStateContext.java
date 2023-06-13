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
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, the LayoutState reference is nullified. Using a wrapper instead of passing
 * the instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutStateContext implements CalculationStateContext {

  private final int mComponentTreeId;
  private @Nullable TreeState mTreeState;
  private @Nullable TreeFuture mLayoutStateFuture;
  private @Nullable DiffNode mCurrentDiffTree;
  private @Nullable ComponentContext mRootComponentContext;
  private final int mLayoutVersion;
  private final int mRootComponentId;
  private final boolean mIsAccessibilityEnabled;
  private @Nullable ArrayList<Pair<String, EventHandler<?>>> mCreatedEventHandlers = null;

  private @Nullable DiffNode mCurrentNestedTreeDiffNode;
  private boolean mIsReleased = false;

  private @Nullable PerfEvent mPerfEvent;

  private final MeasuredResultCache mCache;

  private final String mThreadCreatedOn;
  private final List<String> mThreadReleasedOn = new LinkedList<>();
  private final List<String> mThreadResumedOn = new LinkedList<>();

  LayoutStateContext(
      final int componentTreeId,
      final MeasuredResultCache cache,
      final ComponentContext rootComponentContext,
      final TreeState treeState,
      final int layoutVersion,
      final int rootComponentId,
      final boolean isAccessibilityEnabled,
      final @Nullable DiffNode currentDiffTree,
      final @Nullable TreeFuture layoutStateFuture) {
    mComponentTreeId = componentTreeId;
    mCache = cache;
    mRootComponentContext = rootComponentContext;
    mTreeState = treeState;
    mLayoutVersion = layoutVersion;
    mRootComponentId = rootComponentId;
    mCurrentDiffTree = currentDiffTree;
    mLayoutStateFuture = layoutStateFuture;
    mIsAccessibilityEnabled = isAccessibilityEnabled;
    mThreadCreatedOn = Thread.currentThread().getName();
  }

  void releaseReference() {
    mTreeState = null;
    mLayoutStateFuture = null;
    mCurrentDiffTree = null;
    mRootComponentContext = null;
    mPerfEvent = null;
    mThreadReleasedOn.add(Thread.currentThread().getName());
    mIsReleased = true;
  }

  /**
   * Returns true if this layout associated with this instance is completed and no longer in use.
   */
  public boolean isReleased() {
    return mIsReleased;
  }

  /** Returns the root component-context for the entire tree. */
  @Nullable
  ComponentContext getRootComponentContext() {
    return mRootComponentContext;
  }

  /** Returns the {@link ComponentTree} id associated to this Layout phase context. */
  @Override
  public int getTreeId() {
    return mComponentTreeId;
  }

  @Override
  public int getLayoutVersion() {
    return mLayoutVersion;
  }

  @Override
  public int getRootComponentId() {
    return mRootComponentId;
  }

  @Override
  public boolean isFutureReleased() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isReleased();
  }

  @Override
  public @Nullable TreeFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  public @Nullable DiffNode getCurrentDiffTree() {
    return mCurrentDiffTree;
  }

  void setNestedTreeDiffNode(@Nullable DiffNode diff) {
    mCurrentNestedTreeDiffNode = diff;
  }

  boolean hasNestedTreeDiffNodeSet() {
    return mCurrentNestedTreeDiffNode != null;
  }

  public @Nullable DiffNode consumeNestedTreeDiffNode() {
    final DiffNode node = mCurrentNestedTreeDiffNode;
    mCurrentNestedTreeDiffNode = null;
    return node;
  }

  @Override
  public TreeState getTreeState() {
    return Preconditions.checkNotNull(mTreeState);
  }

  @Override
  public MeasuredResultCache getCache() {
    return mCache;
  }

  @Nullable
  public PerfEvent getPerfEvent() {
    return mPerfEvent;
  }

  public void setPerfEvent(@Nullable PerfEvent perfEvent) {
    mPerfEvent = perfEvent;
  }

  public void markLayoutResumed() {
    mThreadResumedOn.add(Thread.currentThread().getName());
  }

  @Override
  public void recordEventHandler(String globalKey, EventHandler<?> eventHandler) {
    if (mCreatedEventHandlers == null) {
      mCreatedEventHandlers = new ArrayList<>();
    }
    mCreatedEventHandlers.add(new Pair<>(globalKey, eventHandler));
  }

  @Override
  public @Nullable List<Pair<String, EventHandler<?>>> getCreatedEventHandlers() {
    return mCreatedEventHandlers;
  }

  @Override
  public boolean isAccessibilityEnabled() {
    return mIsAccessibilityEnabled;
  }

  public String getLifecycleDebugString() {
    StringBuilder builder = new StringBuilder();

    builder
        .append("LayoutStateContext was created on: ")
        .append(mThreadCreatedOn)
        .append("\n")
        .append("LayoutStateContext was released on: [");

    for (String thread : mThreadReleasedOn) {
      builder.append(thread).append(" ,");
    }

    builder.append("]").append("LayoutStateContext was resumed on: [");

    for (String thread : mThreadResumedOn) {
      builder.append(thread).append(" ,");
    }

    builder.append("]");

    return builder.toString();
  }
}
