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

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.GridLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a
 * {@link android.support.v7.widget.GridLayoutManager} for the {@link RecyclerView}.
 */
public class GridRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {

  private static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
      new RecyclerBinderConfiguration(4.0);

  private final int mOrientation;
  private final int mNumColumns;
  private final boolean mReverseLayout;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;
  private final boolean mAllowMeasureOverride;

  /**
   * Static factory method to create a recycler configuration
   * with incremental mount optionally turned on.
   */
  public static GridRecyclerConfiguration createWithRecyclerBinderConfiguration(
      int numColumns,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {

    return new GridRecyclerConfiguration(
        LinearLayoutManager.VERTICAL,
        numColumns,
        false,
        recyclerBinderConfiguration);
  }

  public GridRecyclerConfiguration(int numColumns) {
    this(LinearLayoutManager.VERTICAL, numColumns, false);
  }

  public GridRecyclerConfiguration(int orientation, int numColumns, boolean reverseLayout) {
    this(
        orientation,
        numColumns,
        reverseLayout,
        RECYCLER_BINDER_CONFIGURATION);
  }

  public GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    this(orientation, numColumns, reverseLayout, recyclerBinderConfiguration, false);
  }

  public GridRecyclerConfiguration(
      int orientation,
      int numColumns,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration,
      boolean allowMeasureOverride) {
    mOrientation = orientation;
    mNumColumns = numColumns;
    mReverseLayout = reverseLayout;
    mRecyclerBinderConfiguration = recyclerBinderConfiguration == null
        ? RECYCLER_BINDER_CONFIGURATION
        : recyclerBinderConfiguration;
    mAllowMeasureOverride = allowMeasureOverride;
  }

  @Override
  public T buildTarget(ComponentContext c) {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio((float) mRecyclerBinderConfiguration.getRangeRatio())
            .layoutInfo(
                new GridLayoutInfo(
                    c, mNumColumns, mOrientation, mReverseLayout, mAllowMeasureOverride))
            .layoutHandlerFactory(mRecyclerBinderConfiguration.getLayoutHandlerFactory())
            .wrapContent(mRecyclerBinderConfiguration.isWrapContent())
            .fillListViewport(mRecyclerBinderConfiguration.getFillListViewport())
            .fillListViewportHScrollOnly(
                mRecyclerBinderConfiguration.getFillListViewportHScrollOnly())
            .threadPoolForParallelFillViewportConfig(
                mRecyclerBinderConfiguration.getThreadPoolForParallelFillViewportConfig())
            .enableStableIds(mRecyclerBinderConfiguration.getEnableStableIds())
            .invalidStateLogParamsList(mRecyclerBinderConfiguration.getInvalidStateLogParamsList())
            .useSharedLayoutStateFuture(
                mRecyclerBinderConfiguration.getUseSharedLayoutStateFuture())
            .build(c);
    return (T)
        new SectionBinderTarget(
            recyclerBinder, mRecyclerBinderConfiguration.getUseAsyncMutations());
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    return null;
  }

  @Override
  public int getOrientation() {
    return mOrientation;
  }

  @Override
  public boolean isWrapContent() {
    return mRecyclerBinderConfiguration.isWrapContent();
  }
}
