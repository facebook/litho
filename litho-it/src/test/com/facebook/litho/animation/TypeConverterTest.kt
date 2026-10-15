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

import android.graphics.Color as AndroidColor
import com.facebook.rendercore.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypeConverterTest {

  // -- FloatTypeConverter (AnimationVector1D) --

  @Test
  fun floatConverter_isOneComponent_andRoundTrips() {
    val vector = FloatTypeConverter.newVector()
    assertThat(vector.size).isEqualTo(1)
    FloatTypeConverter.toVector(3.14f, vector)
    assertThat(vector[0]).isEqualTo(3.14f)
    assertThat(FloatTypeConverter.fromVector(AnimationVector1D(2.71f))).isEqualTo(2.71f)
  }

  // -- IntTypeConverter (AnimationVector1D) --

  @Test
  fun intConverter_isOneComponent_andRoundsFromVector() {
    val vector = IntTypeConverter.newVector()
    assertThat(vector.size).isEqualTo(1)
    IntTypeConverter.toVector(42, vector)
    assertThat(vector[0]).isEqualTo(42f)
    assertThat(IntTypeConverter.fromVector(AnimationVector1D(3.4f))).isEqualTo(3)
    assertThat(IntTypeConverter.fromVector(AnimationVector1D(3.5f))).isEqualTo(4)
  }

  // -- ColorTypeConverter (AnimationVector4D: alpha, red, green, blue) --

  @Test
  fun colorConverter_isFourComponents_andSplitsArgb() {
    val vector = ColorTypeConverter.newVector()
    assertThat(vector.size).isEqualTo(4)
    ColorTypeConverter.toVector(Color.fromInt(AndroidColor.argb(255, 200, 0, 0)), vector)
    assertThat(vector[0]).isEqualTo(1f) // alpha
    assertThat(vector[1]).isEqualTo(200f) // red
    assertThat(vector[2]).isEqualTo(0f) // green
    assertThat(vector[3]).isEqualTo(0f) // blue
  }

  @Test
  fun colorConverter_fromVector_reconstructsAndClampsPerComponent() {
    val exact = ColorTypeConverter.fromVector(AnimationVector4D(1f, 100f, 50f, 0f))
    assertThat(exact.alpha).isEqualTo(1f)
    assertThat(exact.red).isEqualTo(100)
    assertThat(exact.green).isEqualTo(50)
    assertThat(exact.blue).isEqualTo(0)

    // Overshoot on individual components clamps independently.
    val clamped = ColorTypeConverter.fromVector(AnimationVector4D(1.5f, -100f, 300f, 0f))
    assertThat(clamped.alpha).isEqualTo(1f)
    assertThat(clamped.red).isEqualTo(0)
    assertThat(clamped.green).isEqualTo(255)
  }

  // -- evaluateColor (standalone per-channel blend) --

  @Test
  fun evaluateColor_atMidpoint_blendsChannels() {
    val start = Color.fromInt(AndroidColor.argb(255, 200, 0, 0))
    val end = Color.fromInt(AndroidColor.argb(255, 0, 100, 0))
    val mid = evaluateColor(0.5f, start, end)

    assertThat(mid.alpha).isEqualTo(1f)
    assertThat(mid.red).isEqualTo(100)
    assertThat(mid.green).isEqualTo(50)
    assertThat(mid.blue).isEqualTo(0)
  }

  @Test
  fun evaluateColor_clampsChannelsToValidRange() {
    // Overshoot (fraction > 1) should clamp channels to [0, 255]
    val result = evaluateColor(
        1.5f,
        Color.fromInt(AndroidColor.argb(255, 200, 0, 0)),
        Color.fromInt(AndroidColor.argb(255, 0, 100, 0)),
    )
    assertThat(result.red).isEqualTo(0) // 200 + (0 - 200) * 1.5 = -100 → clamped to 0
    assertThat(result.green).isEqualTo(150) // 0 + 100 * 1.5 = 150
  }

  // -- Custom converter: the public extension point via AnimationVector.ofSize --

  private data class Vec3(val x: Float, val y: Float, val z: Float)

  private object Vec3Converter : TypeConverter<Vec3> {
    override fun newVector(): AnimationVector = AnimationVector.ofSize(3)

    override fun toVector(value: Vec3, vector: AnimationVector) {
      vector[0] = value.x
      vector[1] = value.y
      vector[2] = value.z
    }

    override fun fromVector(vector: AnimationVector): Vec3 = Vec3(vector[0], vector[1], vector[2])
  }

  @Test
  fun customConverter_newVectorIsCorrectlyShaped_andRoundTrips() {
    assertThat(Vec3Converter.newVector().size).isEqualTo(3)
    val vector = Vec3Converter.newVector()
    Vec3Converter.toVector(Vec3(1f, 2f, 3f), vector)
    assertThat(vector[0]).isEqualTo(1f)
    assertThat(vector[1]).isEqualTo(2f)
    assertThat(vector[2]).isEqualTo(3f)
    assertThat(Vec3Converter.fromVector(vector)).isEqualTo(Vec3(1f, 2f, 3f))
  }

  @Test
  fun customConverter_drivesSpringPerComponent() {
    // Exercises the whole extension point: a converter defined with only the public API animates
    // every component independently through a real spring.
    val animator =
        spring<Vec3>().createAnimator(Vec3(0f, 0f, 0f), Vec3(100f, 200f, 300f), Vec3Converter)
    val mid = animator.getValue(50_000_000L) // 50ms
    assertThat(mid.x).isGreaterThan(0f).isLessThan(100f)
    assertThat(mid.y).isGreaterThan(0f).isLessThan(200f)
    assertThat(mid.z).isGreaterThan(0f).isLessThan(300f)
  }
}
