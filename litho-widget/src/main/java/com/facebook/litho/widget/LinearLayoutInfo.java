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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.recyclerview.widget.OrientationHelper.VERTICAL;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.SizeSpec;
import java.util.List;

/**
 * An implementation for {@link LayoutInfo} to implement linear lists with a {@link
 * LinearLayoutManager}.
 */
public class LinearLayoutInfo implements LayoutInfo {

  private static final int MAX_SANE_RANGE = 10;
  private static final int MIN_SANE_RANGE = 2;

  private final LinearLayoutManager mLinearLayoutManager;

  public LinearLayoutInfo(LinearLayoutManager linearLayoutManager) {
    mLinearLayoutManager = linearLayoutManager;
  }

  public LinearLayoutInfo(ComponentContext context, int orientation, boolean reverseLayout) {
    this(context.getAndroidContext(), orientation, reverseLayout);
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
  public void setRenderInfoCollection(@Nullable RenderInfoCollection renderInfoCollection) {
    // Do nothing
  }

  @Override
  public void scrollToPositionWithOffset(int position, int offset) {
    mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
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
        approximateRange =
            (int) Math.ceil((float) recyclerMeasuredWidth / (float) firstMeasuredItemWidth);
        break;
      default:
        approximateRange =
            (int) Math.ceil((float) recyclerMeasuredHeight / (float) firstMeasuredItemHeight);
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

  @Override
  public ViewportFiller createViewportFiller(int measuredWidth, int measuredHeight) {
    return new ViewportFiller(measuredWidth, measuredHeight, getScrollDirection());
  }

  @Override
  public int computeWrappedHeight(int maxHeight, List<ComponentTreeHolder> componentTreeHolders) {
    return LayoutInfoUtils.computeLinearLayoutWrappedHeight(
        mLinearLayoutManager, maxHeight, componentTreeHolders);
  }

  private static class InternalLinearLayoutManager extends LinearLayoutManager {

    InternalLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
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

  /**
   * Simple implementation of {@link LayoutInfo.ViewportFiller} which fills the viewport by laying
   * out items sequentially.
   */
  public static class ViewportFiller implements LayoutInfo.ViewportFiller {

    private final int mWidth;
    private final int mHeight;
    private final int mOrientation;
    private int mFill;

    public ViewportFiller(int width, int height, int orientation) {
      mWidth = width;
      mHeight = height;
      mOrientation = orientation;
    }

    @Override
    public boolean wantsMore() {
      final int target = mOrientation == VERTICAL ? mHeight : mWidth;
      return mFill < target;
    }

    @Override
    public void add(RenderInfo renderInfo, int width, int height) {
      mFill += mOrientation == VERTICAL ? height : width;
    }

    @Override
    public int getFill() {
      return mFill;
    }
  }
}
