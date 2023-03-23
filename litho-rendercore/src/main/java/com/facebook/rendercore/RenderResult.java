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

package com.facebook.rendercore;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.RenderState.ResolveFunc;
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.Collections;
import java.util.List;

/**
 * Result from resolving a {@link ResolveFunc}. A {@link RenderResult} from a previous computation
 * will make the next computation of a new {@link ResolveFunc} more efficient with internal caching.
 */
public class RenderResult<State, RenderContext> {
  private final RenderTree mRenderTree;
  private final Node<RenderContext> mNodeTree;
  private final LayoutCache.CachedData mLayoutCacheData;
  @Nullable private final State mState;

  public static <State, RenderContext> RenderResult<State, RenderContext> render(
      final Context context,
      final ResolveFunc<State, RenderContext> resolveFunc,
      final @Nullable RenderContext renderContext,
      final @Nullable RenderCoreExtension<?, ?>[] extensions,
      final @Nullable RenderResult<State, RenderContext> previousResult,
      final int layoutVersion,
      final int widthSpec,
      final int heightSpec) {
    RenderCoreSystrace.beginSection("RC Create Tree");
    final Pair<Node<RenderContext>, State> result;

    result =
        resolveFunc.resolve(
            new ResolveContext<>(
                renderContext,
                new StateUpdateReceiver() {
                  @Override
                  public void enqueueStateUpdate(StateUpdate stateUpdate) {
                    // Does nothing. This will go away once we refactor RenderResult.resolve
                  }
                }),
            null,
            null,
            Collections.emptyList());
    final RenderResult<State, RenderContext> renderResult;

    if (shouldReuseResult(result.first, widthSpec, heightSpec, previousResult)) {
      renderResult =
          new RenderResult<>(
              previousResult.getRenderTree(),
              result.first,
              previousResult.getLayoutCacheData(),
              result.second);
    } else {
      final LayoutContext layoutContext =
          createLayoutContext(previousResult, renderContext, context, layoutVersion, extensions);
      renderResult = layout(layoutContext, result.first, result.second, widthSpec, heightSpec);
    }

    RenderCoreSystrace.endSection();

    return renderResult;
  }

  public static <RenderContext> LayoutContext<RenderContext> createLayoutContext(
      @Nullable RenderResult previousResult,
      RenderContext renderContext,
      Context context,
      int layoutVersion,
      @Nullable RenderCoreExtension<?, ?>[] extensions) {
    final LayoutCache layoutCache =
        buildCache(previousResult == null ? null : previousResult.getLayoutCacheData());

    return new LayoutContext<>(context, renderContext, layoutVersion, layoutCache, extensions);
  }

  public static <State, RenderContext> RenderResult<State, RenderContext> layout(
      LayoutContext<RenderContext> layoutContext,
      Node<RenderContext> node,
      @Nullable State state,
      int widthSpec,
      int heightSpec) {

    RenderCoreSystrace.beginSection("RC Layout");

    final LayoutResult layoutResult = node.calculateLayout(layoutContext, widthSpec, heightSpec);
    RenderCoreSystrace.endSection();

    RenderCoreSystrace.beginSection("RC Reduce");
    RenderResult renderResult =
        create(layoutContext, node, layoutResult, widthSpec, heightSpec, state);
    RenderCoreSystrace.endSection();
    layoutContext.clearCache();

    return renderResult;
  }

  public static <State, RenderContext> RenderResult<State, RenderContext> create(
      final LayoutContext<RenderContext> c,
      final Node<RenderContext> node,
      final LayoutResult layoutResult,
      final int widthSpec,
      final int heightSpec,
      final @Nullable State state) {
    return new RenderResult<>(
        Reducer.getReducedTree(
            c.getAndroidContext(),
            layoutResult,
            widthSpec,
            heightSpec,
            RenderState.NO_ID, // TODO: Get render state id from layout context
            c.getExtensions()),
        node,
        c.getLayoutCache().getWriteCacheData(),
        state);
  }

  public static <State, RenderContext> boolean shouldReuseResult(
      final Node<RenderContext> node,
      final int widthSpec,
      final int heightSpec,
      @Nullable final RenderResult<State, RenderContext> previousResult) {
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

  RenderResult(
      RenderTree renderTree,
      Node<RenderContext> nodeTree,
      LayoutCache.CachedData layoutCacheData,
      @Nullable State state) {
    mRenderTree = renderTree;
    mNodeTree = nodeTree;
    mLayoutCacheData = layoutCacheData;
    mState = state;
  }

  public RenderTree getRenderTree() {
    return mRenderTree;
  }

  Node<RenderContext> getNodeTree() {
    return mNodeTree;
  }

  LayoutCache.CachedData getLayoutCacheData() {
    return mLayoutCacheData;
  }

  @Nullable
  public State getState() {
    return mState;
  }

  @VisibleForTesting
  public static LayoutCache buildCache(@Nullable LayoutCache.CachedData previousCache) {
    return previousCache != null ? new LayoutCache(previousCache) : new LayoutCache(null);
  }

  public static <T, R> ResolveFunc<T, R> wrapInResolveFunc(Node<R> node) {
    return wrapInResolveFunc(node, null);
  }

  public static <T, R> ResolveFunc<T, R> wrapInResolveFunc(
      final Node<R> node, final @Nullable T state) {
    return new ResolveFunc<T, R>() {
      @Override
      public Pair<Node<R>, T> resolve(
          ResolveContext<R> resolveContext,
          @Nullable Node<R> committedTree,
          @Nullable T committedState,
          List<? extends StateUpdate> stateUpdatesToApply) {
        return new Pair<>(node, state);
      }
    };
  }
}
