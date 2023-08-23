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

import androidx.annotation.Nullable;
import com.facebook.litho.ComponentLogParams;
import com.facebook.litho.ErrorEventHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.ComponentWarmer;
import com.facebook.litho.widget.LayoutHandlerFactory;
import com.facebook.litho.widget.LithoViewFactory;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.rendercore.RunnableHandler;
import java.util.List;

/** Configuration setting for {@link RecyclerBinder}. */
public class RecyclerBinderConfiguration {
  /**
   * Used to pass through configuration flags to the componentTree that can be read directly from
   * this componentsConfiguration instance.
   */
  private final @Nullable ComponentsConfiguration mComponentsConfiguration;

  private final float mRangeRatio;
  @Nullable private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mIsCircular;
  private final boolean mIsWrapContent;
  private final @Nullable ComponentWarmer mComponentWarmer;
  private final @Nullable LithoViewFactory mLithoViewFactory;
  // TODO T34627443 make all fields final after removing setters
  private boolean mHasDynamicItemHeight;
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  private boolean mHScrollAsyncMode;
  private boolean mEnableStableIds;
  private final boolean mEnableItemPrefetch;
  private final int mItemViewCacheSize;
  private final boolean mRequestMountForPrefetchedItems;
  private LayoutThreadPoolConfiguration mThreadPoolConfiguration =
      ComponentsConfiguration.threadPoolConfiguration;
  @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
  @Nullable private RunnableHandler mChangeSetThreadHandler;
  private final boolean mIsReconciliationEnabled;
  private final boolean mIsLayoutDiffingEnabled;
  private final boolean mPostToFrontOfQueueForFirstChangeset;
  private final int mEstimatedViewportCount;
  @Nullable private final ErrorEventHandler mErrorEventHandler;

  private final boolean mShouldPreallocatePerMountContent;

  public static Builder create() {
    return new Builder();
  }

  public static Builder create(RecyclerBinderConfiguration configuration) {
    return new Builder(configuration);
  }

  private RecyclerBinderConfiguration(
      float rangeRatio,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      @Nullable ComponentsConfiguration componentsConfiguration,
      boolean circular,
      boolean wrapContent,
      @Nullable List<ComponentLogParams> invalidStateLogParamsList,
      @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration,
      boolean dynamicItemHeight,
      boolean useBackgroundChangeSets,
      boolean hScrollAsyncMode,
      boolean enableStableIds,
      boolean enableItemPrefetch,
      int itemViewCacheSize,
      boolean requestMountForPrefetchedItems,
      @Nullable RunnableHandler changeSetThreadHandler,
      boolean isReconciliationEnabled,
      boolean isLayoutDiffingEnabled,
      boolean postToFrontOfQueueForFirstChangeset,
      @Nullable ComponentWarmer componentWarmer,
      int estimatedViewportCount,
      @Nullable LithoViewFactory lithoViewFactory,
      @Nullable ErrorEventHandler errorEventHandler,
      boolean shouldPreallocatePerMountContent) {
    mRangeRatio = rangeRatio;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mComponentsConfiguration = componentsConfiguration;
    mIsCircular = circular;
    mIsWrapContent = wrapContent;
    mInvalidStateLogParamsList = invalidStateLogParamsList;
    mThreadPoolConfiguration = threadPoolConfiguration;
    mHasDynamicItemHeight = dynamicItemHeight;
    mUseBackgroundChangeSets = useBackgroundChangeSets;
    mHScrollAsyncMode = hScrollAsyncMode;
    mEnableStableIds = enableStableIds;
    mChangeSetThreadHandler = changeSetThreadHandler;
    mIsReconciliationEnabled = isReconciliationEnabled;
    mIsLayoutDiffingEnabled = isLayoutDiffingEnabled;
    mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
    mComponentWarmer = componentWarmer;
    mEstimatedViewportCount = estimatedViewportCount;
    mLithoViewFactory = lithoViewFactory;
    mErrorEventHandler = errorEventHandler;
    mEnableItemPrefetch = enableItemPrefetch;
    mItemViewCacheSize = itemViewCacheSize;
    mRequestMountForPrefetchedItems = requestMountForPrefetchedItems;
    mShouldPreallocatePerMountContent = shouldPreallocatePerMountContent;
  }

