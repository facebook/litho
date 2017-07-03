/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.Nullable;

import com.facebook.litho.displaylist.DisplayList;

/**
 * Wrapper around {@link DisplayList} that is shared between {@link LayoutOutput} and
 * {@link MountItem}. This is useful to share the generated displaylist output across
 * both data structures.
 */
class DisplayListContainer {
  private @Nullable DisplayList mDisplayList;
  private boolean mCanCacheDrawingDisplayLists;

  void setDisplayList(DisplayList displayList) {
    mDisplayList = displayList;
  }

  @Nullable DisplayList getDisplayList() {
    return mDisplayList;
  }

  boolean hasDisplayList() {
    return mDisplayList != null;
  }

  void setCacheDrawingDisplayListsEnabled(boolean canCacheDrawingDisplayLists) {
    this.mCanCacheDrawingDisplayLists = canCacheDrawingDisplayLists;
  }

  boolean canCacheDrawingDisplayLists() {
    return mCanCacheDrawingDisplayLists;
  }

  void release() {
    mDisplayList = null;
    mCanCacheDrawingDisplayLists = false;
  }
}
