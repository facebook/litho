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

import com.facebook.litho.SizeSpec
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints

/** Resolved edges — margins, padding or border — in pixels, by physical edge. */
internal data class IntEdges(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  val horizontal: Int
    get() = left + right

  val vertical: Int
    get() = top + bottom

  companion object {
    val None: IntEdges = IntEdges(0, 0, 0, 0)
  }
}

/**
 * How a container child, or a root `Layout`, sits within the constraints it is measured under: its
 * resolved edges, in pixels by physical edge, and the constraints its border box is laid out in.
 *
 * The child's answer is its border box plus [margins]. It does not have to fit the constraints: a
 * child's own width, height, min and max win over them, so the container coerces the child's answer
 * into them. A root has no container: [resolveRootBox] keeps it within their maximum unless its
 * edges need more.
 */
internal data class BoxConstraints(
    val margins: IntEdges,
    val padding: IntEdges,
    val border: IntEdges,
    /** The constraints for the border box. An exact axis has `min == max`. */
    val borderBox: SizeConstraints,
) {
  /** The padding and border across: what its content box is narrower than its border box by. */
  val edgesAcross: Int
    get() = padding.horizontal + border.horizontal

  /** The padding and border down: what its content box is shorter than its border box by. */
  val edgesDown: Int
    get() = padding.vertical + border.vertical
}

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
internal fun resolveBoxConstraints(
    constraints: SizeConstraints,
    props: BoxLayoutProps,
    isRtl: Boolean,
): BoxConstraints {
  val roomMinWidth = constraints.minWidth
  val roomMaxWidth = constraints.maxWidth
  val roomMinHeight = constraints.minHeight
  val roomMaxHeight = constraints.maxHeight
  val widthBase = if (roomMaxWidth != SizeConstraints.Infinity) roomMaxWidth else null
  val margins = props.margin.resolve(isRtl, widthBase, allowNegative = true)
  val padding = props.padding.resolve(isRtl, widthBase)
  val border = props.border.resolve(isRtl, percentBase = null)

  val widthRange = AxisRange(roomMinWidth, roomMaxWidth)
  val heightRange = AxisRange(roomMinHeight, roomMaxHeight)
  widthRange.removeMargins(margins.horizontal)
  heightRange.removeMargins(margins.vertical)

  val heightBase = if (roomMaxHeight != SizeConstraints.Infinity) roomMaxHeight else null
  val minWidth = props.minWidth.toPx(widthBase)
  val maxWidth = props.maxWidth.toPx(widthBase)
  val minHeight = props.minHeight.toPx(heightBase)
  val maxHeight = props.maxHeight.toPx(heightBase)
  widthRange.requireWithin(minWidth, maxWidth)
  heightRange.requireWithin(minHeight, maxHeight)
  widthRange.fixAt(props.width, widthBase, minWidth, maxWidth)
  heightRange.fixAt(props.height, heightBase, minHeight, maxHeight)
  val ratio = props.aspectRatio
  if (ratio.isFinite() && ratio > 0f) {
    if (widthRange.isExact && !heightRange.isExact) {
      heightRange.fix(coerceOwn(FastMath.round(widthRange.min / ratio), minHeight, maxHeight))
    } else if (heightRange.isExact && !widthRange.isExact) {
      widthRange.fix(coerceOwn(FastMath.round(heightRange.min * ratio), minWidth, maxWidth))
    }
  }
  widthRange.floorAt(padding.horizontal + border.horizontal)
  heightRange.floorAt(padding.vertical + border.vertical)
  return BoxConstraints(
      margins = margins,
      padding = padding,
      border = border,
      borderBox =
          encodableConstraints(
              minWidth = widthRange.min,
              maxWidth = widthRange.max,
              minHeight = heightRange.min,
              maxHeight = heightRange.max,
          ),
  )
}

