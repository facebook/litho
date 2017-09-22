/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import static android.support.v7.widget.LinearSmoothScroller.SNAP_TO_END;
import static android.support.v7.widget.LinearSmoothScroller.SNAP_TO_START;

import android.support.annotation.IntDef;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.widget.RecyclerCollectionComponentSpec.RecyclerConfiguration;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a
 * {@link android.support.v7.widget.LinearLayoutManager} for the {@link RecyclerView}.
 */
public class ListRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {

  public static final int SNAP_TO_CENTER = Integer.MAX_VALUE;
  public static final int SNAP_NONE = Integer.MIN_VALUE;

  private static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
      new RecyclerBinderConfiguration(4.0);

  @IntDef({SNAP_NONE, SNAP_TO_END, SNAP_TO_START, SNAP_TO_CENTER})
  @Retention(RetentionPolicy.SOURCE)
  public @interface SnapMode {}

  private final int mOrientation;
  private final boolean mReverseLayout;
  private final @SnapMode int mSnapMode;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;

  /**
   * Static factory method to create a recycler configuration
   * with incremental mount optionally turned on.
   */
  public static ListRecyclerConfiguration createWithRecyclerBinderConfiguration(
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    return new ListRecyclerConfiguration (
        LinearLayoutManager.VERTICAL,
        false,
        SNAP_NONE,
        recyclerBinderConfiguration);
  }

  public ListRecyclerConfiguration() {
    this (LinearLayoutManager.VERTICAL, false, SNAP_NONE);
  }

  public ListRecyclerConfiguration(int orientation, boolean reverseLayout) {
    this (orientation, reverseLayout, SNAP_NONE);
  }

  public ListRecyclerConfiguration(int orientation, boolean reverseLayout, @SnapMode int snapMode) {
    this(orientation, reverseLayout, snapMode, RECYCLER_BINDER_CONFIGURATION);
  }

  public ListRecyclerConfiguration(
      int orientation,
      boolean reverseLayout,
      @SnapMode int snapMode,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    if (orientation != OrientationHelper.HORIZONTAL && snapMode != SNAP_NONE) {
      throw new UnsupportedOperationException("Snapping is only implemented for horizontal lists");
    }
    mOrientation = orientation;
    mReverseLayout = reverseLayout;
    mSnapMode = snapMode;
    mRecyclerBinderConfiguration = recyclerBinderConfiguration == null
        ? RECYCLER_BINDER_CONFIGURATION
        : recyclerBinderConfiguration;
  }

  @Override
  public T buildTarget(ComponentContext c) {
    final LinearLayoutInfo linearLayoutInfo;
    linearLayoutInfo = new LinearLayoutInfo(c, mOrientation, mReverseLayout);

    final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
        .rangeRatio((float) mRecyclerBinderConfiguration.getRangeRatio())
        .layoutInfo(linearLayoutInfo)
        .layoutHandlerFactory(mRecyclerBinderConfiguration.getLayoutHandlerFactory())
        .canPrefetchDisplayLists(mRecyclerBinderConfiguration.canPrefetchDisplayLists())
        .build(c);
    return (T) new SectionBinderTarget(recyclerBinder);
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    if (mSnapMode == SNAP_TO_CENTER) {
      return new PagerSnapHelper();
    } else if (mSnapMode == SNAP_TO_START) {
      return new StartSnapHelper();
    }

    return null;
  }
}
