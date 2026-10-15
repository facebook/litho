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
import com.facebook.rendercore.SizeConstraints

/** Resolved edges — margins, padding or border — in pixels, by physical edge. */
internal class IntEdges(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  val horizontal: Int
    get() = left + right

  val vertical: Int
    get() = top + bottom

  companion object {
    val None: IntEdges = IntEdges(0, 0, 0, 0)
  }
}

/**
 * How a container child sits within the constraints it is measured under: its resolved edges, in
 * pixels by physical edge, and the constraints its border box is laid out in.
 *
 * The child's answer is its border box plus [margins]. It does not have to fit the constraints: a
 * child's own width, height, min and max win over them, so the container coerces the child's answer
 * into them.
 */
internal class ChildBox(
    val margins: IntEdges,
    val padding: IntEdges,
    val border: IntEdges,
    /** The constraints for the border box. An exact axis has `min == max`. */
    val borderBoxConstraints: SizeConstraints,
)

/**
 * Applies a container child's own box props within [constraints], the room its container measures
 * it under.
 *
 * Per axis, in order:
 * 1. **Percentages** are taken of the room before margins: a size, min or max of its own axis's
 *    maximum, and is ignored when that axis is unbounded; a margin or padding of the width, as in
 *    CSS, and is 0 when the width is unbounded.
 * 2. **Margins** come out of the room: both of its bounds drop by them, so a negative margin raises
 *    both, the minimum included.
 * 3. **Own min** replaces the room's minimum, as Compose's `requiredWidthIn` does: a min under a
 *    larger exact room lets the content choose between the two. **Own max** only narrows the room,
 *    as `widthIn` does, so `maxWidth` caps a child as it does in a Column. When the min is above
 *    the max, the max wins, as in Compose.
 * 4. **Own width or height** fixes the axis at that size, limited only by the child's own min and
 *    max, whatever the room, as Compose's `requiredWidth` does. A **percentage** size fixes it as
 *    `fillMaxWidth(fraction)` does: the fraction, at most 1, of the room's maximum, within the
 *    range so far.
 * 5. **Aspect ratio** derives the other axis when exactly one is fixed, limited only by that axis's
 *    own min and max. A ratio that is not a finite number above 0 counts as none, as in Yoga.
 * 6. **Padding and border** set the smallest the border box can be.
 *
 * So the border box plus margins may fall outside the room; the container coerces the child's
 * answer into it.
 *
 * Logical edges resolve as Yoga resolves them (see [BoxEdges.resolve]).
 */
internal fun resolveChildBox(
    constraints: SizeConstraints,
    props: BoxLayoutProps,
    isRtl: Boolean,
): ChildBox {
  val widthBase = if (constraints.hasBoundedWidth) constraints.maxWidth else null
  val margins = props.margin.resolve(isRtl, widthBase, allowNegative = true)
  val padding = props.padding.resolve(isRtl, widthBase)
  val border = props.border.resolve(isRtl, percentBase = null)

  val w = AxisRange(constraints.minWidth, constraints.maxWidth)
  val h = AxisRange(constraints.minHeight, constraints.maxHeight)
  w.removeMargins(margins.horizontal)
  h.removeMargins(margins.vertical)

  val heightBase = if (constraints.hasBoundedHeight) constraints.maxHeight else null
  val minWidth = props.minWidth.toPx(widthBase)
  val maxWidth = props.maxWidth.toPx(widthBase)
  val minHeight = props.minHeight.toPx(heightBase)
  val maxHeight = props.maxHeight.toPx(heightBase)
  w.requireWithin(minWidth, maxWidth)
  h.requireWithin(minHeight, maxHeight)
  w.fixAt(props.width, widthBase, minWidth, maxWidth)
  h.fixAt(props.height, heightBase, minHeight, maxHeight)
  val ratio = props.aspectRatio
  if (ratio.isFinite() && ratio > 0f) {
    if (w.isExact && !h.isExact) {
      h.fix(coerceOwn(FastMath.round(w.min / ratio), minHeight, maxHeight))
    } else if (h.isExact && !w.isExact) {
      w.fix(coerceOwn(FastMath.round(h.min * ratio), minWidth, maxWidth))
    }
  }
  w.floorAt(padding.horizontal + border.horizontal)
  h.floorAt(padding.vertical + border.vertical)
  return ChildBox(
      margins = margins,
      padding = padding,
      border = border,
      borderBoxConstraints =
          encodableConstraints(
              minWidth = w.min,
              maxWidth = w.max,
              minHeight = h.min,
              maxHeight = h.max,
          ),
  )
}

