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

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange

/**
 * Represents timing functions (easing curves) for animations.
 *
 * Timing functions define the rate of change of an animation over time, creating different visual
 * effects such as linear motion, acceleration, deceleration, or bounce effects.
 */
fun interface Timing {
  fun transform(@FloatRange(from = 0.0, to = 1.0) fraction: Float): Float

  companion object {
    /** Constant rate of change throughout the animation. */
    fun linear(): Timing = InterpolatorTiming(0f, LinearInterpolator())

    /**
     * Starts slowly and accelerates towards the end.
     *
     * @param factor Degree to which the animation should be eased. Setting factor to 1.0f produces
     *   an y=x^2 parabola. Increasing factor above 1.0f exaggerates the ease-in effect (i.e., it
     *   starts even slower and ends even faster).
     */
    fun easeIn(factor: Float = 1.0f): Timing =
        InterpolatorTiming(factor, AccelerateInterpolator(factor))

    /**
     * Starts quickly and decelerates towards the end.
     *
     * @param factor Degree to which the animation should be eased. Setting factor to 1.0f produces
     *   an y=x^2 parabola. Increasing factor above 1.0f exaggerates the ease-out effect (i.e., it
     *   starts even faster and ends even slower).
     */
    fun easeOut(factor: Float = 1.0f): Timing =
        InterpolatorTiming(factor, DecelerateInterpolator(factor))

    /** Starts slowly, speeds up, then slows down towards the end. */
    fun easeInOut(): Timing = InterpolatorTiming(0f, AccelerateDecelerateInterpolator())

    internal fun fromInterpolator(interpolator: Interpolator): Timing =
        InterpolatorTiming(0f, interpolator)
  }
}

internal class InterpolatorTiming(
    private val factor: Float,
    val interpolator: Interpolator,
) : Timing, Interpolator {

  override fun transform(fraction: Float): Float {
    return interpolator.getInterpolation(fraction)
  }

  override fun getInterpolation(input: Float): Float {
    return interpolator.getInterpolation(input)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is InterpolatorTiming) return false
    return factor == other.factor && interpolator.javaClass == other.interpolator.javaClass
  }

  override fun hashCode(): Int {
    var result = factor.hashCode()
    result = 31 * result + interpolator::class.hashCode()
    return result
  }
}

/**
 * Creates an [Interpolator] from this [Timing].
 *
 * If the [Timing] is already an [InterpolatorTiming], the underlying interpolator is returned
 * directly. Otherwise, a new interpolator is created that delegates to this [Timing].
 *
 * Example usage:
 * ```
 * val interpolator = Timing.easeInOut().toInterpolator()
 * ```
 */
fun Timing.toInterpolator(): Interpolator =
    when (this) {
      is InterpolatorTiming -> this
      else -> TimingInterpolator(this)
    }

private class TimingInterpolator(private val timing: Timing) : Interpolator {
  override fun getInterpolation(input: Float): Float {
    return timing.transform(input)
  }
}
