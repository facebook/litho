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

package com.facebook.litho.widget;

import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.SnapHelper;

public class StartSnapHelper extends SnapHelper {

  private static final int MAX_SCROLL_ON_FLING_DURATION = 100; // ms
  private static final float MILLISECONDS_PER_INCH = 100f;

  // Orientation helpers are lazily created per LayoutManager.
  @Nullable private OrientationHelper mVerticalHelper;
  @Nullable private OrientationHelper mHorizontalHelper;

  @Nullable private LayoutManager mVerticalHelperLayoutManager;
  @Nullable private LayoutManager mHorizontalHelperLayoutManager;
  @Nullable private RecyclerView mRecyclerView;
  private final int mFlingOffset;

  public StartSnapHelper(int flingOffset) {
    mFlingOffset = flingOffset;
  }

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
      return findViewClosestToStart(layoutManager, getVerticalHelper(layoutManager));
    } else if (layoutManager.canScrollHorizontally()) {
      return findViewClosestToStart(layoutManager, getHorizontalHelper(layoutManager));
    }
    return null;
  }

  @Override
  public int findTargetSnapPosition(LayoutManager layoutManager, int velocityX, int velocityY) {
    final int itemCount = layoutManager.getItemCount();
    if (itemCount == 0) {
      return RecyclerView.NO_POSITION;
    }

    final boolean isHorizontal = layoutManager.canScrollHorizontally();
    final OrientationHelper orientationHelper =
        isHorizontal ? getHorizontalHelper(layoutManager) : getVerticalHelper(layoutManager);
    final View firstBeforeStartChild = findFirstViewBeforeStart(layoutManager, orientationHelper);

    if (firstBeforeStartChild == null) {
      return RecyclerView.NO_POSITION;
    }
    final int firstBeforeStartPosition = layoutManager.getPosition(firstBeforeStartChild);
    if (firstBeforeStartPosition == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION;
    }

    final boolean forwardDirection = isHorizontal ? velocityX > 0 : velocityY > 0;

    boolean reverseLayout = false;
    if ((layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
      RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
          (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;
      PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
      if (vectorForEnd != null) {
        reverseLayout = vectorForEnd.x < 0 || vectorForEnd.y < 0;
      }
    }

    int targetPos =
        forwardDirection
            ? getForwardTargetPosition(layoutManager, reverseLayout, firstBeforeStartPosition)
            : firstBeforeStartPosition;
    if (targetPos < 0) {
      targetPos = 0;
    }
    if (targetPos >= itemCount) {
      targetPos = itemCount - 1;
    }
    return targetPos;
  }

  private int getForwardTargetPosition(
      LayoutManager layoutManager, boolean reverseLayout, int firstBeforeStartPosition) {
    if (layoutManager instanceof GridLayoutManager) {
      return reverseLayout
          ? (firstBeforeStartPosition - ((GridLayoutManager) layoutManager).getSpanCount())
              / mFlingOffset
          : (firstBeforeStartPosition + ((GridLayoutManager) layoutManager).getSpanCount())
              * mFlingOffset;
    }
    return reverseLayout
        ? firstBeforeStartPosition - mFlingOffset
        : firstBeforeStartPosition + mFlingOffset;
  }

  @Override
  protected @Nullable LinearSmoothScroller createSnapScroller(LayoutManager layoutManager) {
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
    final int childStart = helper.getDecoratedStart(targetView);
    final int containerStart = helper.getStartAfterPadding();
    return childStart - containerStart;
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
  private static View findViewClosestToStart(
      LayoutManager layoutManager, OrientationHelper helper) {
    int childCount = layoutManager.getChildCount();
    if (childCount == 0) {
      return null;
    }

    View closestChild = null;
    final int start = helper.getStartAfterPadding();
    int absClosest = Integer.MAX_VALUE;

    for (int i = 0; i < childCount; i++) {
      final View child = layoutManager.getChildAt(i);
      int childStart = helper.getDecoratedStart(child);
      int absDistance = Math.abs(childStart - start);

      /** if child start is closer than previous closest, set it as closest * */
      if (absDistance < absClosest) {
        absClosest = absDistance;
        closestChild = child;
      }
    }

    return closestChild;
  }

  /** @return the first View whose start is before the start of this recycler view */
  @Nullable
  private static View findFirstViewBeforeStart(
      LayoutManager layoutManager, OrientationHelper helper) {
    int childCount = layoutManager.getChildCount();
    if (childCount == 0) {
      return null;
    }

    View closestChild = null;
    final int start = helper.getStartAfterPadding();
    int absClosest = Integer.MAX_VALUE;

    for (int i = 0; i < childCount; i++) {
      final View child = layoutManager.getChildAt(i);
      int childStart = helper.getDecoratedStart(child);
      int absDistance = Math.abs(childStart - start);

      if (childStart < start && absDistance < absClosest) {
        absClosest = absDistance;
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
