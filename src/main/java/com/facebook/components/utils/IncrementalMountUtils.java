/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.ComponentView;

import static com.facebook.litho.ThreadUtils.assertMainThread;

/**
 * Provides methods for enabling incremental mount.
 */
public class IncrementalMountUtils {

  /**
   * A view that wraps a child view and that provides a wrapped view to be incrementally mounted.
   */
  public interface WrapperView {

    /**
     * @return A child view that will be incrementally mounted.
     */
    View getWrappedView();
  }

  private static final Pools.SynchronizedPool<Rect> sRectPool =
      new Pools.SynchronizedPool<>(10);

  /**
   * Performs incremental mount on the children views of the given ViewGroup.
   * @param scrollingViewParent ViewGroup container of views that will be incrementally mounted.
   */
  public static void performIncrementalMount(ViewGroup scrollingViewParent) {
    assertMainThread();
    final int viewGroupWidth = scrollingViewParent.getWidth();
    final int viewGroupHeight = scrollingViewParent.getHeight();
    for (int i = 0; i < scrollingViewParent.getChildCount(); i++) {
      maybePerformIncrementalMountOnView(
          viewGroupWidth,
          viewGroupHeight,
          scrollingViewParent.getChildAt(i));
    }
  }
  /**
   * Returns a scroll listener that performs incremental mount.  This is needed
   * for feeds that do not use a ScrollingViewProxy, but rather directly use a
   * RecyclerView.
  */
  public static RecyclerView.OnScrollListener createRecyclerViewListener() {
    return new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          performIncrementalMount(recyclerView);
        }
    };
  }

  private static void maybePerformIncrementalMountOnView(
      int scrollingParentWidth,
      int scrollingParentHeight,
      View view) {
    final View underlyingView = view instanceof WrapperView
        ? ((WrapperView) view).getWrappedView()
        : view;

    if (!(underlyingView instanceof ComponentView)) {
      return;
    }

    final ComponentView componentView = (ComponentView) underlyingView;
    if (!componentView.isIncrementalMountEnabled()) {
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

    if (left >= 0 &&
        top >= 0 &&
        right <= scrollingParentWidth &&
        bottom <= scrollingParentHeight &&
        componentView.getPreviousMountBounds().width() == componentView.getWidth() &&
        componentView.getPreviousMountBounds().height() == componentView.getHeight()) {
      // View is fully visible, and has already been completely mounted.
      return;
    }

    final Rect rect = acquireRect();
    rect.set(
        Math.max(0, -left),
        Math.max(0, -top),
        Math.min(right, scrollingParentWidth) - left,
        Math.min(bottom, scrollingParentHeight) - top);

