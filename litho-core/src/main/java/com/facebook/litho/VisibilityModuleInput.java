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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisibilityModuleInput {
  private ArrayList<IncrementalModule.IncrementalModuleItem> mIncrementalVisibilityItemsTops;
  private ArrayList<IncrementalModule.IncrementalModuleItem> mIncrementalVisibilitytemsBottoms;
  private ArrayList<IncrementalModule.IncrementalModuleItem> mIncrementalFullImpressionItemsTops;
  private ArrayList<IncrementalModule.IncrementalModuleItem> mIncrementalFullImpressionItemsBottoms;
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

  ArrayList<IncrementalModule.IncrementalModuleItem> getIncrementalVisibilityItemsTops() {
    return mIncrementalVisibilityItemsTops;
  }

  ArrayList<IncrementalModule.IncrementalModuleItem> getIncrementalVisibilityItemsBottoms() {
    return mIncrementalVisibilitytemsBottoms;
  }

  ArrayList<IncrementalModule.IncrementalModuleItem> getFullImpressionItemsTops() {
    return mIncrementalFullImpressionItemsTops;
  }

  ArrayList<IncrementalModule.IncrementalModuleItem> getFullImpressionItemsBottoms() {
    return mIncrementalFullImpressionItemsBottoms;
  }

  ArrayList<FocusedIncrementalModuleItem> getIncrementalFocusedItems() {
    return mIncrementalFocusedItems;
  }

  ArrayList<VisibilityOutput> getVisibilityChangedOutputs() {
    return mVisibilityChangedOutputs;
  }

  private static void processInvisible(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getInvisibleEventHandler() != null) {
      EventDispatcherUtils.dispatchOnInvisible(visibilityOutput.getInvisibleEventHandler());
    }
  }

  private static void processVisible(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getVisibleEventHandler() != null) {
      EventDispatcherUtils.dispatchOnVisible(visibilityOutput.getVisibleEventHandler());
    }
  }

  private static void processFocused(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFocusedEventHandler() != null) {
      EventDispatcherUtils.dispatchOnFocused(visibilityOutput.getFocusedEventHandler());
    }
  }

  private static void processUnfocused(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getUnfocusedEventHandler() != null) {
      EventDispatcherUtils.dispatchOnUnfocused(visibilityOutput.getUnfocusedEventHandler());
    }
  }

  private static void processFullImpressionHandler(VisibilityOutput visibilityOutput) {
    if (visibilityOutput.getFullImpressionEventHandler() != null) {
      EventDispatcherUtils.dispatchOnFullImpression(
          visibilityOutput.getFullImpressionEventHandler());
    }
  }

  static final class VisibleIncrementalModuleItem
      implements IncrementalModule.IncrementalModuleItem {
    private final VisibilityOutput mVisibilityOutput;

    VisibleIncrementalModuleItem(VisibilityOutput visibilityOutput) {
      this.mVisibilityOutput = visibilityOutput;
    }

    @Override
    public String getId() {
      return "v_" + mVisibilityOutput.getId();
    }

    @Override
    public Rect getBounds() {
      return mVisibilityOutput.getBounds();
    }

    @Override
    public float getEnterRangeTop() {
      return mVisibilityOutput.getVisibilityTop();
    }

    @Override
    public float getEnterRangeBottom() {
      return mVisibilityOutput.getVisibilityBottom();
    }

    @Override
    public void onEnterVisibleRange() {
      processVisible(mVisibilityOutput);
    }

    @Override
    public void onExitVisibleRange() {
      processInvisible(mVisibilityOutput);
    }

    @Override
    public void onLithoViewAvailable(LithoView lithoView) {}
  }

  static final class FocusedIncrementalModuleItem
      implements IncrementalModule.IncrementalModuleItem {

    private final VisibilityOutput mVisibilityOutput;

    FocusedIncrementalModuleItem(VisibilityOutput visibilityOutput) {
      mVisibilityOutput = visibilityOutput;
    }

    @Override
    public String getId() {
      return "f_" + mVisibilityOutput.getId();
    }

    @Override
    public Rect getBounds() {
      return mVisibilityOutput.getBounds();
    }

    @Override
    public float getEnterRangeTop() {
      return mVisibilityOutput.getFocusedTop();
    }

    @Override
    public float getEnterRangeBottom() {
      return mVisibilityOutput.getFocusedBottom();
    }

    @Override
    public void onEnterVisibleRange() {
      processFocused(mVisibilityOutput);
    }

    @Override
    public void onExitVisibleRange() {
      processUnfocused(mVisibilityOutput);
    }

    public void onLithoViewAvailable(LithoView lithoView) {
      final View parent = (View) lithoView.getParent();
      if (parent == null) {
        return;
      }

      final int halfViewportArea = parent.getWidth() * parent.getHeight() / 2;

      if (mVisibilityOutput.getComponentArea() >= halfViewportArea) {
        float ratio = 0.5f * halfViewportArea / halfViewportArea;
        mVisibilityOutput.setFocusedRatio(ratio);
      } else {
        mVisibilityOutput.setFocusedRatio(1.0f);
      }
    }
  }

  static final class FullImpressionIncrementalModuleItem
      implements IncrementalModule.IncrementalModuleItem {

    private final VisibilityOutput mVisibilityOutput;

    FullImpressionIncrementalModuleItem(VisibilityOutput visibilityOutput) {
      mVisibilityOutput = visibilityOutput;
    }

    @Override
    public String getId() {
      return "fi_" + mVisibilityOutput.getId();
    }

    @Override
    public Rect getBounds() {
      return mVisibilityOutput.getBounds();
    }

    @Override
    public float getEnterRangeTop() {
      return mVisibilityOutput.getFullImpressionTop();
    }

    @Override
    public float getEnterRangeBottom() {
      return mVisibilityOutput.getFullImpressionBottom();
    }

    @Override
    public void onEnterVisibleRange() {
      processFullImpressionHandler(mVisibilityOutput);
    }

    @Override
    public void onExitVisibleRange() {}

    @Override
    public void onLithoViewAvailable(LithoView lithoView) {}
  }
}
