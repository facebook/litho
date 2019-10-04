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

package com.facebook.litho.sections.widget;

import static com.facebook.litho.widget.SnapUtil.SNAP_NONE;
import static com.facebook.litho.widget.SnapUtil.SnapMode;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.GridLayoutInfo;
import com.facebook.litho.widget.LayoutInfo;
import com.facebook.litho.widget.SnapUtil;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a {@link
 * androidx.recyclerview.widget.GridLayoutManager} for the {@link RecyclerView}.
 */
public class GridRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {
  private final int mOrientation;
  private final int mNumColumns;
  private final @SnapMode int mSnapMode;
  private final boolean mReverseLayout;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;
  private final GridLayoutInfoFactory mGridLayoutInfoFactory;
  private final boolean mAllowMeasureOverride;
  private int mDeltaJumpThreshold = Integer.MAX_VALUE;

  public static GridRecyclerConfiguration.Builder create() {
    return new Builder();
  }

  /**
   * Use {@link #create()} instead.
   *
   * <p>Static factory method to create a recycler configuration with incremental mount optionally
   * turned on.
   */
  @Deprecated
  public static GridRecyclerConfiguration createWithRecyclerBinderConfiguration(
      int numColumns, RecyclerBinderConfiguration recyclerBinderConfiguration) {

    return new GridRecyclerConfiguration(
        LinearLayoutManager.VERTICAL, numColumns, false, recyclerBinderConfiguration);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public GridRecyclerConfiguration(int numColumns) {
    this(LinearLayoutManager.VERTICAL, numColumns, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public GridRecyclerConfiguration(int orientation, int numColumns, boolean reverseLayout) {
    this(orientation, numColumns, reverseLayout, Builder.RECYCLER_BINDER_CONFIGURATION);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    this(orientation, numColumns, reverseLayout, recyclerBinderConfiguration, false);
  }

  @Deprecated
  public GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration,
      boolean allowMeasureOverride) {
    this(
        orientation,
        numColumns,
        reverseLayout,
        recyclerBinderConfiguration,
        allowMeasureOverride,
        Builder.GRID_LAYOUT_INFO_FACTORY,
        SNAP_NONE);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration,
      boolean allowMeasureOverride,
      @Nullable GridLayoutInfoFactory gridLayoutInfoFactory,
      @SnapMode int snapMode) {
    mOrientation = orientation;
    mNumColumns = numColumns;
    mReverseLayout = reverseLayout;
    mRecyclerBinderConfiguration =
        recyclerBinderConfiguration == null
            ? Builder.RECYCLER_BINDER_CONFIGURATION
            : recyclerBinderConfiguration;
    mAllowMeasureOverride = allowMeasureOverride;
    mGridLayoutInfoFactory =
        gridLayoutInfoFactory == null
            ? GridRecyclerConfiguration.Builder.GRID_LAYOUT_INFO_FACTORY
            : gridLayoutInfoFactory;
    mSnapMode = snapMode;
  }

  @Override
  public Builder acquireBuilder() {
    return new Builder(this);
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    return SnapUtil.getSnapHelper(mSnapMode, mDeltaJumpThreshold);
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
    private boolean mAllowMeasureOverride = false;
    private RecyclerBinderConfiguration mRecyclerBinderConfiguration =
        RECYCLER_BINDER_CONFIGURATION;
    private GridLayoutInfoFactory mGridLayoutInfoFactory = GRID_LAYOUT_INFO_FACTORY;
    private int mDeltaJumpThreshold = Integer.MAX_VALUE;
    private @SnapMode int mSnapMode = SNAP_NONE;

    Builder() {}

    Builder(GridRecyclerConfiguration gridRecyclerConfiguration) {
      this.mOrientation = gridRecyclerConfiguration.mOrientation;
      this.mNumColumns = gridRecyclerConfiguration.mNumColumns;
      this.mReverseLayout = gridRecyclerConfiguration.mReverseLayout;
      this.mAllowMeasureOverride = gridRecyclerConfiguration.mAllowMeasureOverride;
      this.mRecyclerBinderConfiguration = gridRecyclerConfiguration.mRecyclerBinderConfiguration;
      this.mGridLayoutInfoFactory = gridRecyclerConfiguration.mGridLayoutInfoFactory;
      this.mDeltaJumpThreshold = gridRecyclerConfiguration.mDeltaJumpThreshold;
      this.mSnapMode = gridRecyclerConfiguration.mSnapMode;
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

    public Builder numColumns(int numColumns) {
      mNumColumns = numColumns;
      return this;
    }

    public Builder reverseLayout(boolean reverseLayout) {
      mReverseLayout = reverseLayout;
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
      GridRecyclerConfiguration configuration =
          new GridRecyclerConfiguration(
              mOrientation,
              mNumColumns,
              mReverseLayout,
              mRecyclerBinderConfiguration,
              mAllowMeasureOverride,
              mGridLayoutInfoFactory,
              mSnapMode);
      configuration.mDeltaJumpThreshold = mDeltaJumpThreshold;
      validate(configuration);
      return configuration;
    }
  }
}
