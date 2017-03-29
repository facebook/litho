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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

import com.facebook.litho.SizeSpec;
import com.facebook.litho.utils.IncrementalMountUtils;

import static com.facebook.litho.SizeSpec.UNSPECIFIED;

public abstract class LinearComponentBinder extends
    RecyclerComponentBinder<
        LinearLayoutManager,
        RecyclerComponentBinder.RecyclerComponentWorkingRangeController>
    implements HasStickyHeader {

  private int mInitFillMainAxis;

  public LinearComponentBinder(Context context, LinearLayoutManager layoutManager) {
    this(context, layoutManager, new RecyclerComponentWorkingRangeController());
  }

  public LinearComponentBinder(Context context, LinearLayoutManager layoutManager,
      RecyclerComponentWorkingRangeController rangeController) {
    super(context, layoutManager, rangeController);
  }

  @Override
  protected void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);
    final LinearLayoutManager layoutManager = getLayoutManager();
    final int firstViewPosition = layoutManager.findFirstVisibleItemPosition();
    final int lastViewPosition = layoutManager.findLastVisibleItemPosition();

    if (firstViewPosition == RecyclerView.NO_POSITION ||
        lastViewPosition == RecyclerView.NO_POSITION) {
      return;
    }

    if (isIncrementalMountEnabled()) {
      IncrementalMountUtils.performIncrementalMount(recyclerView);
    }

    getRangeController().notifyOnScroll(
        firstViewPosition,
        lastViewPosition - firstViewPosition + 1);
  }

  @Override
  protected int getWidthSpec(int position) {
    if (getLayoutManager().getOrientation() == LinearLayoutManager.VERTICAL) {
      return super.getWidthSpec(position);
    } else {
      return SizeSpec.makeSizeSpec(0, UNSPECIFIED);
    }
  }

  @Override
  protected int getHeightSpec(int position) {
    if (getLayoutManager().getOrientation() == LinearLayoutManager.VERTICAL) {
      return SizeSpec.makeSizeSpec(0, UNSPECIFIED);
    } else {
      return super.getHeightSpec(position);
    }
  }

  @Override
  protected boolean shouldContinueInitialization(
      int position,
      int itemWidth,
      int itemHeight) {
    final int itemMainAxisSize;
    final int recyclerMainAxisSize;
    // Assuming the range is starting always from the first position, the initial range consists
    // of the visible viewport plus # viewports after depending by the WorkingRange size.
    final int viewportsToInit = 1 + (RecyclerComponentWorkingRangeController.RANGE_SIZE - 1) / 2;
    if (getInitializeStartPosition() == position) {
      mInitFillMainAxis = 0;
    }

    if (getLayoutManager().getOrientation() == OrientationHelper.VERTICAL) {
      itemMainAxisSize = itemHeight;
      recyclerMainAxisSize = getHeight();
    } else {
      itemMainAxisSize = itemWidth;
      recyclerMainAxisSize = getWidth();
    }

    mInitFillMainAxis += itemMainAxisSize;

    // Check if the initial range is filled. The initial range consists of the viewport and the
    // the items that should be loaded after the currently visible ones. Since the initial range
    // always starts at the first position, there are no items before the viewport.
    return mInitFillMainAxis < recyclerMainAxisSize * viewportsToInit;
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return getLayoutManager().findFirstVisibleItemPosition();
  }

  @Override
  public int findLastVisibleItemPosition() {
    return getLayoutManager().findLastVisibleItemPosition();
  }

  @Override
  public boolean isSticky(int position) {
    return false;
  }
}
