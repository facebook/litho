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
import android.graphics.Rect
import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.extensions.LayoutResultVisitor
import com.facebook.rendercore.extensions.RenderCoreExtension

/**
 * Reduces a tree of Node into a flattened tree of RenderTreeNode. As part of the reduction process
 * all the positions are translated relative to the new hosts.
 */
object Reducer {
  @JvmField @VisibleForTesting val ROOT_HOST_RENDER_UNIT: RenderUnit<*> = RootHostRenderUnit()

  @JvmStatic
  private fun reduceTree(
      context: Context,
      layoutResult: LayoutResult?,
      parent: RenderTreeNode,
      x: Int,
      y: Int,
      flattenedTree: ArrayList<RenderTreeNode>,
      extensions: List<Pair<RenderCoreExtension<*, *>, Any>>?
  ) {
    if (layoutResult == null) {
      return
    }
    val bounds = Rect(x, y, x + layoutResult.width, y + layoutResult.height)
    val absoluteX = parent.absoluteX + x
    val absoluteY = parent.absoluteY + y
    visit(parent, layoutResult, bounds, absoluteX, absoluteY, flattenedTree.size, extensions)
    val renderUnit = layoutResult.renderUnit
    val xTranslation: Int
    val yTranslation: Int
    val nextParent: RenderTreeNode
    if (renderUnit != null && layoutResult.childrenCount > 0) { // The render unit is a host

      // Create new host node;
      val node = createRenderTreeNode(layoutResult, renderUnit, bounds, parent)
      flattenedTree.add(node)

      // Add new child to the parent.
      parent.child(node)

      // Set it as the parent for its children.
      nextParent = node

      // The child do not need to be translated.
      xTranslation = 0
      yTranslation = 0
    } else if (renderUnit != null) { // The render unit is a leaf.

      // Create new content node;
      val content = createRenderTreeNode(layoutResult, renderUnit, bounds, parent)
      flattenedTree.add(content)

      // Add new child to the parent.
      parent.child(content)

      // The next parent is irrelevant because there are not children.
      nextParent = parent

      // The translations are irrelevant because there are not children.
      xTranslation = 0
      yTranslation = 0
    } else { // No render unit.

      // The next parent for any children will be the current parent.
      nextParent = parent

      // The translations for any children will be inherited from the parent.
      xTranslation = x
      yTranslation = y
    }
    for (i in 0 until layoutResult.childrenCount) {
      reduceTree(
          context,
          layoutResult.getChildAt(i),
          nextParent,
          layoutResult.getXForChildAtIndex(i) + xTranslation,
          layoutResult.getYForChildAtIndex(i) + yTranslation,
          flattenedTree,
          extensions)
    }
  }

  @JvmStatic
  private fun createRenderTreeNode(
      layoutResult: LayoutResult,
      renderUnit: RenderUnit<*>,
      bounds: Rect,
      parent: RenderTreeNode?
  ): RenderTreeNode {
    val hasPadding =
        layoutResult.paddingLeft != 0 ||
            layoutResult.paddingTop != 0 ||
            layoutResult.paddingRight != 0 ||
            layoutResult.paddingBottom != 0
    val padding =
        if (hasPadding) {
          Rect(
              layoutResult.paddingLeft,
              layoutResult.paddingTop,
              layoutResult.paddingRight,
              layoutResult.paddingBottom)
        } else {
          null
        }
    return RenderTreeNode(
        parent, renderUnit, layoutResult.layoutData, bounds, padding, parent?.childrenCount ?: 0)
  }

  @JvmStatic
  fun getReducedTree(
      context: Context,
      layoutResult: LayoutResult,
      sizeConstraints: SizeConstraints,
      renderStateId: Int,
      extensions: Array<RenderCoreExtension<*, *>>?
  ): RenderTree {
    RenderCoreSystrace.beginSection("Reducer.reduceTree")
    val results = populate(extensions)
    val nodes = ArrayList<RenderTreeNode>()
    val bounds = Rect(0, 0, layoutResult.width, layoutResult.height)
    visit(null, layoutResult, bounds, 0, 0, 0, results)
    val root = createRenderTreeNode(layoutResult, ROOT_HOST_RENDER_UNIT, bounds, null)
    nodes.add(root)
    reduceTree(context, layoutResult, root, 0, 0, nodes, results)
    val nodesArray = nodes.toTypedArray()
    RenderCoreSystrace.endSection()
    var debugData: Any? = null
    if (BuildConfig.DEBUG) {
      debugData = layoutResult
    }
    return RenderTree(root, nodesArray, sizeConstraints, renderStateId, results, debugData)
  }

  @JvmStatic
  private fun populate(
      extensions: Array<RenderCoreExtension<*, *>>?
  ): List<Pair<RenderCoreExtension<*, *>, Any>>? {
    if (extensions.isNullOrEmpty()) {
      return null
    }
    val results: MutableList<Pair<RenderCoreExtension<*, *>, Any>> = ArrayList(extensions.size)
    for (i in extensions.indices) {
      val input = extensions[i].createInput()
      results.add(Pair(extensions[i], input))
    }
    return results
  }

  @Suppress("UNCHECKED_CAST")
  @JvmStatic
  private fun visit(
      parent: RenderTreeNode?,
      result: LayoutResult,
      bounds: Rect,
      absoluteX: Int,
      absoluteY: Int,
      size: Int,
      extensions: List<Pair<RenderCoreExtension<*, *>, Any>>?
  ) {
    if (extensions != null) {
      for (entry in extensions) {
        val e = entry.first
        val visitor = e.getLayoutVisitor() as LayoutResultVisitor<Any?>?
        if (visitor != null) {
          val input = entry.second
          visitor.visit(parent, result, bounds, absoluteX, absoluteY, size, input)
        }
      }
    }
  }

  private class RootHostRenderUnit : RenderUnit<Any>(RenderType.VIEW), ContentAllocator<Any> {
    override fun createContent(context: Context): Any = Unit

    override fun getPoolableContentType(): Class<*> = renderContentType

    override val contentAllocator: ContentAllocator<Any> = this

    override val id: Long = MountState.ROOT_HOST_ID
  }
}
