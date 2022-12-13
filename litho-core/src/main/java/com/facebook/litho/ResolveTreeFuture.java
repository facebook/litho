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

import static com.facebook.litho.ComponentTree.SIZE_UNINITIALIZED;

import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stats.LithoStats;
import java.util.List;

public class ResolveTreeFuture extends TreeFuture<ResolveResult> {
  private final ComponentContext mComponentContext;
  private final Component mComponent;
  private final TreeState mTreeState;
  private final @Nullable LithoNode mCurrentRootNode;
  private final @Nullable PerfEvent mPerfEvent;
  private final int mResolveVersion;

  // TODO(T137275959): Refactor sync render logic to remove sizes from resolved tree future
  @Deprecated private final int mSyncWidthSpec;
  @Deprecated private final int mSyncHeightSpec;

  // Only needed for resume logic.
  private @Nullable ResolveStateContext mResolveStateContextForResume;

  public ResolveTreeFuture(
      final ComponentContext c,
      final Component component,
      final TreeState treeState,
      final @Nullable LithoNode currentRootNode,
      final @Nullable PerfEvent perfEvent,
      final int resolveVersion,
      final boolean useCancellableFutures) {
    this(
        c,
        component,
        treeState,
        currentRootNode,
        perfEvent,
        resolveVersion,
        useCancellableFutures,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED);
  }

  /**
   * TODO(T137275959)
   *
   * @deprecated Refactor sync render logic to remove sizes from resolved tree future
   */
  @Deprecated
  public ResolveTreeFuture(
      final ComponentContext c,
      final Component component,
      final TreeState treeState,
      final @Nullable LithoNode currentRootNode,
      final @Nullable PerfEvent perfEvent,
      final int resolveVersion,
      final boolean useCancellableFutures,
      final int syncWidthSpec,
      final int syncHeightSpec) {
    super(useCancellableFutures);
    mComponentContext = c;
    mComponent = component;
    mTreeState = treeState;
    mCurrentRootNode = currentRootNode;
    mPerfEvent = perfEvent;
    mResolveVersion = resolveVersion;
    mSyncWidthSpec = syncWidthSpec;
    mSyncHeightSpec = syncHeightSpec;

    // Allow interrupt to happen during tryRegisterForResponse when config is enabled.
    mEnableEarlyInterrupt = ComponentsConfiguration.isInterruptEarlyWithSplitFuturesEnabled;
  }

  @Override
  protected ResolveResult calculate() {
    LithoStats.incrementResolveCount();

    final ResolveStateContext rsc =
        new ResolveStateContext(
            new MeasuredResultCache(),
            mTreeState,
            mResolveVersion,
            this,
            mCurrentRootNode,
            mPerfEvent);

    final @Nullable CalculationStateContext previousStateContext =
        mComponentContext.getCalculationStateContext();

    final @Nullable LithoNode node;
    try {
      mComponentContext.setRenderStateContext(rsc);
      node = Resolver.createResolvedTree(rsc, mComponentContext, mComponent);
    } finally {
      mComponentContext.setCalculationStateContext(previousStateContext);
    }

    if (rsc.isLayoutInterrupted()) {
      mResolveStateContextForResume = rsc;
    } else {
      rsc.getCache().freezeCache();
    }

    return new ResolveResult(
        node,
        mComponentContext,
        mComponent,
        rsc.getCache(),
        mTreeState,
        rsc.isLayoutInterrupted(),
        mResolveVersion,
        rsc.getCreatedEventHandlers());
  }

  @Override
  protected ResolveResult resumeCalculation(ResolveResult partialResult) {

    LithoStats.incrementResumeCount();

    if (!partialResult.isPartialResult) {
      throw new IllegalStateException("Cannot resume a non-partial result");
    }

    if (partialResult.node == null) {
      throw new IllegalStateException("Cannot resume a partial result with a null node");
    }

    if (mResolveStateContextForResume == null) {
      throw new IllegalStateException("RenderStateContext cannot be null during resume");
    }

    final @Nullable CalculationStateContext previousStateContext =
        mComponentContext.getCalculationStateContext();

    final @Nullable LithoNode node;
    try {
      mComponentContext.setRenderStateContext(mResolveStateContextForResume);
      node = Resolver.resumeResolvingTree(mResolveStateContextForResume, partialResult.node);
    } finally {
      mComponentContext.setCalculationStateContext(previousStateContext);
    }

    mResolveStateContextForResume.getCache().freezeCache();
    final List<Pair<String, EventHandler>> createdEventHandlers =
        mResolveStateContextForResume.getCreatedEventHandlers();
    mResolveStateContextForResume = null;

    return new ResolveResult(
        node,
        mComponentContext,
        partialResult.component,
        partialResult.consumeCache(),
        partialResult.treeState,
        false,
        mResolveVersion,
        createdEventHandlers);
  }

  @Override
  public boolean isEquivalentTo(TreeFuture that) {
    if (!(that instanceof ResolveTreeFuture)) {
      return false;
    }

    final ResolveTreeFuture thatRtf = (ResolveTreeFuture) that;

    if (mComponent.getId() != thatRtf.mComponent.getId()) {
      return false;
    }

    if (mComponentContext.getTreeProps() != thatRtf.mComponentContext.getTreeProps()) {
      return false;
    }

    // TODO(T137275959): delete on refactor
    if (mSyncWidthSpec != thatRtf.mSyncWidthSpec) {
      return false;
    }

    // TODO(T137275959): delete on refactor
    if (mSyncHeightSpec != thatRtf.mSyncHeightSpec) {
      return false;
    }

    return true;
  }
}
