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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.dp
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests that `useCached` keeps a stable, collision-free cache identity under hook identity when it
 * is routed to the legacy cached-values store (`useStateForCachedValues = false`). In that path the
 * cache key is derived from the current [HookKey] rather than a per-render sequential index, so a
 * conditional/looped `useCached` stays bound to its own entry when the hook set changes across
 * renders instead of colliding with a neighbor.
 *
 * enterHook/exitHook are called manually to simulate the compiler plugin, matching
 * [HookIdentityTest].
 */
@Suppress("LegacyLooperMode")
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class KCachedHookIdentityTest {

  @Rule
  @JvmField
  val lithoTestRule = LithoTestRule(
      ComponentsConfiguration.defaultInstance.copy(
          useStateForCachedValues = false,
      ),
  )

  @Test
  fun useCached_conditionalCallSite_staysBoundToItsOwnCacheEntry() {
    val cachedRef = AtomicReference("")
    val secondComputeCount = AtomicInteger(0)

    class TestComponent(val includeFirst: Boolean) : KComponent() {
      // Intentional rule-of-hooks violation: the first useCached is conditional. With sequential
      // index keying, skipping it shifts the second useCached onto the first's slot and (since both
      // share an input) returns the first's cached value. HookKey identity keeps the second bound
      // to
      // its own entry. enterHook/exitHook simulates the compiler plugin.
      override fun ComponentScope.render(): Component? {
        if (includeFirst) {
          enterHook(0)
          useCached("shared") { "from-first" }
          exitHook()
        }

        enterHook(1)
        val second =
            useCached("shared") {
              secondComputeCount.incrementAndGet()
              "from-second"
            }
        exitHook()
        cachedRef.set(second)

        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    // Both useCached calls run; the second computes and caches its own value.
    val testLithoView = lithoTestRule.render { TestComponent(includeFirst = true) }
    assertThat(cachedRef.get()).isEqualTo("from-second")
    assertThat(secondComputeCount.get()).isEqualTo(1)

    // The first useCached is now skipped. Under index keying the second would collide with the
    // first's slot and return "from-first"; under HookKey identity it stays bound to its own entry
    // and reads its cached value without recomputing.
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(includeFirst = false)) }
    assertThat(cachedRef.get()).isEqualTo("from-second")
    assertThat(secondComputeCount.get()).isEqualTo(1)
  }
}
