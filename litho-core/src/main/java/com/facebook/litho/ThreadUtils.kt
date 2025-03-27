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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LithoDebugConfigurations

/** Thread assertion utilities. */
object ThreadUtils {

  const val OVERRIDE_DISABLED: Int = 0
  const val OVERRIDE_MAIN_THREAD_TRUE: Int = 1
  const val OVERRIDE_MAIN_THREAD_FALSE: Int = 2
  @MainThreadOverride private var mainThreadOverride = OVERRIDE_DISABLED
  // Defaults to -1 for cases where the Android SDK is stubbed out. E.g when running unit tests.
  private val mainThreadId = runCatching { Looper.getMainLooper().thread.id }.getOrDefault(-1)

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

  @JvmStatic
  fun assertMainThread() {
    if (ComponentsConfiguration.isEndToEndTestRun) {
      return
    }
    check(isMainThread) {
      ("This must run on the main thread; but is running on ${Thread.currentThread().name}")
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
   * Try to raise the priority of {@param threadId} to the priority of the calling thread
   *
   * @return the original thread priority of the target thread.
   */
  @JvmStatic
  fun tryInheritThreadPriorityFromCurrentThread(threadId: Int): Int =
      tryRaiseThreadPriority(threadId, Process.getThreadPriority(Process.myTid()))

  /**
   * Try to raise the priority of {@param threadId} to {@param targetThreadPriority}.
   *
   * @return the original thread priority of the target thread.
   */
  @JvmStatic
  fun tryRaiseThreadPriority(threadId: Int, targetThreadPriority: Int): Int {
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
    return originalThreadPriority
  }

  @IntDef(OVERRIDE_DISABLED, OVERRIDE_MAIN_THREAD_FALSE, OVERRIDE_MAIN_THREAD_TRUE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class MainThreadOverride
}
