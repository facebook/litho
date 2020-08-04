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

package com.facebook.rendercore.visibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisibilityModuleInput {
  private ArrayList<IncrementalModuleItem> mIncrementalVisibilityItemsTops;
  private ArrayList<IncrementalModuleItem> mIncrementalVisibilitytemsBottoms;
  private ArrayList<IncrementalModuleItem> mIncrementalFullImpressionItemsTops;
  private ArrayList<IncrementalModuleItem> mIncrementalFullImpressionItemsBottoms;
  private ArrayList<FocusedIncrementalModuleItem> mIncrementalFocusedItems;
  private ArrayList<VisibilityOutput> mVisibilityChangedOutputs;

  public void setIncrementalModuleItems(List<VisibilityOutput> visibilityOutputs) {
    clear();
    for (int i = 0, size = visibilityOutputs.size(); i < size; i++) {
      final VisibilityOutput visibilityOutput = visibilityOutputs.get(i);

      maybeAddFocusedItem(visibilityOutput);
      maybeAddFullImpressionyItem(visibilityOutput);
      maybeAddVisibilityItem(visibilityOutput);
      maybeAddVisibilityChangedItem(visibilityOutput);
    }

    sortItems();
  }

  private void clear() {
    mIncrementalFullImpressionItemsTops = null;
    mIncrementalFullImpressionItemsBottoms = null;
    mIncrementalFullImpressionItemsTops = null;
    mIncrementalFullImpressionItemsBottoms = null;
    mIncrementalFocusedItems = null;
    mVisibilityChangedOutputs = null;
  }

  private void maybeAddVisibilityItem(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getVisibleEventHandler() == null
        && visibilityOutput.getInvisibleEventHandler() == null) {
      return;
    }

    if (mIncrementalVisibilityItemsTops == null) {
      mIncrementalVisibilityItemsTops = new ArrayList<>(2);
      mIncrementalVisibilitytemsBottoms = new ArrayList<>(2);
    }

    final VisibleIncrementalModuleItem item = new VisibleIncrementalModuleItem(visibilityOutput);
    mIncrementalVisibilityItemsTops.add(item);
    mIncrementalVisibilitytemsBottoms.add(item);
  }

  private void maybeAddFocusedItem(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFocusedEventHandler() == null
        && visibilityOutput.getUnfocusedEventHandler() == null) {
      return;
    }

    if (mIncrementalFocusedItems == null) {
      mIncrementalFocusedItems = new ArrayList<>(2);
    }

    mIncrementalFocusedItems.add(new FocusedIncrementalModuleItem(visibilityOutput));
  }

  private void maybeAddFullImpressionyItem(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFullImpressionEventHandler() == null) {
      return;
    }

    if (mIncrementalFullImpressionItemsTops == null) {
      mIncrementalFullImpressionItemsTops = new ArrayList<>(2);
      mIncrementalFullImpressionItemsBottoms = new ArrayList<>(2);
    }

    final FullImpressionIncrementalModuleItem item =
        new FullImpressionIncrementalModuleItem(visibilityOutput);
    mIncrementalFullImpressionItemsTops.add(item);
    mIncrementalFullImpressionItemsBottoms.add(item);
  }

  private void maybeAddVisibilityChangedItem(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getVisibilityChangedEventHandler() == null) {
      return;
    }

    if (mVisibilityChangedOutputs == null) {
      mVisibilityChangedOutputs = new ArrayList<>(2);
    }

    mVisibilityChangedOutputs.add(visibilityOutput);
  }

  private void sortItems() {
    if (mIncrementalVisibilityItemsTops != null) {
      Collections.sort(mIncrementalVisibilityItemsTops, IncrementalModule.sTopsComparators);
      Collections.sort(mIncrementalVisibilitytemsBottoms, IncrementalModule.sBottomsComparator);
    }

    if (mIncrementalFullImpressionItemsTops != null) {
      Collections.sort(mIncrementalFullImpressionItemsTops, IncrementalModule.sTopsComparators);
      Collections.sort(
          mIncrementalFullImpressionItemsBottoms, IncrementalModule.sBottomsComparator);
    }
  }

  public ArrayList<IncrementalModuleItem> getIncrementalVisibilityItemsTops() {
    return mIncrementalVisibilityItemsTops;
  }

  public ArrayList<IncrementalModuleItem> getIncrementalVisibilityItemsBottoms() {
    return mIncrementalVisibilitytemsBottoms;
  }

  public ArrayList<IncrementalModuleItem> getFullImpressionItemsTops() {
    return mIncrementalFullImpressionItemsTops;
  }

  public ArrayList<IncrementalModuleItem> getFullImpressionItemsBottoms() {
    return mIncrementalFullImpressionItemsBottoms;
  }

  public ArrayList<FocusedIncrementalModuleItem> getIncrementalFocusedItems() {
    return mIncrementalFocusedItems;
  }

  public ArrayList<VisibilityOutput> getVisibilityChangedOutputs() {
    return mVisibilityChangedOutputs;
  }

  static void processInvisible(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getInvisibleEventHandler() != null) {
      VisibilityUtils.dispatchOnInvisible(visibilityOutput.getInvisibleEventHandler());
    }
  }

  static void processVisible(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getVisibleEventHandler() != null) {
      VisibilityUtils.dispatchOnVisible(visibilityOutput.getVisibleEventHandler());
    }
  }

  static void processFocused(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFocusedEventHandler() != null) {
      VisibilityUtils.dispatchOnFocused(visibilityOutput.getFocusedEventHandler());
    }
  }

  static void processUnfocused(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getUnfocusedEventHandler() != null) {
      VisibilityUtils.dispatchOnUnfocused(visibilityOutput.getUnfocusedEventHandler());
    }
  }

  static void processFullImpressionHandler(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFullImpressionEventHandler() != null) {
      VisibilityUtils.dispatchOnFullImpression(visibilityOutput.getFullImpressionEventHandler());
    }
  }
}
