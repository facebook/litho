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
import android.view.View;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Implementation of the {@link LinearSnapHelper} supporting hscroll custom target child view
 *
 * <p>The implementation will snap the center of the custom target child view to the center of the
 * attached {@link RecyclerView}. If you intend to change this behavior then override {@link
 * SnapHelper#findTargetSnapPosition}.
 */
public class CustomSpeedLinearSnapHelper extends LinearSnapHelper {
  private static final float INVALID_DISTANCE = 1f;
  private static int mDeltaJumpThreshold = Integer.MAX_VALUE;

  public CustomSpeedLinearSnapHelper(int deltaJumpThreshold) {
    mDeltaJumpThreshold = deltaJumpThreshold;
  }

  @Override
  public int findTargetSnapPosition(
      RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
    if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
      return RecyclerView.NO_POSITION;
    }

    final int itemCount = layoutManager.getItemCount();
    if (itemCount == 0) {
      return RecyclerView.NO_POSITION;
    }

    final View currentView = findSnapView(layoutManager);
    if (currentView == null) {
      return RecyclerView.NO_POSITION;
    }

    final int currentPosition = layoutManager.getPosition(currentView);
    if (currentPosition == RecyclerView.NO_POSITION) {
      return RecyclerView.NO_POSITION;
    }

    RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
        (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;
    // deltaJumps sign comes from the velocity which may not match the order of children in
    // the LayoutManager. To overcome this, we ask for a vector from the LayoutManager to
    // get the direction.
    PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
    if (vectorForEnd == null) {
      // cannot get a vector for the given position.
      return RecyclerView.NO_POSITION;
    }

    int vDeltaJump, hDeltaJump;
    if (layoutManager.canScrollHorizontally()) {
      hDeltaJump =
          estimateNextPositionDiffForFling(
              layoutManager, OrientationHelper.createHorizontalHelper(layoutManager), velocityX, 0);
      // set a threshold to the jump
      if (hDeltaJump > mDeltaJumpThreshold) {
        hDeltaJump = mDeltaJumpThreshold;
      }
      if (hDeltaJump < -mDeltaJumpThreshold) {
        hDeltaJump = -mDeltaJumpThreshold;
      }
      if (vectorForEnd.x < 0) {
        hDeltaJump = -hDeltaJump;
      }
    } else {
      hDeltaJump = 0;
    }
    if (layoutManager.canScrollVertically()) {
      vDeltaJump =
          estimateNextPositionDiffForFling(
              layoutManager, OrientationHelper.createVerticalHelper(layoutManager), 0, velocityY);
      if (vectorForEnd.y < 0) {
        vDeltaJump = -vDeltaJump;
      }
    } else {
      vDeltaJump = 0;
    }

    int deltaJump = layoutManager.canScrollVertically() ? vDeltaJump : hDeltaJump;
    if (deltaJump == 0) {
      return RecyclerView.NO_POSITION;
    }

    int targetPos = currentPosition + deltaJump;
    if (targetPos < 0) {
      targetPos = 0;
    }
    if (targetPos >= itemCount) {
      targetPos = itemCount - 1;
    }
    return targetPos;
  }

  private int estimateNextPositionDiffForFling(
      RecyclerView.LayoutManager layoutManager,
      OrientationHelper helper,
      int velocityX,
      int velocityY) {
    int[] distances = calculateScrollDistance(velocityX, velocityY);
    float distancePerChild = computeDistancePerChild(layoutManager, helper);
    if (distancePerChild <= 0) {
      return 0;
    }
    int distance = Math.abs(distances[0]) > Math.abs(distances[1]) ? distances[0] : distances[1];
    return (int) Math.round(distance / distancePerChild);
  }

  private static float computeDistancePerChild(
      RecyclerView.LayoutManager layoutManager, OrientationHelper helper) {
    View minPosView = null;
    View maxPosView = null;
    int minPos = Integer.MAX_VALUE;
    int maxPos = Integer.MIN_VALUE;
    int childCount = layoutManager.getChildCount();
    if (childCount == 0) {
      return INVALID_DISTANCE;
    }

    for (int i = 0; i < childCount; i++) {
      View child = layoutManager.getChildAt(i);
      final int pos = layoutManager.getPosition(child);
      if (pos == RecyclerView.NO_POSITION) {
        continue;
      }
      if (pos < minPos) {
        minPos = pos;
        minPosView = child;
      }
      if (pos > maxPos) {
        maxPos = pos;
        maxPosView = child;
      }
    }
    if (minPosView == null || maxPosView == null) {
      return INVALID_DISTANCE;
    }
    int start =
        Math.min(helper.getDecoratedStart(minPosView), helper.getDecoratedStart(maxPosView));
    int end = Math.max(helper.getDecoratedEnd(minPosView), helper.getDecoratedEnd(maxPosView));
    int distance = end - start;
    if (distance == 0) {
      return INVALID_DISTANCE;
    }
    return 1f * distance / ((maxPos - minPos) + 1);
  }
}
