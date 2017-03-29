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
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Drawable that draws border lines with given color and given border widths.
 */
public class BorderColorDrawable extends Drawable {

  private static final RectF mBorderBounds = new RectF();
  private final Paint mPaint = new Paint();
  private float mLeftBorderWidth;
  private float mTopBorderWidth;
  private float mRightBorderWidth;
  private float mBottomBorderWidth;

  BorderColorDrawable() {
  }

  public void init(
      int color,
      float leftBorderWidth,
      float topBorderWidth,
      float rightBorderWidth,
      float bottomBorderWidth) {
    mPaint.setColor(color);
    mLeftBorderWidth = leftBorderWidth;
    mTopBorderWidth = topBorderWidth;
    mRightBorderWidth = rightBorderWidth;
    mBottomBorderWidth = bottomBorderWidth;
  }

  @Override
  public void draw(Canvas canvas) {
    if (mLeftBorderWidth == 0 &&
        mTopBorderWidth == 0 &&
        mRightBorderWidth == 0 &&
        mBottomBorderWidth == 0) {
      return;
    }

    final Rect bounds = getBounds();

    // Draw left border.
    if (mLeftBorderWidth > 0) {
      drawBorder(
          canvas,
          bounds.left,
          bounds.top,
          Math.min(bounds.left + mLeftBorderWidth, bounds.right),
          bounds.bottom);
    }

    // Draw top border.
    if (mTopBorderWidth > 0) {
      drawBorder(
          canvas,
          bounds.left,
          bounds.top,
          bounds.right,
          Math.min(bounds.top + mTopBorderWidth, bounds.bottom));
    }

    // Draw right border.
    if (mRightBorderWidth > 0) {
      drawBorder(
          canvas,
          Math.max(bounds.right - mRightBorderWidth, bounds.left),
          bounds.top,
          bounds.right,
          bounds.bottom);
    }

    // Draw bottom border.
    if (mBottomBorderWidth > 0) {
      drawBorder(
          canvas,
          bounds.left,
          Math.max(bounds.bottom - mBottomBorderWidth, bounds.top),
          bounds.right,
          bounds.bottom);
    }
  }

  @Override
  public void setAlpha(int alpha) {
    // no-op
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    // no-op
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }

  private void drawBorder(Canvas canvas, float left, float top, float right, float bottom) {
    mBorderBounds.set(left, top, right, bottom);
    canvas.drawRect(mBorderBounds, mPaint);
  }
}
