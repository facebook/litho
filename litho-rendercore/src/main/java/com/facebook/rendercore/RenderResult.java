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

package com.facebook.rendercore;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.rendercore.RenderState.LazyTree;
import com.facebook.rendercore.utils.MeasureSpecUtils;

/**
 * Result from resolving a {@link LazyTree}. A {@link RenderResult} from a previous computation will
 * make the next computation of a new {@link LazyTree} more efficient with internal caching.
 */
public class RenderResult<State> {
  private final RenderTree mRenderTree;
  private final LazyTree mLazyTree;
  private final Node mNodeTree;
  private final LayoutCache mLayoutCache;
  @Nullable private final State mState;

  public static <State, RenderContext> RenderResult<State> resolve(
      final Context context,
      LazyTree<State> lazyTree,
      @Nullable RenderContext renderContext,
      @Nullable final RenderResult<State> previousResult,
      final int layoutVersion,
      final int widthSpec,
      final int heightSpec) {
    final Node previousTree = previousResult != null ? previousResult.getNodeTree() : null;
    final State previousState = previousResult != null ? previousResult.getState() : null;

    RenderCoreSystrace.beginSection("RC Create Tree");
    final Pair<Node, State> result;

    if (previousResult != null && lazyTree == previousResult.getLazyTree()) {
      result = new Pair<>(previousTree, previousState);
    } else {
      result = lazyTree.resolve();
    }
    final RenderResult renderResult;

    if (shouldReuseResult(result.first, widthSpec, heightSpec, previousResult)) {
      renderResult =
          new RenderResult<>(
              previousResult.getRenderTree(),
              lazyTree,
              result.first,
              previousResult.getLayoutCache(),
              result.second);
    } else {
      RenderCoreSystrace.beginSection("RC Layout");

      final LayoutCache layoutCache =
          buildCache(previousResult == null ? null : previousResult.getLayoutCache());

      final LayoutContext<RenderContext> layoutContext =
          new LayoutContext<>(context, renderContext, layoutVersion, layoutCache);

      final Node.LayoutResult layoutResult =
          result.first.calculateLayout(layoutContext, widthSpec, heightSpec);
      RenderCoreSystrace.endSection();

      RenderCoreSystrace.beginSection("RC Reduce");
      renderResult =
          create(
              layoutContext,
              result.first,
              layoutResult,
              lazyTree,
              widthSpec,
              heightSpec,
              result.second);
      RenderCoreSystrace.endSection();
      layoutContext.clearCache();
    }

    RenderCoreSystrace.endSection();

    return renderResult;
  }

  public static <State> RenderResult<State> create(
      final LayoutContext c,
      final Node node,
      final Node.LayoutResult layoutResult,
      final LazyTree<State> lazyTree,
      final int widthSpec,
      final int heightSpec,
      final @Nullable State state) {
    return new RenderResult<>(
        Reducer.getReducedTree(c.getAndroidContext(), layoutResult, widthSpec, heightSpec),
        lazyTree,
        node,
        c.getLayoutCache(),
        state);
  }

  public static <State> boolean shouldReuseResult(
      final Node node,
      final int widthSpec,
      final int heightSpec,
      @Nullable final RenderResult<State> previousResult) {
    if (previousResult == null) {
      return false;
    }

    final RenderTree prevRenderTree = previousResult.getRenderTree();
    return previousResult.getRenderTree() != null
        && node == previousResult.getNodeTree()
        && MeasureSpecUtils.isMeasureSpecCompatible(
            prevRenderTree.getWidthSpec(), widthSpec, prevRenderTree.getWidth())
        && MeasureSpecUtils.isMeasureSpecCompatible(
            prevRenderTree.getHeightSpec(), heightSpec, prevRenderTree.getHeight());
  }

  private RenderResult(
      RenderTree renderTree,
      LazyTree lazyTree,
      Node nodeTree,
      LayoutCache layoutCache,
      @Nullable State state) {
    mRenderTree = renderTree;
    mLazyTree = lazyTree;
    mNodeTree = nodeTree;
    mLayoutCache = layoutCache;
    mState = state;
  }

  public RenderTree getRenderTree() {
    return mRenderTree;
  }

  LazyTree getLazyTree() {
    return mLazyTree;
  }

  Node getNodeTree() {
    return mNodeTree;
  }

  LayoutCache getLayoutCache() {
    return mLayoutCache;
  }

  @Nullable
  State getState() {
    return mState;
  }

  @VisibleForTesting
  public static LayoutCache buildCache(@Nullable LayoutCache previousCache) {

    return previousCache != null
        ? new LayoutCache(previousCache.getWriteCache())
        : new LayoutCache(null);
  }

  public static LazyTree<Void> wrapInLazyTree(final Node node) {
    return new LazyTree<Void>() {
      @Override
      public Pair<Node, Void> resolve() {
        return new Pair(node, null);
      }
    };
  }
}
