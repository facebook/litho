// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.facebook.components.LayoutHandler;
import com.facebook.components.SizeSpec;

/**
 * An implementation for {@link LayoutInfo} to implement linear lists with a
 * {@link LinearLayoutManager}.
 */
public class LinearLayoutInfo implements LayoutInfo {

  private final LinearLayoutManager mLinearLayoutManager;

  public LinearLayoutInfo(Context context, int orientation, boolean reverseLayout) {
    mLinearLayoutManager = new LinearLayoutManager(context, orientation, reverseLayout);
  }

  @Override
  public int getScrollDirection() {
    return mLinearLayoutManager.getOrientation();
  }

  @Override
  public int findFirstVisiblePosition() {
    return mLinearLayoutManager.findFirstVisibleItemPosition();
  }

  @Override
  public int findLastVisiblePosition() {
    return mLinearLayoutManager.findLastVisibleItemPosition();
  }

  @Override
  public RecyclerView.LayoutManager getLayoutManager() {
    return mLinearLayoutManager;
  }

  @Override
  public int approximateRangeSize(
      int firstMeasuredItemWidth,
      int firstMeasuredItemHeight,
      int recyclerMeasuredWidth,
      int recyclerMeasuredHeight) {

    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return (int)
            Math.ceil((float) recyclerMeasuredWidth / (float) firstMeasuredItemWidth);

      default:
        return (int)
            Math.ceil((float) recyclerMeasuredHeight / (float) firstMeasuredItemHeight);
    }
  }

  @Override
  public int getChildHeightSpec(int heightSpec) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return heightSpec;
      default:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    }
  }

  @Override
  public int getChildWidthSpec(int widthSpec) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
      default:
        return widthSpec;
    }
  }
}
