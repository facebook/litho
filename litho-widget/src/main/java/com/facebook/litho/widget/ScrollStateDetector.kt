/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.widget

import android.view.MotionEvent
import android.view.View
import androidx.annotation.UiThread

/**
 * Detects scroll status based on onTouch(), onScrollChanged(), fling(), and draw() callbacks.
 *
 * The draw() method on the ScrollView always calls onScrollChanged() internally. Hence, if there is
 * no call to onScrollChanged() between two draw calls, this indicates that scrolling has stopped.
 *
 * For more details see:
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=21712
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/View.java;l=20840
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1420
 *
 * Here is a comment that elaborates why draw() method needs to perform onScroll():
 * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/HorizontalScrollView.java;l=1395
 */
internal class ScrollStateDetector(private val hostView: View) {
  private var scrollStateListener: ScrollStateListener? = null

  /**
   * The potential timeline of events.
   *
   * #1 scroll without fling: touch_down --> onScrollChanged(...) --> touch_up
   *
   * #2 scroll with fling: touch_down --> onScrollChanged(...) --> onFling --> touch_up -->
   * onScrollChanged(...)
   *
   * Below variables are always be accessed from main thread, and no need to synchronize.
   */
  private var scrollStarted = false

  private var flingStarted = false
  private var scrollStopped = false
  private var hasScrolledBetweenDraw = false

  @UiThread
  fun onTouchEvent(motionEvent: MotionEvent) {
    if (motionEvent.action == MotionEvent.ACTION_DOWN ||
        motionEvent.action == MotionEvent.ACTION_MOVE) {
      if (scrollStopped) {
        reset()
      } else {
        flingStarted = false
        hasScrolledBetweenDraw = true
      }
    } else if (motionEvent.action == MotionEvent.ACTION_UP ||
        motionEvent.action == MotionEvent.ACTION_CANCEL) {
      // if the touch events hasn't triggered fling, the up event leads to scroll_stop.
      if (!flingStarted && scrollStarted && !scrollStopped) {
        scrollStopped = true
        scrollStateListener?.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STOPPED)
      }
    }
  }

  @UiThread
  fun onScrollChanged() {
    if (!scrollStarted && !scrollStopped) {
      scrollStarted = true
      scrollStateListener?.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STARTED)
    }
    hasScrolledBetweenDraw = true
  }

  @UiThread
  fun onDraw() {
    if (flingStarted && !scrollStopped) {
      if (!hasScrolledBetweenDraw) {
        scrollStopped = true
        flingStarted = false
        scrollStateListener?.onScrollStateChanged(hostView, ScrollStateListener.SCROLL_STOPPED)
      }
      hasScrolledBetweenDraw = false
    }
  }

  @UiThread
  fun fling() {
    flingStarted = true
  }

  @UiThread
  fun setListener(listener: ScrollStateListener?) {
    this.scrollStateListener = listener
  }

  @UiThread
  private fun reset() {
    scrollStarted = false
    flingStarted = false
    scrollStopped = false
  }
}
