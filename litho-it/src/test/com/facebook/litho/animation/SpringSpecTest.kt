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
import com.facebook.rendercore.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpringSpecTest {

  private val tolerance = Offset.offset(0.5f)

  private fun seconds(s: Double): Long = (s * 1_000_000_000).toLong()

  // -- Multi-channel springs: each axis is driven independently (no lossy scalar projection) --

  @Test
  fun springAnimator_intSizeSharingADiagonal_animatesPerAxisInsteadOfSnapping() {
    // hypot(100, 200) == hypot(200, 100): a single-float projection would call this a no-op and
    // teleport. Per-channel solvers animate width up and height down independently.
    val spec = spring<IntSize>()
    val animator = spec.createAnimator(IntSize(100, 200), IntSize(200, 100), IntSizeTypeConverter)

    assertThat(animator.isFinished(0L)).isFalse()
    val mid = animator.getValue(seconds(0.1))
    assertThat(mid.width).isGreaterThan(100).isLessThan(200)
    assertThat(mid.height).isGreaterThan(100).isLessThan(200)
  }

  @Test
  fun springAnimator_intSize_reachesTargetPerAxis() {
    val animator =
        spring<IntSize>().createAnimator(IntSize(0, 0), IntSize(1, 100), IntSizeTypeConverter)
    val velocity = AnimationVector2D()
    animator.getVelocity(seconds(0.5), velocity)
    assertThat(animator.isFinished(seconds(0.5))).isFalse()
    assertThat(velocity[0]).isGreaterThan(0f)
    assertThat(velocity[1]).isGreaterThan(0f)
    animator.getVelocity(seconds(1.0), velocity)
    assertThat(animator.isFinished(seconds(1.0))).isTrue()
    assertThat(animator.getValue(seconds(1.0))).isEqualTo(IntSize(1, 100))
    assertThat(velocity[0]).isZero()
    assertThat(velocity[1]).isZero()
    // Querying a completed animator must not affect earlier play times.
    animator.getVelocity(seconds(0.5), velocity)
    assertThat(animator.isFinished(seconds(0.5))).isFalse()
    assertThat(velocity[0]).isGreaterThan(0f)
  }

  @Test
  fun springAnimator_intSizeEqualValues_isImmediatelyFinished() {
    val spec = spring<IntSize>()
    val animator = spec.createAnimator(IntSize(100, 100), IntSize(100, 100), IntSizeTypeConverter)
    assertThat(animator.isFinished(0L)).isTrue()
    assertThat(animator.getValue(0L)).isEqualTo(IntSize(100, 100))
  }

  @Test
  fun springAnimator_intSizeInterruption_carriesPerAxisVelocity() {
    val spec = spring<IntSize>()
    val running = spec.createAnimator(IntSize(0, 0), IntSize(200, 100), IntSizeTypeConverter)

    // Mid-flight the two axes travel different distances, so they carry different velocities — a
    // single scalar could not represent both.
    val velocity = IntSizeTypeConverter.newVector()
    running.getVelocity(seconds(0.05), velocity)
    assertThat(velocity[0]).isGreaterThan(0f)
    assertThat(velocity[1]).isGreaterThan(0f)
    assertThat(velocity[0]).isNotEqualTo(velocity[1])

    // Seeding a fresh animator with that per-axis velocity advances BOTH axes further than a
    // from-rest animator at the same early time — the inherited velocity reached each solver.
    val seeded =
        spec.createAnimator(IntSize(0, 0), IntSize(200, 100), IntSizeTypeConverter, velocity)
    val fromRest = spec.createAnimator(IntSize(0, 0), IntSize(200, 100), IntSizeTypeConverter)
    val seededEarly = seeded.getValue(seconds(0.01))
    val restEarly = fromRest.getValue(seconds(0.01))
    assertThat(seededEarly.width).isGreaterThan(restEarly.width)
    assertThat(seededEarly.height).isGreaterThan(restEarly.height)
  }

  @Test
  fun springAnimator_color_animatesPerChannel() {
    val start = Color.fromRgba(red = 200, green = 0, blue = 0, alpha = 1f)
    val end = Color.fromRgba(red = 0, green = 200, blue = 0, alpha = 1f)
    val animator = spring<Color>().createAnimator(start, end, ColorTypeConverter)

    val mid = animator.getValue(seconds(0.05))
    // Red falls and green rises independently — no single-scalar projection could animate both.
    assertThat(mid.red).isGreaterThan(0).isLessThan(200)
    assertThat(mid.green).isGreaterThan(0).isLessThan(200)
  }

  @Test
  fun springAnimator_color_reachesTargetPerChannel() {
    val start = Color.fromRgba(red = 200, green = 0, blue = 0, alpha = 1f)
    val end = Color.fromRgba(red = 0, green = 200, blue = 0, alpha = 1f)
    val animator = spring<Color>().createAnimator(start, end, ColorTypeConverter)
    var settled = false
    for (i in 1..600) {
      val t = seconds(i * 0.01)
      if (animator.isFinished(t)) {
        val color = animator.getValue(t)
        assertThat(color.red).isEqualTo(0)
        assertThat(color.green).isEqualTo(200)
        assertThat(color.blue).isEqualTo(0)
        settled = true
        break
      }
    }
    assertThat(settled).isTrue()
  }

  // -- SpringSolver direct tests (internal, accessible via friend_paths) --

  @Test
  fun underdampedSpring_oscillatesAroundTarget() {
    val solver =
        SpringSolver.create(
            stiffness = Stiffness.Medium.value,
            dampingRatio = DampingRatio.HighBouncy.value, // 0.2 — very bouncy
            initialDisplacement = 100f,
            initialVelocity = 0f,
        )

    // At some point during the animation, displacement should cross zero (overshoot)
    var crossedZero = false
    for (i in 1..200) {
      val t = seconds(i * 0.01)
      val displacement = solver.getDisplacement(t)
      if (displacement < 0) {
        crossedZero = true
        break
      }
    }
    assertThat(crossedZero).isTrue()
  }

  @Test
  fun criticallyDampedSpring_doesNotOscillate() {
    val solver =
        SpringSolver.create(
            stiffness = Stiffness.Medium.value,
            dampingRatio = DampingRatio.NoBouncy.value, // 1.0 — critically damped
            initialDisplacement = 100f,
            initialVelocity = 0f,
        )

    // Displacement should stay positive (same sign as initial) and monotonically decrease
    var prev = 100f
    for (i in 1..200) {
      val t = seconds(i * 0.01)
      val displacement = solver.getDisplacement(t)
      assertThat(displacement).isGreaterThanOrEqualTo(-0.5f) // never significantly crosses zero
      if (displacement > 0.5f) {
        assertThat(displacement).isLessThanOrEqualTo(prev + 0.01f)
      }
      prev = displacement
    }
  }

  @Test
  fun overdampedSpring_doesNotOscillate() {
    val solver =
        SpringSolver.create(
            stiffness = Stiffness.Medium.value,
            dampingRatio = 2.0f, // overdamped
            initialDisplacement = 100f,
            initialVelocity = 0f,
        )

    // Displacement should stay positive and decrease monotonically
    var prev = 100f
    for (i in 1..200) {
      val t = seconds(i * 0.01)
      val displacement = solver.getDisplacement(t)
      assertThat(displacement).isGreaterThanOrEqualTo(-0.5f)
      if (displacement > 0.5f) {
        assertThat(displacement).isLessThanOrEqualTo(prev + 0.01f)
      }
      prev = displacement
    }
  }

  @Test
  fun springSolver_eventuallyConverges() {
    val solver =
        SpringSolver.create(
            stiffness = Stiffness.Medium.value,
            dampingRatio = DampingRatio.MediumBouncy.value,
            initialDisplacement = 100f,
            initialVelocity = 0f,
        )

    assertThat(solver.durationNanos).isLessThanOrEqualTo(seconds(5.0))
  }

  // -- SpringSpec + SpringAnimator tests --

  @Test
  fun springAnimator_reachesTargetValue() {
    val spec = spring<Float>(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.NoBouncy)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    // Find the time when animation finishes
    var finishTime = 0L
    for (i in 1..500) {
      val t = seconds(i * 0.01)
      if (animator.isFinished(t)) {
        finishTime = t
        break
      }
    }
    assertThat(finishTime).isGreaterThan(0)
    assertThat(animator.getValue(finishTime)).isCloseTo(100f, tolerance)
  }

  @Test
  fun springAnimator_withInitialVelocity_differsByTrajectory() {
    val spec = spring<Float>(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.NoBouncy)
    val noVelocity = spec.createAnimator(0f, 100f, FloatTypeConverter, AnimationVector1D(0f))
    val withVelocity = spec.createAnimator(0f, 100f, FloatTypeConverter, AnimationVector1D(500f))

    // At an early time, the velocity-boosted animation should be further along
    val earlyTime = seconds(0.1)
    val valueNoV = noVelocity.getValue(earlyTime)
    val valueWithV = withVelocity.getValue(earlyTime)
    assertThat(valueWithV).isGreaterThan(valueNoV)
  }

  @Test
  fun springAnimator_alreadyAtTarget_isImmediatelyFinished() {
    val spec = spring<Float>()
    val animator = spec.createAnimator(100f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(0L)).isTrue()
    assertThat(animator.getValue(0L)).isEqualTo(100f)
    val velocity = FloatTypeConverter.newVector()
    animator.getVelocity(0L, velocity)
    assertThat(velocity[0]).isEqualTo(0f)
  }

  @Test
  fun springAnimator_alreadyAtTargetWithVelocity_preservesMomentum() {
    val spec = spring<Float>()
    val animator = spec.createAnimator(100f, 100f, FloatTypeConverter, AnimationVector1D(500f))

    assertThat(animator.isFinished(0L)).isFalse()
    assertThat(animator.getValue(0L)).isEqualTo(100f)
    val velocity = FloatTypeConverter.newVector()
    animator.getVelocity(0L, velocity)
    assertThat(velocity[0]).isEqualTo(500f)
    assertThat(animator.getValue(seconds(0.01))).isGreaterThan(100f)

    var settled = false
    for (i in 1..500) {
      val t = seconds(i * 0.01)
      if (animator.isFinished(t)) {
        assertThat(animator.getValue(t)).isEqualTo(100f)
        settled = true
        break
      }
    }
    assertThat(settled).isTrue()
  }

  @Test
  fun springAnimator_smallFloatDeltaDoesNotFinishAtStart() {
    val spec = spring<Float>(stiffness = Stiffness.Medium, dampingRatio = DampingRatio.NoBouncy)
    val animator = spec.createAnimator(0.96f, 1f, FloatTypeConverter)

    assertThat(animator.isFinished(0L)).isFalse()
    assertThat(animator.getValue(0L)).isEqualTo(0.96f)

    val valueAfter100ms = animator.getValue(seconds(0.1))
    assertThat(valueAfter100ms).isGreaterThan(0.96f)
    assertThat(valueAfter100ms).isLessThan(1f)
  }

  @Test
  fun springAnimator_intValues_reachesTarget() {
    val spec = spring<Int>()
    val animator = spec.createAnimator(0, 100, IntTypeConverter)

    // Should eventually reach target
    var finished = false
    for (i in 1..500) {
      val t = seconds(i * 0.01)
      if (animator.isFinished(t)) {
        assertThat(animator.getValue(t)).isEqualTo(100)
        finished = true
        break
      }
    }
    assertThat(finished).isTrue()
  }

  @Test
  fun `quiet frame before a later excursion does not finish a bouncy spring`() {
    val solver = SpringSolver.create(10f, 0.2f, -1f, 0f)
    assertThat(solver.getDisplacement(seconds(5.55))).isBetween(-0.01f, 0.01f)
    assertThat(solver.getVelocity(seconds(5.55))).isBetween(-0.1f, 0.1f)
    assertThat(solver.durationNanos).isGreaterThan(seconds(5.55))
    assertThat(solver.getDisplacement(seconds(6.0))).isLessThan(-0.01f)
    assertThat(solver.durationNanos).isLessThanOrEqualTo(seconds(7.26))
  }

  @Test
  fun `small initial velocity at the target can produce a later excursion`() {
    val animator =
        spring<Float>(Stiffness(0.01f))
            .createAnimator(
                0f,
                0f,
                FloatTypeConverter,
                AnimationVector1D(0.0009f),
            )
    assertThat(animator.isFinished(0)).isFalse()
    assertThat(animator.getValue(seconds(10.0))).isGreaterThan(0.003f)
    assertThat(animator.isFinished(seconds(34.29))).isFalse()
    assertThat(animator.isFinished(seconds(34.30))).isTrue()
  }

  @Test
  fun `duration covers the last crossing across damping regimes and extreme roots`() {
    val cases = arrayOf(
        floatArrayOf(200f, 0.2f, 1f, 0f),
        floatArrayOf(200f, 0.99f, 1f, -14f), // Starts on the final descending segment.
        floatArrayOf(200f, 0.99995f, 1f, 20f),
        floatArrayOf(200f, 1.00005f, 1f, -20f),
        floatArrayOf(200f, 2f, 1f, -100f),
        floatArrayOf(200f, 0.000001f, 1f, 0f),
        floatArrayOf(200f, 100000000f, 1f, 0f),
    )
    for (case in cases) {
      val solver = SpringSolver.create(case[0], case[1], case[2], case[3])
      val duration = solver.durationNanos
      assertThat(duration).isGreaterThan(0)
      for (step in 0..100) {
        val t = duration + duration / 100 * step
        assertThat(solver.getDisplacement(t)).isBetween(-0.01f, 0.01f)
      }
    }
  }

  @Test
  fun `finite springs reject invalid parameters and shapes`() {
    for (value in
        floatArrayOf(0f, -1f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)) {
      assertThatThrownBy { DampingRatio(value) }.isInstanceOf(IllegalArgumentException::class.java)
      assertThatThrownBy { Stiffness(value) }.isInstanceOf(IllegalArgumentException::class.java)
    }
    val spec = spring<Float>()
    assertThatThrownBy { spec.createAnimator(0f, 0f, FloatTypeConverter, AnimationVector2D()) }
        .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `unsupported durations finish at the target without mutating inherited velocity`() {
    val cases = arrayOf(
        floatArrayOf(200f, 1e-12f, 0f, 1f, 0f),
        floatArrayOf(200f, Float.MIN_VALUE, 0f, 1f, 0f),
        floatArrayOf(200f, Float.MAX_VALUE, 0f, 1f, 0f),
        floatArrayOf(Float.MIN_VALUE, 1f, 0f, 1f, 0f),
        floatArrayOf(Float.MIN_VALUE, 0.2f, 0f, 0f, Float.MAX_VALUE),
        floatArrayOf(1f, 1e-9f, 0f, 1f, Float.MAX_VALUE),
    )
    assertThat(SpringSolver.create(1f, 1e-9f, -1f, 0f).durationNanos)
        .isNotEqualTo(SpringSolver.UNSUPPORTED_DURATION)
    for (case in cases) {
      assertThat(SpringSolver.create(case[0], case[1], case[2] - case[3], case[4]).durationNanos)
          .isEqualTo(SpringSolver.UNSUPPORTED_DURATION)
      val inheritedVelocity = AnimationVector1D(case[4])
      val animator =
          spring<Float>(Stiffness(case[0]), DampingRatio(case[1]))
              .createAnimator(case[2], case[3], FloatTypeConverter, inheritedVelocity)
      val velocity = AnimationVector1D()
      for (time in longArrayOf(0L, 1L, Long.MAX_VALUE)) {
        assertThat(animator.isFinished(time)).isTrue()
        assertThat(animator.getValue(time).toRawBits()).isEqualTo(case[3].toRawBits())
        velocity[0] = 500f
        animator.getVelocity(time, velocity)
        assertThat(velocity[0]).isZero()
      }
      assertThat(inheritedVelocity[0].toRawBits()).isEqualTo(case[4].toRawBits())
    }
  }

  @Test
  fun `one unsupported component finishes the entire spring`() {
    val inheritedVelocity = AnimationVector2D().apply { this[1] = Float.MAX_VALUE }
    val target = IntSize(1, 2)
    val animator =
        spring<IntSize>(Stiffness(1f), DampingRatio(1e-9f))
            .createAnimator(IntSize(0, 0), target, IntSizeTypeConverter, inheritedVelocity)
    val velocity = AnimationVector2D()
    for (time in longArrayOf(0L, 1L, Long.MAX_VALUE)) {
      assertThat(animator.isFinished(time)).isTrue()
      assertThat(animator.getValue(time)).isEqualTo(target)
      velocity[0] = 500f
      velocity[1] = 500f
      animator.getVelocity(time, velocity)
      assertThat(velocity[0]).isZero()
      assertThat(velocity[1]).isZero()
    }
    assertThat(inheritedVelocity[0]).isZero()
    assertThat(inheritedVelocity[1]).isEqualTo(Float.MAX_VALUE)
  }

  @Test
  fun `invalid motion finishes at its target without mutating inherited velocity`() {
    val cases = arrayOf(
        floatArrayOf(Float.NaN, 1f, 0f),
        floatArrayOf(Float.POSITIVE_INFINITY, 1f, 0f),
        floatArrayOf(Float.NEGATIVE_INFINITY, 1f, 0f),
        floatArrayOf(0f, Float.NaN, 0f),
        floatArrayOf(0f, Float.POSITIVE_INFINITY, 0f),
        floatArrayOf(0f, Float.NEGATIVE_INFINITY, 0f),
        floatArrayOf(Float.NaN, Float.NaN, 0f),
        floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0f),
        floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f),
        floatArrayOf(0f, 1f, Float.NaN),
        floatArrayOf(0f, 1f, Float.POSITIVE_INFINITY),
        floatArrayOf(0f, 1f, Float.NEGATIVE_INFINITY),
        floatArrayOf(Float.MAX_VALUE, -Float.MAX_VALUE, 0f),
        floatArrayOf(-Float.MAX_VALUE, Float.MAX_VALUE, 0f),
    )
    val spec = spring<Float>()
    for (case in cases) {
      val inheritedVelocity = AnimationVector1D(case[2])
      val animator = spec.createAnimator(case[0], case[1], FloatTypeConverter, inheritedVelocity)
      assertThat(animator.isFinished(0L)).isTrue()
      assertThat(animator.getValue(0L).toRawBits()).isEqualTo(case[1].toRawBits())
      assertThat(animator.getValue(Long.MAX_VALUE).toRawBits()).isEqualTo(case[1].toRawBits())
      val velocity = AnimationVector1D(500f)
      animator.getVelocity(0L, velocity)
      assertThat(velocity[0]).isZero()
      assertThat(inheritedVelocity[0].toRawBits()).isEqualTo(case[2].toRawBits())
    }
    val invalidTargetConverter =
        object : TypeConverter<Float> {
          override fun newVector(): AnimationVector = AnimationVector2D()

          override fun toVector(value: Float, vector: AnimationVector) {
            vector[0] = value
            vector[1] = Float.NaN
          }

          override fun fromVector(vector: AnimationVector): Float = vector[0]
        }
    val animator = spec.createAnimator(0f, 1f, invalidTargetConverter)
    assertThat(animator.isFinished(0L)).isTrue()
    assertThat(animator.getValue(0L)).isEqualTo(1f)
  }
}