/**
 * Applies a root `Layout`'s own box props within [constraints], the ones its holder lays it out
 * under. Its holder, the view for a tree's root or the node that holds deferred content, gives two
 * things: a room from 0 to the constraints' maximum on each axis, which its margin box stays within
 * unless its margins, padding and border alone need more, and a default for a size the root doesn't
 * declare. The root may be smaller than its room, as a `wrap_content` child of an Android
 * `FrameLayout` may be. Its default fills an exact axis and wraps the content on a bounded or
 * unbounded one. Its holder builds [constraints] from measure specs, so on each axis the minimum is
 * 0 or equals the maximum, and the room from 0 loses nothing.
 *
 * Within that, the root's own style decides, per axis:
 * 1. **Margins** come out of the room, and are where its holder places it. An own max bounds the
 *    border box alone, the margins outside it, as CSS's `max-width` does under border-box sizing,
 *    so an own max never takes the margin box out of the room. A Yoga root doesn't do either
 *    consistently: on an exact axis its max ignores the margins and the margin box can pass its
 *    holder; on an at-most or unbounded one it takes the margins out of the max. A percentage
 *    margin or padding is of the width's maximum, as in CSS, and 0 when the width is unbounded.
 * 2. **Own width or height**, in pixels or as a percentage of the constraints' maximum, is used,
 *    then kept within its own min and max and within the room. A percentage of an unbounded axis
 *    has no effect.
 * 3. **Own min and max** apply within the room; when the min is above the max, the max wins, as for
 *    a `Layout`'s child ([resolveBoxConstraints]) and in Compose. A Yoga root keeps one or the
 *    other depending on its declared size and the constraints; CSS keeps the min.
 * 4. **Padding and border** set the smallest the border box can be, even past the room.
 * 5. **Aspect ratio** derives the other axis when exactly one is decided, within each axis's range,
 *    padding and border included. When the derived one would leave its range, it takes its nearest
 *    bound, and an axis its holder decided follows it; a declared size stays and the ratio yields,
 *    as for a child. A declared size below the floor its padding and border set derives the other
 *    axis at that floor, where a child derives it from the declared size: the one place a root and
 *    a child differ on a declared size within the room. When neither is decided, the bounded one is
 *    decided at its maximum, the width first, as Compose's `aspectRatio` takes the most room that
 *    keeps the ratio. When no size in both ranges keeps it, the ratio yields too, the decided axis
 *    kept. A ratio that is not a finite number above 0, or whose inverse is not finite, counts as
 *    none.
 * 6. **Position insets** are not read: its holder places it at its margins.
 *
 * A root that has to be larger than its holder goes in a scroll container, whose room for it is
 * unbounded. On an at-most or unbounded axis, a holder sized to the border box alone leaves no
 * space for a margin there, so part of the border box falls outside it, as for a `Column` root.
 *
 * A `Layout`'s child is resolved by [resolveBoxConstraints] instead: its room's minimum is a
 * requirement it fills, where a root may be smaller than its room.
 */
