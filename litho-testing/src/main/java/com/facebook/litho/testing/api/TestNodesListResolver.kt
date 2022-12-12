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
import com.facebook.litho.LithoNode
import com.facebook.litho.LithoView
import com.facebook.litho.NestedTreeHolderResult
import com.facebook.litho.ScopedComponentInfo
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder

class TestNodesListResolver {

  fun getCurrentTestNodes(lithoView: LithoView): List<TestNode> {
    val rootLayoutResult = getLayoutRoot(lithoView) ?: return emptyList()
    return searchTestNodesWithBFS(rootLayoutResult)
  }

  @SuppressLint("RestrictedApi")
  private fun getLayoutRoot(lithoView: LithoView): LithoLayoutResult? {
    val commitedLayoutState =
        lithoView.componentTree?.committedLayoutState
            ?: throw IllegalStateException(
                "No ComponentTree/Committed Layout/Layout Root found. Please call render() first")
    return commitedLayoutState.rootLayoutResult
  }

  private fun searchTestNodesWithBFS(layoutResult: LithoLayoutResult?): List<TestNode> {
    val testNodes = mutableListOf<TestNode>()

    componentBreadthFirstSearch(layoutResult) { scopedComponents ->
      val recyclers: List<Recycler> = scopedComponents.filterIsInstance<Recycler>()
      recyclers.forEach { recycler ->
        val recyclerBinder: RecyclerBinder = recycler.binder

        (0 until recyclerBinder.itemCount).forEach { recyclerItemIndex ->
          val holder = recyclerBinder.getComponentTreeHolderAt(recyclerItemIndex)
          val holderLithoView = holder.componentTree?.lithoView

          holderLithoView?.let {
            val holderSubcomponents = getCurrentTestNodes(holderLithoView)
            testNodes.addAll(holderSubcomponents)
          }
        }
      }

      testNodes.addAll(scopedComponents.map { TestNode(it) })
    }

    return testNodes
  }

  /**
   * Internal function to handle BFS through a set of components.
   *
   * @param onHandleScopedComponents lambda which handles the scoped components of the particular
   *   layout. This enables the caller of the function to properly handle if any of those components
   *   match. (For example, if looking for a single component, you would want to return in the
   *   lambda if it matches. If looking for multiple, you would simply want to add all matching
   *   components to a list.)
   */
  private inline fun componentBreadthFirstSearch(
      startingLayoutResult: LithoLayoutResult?,
      onHandleScopedComponents: (List<Component>) -> Unit
  ) {
    startingLayoutResult ?: return

    val enqueuedLayouts = mutableSetOf(startingLayoutResult)
    val layoutsQueue = ArrayDeque(enqueuedLayouts)

    while (layoutsQueue.isNotEmpty()) {
      val currentLayoutResult = layoutsQueue.removeFirst()

      val internalNode = currentLayoutResult.node
      onHandleScopedComponents(getOrderedScopedComponentInfos(internalNode).map { it.component })

      if (currentLayoutResult is NestedTreeHolderResult) {
        val nestedLayout =
            currentLayoutResult.nestedResult?.takeUnless { it in enqueuedLayouts } ?: continue
        layoutsQueue.add(nestedLayout)
        continue
      }

      for (i in 0 until internalNode.childCount) {
        val childLayout =
            currentLayoutResult.getChildAt(i).takeUnless { it in enqueuedLayouts } ?: continue
        layoutsQueue.add(childLayout)
      }
    }
  }

  /**
   * @return list of the [ScopedComponentInfo], ordered from the head (closest to the root) to the
   *   tail.
   */
  private fun getOrderedScopedComponentInfos(internalNode: LithoNode): List<ScopedComponentInfo> {
    return internalNode.scopedComponentInfos.reversed()
  }

  private val Recycler.binder: RecyclerBinder
    get() {
      val binder =
          Whitebox.getInternalState<Binder<RecyclerView>>(this, "binder") as SectionBinderTarget

      return Whitebox.getInternalState(binder, "mRecyclerBinder")
    }
}
