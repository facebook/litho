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

import com.facebook.litho.ComponentView;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.utils.IncrementalMountUtils;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * WARNING: In order to benefit from async layout, all the items in a row need to have
 *          the same height. If items in the same row have different heights, the layout will
 *          be recomputed on the main thread.
 *          The use of {@link android.support.v7.widget.RecyclerView.ItemDecoration} will
 *          also prevent the layout pass to be computed in the background but it will run
 *          on the main thread.
 */
public abstract class GridComponentBinder extends RecyclerComponentBinder<
    GridLayoutManager,
    RecyclerComponentBinder.RecyclerComponentWorkingRangeController> {

  private int mInitFillMainAxis;
  private int mInitFillCrossAxis;

  public GridComponentBinder(Context context, GridLayoutManager layoutManager) {
    this(context, layoutManager, new RecyclerComponentWorkingRangeController());
  }

  public GridComponentBinder(
      Context context,
      GridLayoutManager layoutManager,
      RecyclerComponentWorkingRangeController rangeController) {
    super(context, layoutManager, rangeController);
  }

  /**
   * ComponentView.performIncrementalMount() checks visible bounds of the View
   * and incrementally mounts/unmounts.
   * When a view is completely visible don't need to check the bounds to unmount some stuff.
   * But only the Views at the beginning/end of the scrolling parent need to check if it needs to
   * mount/unmount stuff.
   *
   * Example with 6 items: items 0 and 1 partially visible, item 2 and 3 completely visible,
   * item 4 and 5 partially visible.
   *                  _______
   * ------------     |__|__|
   * ViewPort         |__|__|
   * ------------     |__|__|
   *
   *
   * Example with 4 items so big that in the center there are no items completely visible...
   *                _________
   * ------------   |   |   |
   * ViewPort       |___|___|
   * ------------   |   |   |
   *                |___|___|
   */
  @Override
  protected void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);

    final GridLayoutManager layoutManager = getLayoutManager();

    final int firstItemPosition = layoutManager.findFirstVisibleItemPosition();
    final int lastItemPosition = layoutManager.findLastVisibleItemPosition();

    if (firstItemPosition == RecyclerView.NO_POSITION ||
        lastItemPosition == RecyclerView.NO_POSITION) {
      return;
    }

    getRangeController().notifyOnScroll(
        firstItemPosition,
        lastItemPosition - firstItemPosition + 1);

    if (isIncrementalMountEnabled()) {
      IncrementalMountUtils.performIncrementalMount(recyclerView);
    }
  }

  /**
   * Returns the index of the span in which the item at the position given as parameter is
   * situated.
   */
  public int getSpanIndex(int position) {
    GridLayoutManager layoutManager = getLayoutManager();
    return layoutManager.getSpanSizeLookup().getSpanIndex(position, layoutManager.getSpanCount());
  }

  /**
   * Perform incremental mount on positions [fromPosition, toPosition).
   */
  private void performIncrementalMountOnRange(int fromPosition, int toPosition) {
    final GridLayoutManager layoutManager = getLayoutManager();

    for (int i = fromPosition; i < toPosition; i++) {
      ComponentView partiallyVisibleView = (ComponentView) layoutManager.findViewByPosition(i);
      if (partiallyVisibleView != null) {
        partiallyVisibleView.performIncrementalMount();
      }
    }
  }

  @Override
  public int getWidthSpec(int position) {
    final GridLayoutManager layoutManager = getLayoutManager();
    final int spanCount = layoutManager.getSpanCount();
    final int itemSpanCount = layoutManager.getSpanSizeLookup().getSpanSize(position);

    if (layoutManager.getOrientation() == OrientationHelper.VERTICAL) {
      return SizeSpec.makeSizeSpec(
          (SizeSpec.getSize(super.getWidthSpec(position)) / spanCount) * itemSpanCount,
          EXACTLY);
    } else {
