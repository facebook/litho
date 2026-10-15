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
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ThreadUtils.assertMainThread] used to no-op for the whole of an end-to-end test run, which meant
 * a main-thread violation reached by a test, for example reading `Ref.value` while Litho resolves a
 * component on a layout thread, was silently swallowed instead of crashing. The bypass is now
 * scoped to callers that cannot satisfy the assertion: runs with no real main Looper, and
 * instrumentation test bodies. Robolectric supplies a main Looper, so these tests exercise the
 * on-device path.
 */
@RunWith(LithoTestRunner::class)
class ThreadUtilsAssertMainThreadTest {

  private var previousIsEndToEndTestRun: Boolean = false

  @Before
  fun setUp() {
    // A mutable process global; leaving it flipped pollutes any later test that shares this class
    // loader.
    previousIsEndToEndTestRun = ComponentsConfiguration.isEndToEndTestRun

    // Normalize rather than just save. The override only gets reset on the way out, and inheriting
    // it would make the bypass cases pass vacuously and the throwing cases fail.
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED)
  }

  @After
  fun tearDown() {
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_DISABLED)
    ComponentsConfiguration.isEndToEndTestRun = previousIsEndToEndTestRun
  }

  @Test
  fun `assertMainThread throws off the main thread during an end-to-end test run`() {
    ComponentsConfiguration.isEndToEndTestRun = true
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE)

    assertThatThrownBy { ThreadUtils.assertMainThread() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("This must run on the main thread")
  }

  @Test
  fun `reading Ref value off the main thread during an end-to-end test run throws`() {
    ComponentsConfiguration.isEndToEndTestRun = true
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_FALSE)

    val ref = Ref(false)

    assertThatThrownBy { ref.value }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `assertMainThread does not throw on the main thread`() {
    ComponentsConfiguration.isEndToEndTestRun = true
    ThreadUtils.setMainThreadOverride(ThreadUtils.OVERRIDE_MAIN_THREAD_TRUE)

    assertThat(ThreadUtils.isMainThread).isTrue
    assertThatCode { ThreadUtils.assertMainThread() }.doesNotThrowAnyException()
  }

  @Test
  fun `instrumentation runner thread is exempt`() {
    // Instrumentation test bodies call Litho directly from the runner's thread. Arming the
    // assertion for them broke six on-device suites, so that thread stays exempt.
    ComponentsConfiguration.isEndToEndTestRun = true

    assertThat(assertMainThreadOnThreadNamed("Instr: com.example.FakeTestRunner")).isNull()
  }

  @Test
  fun `an ordinary background thread is not exempt`() {
    // Guards the exemption above from widening: only the instrumentation runner is excused, a
    // normal background thread, such as Litho's layout thread, still fails.
    ComponentsConfiguration.isEndToEndTestRun = true

    assertThat(assertMainThreadOnThreadNamed("ComponentLayoutThread"))
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("This must run on the main thread")
  }

  /**
   * Runs [ThreadUtils.assertMainThread] on a real thread with [name] and returns what it threw, or
   * null, so the thread-name-based exemption can be exercised directly. The main-thread override is
   * deliberately not used: it short-circuits [ThreadUtils.isMainThread] and would skip the branch
   * under test.
   *
   * Completion is asserted rather than assumed. A bare `join` with a timeout would let a thread
   * that never ran look identical to one that ran and threw nothing, which would make the exemption
   * test pass without exercising anything.
   */
  private fun assertMainThreadOnThreadNamed(name: String): Throwable? {
    val thrown = AtomicReference<Throwable?>()
    val finished = CountDownLatch(1)
    val thread = Thread(
        {
          try {
            ThreadUtils.assertMainThread()
          } catch (t: Throwable) {
            thrown.set(t)
          } finally {
            finished.countDown()
          }
        },
        name,
    )
    thread.start()

    assertThat(finished.await(10, TimeUnit.SECONDS))
        .describedAs("thread '%s' did not finish", name)
        .isTrue

    return thrown.get()
  }
}