internal fun resolveRootBox(
    constraints: SizeConstraints,
    props: BoxLayoutProps,
    isRtl: Boolean,
): BoxConstraints {
  val widthBase = if (constraints.hasBoundedWidth) constraints.maxWidth else null
  val heightBase = if (constraints.hasBoundedHeight) constraints.maxHeight else null
  val margins = props.margin.resolve(isRtl, widthBase, allowNegative = true)
  val padding = props.padding.resolve(isRtl, widthBase)
  val border = props.border.resolve(isRtl, percentBase = null)

  val minWidth = props.minWidth.toPx(widthBase)
  val maxWidth = props.maxWidth.toPx(widthBase)
  val minHeight = props.minHeight.toPx(heightBase)
  val maxHeight = props.maxHeight.toPx(heightBase)
  val width = RootAxis(constraints.maxWidth, margins.horizontal, minWidth, maxWidth)
  val height = RootAxis(constraints.maxHeight, margins.vertical, minHeight, maxHeight)
  width.decide(props.width, widthBase, isExact = constraints.hasExactWidth)
  height.decide(props.height, heightBase, isExact = constraints.hasExactHeight)
  width.floorAt(padding.horizontal + border.horizontal)
  height.floorAt(padding.vertical + border.vertical)
  val ratio = props.aspectRatio
  if (ratio.isFinite() && ratio > 0f && (1f / ratio).isFinite()) {
    if (width.size == null && height.size == null) {
      if (width.max != SizeConstraints.Infinity) {
        width.fix(width.max)
      } else if (height.max != SizeConstraints.Infinity) {
        height.fix(height.max)
      }
    }
    val decidedWidth = width.size
    val decidedHeight = height.size
    if (decidedWidth != null && decidedHeight == null) {
      keepRatio(decided = width, derived = height, decidedWidth, ratio, derivesHeight = true)
    } else if (decidedHeight != null && decidedWidth == null) {
      keepRatio(decided = height, derived = width, decidedHeight, ratio, derivesHeight = false)
    }
  }
  return BoxConstraints(
      margins = margins,
      padding = padding,
      border = border,
      borderBox =
          encodableConstraints(
              minWidth = width.borderBoxMin,
              maxWidth = width.borderBoxMax,
              minHeight = height.borderBoxMin,
              maxHeight = height.borderBoxMax,
          ),
  )
}

/**
 * Decides [derived] from [decided]'s [decidedSize] by [ratio], keeping the ratio: when the derived
 * size leaves its range, it's kept at the nearest bound and [decided], unless declared, is derived
 * back from it, or, past its own range, takes the nearest size in range that derives that bound.
 * Otherwise the ratio yields: [decided] keeps [decidedSize] and [derived] stays at that bound, the
 * nearest it can come to the ratio.
 */
private fun keepRatio(
    decided: RootAxis,
    derived: RootAxis,
    decidedSize: Int,
    ratio: Float,
    derivesHeight: Boolean,
) {
  val derivedSize = sizeByRatio(decidedSize, ratio, toHeight = derivesHeight)
  if (derivedSize in derived.min..derived.max) {
    derived.fix(derivedSize)
    return
  }
  val derivedInRange = derivedSize.coerceIn(derived.min, derived.max)
  val decidedForBound = sizeByRatio(derivedInRange, ratio, toHeight = !derivesHeight)
  val decidedInRange = decidedForBound.coerceIn(decided.min, decided.max)
  if (
      !decided.isDeclared &&
          (decidedInRange == decidedForBound ||
              sizeByRatio(decidedInRange, ratio, toHeight = derivesHeight) == derivedInRange)
  ) {
    decided.fix(decidedInRange)
  }
  derived.fix(derivedInRange)
}

/**
 * The size [ratio] derives from [size]: a height from a width when [toHeight], else a width from a
 * height, rounded as [resolveBoxConstraints] rounds it.
 */
private fun sizeByRatio(size: Int, ratio: Float, toHeight: Boolean): Int =
    if (toHeight) FastMath.round(size / ratio) else FastMath.round(size * ratio)

/**
 * One axis of a root's border box: its range, the room its holder gives, from 0 to [roomMax] less
 * [margins], narrowed by its own [ownMin] and [ownMax] and raised to the floor its padding and
 * border set ([floorAt]); its [size], once it's decided; and whether that size is its own declared
 * one ([isDeclared]).
 */
private class RootAxis(roomMax: Int, margins: Int, ownMin: Int?, ownMax: Int?) {
  var min: Int
    private set

  var max: Int
    private set

  var size: Int? = null
    private set

  var isDeclared: Boolean = false
    private set

  init {
    val room =
        if (roomMax == SizeConstraints.Infinity) roomMax else (roomMax - margins).coerceAtLeast(0)
    max = ownMax?.coerceIn(0, room) ?: room
    min = (ownMin ?: 0).coerceIn(0, max)
  }

  /** The border box's least size: its size once decided, else its range's minimum. */
  val borderBoxMin: Int
    get() = size ?: min

