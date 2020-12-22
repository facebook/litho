package com.facebook.litho.widget;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

import android.support.annotation.UiThread;
import android.view.MotionEvent;
import android.view.View;

// Detects scroll status based on onTouch(), onScrollChanged(), fling(), and
// draw() callbacks.
//
// The draw() method on the ScrollView always calls onScrollChanged() internally.
// Hence, if there is no call to onScrollChanged() between two draw calls, this
// indicates that scrolling has stopped.
//
// For more details see:
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=21712
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=20840
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1420
//
// Here is a comment that elaborates why draw() method needs to perform onScroll():
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1395
class ScrollStateDetector {

  private final View mHostView;
  private final ScrollStateListener mScrollStateListener;

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

  ScrollStateDetector(View view, ScrollStateListener scrollStateListener) {
    this.mHostView = view;
    this.mScrollStateListener = scrollStateListener;
  }

  @UiThread
  void onTouchEvent(MotionEvent motionEvent) {
    if (motionEvent.getAction() == ACTION_DOWN) {
      if (mScrollStopped) {
        reset();
      } else {
        mFlingStarted = false;
        mHasScrolledBetweenDraw = true;
      }
    } else if (motionEvent.getAction() == ACTION_UP) {
      // if the touch events hasn't triggered fling, the up event leads to scroll_stop.
      if (!mFlingStarted && mScrollStarted && !mScrollStopped) {
        mScrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STOPPED);
        mScrollStopped = true;
      }
    }
  }

  @UiThread
  void onScrollChanged() {
    if (!mScrollStarted && !mScrollStopped) {
      mScrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STARTED);
      mScrollStarted = true;
    }
    mHasScrolledBetweenDraw = true;
  }

  @UiThread
  void onDraw() {
    if (mFlingStarted && !mScrollStopped) {
      if (!mHasScrolledBetweenDraw) {
        mScrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STOPPED);
        mScrollStopped = true;
        mFlingStarted = false;
      }
      mHasScrolledBetweenDraw = false;
    }
  }

  @UiThread
  void fling() {
    mFlingStarted = true;
  }

  @UiThread
  private void reset() {
    mScrollStarted = false;
    mFlingStarted = false;
    mScrollStopped = false;
  }
}