/**
 * [SizeConstraints] for a border box ranging [minWidth] to [maxWidth] by [minHeight] to
 * [maxHeight], kept to what it can encode. No size is above 1 073 741 822 px, the most an axis
 * stores; an unbounded maximum stays unbounded. An axis with a minimum above 0 and a different
 * maximum stores its minimum in 13 bits and its maximum in 18, so far past any screen it is
 * narrowed further: a minimum above 8 190 px fixes the axis at that minimum, and a maximum above
 * 262 142 px is cut to it.
 */
private fun encodableConstraints(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int,
): SizeConstraints {
  val boundedMinWidth = minWidth.coerceAtMost(MAX_SIZE)
  val boundedMinHeight = minHeight.coerceAtMost(MAX_SIZE)
  return SizeConstraints(
      minWidth = boundedMinWidth,
      maxWidth = encodableMax(boundedMinWidth, maxWidth),
      minHeight = boundedMinHeight,
      maxHeight = encodableMax(boundedMinHeight, maxHeight),
  )
}

private fun encodableMax(min: Int, max: Int): Int {
  val bounded = if (max == SizeConstraints.Infinity) max else max.coerceAtMost(MAX_SIZE)
  return when {
    min == bounded || min == 0 -> bounded
    min > MAX_RANGE_MIN -> min
    bounded == SizeConstraints.Infinity -> bounded
    else -> minOf(bounded, MAX_RANGE_MAX)
  }
}

/** The largest size a [SizeConstraints] axis encodes. */
private const val MAX_SIZE: Int = 0x3FFFFFFF - 1

/** The largest minimum, and maximum, a [SizeConstraints] range with a minimum above 0 encodes. */
private const val MAX_RANGE_MIN: Int = 0x1FFF - 1
private const val MAX_RANGE_MAX: Int = 0x3FFFF - 1

/** [size] within a child's own [ownMin] and [ownMax]; when they conflict, the maximum wins. */
private fun coerceOwn(size: Int, ownMin: Int?, ownMax: Int?): Int {
  val upper = ownMax?.coerceAtLeast(0) ?: Int.MAX_VALUE
  return size.coerceIn((ownMin ?: 0).coerceIn(0, upper), upper)
}

/** One axis of the constraints a child's border box is laid out in. [max] may be unbounded. */
private class AxisRange(var min: Int, var max: Int) {

  val isExact: Boolean
    get() = min == max

  fun removeMargins(margins: Int) {
    min = (min - margins).coerceAtLeast(0)
    if (max != INFINITY) {
      max = (max - margins).coerceAtLeast(0)
    }
    if (min > max) {
      min = max
    }
  }

  /**
   * Applies the child's own bounds: [ownMin] becomes the minimum, as Compose's `requiredWidthIn`
   * does, and [ownMax] lowers the maximum but never raises it, as `widthIn` does. The other bound
   * moves only as far as the declared one needs. When [ownMin] is above [ownMax], the maximum wins.
   */
  fun requireWithin(ownMin: Int?, ownMax: Int?) {
    if (ownMin == null && ownMax == null) {
      return
    }
    val upper = ownMax?.coerceAtLeast(0) ?: INFINITY
    val lower = (ownMin ?: 0).coerceIn(0, upper)
    val newMin = if (ownMin != null) lower else min.coerceAtMost(upper)
    val newMax = (if (ownMax != null) minOf(upper, max) else max).coerceAtLeast(newMin)
    min = newMin
    max = newMax
  }

  /**
   * Fixes the axis at the child's own [size], if it declares one: a size in pixels within [ownMin]
   * and [ownMax], whatever the range, as Compose's `requiredWidth` does; a percentage of [base], at
   * most all of it, within the range, as `fillMaxWidth(fraction)` does. A percentage with no
   * [base], or a size that is not a finite number, has no effect.
   */
  fun fixAt(size: BoxLength, base: Int?, ownMin: Int?, ownMax: Int?) {
    when (size) {
      is BoxLength.Px -> size.toPx(base)?.let { fix(coerceOwn(it, ownMin, ownMax)) }
      is BoxLength.Percent ->
          if (base != null && size.value.isFinite()) {
            val fraction = (size.value / 100f).coerceIn(0f, 1f)
            fix(FastMath.round(fraction * base).coerceIn(min, max))
          }
      BoxLength.Unset -> Unit
    }
  }

  fun fix(size: Int) {
    min = size
    max = size
  }

  /** Raises the range to at least [floor]. */
  fun floorAt(floor: Int) {
    if (min < floor) {
      min = floor
    }
    if (max < min) {
      max = min
    }
  }

  private companion object {
    const val INFINITY: Int = SizeConstraints.Infinity
  }
}
