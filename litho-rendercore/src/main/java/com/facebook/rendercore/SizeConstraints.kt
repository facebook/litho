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

/** Creates [SizeConstraints] from the provided values. */
fun SizeConstraints(
    minWidth: Int = 0,
    maxWidth: Int = SizeConstraints.Infinity,
    minHeight: Int = 0,
    maxHeight: Int = SizeConstraints.Infinity,
): SizeConstraints {
  return SizeConstraints(
      SizeConstraints.Helper.sizeConstraints(minWidth, maxWidth, minHeight, maxHeight))
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
    return MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
  }
  if (maxWidth != SizeConstraints.Infinity) {
    return MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
  }
  return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
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
    return MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
  }
  if (maxHeight != SizeConstraints.Infinity) {
    return MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
  }
  return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
}

/** A maximum width value that can be stored in this [SizeConstraints] instance. */
val SizeConstraints.MaxPossibleWidthValue: Int
  get() = SizeConstraints.Helper.Mode.forRange(minWidth, maxWidth).supportedRange.last

/** A maximum height value that can be stored in this [SizeConstraints] instance. */
val SizeConstraints.MaxPossibleHeightValue: Int
  get() = SizeConstraints.Helper.Mode.forRange(minHeight, maxHeight).supportedRange.last

/** Returns true if the size fits within the given constraints, otherwise returns false. */
fun Size.fitsWithin(sizeConstraints: SizeConstraints): Boolean {
  return (width in sizeConstraints.minWidth..sizeConstraints.maxWidth) &&
      (height in sizeConstraints.minHeight..sizeConstraints.maxHeight)
}

/**
 * Returns true if the given [size] that was measured using [otherConstraints] is compatible with
 * current size constraints.
 *
 * Knowing that a Size that was measured with one SizeConstraints wouldn't change if it was measured
 * with other SizeConstraints may be used to avoid unnecessary measurements.
 */
fun SizeConstraints.areCompatible(otherConstraints: SizeConstraints, size: Size): Boolean {
  // exactly the same constraints, return early
  if (encodedValue == otherConstraints.encodedValue) {
    return true
  }

  // new width constraint is exact - check if measured width matches its size
  val isNewWidthExactAndMeasuredWidthFits = hasExactWidth && maxWidth == size.width

  // check if new new width constraints are not less strict than the old width constraints
  val doesNewWidthConstraintFitTheOldOne =
      otherConstraints.minWidth <= minWidth && otherConstraints.maxWidth >= maxWidth

  // check if measured width fits within the new width constraints
  val doesMeasuredWidthFitNewWidthConstraint = size.width in minWidth..maxWidth

  val isWidthCompatible =
      isNewWidthExactAndMeasuredWidthFits ||
          (doesNewWidthConstraintFitTheOldOne && doesMeasuredWidthFitNewWidthConstraint)

  // new height constraint is exact - check if measured height matches its size
  val isNewHeightExactAndMeasuredHeightFits = hasExactHeight && maxHeight == size.height

  // check if new new height constraints are not less strict than the old height constraints
  val doesNewHeightConstraintFitTheOldOne =
      otherConstraints.minHeight <= minHeight && otherConstraints.maxHeight >= maxHeight

  // check if measured height fits within the new height constraints
  val doesMeasuredHeightFitNewHeightConstraint = size.height in minHeight..maxHeight

  val isHeightCompatible =
      isNewHeightExactAndMeasuredHeightFits ||
          (doesNewHeightConstraintFitTheOldOne && doesMeasuredHeightFitNewHeightConstraint)

  return isWidthCompatible && isHeightCompatible
}

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
 * values. For mode details about the encoding see [Helper.Mode].
 */
