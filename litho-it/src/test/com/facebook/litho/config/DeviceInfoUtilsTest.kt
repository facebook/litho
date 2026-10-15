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

package com.facebook.litho.config

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class DeviceInfoUtilsTest {

  @Before
  fun setUp() {
    resetNumCoresCache()
  }

  @After
  fun tearDown() {
    resetNumCoresCache()
  }

  @Test
  fun getCoresFromFileString_withNullOrEmpty_returnsUnknown() {
    assertThat(DeviceInfoUtils.getCoresFromFileString(null))
        .isEqualTo(DeviceInfoUtils.DEVICEINFO_UNKNOWN)
    assertThat(DeviceInfoUtils.getCoresFromFileString(""))
        .isEqualTo(DeviceInfoUtils.DEVICEINFO_UNKNOWN)
  }

  @Test
  fun getCoresFromFileString_withInvalidFormats_returnsUnknown() {
    val invalidInputs = listOf("1-3", "0-", "abc", "0-3abc", "prefix0-3", "-", "3")
    for (input in invalidInputs) {
      assertThat(DeviceInfoUtils.getCoresFromFileString(input))
          .describedAs("Expected UNKNOWN for input: '$input'")
          .isEqualTo(DeviceInfoUtils.DEVICEINFO_UNKNOWN)
    }
  }

  @Test
  fun getCoresFromFileString_withValidFormats_returnsCorrectCoreCount() {
    assertThat(DeviceInfoUtils.getCoresFromFileString("0-0")).isEqualTo(1)
    assertThat(DeviceInfoUtils.getCoresFromFileString("0-3")).isEqualTo(4)
    assertThat(DeviceInfoUtils.getCoresFromFileString("0-7")).isEqualTo(8)
    assertThat(DeviceInfoUtils.getCoresFromFileString("0-99")).isEqualTo(100)
  }

  @Test
  fun hasMultipleCores_whenCoresGreaterThanOne_returnsTrue() {
    setNumCoresCache(4)
    assertThat(DeviceInfoUtils.hasMultipleCores()).isTrue
  }

  @Test
  fun hasMultipleCores_whenCoresEqualsOne_returnsFalse() {
    setNumCoresCache(1)
    assertThat(DeviceInfoUtils.hasMultipleCores()).isFalse
  }

  @Test
  fun hasMultipleCores_whenCoresIsUnknown_returnsFalse() {
    setNumCoresCache(DeviceInfoUtils.DEVICEINFO_UNKNOWN)
    assertThat(DeviceInfoUtils.hasMultipleCores()).isFalse
  }

  // --- numberOfCPUCores caching tests ---

  @Test
  fun numberOfCPUCores_returnsCachedValue_whenAlreadySet() {
    setNumCoresCache(8)
    val result = DeviceInfoUtils.numberOfCPUCores
    assertThat(result).isEqualTo(8)
  }

  @Test
  fun numberOfCPUCores_returnsSameValueOnSecondCall() {
    val firstResult = DeviceInfoUtils.numberOfCPUCores
    val secondResult = DeviceInfoUtils.numberOfCPUCores
    assertThat(secondResult).isEqualTo(firstResult)
  }

  // --- Helper methods ---

  private fun resetNumCoresCache() {
    val field = DeviceInfoUtils::class.java.getDeclaredField("numCores")
    field.isAccessible = true
    field.setInt(DeviceInfoUtils, DeviceInfoUtils.NUM_CORES_NOT_SET)
  }

  private fun setNumCoresCache(value: Int) {
    val field = DeviceInfoUtils::class.java.getDeclaredField("numCores")
    field.isAccessible = true
    field.setInt(DeviceInfoUtils, value)
  }
}
