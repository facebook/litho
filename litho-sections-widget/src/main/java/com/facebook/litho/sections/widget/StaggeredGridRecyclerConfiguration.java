/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.StaggeredGridLayoutInfo;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a {@link
 * android.support.v7.widget.StaggeredGridLayoutManager} for the {@link RecyclerView}.
 */
public class StaggeredGridRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {
  private static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
      new RecyclerBinderConfiguration(4.0);

  private final int mNumSpans;
  private final int mOrientation;
  private final boolean mReverseLayout;
  private final int mGapStrategy;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;

  public static StaggeredGridRecyclerConfiguration createWithRecyclerBinderConfiguration(
      int numSpans, RecyclerBinderConfiguration recyclerBinderConfiguration) {
    return new StaggeredGridRecyclerConfiguration(
        numSpans,
        StaggeredGridLayoutManager.VERTICAL,
        false,
        StaggeredGridLayoutManager.GAP_HANDLING_NONE,
        recyclerBinderConfiguration);
  }

  public StaggeredGridRecyclerConfiguration(int numSpans) {
    this(numSpans, StaggeredGridLayoutManager.VERTICAL, false);
  }

  public StaggeredGridRecyclerConfiguration(int numSpans, int orientation, boolean reverseLayout) {
    this(numSpans, orientation, reverseLayout, RECYCLER_BINDER_CONFIGURATION);
  }

  public StaggeredGridRecyclerConfiguration(
      int numSpans,
      int orientation,
      boolean reverseLayout,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    this(
        numSpans,
        orientation,
        reverseLayout,
        StaggeredGridLayoutManager.GAP_HANDLING_NONE,
        recyclerBinderConfiguration);
  }

  public StaggeredGridRecyclerConfiguration(
      int numSpans,
      int orientation,
      boolean reverseLayout,
      int gapStrategy,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    mNumSpans = numSpans;
    mOrientation = orientation;
    mReverseLayout = reverseLayout;
    mGapStrategy = gapStrategy;
    mRecyclerBinderConfiguration =
        recyclerBinderConfiguration == null
            ? RECYCLER_BINDER_CONFIGURATION
            : recyclerBinderConfiguration;
  }

  @Override
  public T buildTarget(ComponentContext c) {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio((float) mRecyclerBinderConfiguration.getRangeRatio())
            .layoutInfo(
                new StaggeredGridLayoutInfo(mNumSpans, mOrientation, mReverseLayout, mGapStrategy))
            .layoutHandlerFactory(mRecyclerBinderConfiguration.getLayoutHandlerFactory())
            .build(c);
    return (T) new SectionBinderTarget(recyclerBinder);
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    return null;
  }

  @Override
  public int getOrientation() {
    return mOrientation;
  }
}