  /** The border box's greatest size: its size once decided, else its range's maximum. */
  val borderBoxMax: Int
    get() = size ?: max

  /**
   * Decides the axis at its own [declared] size, in pixels or a percentage of [base], within its
   * range; or, with none, at its range's maximum when the axis is exact. An undecided axis wraps
   * its content.
   */
  fun decide(declared: BoxLength, base: Int?, isExact: Boolean) {
    val declaredPx =
        when (declared) {
          is BoxLength.Px -> declared.toPx(base)
          is BoxLength.Percent ->
              if (base != null && declared.value.isFinite()) {
                // Divides first, as resolveBoxConstraints does; toPx rounds some halves
                // differently.
                FastMath.round(declared.value / 100f * base)
              } else {
                null
              }
          BoxLength.Unset -> null
        }
    when {
      declaredPx != null -> {
        size = declaredPx.coerceIn(min, max)
        isDeclared = true
      }
      isExact && max != SizeConstraints.Infinity -> size = max
    }
  }

  fun fix(size: Int) {
    this.size = size
  }

  /** Raises the range, and its size, to at least [floor]. */
  fun floorAt(floor: Int) {
    if (min < floor) min = floor
    if (max < min) max = min
    size?.let { if (it < floor) size = floor }
  }
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

/**
 * The border-box size [box] gives a leaf, given [measure], which measures its content under a width
 * and a height spec. [measure] is not called when both axes are exact, as Yoga does not call a
 * leaf's measure function then.
 *
 * It's the border-box size around the content it measures: [borderBoxSizeAround].
 */
internal inline fun borderBoxSize(
    box: BoxConstraints,
    measure: (Int, Int) -> IntSize,
): IntSize {
  val room = box.borderBox
  if (room.hasExactWidth && room.hasExactHeight) {
    return borderBoxSizeAround(0, 0, box)
  }
  val content = measure(
      contentSpec(room.hasExactWidth, room.hasBoundedWidth, room.maxWidth, box.edgesAcross),
      contentSpec(room.hasExactHeight, room.hasBoundedHeight, room.maxHeight, box.edgesDown),
  )
  return borderBoxSizeAround(content.width, content.height, box)
}

/**
 * The border-box size around content of [contentWidth] x [contentHeight] in [box], as Yoga bounds a
 * measured root: on an exact axis it's the constraint, whatever the content; otherwise the content
 * plus the padding and border, within the box's minimum and maximum. Either way it's at least the
 * padding and border.
 */
internal fun borderBoxSizeAround(
    contentWidth: Int,
    contentHeight: Int,
    box: BoxConstraints,
): IntSize {
  val room = box.borderBox
  return IntSize(
      axisSize(room.hasExactWidth, room.minWidth, room.maxWidth, contentWidth, box.edgesAcross),
      axisSize(room.hasExactHeight, room.minHeight, room.maxHeight, contentHeight, box.edgesDown),
  )
}

/** The measure spec for the content: the room inside the padding and border. */
internal fun contentSpec(exact: Boolean, bounded: Boolean, max: Int, edges: Int): Int =
    when {
      exact -> SizeSpec.makeSizeSpec(maxOf(0, max - edges), SizeSpec.EXACTLY)
      bounded -> SizeSpec.makeSizeSpec(maxOf(0, max - edges), SizeSpec.AT_MOST)
      else -> SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED)
    }

/**
 * The border box along one axis, as Yoga bounds a measured root. Unbounded content, such as a
 * function declaring its unbounded maximum, stays unbounded with the edges added.
 */
internal fun axisSize(exact: Boolean, min: Int, max: Int, content: Int, edges: Int): Int {
  if (exact) {
    return maxOf(max, edges)
  }
  var size =
      if (content > SizeConstraints.Infinity - edges) SizeConstraints.Infinity else content + edges
  if (max != SizeConstraints.Infinity && size > max) {
    size = max
  } else if (min > 0 && size < min) {
    size = min
  }
  return maxOf(size, edges)
}
