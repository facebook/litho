/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

import com.facebook.litho.SizeSpec;

public class GridLayoutInfo implements LayoutInfo {

  private final GridLayoutManager mGridLayoutManager;

  public GridLayoutInfo(Context context, int spanCount, int orientation, boolean reverseLayout) {
    mGridLayoutManager = new GridLayoutManager(context, spanCount, orientation, reverseLayout);
  }

  public GridLayoutInfo(Context context, int spanCount) {
    this(context, spanCount, OrientationHelper.VERTICAL, false);
  }

  @Override
  public int getScrollDirection() {
    return mGridLayoutManager.getOrientation();
  }

  @Override
  public int findFirstVisiblePosition() {
    return mGridLayoutManager.findFirstVisibleItemPosition();
  }

  @Override
  public int findLastVisiblePosition() {
    return mGridLayoutManager.findLastVisibleItemPosition();
  }

  @Override
  public RecyclerView.LayoutManager getLayoutManager() {
    return mGridLayoutManager;
  }

  @Override
  public int approximateRangeSize(
      int firstMeasuredItemWidth,
      int firstMeasuredItemHeight,
      int recyclerMeasuredWidth,
      int recyclerMeasuredHeight) {
    final int spanCount = mGridLayoutManager.getSpanCount();
    switch (mGridLayoutManager.getOrientation()) {
      case GridLayoutManager.HORIZONTAL:
        final int colCount = (int) Math.ceil(
            (double) recyclerMeasuredWidth / (double) firstMeasuredItemWidth);

        return colCount * spanCount;
      default:
        final int rowCount = (int) Math.ceil(
            (double) recyclerMeasuredHeight / (double) firstMeasuredItemHeight);

        return rowCount * spanCount;
    }
  }

  /**
   * @param widthSpec the widthSpec used to measure the parent {@link RecyclerSpec}.
   * @return widthSpec of a child that is of span size 1
   */
  @Override
  public int getChildWidthSpec(int widthSpec) {
    switch (mGridLayoutManager.getOrientation()) {
      case GridLayoutManager.HORIZONTAL:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
      default:
        final int spanCount = mGridLayoutManager.getSpanCount();
        return SizeSpec.makeSizeSpec(SizeSpec.getSize(widthSpec) / spanCount, SizeSpec.EXACTLY);
    }
  }

  /**
   * @param heightSpec the heightSpec used to measure the parent {@link RecyclerSpec}.
   * @return heightSpec of a child that is of span size 1
   */
  @Override
  public int getChildHeightSpec(int heightSpec) {
    switch (mGridLayoutManager.getOrientation()) {
      case GridLayoutManager.HORIZONTAL:
        final int spanCount = mGridLayoutManager.getSpanCount();
        return SizeSpec.makeSizeSpec(SizeSpec.getSize(heightSpec) / spanCount, SizeSpec.EXACTLY);
      default:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    }
  }

  @Override
  public int getSpanCount() {
    return mGridLayoutManager.getSpanCount();
  }
}
