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

package com.facebook.litho.diagnostics

import com.facebook.litho.AbstractCompilerTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class LithoHookUsageCheckerTest(private val useK2: Boolean) : AbstractCompilerTest() {

  companion object {
    @JvmStatic @Parameters(name = "useK2={0}") fun useK2() = listOf(false, true)
  }

  @Test
  fun hooks_misuse() {
    runTest("test-data/Hooks.misuse.kt", useK2 = useK2)
  }

  @Test
  fun hooks_good_use() {
    runTest("test-data/Hooks.good.kt", useK2 = useK2)
  }
}
