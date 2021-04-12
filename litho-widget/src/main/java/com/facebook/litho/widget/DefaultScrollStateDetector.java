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

package com.facebook.litho.widget;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.UiThread;
import javax.annotation.Nullable;

/**
 * Detects scroll status based on onTouch(), onScrollChanged(), fling(), and draw() callbacks.
 *
 * <p>The draw() method on the ScrollView always calls onScrollChanged() internally. Hence, if there
 * is no call to onScrollChanged() between two draw calls, this indicates that scrolling has
 * stopped.
 *
 * <p>For more details see:
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=21712
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=20840
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1420
 *
 * <p>Here is a comment that elaborates why draw() method needs to perform onScroll():
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1395
 */
class DefaultScrollStateDetector implements ScrollStateDetector {

  @Nullable private ScrollStateListener scrollStateListener;

  /**
   * The potential timeline of events.
   *
   * <p>#1 scroll without fling: touch_down --> onScrollChanged(...) --> touch_up
   *
   * <p>#2 scroll with fling: touch_down --> onScrollChanged(...) --> onFling --> touch_up -->
   * onScrollChanged(...)
   *
   * <p>Below variables are always be accessed from main thread, and no need to synchronize.
   */
  private boolean mScrollStarted;

  private boolean mFlingStarted;
  private boolean mScrollStopped;
  private boolean mHasScrolledBetweenDraw;

  @Override
  @UiThread
  public void onInterceptTouchEvent(View hostView, MotionEvent motionEvent) {
    // no op - all detection is done through onTouchEvent
  }

  @Override
  @UiThread
  public void onTouchEvent(View hostView, MotionEvent motionEvent) {
    if (motionEvent.getAction() == ACTION_DOWN || motionEvent.getAction() == ACTION_MOVE) {
      if (mScrollStopped) {
        reset();
      } else {
        mFlingStarted = false;
        mHasScrolledBetweenDraw = true;
      }
    } else if (motionEvent.getAction() == ACTION_UP || motionEvent.getAction() == ACTION_CANCEL) {
      // if the touch events hasn't triggered fling, the up event leads to scroll_stop.
      if (!mFlingStarted && mScrollStarted && !mScrollStopped) {
        mScrollStopped = true;
        if (scrollStateListener != null) {
          scrollStateListener.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STOPPED);
        }
      }
    }
  }

  @Override
  @UiThread
  public void onScrollChanged(View hostView) {
    if (!mScrollStarted && !mScrollStopped) {
      mScrollStarted = true;
      if (scrollStateListener != null) {
        scrollStateListener.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STARTED);
      }
    }
    mHasScrolledBetweenDraw = true;
  }

  @Override
  @UiThread
  public void onDraw(View hostView) {
    if (mFlingStarted && !mScrollStopped) {
      if (!mHasScrolledBetweenDraw) {
        mScrollStopped = true;
        mFlingStarted = false;
        if (scrollStateListener != null) {
          scrollStateListener.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STOPPED);
        }
      }
      mHasScrolledBetweenDraw = false;
    }
  }

  @Override
  @UiThread
  public void fling(View hostView) {
    mFlingStarted = true;
  }

  @Override
  @UiThread
  public void setListener(@Nullable ScrollStateListener listener) {
    this.scrollStateListener = listener;
  }

  @UiThread
  private void reset() {
    mScrollStarted = false;
    mFlingStarted = false;
    mScrollStopped = false;
  }
}
