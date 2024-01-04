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

import com.facebook.litho.ComponentsSystrace.beginSection
import com.facebook.litho.ComponentsSystrace.endSection
import com.facebook.litho.ComponentsSystrace.isTracing
import com.facebook.litho.Layout.measure
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult
import com.facebook.yoga.YogaNode

/** This is an output only [NestedTreeHolderResult]; this is created by a [NestedTreeHolder]. */
class NestedTreeHolderResult(
    c: ComponentContext,
    internalNode: NestedTreeHolder,
    yogaNode: YogaNode,
    widthFromStyle: Float,
    heightFromStyle: Float,
) : LithoLayoutResult(c, internalNode, yogaNode, widthFromStyle, heightFromStyle) {

  var nestedResult: LithoLayoutResult? = null

  override val node: NestedTreeHolder
    get() = super.node as NestedTreeHolder

  override fun measureInternal(
      context: LayoutContext<LithoRenderContext>,
      widthSpec: Int,
      heightSpec: Int
  ): MeasureResult {

    val isTracing = isTracing
    val component = node.tailComponent
    val renderContext = checkNotNull(context.renderContext)

    check(!renderContext.lithoLayoutContext.isReleased) {
      (component.simpleName +
          ": To measure a component outside of a layout calculation use" +
          " Component#measureMightNotCacheInternalNode.")
    }

    val count = node.componentCount
    val parentContext: ComponentContext? =
        if (count == 1) {
          val parentFromNode = node.mParentContext
          parentFromNode ?: renderContext.lithoLayoutContext.rootComponentContext
        } else {
          node.getComponentContextAt(1)
        }

    checkNotNull(parentContext) { component.simpleName + ": Null component context during measure" }

    if (isTracing) {
      beginSection("resolveNestedTree:" + component.simpleName)
    }

    return try {
      val nestedTree =
          measure(
              renderContext.lithoLayoutContext,
              parentContext,
              this,
              widthSpec,
              heightSpec,
          )

      if (nestedTree != null) {
        MeasureResult(nestedTree.width, nestedTree.height, nestedTree.layoutData)
      } else {
        MeasureResult(0, 0)
      }
    } finally {
      if (isTracing) {
        endSection()
      }
    }
  }

  override fun releaseLayoutPhaseData() {
    super.releaseLayoutPhaseData()
    nestedResult?.releaseLayoutPhaseData()
  }
}
