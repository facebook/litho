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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimationVectorTest {

  @Test
  fun ofSize_createsVectorOfRequestedSize() {
    // Spans the field-backed shapes (1..4) and the array-backed fallback (5+).
    for (n in 1..6) {
      assertThat(AnimationVector.ofSize(n).size).describedAs("size %d", n).isEqualTo(n)
    }
  }

  @Test
  fun ofSize_belowOne_throws() {
    assertThatThrownBy { AnimationVector.ofSize(0) }
        .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun getSet_roundTripsPerComponent_fieldBacked() {
    val vector = AnimationVector.ofSize(3)
    vector[0] = 1f
    vector[1] = 2f
    vector[2] = 3f
    assertThat(vector[0]).isEqualTo(1f)
    assertThat(vector[1]).isEqualTo(2f)
    assertThat(vector[2]).isEqualTo(3f)
  }

  @Test
  fun getSet_roundTripsPerComponent_arrayBacked() {
    val vector = AnimationVector.ofSize(5)
    for (i in 0 until 5) vector[i] = (i + 1).toFloat()
    for (i in 0 until 5) assertThat(vector[i]).isEqualTo((i + 1).toFloat())
  }

  @Test
  fun outOfRangeAccess_isLenient() {
    val vector = AnimationVector.ofSize(2)
    vector[5] = 9f // ignored rather than throwing
    assertThat(vector[5]).isEqualTo(0f)
    assertThat(vector[0]).isEqualTo(0f)
  }
}
