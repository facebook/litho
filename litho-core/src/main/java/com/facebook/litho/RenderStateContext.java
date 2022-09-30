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
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class RenderStateContext implements CalculationStateContext {
  private @Nullable Map<Integer, LithoNode> mComponentIdToWillRenderLayout;

  private boolean mIsInterruptible = true;
  private @Nullable TreeState mTreeState;
  private final MeasuredResultCache mCache;
  private @Nullable ComponentTree.LayoutStateFuture mLayoutStateFuture;
  private @Nullable LayoutStateContext mLayoutStateContext = null;
  private int mLayoutVersion;

  @Deprecated
  public RenderStateContext(
      final @Nullable ComponentTree.LayoutStateFuture layoutStateFuture,
      final TreeState treeState,
      final LayoutStateContext layoutStateContext) { // Temp LSC member
    mLayoutStateFuture = layoutStateFuture;
    mTreeState = treeState;
    mCache = new MeasuredResultCache();
    mLayoutStateContext = layoutStateContext;
  }

  RenderStateContext(
      final MeasuredResultCache cache,
      final TreeState treeState,
      final int layoutVersion,
      final @Nullable ComponentTree.LayoutStateFuture layoutStateFuture) {
    mCache = cache;
    mTreeState = treeState;
    mLayoutVersion = layoutVersion;
    mLayoutStateFuture = layoutStateFuture;
  }

  // Temp workaround for implementing split render and layout. Do not add usages to this method.
  // It will be removed soon.
  @Deprecated
  void setLayoutStateContext(final LayoutStateContext layoutStateContext) {
    mLayoutStateContext = layoutStateContext;
  }

  // Temp workaround for implementing split render and layout. Do not add usages to this method.
  // It will be removed soon.
  @Deprecated
  public LayoutStateContext getLayoutStateContext() {
    return Preconditions.checkNotNull(mLayoutStateContext);
  }

  @Override
  public int getLayoutVersion() {
    return mLayoutVersion;
  }

  @Nullable
  LithoNode consumeLayoutCreatedInWillRender(int componentId) {
    if (mComponentIdToWillRenderLayout != null) {
      return mComponentIdToWillRenderLayout.remove(componentId);
    } else {
      return null;
    }
  }

  @Nullable
  LithoNode getLayoutCreatedInWillRender(int componentId) {
    if (mComponentIdToWillRenderLayout != null) {
      return mComponentIdToWillRenderLayout.get(componentId);
    } else {
      return null;
    }
  }

  void setLayoutCreatedInWillRender(int componentId, final @Nullable LithoNode node) {
    if (mComponentIdToWillRenderLayout == null) {
      mComponentIdToWillRenderLayout = new HashMap<>();
    }
    mComponentIdToWillRenderLayout.put(componentId, node);
  }

  boolean isInterruptible() {
    return mIsInterruptible;
  }

  boolean isLayoutInterrupted() {
    boolean isInterruptRequested =
        mLayoutStateFuture != null
            && mLayoutStateFuture.isInterruptRequested()
            && !ThreadUtils.isMainThread();

    return isInterruptible() && isInterruptRequested;
  }

  @Override
  public boolean isFutureReleased() {
    return mLayoutStateFuture != null && mLayoutStateFuture.isReleased();
  }

  @Nullable
  @Override
  public ComponentTree.LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  public void markLayoutUninterruptible() {
    mIsInterruptible = false;
  }

  @Override
  public TreeState getTreeState() {
    if (mTreeState == null) {
      throw new IllegalStateException("Attempt to fetch TreeState after release");
    }

    return mTreeState;
  }

  @Override
  public MeasuredResultCache getCache() {
    return mCache;
  }

  public void release() {
    mComponentIdToWillRenderLayout = null;
    mLayoutStateFuture = null;
    mTreeState = null;
  }

  @VisibleForTesting
  public void setLayoutStateFuture(@Nullable ComponentTree.LayoutStateFuture layoutStateFuture) {
    mLayoutStateFuture = layoutStateFuture;
  }
}
