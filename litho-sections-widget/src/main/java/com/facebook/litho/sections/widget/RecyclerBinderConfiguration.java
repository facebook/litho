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

import androidx.annotation.Nullable;
import com.facebook.litho.ComponentLogParams;
import com.facebook.litho.LithoHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.LayoutHandlerFactory;
import com.facebook.litho.widget.RecyclerBinder;
import java.util.List;

/** Configuration setting for {@link RecyclerBinder}. */
public class RecyclerBinderConfiguration {
  private final float mRangeRatio;
  @Nullable private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mIsCircular;
  private final boolean mIsWrapContent;
  private final boolean mMoveLayoutsBetweenThreads;
  private final boolean mUseCancelableLayoutFutures;
  // TODO T34627443 make all fields final after removing setters
  private boolean mHasDynamicItemHeight;
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  private boolean mHScrollAsyncMode;
  private boolean mEnableStableIds;
  private LayoutThreadPoolConfiguration mThreadPoolConfiguration =
      ComponentsConfiguration.threadPoolConfiguration;
  private boolean mAsyncInitRange = ComponentsConfiguration.asyncInitRange;
  @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
  private final boolean mSplitLayoutForMeasureAndRangeEstimation;
  @Nullable private LithoHandler mChangeSetThreadHandler;
  private final boolean mEnableDetach;

  public static Builder create() {
    return new Builder();
  }

  private RecyclerBinderConfiguration(
      float rangeRatio,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      boolean circular,
      boolean wrapContent,
      @Nullable List<ComponentLogParams> invalidStateLogParamsList,
      LayoutThreadPoolConfiguration threadPoolConfiguration,
      boolean dynamicItemHeight,
      boolean useBackgroundChangeSets,
      boolean hScrollAsyncMode,
      boolean enableStableIds,
      boolean asyncInitRange,
      boolean splitLayoutForMeasureAndRangeEstimation,
      boolean enableDetach,
      @Nullable LithoHandler changeSetThreadHandler,
      boolean moveLayoutsBetweenThreads,
      boolean useCancelableLayoutFutures) {
    mRangeRatio = rangeRatio;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mIsCircular = circular;
    mIsWrapContent = wrapContent;
    mInvalidStateLogParamsList = invalidStateLogParamsList;
    mThreadPoolConfiguration = threadPoolConfiguration;
    mHasDynamicItemHeight = dynamicItemHeight;
    mUseBackgroundChangeSets = useBackgroundChangeSets;
    mHScrollAsyncMode = hScrollAsyncMode;
    mEnableStableIds = enableStableIds;
    mAsyncInitRange = asyncInitRange;
    mSplitLayoutForMeasureAndRangeEstimation = splitLayoutForMeasureAndRangeEstimation;
    mEnableDetach = enableDetach;
    mChangeSetThreadHandler = changeSetThreadHandler;
    mMoveLayoutsBetweenThreads = moveLayoutsBetweenThreads;
    mUseCancelableLayoutFutures = useCancelableLayoutFutures;
  }

  public float getRangeRatio() {
    return mRangeRatio;
  }

  public @Nullable LayoutHandlerFactory getLayoutHandlerFactory() {
    return mLayoutHandlerFactory;
  }

  public boolean isCircular() {
    return mIsCircular;
  }

  public boolean isWrapContent() {
    return mIsWrapContent;
  }

  boolean hasDynamicItemHeight() {
    return mHasDynamicItemHeight;
  }

  public boolean getUseBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  public boolean getHScrollAsyncMode() {
    return mHScrollAsyncMode;
  }

  public LayoutThreadPoolConfiguration getThreadPoolConfiguration() {
    return mThreadPoolConfiguration;
  }

  public boolean getAsyncInitRange() {
    return mAsyncInitRange;
  }

  public boolean getEnableStableIds() {
    return mEnableStableIds;
  }

  public @Nullable List<ComponentLogParams> getInvalidStateLogParamsList() {
    return mInvalidStateLogParamsList;
  }

  public @Nullable LithoHandler getChangeSetThreadHandler() {
    return mChangeSetThreadHandler;
  }

  public boolean splitLayoutForMeasureAndRangeEstimation() {
    return mSplitLayoutForMeasureAndRangeEstimation;
  }

  public boolean useCancelableLayoutFutures() {
    return mUseCancelableLayoutFutures;
  }

  public boolean moveLayoutsBetweenThreads() {
    return mMoveLayoutsBetweenThreads;
  }

  public boolean getEnableDetach() {
    return mEnableDetach;
  }

  public static class Builder {
    public static final LayoutThreadPoolConfiguration DEFAULT_THREAD_POOL_CONFIG =
        ComponentsConfiguration.threadPoolConfiguration;
    static final float DEFAULT_RANGE = RecyclerBinder.Builder.DEFAULT_RANGE_RATIO;

    @Nullable private LayoutHandlerFactory mLayoutHandlerFactory;
    @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
    private LayoutThreadPoolConfiguration mThreadPoolConfiguration = DEFAULT_THREAD_POOL_CONFIG;
    private float mRangeRatio = DEFAULT_RANGE;
    private boolean mCircular = false;
    private boolean mWrapContent = false;
    private boolean mDynamicItemHeight = false;
    private boolean mHScrollAsyncMode = false;
    private boolean mEnableStableIds = false;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    private boolean mAsyncInitRange = ComponentsConfiguration.asyncInitRange;
    private boolean mSplitLayoutForMeasureAndRangeEstimation =
        ComponentsConfiguration.splitLayoutForMeasureAndRangeEstimation;
    private boolean mUseCancelableLayoutFutures =
        ComponentsConfiguration.useCancelableLayoutFutures;
    private boolean mMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;
    private boolean mEnableDetach = false;
    @Nullable private LithoHandler mChangeSetThreadHandler;

