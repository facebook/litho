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

package com.facebook.litho.utils;

import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.LithoView;

/** Provides methods for enabling incremental mount. */
public class IncrementalMountUtils {

  /**
   * A view that wraps a child view and that provides a wrapped view to be incrementally mounted.
   */
  public interface WrapperView {

    /** @return A child view that will be incrementally mounted. */
    View getWrappedView();
  }

  /** Performs incremental mount on all {@link LithoView}s within the given View. */
  public static void incrementallyMountLithoViews(View view) {
    if (view instanceof LithoView && ((LithoView) view).isIncrementalMountEnabled()) {
      ((LithoView) view).performIncrementalMount();
    } else if (view instanceof ViewGroup) {
      for (int i = 0, size = ((ViewGroup) view).getChildCount(); i < size; i++) {
        final View child = ((ViewGroup) view).getChildAt(i);
        incrementallyMountLithoViews(child);
      }
    }
  }

  /**
   * Performs incremental mount on the children views of the given ViewGroup.
   *
   * @param scrollingViewParent ViewGroup container of views that will be incrementally mounted.
   */
  public static void performIncrementalMount(ViewGroup scrollingViewParent) {
    assertMainThread();

    final int viewGroupWidth = scrollingViewParent.getWidth();
    final int viewGroupHeight = scrollingViewParent.getHeight();
    for (int i = 0; i < scrollingViewParent.getChildCount(); i++) {
      maybePerformIncrementalMountOnView(
          viewGroupWidth, viewGroupHeight, scrollingViewParent.getChildAt(i));
    }
  }

  private static void maybePerformIncrementalMountOnView(
      int scrollingParentWidth, int scrollingParentHeight, View view) {
    final View underlyingView =
        view instanceof WrapperView ? ((WrapperView) view).getWrappedView() : view;

    if (!(underlyingView instanceof LithoView)) {
      return;
    }

    final LithoView lithoView = (LithoView) underlyingView;
    if (!lithoView.isIncrementalMountEnabled()) {
      return;
    }

    if (view != underlyingView && view.getHeight() != underlyingView.getHeight()) {
      throw new IllegalStateException(
          "ViewDiagnosticsWrapper must be the same height as the underlying view");
    }

    final int translationX = (int) view.getTranslationX();
    final int translationY = (int) view.getTranslationY();
    final int top = view.getTop() + translationY;
    final int bottom = view.getBottom() + translationY;
    final int left = view.getLeft() + translationX;
    final int right = view.getRight() + translationX;

    if (left >= 0
        && top >= 0
        && right <= scrollingParentWidth
        && bottom <= scrollingParentHeight
        && lithoView.getPreviousMountBounds().width() == lithoView.getWidth()
        && lithoView.getPreviousMountBounds().height() == lithoView.getHeight()) {
      // View is fully visible, and has already been completely mounted.
      return;
    }

    final Rect rect =
        new Rect(
            Math.max(0, -left),
            Math.max(0, -top),
            Math.min(right, scrollingParentWidth) - left,
            Math.min(bottom, scrollingParentHeight) - top);

    if (rect.isEmpty()) {
      // View is not visible at all, nothing to do.
      return;
    }

    lithoView.performIncrementalMount(rect, true);
  }
}
