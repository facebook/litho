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
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.LayoutHandlerFactory;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RecyclerBinderConfig;
import com.facebook.rendercore.RunnableHandler;

/** Configuration setting for {@link RecyclerBinder}. */
public class RecyclerBinderConfiguration {

  private final float mRangeRatio;
  @Nullable private final LayoutHandlerFactory mLayoutHandlerFactory;
  private final boolean mIsWrapContent;
  // TODO T34627443 make all fields final after removing setters
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  private boolean mEnableStableIds;
  @Nullable private RunnableHandler mChangeSetThreadHandler;
  private final boolean mIsIncrementalMountEnabled;
  private final boolean mPostToFrontOfQueueForFirstChangeset;

  private final RecyclerBinderConfig mRecyclerBinderConfig;

  public static Builder create() {
    return new Builder();
  }

  public static Builder create(RecyclerBinderConfiguration configuration) {
    return new Builder(configuration);
  }

  private RecyclerBinderConfiguration(
      RecyclerBinderConfig recyclerBinderConfig,
      float rangeRatio,
      @Nullable LayoutHandlerFactory layoutHandlerFactory,
      boolean wrapContent,
      boolean useBackgroundChangeSets,
      boolean enableStableIds,
      @Nullable RunnableHandler changeSetThreadHandler,
      boolean isIncrementalMountEnabled,
      boolean postToFrontOfQueueForFirstChangeset) {
    mRangeRatio = rangeRatio;
    mLayoutHandlerFactory = layoutHandlerFactory;
    mIsWrapContent = wrapContent;
    mUseBackgroundChangeSets = useBackgroundChangeSets;
    mEnableStableIds = enableStableIds;
    mChangeSetThreadHandler = changeSetThreadHandler;
    mIsIncrementalMountEnabled = isIncrementalMountEnabled;
    mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
    mRecyclerBinderConfig = recyclerBinderConfig;
  }

  public float getRangeRatio() {
    return mRangeRatio;
  }

  public @Nullable LayoutHandlerFactory getLayoutHandlerFactory() {
    return mLayoutHandlerFactory;
  }

  public boolean isWrapContent() {
    return mIsWrapContent;
  }

  public boolean getUseBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  public boolean getEnableStableIds() {
    return mEnableStableIds;
  }

  public @Nullable RunnableHandler getChangeSetThreadHandler() {
    return mChangeSetThreadHandler;
  }

  public boolean isIncrementalMountEnabled() {
    return mIsIncrementalMountEnabled;
  }

  public boolean isPostToFrontOfQueueForFirstChangeset() {
    return mPostToFrontOfQueueForFirstChangeset;
  }

  public RecyclerBinderConfig getRecyclerBinderConfig() {
    return mRecyclerBinderConfig;
  }

  public static class Builder {

    static final float DEFAULT_RANGE = RecyclerBinder.Builder.DEFAULT_RANGE_RATIO;

    private RecyclerBinderConfig mRecyclerBinderConfig;

    @Nullable private LayoutHandlerFactory mLayoutHandlerFactory;
    private float mRangeRatio = DEFAULT_RANGE;
    private boolean mWrapContent = false;
    private boolean mEnableStableIds =
        ComponentsConfiguration.defaultRecyclerBinderConfigUseStableId;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    @Nullable private RunnableHandler mChangeSetThreadHandler;
    private boolean mIsIncrementalMountEnabled =
        !ComponentsConfiguration.isIncrementalMountGloballyDisabled;
    private boolean mPostToFrontOfQueueForFirstChangeset;

    Builder() {}

    private Builder(RecyclerBinderConfiguration configuration) {
      mRecyclerBinderConfig = configuration.mRecyclerBinderConfig;
      this.mLayoutHandlerFactory = configuration.mLayoutHandlerFactory;
      this.mRangeRatio = configuration.mRangeRatio;
      this.mWrapContent = configuration.mIsWrapContent;
      this.mEnableStableIds = configuration.mEnableStableIds;
      this.mUseBackgroundChangeSets = configuration.mUseBackgroundChangeSets;
      this.mChangeSetThreadHandler = configuration.mChangeSetThreadHandler;
      mIsIncrementalMountEnabled = configuration.mIsIncrementalMountEnabled;
      this.mPostToFrontOfQueueForFirstChangeset =
          configuration.mPostToFrontOfQueueForFirstChangeset;
    }

    /**
     * Sets the {@link RecyclerBinderConfig} to be used by the underlying {@link RecyclerBinder}
     *
     * <p>This is a transitory API, and eventually it will replace both the {@link
     * RecyclerBinderConfiguration} and {@link RecyclerBinder.Builder}
     */
    public Builder recyclerBinderConfig(RecyclerBinderConfig recyclerBinderConfig) {
      mRecyclerBinderConfig = recyclerBinderConfig;
      return this;
    }

    /**
     * @param idleExecutor This determines the thread on which the Component layout calculation will
     *     be processed in. Null means that the computation will be done in the background thread.
     */
    public Builder idleExecutor(@Nullable LayoutHandlerFactory idleExecutor) {
      mLayoutHandlerFactory = idleExecutor;
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
     * @param isWrapContent If true, the underlying RecyclerBinder will measure the parent height by
     *     the height of children if the orientation is vertical, or measure the parent width by the
     *     width of children if the orientation is horizontal.
     */
    public Builder wrapContent(boolean isWrapContent) {
      mWrapContent = isWrapContent;
      return this;
    }

    /**
     * Experimental. See {@link SectionTree.Target#supportsBackgroundChangeSets()} for more info.
     */
    public Builder useBackgroundChangeSets(boolean useBackgroundChangeSets) {
      mUseBackgroundChangeSets = useBackgroundChangeSets;
      return this;
    }

    public Builder enableStableIds(boolean enableStableIds) {
      mEnableStableIds = enableStableIds;
      return this;
    }

    public Builder changeSetThreadHandler(@Nullable RunnableHandler changeSetThreadHandler) {
      mChangeSetThreadHandler = changeSetThreadHandler;
      return this;
    }

    public Builder isIncrementalMountEnabled(boolean isEnabled) {
      if (ComponentsConfiguration.isIncrementalMountGloballyDisabled) {
        mIsIncrementalMountEnabled = false;
      } else {
        mIsIncrementalMountEnabled = isEnabled;
      }
      return this;
    }

    public Builder postToFrontOfQueueForFirstChangeset(
        boolean postToFrontOfQueueForFirstChangeset) {
      mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
      return this;
    }

    public RecyclerBinderConfiguration build() {
      RecyclerBinderConfig builderRecyclerBinderConfig = mRecyclerBinderConfig;

      return new RecyclerBinderConfiguration(
          builderRecyclerBinderConfig != null
              ? builderRecyclerBinderConfig
              : new RecyclerBinderConfig(),
          mRangeRatio,
          mLayoutHandlerFactory,
          mWrapContent,
          mUseBackgroundChangeSets,
          mEnableStableIds,
          mChangeSetThreadHandler,
          mIsIncrementalMountEnabled,
          mPostToFrontOfQueueForFirstChangeset);
    }
  }
}
