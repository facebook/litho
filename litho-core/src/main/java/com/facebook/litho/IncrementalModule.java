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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Module for incrementally checking whether items are entering/exiting a range and perform
 * operations when those events occur. Example use cases: incrementally mounting components when
 * scrolling, incrementally processing visibility handlers.
 */
class IncrementalModule {

  private final View mView;
  private final Map<String, IncrementalModuleItem> mPreviousIncrementalVertical = new HashMap<>();

  /** A list of items sorted by the range top position using {@link #sTopsComparators}. */
  private final ArrayList<IncrementalModuleItem> mTops = new ArrayList<>();
  /** A list of items sorted by the range bottom position using {@link #sBottomsComparator}. */
  private final ArrayList<IncrementalModuleItem> mBottoms = new ArrayList<>();

  /**
   * mPreviousTopIndex is the index of the item with the lowest top point that doesn't qualify to be
   * in range above the bottom of the visible rect. Between two non-dirty mounts, it's used as a
   * starting point to check if the range status of an item changed as it moved above or below the
   * bottom of the visible rect.
   */
  private int mPreviousTopIndex;

  /**
   * mPreviousBottomIndex is the index of the item with the lowest bottom point that qualifies to be
   * in range below the top of the visible rect. Between two non-dirty mounts, it's used as a
   * starting point to check if the range status of an item changed as it moved above or below the
   * top of the visible rect.
   */
  private int mPreviousBottomIndex;

  /**
   * This is a temporary wrapper around VisibilityOutput, the plan is to make VisibilityOutput
   * implement this in a future diff, so we don't have two abstractions for the same thing. Right
   * now it only supports vertical incremental handling.
   */
  public interface IncrementalModuleItem {
    String getId();

    Rect getBounds();

    /**
     * The minimum point from top of this item which would need to be visible so that this items
     * qualifies as "in range".
     */
    float getEnterRangeTop();

    /**
     * The minimum point from bottom of this item which would need to be visible so that this item
     * qualifies as "in range".
     */
    float getEnterRangeBottom();

    void onEnterVisibleRange();

    void onExitVisibleRange();

    /**
     * We might need to do some setup when the LithoView is available. Ex: for focused events we
     * need to know the size of the parent LithoView to decide how much the item needs to be visible
     * to be eligible.
     */
    void onLithoViewAvailable(LithoView lithoView);
  }

  static final Comparator<IncrementalModuleItem> sTopsComparators =
      new Comparator<IncrementalModuleItem>() {
        @Override
        public int compare(IncrementalModuleItem lhs, IncrementalModuleItem rhs) {
          final float lhsTop = lhs.getEnterRangeTop();
          final float rhsTop = rhs.getEnterRangeTop();
          return (int) (lhsTop - rhsTop);
        }
      };

  static final Comparator<IncrementalModuleItem> sBottomsComparator =
      new Comparator<IncrementalModuleItem>() {
        @Override
        public int compare(IncrementalModuleItem lhs, IncrementalModuleItem rhs) {
          final float lhsBottom = lhs.getEnterRangeBottom();
          final float rhsBottom = rhs.getEnterRangeBottom();
          return (int) (lhsBottom - rhsBottom);
        }
      };

  IncrementalModule(View view) {
    mView = view;
  }

  /**
   * Triggers exit range callbacks for all items which were inside the range, used for example when
   * the LithoView is not visible anymore.
   */
  void clearIncrementalVisibilityItems() {
    for (String key : mPreviousIncrementalVertical.keySet()) {
      mPreviousIncrementalVertical.get(key).onExitVisibleRange();
    }

    mPreviousIncrementalVertical.clear();
    mTops.clear();
    mBottoms.clear();
  }

  /**
   * Checks if an item's range top is below the bottom range. If true, it means the item is outside
   * range.
   */
  private static boolean isBelowViewportBottom(Rect visibleRect, IncrementalModuleItem item) {
    return visibleRect.bottom < item.getEnterRangeTop()
        || (visibleRect.bottom == item.getEnterRangeTop()
            && item.getBounds().top >= visibleRect.bottom);
  }

  /**
   * Checks if an item's range top is above the bottom range. If true, it means the item is inside
   * range.
   */
  private static boolean isAboveViewportBottom(Rect visibleRect, IncrementalModuleItem item) {
    return !isBelowViewportBottom(visibleRect, item);
  }

  /**
   * Checks if an item's range bottom is below the top range. If true, it means the item is inside
   * range.
   */
  private static boolean isBelowViewportTop(Rect visibleRect, IncrementalModuleItem item) {
    return visibleRect.top < item.getEnterRangeBottom()
        || (visibleRect.top == item.getEnterRangeBottom()
            && item.getBounds().bottom > visibleRect.top);
  }

  /**
   * Checks if an item's range bottom is above the top range. If true, it means the item is outside
   * range.
   */
  private static boolean isAboveViewportTop(Rect visibleRect, IncrementalModuleItem item) {
    return !isBelowViewportTop(visibleRect, item);
  }

  /**
   * Sets up initial incremental data after a dirty mount. It sorts the incremental items mTops and
   * mBottoms and calculates the top and bottom indexes which are the bounds of the range.
   */
  private void setupInitialIncrementalData(
      @Nullable List<IncrementalModuleItem> sortedTops,
      @Nullable List<IncrementalModuleItem> sortedBottoms,
      Rect localVisibleRect) {
    if (localVisibleRect == null) {
      return;
    }

    setupInitialVerticalData(sortedTops, sortedBottoms, localVisibleRect);
  }

