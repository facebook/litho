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

package com.facebook.litho.layout

import com.facebook.rendercore.FastMath

/**
 * One box prop's value: unset, a size in pixels, or a percentage of a base. There is no `auto`: for
 * a container's child it would mean no size of its own, which is unset. A value that is not a
 * finite number counts as unset, as it does in Yoga.
 */
internal sealed class BoxLength {

  /** Not declared, or a size declared `auto`. */
  data object Unset : BoxLength()

  data class Px(val value: Float) : BoxLength()

  data class Percent(val value: Float) : BoxLength()

  /**
   * In pixels, a percentage taken of [base]; null when unset, a percentage of no base, or not a
   * finite number.
   */
  fun toPx(base: Int?): Int? =
      when (this) {
        is Px -> if (value.isFinite()) FastMath.round(value) else null
        is Percent ->
            if (base != null && value.isFinite()) FastMath.round(value * base / 100f) else null
        Unset -> null
      }
}

/** An edge a box prop can be declared on: physical, logical, an axis, or all four. */
internal enum class BoxEdge {
  LEFT,
  TOP,
  RIGHT,
  BOTTOM,
  START,
  END,
  HORIZONTAL,
  VERTICAL,
  ALL,
}

/**
 * One edge prop as declared — margins, padding or border — a [BoxLength] per [BoxEdge]. Declaring
 * an edge again replaces it, whatever the unit.
 */
internal class BoxEdges {
  private var declared: Array<BoxLength?>? = null

  operator fun set(edge: BoxEdge, length: BoxLength) {
    (declared ?: arrayOfNulls<BoxLength>(EDGE_COUNT).also { declared = it })[edge.ordinal] = length
  }

  /** The value declared on [edge] itself, or [BoxLength.Unset]. */
  operator fun get(edge: BoxEdge): BoxLength = declared?.get(edge.ordinal) ?: BoxLength.Unset

  /**
   * In pixels by physical edge, each from the first edge declared, in Yoga's precedence
   * (`Style::computeLeftEdge` and siblings): the start or end edge for the direction, then the
   * physical edge, then the axis, then [BoxEdge.ALL]. A percentage is taken of [percentBase], and
   * is 0 without one. Only margins may be negative.
   */
  fun resolve(isRtl: Boolean, percentBase: Int?, allowNegative: Boolean = false): IntEdges {
    if (declared == null) {
      return IntEdges.None
    }
    fun edge(physical: BoxEdge): Int {
      val px = px(physical, isRtl, percentBase)
      return if (allowNegative) px else maxOf(0, px)
    }
    val left = edge(BoxEdge.LEFT)
    val top = edge(BoxEdge.TOP)
    val right = edge(BoxEdge.RIGHT)
    val bottom = edge(BoxEdge.BOTTOM)
    return if (left == 0 && top == 0 && right == 0 && bottom == 0) IntEdges.None
    else IntEdges(left = left, top = top, right = right, bottom = bottom)
  }

  private fun px(physical: BoxEdge, isRtl: Boolean, percentBase: Int?): Int {
    val logical =
        when (physical) {
          BoxEdge.LEFT -> if (isRtl) BoxEdge.END else BoxEdge.START
          BoxEdge.RIGHT -> if (isRtl) BoxEdge.START else BoxEdge.END
          else -> null
        }
    val axis =
        if (physical == BoxEdge.TOP || physical == BoxEdge.BOTTOM) BoxEdge.VERTICAL
        else BoxEdge.HORIZONTAL
    return logical?.let { declaredPx(it, percentBase) }
        ?: declaredPx(physical, percentBase)
        ?: declaredPx(axis, percentBase)
        ?: declaredPx(BoxEdge.ALL, percentBase)
        ?: 0
  }

  /**
   * [edge] in pixels as declared on that edge itself; null when undeclared or not a finite number,
   * which Yoga treats as undeclared too.
   */
  private fun declaredPx(edge: BoxEdge, percentBase: Int?): Int? =
      when (val length = this[edge]) {
        is BoxLength.Px -> if (length.value.isFinite()) FastMath.round(length.value) else null
        is BoxLength.Percent ->
            when {
              !length.value.isFinite() -> null
              // Declared, so it still takes precedence over the edges behind it.
              percentBase == null -> 0
              else -> FastMath.round(length.value * percentBase / 100f)
            }
        BoxLength.Unset -> null
      }

  private companion object {
    val EDGE_COUNT: Int = BoxEdge.values().size
  }
}

/**
 * A custom-layout child's own box props, in Litho's own types.
 *
 * A flexbox parent reads a child's margins, size, min/max, aspect ratio, padding and border off the
 * child's Yoga node. A custom layout applies them itself, against the constraints it measures the
 * child under ([resolveChildBox]), so they are kept here rather than on the child's Yoga node.
 */
internal class BoxLayoutProps {
  var width: BoxLength = BoxLength.Unset
  var height: BoxLength = BoxLength.Unset
  var minWidth: BoxLength = BoxLength.Unset
  var maxWidth: BoxLength = BoxLength.Unset
  var minHeight: BoxLength = BoxLength.Unset
  var maxHeight: BoxLength = BoxLength.Unset
  var aspectRatio: Float = Float.NaN
  val margin: BoxEdges = BoxEdges()
  val padding: BoxEdges = BoxEdges()
  val border: BoxEdges = BoxEdges()
}
