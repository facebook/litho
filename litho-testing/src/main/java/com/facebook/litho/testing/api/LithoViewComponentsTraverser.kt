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

package com.facebook.litho.testing.api

import android.annotation.SuppressLint
import android.view.ViewGroup
import com.facebook.litho.Component
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoView
import com.facebook.litho.NestedTreeHolderResult
import com.facebook.litho.getMountedContent
import java.util.LinkedList

/**
 * Allows traversing through all the [Component] that define a hierarchy in a given [LithoView].
 *
 * It is a generic purpose traversion, which allow whoever calls [traverse] to perform an operation
 * at any component that is traversed (and its parent).
 */
class LithoViewComponentsTraverser {

  fun traverse(
      lithoView: LithoView,
      onComponentFound: (component: Component, parent: Component?) -> Unit
  ) {
    val rootLayoutResult = lithoView.extractLayoutResult(isRoot = true)
    traverse(layoutResult = rootLayoutResult, onComponentFound = onComponentFound)
  }

  private fun traverse(
      layoutResult: LithoLayoutResult?,
      onComponentFound: (component: Component, parent: Component?) -> Unit
  ) {
    if (layoutResult == null) return

    val visited = HashSet<LithoLayoutResult>()

    val layoutsStack = LinkedList<TraverseNode>()
    layoutsStack.push(TraverseNode(layoutResult, null))

    while (layoutsStack.isNotEmpty()) {
      val (currentLayoutResult, parentComponent) = layoutsStack.pop()

      if (currentLayoutResult in visited) {
        continue
      }

      visited.add(currentLayoutResult)

      val lithoNode = currentLayoutResult.node
      val components = lithoNode.scopedComponentInfos.asReversed().mapNotNull { it.component }
      val lastScopedComponent = components.lastOrNull()

      if (currentLayoutResult is NestedTreeHolderResult) {
        val nestedResult = currentLayoutResult.nestedResult

        /*
         * Let's consider an example where we have this hierarchy:
         *
         * - ComponentThatUsesLithoLayoutSizeSpecs (uses @OnCreateLayoutWithSizeSpecs)
         *   - Text
         *
         * This will generate this hierarchy of LayoutResults
         * #1. NestedTreeHolderResult (components: [ComponentThatUsesLithoLayoutSizeSpecs])
         *  #2. NestedTreeHolderResult (components: [ComponentThatUsesLithoLayoutSizeSpecs, Text])
         *   #3. LithoLayoutResult (components: [Text])
         *
         * Therefore we only let the "traversing" of the components for the LayoutResult for #2,
         * since considering #1 and #3 would give repeated information.
         *
         * The other scenario would be where the layout spec that uses @OnCreateLayoutWithSizeSpecs uses
         * a Row/Column wrapping a MountSpec:
         * - ComponentThatUsesLithoLayoutSizeSpecs (uses @OnCreateLayoutWithSizeSpecs)
         *  - Row
         *   - Text
         *
         * This will generate this hierarchy of LayoutResults:
         *
         * #1  NestedTreeHolderResult (components: [ComponentThatUsesLithoLayoutSizeSpecs]
         *  #2 LithoLayoutResult (components: [ComponentThatUsesLithoLayoutSizeSpecs, Row])
         *    #3 NestedTreeHolderResult (components: [Text])
         *      #4 LithoLayoutResult (components: [Text])
         *
         * In this case we we will only process #2 (a LithoLayoutResult with children) and
         * #3 (a NestedTreeHolderResult whose nested result is a LithoLayoutResult with no children)
         * */
        if (nestedResult != null &&
            (nestedResult.node.childCount > 0 || nestedResult is NestedTreeHolderResult)) {
          layoutsStack.push(TraverseNode(nestedResult, parentComponent))
          continue
        }
      }

      (listOf(parentComponent) + components).zipWithNext {
          currentParent: Component?,
          child: Component? ->
        if (child == null) error("Child should never be null")
        onComponentFound(child, currentParent)
      }

      currentLayoutResult.childResults.forEach { child ->
        layoutsStack.push(TraverseNode(child, lastScopedComponent))
      }
    }
  }

  @SuppressLint("RestrictedApi")
  private fun LithoView.extractLayoutResult(isRoot: Boolean): LithoLayoutResult? {
    val committedLayoutState = componentTree?.committedLayoutState

    check(!isRoot || committedLayoutState != null) {
      "No ComponentTree/Committed Layout/Layout Root found. Please call render() first"
    }

    return committedLayoutState?.rootLayoutResult
  }

  private val LithoLayoutResult.childResults: List<LithoLayoutResult>
    get() {
      val mountedContent = getMountedContent()
      if (mountedContent is ViewGroup) {
        val childrenLithoViews = mountedContent.firstLevelInnerLithoViews
        val results = childrenLithoViews.mapNotNull { it.extractLayoutResult(isRoot = false) }
        if (results.isNotEmpty()) return results.asReversed()
      }

      return (childCount - 1 downTo 0).map { getChildAt(it) }
    }

  /**
   * This method will perform a search from the given [ViewGroup] in order to find all first-level
   * occurences of a [LithoView].
   *
   * This means, that once a [LithoView] is found, we stop the search down that sub-tree. All of the
   * found [LithoView] will be used to start a new traversal with [traverse] - which will enable the
   * full traverse (even on scenarios of nested [LithoView]
   */
  private val ViewGroup.firstLevelInnerLithoViews: List<LithoView>
    get() {
      val lithoViews = mutableListOf<LithoView>()

      for (i in 0..childCount) {
        val child = getChildAt(i)
        if (child is LithoView) {
          lithoViews.add(child)
        } else {
          (child as? ViewGroup)?.let { lithoViews.addAll(child.firstLevelInnerLithoViews) }
        }
      }

      return lithoViews
    }

  internal data class TraverseNode(
      val layoutResult: LithoLayoutResult,
      val parentComponent: Component?
  )
}
