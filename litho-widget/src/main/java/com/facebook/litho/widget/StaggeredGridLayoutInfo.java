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

import static androidx.recyclerview.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.facebook.litho.LithoView;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.widget.RecyclerBinder.RecyclerViewLayoutManagerOverrideParams;
import java.util.List;

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
    return new ViewportFiller(
        measuredWidth,
        measuredHeight,
        getScrollDirection(),
        mStaggeredGridLayoutManager.getSpanCount());
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

  @Override
  public int computeWrappedHeight(int maxHeight, List<ComponentTreeHolder> componentTreeHolders) {
    final int itemCount = componentTreeHolders.size();
    final int spanCount = mStaggeredGridLayoutManager.getSpanCount();

    int measuredHeight = 0;

    switch (mStaggeredGridLayoutManager.getOrientation()) {
      case StaggeredGridLayoutManager.VERTICAL:
        for (int i = 0; i < itemCount; i += spanCount) {
          measuredHeight +=
              LayoutInfoUtils.getMaxHeightInRow(i, i + spanCount, componentTreeHolders);
          measuredHeight += LayoutInfoUtils.getTopDecorationHeight(mStaggeredGridLayoutManager, i);
          measuredHeight +=
              LayoutInfoUtils.getBottomDecorationHeight(mStaggeredGridLayoutManager, i);

          if (measuredHeight > maxHeight) {
            measuredHeight = maxHeight;
            break;
          }
        }
        return measuredHeight;

      case StaggeredGridLayoutManager.HORIZONTAL:
      default:
        throw new IllegalStateException(
            "This method should only be called when orientation is vertical");
    }
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

      @Override
      public boolean hasValidAdapterPosition() {
        return false;
      }
    }
  }

  static class ViewportFiller implements LayoutInfo.ViewportFiller {

    private final int mWidth;
    private final int mHeight;
    private final int mOrientation;
    private final int mSpanCount;
    private int mIndexOfSpan;
    private int[] mFills;
    private int mMaxFill;

    public ViewportFiller(int width, int height, int orientation, int spanCount) {
      mWidth = width;
      mHeight = height;
      mOrientation = orientation;
      mSpanCount = spanCount;
      mFills = new int[spanCount];
    }

    @Override
    public boolean wantsMore() {
      final int target = mOrientation == VERTICAL ? mHeight : mWidth;
      return mMaxFill < target;
    }

    @Override
    public void add(RenderInfo renderInfo, int width, int height) {
      mFills[mIndexOfSpan] += mOrientation == VERTICAL ? height : width;
      mMaxFill = Math.max(mMaxFill, mFills[mIndexOfSpan]);

      if (++mIndexOfSpan == mSpanCount) {
        // Reset the index after exceeding the span.
        mIndexOfSpan = 0;
      }
    }

    @Override
    public int getFill() {
      return mMaxFill;
    }
  }
}
