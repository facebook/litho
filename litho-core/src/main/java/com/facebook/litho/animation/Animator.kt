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
 * A stateless, time-driven computation engine that produces animation values.
 *
 * An [Animator] is created by an [AnimationSpec] with specific initial conditions (initial value,
 * target value, initial velocity) baked in. Once created, it can be queried at any point in time to
 * produce the current value, velocity, and completion status.
 *
 * ## Stateless Time-Driven Model
 *
 * The animator is a pure function of elapsed time — calling [getValue] with the same
 * `playTimeNanos` always produces the same result. This enables:
 * - **Deterministic replay**: Same inputs produce same outputs regardless of frame timing.
 * - **Testability**: Tests can evaluate at arbitrary time points without running a real clock.
 * - **Interruption**: Current value and velocity can be captured at any moment and used as initial
 *   conditions for a new animator.
 *
 * ## Usage
 *
 * Animators are not created directly — they are produced by [AnimationSpec.createAnimator]:
 * ```kotlin
 * val spec = spring<Float>()
 * val animator = spec.createAnimator(
 *     initialValue = 0f,
 *     targetValue = 1f,
 *     typeConverter = FloatTypeConverter,
 * )
 *
 * // Query at specific times
 * val valueAt100ms = animator.getValue(100_000_000L)  // 100ms in nanos
 * val velocity = FloatTypeConverter.newVector()
 * animator.getVelocity(100_000_000L, velocity)
 * val done = animator.isFinished(100_000_000L)
 * ```
 *
 * @param T The type of value this animator produces.
 * @see AnimationSpec
 */
interface Animator<out T> {

  /**
   * Returns the animated value at the given elapsed time.
   *
   * @param playTimeNanos Elapsed time since the animation started, in nanoseconds.
   * @return The animated value at the specified time.
   */
  fun getValue(playTimeNanos: Long): T

  /**
   * Writes the current per-component velocity at the given elapsed time into [velocity], in
   * float-space units per second.
   *
   * Velocity is expressed per component so interruption preserves each component's motion
   * independently — a size retargeted mid-flight carries its width and height velocities separately
   * rather than a single collapsed scalar. [velocity] is a caller-owned [AnimationVector] shaped by
   * the animation's [TypeConverter.newVector]; the animator writes every component.
   *
   * @param playTimeNanos Elapsed time since the animation started, in nanoseconds.
   * @param velocity Output vector receiving one float-space velocity per component.
   */
  fun getVelocity(playTimeNanos: Long, velocity: AnimationVector)

  /**
   * Returns whether the animation has reached its final state at the given elapsed time.
   *
   * For tween animations, this is true when elapsed time exceeds the duration. For spring
   * animations, this is true when displacement and velocity are both below the convergence
   * threshold.
   *
   * @param playTimeNanos Elapsed time since the animation started, in nanoseconds.
   * @return `true` if the animation is complete.
   */
  fun isFinished(playTimeNanos: Long): Boolean
}
