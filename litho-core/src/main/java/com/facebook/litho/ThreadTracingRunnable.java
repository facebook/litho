/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import android.util.Log;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.config.ComponentsConfiguration;

/**
 * ThreadTracingRunnable tries to help debugging crashes happening across threads showing the
 * stacktrace of the crash origin which scheduled this runnable.
 *
 * <p>A stacktrace, together with the origin thread name, is going to be "saved" at the time this
 * class is instantiated and appended as "cause" to whatever is thrown from the {@link #run()}
 * method.
 *
 * <p>To use this class, just extends it instead of implementing {@link Runnable}. Then just
 * implement {@link #tracedRun(ThreadTracingRunnable)}.
 *
 * <p>If the runnable is created ahead of time and used somewhere else later, a new, more relevant,
 * stacktrace can be created calling {@link #resetTrace()}.
 *
 * <p>If you need to chain another Runnable from your run method, you can pass the Throwable
 * parameter from {@link #tracedRun(ThreadTracingRunnable)} to the constructor {@link
 * ThreadTracingRunnable#tracedRun(ThreadTracingRunnable)}. Therefore, the crash can now be tracked
 * across boundaries of multiple threads.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class ThreadTracingRunnable implements Runnable {

  private static final String MESSAGE_PART_1 = "Runnable instantiated on thread id: ";
  private static final String MESSAGE_PART_2 = ", name: ";

  // If null, we are not tracing in this instance.
  private final @Nullable Throwable mTracingThrowable;

  private ThreadTracingRunnable(boolean isTracingEnabled) {
    // This Throwable is saving the call stack which created this Runnable.
    if (isTracingEnabled) {
      final Thread thread = Thread.currentThread();
      mTracingThrowable =
          new Throwable(
              new StringBuilder(MESSAGE_PART_1)
                  .append(thread.getId())
                  .append(MESSAGE_PART_2)
                  .append(thread.getName())
                  .toString());
    } else {
      mTracingThrowable = null;
    }
  }

  public ThreadTracingRunnable() {
    this(ComponentsConfiguration.enableThreadTracingStacktrace /* isTracingEnabled */);
  }

  /**
   * If you are chaining multiple Runnable together that are going to schedule each other within
   * their run() method, use this constructor to track their stacktraces across threads. The
   * required parameter here is the argument coming from {@link #tracedRun(ThreadTracingRunnable)}.
   */
  public ThreadTracingRunnable(@Nullable ThreadTracingRunnable prevTracingRunnable) {
    // If there's a previous ThreadTracingRunnable, use its "isTracingEnable" setting to decide if
    // enabling tracing for this instance too or not.
    // If the previous ThreadTracingRunnable is null, then check the ComponentsConfiguration to
    // enable the tracing.
    this(
        prevTracingRunnable != null
            ? prevTracingRunnable.mTracingThrowable != null
            : ComponentsConfiguration.enableThreadTracingStacktrace);

    if (mTracingThrowable != null && prevTracingRunnable != null) {
      mTracingThrowable.initCause(prevTracingRunnable.mTracingThrowable);
    }
  }

  /**
   * Implement here your {@link Runnable#run()} code.
   *
   * @param prevTracingRunnable If this ThreadTracingRunnable schedule another ThreadTracingRunnable
   *     to another thread, pass this parameter to the new ThreadTracingRunnable class with {@link
   *     #ThreadTracingRunnable(ThreadTracingRunnable)}.
   */
  public abstract void tracedRun(ThreadTracingRunnable prevTracingRunnable);

  /**
   * Reset the stacktrace of this Runnable to this point. To be called right before the runnable is
   * scheduled to another thread, in case it was instantiated ahead of time with a different code
   * flow.
   */
  public void resetTrace() {
    if (mTracingThrowable != null) {
      mTracingThrowable.fillInStackTrace();
    }
  }

  @Override
  public final void run() {
    try {
      tracedRun(this);
    } catch (Throwable t) {
      if (mTracingThrowable != null) {
        Log.w("LithoThreadTracing", "--- start debug trace");
        Log.w("LithoThreadTracing", "Thread tracing stacktrace", mTracingThrowable);
        Log.w("LithoThreadTracing", "--- end debug trace");
      }
      throw t;
    }
  }
}