    Builder() {}

    /**
     * @param idleExecutor This determines the thread on which the Component layout calculation will
     *     be processed in. Null means that the computation will be done in the background thread.
     */
    public Builder idleExecutor(@Nullable LayoutHandlerFactory idleExecutor) {
      mLayoutHandlerFactory = idleExecutor;
      return this;
    }

    public Builder invalidStateLogParamsList(
        @Nullable List<ComponentLogParams> invalidStateLogParamsList) {
      mInvalidStateLogParamsList = invalidStateLogParamsList;
      return this;
    }

    /** Null value will fall back to the non-null default one. */
    public Builder threadPoolConfiguration(
        @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration) {
      if (threadPoolConfiguration != null) {
        mThreadPoolConfiguration = threadPoolConfiguration;
      } else {
        mThreadPoolConfiguration = DEFAULT_THREAD_POOL_CONFIG;
      }
      return this;
    }

    /**
     * @param rangeRatio Ratio to determine the number of components before and after the {@link
     *     androidx.recyclerview.widget.RecyclerView}'s total number of currently visible items to
     *     have their Component layout computed ahead of time.
     *     <p>e.g total number of visible items = 5 rangeRatio = 10 total number of items before the
     *     1st visible item to be computed = 5 * 10 = 50 total number of items after the last
     *     visible item to be computed = 5 * 10 = 50
     */
    public Builder rangeRatio(float rangeRatio) {
      if (rangeRatio < 0) {
        throw new IllegalArgumentException("Range ratio cannot be negative: " + rangeRatio);
      }
      mRangeRatio = rangeRatio;
      return this;
    }

    /**
     * @param isCircular If true, the underlying RecyclerBinder will have a circular behaviour.
     *     Note: circular lists DO NOT support any operation that changes the size of items like
     *     insert, remove, insert range, remove range
     */
    public Builder isCircular(boolean isCircular) {
      mCircular = isCircular;
      return this;
    }

    /**
     * @param isWrapContent If true, the underlying RecyclerBinder will measure the parent height by
     *     the height of children if the orientation is vertical, or measure the parent width by the
     *     width of children if the orientation is horizontal.
     */
    public Builder wrapContent(boolean isWrapContent) {
      mWrapContent = isWrapContent;
      return this;
    }

    /**
     * TODO T23919104 mihaelao Do not enable this. This is an experimental feature and your Section
     * surface will take a perf hit if you use it. Talk to the Litho team if you think you need
     * this.
     */
    public Builder hasDynamicItemHeight(boolean hasDynamicItemHeight) {
      mDynamicItemHeight = hasDynamicItemHeight;
      return this;
    }

    /**
     * Experimental. See {@link SectionTree.Target#supportsBackgroundChangeSets()} for more info.
     */
    public Builder useBackgroundChangeSets(boolean useBackgroundChangeSets) {
      mUseBackgroundChangeSets = useBackgroundChangeSets;
      return this;
    }

    /** Experimental. See {@link RecyclerBinder.Builder#hscrollAsyncMode(boolean)} for more info. */
    public Builder hScrollAsyncMode(boolean hScrollAsyncMode) {
      mHScrollAsyncMode = hScrollAsyncMode;
      return this;
    }

    public Builder enableStableIds(boolean enableStableIds) {
      mEnableStableIds = enableStableIds;
      return this;
    }

    public Builder asyncInitRange(boolean asyncInitRange) {
      mAsyncInitRange = asyncInitRange;
      return this;
    }

    public Builder splitLayoutForMeasureAndRangeEstimation(
        boolean splitLayoutForMeasureAndRangeEstimation) {
      mSplitLayoutForMeasureAndRangeEstimation = splitLayoutForMeasureAndRangeEstimation;
      return this;
    }

    public Builder canInterruptAndMoveLayoutsBetweenThreads(boolean isEnabled) {
      this.mMoveLayoutsBetweenThreads = isEnabled;
      return this;
    }

    public Builder useCancelableLayoutFutures(boolean isEnabled) {
      this.mUseCancelableLayoutFutures = isEnabled;
      return this;
    }

    public Builder changeSetThreadHandler(LithoHandler changeSetThreadHandler) {
      mChangeSetThreadHandler = changeSetThreadHandler;
      return this;
    }

    /** If true, detach components under the hood when RecyclerBinder#detach() is called. */
    public Builder enableDetach(boolean enableDetach) {
      mEnableDetach = enableDetach;
      return this;
    }

    public RecyclerBinderConfiguration build() {
      return new RecyclerBinderConfiguration(
          mRangeRatio,
          mLayoutHandlerFactory,
          mCircular,
          mWrapContent,
          mInvalidStateLogParamsList,
          mThreadPoolConfiguration,
          mDynamicItemHeight,
          mUseBackgroundChangeSets,
          mHScrollAsyncMode,
          mEnableStableIds,
          mAsyncInitRange,
          mSplitLayoutForMeasureAndRangeEstimation,
          mEnableDetach,
          mChangeSetThreadHandler,
          mMoveLayoutsBetweenThreads,
          mUseCancelableLayoutFutures);
    }
  }
}
