package com.facebook.litho.widget;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.UiThread;
import javax.annotation.Nullable;

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
  @Nullable private final ScrollStateListener scrollStateListener;

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

  ScrollStateDetector(View view) {
    this.mHostView = view;
  }

  @UiThread
  void onTouchEvent(MotionEvent motionEvent) {
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
          scrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STOPPED);
        }
      }
    }
  }

  @UiThread
  void onScrollChanged() {
    if (!mScrollStarted && !mScrollStopped) {
      mScrollStarted = true;
      if (scrollStateListener != null) {
        scrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STARTED);
      }
    }
    mHasScrolledBetweenDraw = true;
  }

  @UiThread
  void onDraw() {
    if (mFlingStarted && !mScrollStopped) {
      if (!mHasScrolledBetweenDraw) {
        mScrollStopped = true;
        mFlingStarted = false;
        if (scrollStateListener != null) {
          scrollStateListener.onScrollStateChanged(mHostView, ScrollStateListener.SCROLL_STOPPED);
        }
      }
      mHasScrolledBetweenDraw = false;
    }
  }

  @UiThread
  void fling() {
    mFlingStarted = true;
  }

  @UiThread
  void setListener(@Nullable ScrollStateListener listener) {
    this.scrollStateListener = listener;
  }

  @UiThread
  private void reset() {
    mScrollStarted = false;
    mFlingStarted = false;
    mScrollStopped = false;
  }
}
