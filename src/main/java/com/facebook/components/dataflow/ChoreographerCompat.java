/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.dataflow;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

/**
 * Wrapper class for abstracting away availability of the JellyBean Choreographer. If Choreographer
 * is unavailable we fallback to using a normal Handler.
 *
 * This code was taken from the facebook/rebound repository.
 */
public class ChoreographerCompat {

  private static final long ONE_FRAME_MILLIS = 17;
  private static final boolean IS_JELLYBEAN_OR_HIGHER =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
  private static final ChoreographerCompat INSTANCE = new ChoreographerCompat();

  private Handler mHandler;
  private Choreographer mChoreographer;

  public static ChoreographerCompat getInstance() {
    return INSTANCE;
  }

  private ChoreographerCompat() {
    if (IS_JELLYBEAN_OR_HIGHER) {
      mChoreographer = getChoreographer();
    } else {
      mHandler = new Handler(Looper.getMainLooper());
    }
  }

  public void postFrameCallback(FrameCallback callbackWrapper) {
    if (IS_JELLYBEAN_OR_HIGHER) {
      choreographerPostFrameCallback(callbackWrapper.getFrameCallback());
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), 0);
    }
  }

  public void postFrameCallbackDelayed(FrameCallback callbackWrapper, long delayMillis) {
    if (IS_JELLYBEAN_OR_HIGHER) {
      choreographerPostFrameCallbackDelayed(callbackWrapper.getFrameCallback(), delayMillis);
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), delayMillis + ONE_FRAME_MILLIS);
    }
  }

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
      Choreographer.FrameCallback frameCallback,
      long delayMillis) {
    mChoreographer.postFrameCallbackDelayed(frameCallback, delayMillis);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void choreographerRemoveFrameCallback(Choreographer.FrameCallback frameCallback) {
    mChoreographer.removeFrameCallback(frameCallback);
  }

  /**
   * This class provides a compatibility wrapper around the JellyBean FrameCallback with methods
   * to access cached wrappers for submitting a real FrameCallback to a Choreographer or a Runnable
   * to a Handler.
   */
  public static abstract class FrameCallback {

    private Runnable mRunnable;
    private Choreographer.FrameCallback mFrameCallback;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    Choreographer.FrameCallback getFrameCallback() {
      if (mFrameCallback == null) {
        mFrameCallback = new Choreographer.FrameCallback() {
          @Override
          public void doFrame(long frameTimeNanos) {
            FrameCallback.this.doFrame(frameTimeNanos);
          }
        };
      }
      return mFrameCallback;
    }

    Runnable getRunnable() {
      if (mRunnable == null) {
        mRunnable = new Runnable() {
          @Override
          public void run() {
            doFrame(System.nanoTime());
          }
        };
      }
      return mRunnable;
    }

    public abstract void doFrame(long frameTimeNanos);
  }
}
