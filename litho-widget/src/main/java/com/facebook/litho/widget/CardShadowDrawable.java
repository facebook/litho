/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class CardShadowDrawable extends Drawable {

  private int mShadowStartColor;
  private int mShadowEndColor;

  private final Paint mEdgeShadowPaint;

  private final Path mCornerShadowTopLeftPath = new Path();
  private final Path mCornerShadowBottomLeftPath = new Path();
  private final Path mCornerShadowTopRightPath = new Path();
  private final Path mCornerShadowBottomRightPath = new Path();

  private final Paint mCornerShadowPaint;

  private float mCornerRadius;
  private float mShadowSize;
  private float mShadowDx;
  private float mShadowDy;

  private boolean mHideTopShadow;
  private boolean mHideBottomShadow;

  private boolean mDirty = true;

  CardShadowDrawable() {
    mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    mCornerShadowPaint.setStyle(Paint.Style.FILL);

    mEdgeShadowPaint = new Paint(mCornerShadowPaint);
    mEdgeShadowPaint.setAntiAlias(false);
  }

  @Override
  public void setAlpha(int alpha) {
    mCornerShadowPaint.setAlpha(alpha);
    mEdgeShadowPaint.setAlpha(alpha);
  }

  public static int getShadowLeft(float shadowSize, float shadowDx) {
    return (int) Math.ceil(toEven(shadowSize) - shadowDx);
  }

  public static int getShadowTop(float shadowSize, float shadowDy) {
    return (int) Math.ceil(toEven(shadowSize) - shadowDy);
  }

  public static int getShadowRight(float shadowSize, float shadowDx) {
    return (int) Math.ceil(toEven(shadowSize) + shadowDx);
  }

  public static int getShadowBottom(float shadowSize, float shadowDy) {
    return (int) Math.ceil(toEven(shadowSize) + shadowDy);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mCornerShadowPaint.setColorFilter(cf);
    mEdgeShadowPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDirty) {
      buildShadow();
      mDirty = false;
    }

    final Rect bounds = getBounds();

    drawShadowCorners(canvas, bounds);
    drawShadowEdges(canvas, bounds);
  }

  void setShadowStartColor(int shadowStartColor) {
    if (mShadowStartColor == shadowStartColor) {
      return;
    }

    mShadowStartColor = shadowStartColor;

    mDirty = true;
    invalidateSelf();
  }

  void setShadowEndColor(int shadowEndColor) {
    if (mShadowEndColor == shadowEndColor) {
      return;
    }

    mShadowEndColor = shadowEndColor;

    mDirty = true;
    invalidateSelf();
  }

  void setCornerRadius(float radius) {
    radius = (int) (radius + .5f);
    if (mCornerRadius == radius) {
      return;
    }

    mCornerRadius = radius;

    mDirty = true;
    invalidateSelf();
  }

  void setShadowSize(float shadowSize) {
    if (shadowSize < 0) {
      throw new IllegalArgumentException("invalid shadow size");
    }

    shadowSize = toEven(shadowSize);
    if (mShadowSize == shadowSize) {
      return;
    }

    mShadowSize = shadowSize;

    mDirty = true;
    invalidateSelf();
  }

  void setShadowDx(float shadowDx) {
    if (shadowDx == mShadowDx) {
      return;
    }

    mShadowDx = shadowDx;

    mDirty = true;
    invalidateSelf();
  }

  void setShadowDy(float shadowDy) {
    if (shadowDy == mShadowDy) {
      return;
    }

    mShadowDy = shadowDy;

    mDirty = true;
    invalidateSelf();
  }

  void setHideTopShadow(boolean hideTopShadow) {
    mHideTopShadow = hideTopShadow;
  }

  void setHideBottomShadow(boolean hideBottomShadow) {
    mHideBottomShadow = hideBottomShadow;
  }

  private static void setPath(Path path, int shadowX, int shadowY, float cornerRadius) {

    final RectF innerBounds =
        new RectF(shadowX, shadowY, shadowX + 2 * cornerRadius, shadowY + 2 * cornerRadius);

    final RectF outerBounds = new RectF(0, 0, 2 * cornerRadius, 2 * cornerRadius);

    path.reset();
    path.setFillType(Path.FillType.EVEN_ODD);
    path.moveTo(shadowX + cornerRadius, shadowY);
    path.arcTo(innerBounds, 270f, -90f, true);
    path.rLineTo(-shadowX, 0);
    path.lineTo(0, cornerRadius);
    path.arcTo(outerBounds, 180f, 90f, true);
    path.lineTo(shadowX + cornerRadius, 0);
    path.rLineTo(0, shadowY);
    path.close();
  }

  private void buildShadow() {

    final float shadowCornerRadius = mShadowSize + mCornerRadius;

    mCornerShadowPaint.setShader(
        new RadialGradient(
            shadowCornerRadius,
            shadowCornerRadius,
            shadowCornerRadius,
            new int[] {mShadowStartColor, mShadowStartColor, mShadowEndColor},
            new float[] {0f, .2f, 1f},
            Shader.TileMode.CLAMP));

    final int shadowLeft = getShadowLeft(mShadowSize, mShadowDx);
    final int shadowRight = getShadowRight(mShadowSize, mShadowDx);
    final int shadowTop = getShadowTop(mShadowSize, mShadowDy);
    final int shadowBottom = getShadowBottom(mShadowSize, mShadowDy);

    setPath(mCornerShadowTopLeftPath, shadowLeft, shadowTop, mCornerRadius);
    setPath(mCornerShadowTopRightPath, shadowRight, shadowTop, mCornerRadius);
    setPath(mCornerShadowBottomLeftPath, shadowLeft, shadowBottom, mCornerRadius);
    setPath(mCornerShadowBottomRightPath, shadowRight, shadowBottom, mCornerRadius);

    // We offset the content (shadowSize / 2) pixels up to make it more realistic.
    // This is why edge shadow shader has some extra space. When drawing bottom edge
    // shadow, we use that extra space.
    mEdgeShadowPaint.setShader(
        new LinearGradient(
            0,
            shadowCornerRadius,
            0,
            0,
            new int[] {mShadowStartColor, mShadowStartColor, mShadowEndColor},
            new float[] {0f, .2f, 1f},
            Shader.TileMode.CLAMP));

    mEdgeShadowPaint.setAntiAlias(false);
  }

  private void drawShadowCorners(Canvas canvas, Rect bounds) {
    int saved = canvas.save();
    if (!mHideTopShadow) {
      // left-top
      canvas.translate(bounds.left, bounds.top);
      canvas.drawPath(mCornerShadowTopLeftPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);

      // right-top
      saved = canvas.save();
      canvas.translate(bounds.right, bounds.top);
      canvas.scale(-1f, 1f);
      canvas.drawPath(mCornerShadowTopRightPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);
    }

    if (!mHideBottomShadow) {
      // right-bottom
      saved = canvas.save();
      canvas.translate(bounds.right, bounds.bottom);
      canvas.scale(-1f, -1f);
      canvas.drawPath(mCornerShadowBottomRightPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);

      // left-bottom
      saved = canvas.save();
      canvas.translate(bounds.left, bounds.bottom);
      canvas.scale(1f, -1f);
      canvas.drawPath(mCornerShadowBottomLeftPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);
    }
  }

  private void drawShadowEdges(Canvas canvas, Rect bounds) {
    final int paddingLeft = getShadowLeft(mShadowSize, mShadowDx);
    final int paddingTop = getShadowTop(mShadowSize, mShadowDy);
    final int paddingRight = getShadowRight(mShadowSize, mShadowDx);
    final int paddingBottom = getShadowBottom(mShadowSize, mShadowDy);

    int saved = canvas.save();

    if (!mHideTopShadow) {
      // top
      canvas.translate(bounds.left, bounds.top);
      canvas.drawRect(
          paddingLeft + mCornerRadius,
          0,
          bounds.width() - mCornerRadius - paddingRight,
          paddingTop,
          mEdgeShadowPaint);
      canvas.restoreToCount(saved);
    }

    if (!mHideBottomShadow) {
      // bottom
      saved = canvas.save();
      canvas.translate(bounds.right, bounds.bottom);
      canvas.rotate(180f);
      canvas.drawRect(
          paddingRight + mCornerRadius,
          0,
          bounds.width() - mCornerRadius - paddingLeft,
          paddingBottom,
          mEdgeShadowPaint);
      canvas.restoreToCount(saved);
    }

    // left
    saved = canvas.save();
    canvas.translate(bounds.left, bounds.bottom);
    canvas.rotate(270f);
    canvas.drawRect(
        mHideBottomShadow ? 0 : (paddingBottom + mCornerRadius),
        0,
        bounds.height() - (mHideTopShadow ? 0 : mCornerRadius + paddingTop),
        paddingLeft,
        mEdgeShadowPaint);
    canvas.restoreToCount(saved);

    // right
    saved = canvas.save();
    canvas.translate(bounds.right, bounds.top);
    canvas.rotate(90f);
    canvas.drawRect(
        mHideTopShadow ? 0 : (paddingTop + mCornerRadius),
        0,
        bounds.height() - (mHideBottomShadow ? 0 : mCornerRadius + paddingBottom),
        paddingRight,
        mEdgeShadowPaint);
    canvas.restoreToCount(saved);
  }

  private static int toEven(float value) {
    final int i = (int) (value + .5f);
    if (i % 2 == 1) {
      return i - 1;
    }

    return i;
  }
}
