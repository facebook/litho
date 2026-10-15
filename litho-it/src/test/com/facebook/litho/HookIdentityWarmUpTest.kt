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

/**
 * [HookIdentityWarmUp] runs on the app-startup path of both FB4A and IG4A, in every experiment arm,
 * so the bar is that it completes quietly and leaves no trace a real render could observe.
 *
 * The warm-up's actual purpose -- getting the hook-identity methods into the harvested ART profile
 * -- cannot be asserted here; it only shows up in a Deep Data harvest weeks later.
 */
@RunWith(LithoTestRunner::class)
class HookIdentityWarmUpTest {

  @Test
  fun `warm up completes and records a result`() {
    HookIdentityWarmUp.warmUp()

    assertThat(HookIdentityWarmUp.lastResult)
        .describedAs("the sink that stops R8/Redex eliminating the warm-up")
        .isNotNull
  }

  @Test
  fun `warm up is idempotent`() {
    HookIdentityWarmUp.warmUp()
    val afterFirst = HookIdentityWarmUp.lastResult

    HookIdentityWarmUp.warmUp()

    assertThat(HookIdentityWarmUp.lastResult).isEqualTo(afterFirst)
  }

  @Test
  fun `warm up leaves interning intact for real call site ids`() {
    HookIdentityWarmUp.warmUp()

    // The warm-up interns one root under a reserved negative call-site id. Real ids start at 0 and
    // must still resolve to the canonical instance.
    val first = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = true)
    val second = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = true)

    assertThat(first).isSameAs(second)
  }

  @Test
  fun `warm up keys never collide with real keys`() {
    HookIdentityWarmUp.warmUp()

    val real = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = true)
    val warmUp = HookKey.create(callSiteId = Int.MIN_VALUE, index = 0, parent = null, intern = true)

    assertThat(real).isNotEqualTo(warmUp)
    assertThat(real.id).isNotEqualTo(warmUp.id)
  }
}
