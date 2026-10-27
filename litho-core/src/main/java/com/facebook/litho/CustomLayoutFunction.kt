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

package com.facebook.litho

import android.graphics.Point
import com.facebook.litho.layout.internal.DefaultLayoutBuilder
import com.facebook.litho.layout.resolveRootBox
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints

/**
 * How a node lays itself and its subtree out. [LithoNode.calculateLayout] calls the one its node
 * selects, so a node's layout is decided by the node, not by whoever lays out its parent.
 */
internal sealed interface CustomLayoutFunction {

  /** Lays [node] and its subtree out under [sizeConstraints]. */
  fun calculateLayout(
      context: LayoutContext<LithoLayoutContext>,
      node: LithoNode,
      sizeConstraints: SizeConstraints,
  ): LithoLayoutResult

  /** Lays a node and its subtree out with flexbox, through Yoga. Every node's, by default. */
  data object Yoga : CustomLayoutFunction {

    override fun calculateLayout(
        context: LayoutContext<LithoLayoutContext>,
        node: LithoNode,
        sizeConstraints: SizeConstraints,
    ): LithoLayoutResult = LithoYogaLayoutFunction.calculateLayout(context, sizeConstraints, node)
  }

  /**
   * Lays a `Layout` out through its own layout function, with no Yoga tree: laid out on its own, it
   * resolves its box within [sizeConstraints] by the root rule (`resolveRootBox`) and builds it
   * with `DefaultLayoutBuilder.buildRoot`. Its margins are the root offset, and [sizeConstraints]
   * its specs. Under a flexbox parent, the parent's Yoga tree holds it as a leaf instead, whose
   * measure callback runs its function.
   */
  data object Container : CustomLayoutFunction {

    override fun calculateLayout(
        context: LayoutContext<LithoLayoutContext>,
        node: LithoNode,
        sizeConstraints: SizeConstraints,
    ): LithoLayoutResult {
      LithoYogaLayoutFunction.applyOverridesRecursive(node)
      val renderContext = checkNotNull(context.renderContext)
      val previousDiffNode = LithoYogaLayoutFunction.rootDiffNode(renderContext, node)
      val builder = DefaultLayoutBuilder(context, node, previousDiffNode)
      val box = resolveRootBox(sizeConstraints, builder.boxProps, node.layoutDirection.isRTL)
      val result = builder.buildRoot(box, sizeConstraints)
      renderContext.rootOffset = Point(box.margins.left, box.margins.top)
      return result
    }
  }
}
