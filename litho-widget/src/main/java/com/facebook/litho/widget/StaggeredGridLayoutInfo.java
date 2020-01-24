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

import static androidx.recyclerview.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.facebook.litho.LithoView;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.widget.RecyclerBinder.RecyclerViewLayoutManagerOverrideParams;
import java.lang.ref.WeakReference;
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
    this(spanCount, orientation, reverseLayout, gapStrategy, false, false);
  }

  public StaggeredGridLayoutInfo(
      int spanCount,
      int orientation,
      boolean reverseLayout,
      int gapStrategy,
      boolean eagerlyClearsSpanAssignmentsOnUpdates,
      boolean invalidatesItemDecorationsOnUpdates) {
    mStaggeredGridLayoutManager =
        new LithoStaggeredGridLayoutManager(
            spanCount,
            orientation,
            eagerlyClearsSpanAssignmentsOnUpdates,
            invalidatesItemDecorationsOnUpdates);
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
    private boolean mEagerlyClearsSpanAssignmentsOnUpdates;
    private boolean mInvalidatesItemDecorationsOnUpdates;

    // Pairs with mInvalidatesItemDecorationsOnUpdates to store the RecyclerView requiring
    // invalidation, since the RecyclerView isn't available as a member.
    private WeakReference<RecyclerView> mRecyclerViewToInvalidateItemDecorations =
        new WeakReference<>(null);

    public LithoStaggeredGridLayoutManager(
        int spanCount,
        int orientation,
        boolean eagerlyClearsSpanAssignmentsOnUpdates,
        boolean invalidatesItemDecorationsOnUpdates) {
      super(spanCount, orientation);

      mEagerlyClearsSpanAssignmentsOnUpdates = eagerlyClearsSpanAssignmentsOnUpdates;
      mInvalidatesItemDecorationsOnUpdates = invalidatesItemDecorationsOnUpdates;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
      if (lp instanceof RecyclerViewLayoutManagerOverrideParams) {
        return new LayoutParams((RecyclerViewLayoutManagerOverrideParams) lp);
      } else {
        return super.generateLayoutParams(lp);
      }
    }

    @Override
    public void onItemsRemoved(final RecyclerView recyclerView, int positionStart, int itemCount) {
      invalidateSpanAssignmentsEagerlyIfNeeded(recyclerView);
      prepareToInvalidateItemDecorationsIfNeeded(recyclerView);

      super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsAdded(final RecyclerView recyclerView, int positionStart, int itemCount) {
      invalidateSpanAssignmentsEagerlyIfNeeded(recyclerView);
      prepareToInvalidateItemDecorationsIfNeeded(recyclerView);

      super.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsMoved(final RecyclerView recyclerView, int from, int to, int itemCount) {
      invalidateSpanAssignmentsEagerlyIfNeeded(recyclerView);
      prepareToInvalidateItemDecorationsIfNeeded(recyclerView);

      super.onItemsMoved(recyclerView, from, to, itemCount);
    }

    @Override
    public void onItemsUpdated(
        final RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
      invalidateSpanAssignmentsEagerlyIfNeeded(recyclerView);
      prepareToInvalidateItemDecorationsIfNeeded(recyclerView);

      super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
    }

    @Override
    public void onLayoutCompleted(final RecyclerView.State recyclerViewState) {
      super.onLayoutCompleted(recyclerViewState);

      @Nullable
      final RecyclerView recyclerViewToInvalidateItemDecorations =
          mRecyclerViewToInvalidateItemDecorations.get();
      if (recyclerViewToInvalidateItemDecorations != null) {
        // Post to ensure we're not in a layout pass (otherwise we'll get an exception for calling
        // this directly inside the layout completion).
        recyclerViewToInvalidateItemDecorations
            .getHandler()
            .postAtFrontOfQueue(
                new Runnable() {
                  @Override
                  public void run() {
                    if (recyclerViewToInvalidateItemDecorations.isComputingLayout()) {
                      return;
                    }

                    recyclerViewToInvalidateItemDecorations.invalidateItemDecorations();
                  }
                });
        mRecyclerViewToInvalidateItemDecorations.clear();
      }
    }

    private void prepareToInvalidateItemDecorationsIfNeeded(RecyclerView recyclerView) {
      // When an item decorator is called for a staggered layout, the params for span info (index,
      // full width) are retrieved from the view. However, the params may not have been updated yet
      // to reflect the latest ordering and resultant layout changes. As a result, the span indices
      // can be inaccurate and result in broken layouts. Enabling this param  works around (perhaps
      // inefficiently) by invalidating the item decorations on the next successful layout. Then,
      // the values will be updated and the decorations will be applied correctly.
      if (mInvalidatesItemDecorationsOnUpdates) {
        // The layout completion callback will be invoked on the layout info, but without a
        // reference to the recycler view, so we need to store it ourselves temporarily.
        mRecyclerViewToInvalidateItemDecorations = new WeakReference<>(recyclerView);
      }
    }

    private void invalidateSpanAssignmentsEagerlyIfNeeded(RecyclerView recyclerView) {
      // StaggeredGridLayoutManager uses full spans to limit the extent it needs to reflow the rest
      // of a grid layout (it stops after bumping into the next full span). However, there seem to
      // be logical errors with this (or non-trivial bugs in our usage), so this param invalidates
      // the span assignment cache, which avoids the logical errors / crashes. In general, this is
      // likely terrible for performance (unbenchmarked), so it should be enabled with caution.
      if (mEagerlyClearsSpanAssignmentsOnUpdates) {
        this.onItemsChanged(recyclerView);
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
