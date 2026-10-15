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

package com.facebook.rendercore.utils

import kotlin.math.max
import kotlin.math.pow

/** pack two Float values into one Long value */
fun pack2Floats(x: Float, y: Float): Long {
  val v1 = x.toBits().toLong()
  val v2 = y.toBits().toLong()
  val packed = v1.shl(32) or (v2 and 0xFFFFFFFF)
  return packed
}

/** Unpacks the first Float value in [packFloats] from its returned Long. */
fun unpackFloat1(value: Long): Float {
  return Float.fromBits((value shr 32).toInt())
}

/** Unpacks the second Float value in [packFloats] from its returned Long. */
fun unpackFloat2(value: Long): Float {
  return Float.fromBits((value and 0xFFFFFFFF).toInt())
}

/** Unpacks the first absolute Float value in [packFloats] from its returned Long. */
fun unpackAbsFloat1(value: Long): Float {
  return Float.fromBits(((value shr 32) and 0x7FFFFFFF).toInt())
}

/** Unpacks the second absolute Float value in [packFloats] from its returned Long. */
fun unpackAbsFloat2(value: Long): Float {
  return Float.fromBits((value and 0x7FFFFFFF).toInt())
}

/** Linearly interpolate between [start] and [stop] with [fraction] fraction between them. */
fun lerp(start: Float, stop: Float, fraction: Float): Float {
  return (1 - fraction) * start + fraction * stop
}

/**
 * Converts this Float to a string representation with a fixed number of decimal places.
 *
 * This function formats the Float value to display exactly the specified number of decimal digits,
 * using standard rounding rules (rounds up when the decimal is >= 0.5).
 *
 * **Special cases:**
 * - Returns `"NaN"` if this value is [Float.NaN]
 * - Returns `"Infinity"` or `"-Infinity"` if this value is positive or negative infinity
 * - If [digits] is negative, it is clamped to 0 (no decimal places)
 *
 * **Rounding behavior:**
 * - Uses "round half up" strategy (0.5 rounds up to 1, not down)
 * - This differs from [kotlin.math.round] which uses "round half to even"
 *
 * Example usage:
 * ```
 * 3.14159f.toStringAsFixed(2)  // Returns "3.14"
 * 3.145f.toStringAsFixed(2)    // Returns "3.15" (rounds up)
 * 3.144f.toStringAsFixed(2)    // Returns "3.14" (rounds down)
 * 42.0f.toStringAsFixed(0)     // Returns "42"
 * 42.5f.toStringAsFixed(0)     // Returns "43" (rounds up)
 * Float.NaN.toStringAsFixed(2) // Returns "NaN"
 * ```
 *
 * @param digits The number of decimal places to include in the output. Negative values are treated
 *   as 0.
 * @return A string representation of this Float with the specified number of decimal places.
 */
internal fun Float.toStringAsFixed(digits: Int): String {
  if (isNaN()) return "NaN"
  if (isInfinite()) return if (this < 0f) "-Infinity" else "Infinity"

  val clampedDigits: Int = max(digits, 0) // Accept positive numbers and 0 only
  val pow = 10f.pow(clampedDigits)
  val shifted = this * pow // shift the given value by the corresponding power of 10
  val decimal = shifted - shifted.toInt() // obtain the decimal of the shifted value
  // Manually round up if the decimal value is greater than or equal to 0.5f.
  // because kotlin.math.round(0.5f) rounds down
  val roundedShifted =
      if (decimal >= 0.5f) {
        shifted.toInt() + 1
      } else {
        shifted.toInt()
      }

  val rounded = roundedShifted / pow // divide off the corresponding power of 10 to shift back
  return if (clampedDigits > 0) {
    // If we have any decimal points, convert the float to a string
    rounded.toString()
  } else {
    // If we do not have any decimal points, return the int
    // based string representation
    rounded.toInt().toString()
  }
}

/**
 * A constant representing two unspecified (NaN) Float values packed into a single Long.
 *
 * This value is used as a sentinel to indicate that both packed Float values are unspecified or
 * invalid. It consists of two IEEE 754 NaN (Not a Number) bit patterns packed together:
 * - Upper 32 bits: `0x7fc00000` (quiet NaN)
 * - Lower 32 bits: `0x7fc00000` (quiet NaN)
 *
 * Use this constant to initialize packed Float pairs that have no meaningful value yet, or to check
 * if a packed value represents an unspecified state.
 *
 * Example usage:
 * ```
 * var packedSize: Long = UnspecifiedPackedFloats
 *
 * // Check if unspecified
 * if (packedSize == UnspecifiedPackedFloats) {
 *     // Handle unspecified case
 * }
 *
 * // Set actual values
 * packedSize = pack2Floats(width, height)
 * ```
 *
 * @see pack2Floats
 * @see unpackFloat1
 * @see unpackFloat2
 */
@PublishedApi internal const val UnspecifiedPackedFloats: Long = 0x7fc00000_7fc00000L
