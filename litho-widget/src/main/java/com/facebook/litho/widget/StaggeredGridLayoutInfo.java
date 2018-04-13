/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ViewGroup;
import com.facebook.litho.LithoView;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.widget.RecyclerBinder.RecyclerViewLayoutManagerOverrideParams;

/**
 * An implementation for {@link LayoutInfo} to implement staggered grids with a {@link
 * StaggeredGridLayoutManager}.
 */
public class StaggeredGridLayoutInfo implements LayoutInfo {
  // A custom LayoutInfo param to override the size of an item in the grid. Since
  // StaggeredGridLayoutInfo does not support item decorations offsets on the non scrolling side
  // natively, this can be useful to manually compute the size of an item after such decorations are
  // taken into account.
  public static final String OVERRIDE_SIZE = "OVERRIDE_SIZE";

  private final StaggeredGridLayoutManager mStaggeredGridLayoutManager;

  public StaggeredGridLayoutInfo(
      int spanCount, int orientation, boolean reverseLayout, int gapStrategy) {
    mStaggeredGridLayoutManager = new LithoStaggeredGridLayoutManager(spanCount, orientation);
    mStaggeredGridLayoutManager.setReverseLayout(reverseLayout);
    mStaggeredGridLayoutManager.setGapStrategy(gapStrategy);
  }

  @Override
  public int getScrollDirection() {
    return mStaggeredGridLayoutManager.getOrientation();
  }

  @Override
  public RecyclerView.LayoutManager getLayoutManager() {
    return mStaggeredGridLayoutManager;
  }

  @Override
  public void setRenderInfoCollection(RenderInfoCollection renderInfoCollection) {
    // no op
  }

  @Override
  public int approximateRangeSize(
      int firstMeasuredItemWidth,
      int firstMeasuredItemHeight,
      int recyclerMeasuredWidth,
      int recyclerMeasuredHeight) {
    final int spanCount = mStaggeredGridLayoutManager.getSpanCount();
    switch (mStaggeredGridLayoutManager.getOrientation()) {
      case StaggeredGridLayoutManager.HORIZONTAL:
        final int colCount =
            (int) Math.ceil((double) recyclerMeasuredWidth / (double) firstMeasuredItemWidth);

        return colCount * spanCount;
      default:
        final int rowCount =
            (int) Math.ceil((double) recyclerMeasuredHeight / (double) firstMeasuredItemHeight);

        return rowCount * spanCount;
    }
  }

  /**
   * @param widthSpec the widthSpec used to measure the parent {@link RecyclerSpec}.
   * @return widthSpec of a child that is of span size 1
   */
  @Override
  public int getChildWidthSpec(int widthSpec, RenderInfo renderInfo) {
    switch (mStaggeredGridLayoutManager.getOrientation()) {
      case StaggeredGridLayoutManager.HORIZONTAL:
        return SizeSpec.makeSizeSpec(0, UNSPECIFIED);
      default:
        Integer overrideWidth = (Integer) renderInfo.getCustomAttribute(OVERRIDE_SIZE);
        if (overrideWidth != null) {
          return SizeSpec.makeSizeSpec(overrideWidth, EXACTLY);
        }

        final int spanCount = mStaggeredGridLayoutManager.getSpanCount();
        final int spanSize =
            renderInfo.isFullSpan() ? mStaggeredGridLayoutManager.getSpanCount() : 1;

        return SizeSpec.makeSizeSpec(
            spanSize * ((SizeSpec.getSize(widthSpec)) / spanCount), EXACTLY);
    }
  }

  /**
   * @param heightSpec the heightSpec used to measure the parent {@link RecyclerSpec}.
   * @return heightSpec of a child that is of span size 1
   */
  @Override
  public int getChildHeightSpec(int heightSpec, RenderInfo renderInfo) {
    switch (mStaggeredGridLayoutManager.getOrientation()) {
      case StaggeredGridLayoutManager.HORIZONTAL:
        Integer overrideHeight = (Integer) renderInfo.getCustomAttribute(OVERRIDE_SIZE);
        if (overrideHeight != null) {
          return SizeSpec.makeSizeSpec(overrideHeight, EXACTLY);
        }

        final int spanCount = mStaggeredGridLayoutManager.getSpanCount();
        final int spanSize =
            renderInfo.isFullSpan() ? mStaggeredGridLayoutManager.getSpanCount() : 1;

        return SizeSpec.makeSizeSpec(
            spanSize * (SizeSpec.getSize(heightSpec) / spanCount), EXACTLY);
      default:
        return SizeSpec.makeSizeSpec(0, UNSPECIFIED);
    }
  }

  @Override
  public ViewportFiller createViewportFiller(int measuredWidth, int measuredHeight) {
    // TODO(t28219976): Implement ViewportFiller for StaggeredGridLayout
    return null;
  }

  @Override
  public int findFirstVisibleItemPosition() {
    return StaggeredGridLayoutHelper.findFirstVisibleItemPosition(mStaggeredGridLayoutManager);
  }

  @Override
  public int findLastVisibleItemPosition() {
    return StaggeredGridLayoutHelper.findLastVisibleItemPosition(mStaggeredGridLayoutManager);
  }

  @Override
  public int findFirstFullyVisibleItemPosition() {
    return StaggeredGridLayoutHelper.findFirstFullyVisibleItemPosition(mStaggeredGridLayoutManager);
  }

  @Override
  public int findLastFullyVisibleItemPosition() {
    return StaggeredGridLayoutHelper.findLastFullyVisibleItemPosition(mStaggeredGridLayoutManager);
  }

  @Override
  public int getItemCount() {
    return mStaggeredGridLayoutManager.getItemCount();
  }

  private static class LithoStaggeredGridLayoutManager extends StaggeredGridLayoutManager {
    public LithoStaggeredGridLayoutManager(int spanCount, int orientation) {
      super(spanCount, orientation);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
      if (lp instanceof RecyclerViewLayoutManagerOverrideParams) {
        return new LayoutParams((RecyclerViewLayoutManagerOverrideParams) lp);
      } else {
        return super.generateLayoutParams(lp);
      }
    }

    public static class LayoutParams extends StaggeredGridLayoutManager.LayoutParams
        implements LithoView.LayoutManagerOverrideParams {
      private final int mOverrideWidthMeasureSpec;
      private final int mOverrideHeightMeasureSpec;

      public LayoutParams(RecyclerViewLayoutManagerOverrideParams source) {
        super(source);
        setFullSpan(source.isFullSpan());
        mOverrideWidthMeasureSpec = source.getWidthMeasureSpec();
        mOverrideHeightMeasureSpec = source.getHeightMeasureSpec();
      }

      @Override
      public int getWidthMeasureSpec() {
        return mOverrideWidthMeasureSpec;
      }

      @Override
      public int getHeightMeasureSpec() {
        return mOverrideHeightMeasureSpec;
      }
    }
  }
}
