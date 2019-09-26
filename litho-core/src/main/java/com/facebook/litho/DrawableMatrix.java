/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;
import javax.annotation.Nullable;

/** Static class containing a factory method for creating a matrix to apply to a drawable. */
public final class DrawableMatrix extends Matrix {

  private boolean mShouldClipRect;

  private DrawableMatrix() {}

  /** @return True if this Matrix requires a clipRect() on the bounds of the drawable. */
  public boolean shouldClipRect() {
    return mShouldClipRect;
  }

  /**
   * Create a matrix to be applied to a drawable which scales the drawable according to its scale
   * type.
   *
   * @param d The drawable to create a matrix for
   * @param scaleType A scale type describing how to scale the drawable.
   * @param width The width of the drawable's container.
   * @param height The height of the drawable's container.
   * @return The scale matrix or null if the drawable does not need to be scaled.
   */
  public static @Nullable DrawableMatrix create(
      final Drawable d, ScaleType scaleType, final int width, final int height) {

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
    } else if (ScaleType.CENTER_INSIDE == scaleType) {

      final float scale;
      if (intrinsicWidth <= width && intrinsicHeight <= height) {
        scale = 1.0f;
      } else {
        scale =
            Math.min(
                (float) width / (float) intrinsicWidth, (float) height / (float) intrinsicHeight);
      }

      final float dx = FastMath.round((width - intrinsicWidth * scale) * 0.5f);
      final float dy = FastMath.round((height - intrinsicHeight * scale) * 0.5f);

      result.setScale(scale, scale);
      result.postTranslate(dx, dy);
    } else {
      RectF src = new RectF();
      RectF dest = new RectF();

      // Generate the required transform.
      src.set(0, 0, intrinsicWidth, intrinsicHeight);
      dest.set(0, 0, width, height);

      result.setRectToRect(src, dest, scaleTypeToScaleToFit(scaleType));
    }

    return result;
  }

  private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType st) {
    // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
    switch (st) {
      case FIT_XY:
        return Matrix.ScaleToFit.FILL;
      case FIT_START:
        return Matrix.ScaleToFit.START;
      case FIT_CENTER:
        return Matrix.ScaleToFit.CENTER;
      case FIT_END:
        return Matrix.ScaleToFit.END;

      default:
        throw new IllegalArgumentException("Only FIT_... values allowed");
    }
  }
}
