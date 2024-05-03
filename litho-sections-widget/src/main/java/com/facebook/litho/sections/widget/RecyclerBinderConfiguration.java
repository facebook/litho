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
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.widget.RecyclerBinder;
import com.facebook.litho.widget.RecyclerBinderConfig;
import com.facebook.rendercore.RunnableHandler;

/** Configuration setting for {@link RecyclerBinder}. */
public class RecyclerBinderConfiguration {
  // TODO T34627443 make all fields final after removing setters
  private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
  @Nullable private RunnableHandler mChangeSetThreadHandler;
  private final boolean mPostToFrontOfQueueForFirstChangeset;

  /**
   * Whether to use the primitive recycler API. This is an experimental feature and should not be
   * used outside of the Litho team.
   */
  private final boolean mPrimitiveRecyclerEnabled;

  private final RecyclerBinderConfig mRecyclerBinderConfig;

  public static Builder create() {
    return new Builder();
  }

  public static Builder create(RecyclerBinderConfiguration configuration) {
    return new Builder(configuration);
  }

  private RecyclerBinderConfiguration(
      RecyclerBinderConfig recyclerBinderConfig,
      boolean useBackgroundChangeSets,
      @Nullable RunnableHandler changeSetThreadHandler,
      boolean postToFrontOfQueueForFirstChangeset,
      boolean primitiveRecyclerEnabled) {
    mUseBackgroundChangeSets = useBackgroundChangeSets;
    mChangeSetThreadHandler = changeSetThreadHandler;
    mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
    mRecyclerBinderConfig = recyclerBinderConfig;
    mPrimitiveRecyclerEnabled = primitiveRecyclerEnabled;
  }

  public boolean getUseBackgroundChangeSets() {
    return mUseBackgroundChangeSets;
  }

  public @Nullable RunnableHandler getChangeSetThreadHandler() {
    return mChangeSetThreadHandler;
  }

  public boolean isPostToFrontOfQueueForFirstChangeset() {
    return mPostToFrontOfQueueForFirstChangeset;
  }

  public RecyclerBinderConfig getRecyclerBinderConfig() {
    return mRecyclerBinderConfig;
  }

  public boolean isPrimitiveRecyclerEnabled() {
    return mPrimitiveRecyclerEnabled;
  }

  public static class Builder {

    private RecyclerBinderConfig mRecyclerBinderConfig;
    private boolean mUseBackgroundChangeSets = SectionsConfiguration.useBackgroundChangeSets;
    @Nullable private RunnableHandler mChangeSetThreadHandler;
    private boolean mPostToFrontOfQueueForFirstChangeset;
    private boolean mIsPrimitiveRecyclerEnabled = false;

    Builder() {}

    private Builder(RecyclerBinderConfiguration configuration) {
      mRecyclerBinderConfig = configuration.mRecyclerBinderConfig;
      this.mUseBackgroundChangeSets = configuration.mUseBackgroundChangeSets;
      this.mChangeSetThreadHandler = configuration.mChangeSetThreadHandler;
      this.mPostToFrontOfQueueForFirstChangeset =
          configuration.mPostToFrontOfQueueForFirstChangeset;
      this.mIsPrimitiveRecyclerEnabled = configuration.mPrimitiveRecyclerEnabled;
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
     * Experimental. See {@link SectionTree.Target#supportsBackgroundChangeSets()} for more info.
     */
    public Builder useBackgroundChangeSets(boolean useBackgroundChangeSets) {
      mUseBackgroundChangeSets = useBackgroundChangeSets;
      return this;
    }

    public Builder changeSetThreadHandler(@Nullable RunnableHandler changeSetThreadHandler) {
      mChangeSetThreadHandler = changeSetThreadHandler;
      return this;
    }

    public Builder postToFrontOfQueueForFirstChangeset(
        boolean postToFrontOfQueueForFirstChangeset) {
      mPostToFrontOfQueueForFirstChangeset = postToFrontOfQueueForFirstChangeset;
      return this;
    }

    public Builder isPrimitiveRecyclerEnabled(boolean isPrimitiveRecyclerEnabled) {
      mIsPrimitiveRecyclerEnabled = isPrimitiveRecyclerEnabled;
      return this;
    }

    public RecyclerBinderConfiguration build() {
      RecyclerBinderConfig builderRecyclerBinderConfig = mRecyclerBinderConfig;

      return new RecyclerBinderConfiguration(
          builderRecyclerBinderConfig != null
              ? builderRecyclerBinderConfig
              : new RecyclerBinderConfig(),
          mUseBackgroundChangeSets,
          mChangeSetThreadHandler,
          mPostToFrontOfQueueForFirstChangeset,
          mIsPrimitiveRecyclerEnabled);
    }
  }
}
