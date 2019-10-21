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

package com.facebook.litho.widget;

import android.util.SparseArray;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.viewcompat.ViewCreator;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Helper class to keep track of the different view types that we're rendering using ViewRenderInfo.
 */
public class RenderInfoViewCreatorController {

  @VisibleForTesting final SparseArray<ViewCreator> mViewTypeToViewCreator = new SparseArray<>();

  @VisibleForTesting final Map<ViewCreator, Integer> mViewCreatorToViewType = new HashMap<>();

  public static final int DEFAULT_COMPONENT_VIEW_TYPE = 0;
  private final boolean mCustomViewTypeEnabled;
  private final int mComponentViewType;
  private int mViewTypeCounter;

  public RenderInfoViewCreatorController(boolean customViewTypeEnabled, int componentViewType) {
    mCustomViewTypeEnabled = customViewTypeEnabled;
    mComponentViewType = componentViewType;
    mViewTypeCounter = componentViewType + 1;
  }

  @UiThread
  public void maybeTrackViewCreator(RenderInfo renderInfo) {
    if (!renderInfo.rendersView()) {
      return;
    }

    ensureCustomViewTypeValidity(renderInfo);

    final ViewCreator viewCreator = renderInfo.getViewCreator();
    final int viewType;
    if (mViewCreatorToViewType.containsKey(viewCreator)) {
      viewType = mViewCreatorToViewType.get(viewCreator);
    } else {
      viewType = renderInfo.hasCustomViewType() ? renderInfo.getViewType() : mViewTypeCounter++;
      mViewTypeToViewCreator.put(viewType, viewCreator);
      mViewCreatorToViewType.put(viewCreator, viewType);
    }

    if (!renderInfo.hasCustomViewType()) {
      renderInfo.setViewType(viewType);
    }
  }

  private void ensureCustomViewTypeValidity(RenderInfo renderInfo) {
    if (mCustomViewTypeEnabled && !renderInfo.hasCustomViewType()) {
      throw new IllegalStateException(
          "If you enable custom viewTypes, you must provide a customViewType in ViewRenderInfo.");
    } else if (!mCustomViewTypeEnabled && renderInfo.hasCustomViewType()) {
      throw new IllegalStateException(
          "You must enable custom viewTypes to provide customViewType in ViewRenderInfo.");
    } else if (mCustomViewTypeEnabled && mComponentViewType == renderInfo.getViewType()) {
      throw new IllegalStateException("CustomViewType cannot be the same as ComponentViewType.");
    }
  }

  public @Nullable ViewCreator getViewCreator(int viewType) {
    return mViewTypeToViewCreator.get(viewType);
  }

  int getComponentViewType() {
    return mComponentViewType;
  }
}
