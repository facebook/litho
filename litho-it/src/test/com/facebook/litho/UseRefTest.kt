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

import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [useRef]. */
@RunWith(LithoTestRunner::class)
class UseRefTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Test
  fun useRef_onMutation_doesNotTriggerLayout() {
    val logCount = AtomicInteger()
    val renderCount = AtomicInteger()
    val sideEffectCount = AtomicInteger()

    class UseRefTestComponent(
        private val logCount: AtomicInteger,
        private val renderCount: AtomicInteger,
        private val sideEffectCount: AtomicInteger,
        private val seq: Int,
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val hasLogged = useRef { false }
        // Pass `Any()` so deps differ on every render and the effect re-runs each layout.
        // `Unit` would only fire once (singleton equality), defeating the useRef demonstration.
        useEffect(Any()) {
          sideEffectCount.incrementAndGet()
          if (!hasLogged.value) {
            logCount.incrementAndGet()
            hasLogged.value = true
          }
          null
        }

        renderCount.incrementAndGet()

        return Text(text = "Test $seq")
      }
    }

    val testLithoView = lithoTestRule.createTestLithoView().attachToWindow().measure().layout()

    testLithoView.setRootAsync(
        UseRefTestComponent(
            logCount = logCount,
            renderCount = renderCount,
            sideEffectCount = sideEffectCount,
            seq = 0,
        ),
    )
    lithoTestRule.idle()

    assertThat(logCount.get()).isEqualTo(1)
    assertThat(renderCount.get()).isEqualTo(1)
    assertThat(sideEffectCount.get()).isEqualTo(1)

    testLithoView.setRootAsync(
        UseRefTestComponent(
            logCount = logCount,
            renderCount = renderCount,
            sideEffectCount = sideEffectCount,
            seq = 0,
        ),
    )
    lithoTestRule.idle()

    // useRef's value persists across renders → logCount stays at 1 even though
    // the effect fired again (sideEffectCount == 2, renderCount == 2).
    assertThat(logCount.get()).isEqualTo(1)
    assertThat(renderCount.get()).isEqualTo(2)
    assertThat(sideEffectCount.get()).isEqualTo(2)
  }
}
