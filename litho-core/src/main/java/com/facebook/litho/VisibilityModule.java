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

package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.litho.IncrementalModule.IncrementalModuleItem;
import com.facebook.litho.VisibilityModuleInput.FocusedIncrementalModuleItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisibilityModule {
  private @Nullable IncrementalModule mIncrementalModuleVisibility;
  private @Nullable IncrementalModule mIncrementalModuleFullImpression;
  private @Nullable IncrementalModule mIncrementalModuleFocused;

  private @Nullable Map<String, VisibilityOutput> mVisibilityRatioChanged;
  private final List<IncrementalModuleItem> mLazySortTops = new ArrayList<>();
  private final List<IncrementalModuleItem> mLazySortBottoms = new ArrayList<>();
  private View mView;

  public VisibilityModule(View view) {
    mView = view;
    mIncrementalModuleVisibility = new IncrementalModule(view);
    mIncrementalModuleFullImpression = new IncrementalModule(view);
    mIncrementalModuleFocused = new IncrementalModule(view);
    mVisibilityRatioChanged = new HashMap<>();
  }

  void processVisibilityOutputs(
      boolean isDirty,
      VisibilityModuleInput visibilityModuleInput,
      @Nullable Rect localVisibleRect,
      @Nullable Rect previousLocalVisibleRect) {
    if (mView == null) {
      throw new IllegalStateException(
          "Trying to process visibility outputs but module has not been initialized with a LithoView");
    }

    if (mIncrementalModuleFocused != null) {
      if (isDirty) {
        final List<FocusedIncrementalModuleItem> lazySortItems =
            visibilityModuleInput.getIncrementalFocusedItems();
        mLazySortTops.clear();
        mLazySortBottoms.clear();

        if (lazySortItems != null) {

          mLazySortTops.addAll(lazySortItems);
          mLazySortBottoms.addAll(lazySortItems);

          if (!lazySortItems.isEmpty()) {
            for (int i = 0, size = lazySortItems.size(); i < size; i++) {
              lazySortItems.get(i).onLithoViewAvailable(mView);
            }
            Collections.sort(mLazySortTops, IncrementalModule.sTopsComparators);
            Collections.sort(mLazySortBottoms, IncrementalModule.sBottomsComparator);
          }
        }
      }

      mIncrementalModuleFocused.performIncrementalProcessing(
          isDirty, mLazySortTops, mLazySortBottoms, localVisibleRect, previousLocalVisibleRect);
    }

    if (mIncrementalModuleVisibility != null) {
      mIncrementalModuleVisibility.performIncrementalProcessing(
          isDirty,
          visibilityModuleInput.getIncrementalVisibilityItemsTops(),
          visibilityModuleInput.getIncrementalVisibilityItemsBottoms(),
          localVisibleRect,
          previousLocalVisibleRect);
    }

    if (mIncrementalModuleFullImpression != null) {
      mIncrementalModuleFullImpression.performIncrementalProcessing(
          isDirty,
          visibilityModuleInput.getFullImpressionItemsTops(),
          visibilityModuleInput.getFullImpressionItemsBottoms(),
          localVisibleRect,
          previousLocalVisibleRect);
    }

    processNonincrementalChanges(visibilityModuleInput, localVisibleRect);
  }

  void clearIncrementalItems() {
    if (mIncrementalModuleVisibility != null) {
      mIncrementalModuleVisibility.clearIncrementalItems();
    }

    if (mIncrementalModuleFocused != null) {
      mIncrementalModuleFocused.clearIncrementalItems();
    }

    if (mIncrementalModuleFullImpression != null) {
      mIncrementalModuleFullImpression.clearIncrementalItems();
    }

    clearVisibilityChanged();
  }

  private void processNonincrementalChanges(
      VisibilityModuleInput visibilityModuleInput, Rect localVisibleRect) {
    List<VisibilityOutput> visibilityOutputs = visibilityModuleInput.getVisibilityChangedOutputs();
    if (visibilityOutputs == null || visibilityOutputs.isEmpty()) {
      clearVisibilityChanged();
      return;
    }

    for (int i = 0; i < visibilityOutputs.size(); i++) {
      VisibilityOutput visibilityOutput = visibilityOutputs.get(i);
      final Rect tempRect = new Rect();
      final Rect visibilityOutputBounds = visibilityOutput.getBounds();
      boolean intersect = tempRect.setIntersect(visibilityOutputBounds, localVisibleRect);

      if (!intersect) {
        if (mVisibilityRatioChanged.containsKey(visibilityOutput.getId()))
          EventDispatcherUtils.dispatchOnVisibilityChanged(
              visibilityOutput.getVisibilityChangedEventHandler(), 0, 0, 0f, 0f);
        mVisibilityRatioChanged.remove(visibilityOutput.getId());

        return;
      }

      final int visibleWidth = tempRect.right - tempRect.left;
      final int visibleHeight = tempRect.bottom - tempRect.top;

      EventDispatcherUtils.dispatchOnVisibilityChanged(
          visibilityOutput.getVisibilityChangedEventHandler(),
          visibleWidth,
          visibleHeight,
          100f * visibleWidth / visibilityOutputBounds.width(),
          100f * visibleHeight / visibilityOutputBounds.height());
      mVisibilityRatioChanged.put(visibilityOutput.getId(), visibilityOutput);
    }
  }

  private void clearVisibilityChanged() {
    for (Map.Entry<String, VisibilityOutput> entry : mVisibilityRatioChanged.entrySet()) {
      VisibilityOutput visibilityOutput = entry.getValue();
      EventDispatcherUtils.dispatchOnVisibilityChanged(
          visibilityOutput.getVisibilityChangedEventHandler(), 0, 0, 0, 0);
    }
    mVisibilityRatioChanged.clear();
  }
}
