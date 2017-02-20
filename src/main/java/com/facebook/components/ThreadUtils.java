// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.os.Looper;

import com.facebook.components.config.ComponentsConfiguration;

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