  public boolean shouldPreallocatePerMountContent() {
    return mShouldPreallocatePerMountContent;
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

  public boolean hasDynamicItemHeight() {
    return mHasDynamicItemHeight;
  }

  public boolean getUseBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  public boolean getHScrollAsyncMode() {
    return mHScrollAsyncMode;
  }

  @Nullable
  public LayoutThreadPoolConfiguration getThreadPoolConfiguration() {
    return mThreadPoolConfiguration;
  }

  public boolean getEnableStableIds() {
    return mEnableStableIds;
  }

  public boolean getEnableItemPrefetch() {
    return mEnableItemPrefetch;
  }

  public int getItemViewCacheSize() {
    return mItemViewCacheSize;
  }

  public boolean getRequestMountForPrefetchedItems() {
    return mRequestMountForPrefetchedItems;
  }

  public @Nullable List<ComponentLogParams> getInvalidStateLogParamsList() {
    return mInvalidStateLogParamsList;
  }

  public @Nullable RunnableHandler getChangeSetThreadHandler() {
    return mChangeSetThreadHandler;
  }

  public boolean isReconciliationEnabled() {
    return mIsReconciliationEnabled;
  }

  public boolean isLayoutDiffingEnabled() {
    return mIsLayoutDiffingEnabled;
  }

  public boolean isPostToFrontOfQueueForFirstChangeset() {
    return mPostToFrontOfQueueForFirstChangeset;
  }

  public @Nullable ComponentWarmer getComponentWarmer() {
    return mComponentWarmer;
  }

  public @Nullable LithoViewFactory getLithoViewFactory() {
    return mLithoViewFactory;
  }

  public int getEstimatedViewportCount() {
    return mEstimatedViewportCount;
  }

  public @Nullable ErrorEventHandler getErrorEventHandler() {
    return mErrorEventHandler;
  }

  public @Nullable ComponentsConfiguration getComponentsConfiguration() {
    return mComponentsConfiguration;
  }

  public static class Builder {
    public static final LayoutThreadPoolConfiguration DEFAULT_THREAD_POOL_CONFIG =
        ComponentsConfiguration.threadPoolConfiguration;
    static final float DEFAULT_RANGE = RecyclerBinder.Builder.DEFAULT_RANGE_RATIO;
    public static final int UNSET = -1;

    @Nullable private LayoutHandlerFactory mLayoutHandlerFactory;
    @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
    private LayoutThreadPoolConfiguration mThreadPoolConfiguration = DEFAULT_THREAD_POOL_CONFIG;
    private @Nullable ComponentsConfiguration mComponentsConfiguration;
    private float mRangeRatio = DEFAULT_RANGE;
    private boolean mCircular = false;
    private boolean mWrapContent = false;
    private boolean mDynamicItemHeight = false;
    private boolean mHScrollAsyncMode = false;
    private boolean mEnableStableIds = ComponentsConfiguration.enableRecyclerBinderStableId;
    private boolean mEnableItemPrefetch = false;
    private int mItemViewCacheSize = 0;
    private boolean mRequestMountForPrefetchedItems = false;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    @Nullable private RunnableHandler mChangeSetThreadHandler;
    private boolean mIsReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private boolean mIsLayoutDiffingEnabled = ComponentsConfiguration.isLayoutDiffingEnabled;
    private boolean mPostToFrontOfQueueForFirstChangeset;
    private @Nullable ComponentWarmer mComponentWarmer;
    private int mEstimatedViewportCount = UNSET;
    private LithoViewFactory mLithoViewFactory;
    private ErrorEventHandler mErrorEventHandler;
    private boolean mShouldPreallocatePerMountContent;

    Builder() {}

    private Builder(RecyclerBinderConfiguration configuration) {
      this.mLayoutHandlerFactory = configuration.mLayoutHandlerFactory;
      this.mInvalidStateLogParamsList = configuration.mInvalidStateLogParamsList;
      this.mThreadPoolConfiguration = configuration.mThreadPoolConfiguration;
      this.mComponentsConfiguration = configuration.mComponentsConfiguration;
      this.mRangeRatio = configuration.mRangeRatio;
      this.mCircular = configuration.mIsCircular;
      this.mWrapContent = configuration.mIsWrapContent;
      this.mDynamicItemHeight = configuration.mHasDynamicItemHeight;
      this.mHScrollAsyncMode = configuration.mHScrollAsyncMode;
      this.mEnableStableIds = configuration.mEnableStableIds;
      this.mUseBackgroundChangeSets = configuration.mUseBackgroundChangeSets;
      this.mChangeSetThreadHandler = configuration.mChangeSetThreadHandler;
      this.mIsReconciliationEnabled = configuration.mIsReconciliationEnabled;
      this.mIsLayoutDiffingEnabled = configuration.mIsLayoutDiffingEnabled;
      this.mPostToFrontOfQueueForFirstChangeset =
          configuration.mPostToFrontOfQueueForFirstChangeset;
      this.mComponentWarmer = configuration.mComponentWarmer;
      this.mEstimatedViewportCount = configuration.mEstimatedViewportCount;
      this.mLithoViewFactory = configuration.mLithoViewFactory;
      this.mErrorEventHandler = configuration.mErrorEventHandler;
      this.mEnableItemPrefetch = configuration.mEnableItemPrefetch;
      this.mItemViewCacheSize = configuration.mItemViewCacheSize;
      this.mRequestMountForPrefetchedItems = configuration.mRequestMountForPrefetchedItems;
      mShouldPreallocatePerMountContent = configuration.mShouldPreallocatePerMountContent;
    }

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

    /**
     * Experimental. See {@link RecyclerBinder.Builder#recyclerViewItemPrefetch(boolean)} for more
     * info.
     */
    public Builder enableItemPrefetch(boolean enableItemPrefetch) {
      mEnableItemPrefetch = enableItemPrefetch;
      return this;
    }

    /** Experimental. See {@link RecyclerBinder.Builder#setItemViewCacheSize(int)} for more info. */
    public Builder setItemViewCacheSize(int cacheSize) {
      mItemViewCacheSize = cacheSize;
      return this;
    }

    /**
     * Experimental. See {@link RecyclerBinder.Builder#requestMountForPrefetchedItems(boolean)} for
     * more info.
     */
    public Builder setRequestMountForPrefetchedItems(boolean isEnabled) {
      mRequestMountForPrefetchedItems = isEnabled;
      return this;
    }

    public Builder componentsConfiguration(ComponentsConfiguration componentsConfiguration) {
      this.mComponentsConfiguration = componentsConfiguration;
      return this;
    }

    public Builder changeSetThreadHandler(@Nullable RunnableHandler changeSetThreadHandler) {
      mChangeSetThreadHandler = changeSetThreadHandler;
      return this;
    }

    public Builder isReconciliationEnabled(boolean isEnabled) {
      mIsReconciliationEnabled = isEnabled;
      return this;
    }

    public Builder isLayoutDiffingEnabled(boolean isEnabled) {
      mIsLayoutDiffingEnabled = isEnabled;
      return this;
    }

    public Builder postToFrontOfQueueForFirstChangeset(
        boolean postToFrontOfQueueForFirstChangeset) {
      mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
      return this;
    }

    public Builder componentWarmer(ComponentWarmer componentWarmer) {
      mComponentWarmer = componentWarmer;
      return this;
    }

    public Builder lithoViewFactory(LithoViewFactory lithoViewFactory) {
      mLithoViewFactory = lithoViewFactory;
      return this;
    }

    public Builder errorEventHandler(@Nullable ErrorEventHandler errorEventHandler) {
      mErrorEventHandler = errorEventHandler;
      return this;
    }

    /**
     * Whether this Recycler children should preallocate mount content after being generated. This
     * will only work if the root {@link com.facebook.litho.ComponentTree} has set a preallocation
     * handler, since it will try to use it to run the preallocation.
     */
    public Builder shouldPreallocatePerMountContent(boolean shouldPreallocatePerMountContent) {
      mShouldPreallocatePerMountContent = shouldPreallocatePerMountContent;
      return this;
    }

    /**
     * This is a temporary hack that allows a surface to manually provide an estimated range. It
     * will go away so don't depend on it.
     */
    @Deprecated
    public Builder estimatedViewportCount(int estimatedViewportCount) {
      if (estimatedViewportCount <= 0) {
        throw new IllegalArgumentException(
            "Estimated viewport count must be > 0: " + estimatedViewportCount);
      }
      mEstimatedViewportCount = estimatedViewportCount;
      return this;
    }

    public RecyclerBinderConfiguration build() {
      return new RecyclerBinderConfiguration(
          mRangeRatio,
          mLayoutHandlerFactory,
          mComponentsConfiguration,
          mCircular,
          mWrapContent,
          mInvalidStateLogParamsList,
          mThreadPoolConfiguration,
          mDynamicItemHeight,
          mUseBackgroundChangeSets,
          mHScrollAsyncMode,
          mEnableStableIds,
          mEnableItemPrefetch,
          mItemViewCacheSize,
          mRequestMountForPrefetchedItems,
          mChangeSetThreadHandler,
          mIsReconciliationEnabled,
          mIsLayoutDiffingEnabled,
          mPostToFrontOfQueueForFirstChangeset,
          mComponentWarmer,
          mEstimatedViewportCount,
          mLithoViewFactory,
          mErrorEventHandler,
          mShouldPreallocatePerMountContent);
    }
  }
}
