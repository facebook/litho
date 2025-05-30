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
 * A controller that can be passed as [com.facebook.litho.annotations.Prop] to a
 * [VerticalScrollSpec] to trigger events from outside the component hierarchy.
 */
class VerticalScrollEventsController {

  private var scrollView: LithoScrollView? = null

  fun setScrollView(scrollView: LithoScrollView?) {
    this.scrollView = scrollView
  }

  fun scrollTo(y: Int) {
    scrollView?.let { scrollView -> scrollView.scrollTo(scrollView.scrollX, y) }
  }

  fun smoothScrollTo(y: Int) {
    scrollView?.let { scrollView ->
      scrollView.smoothScrollTo(scrollView.scrollX, scrollView.scrollY + y)
    }
  }

  fun scrollToTop() {
    scrollView?.fullScroll(View.FOCUS_UP)
  }

  fun scrollToBottom() {
    scrollView?.fullScroll(View.FOCUS_DOWN)
  }
}
