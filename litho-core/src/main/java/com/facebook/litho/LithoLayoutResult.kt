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

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import com.facebook.rendercore.LayoutResult
import com.facebook.yoga.YogaNode

/**
 * This is the default implementation of a [LayoutResult] for Litho. This holds a reference to the
 * [LithoNode] which created it, its [YogaNode], and a list of its children.
 */
open class LithoLayoutResult(
    val context: ComponentContext,
    open val node: LithoNode,
    val lithoLayoutOutput: YogaLithoLayoutOutput
) : LayoutResult {

  private val children: MutableList<LithoLayoutResult> = ArrayList()

  val widthSpec: Int
    get() = lithoLayoutOutput.widthSpec

  val heightSpec: Int
    get() = lithoLayoutOutput.heightSpec

  val delegate: LayoutResult?
    get() = lithoLayoutOutput.delegate

  val diffNode: DiffNode?
    get() = lithoLayoutOutput.diffNode

  val measureHadExceptions: Boolean
    get() = lithoLayoutOutput.measureHadExceptions

  val wasMeasured: Boolean
    get() = lithoLayoutOutput.wasMeasured

  val contentWidth: Int
    get() = lithoLayoutOutput.contentWidth

  val contentHeight: Int
    get() = lithoLayoutOutput.contentHeight

  val lastMeasuredSize: Long
    get() = lithoLayoutOutput.lastMeasuredSize

  val isCachedLayout: Boolean
    get() = lithoLayoutOutput.isCachedLayout

  val cachedMeasuresValid: Boolean
    get() = lithoLayoutOutput.cachedMeasuresValid

  val childCount: Int
    get() = children.size

  val touchExpansionTop: Int
    get() = node.touchExpansionTop()

  val touchExpansionBottom: Int
    get() = node.touchExpansionBottom()

  val touchExpansionLeft: Int
    get() = node.touchExpansionLeft(isDirectionRtl())

  val touchExpansionRight: Int
    get() = node.touchExpansionRight(isDirectionRtl())

  val expandedTouchBounds: Rect?
    get() {
      if (!node.hasTouchExpansion()) {
        return null
      }
      val left = touchExpansionLeft
      val top = touchExpansionTop
      val right = touchExpansionRight
      val bottom = touchExpansionBottom
      return if (left == 0 && top == 0 && right == 0 && bottom == 0) {
        null
      } else {
        Rect(left, top, right, bottom)
      }
    }

  val layoutDirection: Int
    get() = lithoLayoutOutput.layoutDirection

  val contentRenderUnit: LithoRenderUnit?
    get() = lithoLayoutOutput.contentRenderUnit

  val hostRenderUnit: LithoRenderUnit?
    get() = lithoLayoutOutput.hostRenderUnit

  val backgroundRenderUnit: LithoRenderUnit?
    get() = lithoLayoutOutput.backgroundRenderUnit

  val foregroundRenderUnit: LithoRenderUnit?
    get() = lithoLayoutOutput.foregroundRenderUnit

  val borderRenderUnit: LithoRenderUnit?
    get() = lithoLayoutOutput.borderRenderUnit

  @Px override fun getWidth(): Int = lithoLayoutOutput.width

  @Px override fun getHeight(): Int = lithoLayoutOutput.height

  @Px override fun getPaddingTop(): Int = lithoLayoutOutput.paddingTop

  @Px override fun getPaddingRight(): Int = lithoLayoutOutput.paddingRight

  @Px override fun getPaddingBottom(): Int = lithoLayoutOutput.paddingBottom

  @Px override fun getPaddingLeft(): Int = lithoLayoutOutput.paddingLeft

  override fun getRenderUnit(): LithoRenderUnit? = null // Unimplemented.

  override fun getChildrenCount(): Int = children.size

  override fun getChildAt(i: Int): LithoLayoutResult = children[i]

  override fun getXForChildAtIndex(index: Int): Int = children[index].lithoLayoutOutput.x

  override fun getYForChildAtIndex(index: Int): Int = children[index].lithoLayoutOutput.y

  override fun getLayoutData(): Any? = lithoLayoutOutput.layoutData

  fun adjustedLeft(): Int = lithoLayoutOutput.adjustedBounds.left

  fun adjustedTop(): Int = lithoLayoutOutput.adjustedBounds.top

  fun adjustedRight(): Int = lithoLayoutOutput.adjustedBounds.right

  fun adjustedBottom(): Int = lithoLayoutOutput.adjustedBounds.bottom

  /**
   * Since layout data like the layout context and the diff node are not required after layout
   * calculation they can be released to free up memory.
   */
  open fun releaseLayoutPhaseData() {
    lithoLayoutOutput._diffNode = null
    lithoLayoutOutput.yogaNode.data = null
    for (i in 0 until childCount) {
      getChildAt(i).releaseLayoutPhaseData()
    }
  }

  fun addChild(child: LithoLayoutResult) {
    children.add(child)
  }

  private fun isDirectionRtl(): Boolean =
      (lithoLayoutOutput.layoutDirection == View.LAYOUT_DIRECTION_RTL)
}
