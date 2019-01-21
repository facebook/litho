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

package com.facebook.litho.sections;

import static android.support.v4.util.Pools.SynchronizedPool;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools.Pool;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that represents the children of a {@link GroupSectionSpec}. This is used to mimic
 * litho's usage of the Container class in {@link com.facebook.litho.annotations.LayoutSpec}'s API
 */
public class Children {

  private static final Pool<Builder> sBuildersPool = new SynchronizedPool<>(2);

  private List<Section> mSections;

  private Children() {
    mSections = new ArrayList<>();
  }

  public static Builder create() {
    Builder builder = sBuildersPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(new Children());

    return builder;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public List<Section> getChildren() {
    return mSections;
  }

  public static class Builder {

    private Children mChildren;

    private Builder() {}

    private void init(Children children) {
      mChildren = children;
    }

    public Builder child(@Nullable Section section) {
      verifyValidState();

      if (section != null) {
        mChildren.mSections.add(section.makeShallowCopy());
      }

      return this;
    }

    public Builder child(@Nullable List<Section> sectionList) {
      verifyValidState();

      if (sectionList == null || sectionList.isEmpty()) {
        return this;
      }

      for (int i = 0; i < sectionList.size(); i++) {
        Section section = sectionList.get(i);
        if (section != null) {
          mChildren.mSections.add(section.makeShallowCopy());
        }
      }

      return this;
    }

    public Builder child(@Nullable Section.Builder<?> sectionBuilder) {
      verifyValidState();

      if (sectionBuilder != null) {
        mChildren.mSections.add(sectionBuilder.build());
      }

      return this;
    }

    public Builder children(@Nullable List<Section.Builder<?>> sectionBuilderList) {
      verifyValidState();

      if (sectionBuilderList == null || sectionBuilderList.isEmpty()) {
        return this;
      }

      for (int i = 0; i < sectionBuilderList.size(); i++) {
        Section.Builder<?> sectionBuilder = sectionBuilderList.get(i);
        if (sectionBuilder != null) {
          mChildren.mSections.add(sectionBuilder.build());
        }
      }

      return this;
    }

    public Children build() {
      verifyValidState();

      Children children = mChildren;
      mChildren = null;
      sBuildersPool.release(this);

      return children;
    }

    private void verifyValidState() {
      if (mChildren == null) {
        throw new IllegalStateException(".build() call has been already made on this Builder.");
      }
    }
  }
}
