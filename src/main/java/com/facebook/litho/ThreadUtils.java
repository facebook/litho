/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.os.Looper;

import com.facebook.litho.config.ComponentsConfiguration;

/**
 * Thread assertion utilities.
 */
public class ThreadUtils {
  private ThreadUtils() {
  }

  public static boolean isMainThread() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  public static void assertMainThread() {
    if (!ComponentsConfiguration.IS_INTERNAL_BUILD || ComponentsConfiguration.isEndToEndTestRun) {
      return;
    } else if (!isMainThread()) {
      throw new IllegalStateException("This should run on the main thread.");
    }
  }

  public static void assertHoldsLock(Object lock) {
    if (!ComponentsConfiguration.IS_INTERNAL_BUILD) {
      return;
    }

    if (!Thread.holdsLock(lock)) {
      throw new IllegalStateException("This method should be called while holding the lock");
    }
  }

  public static void assertDoesntHoldLock(Object lock) {
    if (!ComponentsConfiguration.IS_INTERNAL_BUILD) {
      return;
    }

    if (Thread.holdsLock(lock)) {
      throw new IllegalStateException("This method should be called outside the lock.");
    }
  }
}
