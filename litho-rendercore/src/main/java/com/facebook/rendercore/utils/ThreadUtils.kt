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

package com.facebook.rendercore.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import com.facebook.rendercore.RenderCoreConfig
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/** Thread assertion utilities. */
object ThreadUtils {

  private const val THREAD_NAME = "ThreadUtilsBackgroundHandler"
  private const val DEFAULT_BACKGROUND_THREAD_PRIORITY = 5

  private val uiThreadHandler: Handler by
      lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Handler(Looper.getMainLooper()) }

  private val defaultBackgroundThreadHandler: Handler by
      lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Handler(
            HandlerThread(THREAD_NAME, DEFAULT_BACKGROUND_THREAD_PRIORITY)
                .also { it.start() }
                .looper)
      }

  @JvmStatic
  @JvmOverloads
  fun assertMainThread(message: String? = null) {
    if (RenderCoreConfig.isEndToEndTestRun) {
      return
    }
    if (!isMainThread) {
      throw IllegalStateException(
          message
              ?: "This must run on the main thread; but is running on ${Thread.currentThread().name}")
    }
  }

  @get:JvmStatic
  val isMainThread: Boolean
    get() = Looper.getMainLooper().thread === Thread.currentThread()

  @JvmStatic
  fun <T> getResultInheritingPriority(future: Future<T>, runningThreadId: Int): T {
    val originalThreadPriority: Int
    val didRaiseThreadPriority: Boolean
    val notRunningOnMyThread = runningThreadId != Process.myTid()
    val shouldWaitForResult = !future.isDone && notRunningOnMyThread
    if (isMainThread && shouldWaitForResult) {
      // Main thread is about to be blocked, raise the running thread priority.
      originalThreadPriority = tryInheritThreadPriorityFromCurrentThread(runningThreadId)
      didRaiseThreadPriority = true
    } else {
      originalThreadPriority = Process.THREAD_PRIORITY_DEFAULT
      didRaiseThreadPriority = false
    }
    return try {
      future.get()
    } catch (e: ExecutionException) {
      val cause = e.cause
      throw if (cause is RuntimeException) cause else RuntimeException(e.message, e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e.message, e)
    } catch (e: CancellationException) {
      throw RuntimeException(e.message, e)
    } finally {
      if (didRaiseThreadPriority) {
        // Reset the running thread's priority after we're unblocked.
        try {
          Process.setThreadPriority(runningThreadId, originalThreadPriority)
        } catch (e: IllegalArgumentException) {
          throw RuntimeException(
              "Unable to restore priority: $runningThreadId, $originalThreadPriority", e)
        } catch (e: SecurityException) {
          throw RuntimeException(
              "Unable to restore priority: $runningThreadId, $originalThreadPriority", e)
        }
      }
    }
  }

  /**
   * Try to raise the priority of [threadId] to the priority of the calling thread
   *
   * @return the original thread priority of the target thread.
   */
  @JvmStatic
  fun tryInheritThreadPriorityFromCurrentThread(threadId: Int): Int =
      tryRaiseThreadPriority(threadId, Process.getThreadPriority(Process.myTid()))

  /**
   * Try to raise the priority of [threadId] to [targetThreadPriority].
   *
   * @return the original thread priority of the target thread.
   */
  @JvmStatic
  fun tryRaiseThreadPriority(threadId: Int, targetThreadPriority: Int): Int {
    // Main thread is about to be blocked, raise the running thread priority.
    val originalThreadPriority = Process.getThreadPriority(threadId)
    var success = false
    var currentTargetThreadPriority = targetThreadPriority
    while (!success && targetThreadPriority < originalThreadPriority) {
      // Keep trying to increase thread priority of running thread as long as it is an increase.
      try {
        Process.setThreadPriority(threadId, currentTargetThreadPriority)
        success = true
      } catch (e: SecurityException) {
        /*
         From [Process#THREAD_PRIORITY_DISPLAY], some applications can not change
         the thread priority to that of the main thread. This catches that potential error
         and tries to set a lower priority.
        */
        currentTargetThreadPriority += Process.THREAD_PRIORITY_LESS_FAVORABLE
      }
    }
    return originalThreadPriority
  }

  @JvmStatic
  fun runOnUiThread(runnable: Runnable) {
    if (isMainThread) {
      runnable.run()
    } else {
      uiThreadHandler.post(runnable)
    }
  }

  @JvmStatic
  fun postOnUiThread(runnable: Runnable) {
    uiThreadHandler.post(runnable)
  }

  @JvmStatic
  fun runOnBackgroundThread(runnable: Runnable) {
    defaultBackgroundThreadHandler.post(runnable)
  }
}
