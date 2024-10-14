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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class DerivedDynamicValueTest {
  @Test
  fun testModifierIsApplied() {
    val dynamicValue = DynamicValue(5)
    val multiplyByFiveDynamicValue = DerivedDynamicValue(dynamicValue) { input -> 5 * input }
    assertThat(multiplyByFiveDynamicValue.get()).isEqualTo(25)
    dynamicValue.set(2)
    assertThat(multiplyByFiveDynamicValue.get()).isEqualTo(10)
  }
}