@JvmInline
value class SizeConstraints internal constructor(val encodedValue: Long) {
  /** Minimum width in pixels. It'll be always <= maxWidth. */
  val minWidth: Int
    get() = Helper.getMinWidth(encodedValue)

  /** Maximum width in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxWidth: Int
    get() = Helper.getMaxWidth(encodedValue)

  /** Minimum height in pixels. It'll be always <= maxHeight. */
  val minHeight: Int
    get() = Helper.getMinHeight(encodedValue)

  /** Maximum height in pixels. It'll be always >= minWidth and <= Infinity. */
  val maxHeight: Int
    get() = Helper.getMaxHeight(encodedValue)

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
    /**
     * A value representing Infinity. It should be used to represent unbounded sizes. When
     * [maxWidth] is set to Infinity, then [hasBoundedWidth] returns false. Similarly when
     * [maxHeight] is set to Infinity, then [hasBoundedHeight] returns false.
     */
    const val Infinity: Int = Int.MAX_VALUE

    /**
     * Creates [SizeConstraints] with exact size in both dimension using the provided width and
     * height.
     */
    fun exact(width: Int, height: Int): SizeConstraints {
      return SizeConstraints(width, width, height, height)
    }

    /** Creates [SizeConstraints] with exact width and unbounded height. */
    fun fixedWidth(width: Int): SizeConstraints {
      return SizeConstraints(width, width, 0, Infinity)
    }

    /** Creates [SizeConstraints] with exact height and unbounded width. */
    fun fixedHeight(height: Int): SizeConstraints {
      return SizeConstraints(0, Infinity, height, height)
    }

    /** Creates encoded [Long] from the provided width and height [View.MeasureSpec]s. */
    @JvmStatic
    fun encodeMeasureSpecs(widthSpec: Int, heightSpec: Int): Long {
      val maxSupportedWidth = Helper.Mode.forMeasureSpec(widthSpec).supportedRange.last

      val widthMode = MeasureSpec.getMode(widthSpec)
      val widthSize = MeasureSpec.getSize(widthSpec).coerceIn(0, maxSupportedWidth)
      val minWidth: Int
      val maxWidth: Int
      when (widthMode) {
        MeasureSpec.EXACTLY -> {
          minWidth = widthSize
          maxWidth = widthSize
        }
        MeasureSpec.UNSPECIFIED -> {
          minWidth = 0
          maxWidth = Infinity
        }
        MeasureSpec.AT_MOST -> {
          minWidth = 0
          maxWidth = widthSize
        }
        else -> throw IllegalStateException("Unknown width spec mode.")
      }

      val maxSupportedHeight = Helper.Mode.forMeasureSpec(heightSpec).supportedRange.last

      val heightMode = MeasureSpec.getMode(heightSpec)
      val heightSize = MeasureSpec.getSize(heightSpec).coerceIn(0, maxSupportedHeight)
      val minHeight: Int
      val maxHeight: Int
      when (heightMode) {
        MeasureSpec.EXACTLY -> {
          minHeight = heightSize
          maxHeight = heightSize
        }
        MeasureSpec.UNSPECIFIED -> {
          minHeight = 0
          maxHeight = Infinity
        }
        MeasureSpec.AT_MOST -> {
          minHeight = 0
          maxHeight = heightSize
        }
        else -> throw IllegalStateException("Unknown height spec mode.")
      }

      return Helper.sizeConstraints(minWidth, maxWidth, minHeight, maxHeight)
    }

    /** Creates [SizeConstraints] from the provided width and height [View.MeasureSpec]s. */
    @JvmStatic
    fun fromMeasureSpecs(widthSpec: Int, heightSpec: Int): SizeConstraints {
      return SizeConstraints(encodeMeasureSpecs(widthSpec = widthSpec, heightSpec = heightSpec))
    }
  }

  object Helper {

    @JvmName("getWidthSpec")
    @JvmStatic
    fun getWidthSpec(sizeConstraints: SizeConstraints): Int {
      return sizeConstraints.toWidthSpec()
    }

    @JvmName("getHeightSpec")
    @JvmStatic
    fun getHeightSpec(sizeConstraints: SizeConstraints): Int {
      return sizeConstraints.toHeightSpec()
    }

    internal fun sizeConstraints(
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
          widthMode.supportedRange.first,
          widthMode.supportedRange.last,
          heightMode.supportedRange.first,
          heightMode.supportedRange.last,
      )

      val encodedWidth: Long = widthMode.encode(minWidth, maxWidth)
      val encodedHeight: Long = heightMode.encode(minHeight, maxHeight)

      return encodedWidth.shl(32) or encodedHeight
    }

    fun encode(encoded: Long): SizeConstraints {
      return SizeConstraints(encoded)
    }

    internal fun getMinWidth(sizeConstraints: Long): Int {
      return Mode.forConstraints(sizeConstraints.high).decodeMinWidth(sizeConstraints)
    }

    internal fun getMaxWidth(sizeConstraints: Long): Int {
      return Mode.forConstraints(sizeConstraints.high).decodeMaxWidth(sizeConstraints)
    }

    internal fun getMinHeight(sizeConstraints: Long): Int {
      return Mode.forConstraints(sizeConstraints.low).decodeMinHeight(sizeConstraints)
    }

    internal fun getMaxHeight(sizeConstraints: Long): Int {
      return Mode.forConstraints(sizeConstraints.low).decodeMaxHeight(sizeConstraints)
    }

    private fun validateSizes(
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
        maxSupportedMinWidth: Int,
        maxSupportedMaxWidth: Int,
        maxSupportedMinHeight: Int,
        maxSupportedMaxHeight: Int,
    ) {
      if (minWidth < 0) {
        throw IllegalArgumentException(
            "minWidth must be >= 0, but was: $minWidth. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (minHeight < 0) {
        throw IllegalArgumentException(
            "minHeight must be >= 0, but was: $minHeight. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (minWidth > maxSupportedMinWidth && minWidth != SizeConstraints.Infinity) {
        throw IllegalArgumentException(
            "minWidth must be <= ${maxSupportedMinWidth}, but was: $minWidth. Components this big may affect performance and lead to out of memory errors. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (maxWidth > maxSupportedMaxWidth && maxWidth != SizeConstraints.Infinity) {
        throw IllegalArgumentException(
            "maxWidth must be <= ${maxSupportedMaxWidth}, but was: $maxWidth. Components this big may affect performance and lead to out of memory errors. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (minHeight > maxSupportedMinHeight && minHeight != SizeConstraints.Infinity) {
        throw IllegalArgumentException(
            "minHeight must be <= ${maxSupportedMinHeight}, but was: $minHeight. Components this big may affect performance and lead to out of memory errors. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (maxHeight > maxSupportedMaxHeight && maxHeight != SizeConstraints.Infinity) {
        throw IllegalArgumentException(
            "maxHeight must be <= ${maxSupportedMaxHeight}, but was: $maxHeight. Components this big may affect performance and lead to out of memory errors. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (minWidth > maxWidth) {
        throw IllegalArgumentException(
            "maxWidth must be >= minWidth, but was: maxWidth=$maxWidth; minWidth=$minWidth. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
      if (minHeight > maxHeight) {
        throw IllegalArgumentException(
            "maxHeight must be >= minHeight, but was: maxHeight=$maxHeight; minHeight=$minHeight. minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight")
      }
    }

    /**
     * Actual encoding and decoding logic for each supported mode.
     *
     * SizeConstraints encodes minWidth, maxWidth, minHeight and maxHeight into a single 64bit long
     * value. The 64 bits are divided into two halves:
     * ```
     * <------------------------64 bit value------------------------>
     * <------32 bit width info------><-----32 bit height info------>
     * ```
     *
     * The first 2 bits in each 32 bit half represent MODE:
     * - 00 - EXACT - min and max values are the same
     * - 01 - UPPER_BOUNDED - min value is 0, max value is >=0 or Infinity
     * - 10 and 11 - RANGE - min and max values are > 0
     *
     * ```
     * <-------------32 bit value------------->
     * <-2 bit mode-><------30 bit size ------>
     * ```
     *
     * In EXACT and UPPER_BOUNDED modes, the actual size value is stored on 30 bits which means that
     * the maximum supported size value in EXACT and UPPER_BOUNDED modes is 0x3FFFFFFF - 1 (all 30
     * bits set to 1 minus 1).
     *
     * In RANGE mode, the min value is stored on highest 13 bits and max value is stored on lowest
     * 18 bits which means that the maximum supported min size value is 0x1FFF - 1 (all 13 bits set
     * to 1 minus 1) and maximum supported max size value is 0x3FFFF - 1 (all 18 bits set to 1 minus
     * 1).
     *
     * There is also one special value - [SizeConstraints.Infinity] which is set to [Int.MAX_VALUE].
     * Infinity is encoded as 0.
     */
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
        override fun decodeMinWidth(sizeConstraints: Long): Int =
            decodeMinValue(sizeConstraints.high)

        override fun decodeMaxWidth(sizeConstraints: Long): Int =
            decodeMaxValue(sizeConstraints.high)

        override fun decodeMinHeight(sizeConstraints: Long): Int =
            decodeMinValue(sizeConstraints.low)

        override fun decodeMaxHeight(sizeConstraints: Long): Int =
            decodeMaxValue(sizeConstraints.low)

        private fun decodeMinValue(encodedHalf: Int): Int =
            valueOrInfinity(encodedHalf.clearMsb ushr 18)

        private fun decodeMaxValue(encodedHalf: Int): Int =
            valueOrInfinity(encodedHalf.clearModeBits and Mask18Bits)

        override fun encode(minValue: Int, maxValue: Int): Long {
          val minValueToEncode: Int = if (minValue == SizeConstraints.Infinity) 0 else minValue + 1
          val maxValueToEncode: Int = if (maxValue == SizeConstraints.Infinity) 0 else maxValue + 1

          val encodedValue: Int =
              (id shl 30) or
                  ((minValueToEncode and Mask13Bits) shl 18) or
                  (maxValueToEncode and Mask18Bits)
          return encodedValue.toLong().clearHighBits
        }
      }

      internal fun valueOrInfinity(value: Int): Int {
        return if (value == 0) SizeConstraints.Infinity else value - 1
      }

      internal fun encode30BitValue(value: Int): Long {
        val valueToEncode: Int = if (value == SizeConstraints.Infinity) 0 else value + 1
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
          return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> Exact
            MeasureSpec.AT_MOST,
            MeasureSpec.UNSPECIFIED -> UpperBounded
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
}
