/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.StaggeredGridLayoutManager;

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
