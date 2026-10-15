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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.LayoutResult

/** This is the default implementation of a [LayoutResult] for Litho. */
open class LithoLayoutResult(
    val context: ComponentContext,
    open val node: LithoNode,
    // Eventually, this will be replaced by LithoLayoutOutput.
    val layoutOutput: YogaLayoutOutput,
) : LayoutResult {

  private val children: MutableList<LithoLayoutResult> = ArrayList()

  val widthSpec: Int
    get() = layoutOutput.widthSpec

  val heightSpec: Int
    get() = layoutOutput.heightSpec

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
      // Manual, developer-set per-edge touch expansion. The per-edge getters already return 0 when
      // the node has no touch expansion set or has no attached touch event handler (see
      // LithoNode.shouldApplyTouchExpansion).
      val hasManualExpansion = node.hasTouchExpansion()
      var left = if (hasManualExpansion) touchExpansionLeft else 0
      var top = if (hasManualExpansion) touchExpansionTop else 0
      var right = if (hasManualExpansion) touchExpansionRight else 0
      var bottom = if (hasManualExpansion) touchExpansionBottom else 0

      // Grow an interactive component's touch target to the configured minimum, augmenting any
      // manual expansion. Only the touch area changes, not the visual bounds.
      if (
          ComponentsConfiguration.useAutoTouchExpansion &&
              node.nodeInfo?.hasTouchEventHandlers() == true
      ) {
        val minPx =
            context.resourceResolver.dipsToPixels(
                ComponentsConfiguration.minTouchTargetSizeDp.toFloat(),
            )
        val deficitX = minPx - (width + left + right)
        if (deficitX > 0) {
          val extraLeft = deficitX / 2
          left += extraLeft
          right += deficitX - extraLeft
        }
        val deficitY = minPx - (height + top + bottom)
        if (deficitY > 0) {
          val extraTop = deficitY / 2
          top += extraTop
          bottom += deficitY - extraTop
        }
      }

      return if (left == 0 && top == 0 && right == 0 && bottom == 0) {
        null
      } else {
        Rect(left, top, right, bottom)
      }
    }

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

  val effects: List<Attachable>?
    get() = layoutOutput.effects

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

  val borderTop: Int
    get() = layoutOutput.borderTop

  val borderRight: Int
    get() = layoutOutput.borderRight

  val borderBottom: Int
    get() = layoutOutput.borderBottom

  val borderLeft: Int
    get() = layoutOutput.borderLeft

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

  /**
   * Called once for each result in a pass's final tree, after layout and before the tree is
   * reduced, for work only a result in that tree may do. Most results have none.
   */
  internal open fun onFinalized(lithoLayoutContext: LithoLayoutContext) {}

  /** The `Layout` that placed this result, which holds its position; null for any other. */
  private var placedBy: LithoLayoutResult? = null

  /**
   * This result for [container] to place: itself, the first time or for the container already
   * placing it; otherwise a copy, since a result holds one position. This is how two measurements
   * of a container that share a child's layout each place it at a position of its own.
   */
  internal fun acquireForPlacement(container: LithoLayoutResult): LithoLayoutResult {
    if (placedBy == null || placedBy === container) {
      placedBy = container
      return this
    }
    return copyForPlacement().also { it.placedBy = container }
  }

  /**
   * A copy of this result with a position of its own: the same layout, sharing its children, since
   * their positions are within it.
   */
  protected open fun copyForPlacement(): LithoLayoutResult {
    val output = layoutOutput
    val copy =
        node.createLayoutResult(
            layoutOutput =
                output
                    .copy(_adjustedBounds = Rect(output.adjustedBounds))
                    .inheritStateReads(output),
        )
    for (i in 0 until childCount) {
      copy.addChild(getChildAt(i))
    }
    copy.layoutOutput.boxGeometry = output.boxGeometry
    copy.layoutOutput.resolvedMargins = output.resolvedMargins
    copy.layoutOutput.childBoxProps = output.childBoxProps
    copy.subtreeHasCustomLayout = subtreeHasCustomLayout
    return copy
  }

  /** Whether this result is a `Layout`'s, or holds one below it. */
  internal var subtreeHasCustomLayout: Boolean = false

  fun addChild(child: LithoLayoutResult) {
    children.add(child)
  }

  private fun isDirectionRtl(): Boolean = layoutOutput.layoutDirection.isRTL
}
