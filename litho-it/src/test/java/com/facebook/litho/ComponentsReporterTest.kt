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

import android.util.Pair
import com.facebook.litho.testing.logging.TestComponentsReporter
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.LogLevel
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [ComponentsReporterTest] */
@RunWith(LithoTestRunner::class)
class ComponentsReporterTest {

  private lateinit var reporter: TestComponentsReporter

  @Before
  fun setup() {
    reporter = TestComponentsReporter()
    ComponentsReporter.provide(reporter)
  }

  @After
  fun tearDown() {
    ComponentsReporter.provide(DefaultComponentsReporter())
  }

  @Test
  fun testEmitFatalMessage() {
    val throwable =
        ThrowableAssert.catchThrowable {
          ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.FATAL, CATEGORY_KEY, FATAL_MSG)
          assertThat(reporter.loggedMessages.size).isEqualTo(1)
          assertThat(reporter.loggedMessages).contains(Pair(LogLevel.FATAL, FATAL_MSG))
        }
    assertThat(throwable).isInstanceOf(RuntimeException::class.java)
    assertThat(throwable.message).isEqualTo(FATAL_MSG)
  }

  @Test
  fun testEmitErrorMessage() {
    ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.ERROR, CATEGORY_KEY, ERROR_MSG)
    assertThat(reporter.loggedMessages.size).isEqualTo(1)
    assertThat(reporter.loggedMessages).contains(Pair(LogLevel.ERROR, ERROR_MSG))
  }

  @Test
  fun testEmitWarningMessage() {
    ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.WARNING, CATEGORY_KEY, WARNING_MSG)
    assertThat(reporter.loggedMessages.size).isEqualTo(1)
    assertThat(reporter.loggedMessages).contains(Pair(LogLevel.WARNING, WARNING_MSG))
  }

  companion object {
    private const val FATAL_MSG = "fatal"
    private const val ERROR_MSG = "error"
    private const val WARNING_MSG = "warning"
    private const val CATEGORY_KEY = "categoryKey"
  }
}
