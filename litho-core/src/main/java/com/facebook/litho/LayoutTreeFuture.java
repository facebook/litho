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
import static com.facebook.litho.debug.LithoDebugEventAttributes.Root;
import static com.facebook.litho.debug.LithoDebugEventAttributes.RunsOnMainThread;

import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import com.facebook.litho.debug.DebugOverlay;
import com.facebook.litho.debug.LithoDebugEvent;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.LayoutCache;
import com.facebook.rendercore.debug.DebugEventAttribute;
import com.facebook.rendercore.debug.DebugEventDispatcher;
import java.util.LinkedHashMap;
import java.util.Map;

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

  public static final String DESCRIPTION = "layout";

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
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public int getVersion() {
    return mLayoutVersion;
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

    final LayoutTreeFuture thatLtf = (LayoutTreeFuture) that;

    return mWidthSpec == thatLtf.mWidthSpec
        && mHeightSpec == thatLtf.mHeightSpec
        && mResolveResult == thatLtf.mResolveResult;
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

    final @Nullable Integer traceId =
        DebugEventDispatcher.generateTraceIdentifier(LithoDebugEvent.ComponentTreeResolve);

    if (traceId != null) {
      final Map<String, Object> attributes = new LinkedHashMap<>();
      attributes.put(RunsOnMainThread, ThreadUtils.isMainThread());
      attributes.put(Root, resolveResult.component.getSimpleName());
      attributes.put(DebugEventAttribute.version, version);
      DebugEventDispatcher.beginTrace(
          traceId, LithoDebugEvent.Layout, String.valueOf(traceId), attributes);
    }

    final TreeState treeState = resolveResult.treeState;

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

      treeState.registerLayoutState();

      final LithoNode node = resolveResult.node;
      final MeasuredResultCache renderPhaseCache = resolveResult.consumeCache();
      final ComponentContext c = resolveResult.context;
      final LayoutCache layoutCache =
          currentLayoutState != null
              ? new LayoutCache(currentLayoutState.mLayoutCacheData)
              : new LayoutCache();
      final LithoLayoutContext lsc =
          new LithoLayoutContext(
              treeId,
              new MeasuredResultCache(renderPhaseCache),
              c,
              treeState,
              version,
              resolveResult.component.getId(),
              AccessibilityUtils.isAccessibilityEnabled(
                  (AccessibilityManager)
                      c.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE)),
              layoutCache,
              diffTreeRoot,
              future);

      final LayoutState layoutState =
          new LayoutState(
              c,
              resolveResult.component,
              treeState,
              resolveResult.outputs != null ? resolveResult.outputs.attachables : null,
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

      final CalculationContext prevContext = c.getCalculationStateContext();

      try {
        c.setLithoLayoutContext(lsc);

        @Nullable
        LithoLayoutResult root =
            Layout.measureTree(
                lsc, c.getAndroidContext(), node, widthSpec, heightSpec, perfEventLogger);

        if (root != null && c.shouldCacheLayouts()) {
          Layout.measurePendingSubtrees(c, root, root.getNode(), layoutState, lsc);
        }

        layoutState.mLayoutResult = root;
        layoutState.mLayoutCacheData = layoutCache.getWriteCacheData();

        if (perfEventLogger != null) {
          perfEventLogger.markerPoint("start_collect_results");
        }

        LayoutState.setSizeAfterMeasureAndCollectResults(c, lsc, layoutState);

        if (perfEventLogger != null) {
          perfEventLogger.markerPoint("end_collect_results");
        }

        if (c.shouldCacheLayouts() && root != null) {
          // release diff node and layout state context since they are no longer required
          // this is not required in the control behaviour because the layout results is
          // cleared from the LayoutState before after collect-results.
          root.releaseLayoutPhaseData();
        }

        layoutState.setCreatedEventHandlers(
            CommonUtils.mergeLists(resolveResult.eventHandlers, lsc.getEventHandlers()));
      } finally {
        c.setCalculationStateContext(prevContext);
        lsc.release();
      }

      LithoStats.incrementComponentCalculateLayoutCount();
      if (ThreadUtils.isMainThread()) {
        LithoStats.incrementComponentCalculateLayoutOnUICount();
      }

      if (DebugOverlay.isEnabled) {
        DebugOverlay.updateLayoutHistory(treeId);
      }

      return layoutState;
    } finally {
      treeState.unregisterLayoutInitialState();
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
      if (traceId != null) {
        DebugEventDispatcher.endTrace(traceId);
      }
    }
  }
}
