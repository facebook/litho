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
import androidx.annotation.Px
import com.facebook.litho.layout.LayoutDirection
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.SizeConstraints

/** This is the default implementation of a [LayoutResult] for Litho. */
open class LithoLayoutResult(
    val context: ComponentContext,
    open val node: LithoNode,
    // Eventually, this will be replaced by LithoLayoutOutput.
    val layoutOutput: YogaLayoutOutput
) : LayoutResult {

  private val children: MutableList<LithoLayoutResult> = ArrayList()

  val widthSpec: Int
    get() = layoutOutput.widthSpec

  val heightSpec: Int
    get() = layoutOutput.heightSpec

  val sizeConstraints: SizeConstraints?
    get() = layoutOutput.sizeConstraints

  val delegate: LayoutResult?
    get() = layoutOutput.delegate

  val diffNode: DiffNode?
    get() = layoutOutput.diffNode

  val measureHadExceptions: Boolean
    get() = layoutOutput.measureHadExceptions

  val wasMeasured: Boolean
    get() = layoutOutput.wasMeasured

  val contentWidth: Int
    get() = layoutOutput.contentWidth

  val contentHeight: Int
    get() = layoutOutput.contentHeight

  val lastMeasuredSize: Long
    get() = layoutOutput.lastMeasuredSize

  val isCachedLayout: Boolean
    get() = layoutOutput.isCachedLayout

  val isDiffedLayout: Boolean
    get() = layoutOutput._isDiffedLayout

  val cachedMeasuresValid: Boolean
    get() = layoutOutput.cachedMeasuresValid

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

  val layoutDirection: LayoutDirection
    get() = layoutOutput.layoutDirection

  val contentRenderUnit: LithoRenderUnit?
    get() = layoutOutput.contentRenderUnit

  val hostRenderUnit: LithoRenderUnit?
    get() = layoutOutput.hostRenderUnit

  val backgroundRenderUnit: LithoRenderUnit?
    get() = layoutOutput.backgroundRenderUnit

  val foregroundRenderUnit: LithoRenderUnit?
    get() = layoutOutput.foregroundRenderUnit

  val borderRenderUnit: LithoRenderUnit?
    get() = layoutOutput.borderRenderUnit

  @get:Px
  override val width: Int
    get() = layoutOutput.width

  @get:Px
  override val height: Int
    get() = layoutOutput.height

  @get:Px
  override val paddingTop: Int
    get() = layoutOutput.paddingTop

  @get:Px
  override val paddingRight: Int
    get() = layoutOutput.paddingRight

  @get:Px
  override val paddingBottom: Int
    get() = layoutOutput.paddingBottom

  @get:Px
  override val paddingLeft: Int
    get() = layoutOutput.paddingLeft

  override val renderUnit: LithoRenderUnit?
    get() = null // Unimplemented.

  override val childrenCount: Int
    get() = children.size

  override fun getChildAt(index: Int): LithoLayoutResult = children[index]

  override fun getXForChildAtIndex(index: Int): Int = children[index].layoutOutput.x

  override fun getYForChildAtIndex(index: Int): Int = children[index].layoutOutput.y

  override val layoutData: Any?
    get() = layoutOutput.layoutData

  fun adjustedLeft(): Int = layoutOutput.adjustedBounds.left

  fun adjustedTop(): Int = layoutOutput.adjustedBounds.top

  fun adjustedRight(): Int = layoutOutput.adjustedBounds.right

  fun adjustedBottom(): Int = layoutOutput.adjustedBounds.bottom

  /**
   * Since layout data like the layout context and the diff node are not required after layout
   * calculation they can be released to free up memory.
   */
  open fun releaseLayoutPhaseData() {
    layoutOutput.clear()
    for (i in 0 until childCount) {
      getChildAt(i).releaseLayoutPhaseData()
    }
  }

  fun addChild(child: LithoLayoutResult) {
    children.add(child)
  }

  private fun isDirectionRtl(): Boolean = layoutOutput.layoutDirection.isRTL
}
