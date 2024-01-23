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
import android.util.Pair
import androidx.annotation.Px
import com.facebook.litho.drawable.BorderColorDrawable
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
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

  var widthSpec: Int = DiffNode.UNSPECIFIED
    internal set

  var heightSpec: Int = DiffNode.UNSPECIFIED
    internal set

  var delegate: LayoutResult? = null
    internal set

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

  val touchExpansionBottom: Int
    get() =
        if (shouldApplyTouchExpansion()) {
          node.touchExpansion?.let { edges -> FastMath.round(edges[YogaEdge.BOTTOM]) } ?: 0
        } else 0

  val touchExpansionLeft: Int
    get() =
        if (shouldApplyTouchExpansion()) {
          node.touchExpansion?.let { edges ->
            FastMath.round(resolveHorizontalEdges(edges, YogaEdge.LEFT))
          } ?: 0
        } else 0

  val touchExpansionRight: Int
    get() =
        if (shouldApplyTouchExpansion()) {
          node.touchExpansion?.let { edges ->
            FastMath.round(resolveHorizontalEdges(edges, YogaEdge.RIGHT))
          } ?: 0
        } else 0

  val touchExpansionTop: Int
    get() =
        if (shouldApplyTouchExpansion()) {
          node.touchExpansion?.let { edges -> FastMath.round(edges[YogaEdge.TOP]) } ?: 0
        } else 0

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

  var contentRenderUnit: LithoRenderUnit? = null
    internal set

  var hostRenderUnit: LithoRenderUnit? = null
    internal set

  var backgroundRenderUnit: LithoRenderUnit? = null
    internal set

  var foregroundRenderUnit: LithoRenderUnit? = null
    internal set

  var borderRenderUnit: LithoRenderUnit? = null
    internal set

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

  fun setSizeSpec(widthSpec: Int, heightSpec: Int) {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
  }

  fun addChild(child: LithoLayoutResult) {
    children.add(child)
  }

  companion object {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun getLayoutContextFromYogaNode(yogaNode: YogaNode): LayoutContext<LithoLayoutContext> =
        (yogaNode.data as Pair<*, *>).first as LayoutContext<LithoLayoutContext>

    @JvmStatic
    fun getLayoutResultFromYogaNode(yogaNode: YogaNode): LithoLayoutResult =
        (yogaNode.data as Pair<*, *>).second as LithoLayoutResult

    internal fun createBorderColorDrawable(result: LithoLayoutResult): BorderColorDrawable {
      val node = result.node
      val isRtl = result.recursivelyResolveLayoutDirection() == YogaDirection.RTL
      val borderRadius = node.borderRadius
      val borderColors = node.borderColors
      val leftEdge = if (isRtl) YogaEdge.RIGHT else YogaEdge.LEFT
      val rightEdge = if (isRtl) YogaEdge.LEFT else YogaEdge.RIGHT
      return BorderColorDrawable.Builder()
          .pathEffect(node.borderPathEffect)
          .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
          .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
          .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
          .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
          .borderLeftWidth(result.getLayoutBorder(leftEdge))
          .borderTopWidth(result.getLayoutBorder(YogaEdge.TOP))
          .borderRightWidth(result.getLayoutBorder(rightEdge))
          .borderBottomWidth(result.getLayoutBorder(YogaEdge.BOTTOM))
          .borderRadius(borderRadius)
          .build()
    }

    internal fun LithoLayoutResult.shouldDrawBorders(): Boolean =
        node.hasBorderColor() &&
            (lithoLayoutOutput.yogaNode.getLayoutBorder(YogaEdge.LEFT) != 0f ||
                lithoLayoutOutput.yogaNode.getLayoutBorder(YogaEdge.TOP) != 0f ||
                lithoLayoutOutput.yogaNode.getLayoutBorder(YogaEdge.RIGHT) != 0f ||
                lithoLayoutOutput.yogaNode.getLayoutBorder(YogaEdge.BOTTOM) != 0f)

    private fun LithoLayoutResult.shouldApplyTouchExpansion(): Boolean =
        node.touchExpansion != null && (node.nodeInfo?.hasTouchEventHandlers() == true)

    private fun LithoLayoutResult.resolveHorizontalEdges(spacing: Edges, edge: YogaEdge): Float {
      val isRtl = lithoLayoutOutput.yogaNode.layoutDirection == YogaDirection.RTL
      val resolvedEdge =
          when (edge) {
            YogaEdge.LEFT -> (if (isRtl) YogaEdge.END else YogaEdge.START)
            YogaEdge.RIGHT -> (if (isRtl) YogaEdge.START else YogaEdge.END)
            else -> throw IllegalArgumentException("Not an horizontal padding edge: $edge")
          }
      var result = spacing.getRaw(resolvedEdge)
      if (YogaConstants.isUndefined(result)) {
        result = spacing[edge]
      }
      return result
    }

    private fun LithoLayoutResult.recursivelyResolveLayoutDirection(): YogaDirection {
      val direction = lithoLayoutOutput.yogaNode.layoutDirection
      check(direction != YogaDirection.INHERIT) {
        "Direction cannot be resolved before layout calculation"
      }
      return direction
    }

    fun LithoLayoutResult.getLayoutBorder(edge: YogaEdge?): Int =
        FastMath.round(lithoLayoutOutput.yogaNode.getLayoutBorder(edge))

    internal fun shouldAlwaysRemeasure(component: Component): Boolean =
        if (component is SpecGeneratedComponent) {
          component.shouldAlwaysRemeasure()
        } else {
          false
        }
  }
}
