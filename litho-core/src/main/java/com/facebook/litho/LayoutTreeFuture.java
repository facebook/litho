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

public class LayoutTreeFuture extends TreeFuture<LayoutState> {
  private final LithoResolutionResult mLithoResolutionResult;
  private final ComponentContext mComponentContext;
  private final Component mRootComponent;
  private final @Nullable LayoutState mCurrentLayoutState;
  private final @Nullable DiffNode mDiffTreeRoot;
  private final @Nullable PerfEvent mLogLayoutStatePerfEvent;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final int mComponentTreeId;
  private final int mLayoutVersion;

  public LayoutTreeFuture(
      final LithoResolutionResult lithoResolutionResult,
      final ComponentContext componentContext,
      final Component component,
      final @Nullable LayoutState currentLayoutState,
      final @Nullable DiffNode diffTreeRoot,
      final @Nullable PerfEvent logLayoutStatePerfEvent,
      final int widthSpec,
      final int heightSpec,
      final int componentTreeId,
      final int layoutVersion) {
    super(false);

    mLithoResolutionResult = lithoResolutionResult;
    mComponentContext = componentContext;
    mRootComponent = component;
    mCurrentLayoutState = currentLayoutState;
    mDiffTreeRoot = diffTreeRoot;
    mLogLayoutStatePerfEvent = logLayoutStatePerfEvent;
    mWidthSpec = widthSpec;
    mHeightSpec = heightSpec;
    mComponentTreeId = componentTreeId;
    mLayoutVersion = layoutVersion;
  }

  @Override
  protected LayoutState calculate() {
    final LithoNode node = mLithoResolutionResult.node;
    final TreeState treeState = mLithoResolutionResult.treeState;
    final MeasuredResultCache renderPhaseCache = mLithoResolutionResult.cache;

    final LayoutState layoutState =
        new LayoutState(
            mComponentContext,
            mRootComponent,
            treeState,
            mCurrentLayoutState,
            mWidthSpec,
            mHeightSpec,
            mComponentTreeId);

    final LayoutStateContext lsc =
        new LayoutStateContext(
            new MeasuredResultCache(renderPhaseCache),
            mComponentContext,
            treeState,
            mComponentContext.getComponentTree(),
            mLayoutVersion,
            mDiffTreeRoot,
            this);

    if (mLogLayoutStatePerfEvent != null) {
      lsc.setPerfEvent(mLogLayoutStatePerfEvent);
    }

    final CalculationStateContext prevContext = mComponentContext.getCalculationStateContext();

    try {
      mComponentContext.setLayoutStateContext(lsc);

      final @Nullable LithoLayoutResult root =
          Layout.measureTree(
              lsc,
              mComponentContext.getAndroidContext(),
              node,
              mWidthSpec,
              mHeightSpec,
              mLogLayoutStatePerfEvent);

      layoutState.mLayoutResult = root;

      if (mLogLayoutStatePerfEvent != null) {
        mLogLayoutStatePerfEvent.markerPoint("start_collect_results");
      }

      LayoutState.setSizeAfterMeasureAndCollectResults(mComponentContext, lsc, layoutState);

      if (mLogLayoutStatePerfEvent != null) {
        mLogLayoutStatePerfEvent.markerPoint("end_collect_results");
      }
    } finally {
      mComponentContext.setCalculationStateContext(prevContext);
    }

    return layoutState;
  }

  @Override
  protected LayoutState resumeCalculation(LayoutState partialResult) {
    throw new UnsupportedOperationException("LayoutTreeFuture cannot be resumed.");
  }
}
