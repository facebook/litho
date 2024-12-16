/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import java.util.Arrays;

public class TransparencyEnabledCardClipDrawable extends Drawable {

  private static final int CORNER_RADII_LIST_SIZE = 8;
  static final int NONE = 0;
  static final int TOP_LEFT = 1 << 0;
  static final int TOP_RIGHT = 1 << 1;
  static final int BOTTOM_LEFT = 1 << 2;
  static final int BOTTOM_RIGHT = 1 << 3;
  private final Paint mBackgroundPaint;
  private final Path mCornerPath = new Path();
  private Paint mCornerPaint;
  private boolean mDirty = true;
  private int mDisableClipCorners = NONE;
  private float mCornerRadius;
  @Nullable private Drawable mBackgroundDrawable;
  private final float[] drawableCornerRadii = new float[CORNER_RADII_LIST_SIZE];
  private final RectF rectF = new RectF();

  public TransparencyEnabledCardClipDrawable() {
    mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    resetCornerPaint();
  }

  @Override
  public void setAlpha(int alpha) {
    mBackgroundPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mBackgroundPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public void draw(Canvas canvas) {
    final Rect bounds = getBounds();
    rectF.set(bounds);
    if (areAllDisableClipFalse()) {
      canvas.drawRect(bounds, mCornerPaint);
      drawWithRoundRectImplementation(canvas, bounds);
    } else {
      drawWithClipPathImplementation(canvas, bounds);
    }
  }

  private void drawWithClipPathImplementation(Canvas canvas, Rect bounds) {
    if (mDirty) {
      buildClippingCorners();
      mDirty = false;
    }

    if (mBackgroundDrawable != null) {
      drawWithClipPathImplementationOnDrawable(canvas, bounds);
      return;
    }

    canvas.drawRect(bounds, mBackgroundPaint);
    if ((mDisableClipCorners & TOP_LEFT) == 0) {
      int saved = canvas.save();
      canvas.translate(bounds.left, bounds.top);
      canvas.drawPath(mCornerPath, mCornerPaint);
      canvas.restoreToCount(saved);
    }

    if ((mDisableClipCorners & BOTTOM_RIGHT) == 0) {
      int saved = canvas.save();
      canvas.translate(bounds.right, bounds.bottom);
      canvas.rotate(180f);
      canvas.drawPath(mCornerPath, mCornerPaint);
      canvas.restoreToCount(saved);
    }

    if ((mDisableClipCorners & BOTTOM_LEFT) == 0) {
      int saved = canvas.save();
      canvas.translate(bounds.left, bounds.bottom);
      canvas.rotate(270f);
      canvas.drawPath(mCornerPath, mCornerPaint);
      canvas.restoreToCount(saved);
    }

    if ((mDisableClipCorners & TOP_RIGHT) == 0) {
      int saved = canvas.save();
      canvas.translate(bounds.right, bounds.top);
      canvas.rotate(90f);
      canvas.drawPath(mCornerPath, mCornerPaint);
      canvas.restoreToCount(saved);
    }
  }

  private void drawWithClipPathImplementationOnDrawable(Canvas canvas, Rect bounds) {
    float topLeftRadius = (mDisableClipCorners & TOP_LEFT) == 0 ? mCornerRadius : 0;
    float topRightRadius = (mDisableClipCorners & TOP_RIGHT) == 0 ? mCornerRadius : 0;
    float bottomLeftRadius = (mDisableClipCorners & BOTTOM_LEFT) == 0 ? mCornerRadius : 0;
    float bottomRightRadius = (mDisableClipCorners & BOTTOM_RIGHT) == 0 ? mCornerRadius : 0;

    drawableCornerRadii[0] = topLeftRadius;
    drawableCornerRadii[1] = topLeftRadius;
    drawableCornerRadii[2] = topRightRadius;
    drawableCornerRadii[3] = topRightRadius;
    drawableCornerRadii[4] = bottomRightRadius;
    drawableCornerRadii[5] = bottomRightRadius;
    drawableCornerRadii[6] = bottomLeftRadius;
    drawableCornerRadii[7] = bottomLeftRadius;

    drawOnBackgroundDrawable(canvas, bounds);
  }

  private void drawOnBackgroundDrawable(Canvas canvas, Rect bounds) {
    if (mBackgroundDrawable != null) {
      int checkpoint = canvas.save();
      mCornerPath.reset();
      mCornerPath.addRoundRect(rectF, drawableCornerRadii, Path.Direction.CW);
      canvas.clipPath(mCornerPath);
      mBackgroundDrawable.setBounds(bounds);
      mBackgroundDrawable.draw(canvas);
      canvas.restoreToCount(checkpoint);

      // Reset the path and rectF
      mCornerPath.reset();
      rectF.setEmpty();
    }
  }

  private void drawWithRoundRectImplementation(Canvas canvas, Rect bounds) {
    if (mBackgroundDrawable != null) {
      drawOnBackgroundDrawable(canvas, bounds);
    } else {
      canvas.drawRoundRect(
          bounds.left,
          bounds.top,
          bounds.right,
          bounds.bottom,
          mCornerRadius,
          mCornerRadius,
          mBackgroundPaint);
    }
  }

  public void setBackgroundColor(int backgroundColor) {
    if (mBackgroundPaint.getColor() == backgroundColor) {
      return;
    }

    mBackgroundPaint.setColor(backgroundColor);
    invalidateSelf();
  }

  public void setClippingColor(int clippingColor) {
    if (mCornerPaint.getColor() == clippingColor) {
      return;
    }

    if (clippingColor == Color.TRANSPARENT && !areAllDisableClipFalse()) {
      setAlphaAndXferModeForTransparentClipping();
    } else {
      resetCornerPaint();
    }

    mCornerPaint.setColor(clippingColor);
    mDirty = true;
    invalidateSelf();
  }

  private void setAlphaAndXferModeForTransparentClipping() {
    mCornerPaint.setAlpha(0xff);
    mCornerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
  }

  public void setBackgroundDrawable(@Nullable Drawable backgroundDrawable) {
    mBackgroundDrawable = backgroundDrawable;
    invalidateSelf();
  }

  public void setCornerRadius(float radius) {
    radius = (int) (radius + .5f);
    if (mCornerRadius == radius) {
      return;
    }

    mDirty = true;
    mCornerRadius = radius;
    Arrays.fill(drawableCornerRadii, radius);
    invalidateSelf();
  }

  private void buildClippingCorners() {
    mCornerPath.reset();

    RectF oval = new RectF(0, 0, mCornerRadius * 2, mCornerRadius * 2);

    mCornerPath.setFillType(Path.FillType.EVEN_ODD);
    mCornerPath.moveTo(0, 0);
    mCornerPath.lineTo(0, mCornerRadius);
    mCornerPath.arcTo(oval, 180f, 90f, true);
    mCornerPath.lineTo(0, 0);

    mCornerPath.close();
  }

  public void setDisableClip(int edge) {
    if (mDisableClipCorners == edge) {
      return;
    }
    mDisableClipCorners = edge;

    // This condition ensures that the required alpha and xfermode is set in case setClippingColor
    // is called before setDisableClip
    if (mCornerPaint.getColor() == Color.TRANSPARENT && !areAllDisableClipFalse()) {
      setAlphaAndXferModeForTransparentClipping();
    }
    mDirty = true;
    invalidateSelf();
  }

  public void resetCornerPaint() {
    mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  }

  private boolean areAllDisableClipFalse() {
    return mDisableClipCorners == NONE;
  }
}
