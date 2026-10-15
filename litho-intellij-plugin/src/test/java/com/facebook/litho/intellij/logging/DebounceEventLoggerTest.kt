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

package com.facebook.litho.intellij.logging

import com.facebook.litho.intellij.extensions.EventLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DebounceEventLoggerTest {

  @Test
  fun logNoDebounce() {
    val testLogger = TestLogger()
    val debounceEventLogger = DebounceEventLogger(-1, testLogger)

    val count = 100
    for (i in 0 until count) {
      debounceEventLogger.log(null)
    }

    assertThat(testLogger.count).isEqualTo(count)
  }

  @Test
  fun logDebounce() {
    val testLogger = TestLogger()
    val debounceEventLogger = DebounceEventLogger(10, testLogger)

    for (i in 0 until 100) {
      debounceEventLogger.log(null)
    }

    assertThat(testLogger.count).isEqualTo(1)
  }

  @Test
  @Throws(InterruptedException::class)
  fun logDebouncePaused() {
    val testLogger = TestLogger()
    val debounceEventLogger = DebounceEventLogger(10, testLogger)

    val count = 5
    for (i in 0 until count) {
      debounceEventLogger.log(null)
      Thread.sleep(15)
    }

    assertThat(testLogger.count).isEqualTo(count)
  }

  internal class TestLogger : EventLogger {
    var count: Int = 0

    override fun log(event: String?, metadata: Map<String, String>) {
      count++
    }
  }
}
