// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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
