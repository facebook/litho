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

package com.facebook.litho.choreographercompat;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.WorkContinuationInstrumenter;
import javax.annotation.Nullable;

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
  private static ChoreographerCompat sInstance = new ChoreographerCompatImpl();

  /**
   * @return a {@link ChoreographerCompat} instance for the main thread. This should really be on
   *     {@link ChoreographerCompat} but interfaces don't support static methods until Java 8.
   *     <p>NB: Unlike android.view.Choreographer#getInstance(), this method ALWAYS returns an
   *     instance that posts to the MAIN THREAD android Choreographer (and main thread Handler for
   *     API<16).
   */
  public static ChoreographerCompat getInstance() {
    return sInstance;
  }

  /** Set the ChoreographerCompat instance for tests. */
  @VisibleForTesting
  public static void setInstance(ChoreographerCompat choreographerCompat) {
    sInstance = choreographerCompat;
  }

  private final @Nullable Handler mHandler;
  private Choreographer mChoreographer;

  @VisibleForTesting
  ChoreographerCompatImpl() {
    if (IS_JELLYBEAN_OR_HIGHER) {
      if (ThreadUtils.isMainThread()) {
        mChoreographer = getChoreographer();
        mHandler = null;
      } else {
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(
            new Runnable() {
              @Override
              public void run() {
                mChoreographer = getChoreographer();
              }
            });
      }
    } else {
      mHandler = new Handler(Looper.getMainLooper());
    }
  }

  @Override
  public void postFrameCallback(FrameCallback callbackWrapper) {
    callbackWrapper.mTokenReference.set(
        WorkContinuationInstrumenter.onOfferWorkForContinuation(
            "ChoreographerCompat_postFrameCallback"));

    if (IS_JELLYBEAN_OR_HIGHER && mChoreographer != null) {
      choreographerPostFrameCallback(callbackWrapper.getFrameCallback());
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), 0);
    }
  }

  @Override
  public void postFrameCallbackDelayed(FrameCallback callbackWrapper, long delayMillis) {
    callbackWrapper.mTokenReference.set(
        WorkContinuationInstrumenter.onOfferWorkForContinuation(
            "ChoreographerCompat_postFrameCallbackDelayed"));

    if (IS_JELLYBEAN_OR_HIGHER && mChoreographer != null) {
      choreographerPostFrameCallbackDelayed(callbackWrapper.getFrameCallback(), delayMillis);
    } else {
      mHandler.postDelayed(callbackWrapper.getRunnable(), delayMillis + ONE_FRAME_MILLIS);
    }
  }

  @Override
  public void removeFrameCallback(FrameCallback callbackWrapper) {
    callbackWrapper.mTokenReference.set(null);

    if (IS_JELLYBEAN_OR_HIGHER && mChoreographer != null) {
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

  @VisibleForTesting
  boolean isUsingChoreographer() {
    return mChoreographer != null;
  }
}
