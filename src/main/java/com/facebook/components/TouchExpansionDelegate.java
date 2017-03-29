/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v4.util.SparseArrayCompat;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Compound touch delegate that forward touch events to recyclable
 * inner touch delegates.
 */
class TouchExpansionDelegate extends TouchDelegate {

  private static final Rect IGNORED_RECT = new Rect();
  private static final Pools.SimplePool<SparseArrayCompat<InnerTouchDelegate>>
      sInnerTouchDelegateScrapArrayPool = new Pools.SimplePool<>(4);
