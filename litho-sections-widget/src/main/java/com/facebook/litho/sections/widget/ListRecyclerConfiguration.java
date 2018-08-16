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
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER_CHILD;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_END;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_START;
import static com.facebook.litho.widget.SnapUtil.SnapMode;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.RecyclerBinder;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a
 * {@link android.support.v7.widget.LinearLayoutManager} for the {@link RecyclerView}.
 */
public class ListRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {

  private static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
      new RecyclerBinderConfiguration(4.0);

  private static final LinearLayoutInfoFactory LINEAR_LAYOUT_INFO_FACTORY =
      new DefaultLinearLayoutInfoFactory();

  private final int mOrientation;
  private final boolean mReverseLayout;
  private final @SnapMode int mSnapMode;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;
  private final LinearLayoutInfoFactory mLinearLayoutInfoFactory;

  /**
   * Static factory method to create a recycler configuration
   * with incremental mount optionally turned on.
   */
  public static ListRecyclerConfiguration createWithRecyclerBinderConfiguration(
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    return new ListRecyclerConfiguration(
        LinearLayoutManager.VERTICAL,
        false,
        SNAP_NONE,
        recyclerBinderConfiguration,
        LINEAR_LAYOUT_INFO_FACTORY);
  }

  public ListRecyclerConfiguration() {
    this (LinearLayoutManager.VERTICAL, false, SNAP_NONE);
  }

  public ListRecyclerConfiguration(int orientation, boolean reverseLayout) {
    this (orientation, reverseLayout, SNAP_NONE);
  }

  public ListRecyclerConfiguration(int orientation, boolean reverseLayout, @SnapMode int snapMode) {
    this(
        orientation,
        reverseLayout,
        snapMode,
        RECYCLER_BINDER_CONFIGURATION);
  }

  public ListRecyclerConfiguration(
      int orientation,
      boolean reverseLayout,
      @SnapMode int snapMode,
      RecyclerBinderConfiguration recyclerBinderConfiguration) {
    this(
        orientation,
        reverseLayout,
        snapMode,
        recyclerBinderConfiguration,
        LINEAR_LAYOUT_INFO_FACTORY);
  }

  public ListRecyclerConfiguration(
      int orientation,
      boolean reverseLayout,
      @SnapMode int snapMode,
      @Nullable RecyclerBinderConfiguration recyclerBinderConfiguration,
      @Nullable LinearLayoutInfoFactory linearLayoutInfoFactory) {
    if (orientation != OrientationHelper.HORIZONTAL && snapMode != SNAP_NONE) {
      throw new UnsupportedOperationException("Snapping is only implemented for horizontal lists");
    }
    mOrientation = orientation;
    mReverseLayout = reverseLayout;
    mSnapMode = snapMode;
    mRecyclerBinderConfiguration = recyclerBinderConfiguration == null
        ? RECYCLER_BINDER_CONFIGURATION
        : recyclerBinderConfiguration;
    mLinearLayoutInfoFactory = linearLayoutInfoFactory == null
        ? LINEAR_LAYOUT_INFO_FACTORY :
        linearLayoutInfoFactory;
  }

  @Override
  public T buildTarget(ComponentContext c) {
    final LinearLayoutInfo linearLayoutInfo;
    linearLayoutInfo = mLinearLayoutInfoFactory
        .createLinearLayoutInfo(c, mOrientation, mReverseLayout);

    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .rangeRatio((float) mRecyclerBinderConfiguration.getRangeRatio())
            .layoutInfo(linearLayoutInfo)
            .layoutHandlerFactory(mRecyclerBinderConfiguration.getLayoutHandlerFactory())
            .canPrefetchDisplayLists(mRecyclerBinderConfiguration.canPrefetchDisplayLists())
            .isCircular(mRecyclerBinderConfiguration.isCircular())
            .wrapContent(mRecyclerBinderConfiguration.isWrapContent())
            .hasDynamicItemHeight(mRecyclerBinderConfiguration.hasDynamicItemHeight())
            .splitLayoutTag(mRecyclerBinderConfiguration.getSplitLayoutTag())
            .enableStableIds(mRecyclerBinderConfiguration.getEnableStableIds())
            .invalidStateLogParamsList(mRecyclerBinderConfiguration.getInvalidStateLogParamsList())
            .useSharedLayoutStateFuture(
                mRecyclerBinderConfiguration.getUseSharedLayoutStateFuture())
            .threadPoolForSharedLayoutStateFutureConfig(
                mRecyclerBinderConfiguration.getThreadPoolForSharedLayoutStateFutureConfig())
            .build(c);
    return (T)
        new SectionBinderTarget(
            recyclerBinder, mRecyclerBinderConfiguration.getUseAsyncMutations());
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    switch (mSnapMode) {
      case SNAP_TO_CENTER:
        return new PagerSnapHelper();

      case SNAP_TO_START:
        return new StartSnapHelper();

      case SNAP_TO_CENTER_CHILD:
        return new LinearSnapHelper();

      case SNAP_TO_END:
      case SNAP_NONE:
      default:
        return null;
    }
  }

  @Override
  public int getOrientation() {
    return mOrientation;
  }

  @Override
  public boolean isWrapContent() {
    return mRecyclerBinderConfiguration.isWrapContent();
  }

  private static class DefaultLinearLayoutInfoFactory implements LinearLayoutInfoFactory {
    @Override
    public LinearLayoutInfo createLinearLayoutInfo(
        Context c, int orientation, boolean reverseLayout) {
      return new LinearLayoutInfo(c, orientation, reverseLayout);
    }
  }
}
