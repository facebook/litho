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

package com.facebook.litho.testing;

import junit.framework.AssertionFailedError;

public class ThreadTestingUtils {

  /**
   * Runs a given runnable on a background thread
   *
   * @param runnable The runnable to run
   * @return a TimeOutSemaphore (0 permits) that is released when the action is completed. If an
   *     exception occurs within the supplied runnable, it is set on the TimeOutSemaphore
   */
  public static TimeOutSemaphore runOnBackgroundThread(final Runnable runnable) {
    final TimeOutSemaphore latch = new TimeOutSemaphore(0);
    runOnBackgroundThread(latch, runnable);
    return latch;
  }

  /**
   * Runs a given runnable on a background thread
   *
   * @param latch TimeOutSemaphore that is released when the runnable finishes
   * @param runnable The runnable to run
   */
  public static void runOnBackgroundThread(final TimeOutSemaphore latch, final Runnable runnable) {
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  runnable.run();
                } catch (Exception | AssertionFailedError e) {
                  latch.setException(e);
                }
                latch.release();
              }
            })
        .start();
  }

  /**
   * Ignore the InterruptedException if that happens, instead of throwing.
   *
   * <p>Example:
   *
   * <pre><code>
   *   failSilentlyIfInterrupted(() ->
   *      // execute method that may throw InterruptedException
   *   );
   * </code></pre>
   */
  public static void failSilentlyIfInterrupted(final RunnableWithInterruptedException runnable) {
    try {
      runnable.run();
    } catch (InterruptedException ignore) {
      // no ops
    }
  }

  /**
   * Fail with RuntimeException instead of InterruptedException if that happens.
   *
   * <p>Example:
   *
   * <pre><code>
   *   failIfInterrupted(() ->
   *      // execute method that may throw InterruptedException
   *   );
   * </code></pre>
   */
  public static void failIfInterrupted(final RunnableWithInterruptedException runnable) {
    try {
      runnable.run();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  public interface RunnableWithInterruptedException {
    void run() throws InterruptedException;
  }
}
