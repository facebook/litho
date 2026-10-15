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

import android.view.View

/**
 * A controller that can be passed to [VerticalScroll] to trigger events from outside the component
 * hierarchy.
 *
 * When used with VerticalScroll, the controller also manages scroll position persistence across
 * unbind/bind cycles. It holds onto the ScrollPosition reference so that when a new LithoScrollView
 * is bound, the saved scroll position is restored, and when unbound, the scroll position is cleanly
 * detached from the old view.
 */
class VerticalScrollEventsController {

  private var scrollView: LithoScrollView? = null
  // Retained across bind/unbind cycles so scroll position survives view recycling.
  private var scrollPosition: LithoScrollView.ScrollPosition? = null

  fun setScrollView(scrollView: LithoScrollView?) {
    if (scrollView != null) {
      this.scrollView = scrollView
      // Restore the persisted scroll position onto the newly bound view.
      scrollView.setScrollPosition(scrollPosition)
    } else {
      // Detach scroll position from the view being unbound so it stops tracking.
      this.scrollView?.setScrollPosition(null)
      this.scrollView = null
    }
  }

  fun setScrollPosition(scrollPosition: LithoScrollView.ScrollPosition?) {
    this.scrollPosition = scrollPosition
    // If a view is already bound, forward immediately so onScrollChanged() can persist to it.
    scrollView?.setScrollPosition(scrollPosition)
  }

  fun scrollTo(y: Int) {
    scrollView?.let { scrollView -> scrollView.scrollTo(scrollView.scrollX, y) }
  }

  fun smoothScrollTo(y: Int) {
    scrollView?.let { scrollView -> scrollView.smoothScrollTo(scrollView.scrollX, y) }
  }

  fun scrollToTop() {
    scrollView?.fullScroll(View.FOCUS_UP)
  }

  fun scrollToBottom() {
    scrollView?.fullScroll(View.FOCUS_DOWN)
  }
}
