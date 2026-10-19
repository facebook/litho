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

package com.facebook.rendercore.thread.utils

import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreadUtilsAssertMainThreadTest {

  private var previousIsEndToEndTestRun = false

  @Before
  fun setUp() {
    previousIsEndToEndTestRun = RenderCoreThreadUtilsConfig.isEndToEndTestRun
  }

  @After
  fun tearDown() {
    RenderCoreThreadUtilsConfig.isEndToEndTestRun = previousIsEndToEndTestRun
  }

  @Test
  fun `assertMainThread throws off the main thread during an end-to-end run`() {
    RenderCoreThreadUtilsConfig.isEndToEndTestRun = true

    val thrown = assertOnThread("LayoutHandlerPoolThread1")

    assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
    assertThat(thrown).hasMessageContaining("LayoutHandlerPoolThread1")
  }

  @Test
  fun `assertMainThread throws off the main thread outside an end-to-end run`() {
    RenderCoreThreadUtilsConfig.isEndToEndTestRun = false

    assertThat(assertOnThread("LayoutHandlerPoolThread1"))
        .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `assertMainThread does not throw on the main thread during an end-to-end run`() {
    RenderCoreThreadUtilsConfig.isEndToEndTestRun = true

    assertThat(runCatching { ThreadUtils.assertMainThread() }.exceptionOrNull()).isNull()
  }

  @Test
  fun `assertMainThread throws on the instrumentation runner thread in both end-to-end states`() {
    RenderCoreThreadUtilsConfig.isEndToEndTestRun = true
    assertThat(assertOnThread("Instr: androidx.test.runner.AndroidJUnitRunner"))
        .isInstanceOf(IllegalStateException::class.java)

    RenderCoreThreadUtilsConfig.isEndToEndTestRun = false
    assertThat(assertOnThread("Instr: androidx.test.runner.AndroidJUnitRunner"))
        .isInstanceOf(IllegalStateException::class.java)
  }

  private fun assertOnThread(threadName: String): Throwable? {
    val thrown = AtomicReference<Throwable?>(null)
    val thread = Thread(
        {
          try {
            ThreadUtils.assertMainThread()
          } catch (t: Throwable) {
            thrown.set(t)
          }
        },
        threadName,
    )
    thread.start()
    thread.join()
    return thrown.get()
  }
}
