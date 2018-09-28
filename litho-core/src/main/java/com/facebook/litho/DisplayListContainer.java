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

package com.facebook.litho;

import android.support.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.displaylist.DisplayList;

/**
 * Wrapper around {@link DisplayList} that is shared between {@link LayoutOutput} and
 * {@link MountItem}. This is useful to share the generated displaylist output across
 * both data structures.
 */
class DisplayListContainer {
  private @Nullable DisplayList mDisplayList;
  private boolean mCanCacheDrawingDisplayLists;
  private @Nullable String mName;

  void setDisplayList(DisplayList displayList) {
    mDisplayList = displayList;
  }

  @Nullable DisplayList getDisplayList() {
    return mDisplayList;
  }

  boolean hasValidDisplayList() {
    return mDisplayList != null && mDisplayList.isValid();
  }

  void init(String name, boolean canCacheDrawingDisplayLists) {
    if (ComponentsConfiguration.forceNotToCacheDisplayLists) {
      throw new RuntimeException(
          "DisplayLists are not supposed to be used, this should never be called");
    }

    mName = name;
    mCanCacheDrawingDisplayLists = canCacheDrawingDisplayLists;
  }

  boolean canCacheDrawingDisplayLists() {
    return mCanCacheDrawingDisplayLists;
  }

  void release() {
    mDisplayList = null;
    mCanCacheDrawingDisplayLists = false;
    mName = null;
  }

  @Nullable String getName() {
    return mName;
  }
}
