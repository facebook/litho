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

import com.facebook.litho.ComponentView;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.utils.IncrementalMountUtils;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * WARNING: In order to benefit from async layout, all the items in a row need to have
 *          the same height. If items in the same row have different heights, the layout will
 *          be recomputed on the main thread.
 *          The use of {@link android.support.v7.widget.RecyclerView.ItemDecoration} will
 *          also prevent the layout pass to be computed in the background but it will run
 *          on the main thread.
 */
