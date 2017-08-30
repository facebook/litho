/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

/**
 * Drawable that draws border lines with given color and given border widths.
 */
public class BorderColorDrawable extends Drawable {

  private static final RectF mBorderBounds = new RectF();
  private final Paint mPaint = new Paint();
  private float mBorderLeftWidth;
  private float mBorderTopWidth;
  private float mBorderRightWidth;
  private float mBorderBottomWidth;
  private @ColorInt int mBorderLeftColor;
  private @ColorInt int mBorderTopColor;
  private @ColorInt int mBorderRightColor;
  private @ColorInt int mBorderBottomColor;

  BorderColorDrawable() {
  }

  public void init(
      float leftBorderWidth,
      float topBorderWidth,
      float rightBorderWidth,
      float bottomBorderWidth,
      @ColorInt int leftBorderColor,
      @ColorInt int topBorderColor,
      @ColorInt int rightBorderColor,
      @ColorInt int bottomBorderColor) {
    mBorderLeftWidth = leftBorderWidth;
    mBorderTopWidth = topBorderWidth;
    mBorderRightWidth = rightBorderWidth;
    mBorderBottomWidth = bottomBorderWidth;

    mBorderLeftColor = leftBorderColor;
    mBorderTopColor = topBorderColor;
    mBorderRightColor = rightBorderColor;
    mBorderBottomColor = bottomBorderColor;
  }

  @Override
  public void draw(Canvas canvas) {
    if (mBorderLeftWidth == 0
        && mBorderTopWidth == 0
        && mBorderRightWidth == 0
        && mBorderBottomWidth == 0) {
      return;
    }

    final Rect bounds = getBounds();

    // Draw left border.
    if (mBorderLeftWidth > 0) {
      drawBorder(
          canvas,
          mBorderLeftColor,
          bounds.left,
          bounds.top,
          Math.min(bounds.left + mBorderLeftWidth, bounds.right),
          bounds.bottom);
    }

    // Draw top border.
    if (mBorderTopWidth > 0) {
      drawBorder(
          canvas,
          mBorderTopColor,
          bounds.left,
          bounds.top,
          bounds.right,
          Math.min(bounds.top + mBorderTopWidth, bounds.bottom));
    }

    // Draw right border.
    if (mBorderRightWidth > 0) {
      drawBorder(
          canvas,
          mBorderRightColor,
          Math.max(bounds.right - mBorderRightWidth, bounds.left),
          bounds.top,
          bounds.right,
          bounds.bottom);
    }

    // Draw bottom border.
    if (mBorderBottomWidth > 0) {
      drawBorder(
          canvas,
          mBorderBottomColor,
          bounds.left,
          Math.max(bounds.bottom - mBorderBottomWidth, bounds.top),
          bounds.right,
          bounds.bottom);
    }
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }

  private void drawBorder(
      Canvas canvas, @ColorInt int color, float left, float top, float right, float bottom) {
    mPaint.setColor(color);
    mBorderBounds.set(left, top, right, bottom);
    canvas.drawRect(mBorderBounds, mPaint);
  }
}
