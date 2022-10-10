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

public class RenderTreeFuture extends TreeFuture<LithoResolutionResult> {
  private final ComponentContext mComponentContext;
  private final Component mComponent;
  private final TreeState mTreeState;
  private final @Nullable LithoNode mCurrentRootNode;
  private final @Nullable PerfEvent mPerfEvent;
  private final int mLayoutVersion;

  // Only needed for resume logic.
  private @Nullable RenderStateContext mRenderStateContextForResume;

  public RenderTreeFuture(
      final ComponentContext c,
      final Component component,
      final TreeState treeState,
      final @Nullable LithoNode currentRootNode,
      final @Nullable PerfEvent perfEvent,
      final int layoutVersion) {
    super(true);

    mComponentContext = c;
    mComponent = component;
    mTreeState = treeState;
    mCurrentRootNode = currentRootNode;
    mPerfEvent = perfEvent;
    mLayoutVersion = layoutVersion;
  }

  @Override
  protected LithoResolutionResult calculate() {
    final RenderStateContext rsc =
        new RenderStateContext(
            new MeasuredResultCache(),
            mTreeState,
            mLayoutVersion,
            this,
            mCurrentRootNode,
            mPerfEvent);

    final @Nullable CalculationStateContext previousStateContext =
        mComponentContext.getCalculationStateContext();

    final @Nullable LithoNode node;
    try {
      mComponentContext.setRenderStateContext(rsc);
      node = ResolvedTree.createResolvedTree(rsc, mComponentContext, mComponent);
    } finally {
      mComponentContext.setCalculationStateContext(previousStateContext);
    }

    if (rsc.isLayoutInterrupted()) {
      mRenderStateContextForResume = rsc;
    } else {
      rsc.getCache().freezeCache();
    }

    return new LithoResolutionResult(node, rsc.getCache(), mTreeState, rsc.isLayoutInterrupted());
  }

  @Override
  protected LithoResolutionResult resumeCalculation(LithoResolutionResult partialResult) {
    if (!partialResult.isPartialResult) {
      throw new IllegalStateException("Cannot resume a non-partial result");
    }

    if (partialResult.node == null) {
      throw new IllegalStateException("Cannot resume a partial result with a null node");
    }

    if (mRenderStateContextForResume == null) {
      throw new IllegalStateException("RenderStateContext cannot be null during resume");
    }

    final @Nullable CalculationStateContext previousStateContext =
        mComponentContext.getCalculationStateContext();

    final @Nullable LithoNode node;
    try {
      mComponentContext.setRenderStateContext(mRenderStateContextForResume);
      node = ResolvedTree.resumeResolvingTree(mRenderStateContextForResume, partialResult.node);
    } finally {
      mComponentContext.setCalculationStateContext(previousStateContext);
    }

    mRenderStateContextForResume.getCache().freezeCache();
    mRenderStateContextForResume = null;

    return new LithoResolutionResult(node, partialResult.cache, partialResult.treeState, false);
  }
}
