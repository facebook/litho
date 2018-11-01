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

import android.support.annotation.Nullable;
import com.facebook.litho.ComponentLogParams;
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
  private final boolean mCanPrefetchDisplayLists;
  private final boolean mIsCircular;
  private final boolean mIsWrapContent;
  // TODO T34627443 make all fields final after removing setters
  private boolean mHasDynamicItemHeight;
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  private boolean mHScrollAsyncMode;
  private boolean mEnableStableIds;
  private boolean mUseSharedLayoutStateFuture = ComponentsConfiguration.useSharedLayoutStateFuture;
  private LayoutThreadPoolConfiguration mThreadPoolConfiguration =
      ComponentsConfiguration.threadPoolConfiguration;
  private boolean mAsyncInitRange = ComponentsConfiguration.asyncInitRange;
  @Nullable private String mSplitLayoutTag;
  @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;

  public static Builder create() {
    return new Builder();
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public RecyclerBinderConfiguration(double rangeRatio) {
    this(rangeRatio, null, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public RecyclerBinderConfiguration(
      double rangeRatio, @Nullable LayoutHandlerFactory idleExecutor) {
    this(rangeRatio, idleExecutor, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public RecyclerBinderConfiguration(
      double rangeRatio,
      @Nullable LayoutHandlerFactory idleExecutor,
      boolean canPrefetchDisplayLists) {
    this(rangeRatio, idleExecutor, canPrefetchDisplayLists, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public RecyclerBinderConfiguration(
      double rangeRatio,
      @Nullable LayoutHandlerFactory idleExecutor,
      boolean canPrefetchDisplayLists,
      boolean isCircular) {
    this(rangeRatio, idleExecutor, canPrefetchDisplayLists, isCircular, false);
  }

  /** Use {@link #create()} instead. */
  @Deprecated
  public RecyclerBinderConfiguration(
      double rangeRatio,
      @Nullable LayoutHandlerFactory idleExecutor,
      boolean canPrefetchDisplayLists,
      boolean isCircular,
      boolean isWrapContent) {
    mRangeRatio = rangeRatio > 0 ? (float) rangeRatio : Builder.DEFAULT_RANGE;
    mLayoutHandlerFactory = idleExecutor;
    mCanPrefetchDisplayLists = canPrefetchDisplayLists;
    mIsCircular = isCircular;
    mIsWrapContent = isWrapContent;
  }

  RecyclerBinderConfiguration(
      float rangeRatio,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      boolean canPrefetchDisplayLists,
      boolean circular,
      boolean wrapContent,
      @Nullable List<ComponentLogParams> invalidStateLogParamsList,
      @Nullable String splitLayoutTag,
      LayoutThreadPoolConfiguration threadPoolConfiguration,
      boolean dynamicItemHeight,
      boolean useBackgroundChangeSets,
      boolean hScrollAsyncMode,
      boolean enableStableIds,
      boolean useSharedLayoutStateFuture,
      boolean asyncInitRange) {
    mRangeRatio = rangeRatio;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mCanPrefetchDisplayLists = canPrefetchDisplayLists;
    mIsCircular = circular;
    mIsWrapContent = wrapContent;
    mInvalidStateLogParamsList = invalidStateLogParamsList;
    mSplitLayoutTag = splitLayoutTag;
    mThreadPoolConfiguration = threadPoolConfiguration;
    mHasDynamicItemHeight = dynamicItemHeight;
    mUseBackgroundChangeSets = useBackgroundChangeSets;
    mHScrollAsyncMode = hScrollAsyncMode;
    mEnableStableIds = enableStableIds;
    mUseSharedLayoutStateFuture = useSharedLayoutStateFuture;
    mAsyncInitRange = asyncInitRange;
  }

  public float getRangeRatio() {
    return mRangeRatio;
  }

  public @Nullable LayoutHandlerFactory getLayoutHandlerFactory() {
    return mLayoutHandlerFactory;
  }

  public boolean canPrefetchDisplayLists() {
    return mCanPrefetchDisplayLists;
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

  public String getSplitLayoutTag() {
    return mSplitLayoutTag;
  }

  public boolean getUseBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  public boolean getHScrollAsyncMode() {
    return mHScrollAsyncMode;
  }

  public boolean getUseSharedLayoutStateFuture() {
    return mUseSharedLayoutStateFuture;
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

  public static class Builder {
    public static final LayoutThreadPoolConfiguration DEFAULT_THREAD_POOL_CONFIG =
        ComponentsConfiguration.threadPoolConfiguration;
    static final float DEFAULT_RANGE = RecyclerBinder.Builder.DEFAULT_RANGE_RATIO;

    @Nullable private LayoutHandlerFactory mLayoutHandlerFactory;
    @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
    @Nullable private String mSplitLayoutTag;
    private LayoutThreadPoolConfiguration mThreadPoolConfiguration = DEFAULT_THREAD_POOL_CONFIG;
    private float mRangeRatio = DEFAULT_RANGE;
    private boolean mCanPrefetchDisplayLists = false;
    private boolean mCircular = false;
    private boolean mWrapContent = false;
    private boolean mDynamicItemHeight = false;
    private boolean mHScrollAsyncMode = false;
    private boolean mEnableStableIds = false;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    private boolean mUseSharedLayoutStateFuture =
        ComponentsConfiguration.useSharedLayoutStateFuture;
    private boolean mAsyncInitRange = ComponentsConfiguration.asyncInitRange;

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

    /** Experimental feature, do not enable! */
    public Builder splitLayoutTag(@Nullable String splitLayoutTag) {
      mSplitLayoutTag = splitLayoutTag;
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
     *     android.support.v7.widget.RecyclerView}'s total number of currently visible items to have
     *     their Component layout computed ahead of time.
     *     <p>e.g total number of visible items = 5 rangeRatio = 10 total number of items before the
     *     1st visible item to be computed = 5 * 10 = 50 total number of items after the last
     *     visible item to be computed = 5 * 10 = 50
     */
    public Builder rangeRatio(float rangeRatio) {
      mRangeRatio = rangeRatio > 0 ? rangeRatio : DEFAULT_RANGE;
      return this;
    }

    /**
     * @param canPrefetchDisplayLists If this is true, displaylists for the Android Views and
     *     Drawables that are not collected during the ComponentTree layout computation will be
     *     collected whenever the UI thread is free.
     */
    public Builder canPrefetchDisplayLists(boolean canPrefetchDisplayLists) {
      mCanPrefetchDisplayLists = canPrefetchDisplayLists;
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

    public Builder useSharedLayoutStateFuture(boolean useSharedLayoutStateFuture) {
      mUseSharedLayoutStateFuture = useSharedLayoutStateFuture;
      return this;
    }

    public Builder asyncInitRange(boolean asyncInitRange) {
      mAsyncInitRange = asyncInitRange;
      return this;
    }

    public RecyclerBinderConfiguration build() {
      return new RecyclerBinderConfiguration(
          mRangeRatio,
          mLayoutHandlerFactory,
          mCanPrefetchDisplayLists,
          mCircular,
          mWrapContent,
          mInvalidStateLogParamsList,
          mSplitLayoutTag,
          mThreadPoolConfiguration,
          mDynamicItemHeight,
          mUseBackgroundChangeSets,
          mHScrollAsyncMode,
          mEnableStableIds,
          mUseSharedLayoutStateFuture,
          mAsyncInitRange);
    }
  }
}
