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

import com.facebook.rendercore.ms
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TweenSpecTest {

  private val tolerance = Offset.offset(0.01f)

  private fun ms(millis: Long): Long = millis * 1_000_000L

  @Test
  fun linearTween_atStart_returnsInitialValue() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(0L)).isCloseTo(0f, tolerance)
  }

  @Test
  fun linearTween_atMidpoint_returnsHalfway() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(ms(150))).isCloseTo(50f, tolerance)
  }

  @Test
  fun linearTween_atEnd_returnsTargetValue() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(ms(300))).isCloseTo(100f, tolerance)
  }

  @Test
  fun linearTween_pastEnd_returnsTargetValue() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(ms(500))).isCloseTo(100f, tolerance)
  }

  @Test
  fun tweenWithEasing_atMidpoint_differsByLinear() {
    val linear = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val eased = tween<Float>(duration = 300.ms, easing = Easing.EaseIn)

    val linearAnimator = linear.createAnimator(0f, 100f, FloatTypeConverter)
    val easedAnimator = eased.createAnimator(0f, 100f, FloatTypeConverter)

    val linearMid = linearAnimator.getValue(ms(150))
    val easedMid = easedAnimator.getValue(ms(150))

    // EaseIn is slower at start, so midpoint value should be less than linear
    assertThat(easedMid).isLessThan(linearMid)
  }

  @Test
  fun tweenWithDelay_duringDelay_returnsInitialValue() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear, delay = 100.ms)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(0L)).isCloseTo(0f, tolerance)
    assertThat(animator.getValue(ms(50))).isCloseTo(0f, tolerance)
    assertThat(animator.getValue(ms(100))).isCloseTo(0f, tolerance)
  }

  @Test
  fun tweenWithDelay_afterDelay_animatesNormally() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear, delay = 100.ms)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    // At 250ms (150ms into animation), should be halfway
    assertThat(animator.getValue(ms(250))).isCloseTo(50f, tolerance)
    // At 400ms (300ms into animation), should be done
    assertThat(animator.getValue(ms(400))).isCloseTo(100f, tolerance)
  }

  @Test
  fun tween_isFinished_beforeDuration_returnsFalse() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(ms(150))).isFalse()
  }

  @Test
  fun tween_isFinished_atDuration_returnsTrue() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(ms(300))).isTrue()
  }

  @Test
  fun tweenWithDelay_isFinished_accountsForDelay() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear, delay = 100.ms)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(ms(300))).isFalse() // only 200ms of animation done
    assertThat(animator.isFinished(ms(400))).isTrue() // 300ms animation + 100ms delay
  }

  @Test
  fun tween_velocity_duringAnimation_isNonZero() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    val velocity = FloatTypeConverter.newVector()
    animator.getVelocity(ms(150), velocity)
    assertThat(velocity[0]).isNotEqualTo(0f)
  }

  @Test
  fun tween_velocity_afterFinish_isZero() {
    val spec = tween<Float>(duration = 300.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    val velocity = FloatTypeConverter.newVector()
    animator.getVelocity(ms(300), velocity)
    assertThat(velocity[0]).isEqualTo(0f)
  }

  @Test
  fun tween_intValues_interpolatesAndRounds() {
    val spec = tween<Int>(duration = 200.ms, easing = Easing.Linear)
    val animator = spec.createAnimator(0, 10, IntTypeConverter)

    assertThat(animator.getValue(ms(100))).isEqualTo(5) // midpoint
    assertThat(animator.getValue(ms(200))).isEqualTo(10) // end
  }
}
