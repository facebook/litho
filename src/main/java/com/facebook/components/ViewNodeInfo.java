/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.yoga.YogaDirection;
import com.facebook.litho.reference.Reference;

/**
 * Additional information passed between {@link LayoutState} and {@link MountState}
 * used on a {@link View}.
 */
class ViewNodeInfo {

  private final AtomicInteger mReferenceCount = new AtomicInteger(0);

  private Reference<Drawable> mBackground;
  private Reference<Drawable> mForeground;
  private Rect mPadding;
  private Rect mExpandedTouchBounds;
  private YogaDirection mLayoutDirection;
