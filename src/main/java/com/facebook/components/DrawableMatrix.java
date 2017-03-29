/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;

/**
 * Static class containing a factory method for creating a matrix to apply to a drawable.
 */
public final class DrawableMatrix extends Matrix {

  private boolean mShouldClipRect;

  private DrawableMatrix() {
  }

  /**
   * @return True if this Matrix requires a clipRect() on the bounds of the drawable.
   */
  public boolean shouldClipRect() {
    return mShouldClipRect;
  }

  /**
   * Create a matrix to be applied to a drawable which scales the drawable according to its
   * scale type.
   *
   * @param d The drawable to create a matrix for
   * @param scaleType A scale type describing how to scale the drawable.
   * @param width The width of the drawable's container.
   * @param height The height of the drawable's container.
   * @return The scale matrix or null if the drawable does not need to be scaled.
   */
  public static DrawableMatrix create(
