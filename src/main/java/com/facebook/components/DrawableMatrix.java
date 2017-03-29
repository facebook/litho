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
      final Drawable d,
      ScaleType scaleType,
      final int width,
      final int height) {

    if (scaleType == null) {
      scaleType = ScaleType.FIT_CENTER;
    }

    if (d == null) {
      return null;
    }

    final int intrinsicWidth = d.getIntrinsicWidth();
    final int intrinsicHeight = d.getIntrinsicHeight();

    if (intrinsicWidth <= 0
        || intrinsicHeight <= 0
        || ScaleType.FIT_XY == scaleType
        || ScaleType.MATRIX == scaleType) {
      return null;
    }

    if (width == intrinsicWidth && height == intrinsicHeight) {
      // The bitmap fits exactly, no transform needed.
      return null;
    }

    final DrawableMatrix result = new DrawableMatrix();

    if (ScaleType.CENTER == scaleType) {
      // Center bitmap in view, no scaling.
      result.setTranslate(
          FastMath.round((width - intrinsicWidth) * 0.5f),
          FastMath.round((height - intrinsicHeight) * 0.5f));

      result.mShouldClipRect = (intrinsicWidth > width || intrinsicHeight > height);
    } else if (ScaleType.CENTER_CROP == scaleType) {

      final float scale;
      float dx = 0;
      float dy = 0;

      if (intrinsicWidth * height > width * intrinsicHeight) {
        scale = (float) height / (float) intrinsicHeight;
        dx = (width - intrinsicWidth * scale) * 0.5f;
      } else {
        scale = (float) width / (float) intrinsicWidth;
        dy = (height - intrinsicHeight * scale) * 0.5f;
      }

      result.setScale(scale, scale);
      result.postTranslate(FastMath.round(dx), FastMath.round(dy));

      result.mShouldClipRect = true;
