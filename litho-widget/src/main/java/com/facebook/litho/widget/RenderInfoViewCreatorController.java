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

/**
 * Helper class to keep track of the different view types that we're rendering using ViewRenderInfo.
 */
public class RenderInfoViewCreatorController {

  @VisibleForTesting final SparseArray<ViewCreator> mViewTypeToViewCreator = new SparseArray<>();

  // Data structure that is used to quickly query ViewCreator key membership and track its usage
  // count so that we can remove it, when usage becomes 0.
  @VisibleForTesting
  final SimpleArrayMap<ViewCreator, Integer> mViewCreatorToUsageCount = new SimpleArrayMap<>();

  public static final int COMPONENT_VIEW_TYPE = 0;
  private int mViewTypeCounter = COMPONENT_VIEW_TYPE + 1;

  @UiThread
  public void maybeUpdateViewCreatorMappingsOnItemInsert(RenderInfo renderInfo) {
    if (!renderInfo.rendersView()) {
      return;
    }

    final ViewCreator viewCreator = renderInfo.getViewCreator();
    final int viewType;
    if (mViewCreatorToUsageCount.containsKey(viewCreator)) {
      final int currentUsageCount = mViewCreatorToUsageCount.get(viewCreator);
      mViewCreatorToUsageCount.put(viewCreator, currentUsageCount + 1);
      viewType = mViewTypeToViewCreator.keyAt(mViewTypeToViewCreator.indexOfValue(viewCreator));
    } else {
      mViewCreatorToUsageCount.put(viewCreator, 1);
      viewType = mViewTypeCounter++;
      mViewTypeToViewCreator.put(viewType, viewCreator);
    }

    renderInfo.setViewType(viewType);
  }


  @UiThread
  private void updateViewType(RenderInfo renderInfo) {
    final ViewCreator viewCreator = renderInfo.getViewCreator();
    if (!mViewCreatorToUsageCount.containsKey(viewCreator)) {
      throw new IllegalStateException(
          "Trying to update viewType of RenderInfo whose ViewCreator isn't being tracked.");
    }

    final int viewType =
        mViewTypeToViewCreator.keyAt(mViewTypeToViewCreator.indexOfValue(viewCreator));
    renderInfo.setViewType(viewType);
  }

  @UiThread
  public void maybeUpdateViewCreatorMappingsOnItemRemove(RenderInfo renderInfo) {
    if (!renderInfo.rendersView()) {
      return;
    }

    final ViewCreator viewCreator = renderInfo.getViewCreator();
    if (!mViewCreatorToUsageCount.containsKey(viewCreator)) {
      throw new IllegalStateException("Trying to remove ViewCreator that isn't being tracked.");
    }

    int usageCount = mViewCreatorToUsageCount.get(viewCreator);
    usageCount--;
    if (usageCount == 0) {
      mViewCreatorToUsageCount.remove(viewCreator);
      final int index = mViewTypeToViewCreator.indexOfValue(viewCreator);
      mViewTypeToViewCreator.removeAt(index);
    } else if (usageCount < 0) {
      throw new IllegalStateException("Usage count of ViewCreator cannot be negative.");
    } else {
      mViewCreatorToUsageCount.put(viewCreator, usageCount);
    }
  }

  public void maybeUpdateViewCreatorMappingsOnItemUpdate(
      RenderInfo previousRenderInfo, RenderInfo newRenderInfo) {

    // If the old and new RenderInfo items have the same ViewCreator, there is no need to update
    // ViewCreator related mappings, as otherwise it might remove and insert the same item which
    // will change its viewType, creating unnecessary remove/add animations instead of update one.
    final boolean skipUpdatingViewCreatorMappings =
        previousRenderInfo.rendersView()
            && newRenderInfo.rendersView()
            && previousRenderInfo.getViewCreator() == newRenderInfo.getViewCreator();

    if (skipUpdatingViewCreatorMappings) {
      updateViewType(newRenderInfo);
    } else {
      maybeUpdateViewCreatorMappingsOnItemRemove(previousRenderInfo);
      maybeUpdateViewCreatorMappingsOnItemInsert(newRenderInfo);
    }
  }

  public ViewCreator getViewCreator(int viewType) {
    return mViewTypeToViewCreator.get(viewType);
  }
}
