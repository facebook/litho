/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import com.facebook.litho.config.ComponentsConfiguration;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Thread assertion utilities.
 */
public class ThreadUtils {
  public static final int OVERRIDE_DISABLED = 0;
  public static final int OVERRIDE_MAIN_THREAD_TRUE = 1;
  public static final int OVERRIDE_MAIN_THREAD_FALSE = 2;

  @IntDef({OVERRIDE_DISABLED, OVERRIDE_MAIN_THREAD_FALSE, OVERRIDE_MAIN_THREAD_TRUE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface MainThreadOverride {}

  @MainThreadOverride
  private static int sMainThreadOverride = OVERRIDE_DISABLED;

  private ThreadUtils() {
  }

  @VisibleForTesting
  public static void setMainThreadOverride(@MainThreadOverride int override) {
    sMainThreadOverride = override;
  }

  public static boolean isMainThread() {
    switch (sMainThreadOverride) {
      case OVERRIDE_MAIN_THREAD_FALSE:
        return false;
      case OVERRIDE_MAIN_THREAD_TRUE:
        return true;
      default:
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
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
