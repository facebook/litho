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

/**
 * Represents the stiffness of a spring animation.
 *
 * Stiffness controls how quickly the spring responds to displacement. Higher stiffness means a
 * stronger restoring force, resulting in faster animations with higher frequency oscillation.
 *
 * Use the predefined constants for common values, or create custom stiffness with any positive
 * float:
 * ```kotlin
 * spring<Float>(stiffness = Stiffness.Medium)
 * spring<Float>(stiffness = Stiffness(500f))
 * ```
 *
 * @param value The stiffness value. Must be positive.
 */
@JvmInline
value class Stiffness(val value: Float) {
  init {
    require(value > 0) { "Stiffness must be a positive number, but was $value" }
  }

  companion object {
    /** Very high stiffness (10000). Very fast, stiff spring. */
    val VeryHigh = Stiffness(10_000f)

    /** High stiffness (1500). Fast, snappy spring. */
    val High = Stiffness(1500f)

    /** Medium stiffness (200). Balanced spring. */
    val Medium = Stiffness(200f)

    /** Medium-low stiffness (120). Moderately loose spring. */
    val MediumLow = Stiffness(120f)

    /** Low stiffness (50). Loose, slow spring. */
    val Low = Stiffness(50f)

    /** Very low stiffness (10). Very loose, very slow spring. */
    val VeryLow = Stiffness(10f)
  }
}

/**
 * Represents the damping ratio of a spring animation.
 *
 * The damping ratio describes how the spring's oscillation decays over time:
 * - **Under-damped** (< 1.0): The spring overshoots and oscillates before settling. Lower values
 *   produce more bounce.
 * - **Critically damped** (= 1.0): The spring returns to rest in the shortest time without
 *   overshooting.
 * - **Over-damped** (> 1.0): The spring returns to rest without overshooting, but slower than
 *   critically damped.
 * - **Undamped** (= 0.0): The spring oscillates forever without settling.
 *
 * ```kotlin
 * spring<Float>(dampingRatio = DampingRatio.MediumBouncy)
 * spring<Float>(dampingRatio = DampingRatio(0.6f))
 * ```
 *
 * @param value The damping ratio value. Must be non-negative.
 */
@JvmInline
value class DampingRatio(val value: Float) {
  init {
    require(value >= 0) { "Damping ratio must be non-negative, but was $value" }
  }

  companion object {
    /** High bounce (0.2). Significant overshoot and oscillation. */
    val HighBouncy = DampingRatio(0.2f)

    /** Medium bounce (0.5). Moderate overshoot. */
    val MediumBouncy = DampingRatio(0.5f)

    /** Low bounce (0.75). Slight overshoot, settles quickly. */
    val LowBouncy = DampingRatio(0.75f)

    /** No bounce (1.0). Critically damped — fastest return without overshoot. */
    val NoBouncy = DampingRatio(1.0f)
  }
}
