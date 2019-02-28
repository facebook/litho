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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that represents the children of a {@link GroupSectionSpec}. This is used to mimic
 * litho's usage of the Container class in {@link com.facebook.litho.annotations.LayoutSpec}'s API
 */
public class Children {

  private List<Section> mSections;

  private Children() {
    mSections = new ArrayList<>();
  }

  public static Builder create() {
    return new Builder();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public List<Section> getChildren() {
    return mSections;
  }

  public static class Builder {

    private final Children mChildren = new Children();

    public Builder child(@Nullable Section section) {
      if (section != null) {
        mChildren.mSections.add(section.makeShallowCopy());
      }

      return this;
    }

    public Builder child(@Nullable List<Section> sectionList) {
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
      if (sectionBuilder != null) {
        mChildren.mSections.add(sectionBuilder.build());
      }

      return this;
    }

    public Builder children(@Nullable List<Section.Builder<?>> sectionBuilderList) {
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
      return mChildren;
    }
  }
}
