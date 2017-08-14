/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.view.Choreographer;

/**
 * Wrapper class for abstracting away availability of the JellyBean Choreographer. If Choreographer
 * is unavailable we fallback to using a normal Handler.
 *
 * <p>This code was taken from the facebook/rebound repository.
 */
public class ChoreographerCompatImpl implements ChoreographerCompat {

  private static final long ONE_FRAME_MILLIS = 17;
  private static final boolean IS_JELLYBEAN_OR_HIGHER =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
  private static ChoreographerCompat sInstance;

  private Handler mHandler;
  private Choreographer mChoreographer;

  /**
   * @return a {@link ChoreographerCompat} instance. This should really be on {@link
   *     ChoreographerCompat} but interfaces don't support static methods until Java 8.
   */
  public static ChoreographerCompat getInstance() {
    if (sInstance == null) {
      sInstance = new ChoreographerCompatImpl();
    }
    return sInstance;
  }

  /** Set the ChoreographerCompat instance for tests. */
  @VisibleForTesting
  public static void setInstance(ChoreographerCompat choreographerCompat) {
    sInstance = choreographerCompat;
  }

  private ChoreographerCompatImpl() {
    if (IS_JELLYBEAN_OR_HIGHER) {
      mChoreographer = getChoreographer();
    } else {
      mHandler = new Handler(Looper.getMainLooper());
    }
  }

  @Override
  public void postFrameCallback(FrameCallback callbackWrapper) {
    if (IS_JELLYBEAN_OR_HIGHER) {
      choreographerPostFrameCallback(callbackWrapper.getFrameCallback());
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), 0);
    }
  }

  @Override
  public void postFrameCallbackDelayed(FrameCallback callbackWrapper, long delayMillis) {
    if (IS_JELLYBEAN_OR_HIGHER) {
      choreographerPostFrameCallbackDelayed(callbackWrapper.getFrameCallback(), delayMillis);
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), delayMillis + ONE_FRAME_MILLIS);
    }
  }

  @Override
  public void removeFrameCallback(FrameCallback callbackWrapper) {
    if (IS_JELLYBEAN_OR_HIGHER) {
      choreographerRemoveFrameCallback(callbackWrapper.getFrameCallback());
    } else {
      mHandler.removeCallbacks(callbackWrapper.getRunnable());
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private Choreographer getChoreographer() {
    return Choreographer.getInstance();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void choreographerPostFrameCallback(Choreographer.FrameCallback frameCallback) {
    mChoreographer.postFrameCallback(frameCallback);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void choreographerPostFrameCallbackDelayed(
      Choreographer.FrameCallback frameCallback, long delayMillis) {
    mChoreographer.postFrameCallbackDelayed(frameCallback, delayMillis);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void choreographerRemoveFrameCallback(Choreographer.FrameCallback frameCallback) {
    mChoreographer.removeFrameCallback(frameCallback);
  }
}
