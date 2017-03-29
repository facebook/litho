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

  private final SparseArrayCompat<InnerTouchDelegate> mDelegates = new SparseArrayCompat<>();
  private SparseArrayCompat<InnerTouchDelegate> mScrapDelegates;

  TouchExpansionDelegate(ComponentHost host) {
    super(IGNORED_RECT, host);
  }

  /**
   * Registers an inner touch delegate for the given view with the specified
   * expansion. It assumes the given view has its final bounds set.
   *
   * @param index The drawing order index of the given view.
   * @param view The view to which touch expansion should be applied.
   * @param touchExpansion The expansion to be applied to each edge of the given view.
   */
  void registerTouchExpansion(int index, View view, Rect touchExpansion) {
    mDelegates.put(index, InnerTouchDelegate.acquire(view, touchExpansion));
  }

  /**
   * Unregisters an inner touch delegate with the given index.
   *
   * @param index The drawing order index of the given view.
   */
  void unregisterTouchExpansion(int index) {
    if (maybeUnregisterFromScrap(index)) {
      return;
    }

    final int valueIndex = mDelegates.indexOfKey(index);
