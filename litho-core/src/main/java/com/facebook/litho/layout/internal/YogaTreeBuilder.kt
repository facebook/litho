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

package com.facebook.litho.layout.internal

import androidx.collection.MutableScatterSet
import com.facebook.litho.DiffNode
import com.facebook.litho.LithoLayoutContext
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoNode
import com.facebook.litho.LithoYogaLayoutFunction
import com.facebook.litho.layout.ChildBox
import com.facebook.litho.state.StateId
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutContext

/**
 * Lays out a `Layout`'s child that isn't a `Layout` as a Yoga tree of its own: one tree, laid out
 * again in place when it's measured within another box, as Yoga does with any node it measures
 * twice. Built, it hands that tree over; discarded, it drops it.
 */
internal class YogaTreeBuilder(
    private val context: LayoutContext<LithoLayoutContext>,
    private val node: LithoNode,
    private val previousDiffNode: DiffNode?,
) : LithoLayoutBuilder {

  private var tree: LithoLayoutResult? = null
  private var laidOutIn: ChildBox? = null
  private var isDone = false

  override fun measure(box: ChildBox): IntSize {
    check(!isDone) { "${node.headComponent.simpleName} measured after it was built or discarded" }
    val tree = treeIn(box)
    return IntSize(tree.width, tree.height)
  }

  override fun build(box: ChildBox): LithoLayoutResult {
    check(!isDone) { "${node.headComponent.simpleName} built twice, or after it was discarded" }
    val tree = treeIn(box)
    isDone = true
    this.tree = null
    return LithoYogaLayoutFunction.buildTree(context, tree)
  }

  override fun discard(reads: MutableScatterSet<StateId>?) {
    if (isDone) return
    isDone = true
    val tree = tree ?: return
    this.tree = null
    // Only a placed tree is written to the layout cache, so nothing else references this one.
    LithoYogaLayoutFunction.discardTree(tree, reads)
  }

  /** Its tree, laid out within [box] unless it already is; only [measure] makes it. */
  private fun treeIn(box: ChildBox): LithoLayoutResult {
    val tree =
        tree
            ?: LithoYogaLayoutFunction.newTree(context, node, previousDiffNode).also { made ->
              tree = made
            }
    if (laidOutIn != box) {
      LithoYogaLayoutFunction.layOutTree(context, tree, box)
      laidOutIn = box
    }
    return tree
  }
}
