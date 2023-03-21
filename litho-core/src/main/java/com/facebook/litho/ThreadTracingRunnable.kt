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

import android.util.Log
import com.facebook.litho.config.ComponentsConfiguration
import java.lang.StringBuilder

/**
 * ThreadTracingRunnable tries to help debugging crashes happening across threads showing the
 * stacktrace of the crash origin which scheduled this runnable.
 *
 * A stacktrace, together with the origin thread name, is going to be "saved" at the time this class
 * is instantiated and appended as "cause" to whatever is thrown from the [run()] method.
 *
 * To use this class, just extends it instead of implementing [Runnable]. Then just implement
 * [tracedRun()].
 *
 * If the runnable is created ahead of time and used somewhere else later, a new, more relevant,
 * stacktrace can be created calling [resetTrace()].
 */
abstract class ThreadTracingRunnable : Runnable {

  private val isTracingEnabled = ComponentsConfiguration.enableThreadTracingStacktrace

  // This Throwable is saving the call stack which created this Runnable. If null, we are not
  // tracing in this instance.
  private var tracingThrowable: Throwable? =
      if (isTracingEnabled) {
        val thread = Thread.currentThread()
        Throwable(
            StringBuilder(MESSAGE_PART_1)
                .append(thread.id)
                .append(MESSAGE_PART_2)
                .append(thread.name)
                .toString())
      } else {
        null
      }

  /** Implement here your [Runnable#run()] code. */
  abstract fun tracedRun()

  /**
   * Reset the stacktrace of this Runnable to this point. To be called right before the runnable is
   * scheduled to another thread, in case it was instantiated ahead of time with a different code
   * flow.
   */
  fun resetTrace() {
    tracingThrowable?.fillInStackTrace()
  }

  override fun run() {
    try {
      tracedRun()
    } catch (t: Throwable) {
      if (tracingThrowable != null) {
        Log.w("LithoThreadTracing", "--- start debug trace")
        Log.w("LithoThreadTracing", "Thread tracing stacktrace", tracingThrowable)
        Log.w("LithoThreadTracing", "--- end debug trace")
      }
      throw t
    }
  }

  companion object {
    private const val MESSAGE_PART_1 = "Runnable instantiated on thread id: "
    private const val MESSAGE_PART_2 = ", name: "
  }
}
