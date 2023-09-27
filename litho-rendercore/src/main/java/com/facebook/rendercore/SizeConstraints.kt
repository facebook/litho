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
import android.view.View.MeasureSpec

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
      return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
        ExperimentalSizeConstraintsHelper.getMinWidth(encodedValue)
      } else {
        val value = encodedValue.shr(48).and(BitMask).toInt()
        if (value < MaxValue) value else Infinity
      }
    }

  /** Maximum width in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxWidth: Int
    get() {
      return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
        ExperimentalSizeConstraintsHelper.getMaxWidth(encodedValue)
      } else {
        val value = encodedValue.shr(32).and(BitMask).toInt()
        if (value < MaxValue) value else Infinity
      }
    }

  /** Minimum height in pixels. It'll be always <= maxHeight. */
  val minHeight: Int
    get() {
      return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
        ExperimentalSizeConstraintsHelper.getMinHeight(encodedValue)
      } else {
        val value = encodedValue.shr(16).and(BitMask).toInt()
        if (value < MaxValue) value else Infinity
      }
    }

  /** Maximum height in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxHeight: Int
    get() {
      return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
        ExperimentalSizeConstraintsHelper.getMaxHeight(encodedValue)
      } else {
        val value = encodedValue.and(BitMask).toInt()
        if (value < MaxValue) value else Infinity
      }
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
  val hasExactWidth: Boolean
    get() = maxWidth == minWidth

  /**
   * Returns true if height is represented as a single value, otherwise returns false when height
   * represents a range of values
   */
  val hasExactHeight: Boolean
    get() = maxHeight == minHeight

  /** Returns true if the area of the component is zero. */
  val isZeroSize: Boolean
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
    val Infinity: Int =
        if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled)
            ExperimentalSizeConstraintsHelper.Infinity
        else 0x20000000 - 1

    /** Creates [SizeConstraints] from the provided width and height [View.MeasureSpec]s. */
    fun fromMeasureSpecs(widthSpec: Int, heightSpec: Int): SizeConstraints {
      val maxSupportedWidth =
          if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
            ExperimentalSizeConstraintsHelper.Mode.forMeasureSpec(widthSpec).supportedRange.last
          } else {
            MaxValue - 1
          }
      val widthMode = View.MeasureSpec.getMode(widthSpec)
      val widthSize = MeasureSpec.getSize(widthSpec).coerceIn(0, maxSupportedWidth)
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

      val maxSupportedHeight =
          if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
            ExperimentalSizeConstraintsHelper.Mode.forMeasureSpec(heightSpec).supportedRange.last
          } else {
            MaxValue - 1
          }
      val heightMode = View.MeasureSpec.getMode(heightSpec)
      val heightSize = MeasureSpec.getSize(heightSpec).coerceIn(0, maxSupportedHeight)
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

      return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
        SizeConstraints(
            ExperimentalSizeConstraintsHelper.sizeConstraints(
                minWidth, maxWidth, minHeight, maxHeight))
      } else {
        validateSizes(
            minWidth,
            maxWidth,
            minHeight,
            maxHeight,
            Infinity,
            MaxValue - 1,
            MaxValue - 1,
            MaxValue - 1,
            MaxValue - 1,
            "minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight, widthSpec=[${View.MeasureSpec.toString(widthSpec)}], heightSpec=[${View.MeasureSpec.toString(heightSpec)}]")
        SizeConstraints(minWidth, maxWidth, minHeight, maxHeight)
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
  return if (RenderCoreConfig.isExperimentalSizeConstraintsEnabled) {
    SizeConstraints(
        ExperimentalSizeConstraintsHelper.sizeConstraints(minWidth, maxWidth, minHeight, maxHeight))
  } else {
    validateSizes(
        minWidth,
        maxWidth,
        minHeight,
        maxHeight,
        SizeConstraints.Infinity,
        SizeConstraints.MaxValue - 1,
        SizeConstraints.MaxValue - 1,
        SizeConstraints.MaxValue - 1,
        SizeConstraints.MaxValue - 1,
        "minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")

    val v1 = SizeConstraints.valueOrInfinity(minWidth)
    val v2 = SizeConstraints.valueOrInfinity(maxWidth)
    val v3 = SizeConstraints.valueOrInfinity(minHeight)
    val v4 = SizeConstraints.valueOrInfinity(maxHeight)
    SizeConstraints(
        encodedValue = v1.shl(48) or v2.shl(32) or v3.shl(16) or (v4 and SizeConstraints.BitMask))
  }
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

private fun validateSizes(
    minWidth: Int,
    maxWidth: Int,
    minHeight: Int,
    maxHeight: Int,
    infinity: Int,
    maxSupportedMinWidth: Int,
    maxSupportedMaxWidth: Int,
    maxSupportedMinHeight: Int,
    maxSupportedMaxHeight: Int,
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
  if (minWidth > maxSupportedMinWidth && minWidth != infinity) {
    throw IllegalArgumentException(
        "minWidth must be <= ${maxSupportedMinWidth}, but was: $minWidth. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
  }
  if (maxWidth > maxSupportedMaxWidth && maxWidth != infinity) {
    throw IllegalArgumentException(
        "maxWidth must be <= ${maxSupportedMaxWidth}, but was: $maxWidth. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
  }
  if (minHeight > maxSupportedMinHeight && minHeight != infinity) {
    throw IllegalArgumentException(
        "minHeight must be <= ${maxSupportedMinHeight}, but was: $minHeight. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
  }
  if (maxHeight > maxSupportedMaxHeight && maxHeight != infinity) {
    throw IllegalArgumentException(
        "maxHeight must be <= ${maxSupportedMaxHeight}, but was: $maxHeight. Components this big may affect performance and lead to out of memory errors. $additionalErrorMessage")
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

private object ExperimentalSizeConstraintsHelper {

  const val Infinity: Int = Int.MAX_VALUE

  fun sizeConstraints(
      minWidth: Int,
      maxWidth: Int,
      minHeight: Int,
      maxHeight: Int,
  ): Long {
    val widthMode: Mode = Mode.forRange(minWidth, maxWidth)
    val heightMode: Mode = Mode.forRange(minHeight, maxHeight)
    validateSizes(
        minWidth,
        maxWidth,
        minHeight,
        maxHeight,
        Infinity,
        widthMode.supportedRange.first,
        widthMode.supportedRange.last,
        heightMode.supportedRange.first,
        heightMode.supportedRange.last,
        "minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")

    val encodedWidth: Long = widthMode.encode(minWidth, maxWidth)
    val encodedHeight: Long = heightMode.encode(minHeight, maxHeight)

    return encodedWidth.shl(32) or encodedHeight
  }

  fun getMinWidth(sizeConstraints: Long): Int {
    return Mode.forConstraints(sizeConstraints.high).decodeMinWidth(sizeConstraints)
  }

  fun getMaxWidth(sizeConstraints: Long): Int {
    return Mode.forConstraints(sizeConstraints.high).decodeMaxWidth(sizeConstraints)
  }

  fun getMinHeight(sizeConstraints: Long): Int {
    return Mode.forConstraints(sizeConstraints.low).decodeMinHeight(sizeConstraints)
  }

  fun getMaxHeight(sizeConstraints: Long): Int {
    return Mode.forConstraints(sizeConstraints.low).decodeMaxHeight(sizeConstraints)
  }

  /** Actual encoding and decoding logic for each supported mode. */
  sealed class Mode(val supportedRange: IntRange, val id: Int) {

    abstract fun encode(minValue: Int, maxValue: Int): Long

    abstract fun decodeMinWidth(sizeConstraints: Long): Int

    abstract fun decodeMaxWidth(sizeConstraints: Long): Int

    abstract fun decodeMinHeight(sizeConstraints: Long): Int

    abstract fun decodeMaxHeight(sizeConstraints: Long): Int

    object Exact : Mode(IntRange(MaxValue30Bits, MaxValue30Bits), 0b00) {
      override fun decodeMinWidth(sizeConstraints: Long): Int = decodeWidth(sizeConstraints)

      override fun decodeMaxWidth(sizeConstraints: Long): Int = decodeWidth(sizeConstraints)

      override fun decodeMinHeight(sizeConstraints: Long): Int = decodeHeight(sizeConstraints)

      override fun decodeMaxHeight(sizeConstraints: Long): Int = decodeHeight(sizeConstraints)

      private fun decodeWidth(sizeConstraints: Long): Int =
          valueOrInfinity(sizeConstraints.high.clearModeBits)

      private fun decodeHeight(sizeConstraints: Long): Int =
          valueOrInfinity(sizeConstraints.low.clearModeBits)

      override fun encode(minValue: Int, maxValue: Int): Long = encode30BitValue(maxValue)
    }

    object UpperBounded : Mode(IntRange(0, MaxValue30Bits), 0b01) {
      override fun decodeMinWidth(sizeConstraints: Long): Int = 0

      override fun decodeMaxWidth(sizeConstraints: Long): Int =
          valueOrInfinity(sizeConstraints.high.clearModeBits)

      override fun decodeMinHeight(sizeConstraints: Long): Int = 0

      override fun decodeMaxHeight(sizeConstraints: Long): Int =
          valueOrInfinity(sizeConstraints.low.clearModeBits)

      override fun encode(minValue: Int, maxValue: Int): Long = encode30BitValue(maxValue)
    }

    object Range : Mode(IntRange(MaxValue13Bits, MaxValue18Bits), 0b10) {
      override fun decodeMinWidth(sizeConstraints: Long): Int = decodeMinValue(sizeConstraints.high)

      override fun decodeMaxWidth(sizeConstraints: Long): Int = decodeMaxValue(sizeConstraints.high)

      override fun decodeMinHeight(sizeConstraints: Long): Int = decodeMinValue(sizeConstraints.low)

      override fun decodeMaxHeight(sizeConstraints: Long): Int = decodeMaxValue(sizeConstraints.low)

      private fun decodeMinValue(encodedHalf: Int): Int =
          valueOrInfinity(encodedHalf.clearMsb ushr 18)

      private fun decodeMaxValue(encodedHalf: Int): Int =
          valueOrInfinity(encodedHalf.clearModeBits and Mask18Bits)

      override fun encode(minValue: Int, maxValue: Int): Long {
        val minValueToEncode: Int = if (minValue == Infinity) 0 else minValue + 1
        val maxValueToEncode: Int = if (maxValue == Infinity) 0 else maxValue + 1

        val encodedValue: Int =
            (id shl 30) or
                ((minValueToEncode and Mask13Bits) shl 18) or
                (maxValueToEncode and Mask18Bits)
        return encodedValue.toLong().clearHighBits
      }
    }

    internal fun valueOrInfinity(value: Int): Int {
      return if (value == 0) Infinity else value - 1
    }

    internal fun encode30BitValue(value: Int): Long {
      val valueToEncode: Int = if (value == Infinity) 0 else value + 1
      val encodedValue: Int = (id shl 30) or (valueToEncode and Mask30Bits)
      return encodedValue.toLong().clearHighBits
    }

    companion object {
      // mask where low 32 bits are set to 1 and high 32 bits are 0
      const val MaxIntMask: Long = 0xFFFFFFFF

      // mask where 30 bits are set to 1
      const val Mask30Bits: Int = 0x3FFFFFFF
      // max constraint value representable on 30 bits, we need to subtract 1 because one value is
      // reserved for Infinity
      const val MaxValue30Bits: Int = Mask30Bits - 1

      // mask where 18 bits are set to 1
      const val Mask18Bits: Int = 0x3FFFF
      // max constraint value representable on 18 bits, we need to subtract 1 because one value is
      // reserved for Infinity
      const val MaxValue18Bits: Int = Mask18Bits - 1

      // mask where 13 bits are set to 1
      const val Mask13Bits: Int = 0x1FFF
      // max constraint value representable on 13 bits, we need to subtract 1 because one value is
      // reserved for Infinity
      const val MaxValue13Bits: Int = Mask13Bits - 1

      @JvmStatic
      fun forConstraints(encodedHalf: Int): Mode {
        return when (encodedHalf.mode) {
          Exact.id -> Exact
          UpperBounded.id -> UpperBounded
          else -> Range
        }
      }

      @JvmStatic
      fun forRange(minValue: Int, maxValue: Int): Mode {
        return when (minValue) {
          maxValue -> Exact
          0 -> UpperBounded
          else -> Range
        }
      }

      @JvmStatic
      fun forMeasureSpec(measureSpec: Int): Mode {
        return when (View.MeasureSpec.getMode(measureSpec)) {
          View.MeasureSpec.EXACTLY -> Exact
          View.MeasureSpec.AT_MOST,
          View.MeasureSpec.UNSPECIFIED -> UpperBounded
          else -> throw IllegalStateException("Unknown width spec mode.")
        }
      }
    }
  }

  // Extensions
  /** Returns most significant half of the Long value. Bits 32..63 */
  private val Long.high: Int
    get() = this.ushr(32).toInt() and Mode.MaxIntMask.toInt()

  /** Returns least significant half of the Long value. Bits 0..31 */
  private val Long.low: Int
    get() = this.toInt() and Mode.MaxIntMask.toInt()

  /** Returns long value with most significant half bits set to 0 */
  private val Long.clearHighBits: Long
    get() = this and Mode.MaxIntMask

  /** Returns a value with 2 most significant bits set to 0 */
  private val Int.clearModeBits: Int
    get() = this and Mode.MaxIntMask.toInt().ushr(2)

  /** Returns a value with the most significant bit set to 0 */
  private val Int.clearMsb: Int
    get() = this and Mode.MaxIntMask.toInt().ushr(1)

  /** Returns a value of 2 most significant bits */
  private val Int.mode: Int
    get() = this.ushr(30)
}
