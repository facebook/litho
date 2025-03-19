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
import com.facebook.litho.layout.LayoutDirection

/**
 * A controller that can be passed as [com.facebook.litho.annotations.Prop] to a [ ] to trigger
 * events from outside the component hierarchy.
 */
class HorizontalScrollEventsController {
  private var horizontalScrollView: HorizontalScrollLithoView? = null

  fun setScrollableView(scrollableView: HorizontalScrollLithoView?) {
    horizontalScrollView = scrollableView
  }

  fun scrollTo(x: Int) {
    horizontalScrollView?.scrollTo(x, 0)
  }

  fun smoothScrollTo(x: Int) {
    horizontalScrollView?.smoothScrollTo(x, 0)
  }

  /**
   * This method will scroll the view to the left or right depending on the layout direction and
   * give the focus to the leftmost/ rightmost component in the new visible area. If no component is
   * a good candidate for focus, this scrollview reclaims the focus.
   */
  fun scrollToEnd() {
    horizontalScrollView?.let { scrollView ->
      val direction =
          if (LayoutDirection.fromContext(scrollView.context) == LayoutDirection.LTR) {
            View.FOCUS_RIGHT
          } else {
            View.FOCUS_LEFT
          }
      scrollView.fullScroll(direction)
    }
  }
}
