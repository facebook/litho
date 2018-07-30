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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import java.util.Arrays;

/** Drawable that draws border lines with given color, widths and path effect. */
public class BorderColorDrawable extends Drawable {

  private static final int QUICK_REJECT_COLOR = Color.TRANSPARENT;
  private static final float CLIP_ANGLE = 45f;
  private static final RectF sClipBounds = new RectF();
  private static final RectF sDrawBounds = new RectF();
  private final Paint mPaint = new Paint();
  private final Path mPath = new Path();
  private float mBorderLeftWidth;
  private float mBorderTopWidth;
  private float mBorderRightWidth;
  private float mBorderBottomWidth;
  private float[] mBorderRadius;
  private boolean mDrawBorderWithPath;
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
      @ColorInt int bottomBorderColor,
      float[] borderRadius) {
    mBorderLeftWidth = leftBorderWidth;
    mBorderTopWidth = topBorderWidth;
    mBorderRightWidth = rightBorderWidth;
    mBorderBottomWidth = bottomBorderWidth;

    mBorderLeftColor = leftBorderColor;
    mBorderTopColor = topBorderColor;
    mBorderRightColor = rightBorderColor;
    mBorderBottomColor = bottomBorderColor;

    mBorderRadius = Arrays.copyOf(borderRadius, borderRadius.length);
    boolean hasRadius = false;
    float lastRadius = 0f;
    for (int i = 0; i < mBorderRadius.length; ++i) {
      final float radius = mBorderRadius[i];
      if (radius > 0f) {
        hasRadius = true;
      }
      if (i == 0) {
        lastRadius = radius;
      } else if (lastRadius != radius) {
        mDrawBorderWithPath = true;
        break;
      }
    }

    if (mDrawBorderWithPath) {
      // Need to duplicate values because Android expects X / Y radii specified separately
      float[] radii = new float[8];
      for (int i = 0; i < 4; ++i) {
        radii[i * 2] = mBorderRadius[i];
        radii[i * 2 + 1] = mBorderRadius[i];
      }
      mBorderRadius = radii;
    }

    mPaint.setPathEffect(pathEffect);
    mPaint.setAntiAlias(pathEffect != null || hasRadius);
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
    sDrawBounds.set(getBounds());
    sDrawBounds.inset(inset, inset);
    mPaint.setStrokeWidth(strokeWidth);
    mPaint.setColor(color);
    drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
  }

  /** Special case, support for multi color with same widths */
  private void drawMultiColoredBorders(Canvas canvas) {
    float inset = mBorderLeftWidth / 2f;
    sDrawBounds.set(getBounds());
    final int translateSaveCount = canvas.save();
    canvas.translate(sDrawBounds.left, sDrawBounds.top);
    sDrawBounds.offsetTo(0.0f, 0.0f);
    mPaint.setStrokeWidth(mBorderLeftWidth);

    int height = Math.round(sDrawBounds.height());
    int width = Math.round(sDrawBounds.width());
    int hypotenuse = (int) Math.round(Math.sqrt(2f * (height / 2f) * (height / 2f)));
    int saveCount;
    sDrawBounds.inset(inset, inset);

    // Left
    if (mBorderLeftColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(CLIP_ANGLE, 0f, 0f);
      canvas.clipRect(0f, 0f, hypotenuse, hypotenuse);
      canvas.rotate(-CLIP_ANGLE, 0f, 0f);

      mPaint.setColor(mBorderLeftColor);
      drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Right
    if (mBorderRightColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(-CLIP_ANGLE, width, 0f);
      canvas.clipRect(width - hypotenuse, 0f, width, hypotenuse);
      canvas.rotate(CLIP_ANGLE, width, 0f);

      mPaint.setColor(mBorderRightColor);
      drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
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
      drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
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
      drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
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
    sClipBounds.set(left, top, right, bottom);
    sDrawBounds.set(getBounds());
    if (insetHorizontal) {
      sDrawBounds.inset(sClipBounds.centerX() - sClipBounds.left, 0f);
    } else {
      sDrawBounds.inset(0f, sClipBounds.centerY() - sClipBounds.top);
    }

    int saveCount = canvas.save();
    canvas.clipRect(sClipBounds);
    drawBorder(canvas, sDrawBounds, path(), mBorderRadius, mPaint);
    canvas.restoreToCount(saveCount);
  }

  private Path path() {
    return mDrawBorderWithPath ? mPath : null;
  }

  private static void drawBorder(
      Canvas canvas, RectF bounds, Path path, float[] radii, Paint paint) {
    float maxRadii = Math.min(bounds.width(), bounds.height()) / 2f;
    if (path == null) {
      // All radii are the same
      float radius = Math.min(maxRadii, radii[0]);
      canvas.drawRoundRect(bounds, radius, radius, paint);
    } else {
      if (path.isEmpty()) {
        path.addRoundRect(bounds, radii, Path.Direction.CW);
      }
      canvas.drawPath(path, paint);
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

  public void reset() {
    mPaint.reset();
    mPath.reset();
    mBorderLeftWidth = 0;
    mBorderTopWidth = 0;
    mBorderRightWidth = 0;
    mBorderBottomWidth = 0;
    mBorderRadius = null;
    mDrawBorderWithPath = false;
    mBorderLeftColor = 0;
    mBorderTopColor = 0;
    mBorderRightColor = 0;
    mBorderBottomColor = 0;
  }
}
