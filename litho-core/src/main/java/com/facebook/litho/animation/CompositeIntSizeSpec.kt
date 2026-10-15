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

import com.facebook.rendercore.IntSize
import kotlin.math.roundToInt

/**
 * Animates an [IntSize] as two independent axes, each driven by its own `Float` spec and
 * [Animator].
 *
 * This bridges two shapes: `Style.transition { bounds(...) }` declares bounds as per-property
 * `Float` specs, while the reflow store animates the size as one coupled [IntSize] — a single
 * layout read and frame loop per node. Composing the axis specs spans that gap without
 * reinterpreting a `Float` spec at [IntSize], so each spec runs at the type it was declared for:
 * the axes interpolate and settle independently, and each carries its own velocity across an
 * interruption.
 *
 * Both axes must be finite — the reflow store settles, and releases its per-frame relayout, only
 * once every axis has finished.
 */
internal data class CompositeIntSizeSpec(
    private val width: FiniteAnimationSpec<Float>,
    private val height: FiniteAnimationSpec<Float>,
) : FiniteAnimationSpec<IntSize> {

  override fun createAnimator(
      initialValue: IntSize,
      targetValue: IntSize,
      typeConverter: TypeConverter<IntSize>,
      initialVelocity: AnimationVector?,
  ): Animator<IntSize> = CompositeIntSizeAnimator(
      width.createAnimator(
          initialValue.width.toFloat(),
          targetValue.width.toFloat(),
          FloatTypeConverter,
          initialVelocity?.componentVector(0),
      ),
      height.createAnimator(
          initialValue.height.toFloat(),
          targetValue.height.toFloat(),
          FloatTypeConverter,
          initialVelocity?.componentVector(1),
      ),
  )

  /**
   * A 1-component velocity vector carrying this vector's [index] component, or null if it is zero.
   */
  private fun AnimationVector.componentVector(index: Int): AnimationVector? {
    val component = this[index]
    if (component == 0f) return null
    return AnimationVector.ofSize(1).apply { this[0] = component }
  }
}

private class CompositeIntSizeAnimator(
    private val width: Animator<Float>,
    private val height: Animator<Float>,
) : Animator<IntSize> {

  // Each axis reports into this in turn; held rather than allocated per call so a retarget-heavy
  // frame does not churn.
  private val axisVelocity = AnimationVector.ofSize(1)

  override fun getValue(playTimeNanos: Long): IntSize = IntSize(
      width.getValue(playTimeNanos).roundToInt(),
      height.getValue(playTimeNanos).roundToInt(),
  )

  override fun getVelocity(playTimeNanos: Long, velocity: AnimationVector) {
    width.getVelocity(playTimeNanos, axisVelocity)
    velocity[0] = axisVelocity[0]
    height.getVelocity(playTimeNanos, axisVelocity)
    velocity[1] = axisVelocity[0]
  }

  override fun isFinished(playTimeNanos: Long): Boolean =
      width.isFinished(playTimeNanos) && height.isFinished(playTimeNanos)
}
