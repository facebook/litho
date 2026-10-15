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

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [Duration] extension properties. */
@RunWith(RobolectricTestRunner::class)
class DurationTest {

  @Test
  fun `Int ms creates correct milliseconds duration`() {
    assertThat(0.ms).isEqualTo(0.milliseconds)
    assertThat(1.ms).isEqualTo(1.milliseconds)
    assertThat(100.ms).isEqualTo(100.milliseconds)
    assertThat(300.ms).isEqualTo(300.milliseconds)
    assertThat(1000.ms).isEqualTo(1000.milliseconds)
  }

  @Test
  fun `Long ms creates correct milliseconds duration`() {
    assertThat(0L.ms).isEqualTo(0.milliseconds)
    assertThat(1L.ms).isEqualTo(1.milliseconds)
    assertThat(100L.ms).isEqualTo(100.milliseconds)
    assertThat(5000L.ms).isEqualTo(5000.milliseconds)
    assertThat(Long.MAX_VALUE.ms).isEqualTo(Long.MAX_VALUE.milliseconds)
  }

  @Test
  fun `Double ms creates correct milliseconds duration`() {
    assertThat(0.0.ms).isEqualTo(0.0.milliseconds)
    assertThat(1.5.ms).isEqualTo(1.5.milliseconds)
    assertThat(100.25.ms).isEqualTo(100.25.milliseconds)
    assertThat(300.75.ms).isEqualTo(300.75.milliseconds)
  }

  @Test
  fun `Int s creates correct seconds duration`() {
    assertThat(0.s).isEqualTo(0.seconds)
    assertThat(1.s).isEqualTo(1.seconds)
    assertThat(5.s).isEqualTo(5.seconds)
    assertThat(60.s).isEqualTo(60.seconds)
  }

  @Test
  fun `Long s creates correct seconds duration`() {
    assertThat(0L.s).isEqualTo(0.seconds)
    assertThat(1L.s).isEqualTo(1.seconds)
    assertThat(100L.s).isEqualTo(100.seconds)
    assertThat(3600L.s).isEqualTo(3600.seconds)
  }

  @Test
  fun `Double s creates correct seconds duration`() {
    assertThat(0.0.s).isEqualTo(0.0.seconds)
    assertThat(1.5.s).isEqualTo(1.5.seconds)
    assertThat(2.25.s).isEqualTo(2.25.seconds)
    assertThat(0.5.s).isEqualTo(0.5.seconds)
  }

  @Test
  fun `ms and s conversions are equivalent`() {
    assertThat(1000.ms).isEqualTo(1.s)
    assertThat(2000.ms).isEqualTo(2.s)
    assertThat(500.ms).isEqualTo(0.5.s)
    assertThat(1500.ms).isEqualTo(1.5.s)
  }

  @Test
  fun `duration values exhibit equality`() {
    assertThat(100.ms).isEqualTo(100.ms)
    assertThat(1.s).isEqualTo(1.s)
    assertThat(100.ms).isNotEqualTo(200.ms)
    assertThat(1.s).isNotEqualTo(2.s)
  }

  @Test
  fun `different types produce same duration for equal values`() {
    assertThat(100.ms).isEqualTo(100L.ms)
    assertThat(100.ms).isEqualTo(100.0.ms)
    assertThat(1.s).isEqualTo(1L.s)
    assertThat(1.s).isEqualTo(1.0.s)
  }
}
