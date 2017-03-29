/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * A Drawable that wraps another drawable.
 */
public class MatrixDrawable<T extends Drawable> extends Drawable
    implements Drawable.Callback, Touchable {

  public static final int UNSET = -1;

  private T mDrawable;
  private DrawableMatrix mMatrix;
  private boolean mShouldClipRect;
  private int mWidth;
  private int mHeight;

  public MatrixDrawable() {
    super();
  }

  public void mount(T drawable, DrawableMatrix matrix) {

    if (mDrawable == drawable) {
      return;
    }

    if (mDrawable != null) {
      mDrawable.setCallback(null);
    }

    mDrawable = drawable;

    if (mDrawable != null) {
      mDrawable.setCallback(this);
    }

    mMatrix = matrix;

    // We should clip rect if either the transformation matrix needs so or
    // if a ColorDrawable in Gingerbread is being drawn because it doesn't
    // respect its bounds.
    mShouldClipRect = (mMatrix != null && mMatrix.shouldClipRect()) ||
        (Build.VERSION.SDK_INT < HONEYCOMB && mDrawable instanceof ColorDrawable) ||
        (mDrawable instanceof InsetDrawable);

    invalidateSelf();
  }

  /**
   * Sets the necessary artifacts to display the given drawable.
   * This method should be called in your component's @OnMount method.
   *
   * @param drawable The drawable to be drawn.
   */
  public void mount(T drawable) {
