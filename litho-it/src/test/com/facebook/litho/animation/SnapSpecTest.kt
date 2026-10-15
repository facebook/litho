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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapSpecTest {

  @Test
  fun snap_getValue_returnsTargetImmediately() {
    val spec = snap<Float>()
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.getValue(0L)).isEqualTo(100f)
  }

  @Test
  fun snap_isFinished_immediatelyTrue() {
    val spec = snap<Float>()
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    assertThat(animator.isFinished(0L)).isTrue()
  }

  @Test
  fun snap_velocity_isZero() {
    val spec = snap<Float>()
    val animator = spec.createAnimator(0f, 100f, FloatTypeConverter)

    val velocity = FloatTypeConverter.newVector()
    animator.getVelocity(0L, velocity)
    assertThat(velocity[0]).isEqualTo(0f)
  }

  @Test
  fun snap_intValues_returnsTarget() {
    val spec = snap<Int>()
    val animator = spec.createAnimator(0, 42, IntTypeConverter)

    assertThat(animator.getValue(0L)).isEqualTo(42)
    assertThat(animator.isFinished(0L)).isTrue()
  }
}
