/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutParams;
import com.facebook.litho.SizeSpec;

/**
 * An implementation for {@link LayoutInfo} to implement linear lists with a
 * {@link LinearLayoutManager}.
 */
public class LinearLayoutInfo implements LayoutInfo {

  private static final int MAX_SANE_RANGE = 10;
  private static final int MIN_SANE_RANGE = 2;

  private final LinearLayoutManager mLinearLayoutManager;

  public LinearLayoutInfo(LinearLayoutManager linearLayoutManager) {
    mLinearLayoutManager = linearLayoutManager;
  }

  public LinearLayoutInfo(Context context, int orientation, boolean reverseLayout) {
    mLinearLayoutManager = new InternalLinearLayoutManager(context, orientation, reverseLayout);
    mLinearLayoutManager.setMeasurementCacheEnabled(false);
  }

  @Override
  public int getScrollDirection() {
    return mLinearLayoutManager.getOrientation();
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return mLinearLayoutManager.findFirstVisibleItemPosition();
  }

  @Override
  public int findLastVisibleItemPosition() {
    return mLinearLayoutManager.findLastVisibleItemPosition();
  }

  @Override
  public int findFirstFullyVisibleItemPosition() {
    return mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
  }

  @Override
  public int findLastFullyVisibleItemPosition() {
    return mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
  }

  @Override
  public int getItemCount() {
    return mLinearLayoutManager.getItemCount();
  }

  @Override
  public RecyclerView.LayoutManager getLayoutManager() {
    return mLinearLayoutManager;
  }

  @Override
  public void setRenderInfoCollection(RenderInfoCollection renderInfoCollection) {
    // Do nothing
  }

  @Override
  public int approximateRangeSize(
      int firstMeasuredItemWidth,
      int firstMeasuredItemHeight,
      int recyclerMeasuredWidth,
      int recyclerMeasuredHeight) {

    int approximateRange;

    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        approximateRange = (int)
            Math.ceil((float) recyclerMeasuredWidth / (float) firstMeasuredItemWidth);
        break;
      default:
        approximateRange = (int)
            Math.ceil((float) recyclerMeasuredHeight / (float) firstMeasuredItemHeight);
        break;
    }

    if (approximateRange < MIN_SANE_RANGE) {
      approximateRange = MIN_SANE_RANGE;
    } else if (approximateRange > MAX_SANE_RANGE) {
      approximateRange = MAX_SANE_RANGE;
    }

    return approximateRange;
  }

  @Override
  public int getChildHeightSpec(int heightSpec, RenderInfo renderInfo) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return heightSpec;
      default:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    }
  }

  @Override
  public int getChildWidthSpec(int widthSpec, RenderInfo renderInfo) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
      default:
        return widthSpec;
    }
  }

  private static class InternalLinearLayoutManager
      extends LinearLayoutManager {

    InternalLinearLayoutManager(
        Context context,
        int orientation,
        boolean reverseLayout) {
      super(context, orientation, reverseLayout);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
      return getOrientation() == OrientationHelper.VERTICAL
          ? new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
          : new RecyclerView.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
      // Predictive animation in RecyclerView has some bugs that surface occasionally when one
      // RecyclerView is used as a child of another RecyclerView and it is very hard to repro
      // (https://issuetracker.google.com/issues/37007605). This one disables that optional
      // feature for Horizontal one which is used as inner RecyclerView most of the cases.
      // Disabling predictive animations lets RecyclerView use the simple animations instead.
      return getOrientation() == OrientationHelper.HORIZONTAL
          ? false
          : super.supportsPredictiveItemAnimations();
    }
  }
}
