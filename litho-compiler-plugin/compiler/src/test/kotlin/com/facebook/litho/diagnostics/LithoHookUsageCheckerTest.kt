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

class LithoHookUsageCheckerTest : AbstractCompilerTest() {

  /**
   * `Hooks.misuse.kt` is also the regression test for what hook identity made legal. Its expected
   * output lists exactly one diagnostic — the hook called from a plain helper — while the file
   * still contains hooks in `if`, `when`, `for`, `while`, `forEach` and `try`/`catch`. If the
   * conditional checks deleted in D120344640 ever come back, those produce diagnostics that are not
   * in the expected output and this fails.
   */
  @Test
  fun hooks_misuse() {
    runHookIdentityTest("test-data/Hooks.misuse.kt")
  }

  @Test
  fun hooks_good_use() {
    runHookIdentityTest("test-data/Hooks.good.kt")
  }
}
