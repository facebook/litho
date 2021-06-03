/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.setRoot
import com.facebook.litho.widget.Text
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [useRef]. */
@RunWith(AndroidJUnit4::class)
class UseRefTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun useRef_onMutation_doesNotTriggerLayout() {
    val logCount = AtomicInteger()
    val renderCount = AtomicInteger()

    class UseRefTestComponent(
        private val logCount: AtomicInteger,
        private val renderCount: AtomicInteger,
        private val seq: Int
    ) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val hasLogged = useRef { false }
        if (!hasLogged.value) {
          logCount.incrementAndGet()
          hasLogged.value = true
        }

        renderCount.incrementAndGet()

        return Text(text = "Test $seq")
      }
    }

    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot { UseRefTestComponent(logCount = logCount, renderCount = renderCount, seq = 0) }
        .attachToWindow()
        .measure()
        .layout()

    assertThat(logCount.get()).isEqualTo(1)
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.setRoot {
      UseRefTestComponent(logCount = logCount, renderCount = renderCount, seq = 0)
    }

    assertThat(logCount.get()).isEqualTo(1)
    assertThat(renderCount.get()).isEqualTo(2)
  }
}
