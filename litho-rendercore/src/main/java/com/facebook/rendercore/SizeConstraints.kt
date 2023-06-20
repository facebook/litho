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

package com.facebook.rendercore

import android.view.View

/**
 * Size constraints for measuring layouts. The parent container can decide what [SizeConstraints]
 * will be passed to its children, which will use these constraints to measure themselves. When a
 * child measures itself, it should choose a size that fits within the [SizeConstraints] provided by
 * the parent:
 * - `minWidth` <= `childWidth` <= `maxWidth`
 * - `minHeight` <= `childHeight` <= `maxHeight`
 *
 * In order to avoid runtime overhead, [SizeConstraints] is a Kotlin value class that wraps a Long
 * value. That wrapped Long value encodes [minWidth], [minHeight], [maxWidth], and [maxHeight]
 * values. Each of them is represented on 16 bits, which means that the range of possible values is
 * 0 - 65536. There is also a special value for Infinity. When the decoded value is >= 0xFFFF, then
 * [Infinity] is returned. Creating [SizeConstraints] with values that don't fit this range will
 * throw an exception.
 */
@JvmInline
value class SizeConstraints internal constructor(@PublishedApi internal val encodedValue: Long) {
  /** Minimum width in pixels. It'll be always <= maxWidth. */
  val minWidth: Int
    get() {
      val value = encodedValue.shr(48).and(BitMask).toInt()
      return if (value < MaxValue) value else Infinity
    }

  /** Maximum width in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxWidth: Int
    get() {
      val value = encodedValue.shr(32).and(BitMask).toInt()
      return if (value < MaxValue) value else Infinity
    }

  /** Minimum height in pixels. It'll be always <= maxHeight. */
  val minHeight: Int
    get() {
      val value = encodedValue.shr(16).and(BitMask).toInt()
      return if (value < MaxValue) value else Infinity
    }

  /** Maximum height in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxHeight: Int
    get() {
      val value = encodedValue.and(BitMask).toInt()
      return if (value < MaxValue) value else Infinity
    }

  /** Returns true if maxWidth is != Infinity, false otherwise. */
  val hasBoundedWidth: Boolean
    get() = maxWidth != Infinity

  /** Returns true if maxHeight is != Infinity, false otherwise. */
  val hasBoundedHeight: Boolean
    get() = maxHeight != Infinity

  /**
   * Returns true if width is represented as a single value, otherwise returns false when width
   * represents a range of values
   */
  val hasExactWidth
    get() = maxWidth == minWidth

  /**
   * Returns true if height is represented as a single value, otherwise returns false when height
   * represents a range of values
   */
  val hasExactHeight
    get() = maxHeight == minHeight

  /** Returns true if the area of the component is zero. */
  val isZeroSize
    get() = maxWidth == 0 || maxHeight == 0

  fun copy(
      minWidth: Int = this.minWidth,
      maxWidth: Int = this.maxWidth,
      minHeight: Int = this.minHeight,
      maxHeight: Int = this.maxHeight
  ): SizeConstraints {
    return SizeConstraints(minWidth, maxWidth, minHeight, maxHeight)
  }

  override fun toString(): String {
    val maxWidthDescription = if (maxWidth == Infinity) "Infinity" else maxWidth
    val maxHeightDescription = if (maxHeight == Infinity) "Infinity" else maxHeight
    return "SizeConstraints[minWidth = $minWidth, maxWidth = $maxWidthDescription, " +
        "minHeight = $minHeight, maxHeight = $maxHeightDescription]"
  }

  companion object {

    /** A mask used for bit operations. */
    internal const val BitMask: Long = 0xFFFF

    /** A maximum value that can be stored in SizeConstraints. */
    const val MaxValue: Int = 0xFFFF

    /**
     * A value representing Infinity. It should be used to represent unbounded sizes. When
     * [maxWidth] is set to Infinity, then [hasBoundedWidth] returns false. Similarly when
     * [maxHeight] is set to Infinity, then [hasBoundedHeight] returns false.
     */
    // 0x20000000 is the largest power of two smaller than Int.MAX_VALUE / 2
    const val Infinity: Int = 0x20000000 - 1

    /** Creates [SizeConstraints] from the provided width and height [View.MeasureSpec]s. */
    fun fromMeasureSpecs(widthSpec: Int, heightSpec: Int): SizeConstraints {
      val widthMode = View.MeasureSpec.getMode(widthSpec)
      val widthSize = View.MeasureSpec.getSize(widthSpec).coerceIn(0, MaxValue - 1)
      val minWidth: Int
      val maxWidth: Int
      when (widthMode) {
        View.MeasureSpec.EXACTLY -> {
          minWidth = widthSize
          maxWidth = widthSize
        }
        View.MeasureSpec.UNSPECIFIED -> {
          minWidth = 0
          maxWidth = Infinity
        }
        View.MeasureSpec.AT_MOST -> {
          minWidth = 0
          maxWidth = widthSize
        }
        else -> throw IllegalStateException("Unknown width spec mode.")
      }

      val heightMode = View.MeasureSpec.getMode(heightSpec)
      val heightSize = View.MeasureSpec.getSize(heightSpec).coerceIn(0, MaxValue - 1)
      val minHeight: Int
      val maxHeight: Int
      when (heightMode) {
        View.MeasureSpec.EXACTLY -> {
          minHeight = heightSize
          maxHeight = heightSize
        }
        View.MeasureSpec.UNSPECIFIED -> {
          minHeight = 0
          maxHeight = Infinity
        }
        View.MeasureSpec.AT_MOST -> {
          minHeight = 0
          maxHeight = heightSize
        }
        else -> throw IllegalStateException("Unknown height spec mode.")
      }

      validateSizes(
          minWidth,
          maxWidth,
          minHeight,
          maxHeight,
          "minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight, widthSpec=[${View.MeasureSpec.toString(widthSpec)}], heightSpec=[${View.MeasureSpec.toString(heightSpec)}]")
      return SizeConstraints(minWidth, maxWidth, minHeight, maxHeight)
    }

    internal fun validateSizes(
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
        additionalErrorMessage: String
    ) {
      if (minWidth < 0) {
        throw IllegalArgumentException(
            "minWidth must be >= 0, but was: $minWidth $additionalErrorMessage")
      }
      if (minHeight < 0) {
        throw IllegalArgumentException(
            "minHeight must be >= 0, but was: $minHeight $additionalErrorMessage")
      }
      if (maxWidth >= MaxValue && maxWidth != Infinity) {
        throw IllegalArgumentException(
            "maxWidth must be < $MaxValue, but was: $maxWidth. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
      }
      if (maxHeight >= MaxValue && maxHeight != Infinity) {
        throw IllegalArgumentException(
            "maxHeight must be < $MaxValue, but was: $maxHeight. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
      }
      if (minWidth > maxWidth) {
        throw IllegalArgumentException(
            "maxWidth must be >= minWidth, but was: maxWidth=$maxWidth; minWidth=$minWidth $additionalErrorMessage")
      }
      if (minHeight > maxHeight) {
        throw IllegalArgumentException(
            "maxHeight must be >= minHeight, but was: maxHeight=$maxHeight; minHeight=$minHeight $additionalErrorMessage")
      }
    }

    internal fun valueOrInfinity(value: Int): Long {
      return (if (value == Infinity) MaxValue else value).toLong()
    }
  }
}

/** Creates [SizeConstraints] from the provided values. */
fun SizeConstraints(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int,
): SizeConstraints {
  SizeConstraints.validateSizes(
      minWidth,
      maxWidth,
      minHeight,
      maxHeight,
      "minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
  val v1 = SizeConstraints.valueOrInfinity(minWidth)
  val v2 = SizeConstraints.valueOrInfinity(maxWidth)
  val v3 = SizeConstraints.valueOrInfinity(minHeight)
  val v4 = SizeConstraints.valueOrInfinity(maxHeight)
  return SizeConstraints(
      encodedValue = v1.shl(48) or v2.shl(32) or v3.shl(16) or (v4 and SizeConstraints.BitMask))
}

/**
 * Returns the minWidth and maxWidth represented as width [View.MeasureSpec].
 *
 * **IMPORTANT** - this is a lossy conversion. With [View.MeasureSpec], it's not possible to
 * represent a range of values where the minimum is != 0 so the information about minWidth may be
 * lost.
 */
fun SizeConstraints.toWidthSpec(): Int {
  if (minWidth == maxWidth) {
    return View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.EXACTLY)
  }
  if (maxWidth != SizeConstraints.Infinity) {
    return View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST)
  }
  return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
}

/**
 * Returns the minHeight and maxHeight represented as width [View.MeasureSpec].
 *
 * **IMPORTANT** - this is a lossy conversion. With [View.MeasureSpec], it's not possible to
 * represent a range of values where the minimum is != 0 so the information about minHeight may be
 * lost.
 */
fun SizeConstraints.toHeightSpec(): Int {
  if (minHeight == maxHeight) {
    return View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.EXACTLY)
  }
  if (maxHeight != SizeConstraints.Infinity) {
    return View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
  }
  return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
}

/** Returns true if the size fits within the given constraints, otherwise returns false. */
fun Size.fitsWithin(sizeConstraints: SizeConstraints): Boolean {
  return (width in sizeConstraints.minWidth..sizeConstraints.maxWidth) &&
      (height in sizeConstraints.minHeight..sizeConstraints.maxHeight)
}
