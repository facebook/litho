/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

import com.facebook.components.SizeSpec;

public class GridLayoutInfo implements LayoutInfo {

  private final GridLayoutManager mGridLayoutManager;

  public GridLayoutInfo(Context context, int spanCount, int orientation, boolean reverseLayout) {
    mGridLayoutManager = new GridLayoutManager(context, spanCount, orientation, reverseLayout);
  }

  public GridLayoutInfo(Context context, int spanCount) {
    this(context, spanCount, OrientationHelper.VERTICAL, false);
  }

