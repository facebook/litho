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

package com.facebook.rendercore.debug

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DurationValueTest {

  @Test
  fun `to string duration less than 1000 correctly`() {
    val duration = Duration(999)
    assertThat(duration.value).isEqualTo(999)
    assertThat(duration.toString()).isEqualTo("999 ns")
  }

  @Test
  fun `to string less than 1_000_000 correctly`() {
    val duration = Duration(999_999)
    assertThat(duration.value).isEqualTo(999_999)
    assertThat(duration.toString()).isEqualTo("999 µs")
  }

  @Test
  fun `to string more than 1_000_000 correctly`() {
    val duration = Duration(999_999_999)
    assertThat(duration.value).isEqualTo(999_999_999)
    assertThat(duration.toString()).isEqualTo("999 ms")
  }
}
