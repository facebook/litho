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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.ComponentTree.SIZE_UNINITIALIZED;

import android.util.Pair;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import com.facebook.litho.cancellation.ExecutionModeKt;
import com.facebook.litho.cancellation.RequestMetadataSupplier;
import com.facebook.litho.cancellation.ResolveMetadata;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stats.LithoStats;
import java.util.List;

public class ResolveTreeFuture extends TreeFuture<ResolveResult>
    implements RequestMetadataSupplier<ResolveMetadata> {
  private final ComponentContext mComponentContext;
  private final Component mComponent;
  private final TreeState mTreeState;
  private final int mComponentTreeId;
  private final @Nullable LithoNode mCurrentRootNode;
  private final @Nullable PerfEvent mPerfEvent;
  private final int mResolveVersion;
  private final @Nullable String mExtraAttribution;

  // TODO(T137275959): Refactor sync render logic to remove sizes from resolved tree future
  @Deprecated private final int mSyncWidthSpec;
  @Deprecated private final int mSyncHeightSpec;

  private final ResolveMetadata mResolveMetadata;

  public ResolveTreeFuture(
      final ComponentContext c,
      final Component component,
      final TreeState treeState,
      final @Nullable LithoNode currentRootNode,
      final @Nullable PerfEvent perfEvent,
      final int resolveVersion,
      final boolean useCancellableFutures,
      final int componentTreeId,
      final @Nullable String extraAttribution,
      final int source) {
    this(
        c,
        component,
        treeState,
        currentRootNode,
        perfEvent,
        resolveVersion,
        useCancellableFutures,
        SIZE_UNINITIALIZED,
        SIZE_UNINITIALIZED,
        componentTreeId,
        extraAttribution,
        source);
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
      final int syncHeightSpec,
      final int componentTreeId,
      final @Nullable String extraAttribution,
      final int source) {
    super(useCancellableFutures);
    mComponentContext = c;
    mComponent = component;
    mComponentTreeId = componentTreeId;
    mTreeState = treeState;
    mCurrentRootNode = currentRootNode;
    mPerfEvent = perfEvent;
    mResolveVersion = resolveVersion;
    mExtraAttribution = extraAttribution;
    mSyncWidthSpec = syncWidthSpec;
    mSyncHeightSpec = syncHeightSpec;
    mResolveMetadata =
        new ResolveMetadata(
            resolveVersion,
            component.getId(),
            mComponentContext.getTreeProps(),
            ExecutionModeKt.getExecutionMode(source),
            source);

    // Allow interrupt to happen during tryRegisterForResponse when config is enabled.
    mEnableEarlyInterrupt = ComponentsConfiguration.isInterruptEarlyWithSplitFuturesEnabled;
  }

  @Override
  protected ResolveResult calculate() {
    return resolve(
        mComponentContext,
        mComponent,
        mTreeState,
        mResolveVersion,
        mComponentTreeId,
        mCurrentRootNode,
        mExtraAttribution,
        this,
        mPerfEvent);
  }

  @Override
  public int getVersion() {
    return mResolveMetadata.getLocalVersion();
  }

  @Override
  public String getDescription() {
    return "resolve";
  }

  public ResolveMetadata getMetadata() {
    return mResolveMetadata;
  }

  @Override
  protected ResolveResult resumeCalculation(ResolveResult partialResult) {
    return resume(partialResult, mExtraAttribution);
  }

  @Override
  public boolean isEquivalentTo(TreeFuture that) {
    if (!(that instanceof ResolveTreeFuture)) {
      return false;
    }

    final ResolveTreeFuture thatRtf = (ResolveTreeFuture) that;

    if (!mResolveMetadata.isEquivalentTo(thatRtf.mResolveMetadata)) {
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

  /** Function which resolves a new RenderResult. */
  public static ResolveResult resolve(
      final ComponentContext context,
      final Component component,
      final TreeState state,
      final int version,
      final int componentTreeId,
      final @Nullable LithoNode currentRootNode,
      final @Nullable String extraAttribution,
      final @Nullable TreeFuture future,
      final @Nullable PerfEvent perfEventLogger) {
    LithoStats.incrementResolveCount();

    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        if (extraAttribution != null) {
          ComponentsSystrace.beginSection("extra:" + extraAttribution);
        }
        ComponentsSystrace.beginSectionWithArgs("resolveTree:" + component.getSimpleName())
            .arg("treeId", componentTreeId)
            .arg("rootId", component.getId())
            .flush();
      }

      final ResolveStateContext rsc =
          new ResolveStateContext(
              new MeasuredResultCache(),
              state,
              version,
              component.getId(),
              AccessibilityUtils.isAccessibilityEnabled(
                  (AccessibilityManager)
                      context.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE)),
              future,
              currentRootNode,
              perfEventLogger,
              context.getLogger());

      final @Nullable CalculationStateContext previousStateContext =
          context.getCalculationStateContext();

      final @Nullable LithoNode node;
      try {
        context.setRenderStateContext(rsc);
        node = Resolver.resolveTree(rsc, context, component);
      } finally {
        context.setCalculationStateContext(previousStateContext);
      }

      final @Nullable List<Attachable> attachables;
      if (rsc.isLayoutInterrupted()) {
        attachables = null;
      } else {
        attachables = context.isNullNodeEnabled() ? Resolver.collectAttachables(node) : null;
        rsc.getCache().freezeCache();
      }

      return new ResolveResult(
          node,
          context,
          component,
          rsc.getCache(),
          state,
          rsc.isLayoutInterrupted(),
          version,
          rsc.getCreatedEventHandlers(),
          attachables,
          rsc.isLayoutInterrupted() ? rsc : null);

    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
        if (extraAttribution != null) {
          ComponentsSystrace.endSection();
        }
      }
    }
  }

  public static ResolveResult resume(
      final ResolveResult partialResult, final @Nullable String extraAttribution) {

    LithoStats.incrementResumeCount();

    final ComponentContext context = partialResult.context;
    final Component component = partialResult.component;
    final int resolveVersion = partialResult.version;

    if (!partialResult.isPartialResult()) {
      throw new IllegalStateException("Cannot resume a non-partial result");
    }

    if (partialResult.node == null) {
      throw new IllegalStateException("Cannot resume a partial result with a null node");
    }

    if (partialResult.contextForResuming == null) {
      throw new IllegalStateException("RenderStateContext cannot be null during resume");
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        if (extraAttribution != null) {
          ComponentsSystrace.beginSection("extra:" + extraAttribution);
        }
        ComponentsSystrace.beginSection("resume:" + component.getSimpleName());
      }

      final @Nullable CalculationStateContext previousStateContext =
          context.getCalculationStateContext();

      final @Nullable LithoNode node;
      try {
        context.setRenderStateContext(partialResult.contextForResuming);
        node = Resolver.resumeResolvingTree(partialResult.contextForResuming, partialResult.node);
      } finally {
        context.setCalculationStateContext(previousStateContext);
      }

      final @Nullable List<Attachable> attachables =
          context.isNullNodeEnabled() ? Resolver.collectAttachables(node) : null;

      partialResult.contextForResuming.getCache().freezeCache();
      final List<Pair<String, EventHandler<?>>> createdEventHandlers =
          partialResult.contextForResuming.getCreatedEventHandlers();

      return new ResolveResult(
          node,
          context,
          partialResult.component,
          partialResult.consumeCache(),
          partialResult.treeState,
          false,
          resolveVersion,
          createdEventHandlers,
          attachables,
          null);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
        if (extraAttribution != null) {
          ComponentsSystrace.endSection();
        }
      }
    }
  }

  interface ExecutionListener {

    void onPreExecution(int version);

    void onPostExecution(int version, boolean released);
  }
}
