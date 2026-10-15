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
import com.facebook.rendercore.ms
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyframesSpecTest {

  private val tolerance = Offset.offset(0.01f)

  private fun ms(millis: Long): Long = millis * 1_000_000L

  @Test
  fun twoKeyframes_behavesLikeLinearTween() {
    val spec =
        keyframes<Float>(duration = 200.ms) {
          0f at 0.ms
          100f at 200.ms
        }
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(0L)).isCloseTo(0f, tolerance)
    assertThat(animator.getValue(ms(100))).isCloseTo(50f, tolerance)
    assertThat(animator.getValue(ms(200))).isCloseTo(100f, tolerance)
  }

  @Test
  fun threeKeyframes_hitsMiddleKeyframe() {
    val spec =
        keyframes<Float>(duration = 200.ms) {
          0f at 0.ms
          80f at 100.ms
          100f at 200.ms
        }
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    // At the midpoint, should be at the middle keyframe value
    assertThat(animator.getValue(ms(100))).isCloseTo(80f, tolerance)
  }

  @Test
  fun keyframes_withEasing_affectsSegment() {
    val linearSpec =
        keyframes<Float>(duration = 200.ms) {
          0f at 0.ms
          100f at 200.ms
        }
    val easedSpec =
        keyframes<Float>(duration = 200.ms) {
          0f at 0.ms
          100f at 200.ms using Easing.EaseIn
        }

    val linearAnimator = linearSpec.createAnimator(0f, 100f, FloatTypeConverter)
    val easedAnimator = easedSpec.createAnimator(0f, 100f, FloatTypeConverter)

    // EaseIn should produce a smaller value at midpoint than linear
    val linearMid = linearAnimator.getValue(ms(100))
    val easedMid = easedAnimator.getValue(ms(100))
    assertThat(easedMid).isLessThan(linearMid)
  }

  @Test
  fun keyframes_isFinished_atDuration() {
    val spec =
        keyframes<Float>(duration = 300.ms) {
          0f at 0.ms
          100f at 300.ms
        }
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(ms(299))).isFalse()
    assertThat(animator.isFinished(ms(300))).isTrue()
  }

  @Test
  fun keyframes_pastEnd_returnsTargetValue() {
    val spec =
        keyframes<Float>(duration = 200.ms) {
          0f at 0.ms
          100f at 200.ms
        }
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(ms(500))).isCloseTo(100f, tolerance)
  }

  @Test
  fun keyframes_implicitStartAndEnd() {
    // No explicit keyframe at t=0 or t=duration — should use initialValue and targetValue
    val spec = keyframes<Float>(duration = 200.ms) { 50f at 100.ms }
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(0L)).isCloseTo(0f, tolerance)
    assertThat(animator.getValue(ms(100))).isCloseTo(50f, tolerance)
    assertThat(animator.getValue(ms(200))).isCloseTo(100f, tolerance)
  }

  @Test
  fun keyframes_intSizeVelocity_isComputedBeforeRounding() {
    val spec =
        keyframes<IntSize>(duration = 1_000.ms) {
          IntSize(0, 0) at 0.ms
          IntSize(100, 200) at 1_000.ms
        }
    val animator = spec.createAnimator(IntSize(0, 0), IntSize(100, 200), IntSizeTypeConverter)
    val velocity = IntSizeTypeConverter.newVector()

    animator.getVelocity(ms(500), velocity)

    assertThat(velocity[0]).isCloseTo(100f, tolerance)
    assertThat(velocity[1]).isCloseTo(200f, tolerance)
  }

  @Test
  fun keyframes_colorVelocity_isComputedBeforeChannelRounding() {
    val start = Color.fromRgba(red = 0, green = 0, blue = 0, alpha = 0f)
    val end = Color.fromRgba(red = 255, green = 100, blue = 50, alpha = 1f)
    val spec =
        keyframes<Color>(duration = 1_000.ms) {
          start at 0.ms
          end at 1_000.ms
        }
    val animator = spec.createAnimator(start, end, ColorTypeConverter)
    val velocity = ColorTypeConverter.newVector()

    animator.getVelocity(ms(500), velocity)

    assertThat(velocity[0]).isCloseTo(1f, tolerance)
    assertThat(velocity[1]).isCloseTo(255f, tolerance)
    assertThat(velocity[2]).isCloseTo(100f, tolerance)
    assertThat(velocity[3]).isCloseTo(50f, tolerance)
  }
}
