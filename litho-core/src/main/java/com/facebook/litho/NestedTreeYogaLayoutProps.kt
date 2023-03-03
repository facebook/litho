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

import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaNode

class NestedTreeYogaLayoutProps(node: YogaNode) : YogaLayoutProps(node) {

  var borderWidth: IntArray? = null
    private set

  var padding: Edges? = null
    private set

  var isPaddingPercentage: BooleanArray? = null
    private set

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    setPadding(edge, padding.toFloat())
    setIsPaddingPercentage(edge, false)
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    setPadding(edge, percent)
    setIsPaddingPercentage(edge, true)
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    if (this.borderWidth == null) {
      this.borderWidth = IntArray(Border.EDGE_COUNT)
    }
    Border.setEdgeValue(this.borderWidth, edge, borderWidth.toInt())
  }

  private fun setPadding(edge: YogaEdge, width: Float) {
    if (padding == null) {
      padding = Edges()
    }
    padding?.set(edge, width)
  }

  private fun setIsPaddingPercentage(edge: YogaEdge, isPercentage: Boolean) {
    if (isPaddingPercentage == null && isPercentage) {
      isPaddingPercentage = BooleanArray(YogaEdge.ALL.intValue() + 1)
    }

    if (isPaddingPercentage != null) {
      isPaddingPercentage?.set(edge.intValue(), isPercentage)
    }
  }
}
