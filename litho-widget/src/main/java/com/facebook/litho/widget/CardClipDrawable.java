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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

class CardClipDrawable extends Drawable {

  private final Paint mCornerPaint;
  private final Path mCornerPath = new Path();

  static final int NONE = 0;
  static final int TOP_LEFT = 1 << 0;
  static final int TOP_RIGHT = 1 << 1;
  static final int BOTTOM_LEFT = 1 << 2;
  static final int BOTTOM_RIGHT = 1 << 3;

  private int mDisableClipCorners = NONE;

  private float mCornerRadius;
  private boolean mDirty = true;

  CardClipDrawable() {
    mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
  }

  @Override
  public void setAlpha(int alpha) {
    mCornerPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mCornerPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  void setDisableClip(int edge) {
    if ((mDisableClipCorners & edge) != 0) {
      return;
    }
    mDisableClipCorners = edge;
    mDirty = true;
    invalidateSelf();
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDirty) {
      buildClippingCorners();
      mDirty = false;
    }

    final Rect bounds = getBounds();

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

  void setClippingColor(int clippingColor) {
    if (mCornerPaint.getColor() == clippingColor) {
      return;
    }

    mCornerPaint.setColor(clippingColor);
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
}
