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

import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import androidx.annotation.FloatRange

/**
 * Defines an easing curve that controls the rate of change of an animation over time.
 *
 * An easing function maps normalized time (0 to 1) to an output fraction that determines how far
 * along the animation has progressed. Different easing curves create different motion
 * characteristics — linear, accelerating, decelerating, or bouncing.
 *
 * ## Built-in Presets
 *
 * ```kotlin
 * Easing.Linear        // constant rate
 * Easing.EaseIn        // starts slow, accelerates
 * Easing.EaseOut       // starts fast, decelerates
 * Easing.EaseInOut     // slow start and end, fast middle
 * Easing.FastOutSlowIn // Material Design standard curve
 * ```
 *
 * ## Custom Easing
 *
 * ```kotlin
 * // Cubic bezier curve
 * val custom = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
 *
 * // Arbitrary easing function
 * val bounce = Easing { fraction -> /* custom math */ }
 * ```
 *
 * @see CubicBezierEasing
 * @see AnimationSpec
 */
fun interface Easing {

  /**
   * Transforms a normalized time fraction into an eased output fraction.
   *
   * @param fraction Input fraction in [0, 1] representing normalized elapsed time.
   * @return Output fraction representing the eased progress. Typically in [0, 1], but may exceed
   *   these bounds for effects like overshoot.
   */
  fun transform(@FloatRange(from = 0.0, to = 1.0) fraction: Float): Float

  companion object {

    /** Constant rate of change — no acceleration or deceleration. */
    val Linear: Easing = Easing { it }

    /** Starts slowly and accelerates toward the end. */
    val EaseIn: Easing = CubicBezierEasing(0.42f, 0.0f, 1.0f, 1.0f)

    /** Starts quickly and decelerates toward the end. */
    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.58f, 1.0f)

    /** Starts slowly, speeds up, then decelerates toward the end. */
    val EaseInOut: Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    /**
     * Material Design standard easing curve.
     *
     * Starts with a fast acceleration and then gradually decelerates. Best for elements that move
     * between resting states.
     */
    val FastOutSlowIn: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Fast acceleration with linear end. Best for elements leaving the screen. */
    val FastOutLinearIn: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Linear start with slow deceleration. Best for elements entering the screen. */
    val LinearOutSlowIn: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
  }
}

/**
 * An [Easing] defined by a cubic bezier curve.
 *
 * The control points (x1, y1) and (x2, y2) define the shape of the curve, where x represents time
 * and y represents progress. The start point (0, 0) and end point (1, 1) are implicit.
 *
 * Delegates to Android's [PathInterpolator] for efficient native evaluation.
 *
 * ```kotlin
 * // Material Design emphasized easing
 * val emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
 * ```
 *
 * @param x1 X coordinate of the first control point. Must be in [0, 1].
 * @param y1 Y coordinate of the first control point.
 * @param x2 X coordinate of the second control point. Must be in [0, 1].
 * @param y2 Y coordinate of the second control point.
 */
class CubicBezierEasing(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float,
) : Easing {

  internal val interpolator: PathInterpolator = PathInterpolator(x1, y1, x2, y2)

  override fun transform(fraction: Float): Float = interpolator.getInterpolation(fraction)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CubicBezierEasing) return false
    return x1 == other.x1 && y1 == other.y1 && x2 == other.x2 && y2 == other.y2
  }

  override fun hashCode(): Int {
    var result = x1.hashCode()
    result = 31 * result + y1.hashCode()
    result = 31 * result + x2.hashCode()
    result = 31 * result + y2.hashCode()
    return result
  }

  override fun toString(): String = "CubicBezierEasing($x1, $y1, $x2, $y2)"
}

/**
 * Wraps an Android [Interpolator] as an [Easing].
 *
 * Useful for reusing existing Android interpolators (e.g., `OvershootInterpolator`,
 * `BounceInterpolator`) with the animation spec API.
 *
 * ```kotlin
 * val bounceEasing = BounceInterpolator().asEasing()
 * val overshootEasing = OvershootInterpolator(2f).asEasing()
 * ```
 */
fun Interpolator.asEasing(): Easing = InterpolatorEasing(this)

/**
 * Converts this [Easing] to an Android [Interpolator].
 *
 * If this easing is already backed by an interpolator, the underlying interpolator is returned
 * directly without wrapping.
 */
fun Easing.asInterpolator(): Interpolator =
    when (this) {
      is InterpolatorEasing -> interpolator
      is CubicBezierEasing -> interpolator
      else -> Interpolator(this::transform)
    }

private class InterpolatorEasing(val interpolator: Interpolator) : Easing {
  override fun transform(fraction: Float): Float = interpolator.getInterpolation(fraction)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is InterpolatorEasing) return false
    // Android Interpolator implementations typically don't override equals/hashCode, so we compare
    // by class identity: interpolators of the same class are expected to be stateless and pure
    // and hence should produce the same curve.
    return interpolator.javaClass == other.interpolator.javaClass
  }

  override fun hashCode(): Int = interpolator.javaClass.hashCode()

  override fun toString(): String = "InterpolatorEasing($interpolator)"
}
