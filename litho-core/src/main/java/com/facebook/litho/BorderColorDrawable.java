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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

/** Drawable that draws border lines with given color, widths and path effect. */
public class BorderColorDrawable extends Drawable {

  private static final int QUICK_REJECT_COLOR = Color.TRANSPARENT;
  private static final float CLIP_ANGLE = 45f;
  private static final RectF mClipBounds = new RectF();
  private static final RectF mDrawBounds = new RectF();
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
      PathEffect pathEffect,
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

    mPaint.setPathEffect(pathEffect);
    mPaint.setAntiAlias(pathEffect != null);
    mPaint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void draw(Canvas canvas) {
    final boolean equalBorderColors =
        mBorderLeftColor == mBorderTopColor
            && mBorderTopColor == mBorderRightColor
            && mBorderRightColor == mBorderBottomColor;
    final boolean equalBorderWidths =
        mBorderLeftWidth == mBorderTopWidth
            && mBorderTopWidth == mBorderRightWidth
            && mBorderRightWidth == mBorderBottomWidth;

    if (equalBorderWidths && mBorderLeftWidth == 0) {
      // No border widths, nothing to draw
      return;
    }

    if (equalBorderWidths && equalBorderColors) {
      drawAllBorders(canvas, mBorderLeftWidth, mBorderLeftColor);
    } else if (equalBorderWidths) {
      drawMultiColoredBorders(canvas);
    } else {
      drawIndividualBorders(canvas);
    }
  }

  /** Best case possible, all colors are the same and all widths are the same */
  private void drawAllBorders(Canvas canvas, float strokeWidth, @ColorInt int color) {
    float inset = strokeWidth / 2f;
    mDrawBounds.set(getBounds());
    mDrawBounds.inset(inset, inset);
    mPaint.setStrokeWidth(strokeWidth);
    mPaint.setColor(color);
    canvas.drawRect(mDrawBounds, mPaint);
  }

  /** Special case, support for multi color with same widths */
  private void drawMultiColoredBorders(Canvas canvas) {
    float inset = mBorderLeftWidth / 2f;
    mDrawBounds.set(getBounds());
    final int translateSaveCount = canvas.save();
    canvas.translate(mDrawBounds.left, mDrawBounds.top);
    mDrawBounds.offsetTo(0.0f, 0.0f);
    mPaint.setStrokeWidth(mBorderLeftWidth);

    int height = Math.round(mDrawBounds.height());
    int width = Math.round(mDrawBounds.width());
    int hypotenuse = (int) Math.round(Math.sqrt(2f * (height / 2f) * (height / 2f)));
    int saveCount;
    mDrawBounds.inset(inset, inset);

    // Left
    if (mBorderLeftColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(CLIP_ANGLE, 0f, 0f);
      canvas.clipRect(0f, 0f, hypotenuse, hypotenuse);
      canvas.rotate(-CLIP_ANGLE, 0f, 0f);

      mPaint.setColor(mBorderLeftColor);
      canvas.drawRect(mDrawBounds, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Right
    if (mBorderRightColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(-CLIP_ANGLE, width, 0f);
      canvas.clipRect(width - hypotenuse, 0f, width, hypotenuse);
      canvas.rotate(CLIP_ANGLE, width, 0f);

      mPaint.setColor(mBorderRightColor);
      canvas.drawRect(mDrawBounds, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Top
    if (mBorderTopColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(-CLIP_ANGLE, 0f, 0f);
      canvas.clipRect(0f, 0f, hypotenuse, hypotenuse);
      canvas.rotate(CLIP_ANGLE, 0f, 0f);

      canvas.rotate(CLIP_ANGLE, width, 0f);
      canvas.clipRect(width - hypotenuse, 0, width, hypotenuse, Region.Op.UNION);
      canvas.rotate(-CLIP_ANGLE, width, 0f);

      canvas.clipRect(hypotenuse, 0f, width - hypotenuse, hypotenuse, Region.Op.UNION);

      mPaint.setColor(mBorderTopColor);
      canvas.drawRect(mDrawBounds, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Bottom
    if (mBorderBottomColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(CLIP_ANGLE, 0f, height);
      canvas.clipRect(0f, height - hypotenuse, hypotenuse, height);
      canvas.rotate(-CLIP_ANGLE, 0f, height);

      canvas.rotate(-CLIP_ANGLE, width, height);
      canvas.clipRect(width - hypotenuse, height - hypotenuse, width, height, Region.Op.UNION);
      canvas.rotate(CLIP_ANGLE, width, height);

      canvas.clipRect(hypotenuse, height - hypotenuse, width - hypotenuse, height, Region.Op.UNION);

      mPaint.setColor(mBorderBottomColor);
      canvas.drawRect(mDrawBounds, mPaint);
      canvas.restoreToCount(saveCount);
    }

    canvas.restoreToCount(translateSaveCount);
  }

  /** Worst case, we have different widths _and_ colors specified */
  private void drawIndividualBorders(Canvas canvas) {
    Rect bounds = getBounds();
    // Draw left border.
    if (mBorderLeftWidth > 0 && mBorderLeftColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mBorderLeftColor,
          mBorderLeftWidth,
          bounds.left,
          bounds.top,
          Math.min(bounds.left + mBorderLeftWidth, bounds.right),
          bounds.bottom,
          true);
    }

    // Draw right border.
    if (mBorderRightWidth > 0 && mBorderRightColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mBorderRightColor,
          mBorderRightWidth,
          Math.max(bounds.right - mBorderRightWidth, bounds.left),
          bounds.top,
          bounds.right,
          bounds.bottom,
          true);
    }

    // Draw top border.
    if (mBorderTopWidth > 0 && mBorderTopColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mBorderTopColor,
          mBorderTopWidth,
          bounds.left,
          bounds.top,
          bounds.right,
          Math.min(bounds.top + mBorderTopWidth, bounds.bottom),
          false);
    }

    // Draw bottom border.
    if (mBorderBottomWidth > 0 && mBorderBottomColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mBorderBottomColor,
          mBorderBottomWidth,
          bounds.left,
          Math.max(bounds.bottom - mBorderBottomWidth, bounds.top),
          bounds.right,
          bounds.bottom,
          false);
    }
  }

  private void drawBorder(
      Canvas canvas,
      @ColorInt int color,
      float strokeWidth,
      float left,
      float top,
      float right,
      float bottom,
      boolean insetHorizontal) {
    mPaint.setStrokeWidth(strokeWidth);
    mPaint.setColor(color);
    mClipBounds.set(left, top, right, bottom);
    mDrawBounds.set(getBounds());
    if (insetHorizontal) {
      mDrawBounds.inset(mClipBounds.centerX() - mClipBounds.left, 0f);
    } else {
      mDrawBounds.inset(0f, mClipBounds.centerY() - mClipBounds.top);
    }

    int saveCount = canvas.save();
    canvas.clipRect(mClipBounds);
    canvas.drawRect(mDrawBounds, mPaint);
    canvas.restoreToCount(saveCount);
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
}
