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

  static final float SHADOW_MULTIPLIER = 1.5f;

  private int mShadowStartColor;
  private int mShadowEndColor;

  private final Paint mEdgeShadowPaint;

  private final Path mCornerShadowTopPath = new Path();
  private final Path mCornerShadowBottomPath = new Path();

  private final Paint mCornerShadowPaint;

  private float mCornerRadius;
  private float mShadowSize;
  private float mRawShadowSize;

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

  public static int getShadowHorizontal(float shadowSize) {
    return (int) Math.ceil(shadowSize);
  }

  public static int getShadowTop(float shadowSize) {
    return (int) Math.ceil(shadowSize / 2);
  }

  public static int getShadowRight(float shadowSize) {
    return (int) Math.ceil(shadowSize);
  }

  public static int getShadowBottom(float shadowSize) {
    return (int) Math.ceil(shadowSize * SHADOW_MULTIPLIER);
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
    if (mRawShadowSize == shadowSize) {
      return;
    }

    mRawShadowSize = shadowSize;
    mShadowSize = (int) (shadowSize * SHADOW_MULTIPLIER + .5f);

    mDirty = true;
    invalidateSelf();
  }

  void setHideTopShadow(boolean hideTopShadow) {
    mHideTopShadow = hideTopShadow;
  }

  void setHideBottomShadow(boolean hideBottomShadow) {
    mHideBottomShadow = hideBottomShadow;
  }

  private void buildShadow() {
    final int shadowHorizontal = getShadowHorizontal(mRawShadowSize);
    final int shadowTop = getShadowTop(mRawShadowSize);
    final int shadowBottom = getShadowBottom(mRawShadowSize);
    final float shadowCornerRadius = shadowHorizontal + mCornerRadius;

    mCornerShadowPaint.setShader(
        new RadialGradient(
            shadowCornerRadius,
            shadowCornerRadius,
            shadowCornerRadius,
            new int[] {mShadowStartColor, mShadowStartColor, mShadowEndColor},
            new float[] {0f, .2f, 1f},
            Shader.TileMode.CLAMP));

    final RectF topInnerBounds =
        new RectF(
            shadowHorizontal,
            shadowTop,
            shadowHorizontal + 2 * mCornerRadius,
            shadowTop + 2 * mCornerRadius);

    final RectF topOuterBounds = new RectF(0, 0, 2 * mCornerRadius, 2 * mCornerRadius);

    mCornerShadowTopPath.reset();
    mCornerShadowTopPath.setFillType(Path.FillType.EVEN_ODD);
    mCornerShadowTopPath.moveTo(shadowHorizontal + mCornerRadius, shadowTop);
    mCornerShadowTopPath.arcTo(topInnerBounds, 270f, -90f, true);
    mCornerShadowTopPath.rLineTo(-shadowHorizontal, 0);
    mCornerShadowTopPath.lineTo(0, mCornerRadius);
    mCornerShadowTopPath.arcTo(topOuterBounds, 180f, 90f, true);
    mCornerShadowTopPath.lineTo(shadowHorizontal + mCornerRadius, 0);
    mCornerShadowTopPath.rLineTo(0, shadowTop);
    mCornerShadowTopPath.close();

    final RectF bottomInnerBounds =
        new RectF(
            getShadowHorizontal(mRawShadowSize),
            getShadowBottom(mRawShadowSize),
            getShadowHorizontal(mRawShadowSize) + 2 * mCornerRadius,
            getShadowBottom(mRawShadowSize) + 2 * mCornerRadius);

    final RectF bottomOuterBounds = new RectF(0, 0, 2 * mCornerRadius, 2 * mCornerRadius);

    mCornerShadowBottomPath.reset();
    mCornerShadowBottomPath.setFillType(Path.FillType.EVEN_ODD);
    mCornerShadowBottomPath.moveTo(shadowHorizontal + mCornerRadius, shadowBottom);
    mCornerShadowBottomPath.arcTo(bottomInnerBounds, 270f, -90f, true);
    mCornerShadowBottomPath.rLineTo(-shadowHorizontal, 0);
    mCornerShadowBottomPath.lineTo(0, mCornerRadius);
    mCornerShadowBottomPath.arcTo(bottomOuterBounds, 180f, 90f, true);
    mCornerShadowBottomPath.lineTo(shadowHorizontal + mCornerRadius, 0);
    mCornerShadowBottomPath.rLineTo(0, shadowBottom);
    mCornerShadowBottomPath.close();

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
      canvas.drawPath(mCornerShadowTopPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);

      // right-top
      saved = canvas.save();
      canvas.translate(bounds.right, bounds.top);
      canvas.scale(-1f, 1f);
      canvas.drawPath(mCornerShadowTopPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);
    }

    if (!mHideBottomShadow) {
      // right-bottom
      saved = canvas.save();
      canvas.translate(bounds.right, bounds.bottom);
      canvas.scale(-1f, -1f);
      canvas.drawPath(mCornerShadowBottomPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);

      // left-bottom
      saved = canvas.save();
      canvas.translate(bounds.left, bounds.bottom);
      canvas.scale(1f, -1f);
      canvas.drawPath(mCornerShadowBottomPath, mCornerShadowPaint);
      canvas.restoreToCount(saved);
    }
  }

  private void drawShadowEdges(Canvas canvas, Rect bounds) {
    final int paddingLeft = getShadowHorizontal(mRawShadowSize);
    final int paddingTop = getShadowTop(mRawShadowSize);
    final int paddingRight = getShadowRight(mRawShadowSize);
    final int paddingBottom = getShadowBottom(mRawShadowSize);

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
