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

import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat
import java.lang.IllegalArgumentException
import java.lang.Long.toHexString

/**
 * A class which represents dimension values in different units: Px, Dp, and Sp. All can be
 * converted to pixels with a [ResourceResolver] which wraps a [Resources].
 *
 * To create a Dimen:
 * - For a literal value, use the [px], [dp], and [sp] extension functions, e.g. `10.dp`.
 * - For a value resolved from a resource, use [dimenRes], e.g. `dimenRes(R.dimen.my_dimen)`
 */
@JvmInline
value class Dimen(val encodedValue: Long) {

  /** Converts the given Dimen to pixels. */
  fun toPixels(resourceResolver: ResourceResolver): Int =
      convertEncodedValueToPixels(resourceResolver, encodedValue)
}

/** Creates a Dimen with a constant dp (density-independent pixels) value. */
inline val Int.dp: Dimen
  get() = Dimen(doubleToRawLongBits(this.toDouble()))

/** Creates a Dimen with a constant dp (density-independent pixels) value. */
inline val Float.dp: Dimen
  get() = Dimen(doubleToRawLongBits(this.toDouble()))

/**
 * Creates a Dimen with a constant dp (density-independent pixels) value.
 *
 * The value will be narrowed to a float automatically.
 */
inline val Double.dp: Dimen
  get() = Dimen(doubleToRawLongBits(this))

/** Creates a Dimen with a constant sp (scale-independent pixels) value. */
inline val Int.sp: Dimen
  get() = Dimen(encodeSpFloat(this.toFloat()))

/** Creates a Dimen with a constant sp (scale-independent pixels) value. */
inline val Float.sp: Dimen
  get() = Dimen(encodeSpFloat(this))

/**
 * Creates a Dimen with a constant sp (scale-independent pixels) value.
 *
 * The value will be narrowed to a float automatically.
 */
inline val Double.sp: Dimen
  get() = Dimen(encodeSpFloat(this.toFloat()))

/** Creates a Dimen with a constant pixel value. */
inline val Int.px: Dimen
  get() = Dimen(encodePxInt(this))

/**
 * Creates a Dimen with a constant pixel value.
 *
 * The value will automatically be converted to an int first.
 */
inline val Float.px: Dimen
  get() = Dimen(encodePxInt(this.toInt()))

/**
 * Creates a Dimen with a constant pixel value.
 *
 * The value will automatically be converted to an int first.
 */
inline val Double.px: Dimen
  get() = Dimen(encodePxInt(this.toInt()))

//
// Below this point are implementation details for packing dimension values + type into a long for
// performance (at the time of rolling this API out, this actually moved the performance needle).
//
// They aren't relevant to usage of the APIs above thus why they are tucked away at the bottom of
// the file.
//

// 0x7FF8000000000000L is the mask used to indicate a quiet NaN, while the or'd flags below indicate
// whether the value in the payload is a px or sp value. See Dimen.toPixels() for more info.
const val NAN_MASK = 0x7FF8_0000_0000_0000L
const val PX_FLAG = NAN_MASK or 0x0001_0000_0000_0000L
const val SP_FLAG = NAN_MASK or 0x0002_0000_0000_0000L
const val PAYLOAD_MASK = 0xFFFF_FFFFL

/**
 * Note on implementation: in order to not have to materialize an actual object for non-nullable
 * usages of Dimen, we use an inline class (Dimen) that contains a Double represented as a Long
 * (i.e. the bits of the double are the bits of the long). We don't directly use a Double since the
 * JRE will sometimes canonicalize NaNs, losing the information we store in the mantissa.
 *
 * This Double encodes both the type of the Dimen (PX, DP, SP) and its value. We can do this because
 * all px/dp/sp values are 32-bits, while doubles are 64-bit. The encoding is as follows:
 * - DP: The double is not NaN, and the double value can just be cast to a float
 * - PX: The double is a quiet NaN with PX_FLAG set in the mantissa, and the least significant
 *   32-bits (the payload) are the signed int value
 * - SP: The double is a quiet NaN with SP_FLAG set in the mantissa, and the least significant
 *   32-bits (the payload) are the signed float value
 *
 * Read more about quiet NaN's here: https://www.doc.ic.ac.uk/~eedwards/compsys/float/nan.html
 */
private inline fun convertEncodedValueToPixels(
    resourceResolver: ResourceResolver,
    encodedValue: Long
): Int =
    when {
      // DP case
      encodedValue and NAN_MASK != NAN_MASK ->
          resourceResolver.dipsToPixels(longBitsToDouble(encodedValue).toFloat())
      // PX case
      encodedValue and PX_FLAG == PX_FLAG -> (encodedValue and PAYLOAD_MASK).toInt()
      // SP case
      encodedValue and SP_FLAG == SP_FLAG ->
          resourceResolver.sipsToPixels(
              intBitsToFloat((encodedValue and PAYLOAD_MASK).toInt()),
          )
      else -> throw IllegalArgumentException("Got unexpected NaN: ${toHexString(encodedValue)}")
    }

@PublishedApi
internal inline fun encodePxInt(value: Int): Long {
  return value.toLong() or PX_FLAG
}

@PublishedApi
internal inline fun encodeSpFloat(value: Float): Long {
  return value.toRawBits().toLong() or SP_FLAG
}
