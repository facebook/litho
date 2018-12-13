/*
 * Copyright 2004-present Facebook, Inc.
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
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

class TransparencyEnabledCardClipDrawable extends Drawable {

  private final Paint mBackgroundPaint;

  private float mCornerRadius;

  TransparencyEnabledCardClipDrawable() {
    mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
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
    Rect bounds = getBounds();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      canvas.drawRoundRect(
          bounds.left,
          bounds.top,
          bounds.right,
          bounds.bottom,
          mCornerRadius,
          mCornerRadius,
          mBackgroundPaint);
    } else {
      canvas.drawRoundRect(
          new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
          mCornerRadius,
          mCornerRadius,
          mBackgroundPaint);
    }
  }

  void setBackgroundColor(int backgroundColor) {
    if (mBackgroundPaint.getColor() == backgroundColor) {
      return;
    }

    mBackgroundPaint.setColor(backgroundColor);
    invalidateSelf();
  }

  void setCornerRadius(float radius) {
    radius = (int) (radius + .5f);
    if (mCornerRadius == radius) {
      return;
    }

    mCornerRadius = radius;
    invalidateSelf();
  }
}
