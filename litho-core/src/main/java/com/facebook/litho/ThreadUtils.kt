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

import android.os.Looper
import android.os.Process
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.debug.DebugInfoReporter

/** Thread assertion utilities. */
object ThreadUtils {

  const val OVERRIDE_DISABLED: Int = 0
  const val OVERRIDE_MAIN_THREAD_TRUE: Int = 1
  const val OVERRIDE_MAIN_THREAD_FALSE: Int = 2
  @MainThreadOverride private var mainThreadOverride = OVERRIDE_DISABLED
  // Defaults to -1 for cases where the Android SDK is stubbed out. E.g when running unit tests.
  private val mainThreadId = runCatching { Looper.getMainLooper().thread.id }.getOrDefault(-1)

  /**
   * False only when the Android SDK is stubbed out and [mainThreadId] could not be resolved, which
   * makes [isMainThread] permanently false. On a real device or emulator the main Looper exists and
   * [isMainThread] is accurate.
   */
  private val hasRealMainLooper: Boolean
    get() = mainThreadId != -1L

  /**
   * Instrumentation test bodies run on the runner's own thread and drive Litho APIs directly from
   * it. Android names that thread `Instr: <runner class>` (ActivityThread.InstrumentationThread),
   * which is the only signal available to tell the test apart from the app.
   *
   * Note the breadth: this exempts everything reached synchronously from the runner thread, product
   * code included, so an on-device test that drives a main-thread-confined API directly still
   * cannot surface a violation in it. Work the app does on its own threads, layout threads among
   * them, is unaffected. The match is on thread name, so any other thread carrying this prefix is
   * exempt too.
   */
  private val isInstrumentationThread: Boolean
    get() = Thread.currentThread().name.startsWith(INSTRUMENTATION_THREAD_PREFIX)

  @JvmStatic
  @VisibleForTesting
  fun setMainThreadOverride(@MainThreadOverride override: Int) {
    mainThreadOverride = override
  }

  @get:JvmStatic
  val isMainThread: Boolean
    get() =
        when (mainThreadOverride) {
          OVERRIDE_MAIN_THREAD_FALSE -> false
          OVERRIDE_MAIN_THREAD_TRUE -> true
          else -> mainThreadId == Thread.currentThread().id
        }

  @get:JvmStatic
  val isLayoutThread: Boolean
    get() = Thread.currentThread().name.startsWith(ComponentTree.DEFAULT_LAYOUT_THREAD_NAME)

  @get:JvmStatic
  val isDefaultLayoutThread: Boolean
    get() = Thread.currentThread().name == ComponentTree.DEFAULT_LAYOUT_THREAD_NAME

  /**
   * The test bypass used to cover the whole of an end-to-end run, which hid main-thread violations
   * that crash in production, such as reading a [Ref] value during background layout. It is now
   * scoped to the two callers that cannot satisfy the assertion: runs with no real main Looper (see
   * [hasRealMainLooper]), and instrumentation test bodies, which drive Litho from the runner's own
   * thread (see [isInstrumentationThread]).
   *
   * Everything else, jest-e2e included, is held to the production contract.
   */
  @JvmStatic
  fun assertMainThread() {
    if (!hasRealMainLooper || isInstrumentationThread) {
      return
    }
    check(isMainThread) {
      "This must run on the main thread; but is running on ${Thread.currentThread().name}"
    }
  }

  @JvmStatic
  fun assertHoldsLock(lock: Any) {
    if (!LithoDebugConfigurations.isDebugModeEnabled) {
      return
    }
    check(Thread.holdsLock(lock)) { "This method should be called while holding the lock" }
  }

  @JvmStatic
  fun assertDoesntHoldLock(lock: Any) {
    if (!LithoDebugConfigurations.isDebugModeEnabled) {
      return
    }
    check(!Thread.holdsLock(lock)) { "This method should be called outside the lock." }
  }

  /**
   * Try to raise the priority of {@param threadId} to {@param targetThreadPriority}.
   *
   * @return a Pair containing the original thread priority and the final thread priority that was
   *   set.
   */
  @JvmStatic
  fun tryRaiseThreadPriority(threadId: Int, targetThreadPriority: Int): Pair<Int, Int> {
    // Main thread is about to be blocked, raise the running thread priority.
    var threadPriority = targetThreadPriority
    val originalThreadPriority = Process.getThreadPriority(threadId)
    var success = false
    while (!success && threadPriority < originalThreadPriority) {
      // Keep trying to increase thread priority of running thread as long as it is an increase.
      try {
        Process.setThreadPriority(threadId, threadPriority)
        success = true
      } catch (e: SecurityException) {
        /*
         From [Process#THREAD_PRIORITY_DISPLAY], some applications can not change
         the thread priority to that of the main thread. This catches that potential error
         and tries to set a lower priority.
        */
        threadPriority += Process.THREAD_PRIORITY_LESS_FAVORABLE
      }
    }
    if (threadPriority == originalThreadPriority) {
      // We were unable to raise the priority of the thread at all.
      DebugInfoReporter.report(category = "ThreadUtilsPriorityInversion") {
        this["event"] = "UnableToRaise"
        this["originalThreadPriority"] = "$originalThreadPriority"
        this["targetThreadPriority"] = "$targetThreadPriority"
      }
    } else if (threadPriority > targetThreadPriority) {
      // We were able to raise the priority but not at the desired level.
      DebugInfoReporter.report(category = "ThreadUtilsPriorityInversion") {
        this["event"] = "RaisedButNotAtDesiredLevel"
        this["originalThreadPriority"] = "$originalThreadPriority"
        this["targetThreadPriority"] = "$targetThreadPriority"
        this["threadPriority"] = "$threadPriority"
      }
    }
    return Pair(originalThreadPriority, threadPriority)
  }

  @IntDef(OVERRIDE_DISABLED, OVERRIDE_MAIN_THREAD_FALSE, OVERRIDE_MAIN_THREAD_TRUE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class MainThreadOverride

  /** Set by `ActivityThread.InstrumentationThread`; see [isInstrumentationThread]. */
  private const val INSTRUMENTATION_THREAD_PREFIX = "Instr: "
}
