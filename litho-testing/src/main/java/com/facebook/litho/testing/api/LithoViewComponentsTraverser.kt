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
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoView
import com.facebook.litho.NestedTreeHolderResult
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
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
    traverse(layoutResult = extractRootLayoutResult(lithoView), onComponentFound = onComponentFound)
  }

  @SuppressLint("RestrictedApi")
  private fun extractRootLayoutResult(lithoView: LithoView?): LithoLayoutResult? {
    val committedLayoutState = lithoView?.componentTree?.committedLayoutState

    check(committedLayoutState != null) {
      "No ComponentTree/Committed Layout/Layout Root found. Please call render() first"
    }

    return committedLayoutState.rootLayoutResult
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
      val traverseNode = layoutsStack.pop()

      val currentLayoutResult = traverseNode.lithoLayoutResult

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
          layoutsStack.push(TraverseNode(nestedResult, traverseNode.parentComponent))
          continue
        }
      }

      (listOf(traverseNode.parentComponent) + components).zipWithNext {
          currentParent: Component?,
          child: Component? ->
        if (child == null) error("Child should never be null")
        onComponentFound(child, currentParent)
      }

      if (lastScopedComponent is Recycler) {
        val binder = lastScopedComponent.binder

        (binder.itemCount - 1 downTo 0).forEach { recyclerItemIndex ->
          val holder = binder.getComponentTreeHolderAt(recyclerItemIndex)
          val holderLithoView = holder.componentTree?.lithoView
          val holderLithoLayoutResult = extractRootLayoutResult(holderLithoView)

          if (holderLithoLayoutResult != null) {
            layoutsStack.push(TraverseNode(holderLithoLayoutResult, lastScopedComponent))
          }
        }
      }

      // add children layout results to be processed
      (lithoNode.childCount - 1 downTo 0)
          .map { childIndex -> currentLayoutResult.getChildAt(childIndex) }
          .forEach { childLayoutResult ->
            layoutsStack.push(TraverseNode(childLayoutResult, lastScopedComponent))
          }
    }
  }

  data class TraverseNode(
      val lithoLayoutResult: LithoLayoutResult,
      val parentComponent: Component?
  )

  private val Recycler.binder: RecyclerBinder
    get() {
      val binder =
          Whitebox.getInternalState<Binder<RecyclerView>>(this, "binder") as SectionBinderTarget

      return Whitebox.getInternalState(binder, "mRecyclerBinder")
    }
}
