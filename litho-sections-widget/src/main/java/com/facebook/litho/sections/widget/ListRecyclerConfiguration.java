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
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER;
import static com.facebook.litho.widget.SnapUtil.SNAP_TO_START;
import static com.facebook.litho.widget.SnapUtil.SnapMode;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.LayoutInfo;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.SnapUtil;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a {@link
 * androidx.recyclerview.widget.LinearLayoutManager} for the {@link RecyclerView}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ListRecyclerConfiguration implements RecyclerConfiguration {
  private final int mOrientation;
  private final boolean mReverseLayout;
  private final boolean mStackFromEnd;
  private final @SnapMode int mSnapMode;
  private final @Nullable SnapHelper mSnapHelper;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;
  private final LinearLayoutInfoFactory mLinearLayoutInfoFactory;

  public static Builder create() {
    return new Builder();
  }

  private ListRecyclerConfiguration(
      int orientation,
      boolean reverseLayout,
      boolean stackFromEnd,
      @SnapMode int snapMode,
      @Nullable SnapHelper snapHelper,
      @Nullable RecyclerBinderConfiguration recyclerBinderConfiguration,
      @Nullable LinearLayoutInfoFactory linearLayoutInfoFactory) {
    if (orientation == OrientationHelper.VERTICAL
        && !(snapMode == SNAP_NONE
            || snapMode == SNAP_TO_START
            || snapMode == SnapUtil.SNAP_TO_CENTER)) {
      throw new UnsupportedOperationException(
          "Only snap to start is implemented for vertical lists");
    }
    mOrientation = orientation;
    mReverseLayout = reverseLayout;
    mStackFromEnd = stackFromEnd;
    mSnapMode = snapMode;
    mSnapHelper = snapHelper;
    mRecyclerBinderConfiguration =
        recyclerBinderConfiguration == null
            ? Builder.RECYCLER_BINDER_CONFIGURATION
            : recyclerBinderConfiguration;
    mLinearLayoutInfoFactory =
        linearLayoutInfoFactory == null
            ? Builder.LINEAR_LAYOUT_INFO_FACTORY
            : linearLayoutInfoFactory;
  }

  @Override
  public Builder acquireBuilder() {
    return new Builder(this);
  }

  @Nullable
  @Override
  public SnapHelper getSnapHelper() {
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
    return mLinearLayoutInfoFactory.createLinearLayoutInfo(
        c.getAndroidContext(), mOrientation, mReverseLayout, mStackFromEnd);
  }

  @Override
  public RecyclerBinderConfiguration getRecyclerBinderConfiguration() {
    return mRecyclerBinderConfiguration;
  }

  private static class DefaultLinearLayoutInfoFactory implements LinearLayoutInfoFactory {
    @Override
    public LinearLayoutInfo createLinearLayoutInfo(
        Context c, int orientation, boolean reverseLayout, boolean stackFromEnd) {
      return new LinearLayoutInfo(c, orientation, reverseLayout, stackFromEnd);
    }
  }

  public static class Builder implements RecyclerConfiguration.Builder {
    static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
        RecyclerBinderConfiguration.create().build();
    static final LinearLayoutInfoFactory LINEAR_LAYOUT_INFO_FACTORY =
        new DefaultLinearLayoutInfoFactory();

    private int mOrientation = LinearLayoutManager.VERTICAL;
    private boolean mReverseLayout = false;
    private boolean mStackFromEnd = false;
    @SnapMode private int mSnapMode = SNAP_NONE;
    private RecyclerBinderConfiguration mRecyclerBinderConfiguration =
        RECYCLER_BINDER_CONFIGURATION;
    private LinearLayoutInfoFactory mLinearLayoutInfoFactory = LINEAR_LAYOUT_INFO_FACTORY;
    private int mDeltaJumpThreshold = Integer.MAX_VALUE;
    private int mStartSnapFlingOffset = SnapUtil.SNAP_TO_START_DEFAULT_FLING_OFFSET;
    private @Nullable SnapHelper mSnapHelper;

    Builder() {}

    Builder(ListRecyclerConfiguration listRecyclerConfiguration) {
      this.mOrientation = listRecyclerConfiguration.mOrientation;
      this.mReverseLayout = listRecyclerConfiguration.mReverseLayout;
      this.mStackFromEnd = listRecyclerConfiguration.mStackFromEnd;
      this.mSnapMode = listRecyclerConfiguration.mSnapMode;
      this.mRecyclerBinderConfiguration = listRecyclerConfiguration.mRecyclerBinderConfiguration;
      this.mLinearLayoutInfoFactory = listRecyclerConfiguration.mLinearLayoutInfoFactory;
      this.mSnapHelper = listRecyclerConfiguration.mSnapHelper;
    }

    @Override
    public Builder orientation(int orientation) {
      mOrientation = orientation;
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
    public Builder snapMode(@SnapMode int snapMode) {
      mSnapMode = snapMode;
      return this;
    }

    @Override
    public Builder recyclerBinderConfiguration(
        RecyclerBinderConfiguration recyclerBinderConfiguration) {
      mRecyclerBinderConfiguration = recyclerBinderConfiguration;
      return this;
    }

    public Builder linearLayoutInfoFactory(LinearLayoutInfoFactory linearLayoutInfoFactory) {
      mLinearLayoutInfoFactory = linearLayoutInfoFactory;
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

    public Builder snapHelper(SnapHelper snapHelper) {
      mSnapHelper = snapHelper;
      return this;
    }

    private static void validate(ListRecyclerConfiguration configuration) {
      int snapMode = configuration.getSnapMode();
      if (configuration.getOrientation() == OrientationHelper.VERTICAL
          && !(snapMode == SNAP_NONE || snapMode == SNAP_TO_START || snapMode == SNAP_TO_CENTER)) {
        throw new UnsupportedOperationException(
            "Only snap to start is implemented for vertical lists");
      }
    }

    /**
     * Builds a {@link ListRecyclerConfiguration} using the parameters specified in this builder.
     */
    @Override
    public ListRecyclerConfiguration build() {
      SnapHelper snapHelper =
          (mSnapHelper != null)
              ? mSnapHelper
              : SnapUtil.getSnapHelper(mSnapMode, mDeltaJumpThreshold, mStartSnapFlingOffset);
      ListRecyclerConfiguration configuration =
          new ListRecyclerConfiguration(
              mOrientation,
              mReverseLayout,
              mStackFromEnd,
              mSnapMode,
              snapHelper,
              mRecyclerBinderConfiguration,
              mLinearLayoutInfoFactory);
      validate(configuration);
      return configuration;
    }
  }
}
