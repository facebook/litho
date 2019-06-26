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

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import com.facebook.litho.widget.LayoutInfo;
import com.facebook.litho.widget.StaggeredGridLayoutInfo;
import javax.annotation.Nullable;

/**
 * A configuration object for {@link RecyclerCollectionComponent} that will create a {@link
 * androidx.recyclerview.widget.StaggeredGridLayoutManager} for the {@link RecyclerView}.
 */
public class StaggeredGridRecyclerConfiguration<T extends SectionTree.Target & Binder<RecyclerView>>
    implements RecyclerConfiguration {
  private final int mNumSpans;
  private final int mOrientation;
  private final boolean mReverseLayout;
  private final int mGapStrategy;
  private final RecyclerBinderConfiguration mRecyclerBinderConfiguration;

  public static Builder create() {
    return new Builder();
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public static StaggeredGridRecyclerConfiguration createWithRecyclerBinderConfiguration(
      int numSpans, RecyclerBinderConfiguration recyclerBinderConfiguration) {
    return new StaggeredGridRecyclerConfiguration(
        numSpans,
        StaggeredGridLayoutManager.VERTICAL,
        false,
        StaggeredGridLayoutManager.GAP_HANDLING_NONE,
        recyclerBinderConfiguration);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public StaggeredGridRecyclerConfiguration(int numSpans) {
    this(numSpans, StaggeredGridLayoutManager.VERTICAL, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public StaggeredGridRecyclerConfiguration(int numSpans, int orientation, boolean reverseLayout) {
    this(numSpans, orientation, reverseLayout, Builder.RECYCLER_BINDER_CONFIGURATION);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
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

  /** Use {@link #create()} instead. */
  @Deprecated
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
            ? Builder.RECYCLER_BINDER_CONFIGURATION
            : recyclerBinderConfiguration;
  }

  @Override
  public Builder acquireBuilder() {
    return new Builder(this);
  }

  @Override
  public @Nullable SnapHelper getSnapHelper() {
    return null;
  }

  @Override
  public int getSnapMode() {
    return SNAP_NONE;
  }

  @Override
  public int getOrientation() {
    return mOrientation;
  }

  @Override
  public LayoutInfo getLayoutInfo(ComponentContext c) {
    return new StaggeredGridLayoutInfo(mNumSpans, mOrientation, mReverseLayout, mGapStrategy);
  }

  @Override
  public RecyclerBinderConfiguration getRecyclerBinderConfiguration() {
    return mRecyclerBinderConfiguration;
  }

  public static class Builder implements RecyclerConfiguration.Builder {
    static final RecyclerBinderConfiguration RECYCLER_BINDER_CONFIGURATION =
        RecyclerBinderConfiguration.create().build();

    private int mNumSpans = 2;
    private int mOrientation = StaggeredGridLayoutManager.VERTICAL;
    private boolean mReverseLayout = false;
    private int mGapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE;
    private RecyclerBinderConfiguration mRecyclerBinderConfiguration =
        RECYCLER_BINDER_CONFIGURATION;

    Builder() {}

    Builder(StaggeredGridRecyclerConfiguration configuration) {
      this.mNumSpans = configuration.mNumSpans;
      this.mOrientation = configuration.mOrientation;
      this.mReverseLayout = configuration.mReverseLayout;
      this.mGapStrategy = configuration.mGapStrategy;
      this.mRecyclerBinderConfiguration = configuration.mRecyclerBinderConfiguration;
    }

    @Override
    public Builder snapMode(int snapMode) {
      throw new UnsupportedOperationException(
          "SnapMode is not supported for StaggeredGridRecyclerConfiguration");
    }

    public Builder numSpans(int numSpans) {
      mNumSpans = numSpans;
      return this;
    }

    @Override
    public Builder orientation(int orientation) {
      mOrientation = orientation;
      return this;
    }

    public Builder reverseLayout(boolean reverseLayout) {
      mReverseLayout = reverseLayout;
      return this;
    }

    public Builder gapStrategy(int gapStrategy) {
      mGapStrategy = gapStrategy;
      return this;
    }

    @Override
    public Builder recyclerBinderConfiguration(
        RecyclerBinderConfiguration recyclerBinderConfiguration) {
      mRecyclerBinderConfiguration = recyclerBinderConfiguration;
      return this;
    }

    /**
     * Builds a {@link StaggeredGridRecyclerConfiguration} using the parameters specified in this
     * builder.
     */
    @Override
    public StaggeredGridRecyclerConfiguration build() {
      return new StaggeredGridRecyclerConfiguration(
          mNumSpans, mOrientation, mReverseLayout, mGapStrategy, mRecyclerBinderConfiguration);
    }
  }
}
