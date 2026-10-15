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
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EasingTest {

  private val tolerance = Offset.offset(0.001f)

  @Test
  fun linear_atBoundaries() {
    assertThat(Easing.Linear.transform(0f)).isEqualTo(0f)
    assertThat(Easing.Linear.transform(1f)).isEqualTo(1f)
  }

  @Test
  fun linear_atMidpoint() {
    assertThat(Easing.Linear.transform(0.5f)).isEqualTo(0.5f)
  }

  @Test
  fun easeIn_startsSlowerThanLinear() {
    val easeInMid = Easing.EaseIn.transform(0.5f)
    assertThat(easeInMid).isLessThan(0.5f)
  }

  @Test
  fun easeIn_atBoundaries() {
    assertThat(Easing.EaseIn.transform(0f)).isCloseTo(0f, tolerance)
    assertThat(Easing.EaseIn.transform(1f)).isCloseTo(1f, tolerance)
  }

  @Test
  fun easeOut_startsFasterThanLinear() {
    val easeOutMid = Easing.EaseOut.transform(0.5f)
    assertThat(easeOutMid).isGreaterThan(0.5f)
  }

  @Test
  fun easeOut_atBoundaries() {
    assertThat(Easing.EaseOut.transform(0f)).isCloseTo(0f, tolerance)
    assertThat(Easing.EaseOut.transform(1f)).isCloseTo(1f, tolerance)
  }

  @Test
  fun easeInOut_atBoundaries() {
    assertThat(Easing.EaseInOut.transform(0f)).isCloseTo(0f, tolerance)
    assertThat(Easing.EaseInOut.transform(1f)).isCloseTo(1f, tolerance)
  }

  @Test
  fun easeInOut_isSymmetric() {
    // EaseInOut should be roughly symmetric: slower at both ends
    val earlyValue = Easing.EaseInOut.transform(0.25f)
    val lateValue = Easing.EaseInOut.transform(0.75f)
    // early < 0.25 (slow start) and late > 0.75 (slow end)
    assertThat(earlyValue).isLessThan(0.25f)
    assertThat(lateValue).isGreaterThan(0.75f)
  }

  @Test
  fun fastOutSlowIn_atBoundaries() {
    assertThat(Easing.FastOutSlowIn.transform(0f)).isCloseTo(0f, tolerance)
    assertThat(Easing.FastOutSlowIn.transform(1f)).isCloseTo(1f, tolerance)
  }

  @Test
  fun cubicBezierEasing_isMonotonic() {
    val easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    var prev = 0f
    for (i in 1..10) {
      val fraction = i / 10f
      val value = easing.transform(fraction)
      assertThat(value).isGreaterThanOrEqualTo(prev)
      prev = value
    }
  }

  @Test
  fun cubicBezierEasing_equals() {
    val a = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val b = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val c = CubicBezierEasing(0.5f, 0f, 0.2f, 1f)
    assertThat(a).isEqualTo(b)
    assertThat(a).isNotEqualTo(c)
  }
}
