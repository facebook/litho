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

package com.facebook.rendercore

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FastMathTest {

  @Test
  fun testRoundPositiveUp() {
    assertThat(2).isEqualTo(FastMath.round(1.6f))
  }

  @Test
  fun testRoundPositiveDown() {
    assertThat(1).isEqualTo(FastMath.round(1.3f))
  }

  @Test
  fun testRoundZero() {
    assertThat(0).isEqualTo(FastMath.round(0f))
  }

  @Test
  fun testRoundNegativeUp() {
    assertThat(-1).isEqualTo(FastMath.round(-1.3f))
  }

  @Test
  fun testRoundNegativeDown() {
    assertThat(-2).isEqualTo(FastMath.round(-1.6f))
  }
}
