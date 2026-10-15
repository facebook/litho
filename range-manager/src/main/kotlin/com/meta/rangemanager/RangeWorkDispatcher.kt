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

package com.meta.rangemanager

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Dispatches range work callbacks serially.
 *
 * Production dispatchers invoke callbacks one at a time and never reentrantly. [removePending]
 * removes work that has not started, but cannot revoke work that has already started or been
 * dequeued for execution.
 */
internal interface RangeWorkDispatcher {
  /**
   * Queues [work] and returns whether it will run. False after [quit], or when the work could not
   * be handed to a thread; it is then dropped and will not run, so the caller decides what to do.
   */
  fun post(work: () -> Unit): Boolean

  fun removePending()

  fun quit()
}

internal class HandlerThreadRangeWorkDispatcher(
    private val threadName: String,
    private val failureReporter: (Exception) -> Unit = logRangeWorkFailure,
) : RangeWorkDispatcher {
  private val lock = Any()
  private var handler: Handler? = null
  private var thread: HandlerThread? = null
  private var isQuit = false

  override fun post(work: () -> Unit): Boolean {
    synchronized(lock) {
      if (isQuit) {
        return false
      }
      val target = handler ?: createHandlerLocked().also { handler = it }
      return target.post {
        try {
          work()
        } catch (exception: Exception) {
          failureReporter(exception)
        }
      }
    }
  }

  override fun removePending() {
    synchronized(lock) { handler?.removeCallbacksAndMessages(null) }
  }

  override fun quit() {
    val target =
        synchronized(lock) {
          if (isQuit) {
            return
          }
          isQuit = true
          handler?.removeCallbacksAndMessages(null)
          handler = null
          thread.also { thread = null }
        }
    target?.quitSafely()
  }

  private fun createHandlerLocked(): Handler {
    val newThread = HandlerThread("$threadName:${THREAD_ID.getAndIncrement()}")
    newThread.start()
    thread = newThread
    return Handler(newThread.looper)
  }

  private companion object {
    private val THREAD_ID = AtomicInteger(0)
  }
}

/** Where a dispatcher sends a failed job's exception when the app gives it no reporter. */
internal val logRangeWorkFailure: (Exception) -> Unit = { exception ->
  Log.e("RangeWorkDispatcher", "Range work callback failed", exception)
}
