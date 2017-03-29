/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.yoga.YogaDirection;
import com.facebook.components.reference.Reference;

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
  private String mTransitionKey;

  void setBackground(Reference<? extends Drawable> background) {
    mBackground = (Reference<Drawable>) background;
  }

  Reference<Drawable> getBackground() {
    return mBackground;
  }

  void setForeground(Reference<? extends Drawable> foreground) {
    mForeground = (Reference<Drawable>) foreground;
  }

  Reference<Drawable> getForeground() {
    return mForeground;
  }

  int getPaddingLeft() {
    return (mPadding != null) ? mPadding.left : 0;
  }

  int getPaddingTop() {
    return (mPadding != null) ? mPadding.top : 0;
  }

  int getPaddingRight() {
    return (mPadding != null) ? mPadding.right : 0;
  }

  int getPaddingBottom() {
    return (mPadding != null) ? mPadding.bottom : 0;
  }

  void setPadding(int l, int t, int r, int b) {
