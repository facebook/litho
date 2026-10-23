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

import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoStatsSuppressionTest {

  @Test
  fun `suppressed nests, and counting resumes once the outermost one ends`() {
    val counter = AtomicLong(0)

    LithoStats.suppressed {
      LithoStats.count(counter)
      LithoStats.suppressed { LithoStats.count(counter) }
      LithoStats.count(counter)
    }
    assertThat(counter.get()).describedAs("counted while suppressed").isEqualTo(0)

    LithoStats.count(counter)
    assertThat(counter.get()).describedAs("counted after").isEqualTo(1)
  }

  @Test
  fun `suppressed ends when its block throws`() {
    val counter = AtomicLong(0)

    runCatching { LithoStats.suppressed { error("thrown") } }
    LithoStats.count(counter)

    assertThat(counter.get()).isEqualTo(1)
  }
}
