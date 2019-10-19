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

import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class StaggeredGridLayoutHelper {
  private static int[] mItemPositionsHolder;

  public static int findFirstVisibleItemPosition(
      StaggeredGridLayoutManager staggeredGridLayoutManager) {
    if (mItemPositionsHolder == null) {
      mItemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
    }
    return min(staggeredGridLayoutManager.findFirstVisibleItemPositions(mItemPositionsHolder));
  }

  public static int findLastVisibleItemPosition(
      StaggeredGridLayoutManager staggeredGridLayoutManager) {
    if (mItemPositionsHolder == null) {
      mItemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
    }
    return max(staggeredGridLayoutManager.findLastVisibleItemPositions(mItemPositionsHolder));
  }

  public static int findFirstFullyVisibleItemPosition(
      StaggeredGridLayoutManager staggeredGridLayoutManager) {
    if (mItemPositionsHolder == null) {
      mItemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
    }
    return min(
        staggeredGridLayoutManager.findFirstCompletelyVisibleItemPositions(mItemPositionsHolder));
  }

  public static int findLastFullyVisibleItemPosition(
      StaggeredGridLayoutManager staggeredGridLayoutManager) {
    if (mItemPositionsHolder == null) {
      mItemPositionsHolder = new int[staggeredGridLayoutManager.getSpanCount()];
    }
    return max(
        staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(mItemPositionsHolder));
  }

  private static int min(int[] a) {
    int min = Integer.MAX_VALUE;
    for (int i : a) {
      if (i < min) {
        min = i;
      }
    }
    return min;
  }

  private static int max(int[] a) {
    int max = Integer.MIN_VALUE;
    for (int i : a) {
      if (i > max) {
        max = i;
      }
    }
    return max;
  }
}
