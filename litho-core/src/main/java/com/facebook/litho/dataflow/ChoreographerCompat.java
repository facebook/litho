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
import android.view.Choreographer;

/** See {@link ChoreographerCompatImpl}. Interface exists for mocking out in tests. */
public interface ChoreographerCompat {

  void postFrameCallback(FrameCallback callbackWrapper);

  void postFrameCallbackDelayed(FrameCallback callbackWrapper, long delayMillis);

  void removeFrameCallback(FrameCallback callbackWrapper);

  /**
   * This class provides a compatibility wrapper around the JellyBean FrameCallback with methods to
   * access cached wrappers for submitting a real FrameCallback to a Choreographer or a Runnable to
   * a Handler.
   */
  abstract class FrameCallback {

    private Runnable mRunnable;
    private Choreographer.FrameCallback mFrameCallback;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    Choreographer.FrameCallback getFrameCallback() {
      if (mFrameCallback == null) {
        mFrameCallback =
            new Choreographer.FrameCallback() {
              @Override
              public void doFrame(long frameTimeNanos) {
                ChoreographerCompat.FrameCallback.this.doFrame(frameTimeNanos);
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
