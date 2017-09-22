/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SnapHelper;
import android.util.DisplayMetrics;
import android.view.View;

public class StartSnapHelper extends SnapHelper {

  private static final int MAX_SCROLL_ON_FLING_DURATION = 100; // ms
  private static final float MILLISECONDS_PER_INCH = 100f;

  // Orientation helpers are lazily created per LayoutManager.
  @Nullable private OrientationHelper mVerticalHelper;
  @Nullable private OrientationHelper mHorizontalHelper;

  @Nullable private LayoutManager mVerticalHelperLayoutManager;
  @Nullable private LayoutManager mHorizontalHelperLayoutManager;
  @Nullable private RecyclerView mRecyclerView;

  @Nullable
  @Override
  public int[] calculateDistanceToFinalSnap(
      @NonNull LayoutManager layoutManager, @NonNull View targetView) {
    int[] out = new int[2];
    if (layoutManager.canScrollHorizontally()) {
      out[0] = distanceToStart(layoutManager, targetView, getHorizontalHelper(layoutManager));
    } else {
      out[0] = 0;
    }

    if (layoutManager.canScrollVertically()) {
      out[1] = distanceToStart(layoutManager, targetView, getVerticalHelper(layoutManager));
    } else {
      out[1] = 0;
    }
    return out;
  }

  @Override
  public void attachToRecyclerView(@Nullable RecyclerView recyclerView)
      throws IllegalStateException {
    mRecyclerView = recyclerView;
    super.attachToRecyclerView(recyclerView);
  }

  @Nullable
  @Override
  public View findSnapView(LayoutManager layoutManager) {
    if (layoutManager.canScrollVertically()) {
      return findCenterView(layoutManager, getVerticalHelper(layoutManager));
    } else if (layoutManager.canScrollHorizontally()) {
      return findCenterView(layoutManager, getHorizontalHelper(layoutManager));
    }
    return null;
  }

  @Override
  public int findTargetSnapPosition(LayoutManager layoutManager, int velocityX, int velocityY) {
    final int itemCount = layoutManager.getItemCount();
    if (itemCount == 0) {
      return RecyclerView.NO_POSITION;
    }

    View mStartMostChildView = null;
    if (layoutManager.canScrollVertically()) {
      mStartMostChildView = findStartView(layoutManager, getVerticalHelper(layoutManager));
    } else if (layoutManager.canScrollHorizontally()) {
      mStartMostChildView = findStartView(layoutManager, getHorizontalHelper(layoutManager));
    }

    if (mStartMostChildView == null) {
      return RecyclerView.NO_POSITION;
    }
    final int centerPosition = layoutManager.getPosition(mStartMostChildView);
    if (centerPosition == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION;
    }

    final boolean forwardDirection;
    if (layoutManager.canScrollHorizontally()) {
      forwardDirection = velocityX > 0;
    } else {
      forwardDirection = velocityY > 0;
    }
    boolean reverseLayout = false;
    if ((layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
      RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
          (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;
      PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
      if (vectorForEnd != null) {
        reverseLayout = vectorForEnd.x < 0 || vectorForEnd.y < 0;
      }
    }
    return reverseLayout
        ? (forwardDirection ? centerPosition - 1 : centerPosition)
        : (forwardDirection ? centerPosition + 1 : centerPosition);
  }

  @Override
  protected LinearSmoothScroller createSnapScroller(LayoutManager layoutManager) {
    if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
      return null;
    }
    return new LinearSmoothScroller(mRecyclerView.getContext()) {
      @Override
      protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
        int[] snapDistances =
            calculateDistanceToFinalSnap(mRecyclerView.getLayoutManager(), targetView);
        final int dx = snapDistances[0];
        final int dy = snapDistances[1];
        final int time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)));
        if (time > 0) {
          action.update(dx, dy, time, mDecelerateInterpolator);
        }
      }

      @Override
      protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
      }

      @Override
      protected int calculateTimeForScrolling(int dx) {
        return Math.min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx));
      }
    };
  }

  private int distanceToStart(
      @NonNull RecyclerView.LayoutManager layoutManager,
      @NonNull View targetView,
      OrientationHelper helper) {
    final int childCenter = helper.getDecoratedStart(targetView);
    final int containerCenter;
    if (layoutManager.getClipToPadding()) {
      containerCenter = helper.getStartAfterPadding();
    } else {
      containerCenter = 0;
    }
    return childCenter - containerCenter;
  }

  /**
   * Return the child view that is currently closest to the center of this parent.
   *
   * @param layoutManager The {@link LayoutManager} associated with the attached {@link
   *     RecyclerView}.
   * @param helper The relevant {@link OrientationHelper} for the attached {@link RecyclerView}.
   * @return the child view that is currently closest to the center of this parent.
   */
  @Nullable
  private View findCenterView(LayoutManager layoutManager, OrientationHelper helper) {
    int childCount = layoutManager.getChildCount();
    if (childCount == 0) {
      return null;
    }

    View closestChild = null;
    final int center;
    if (layoutManager.getClipToPadding()) {
      center = helper.getStartAfterPadding() + helper.getTotalSpace() / 2;
    } else {
      center = helper.getEnd() / 2;
    }
    int absClosest = Integer.MAX_VALUE;

    for (int i = 0; i < childCount; i++) {
      final View child = layoutManager.getChildAt(i);
      int childCenter =
          helper.getDecoratedStart(child) + (helper.getDecoratedMeasurement(child) / 2);
      int absDistance = Math.abs(childCenter - center);

      /** if child center is closer than previous closest, set it as closest * */
      if (absDistance < absClosest) {
        absClosest = absDistance;
        closestChild = child;
      }
    }
    return closestChild;
  }

  /**
   * Return the child view that is currently closest to the start of this parent.
   *
   * @param layoutManager The {@link LayoutManager} associated with the attached {@link
   *     RecyclerView}.
   * @param helper The relevant {@link OrientationHelper} for the attached {@link RecyclerView}.
   * @return the child view that is currently closest to the start of this parent.
   */
  @Nullable
  private View findStartView(LayoutManager layoutManager, OrientationHelper helper) {
    int childCount = layoutManager.getChildCount();
    if (childCount == 0) {
      return null;
    }

    View closestChild = null;
    int startest = Integer.MAX_VALUE;

    for (int i = 0; i < childCount; i++) {
      final View child = layoutManager.getChildAt(i);
      int childStart = helper.getDecoratedStart(child);

      /** if child is more to start than previous closest, set it as closest * */
      if (childStart < startest) {
        startest = childStart;
        closestChild = child;
      }
    }

    return closestChild;
  }

  @NonNull
  private OrientationHelper getVerticalHelper(@NonNull LayoutManager layoutManager) {
    if (mVerticalHelper == null || mVerticalHelperLayoutManager != layoutManager) {
      mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager);
      mVerticalHelperLayoutManager = layoutManager;
    }

    return mVerticalHelper;
  }

  @NonNull
  private OrientationHelper getHorizontalHelper(@NonNull LayoutManager layoutManager) {
    if (mHorizontalHelper == null || mHorizontalHelperLayoutManager != layoutManager) {
      mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager);
      mHorizontalHelperLayoutManager = layoutManager;
    }

    return mHorizontalHelper;
  }
}