  private void setupInitialVerticalData(
      @Nullable List<IncrementalModuleItem> sortedTops,
      @Nullable List<IncrementalModuleItem> sortedBottoms,
      Rect localVisibleRect) {
    mTops.clear();
    mBottoms.clear();

    // Dirty mount with no incremental items.
    if (sortedTops == null || sortedBottoms == null) {
      processPreviousVisibilityOutputs(
          mPreviousIncrementalVertical, new HashMap<String, IncrementalModuleItem>(0));
      return;
    }

    mTops.addAll(sortedTops);
    mBottoms.addAll(sortedBottoms);

    /** Keeps track of all the items that entered range immediately. */
    final Map<String, IncrementalModuleItem> current = new HashMap<>();

    final int count = mTops.size();

    /** Find the item with the first top below the bottom range. */
    mPreviousTopIndex = count;
    for (int i = 0; i < count; i++) {
      if (isBelowViewportBottom(localVisibleRect, mTops.get(i))) {
        mPreviousTopIndex = i;
        break;
      }

      /** This means the item is in range. */
      current.put(mTops.get(i).getId(), mTops.get(i));
    }

    /** Find the item with the first bottom below the top range. */
    mPreviousBottomIndex = count;
    for (int i = 0; i < count; i++) {
      if (isBelowViewportTop(localVisibleRect, mBottoms.get(i))) {
        mPreviousBottomIndex = i;
        break;
      }

      /**
       * This means the item is not in range, if it was above the bottom of the visible rect it
       * needs to be removed.
       */
      current.remove(mBottoms.get(i).getId());
    }

    /**
     * We might have items with the same bottom range position. In that case, we find the largest
     * index that matches that bottom range position and on non-dirty mounts we move towards the
     * start of the sorted bottom array to see if the range status changed.
     */
    while (mPreviousBottomIndex < (count - 1)
        && mBottoms.get(mPreviousBottomIndex).getEnterRangeBottom()
            == mBottoms.get(mPreviousBottomIndex + 1).getEnterRangeBottom()) {
      mPreviousBottomIndex++;
    }

    processPreviousVisibilityOutputs(mPreviousIncrementalVertical, current);
  }

  /**
   * Called after a dirty mount during initial setup. Compares the list of items currently in range
   * with items that were in range on the last known mounted state. It skips dispatching range
   * entered callbacks if the items were previously in range. It dispatches range exited callbacks
   * if the items were previously in range but have been removed or went outside the range after a
   * dirty mount.
   */
  private static void processPreviousVisibilityOutputs(
      Map<String, IncrementalModuleItem> previous, Map<String, IncrementalModuleItem> current) {
    final List<String> toRemove = new ArrayList<>();

    for (Entry<String, IncrementalModuleItem> entry : previous.entrySet()) {
      final String key = entry.getKey();
      if (!current.containsKey(key)) {
        previous.get(key).onExitVisibleRange();
        toRemove.add(key);
      }
    }

    for (int i = 0; i < toRemove.size(); i++) {
      previous.remove(toRemove.get(i));
    }

    for (Entry<String, IncrementalModuleItem> entry : current.entrySet()) {
      final String key = entry.getKey();
      if (!previous.containsKey(key)) {
        current.get(key).onEnterVisibleRange();
      }
      previous.put(key, current.get(key));
    }
  }

  boolean performIncrementalProcessing(
      boolean isDirty,
      @Nullable List<IncrementalModuleItem> sortedTops,
      @Nullable List<IncrementalModuleItem> sortedBottoms,
      @Nullable Rect localVisibleRect,
      Rect previousLocalVisibleRect) {
    if (localVisibleRect == null) {
      return false;
    }

    if (isDirty || mTops.isEmpty() || mBottoms.isEmpty()) {
      setupInitialIncrementalData(sortedTops, sortedBottoms, localVisibleRect);
      return false;
    }

    final int visCount = mTops.size();

    /** Check if range status changed around the top bounds of the visible rect. */
    if (localVisibleRect.top >= 0 || previousLocalVisibleRect.top >= 0) {
      while (mPreviousBottomIndex < visCount
          && isAboveViewportTop(localVisibleRect, mBottoms.get(mPreviousBottomIndex))) {
        mBottoms.get(mPreviousBottomIndex).onExitVisibleRange();
        mPreviousBottomIndex++;
      }

      while (mPreviousBottomIndex > 0
          && isBelowViewportTop(localVisibleRect, mBottoms.get(mPreviousBottomIndex - 1))) {
        mPreviousBottomIndex--;
        mBottoms.get(mPreviousBottomIndex).onEnterVisibleRange();
      }
    }

    /** Check if range status changed around the bottom bounds of the visible rect. */
    final int height = mView.getHeight();
    if (localVisibleRect.bottom <= height || previousLocalVisibleRect.bottom <= height) {
      while (mPreviousTopIndex < visCount
          && isAboveViewportBottom(localVisibleRect, mTops.get(mPreviousTopIndex))) {
        mTops.get(mPreviousTopIndex).onEnterVisibleRange();
        mPreviousTopIndex++;
      }

      while (mPreviousTopIndex > 0
          && isBelowViewportBottom(localVisibleRect, mTops.get(mPreviousTopIndex - 1))) {
        mPreviousTopIndex--;
        mTops.get(mPreviousTopIndex).onExitVisibleRange();
      }
    }

    return true;
  }
}
