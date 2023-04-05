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

import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import com.facebook.litho.cancellation.ExecutionModeKt;
import com.facebook.litho.cancellation.LayoutMetadata;
import com.facebook.litho.cancellation.RequestMetadataSupplier;
import com.facebook.litho.stats.LithoStats;

public class LayoutTreeFuture extends TreeFuture<LayoutState>
    implements RequestMetadataSupplier<LayoutMetadata> {
  private final ResolveResult mResolveResult;
  private final @Nullable LayoutState mCurrentLayoutState;
  private final @Nullable DiffNode mDiffTreeRoot;
  private final @Nullable PerfEvent mLogLayoutStatePerfEvent;
  private final int mWidthSpec;
  private final int mHeightSpec;
  private final int mComponentTreeId;
  private final int mLayoutVersion;
  private final boolean mIsLayoutDiffingEnabled;
  private LayoutMetadata mLayoutMetadata;

  public LayoutTreeFuture(
      final ResolveResult resolveResult,
      final @Nullable LayoutState currentLayoutState,
      final @Nullable DiffNode diffTreeRoot,
      final @Nullable PerfEvent logLayoutStatePerfEvent,
      final int widthSpec,
      final int heightSpec,
      final int componentTreeId,
      final int layoutVersion,
      final boolean isLayoutDiffingEnabled,
      @RenderSource final int source) {
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
    mLayoutMetadata =
        new LayoutMetadata(
            layoutVersion,
            widthSpec,
            heightSpec,
            resolveResult,
            ExecutionModeKt.getExecutionMode(source));
  }

  @Override
  public String getDescription() {
    return "layout";
  }

  @Override
  public int getVersion() {
    return mLayoutMetadata.getLocalVersion();
  }

  @Override
  public LayoutMetadata getMetadata() {
    return mLayoutMetadata;
  }

  @Override
  protected LayoutState calculate() {
    return layout(
        mResolveResult,
        mWidthSpec,
        mHeightSpec,
        mLayoutVersion,
        mComponentTreeId,
        mIsLayoutDiffingEnabled,
        mCurrentLayoutState,
        mDiffTreeRoot,
        this,
        mLogLayoutStatePerfEvent);
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

    return mLayoutMetadata.isEquivalentTo(((LayoutTreeFuture) that).mLayoutMetadata);
  }

  /** Function to calculate a new layout. */
  static LayoutState layout(
      final ResolveResult resolveResult,
      final int widthSpec,
      final int heightSpec,
      final int version,
      final int treeId,
      final boolean isLayoutDiffingEnabled,
      final @Nullable LayoutState currentLayoutState,
      final @Nullable DiffNode diffTreeRoot,
      final @Nullable TreeFuture future,
      final @Nullable PerfEvent perfEventLogger) {

    LithoStats.incrementLayoutCount();

    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs(
                "layoutTree:" + resolveResult.component.getSimpleName())
            .arg("treeId", treeId)
            .arg("rootId", resolveResult.component.getId())
            .arg("widthSpec", SizeSpec.toString(widthSpec))
            .arg("heightSpec", SizeSpec.toString(heightSpec))
            .flush();
      }

      final LithoNode node = resolveResult.node;
      final TreeState treeState = resolveResult.treeState;
      final MeasuredResultCache renderPhaseCache = resolveResult.consumeCache();
      final ComponentContext c = resolveResult.context;

      final LayoutStateContext lsc =
          new LayoutStateContext(
              new MeasuredResultCache(renderPhaseCache),
              c,
              treeState,
              version,
              resolveResult.component.getId(),
              AccessibilityUtils.isAccessibilityEnabled(
                  (AccessibilityManager)
                      c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE)),
              diffTreeRoot,
              future);

      final LayoutState layoutState =
          new LayoutState(
              c,
              resolveResult.component,
              treeState,
              resolveResult.attachables,
              currentLayoutState,
              node,
              widthSpec,
              heightSpec,
              treeId,
              isLayoutDiffingEnabled,
              lsc.isAccessibilityEnabled());

      if (perfEventLogger != null) {
        lsc.setPerfEvent(perfEventLogger);
      }

      final CalculationStateContext prevContext = c.getCalculationStateContext();

      try {
        c.setLayoutStateContext(lsc);

        final @Nullable LithoLayoutResult root =
            Layout.measureTree(
                lsc, c.getAndroidContext(), node, widthSpec, heightSpec, perfEventLogger);

        layoutState.mLayoutResult = root;

        if (perfEventLogger != null) {
          perfEventLogger.markerPoint("start_collect_results");
        }

        LayoutState.setSizeAfterMeasureAndCollectResults(c, lsc, layoutState);

        if (perfEventLogger != null) {
          perfEventLogger.markerPoint("end_collect_results");
        }

        layoutState.setCreatedEventHandlers(
            CommonUtils.mergeLists(
                resolveResult.createdEventHandlers, lsc.getCreatedEventHandlers()));
      } finally {
        c.setCalculationStateContext(prevContext);
        lsc.releaseReference();
      }

      LithoStats.incrementComponentCalculateLayoutCount();
      if (ThreadUtils.isMainThread()) {
        LithoStats.incrementComponentCalculateLayoutOnUICount();
      }

      return layoutState;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }
}
