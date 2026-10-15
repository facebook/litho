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
 * Creates a [SpringSpec] for physics-based spring animations.
 *
 * ```kotlin
 * // Default spring (medium stiffness, no bounce)
 * spring<Float>()
 *
 * // Bouncy spring
 * spring<Float>(stiffness = Stiffness.Low, dampingRatio = DampingRatio.HighBouncy)
 * ```
 *
 * @param stiffness Controls spring responsiveness. Higher = faster, snappier.
 * @param dampingRatio Controls bounce. Lower = more overshoot. 1.0 = no overshoot.
 */
fun <T> spring(
    stiffness: Stiffness = Stiffness.Medium,
    dampingRatio: DampingRatio = DampingRatio.NoBouncy,
): FiniteAnimationSpec<T> = SpringSpec(stiffness, dampingRatio)

/**
 * A spring-based [AnimationSpec] that produces natural, physics-driven motion.
 *
 * Each component of the animated value is driven by its own [SpringSolver], so a multi-component
 * value such as a size or color animates and settles per component — no single scalar projection
 * can make two genuinely different values look equal and teleport.
 *
 * @see spring
 * @see Stiffness
 * @see DampingRatio
 */
private data class SpringSpec<T>(
    val stiffness: Stiffness = Stiffness.Medium,
    val dampingRatio: DampingRatio = DampingRatio.NoBouncy,
) : FiniteAnimationSpec<T> {

  override fun createAnimator(
      initialValue: T,
      targetValue: T,
      typeConverter: TypeConverter<T>,
      initialVelocity: AnimationVector?,
  ): Animator<T> {
    // Equal endpoints are a no-op only at rest. An interrupted spring can reach its target with
    // non-zero velocity and must move away from it before settling.
    if (initialValue == targetValue && initialVelocity.isNullOrZero()) {
      return FinishedAnimator(targetValue)
    }

    val targetVector = typeConverter.newVectorFromValue(targetValue)
    // valueVector holds the initial components only while seeding, then becomes the per-frame
    // buffer.
    val valueVector = typeConverter.newVectorFromValue(initialValue)
    // Each component gets its own solver seeded with its own inherited velocity, so an interrupted
    // multi-component spring preserves every axis's motion across the retarget.
    val solvers =
        Array(targetVector.size) { i ->
          SpringSolver.create(
              stiffness = stiffness.value,
              dampingRatio = dampingRatio.value,
              initialDisplacement = valueVector[i] - targetVector[i],
              initialVelocity = initialVelocity?.get(i) ?: 0f,
          )
        }
    return SpringAnimator(targetValue, targetVector, valueVector, typeConverter, solvers)
  }
}

private fun AnimationVector?.isNullOrZero(): Boolean {
  if (this == null) return true
  for (i in 0 until size) {
    if (this[i] != 0f) return false
  }
  return true
}

private class SpringAnimator<T>(
    private val targetValue: T,
    private val targetVector: AnimationVector,
    // Seeded with the initial components at creation, then reused as the per-frame output buffer.
    private val valueVector: AnimationVector,
    private val typeConverter: TypeConverter<T>,
    private val solvers: Array<SpringSolver>,
) : Animator<T> {

  override fun getValue(playTimeNanos: Long): T {
    if (isFinished(playTimeNanos)) return targetValue
    for (i in solvers.indices) {
      valueVector[i] = targetVector[i] + solvers[i].getDisplacement(playTimeNanos)
    }
    return typeConverter.fromVector(valueVector)
  }

  override fun getVelocity(playTimeNanos: Long, velocity: AnimationVector) {
    for (i in solvers.indices) velocity[i] = solvers[i].getVelocity(playTimeNanos)
  }

  override fun isFinished(playTimeNanos: Long): Boolean {
    for (solver in solvers) if (!solver.isFinished(playTimeNanos)) return false
    return true
  }
}

/** An [Animator] that is immediately finished and always returns the given value. */
internal class FinishedAnimator<T>(private val value: T) : Animator<T> {
  override fun getValue(playTimeNanos: Long): T = value

  override fun getVelocity(playTimeNanos: Long, velocity: AnimationVector) {
    velocity.reset()
  }

  override fun isFinished(playTimeNanos: Long): Boolean = true
}
