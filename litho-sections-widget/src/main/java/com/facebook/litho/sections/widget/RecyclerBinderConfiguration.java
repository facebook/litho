/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.litho.ComponentTree;
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
  private final ComponentsConfiguration mComponentsConfiguration;

  private final float mRangeRatio;
  @Nullable private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mIsCircular;
  private final boolean mIsWrapContent;
  private final boolean mMoveLayoutsBetweenThreads;
  private final @Nullable ComponentWarmer mComponentWarmer;
  private final @ComponentTree.RecyclingMode int mRecyclingMode;
  private final @Nullable LithoViewFactory mLithoViewFactory;
  private final boolean mIgnoreNullLayoutStateError;
  // TODO T34627443 make all fields final after removing setters
  private boolean mHasDynamicItemHeight;
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  private boolean mHScrollAsyncMode;
  private boolean mEnableStableIds;
  private LayoutThreadPoolConfiguration mThreadPoolConfiguration =
      ComponentsConfiguration.threadPoolConfiguration;
  @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
  @Nullable private RunnableHandler mChangeSetThreadHandler;
  private final boolean mEnableDetach;
  private final boolean mIsReconciliationEnabled;
  private final boolean mIsLayoutDiffingEnabled;
  private final boolean mPostToFrontOfQueueForFirstChangeset;
  private final int mEstimatedViewportCount;
  @Nullable private final ErrorEventHandler mErrorEventHandler;

  public static Builder create() {
    return new Builder();
  }

  public static Builder create(RecyclerBinderConfiguration configuration) {
    return new Builder(configuration);
  }

  private RecyclerBinderConfiguration(
      float rangeRatio,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      ComponentsConfiguration componentsConfiguration,
      boolean circular,
      boolean wrapContent,
      @Nullable List<ComponentLogParams> invalidStateLogParamsList,
      @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration,
      boolean dynamicItemHeight,
      boolean useBackgroundChangeSets,
      boolean hScrollAsyncMode,
      boolean enableStableIds,
      boolean enableDetach,
      @Nullable RunnableHandler changeSetThreadHandler,
      boolean moveLayoutsBetweenThreads,
      boolean isReconciliationEnabled,
      boolean ignoreNullLayoutStateError,
      @ComponentTree.RecyclingMode int recyclingMode,
      boolean isLayoutDiffingEnabled,
      boolean postToFrontOfQueueForFirstChangeset,
      @Nullable ComponentWarmer componentWarmer,
      int estimatedViewportCount,
      @Nullable LithoViewFactory lithoViewFactory,
      @Nullable ErrorEventHandler errorEventHandler) {
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
    mEnableDetach = enableDetach;
    mChangeSetThreadHandler = changeSetThreadHandler;
    mMoveLayoutsBetweenThreads = moveLayoutsBetweenThreads;
    mIsReconciliationEnabled = isReconciliationEnabled;
    mIgnoreNullLayoutStateError = ignoreNullLayoutStateError;
    mRecyclingMode = recyclingMode;
    mIsLayoutDiffingEnabled = isLayoutDiffingEnabled;
    mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
    mComponentWarmer = componentWarmer;
    mEstimatedViewportCount = estimatedViewportCount;
    mLithoViewFactory = lithoViewFactory;
    mErrorEventHandler = errorEventHandler;
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

  @Nullable
  public LayoutThreadPoolConfiguration getThreadPoolConfiguration() {
    return mThreadPoolConfiguration;
  }

  public boolean getEnableStableIds() {
    return mEnableStableIds;
  }

  public @Nullable List<ComponentLogParams> getInvalidStateLogParamsList() {
    return mInvalidStateLogParamsList;
  }

  public @Nullable RunnableHandler getChangeSetThreadHandler() {
    return mChangeSetThreadHandler;
  }

  public boolean moveLayoutsBetweenThreads() {
    return mMoveLayoutsBetweenThreads;
  }

  public boolean getEnableDetach() {
    return mEnableDetach;
  }

  public boolean isReconciliationEnabled() {
    return mIsReconciliationEnabled;
  }

  public boolean ignoreNullLayoutStateError() {
    return mIgnoreNullLayoutStateError;
  }

  public @ComponentTree.RecyclingMode int getRecyclingMode() {
    return mRecyclingMode;
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

  public ComponentsConfiguration getComponentsConfiguration() {
    return mComponentsConfiguration;
  }

  public static class Builder {
    public static final LayoutThreadPoolConfiguration DEFAULT_THREAD_POOL_CONFIG =
        ComponentsConfiguration.threadPoolConfiguration;
    static final float DEFAULT_RANGE = RecyclerBinder.Builder.DEFAULT_RANGE_RATIO;
    static final int UNSET = -1;

    @Nullable private LayoutHandlerFactory mLayoutHandlerFactory;
    @Nullable private List<ComponentLogParams> mInvalidStateLogParamsList;
    private LayoutThreadPoolConfiguration mThreadPoolConfiguration = DEFAULT_THREAD_POOL_CONFIG;
    private ComponentsConfiguration mComponentsConfiguration =
        ComponentsConfiguration.getDefaultComponentsConfiguration();
    private float mRangeRatio = DEFAULT_RANGE;
    private boolean mCircular = false;
    private boolean mWrapContent = false;
    private boolean mDynamicItemHeight = false;
    private boolean mHScrollAsyncMode = false;
    private boolean mEnableStableIds = false;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    private boolean mMoveLayoutsBetweenThreads =
        ComponentsConfiguration.canInterruptAndMoveLayoutsBetweenThreads;
    private boolean mEnableDetach = false;
    @Nullable private RunnableHandler mChangeSetThreadHandler;
    private boolean mIsReconciliationEnabled = ComponentsConfiguration.isReconciliationEnabled;
    private boolean mIsLayoutDiffingEnabled = ComponentsConfiguration.isLayoutDiffingEnabled;
    private boolean mPostToFrontOfQueueForFirstChangeset;
    private @Nullable ComponentWarmer mComponentWarmer;
    private int mEstimatedViewportCount = UNSET;
    private @ComponentTree.RecyclingMode int mRecyclingMode = ComponentTree.RecyclingMode.DEFAULT;
    private LithoViewFactory mLithoViewFactory;
    private boolean mIgnoreNullLayoutStateError =
        ComponentsConfiguration.ignoreNullLayoutStateError;
    @Nullable private ErrorEventHandler mErrorEventHandler;

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
      this.mMoveLayoutsBetweenThreads = configuration.mMoveLayoutsBetweenThreads;
      this.mEnableDetach = configuration.mEnableDetach;
      this.mChangeSetThreadHandler = configuration.mChangeSetThreadHandler;
      this.mIsReconciliationEnabled = configuration.mIsReconciliationEnabled;
      this.mRecyclingMode = configuration.mRecyclingMode;
      this.mIsLayoutDiffingEnabled = configuration.mIsLayoutDiffingEnabled;
      this.mPostToFrontOfQueueForFirstChangeset =
          configuration.mPostToFrontOfQueueForFirstChangeset;
      this.mComponentWarmer = configuration.mComponentWarmer;
      this.mEstimatedViewportCount = configuration.mEstimatedViewportCount;
      this.mLithoViewFactory = configuration.mLithoViewFactory;
      this.mErrorEventHandler = configuration.mErrorEventHandler;
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

    public Builder canInterruptAndMoveLayoutsBetweenThreads(boolean isEnabled) {
      this.mMoveLayoutsBetweenThreads = isEnabled;
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

    /** If true, detach components under the hood when RecyclerBinder#detach() is called. */
    public Builder enableDetach(boolean enableDetach) {
      mEnableDetach = enableDetach;
      return this;
    }

    public Builder isReconciliationEnabled(boolean isEnabled) {
      mIsReconciliationEnabled = isEnabled;
      return this;
    }

    public Builder ignoreNullLayoutStateError(boolean shouldIgnore) {
      mIgnoreNullLayoutStateError = shouldIgnore;
      return this;
    }

    public Builder recyclingMode(@ComponentTree.RecyclingMode int recyclingMode) {
      mRecyclingMode = recyclingMode;
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
          mEnableDetach,
          mChangeSetThreadHandler,
          mMoveLayoutsBetweenThreads,
          mIsReconciliationEnabled,
          mIgnoreNullLayoutStateError,
          mRecyclingMode,
          mIsLayoutDiffingEnabled,
          mPostToFrontOfQueueForFirstChangeset,
          mComponentWarmer,
          mEstimatedViewportCount,
          mLithoViewFactory,
          mErrorEventHandler);
    }
  }
}
