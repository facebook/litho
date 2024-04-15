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

package com.facebook.litho.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.LithoView
import com.facebook.litho.ThreadUtils
import kotlin.math.max
import kotlin.math.min

/** Provides methods for enabling incremental mount. */
object IncrementalMountUtils {

  /**
   * Performs incremental mount on all [LithoView]s within the given View.
   *
   * @param view the view to process
   */
  @JvmStatic
  fun incrementallyMountLithoViews(view: View) {
    if (view is LithoView) {
      view.notifyVisibleBoundsChanged()
    } else if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        incrementallyMountLithoViews(child)
      }
    }
  }

  /**
   * Performs incremental mount on the children views of the given ViewGroup.
   *
   * @param scrollingViewParent ViewGroup container of views that will be incrementally mounted.
   */
  @JvmStatic
  fun performIncrementalMount(scrollingViewParent: ViewGroup) {
    ThreadUtils.assertMainThread()
    val viewGroupWidth = scrollingViewParent.width
    val viewGroupHeight = scrollingViewParent.height
    for (i in 0 until scrollingViewParent.childCount) {
      maybePerformIncrementalMountOnView(
          viewGroupWidth, viewGroupHeight, scrollingViewParent.getChildAt(i))
    }
  }

  private fun maybePerformIncrementalMountOnView(
      scrollingParentWidth: Int,
      scrollingParentHeight: Int,
      view: View
  ) {
    val underlyingView: View =
        if (view is WrapperView) {
          (view as WrapperView).wrappedView
        } else {
          view
        }
    val lithoView: LithoView = (underlyingView as? LithoView) ?: return

    if (!lithoView.isIncrementalMountEnabled) {
      return
    }
    check(!(view !== underlyingView && view.height != underlyingView.height)) {
      "ViewDiagnosticsWrapper must be the same height as the underlying view"
    }
    val translationX = view.translationX.toInt()
    val translationY = view.translationY.toInt()
    val top = view.top + translationY
    val bottom = view.bottom + translationY
    val left = view.left + translationX
    val right = view.right + translationX

    if (left >= 0 &&
        top >= 0 &&
        right <= scrollingParentWidth &&
        bottom <= scrollingParentHeight &&
        lithoView.previousMountBounds.width() == lithoView.width &&
        lithoView.previousMountBounds.height() == lithoView.height) {
      // View is fully visible, and has already been completely mounted.
      return
    }
    val rect =
        Rect(
            max(0, -left),
            max(0, -top),
            min(right, scrollingParentWidth) - left,
            min(bottom, scrollingParentHeight) - top)
    if (rect.isEmpty) {
      // View is not visible at all, nothing to do.
      return
    }
    lithoView.notifyVisibleBoundsChanged(rect, true)
  }

  /**
   * A view that wraps a child view and that provides a wrapped view to be incrementally mounted.
   */
  interface WrapperView {
    /** @return A child view that will be incrementally mounted. */
    val wrappedView: View
  }
}
