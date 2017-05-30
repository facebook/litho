/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.facebook.litho.ComponentInfo;
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
  public void setComponentInfoCollection(ComponentInfoCollection componentInfoCollection) {
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
  public int getChildHeightSpec(int heightSpec, ComponentInfo componentInfo) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return heightSpec;
      default:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    }
  }

  @Override
  public int getChildWidthSpec(int widthSpec, ComponentInfo componentInfo) {
    switch (mLinearLayoutManager.getOrientation()) {
      case LinearLayoutManager.HORIZONTAL:
        return SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
      default:
        return widthSpec;
    }
  }
}
