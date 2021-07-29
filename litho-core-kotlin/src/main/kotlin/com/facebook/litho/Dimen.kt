/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat
import java.lang.IllegalArgumentException
import java.lang.Long.toHexString

// 0x7FF8000000000000L is the mask used to indicate a quiet NaN, while the or'd flags below indicate
// whether the value in the payload is a px or sp value. See Dimen.toPixels() for more info.
const val PX_FLAG = 0x7FF8_0000_0000_0000L or 0x0001_0000_0000_0000L
const val SP_FLAG = 0x7FF8_0000_0000_0000L or 0x0002_0000_0000_0000L
const val PAYLOAD_MASK = 0xFFFF_FFFFL

/**
 * A class which represents dimension values in different units: Px, Dp, and Sp. All can be
 * converted to pixels with a [ResourceResolver] which wraps a [Resources].
 *
 * To create a Dimen, use the [px], [dp], and [sp] extension functions below.
 */
inline class Dimen(val encodedValue: Double) {

  /**
   * Converts the given Dimen to pixels.
   *
   * Note on implementation: in order to not have to materialize an actual object for non-nullable
   * usages of Dimen, we use an inline class (Dimen) that contains a Double. This double encodes
   * both the type of the Dimen (PX, DP, SP) and its value. We can do this because all px/dp/sp
   * values are 32-bits, while doubles are 64-bit. The encoding is as follows:
   * - DP: The double is not NaN, and the double value can just be cast to a float
   * - PX: The double is a quiet NaN with PX_FLAG set in the mantissa, and the least significant
   * 32-bits (the payload) are the signed int value
   * - SP: The double is a quiet NaN with SP_FLAG set in the mantissa, and the least significant
   * 32-bits (the payload) are the signed float value
   *
   * Read more about quiet NaN's here: https://www.doc.ic.ac.uk/~eedwards/compsys/float/nan.html
   */
  fun toPixels(resourceResolver: ResourceResolver): Int {
    if (encodedValue.isNaN()) {
      val longBits = doubleToRawLongBits(encodedValue)

      // We expect one of the PX or SP flags to be set, indicating that the payload encodes a
      // integer PX value or a float SP value, respectively
      if (longBits and PX_FLAG == PX_FLAG) {
        return (longBits and PAYLOAD_MASK).toInt()
      } else if (longBits and SP_FLAG == SP_FLAG) {
        val spValue = intBitsToFloat((longBits and PAYLOAD_MASK).toInt())
        return resourceResolver.sipsToPixels(spValue)
      } else {
        throw IllegalArgumentException(
            "Got unexpected NaN: ${toHexString(encodedValue.toRawBits())}")
      }
    }

    // DP case - DP is "encoded" as a regular, not-NaN double
    return resourceResolver.dipsToPixels(encodedValue.toFloat())
  }
}

inline val Int.dp: Dimen
  get() = Dimen(this.toDouble())

inline val Float.dp: Dimen
  get() = Dimen(this.toDouble())

inline val Double.dp: Dimen
  get() = Dimen(this)

inline val Int.sp: Dimen
  get() = Dimen(encodeSpFloat(this.toFloat()))

inline val Float.sp: Dimen
  get() = Dimen(encodeSpFloat(this))

inline val Double.sp: Dimen
  get() = Dimen(encodeSpFloat(this.toFloat()))

inline val Int.px: Dimen
  get() = Dimen(encodePxInt(this))

inline val Float.px: Dimen
  get() = Dimen(encodePxInt(this.toInt()))

inline val Double.px: Dimen
  get() = Dimen(encodePxInt(this.toInt()))

@PublishedApi
internal inline fun encodePxInt(value: Int): Double {
  return longBitsToDouble(value.toLong() or PX_FLAG)
}

@PublishedApi
internal inline fun encodeSpFloat(value: Float): Double {
  return longBitsToDouble(value.toRawBits().toLong() or SP_FLAG)
}
