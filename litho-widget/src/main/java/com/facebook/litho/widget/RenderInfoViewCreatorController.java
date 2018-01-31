/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.widget;

import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;
import android.util.SparseArray;
import com.facebook.litho.viewcompat.ViewCreator;
import javax.annotation.Nullable;

/**
 * Helper class to keep track of the different view types that we're rendering using ViewRenderInfo.
 */
public class RenderInfoViewCreatorController {

  @VisibleForTesting final SparseArray<ViewCreator> mViewTypeToViewCreator = new SparseArray<>();

  @VisibleForTesting
  final SimpleArrayMap<ViewCreator, Integer> mViewCreatorToViewType = new SimpleArrayMap<>();

  public static final int DEFAULT_COMPONENT_VIEW_TYPE = 0;
  private final boolean mCustomViewTypes;
  private final int mComponentViewType;
  private int mViewTypeCounter;

  public RenderInfoViewCreatorController(boolean hasCustomViewType, int componentViewType) {
    mCustomViewTypes = hasCustomViewType;
    mComponentViewType = componentViewType;
    mViewTypeCounter = componentViewType + 1;
  }

  @UiThread
  public void maybeTrackViewCreator(RenderInfo renderInfo) {
    if (!renderInfo.rendersView()) {
      return;
    }
    if (mCustomViewTypes && !renderInfo.hasCustomViewType()) {
      throw new IllegalStateException("If you enable custom viewTypes, you must provide a"
          + " customViewType in ViewRenderInfo.");
    } else if (!mCustomViewTypes && renderInfo.hasCustomViewType()) {
      throw new IllegalStateException("You must enable custom viewTypes to provide customViewType"
          + " in ViewRenderInfo.");
    }

    final ViewCreator viewCreator = renderInfo.getViewCreator();
    final int viewType;
    final int index = mViewCreatorToViewType.indexOfKey(viewCreator);
    if (index >= 0) {
      viewType = mViewCreatorToViewType.valueAt(index);
    } else {
      if (renderInfo.hasCustomViewType()) {
        viewType = renderInfo.getViewType();
      } else {
        viewType = mViewTypeCounter++;
      }
      mViewTypeToViewCreator.put(viewType, viewCreator);
      mViewCreatorToViewType.put(viewCreator, viewType);
    }

    if (!renderInfo.hasCustomViewType()) {
      renderInfo.setViewType(viewType);
    }
  }

  public @Nullable ViewCreator getViewCreator(int viewType) {
    return mViewTypeToViewCreator.get(viewType);
  }

  int getComponentViewType() {
    return mComponentViewType;
  }
}
