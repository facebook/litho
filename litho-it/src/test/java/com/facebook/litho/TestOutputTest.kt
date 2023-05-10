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

package com.facebook.litho

import android.graphics.Rect
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class TestOutputTest {

  private lateinit var testOutput: TestOutput

  @Before
  fun setup() {
    testOutput = TestOutput()
  }

  @Test
  fun testPositionAndSizeSet() {
    testOutput.setBounds(0, 1, 3, 4)
    assertThat(testOutput.bounds.left).isEqualTo(0)
    assertThat(testOutput.bounds.top).isEqualTo(1)
    assertThat(testOutput.bounds.right).isEqualTo(3)
    assertThat(testOutput.bounds.bottom).isEqualTo(4)
  }

  @Test
  fun testRectBoundsSet() {
    val bounds = Rect(0, 1, 3, 4)
    testOutput.bounds = bounds
    assertThat(testOutput.bounds.left).isEqualTo(0)
    assertThat(testOutput.bounds.top).isEqualTo(1)
    assertThat(testOutput.bounds.right).isEqualTo(3)
    assertThat(testOutput.bounds.bottom).isEqualTo(4)
  }
}
