/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections.widget;

import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static com.facebook.litho.widget.SnapUtil.SnapMode;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.GridLayoutInfo;
import com.facebook.litho.widget.LayoutInfo;
import com.facebook.litho.widget.SnapUtil;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a {@link
 * androidx.recyclerview.widget.GridLayoutManager} for the {@link RecyclerView}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class GridRecyclerConfiguration implements RecyclerConfiguration {
  private final int mOrientation;
  private final int mNumColumns;
  private final @SnapMode int mSnapMode;
  private final @Nullable SnapHelper mSnapHelper;
  private final boolean mReverseLayout;
  private final boolean mStackFromEnd;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;
  private final GridLayoutInfoFactory mGridLayoutInfoFactory;
  private final boolean mAllowMeasureOverride;

  public static GridRecyclerConfiguration.Builder create() {
    return new Builder();
  }

  private GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      boolean stackFromEnd,
      RecyclerBinderConfiguration recyclerBinderConfiguration,
      boolean allowMeasureOverride,
      @Nullable GridLayoutInfoFactory gridLayoutInfoFactory,
      @SnapMode int snapMode,
      @Nullable SnapHelper snapHelper) {
    mOrientation = orientation;
    mNumColumns = numColumns;
    mReverseLayout = reverseLayout;
    mStackFromEnd = stackFromEnd;
    mRecyclerBinderConfiguration = recyclerBinderConfiguration;
    mAllowMeasureOverride = allowMeasureOverride;
    mGridLayoutInfoFactory =
        gridLayoutInfoFactory == null
            ? GridRecyclerConfiguration.Builder.GRID_LAYOUT_INFO_FACTORY
            : gridLayoutInfoFactory;
    mSnapMode = snapMode;
    mSnapHelper = snapHelper;
  }

  @Override
  public Builder acquireBuilder() {
    return new Builder(this);
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    return mSnapHelper;
  }

  @Override
  public int getSnapMode() {
    return mSnapMode;
  }

  @Override
  public int getOrientation() {
    return mOrientation;
  }

  @Override
  public boolean getReverseLayout() {
    return mReverseLayout;
  }

  @Override
  public boolean getStackFromEnd() {
    return mStackFromEnd;
  }

  @Override
  public LayoutInfo getLayoutInfo(ComponentContext c) {
    return mGridLayoutInfoFactory.createGridLayoutInfo(
        c.getAndroidContext(), mNumColumns, mOrientation, mReverseLayout, mAllowMeasureOverride);
  }

  @Override
  public RecyclerBinderConfiguration getRecyclerBinderConfiguration() {
    return mRecyclerBinderConfiguration;
  }

  private static class DefaultGridLayoutInfoFactory implements GridLayoutInfoFactory {
    @Override
    public GridLayoutInfo createGridLayoutInfo(
        Context c,
        int spanCount,
        int orientation,
        boolean reverseLayout,
        boolean allowGridMeasuresOverride) {
      return new GridLayoutInfo(
          c, spanCount, orientation, reverseLayout, allowGridMeasuresOverride);
    }
  }

  public static class Builder implements RecyclerConfiguration.Builder {
    static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
        RecyclerBinderConfiguration.create().build();
    static final GridLayoutInfoFactory GRID_LAYOUT_INFO_FACTORY =
        new DefaultGridLayoutInfoFactory();

    private int mOrientation = LinearLayoutManager.VERTICAL;
    private int mNumColumns = 2;
    private boolean mReverseLayout = false;
    private boolean mStackFromEnd = false;
    private boolean mAllowMeasureOverride = false;
    private RecyclerBinderConfiguration mRecyclerBinderConfiguration =
        RECYCLER_BINDER_CONFIGURATION;
    private GridLayoutInfoFactory mGridLayoutInfoFactory = GRID_LAYOUT_INFO_FACTORY;
    private int mDeltaJumpThreshold = Integer.MAX_VALUE;
    private int mStartSnapFlingOffset = SnapUtil.SNAP_TO_START_DEFAULT_FLING_OFFSET;
    private @SnapMode int mSnapMode = SNAP_NONE;
    private @Nullable SnapHelper mSnapHelper;

    Builder() {}

    Builder(GridRecyclerConfiguration gridRecyclerConfiguration) {
      this.mOrientation = gridRecyclerConfiguration.mOrientation;
      this.mNumColumns = gridRecyclerConfiguration.mNumColumns;
      this.mReverseLayout = gridRecyclerConfiguration.mReverseLayout;
      this.mStackFromEnd = gridRecyclerConfiguration.mStackFromEnd;
      this.mAllowMeasureOverride = gridRecyclerConfiguration.mAllowMeasureOverride;
      this.mRecyclerBinderConfiguration = gridRecyclerConfiguration.mRecyclerBinderConfiguration;
      this.mGridLayoutInfoFactory = gridRecyclerConfiguration.mGridLayoutInfoFactory;
      this.mSnapMode = gridRecyclerConfiguration.mSnapMode;
      this.mSnapHelper = gridRecyclerConfiguration.mSnapHelper;
    }

    @Override
    public Builder snapMode(@SnapUtil.SnapMode int snapMode) {
      mSnapMode = snapMode;
      return this;
    }

    @Override
    public Builder orientation(int orientation) {
      mOrientation = orientation;
      return this;
    }

    public Builder deltaJumpThreshold(int deltaJumpThreshold) {
      mDeltaJumpThreshold = deltaJumpThreshold;
      return this;
    }

    public Builder startSnapFlingOffset(int startSnapFlingOffset) {
      mStartSnapFlingOffset = startSnapFlingOffset;
      return this;
    }

    public Builder numColumns(int numColumns) {
      mNumColumns = numColumns;
      return this;
    }

    @Override
    public Builder reverseLayout(boolean reverseLayout) {
      mReverseLayout = reverseLayout;
      return this;
    }

    @Override
    public Builder stackFromEnd(boolean stackFromEnd) {
      mStackFromEnd = stackFromEnd;
      return this;
    }

    @Override
    public Builder recyclerBinderConfiguration(
        RecyclerBinderConfiguration recyclerBinderConfiguration) {
      mRecyclerBinderConfiguration = recyclerBinderConfiguration;
      return this;
    }

    public Builder allowMeasureOverride(boolean allowMeasureOverride) {
      mAllowMeasureOverride = allowMeasureOverride;
      return this;
    }

    public Builder snapHelper(SnapHelper snapHelper) {
      mSnapHelper = snapHelper;
      return this;
    }

    /**
     * Provide a customized {@link GridLayoutInfo} through {@link GridLayoutInfoFactory} interface.
     */
    public Builder gridLayoutInfoFactory(GridLayoutInfoFactory gridLayoutInfoFactory) {
      mGridLayoutInfoFactory = gridLayoutInfoFactory;
      return this;
    }

    private static void validate(GridRecyclerConfiguration configuration) {
      int snapMode = configuration.getSnapMode();
      if (configuration.getOrientation() == OrientationHelper.VERTICAL
          && !(snapMode == SNAP_NONE || snapMode == SnapUtil.SNAP_TO_START)) {
        throw new UnsupportedOperationException(
            "Only snap to start is implemented for vertical lists");
      }
    }

    /**
     * Builds a {@link GridRecyclerConfiguration} using the parameters specified in this builder.
     */
    @Override
    public GridRecyclerConfiguration build() {
      final SnapHelper snapHelper =
          (mSnapHelper != null)
              ? mSnapHelper
              : SnapUtil.getSnapHelper(mSnapMode, mDeltaJumpThreshold, mStartSnapFlingOffset);
      final GridRecyclerConfiguration configuration =
          new GridRecyclerConfiguration(
              mOrientation,
              mNumColumns,
              mReverseLayout,
              mStackFromEnd,
              mRecyclerBinderConfiguration,
              mAllowMeasureOverride,
              mGridLayoutInfoFactory,
              mSnapMode,
              snapHelper);
      validate(configuration);
      return configuration;
    }
  }
}
