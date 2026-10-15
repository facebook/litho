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

package com.facebook.litho.animation

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

/**
 * A one-time shift of a repeating animation's start time, used to stagger or delay loops (see
 * [repeatable] / [infiniteRepeatable]).
 *
 * Unlike a tween's `delay` — which is baked into the spec and so repeats every cycle — a
 * [StartOffset] applies only once, to the first cycle:
 * - [StartOffsetType.Delay] holds the animation at its start value for [offsetMillis] before the
 *   first cycle begins.
 * - [StartOffsetType.FastForward] starts the animation immediately, already [offsetMillis] into its
 *   cycle. Because the cycle is periodic, this is a persistent phase shift across all cycles — the
 *   natural way to stagger several otherwise-identical loops.
 */
@JvmInline
value class StartOffset private constructor(internal val encodedMillis: Int) {

  /**
   * @param offsetMillis The offset magnitude in milliseconds.
   * @param offsetType Whether to delay the first cycle or fast-forward into it.
   */
  constructor(
      offsetMillis: Int,
      offsetType: StartOffsetType = StartOffsetType.Delay,
  ) : this(encodeMillis(offsetMillis, offsetType))

  /**
   * @param offset The offset magnitude.
   * @param offsetType Whether to delay the first cycle or fast-forward into it.
   */
  constructor(
      offset: Duration,
      offsetType: StartOffsetType = StartOffsetType.Delay,
  ) : this(durationMillis(offset), offsetType)

  /** The offset magnitude in milliseconds. */
  val offsetMillis: Int
    get() = abs(encodedMillis)

  /** Whether the offset delays the first cycle or fast-forwards into it. */
  val offsetType: StartOffsetType
    get() = if (encodedMillis > 0) StartOffsetType.FastForward else StartOffsetType.Delay

  /**
   * The offset as a signed nanosecond shift applied to play time: positive fast-forwards (starts
   * ahead in the cycle), negative delays (holds at the start value until it elapses).
   */
  internal val signedNanos: Long
    get() = encodedMillis.toLong() * 1_000_000L

  companion object {

    private fun encodeMillis(
        offsetMillis: Int,
        offsetType: StartOffsetType,
    ): Int {
      require(offsetMillis >= 0) { "StartOffset offsetMillis must be non-negative" }
      return offsetMillis * offsetType.sign
    }

    private fun durationMillis(offset: Duration): Int {
      require(offset >= ZERO) { "StartOffset offset must be non-negative" }
      val millis = offset.inWholeMilliseconds
      require(millis <= Int.MAX_VALUE) {
        "StartOffset offset must fit in Int milliseconds, but was $millis"
      }
      return millis.toInt()
    }
  }
}

/** How a [StartOffset] is applied relative to a repeating animation's first cycle. */
@JvmInline
value class StartOffsetType private constructor(internal val sign: Int) {
  companion object {
    /** Hold at the start value for the offset, then begin the first cycle. */
    val Delay: StartOffsetType = StartOffsetType(-1)

    /** Begin immediately, already the offset into the first cycle (a persistent phase shift). */
    val FastForward: StartOffsetType = StartOffsetType(1)
  }
}
