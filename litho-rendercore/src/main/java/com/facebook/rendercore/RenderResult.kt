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

package com.facebook.rendercore

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.extensions.RenderCoreExtension

/**
 * Result from laying out and reducing a [ResolveResult]. A [RenderResult] from a previous
 * computation will make the next computation of a new [RenderResult] more efficient with internal
 * caching.
 */
class RenderResult<State, RenderContext>
internal constructor(
    val renderTree: RenderTree,
    val nodeTree: Node<RenderContext>,
    val layoutCacheData: Map<Any, Any?>,
    val state: State?
) {

  companion object {

    @Deprecated(message = "Use the render variant that accepts SizeConstraints")
    @JvmStatic
    fun <State, RenderContext> render(
        context: Context,
        result: ResolveResult<Node<RenderContext>, State>,
        renderContext: RenderContext?,
        extensions: Array<RenderCoreExtension<*, *>>?,
        previousResult: RenderResult<State, RenderContext>?,
        layoutVersion: Int,
        widthSpec: Int,
        heightSpec: Int
    ): RenderResult<State, RenderContext> {
      return render(
          context,
          result,
          renderContext,
          extensions,
          previousResult,
          layoutVersion,
          SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
    }

    @JvmStatic
    fun <State, RenderContext> render(
        context: Context,
        result: ResolveResult<Node<RenderContext>, State>,
        renderContext: RenderContext?,
        extensions: Array<RenderCoreExtension<*, *>>?,
        previousResult: RenderResult<State, RenderContext>?,
        layoutVersion: Int,
        sizeConstraints: SizeConstraints
    ): RenderResult<State, RenderContext> {
      RenderCoreSystrace.beginSection("RC Create Tree")
      val renderResult: RenderResult<State, RenderContext> =
          if (previousResult != null &&
              shouldReuseResult(result.resolvedNode, sizeConstraints, previousResult)) {
            RenderResult(
                previousResult.renderTree,
                result.resolvedNode,
                previousResult.layoutCacheData,
                result.resolvedState)
          } else {
            val layoutContext: LayoutContext<RenderContext> =
                createLayoutContext(
                    previousResult, renderContext, context, layoutVersion, extensions)
            layout(layoutContext, result.resolvedNode, result.resolvedState, sizeConstraints)
          }
      RenderCoreSystrace.endSection()
      return renderResult
    }

    @JvmStatic
    fun <RenderContext> createLayoutContext(
        previousResult: RenderResult<*, *>?,
        renderContext: RenderContext?,
        context: Context,
        layoutVersion: Int,
        extensions: Array<RenderCoreExtension<*, *>>?
    ): LayoutContext<RenderContext> {
      val layoutCache = buildCache(previousResult?.layoutCacheData)
      return LayoutContext<RenderContext>(
          context, renderContext, layoutVersion, layoutCache, extensions)
    }

    @Deprecated(message = "Use layout function that accepts SizeConstraints")
    @JvmStatic
    fun <State, RenderContext> layout(
        layoutContext: LayoutContext<RenderContext>,
        node: Node<RenderContext>,
        state: State?,
        widthSpec: Int,
        heightSpec: Int
    ): RenderResult<State, RenderContext> {
      return layout(
          layoutContext, node, state, SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
    }

    @JvmStatic
    fun <State, RenderContext> layout(
        layoutContext: LayoutContext<RenderContext>,
        node: Node<RenderContext>,
        state: State?,
        sizeConstraints: SizeConstraints
    ): RenderResult<State, RenderContext> {
      RenderCoreSystrace.beginSection("RC Layout")
      val layoutResult = node.calculateLayout(layoutContext, sizeConstraints)
      RenderCoreSystrace.endSection()
      RenderCoreSystrace.beginSection("RC Reduce")
      val renderResult: RenderResult<State, RenderContext> =
          create(layoutContext, node, layoutResult, sizeConstraints, state)
      RenderCoreSystrace.endSection()
      layoutContext.clearCache()
      return renderResult
    }

    @Deprecated(message = "Use create function that accepts SizeConstraints")
    @JvmStatic
    fun <State, RenderContext> create(
        c: LayoutContext<RenderContext>,
        node: Node<RenderContext>,
        layoutResult: LayoutResult,
        widthSpec: Int,
        heightSpec: Int,
        state: State?
    ): RenderResult<State, RenderContext> {
      return create(
          c, node, layoutResult, SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec), state)
    }

    @JvmStatic
    fun <State, RenderContext> create(
        c: LayoutContext<RenderContext>,
        node: Node<RenderContext>,
        layoutResult: LayoutResult,
        sizeConstraints: SizeConstraints,
        state: State?
    ): RenderResult<State, RenderContext> {
      return RenderResult(
          Reducer.getReducedTree(
              c.androidContext,
              layoutResult,
              sizeConstraints,
              RenderState.NO_ID, // TODO: Get render state id from layout context
              c.extensions),
          node,
          c.layoutCache.writeCacheData,
          state)
    }

    @JvmStatic
    fun <State, RenderContext> shouldReuseResult(
        node: Node<RenderContext>,
        sizeConstraints: SizeConstraints,
        previousResult: RenderResult<State, RenderContext>?
    ): Boolean {
      if (previousResult == null) {
        return false
      }
      val prevRenderTree = previousResult.renderTree
      return (node === previousResult.nodeTree &&
          sizeConstraints.areCompatible(
              prevRenderTree.sizeConstraints, Size(prevRenderTree.width, prevRenderTree.height)))
    }

    @VisibleForTesting
    @JvmStatic
    fun buildCache(previousCache: Map<Any, Any?>?): LayoutCache {
      return if (previousCache != null) LayoutCache(previousCache) else LayoutCache(null)
    }
  }
}
