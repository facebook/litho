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
import com.facebook.litho.stats.LithoStats;

public class LayoutTreeFuture extends TreeFuture<LayoutState> {
  private final ResolveResult mResolveResult;
  private final @Nullable LayoutState mCurrentLayoutState;
  private final @Nullable DiffNode mDiffTreeRoot;
  private final @Nullable PerfEvent mLogLayoutStatePerfEvent;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final int mComponentTreeId;
  private final int mLayoutVersion;
  private final boolean mIsLayoutDiffingEnabled;

  public LayoutTreeFuture(
      final ResolveResult resolveResult,
      final @Nullable LayoutState currentLayoutState,
      final @Nullable DiffNode diffTreeRoot,
      final @Nullable PerfEvent logLayoutStatePerfEvent,
      final int widthSpec,
      final int heightSpec,
      final int componentTreeId,
      final int layoutVersion,
      final boolean isLayoutDiffingEnabled) {
    super(false);

    mResolveResult = resolveResult;
    mCurrentLayoutState = currentLayoutState;
    mDiffTreeRoot = diffTreeRoot;
    mLogLayoutStatePerfEvent = logLayoutStatePerfEvent;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mComponentTreeId = componentTreeId;
    mLayoutVersion = layoutVersion;
    mIsLayoutDiffingEnabled = isLayoutDiffingEnabled;
  }

  @Override
  protected LayoutState calculate() {

    LithoStats.incrementLayoutCount();

    final LithoNode node = mResolveResult.node;
    final TreeState treeState = mResolveResult.treeState;
    final MeasuredResultCache renderPhaseCache = mResolveResult.consumeCache();
    final ComponentContext c = mResolveResult.context;

    final LayoutState layoutState =
        new LayoutState(
            c,
            mResolveResult.component,
            treeState,
            mCurrentLayoutState,
            node,
            mWidthSpec,
            mHeightSpec,
            mComponentTreeId,
            mIsLayoutDiffingEnabled);

    final LayoutStateContext lsc =
        new LayoutStateContext(
            new MeasuredResultCache(renderPhaseCache),
            c,
            treeState,
            c.getComponentTree(),
            mLayoutVersion,
            mDiffTreeRoot,
            this);

    if (mLogLayoutStatePerfEvent != null) {
      lsc.setPerfEvent(mLogLayoutStatePerfEvent);
    }

    final CalculationStateContext prevContext = c.getCalculationStateContext();

    try {
      c.setLayoutStateContext(lsc);

      final @Nullable LithoLayoutResult root =
          Layout.measureTree(
              lsc, c.getAndroidContext(), node, mWidthSpec, mHeightSpec, mLogLayoutStatePerfEvent);

      layoutState.mLayoutResult = root;

      if (mLogLayoutStatePerfEvent != null) {
        mLogLayoutStatePerfEvent.markerPoint("start_collect_results");
      }

      LayoutState.setSizeAfterMeasureAndCollectResults(c, lsc, layoutState);

      if (mLogLayoutStatePerfEvent != null) {
        mLogLayoutStatePerfEvent.markerPoint("end_collect_results");
      }

      layoutState.setCreatedEventHandlers(
          CommonUtils.mergeLists(
              mResolveResult.createdEventHandlers, lsc.getCreatedEventHandlers()));
    } finally {
      c.setCalculationStateContext(prevContext);
      lsc.releaseReference();
    }

    LithoStats.incrementComponentCalculateLayoutCount();
    if (ThreadUtils.isMainThread()) {
      LithoStats.incrementComponentCalculateLayoutOnUICount();
    }

    return layoutState;
  }

  @Override
  protected LayoutState resumeCalculation(LayoutState partialResult) {
    throw new UnsupportedOperationException("LayoutTreeFuture cannot be resumed.");
  }

  @Override
  public boolean isEquivalentTo(TreeFuture that) {
    if (!(that instanceof LayoutTreeFuture)) {
      return false;
    }

    final LayoutTreeFuture thatLtf = (LayoutTreeFuture) that;

    return mWidthSpec == thatLtf.mWidthSpec
        && mHeightSpec == thatLtf.mHeightSpec
        && mResolveResult == thatLtf.mResolveResult;
  }
}
