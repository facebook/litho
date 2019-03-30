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

package com.facebook.litho.drawable;

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
import androidx.annotation.ColorInt;
import androidx.annotation.Px;
import com.facebook.litho.CommonUtils;
import java.util.Arrays;
import javax.annotation.Nullable;

/** Drawable that draws border lines with given color, widths and path effect. */
public class BorderColorDrawable extends ComparableDrawable {

  private static final int QUICK_REJECT_COLOR = Color.TRANSPARENT;
  private static final float CLIP_ANGLE = 45f;
  private static final RectF sClipBounds = new RectF();
  private static final RectF sDrawBounds = new RectF();

  private final State mState;

  private Paint mPaint;
  private Path mPath;
  private boolean mDrawBorderWithPath;

  private BorderColorDrawable(State state) {
    mState = state;
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

  public void init() {
    mPaint = new Paint();
    mPath = new Path();
    boolean hasRadius = false;
    float lastRadius = 0f;
    for (int i = 0; i < mState.mBorderRadius.length; ++i) {
      final float radius = mState.mBorderRadius[i];
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

    if (mDrawBorderWithPath && mState.mBorderRadius.length != 8) {
      // Need to duplicate values because Android expects X / Y radii specified separately
      float[] radii = new float[8];
      for (int i = 0; i < 4; ++i) {
        radii[i * 2] = mState.mBorderRadius[i];
        radii[i * 2 + 1] = mState.mBorderRadius[i];
      }
      mState.mBorderRadius = radii;
    }

    mPaint.setPathEffect(mState.mPathEffect);
    mPaint.setAntiAlias(mState.mPathEffect != null || hasRadius);
    mPaint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mPaint == null || mPath == null) {
      init();
    }

    final boolean equalBorderColors =
        mState.mBorderLeftColor == mState.mBorderTopColor
            && mState.mBorderTopColor == mState.mBorderRightColor
            && mState.mBorderRightColor == mState.mBorderBottomColor;
    final boolean equalBorderWidths =
        mState.mBorderLeftWidth == mState.mBorderTopWidth
            && mState.mBorderTopWidth == mState.mBorderRightWidth
            && mState.mBorderRightWidth == mState.mBorderBottomWidth;

    if (equalBorderWidths && mState.mBorderLeftWidth == 0) {
      // No border widths, nothing to draw
      return;
    }

    if (equalBorderWidths && equalBorderColors) {
      drawAllBorders(canvas, mState.mBorderLeftWidth, mState.mBorderLeftColor);
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
    drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
  }

  /** Special case, support for multi color with same widths */
  private void drawMultiColoredBorders(Canvas canvas) {
    float inset = mState.mBorderLeftWidth / 2f;
    sDrawBounds.set(getBounds());
    final int translateSaveCount = canvas.save();
    canvas.translate(sDrawBounds.left, sDrawBounds.top);
    sDrawBounds.offsetTo(0.0f, 0.0f);
    mPaint.setStrokeWidth(mState.mBorderLeftWidth);

    int height = Math.round(sDrawBounds.height());
    int width = Math.round(sDrawBounds.width());
    int hypotenuse = (int) Math.round(Math.sqrt(2f * (height / 2f) * (height / 2f)));
    int saveCount;
    sDrawBounds.inset(inset, inset);

    // Left
    if (mState.mBorderLeftColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(CLIP_ANGLE, 0f, 0f);
      canvas.clipRect(0f, 0f, hypotenuse, hypotenuse);
      canvas.rotate(-CLIP_ANGLE, 0f, 0f);

      mPaint.setColor(mState.mBorderLeftColor);
      drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Right
    if (mState.mBorderRightColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(-CLIP_ANGLE, width, 0f);
      canvas.clipRect(width - hypotenuse, 0f, width, hypotenuse);
      canvas.rotate(CLIP_ANGLE, width, 0f);

      mPaint.setColor(mState.mBorderRightColor);
      drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Top
    if (mState.mBorderTopColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(-CLIP_ANGLE, 0f, 0f);
      canvas.clipRect(0f, 0f, hypotenuse, hypotenuse);
      canvas.rotate(CLIP_ANGLE, 0f, 0f);

      canvas.rotate(CLIP_ANGLE, width, 0f);
      canvas.clipRect(width - hypotenuse, 0, width, hypotenuse, Region.Op.UNION);
      canvas.rotate(-CLIP_ANGLE, width, 0f);

      canvas.clipRect(hypotenuse, 0f, width - hypotenuse, hypotenuse, Region.Op.UNION);

      mPaint.setColor(mState.mBorderTopColor);
      drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
      canvas.restoreToCount(saveCount);
    }

    // Bottom
    if (mState.mBorderBottomColor != QUICK_REJECT_COLOR) {
      saveCount = canvas.save();

      canvas.rotate(CLIP_ANGLE, 0f, height);
      canvas.clipRect(0f, height - hypotenuse, hypotenuse, height);
      canvas.rotate(-CLIP_ANGLE, 0f, height);

      canvas.rotate(-CLIP_ANGLE, width, height);
      canvas.clipRect(width - hypotenuse, height - hypotenuse, width, height, Region.Op.UNION);
      canvas.rotate(CLIP_ANGLE, width, height);

      canvas.clipRect(hypotenuse, height - hypotenuse, width - hypotenuse, height, Region.Op.UNION);

      mPaint.setColor(mState.mBorderBottomColor);
      drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
      canvas.restoreToCount(saveCount);
    }

    canvas.restoreToCount(translateSaveCount);
  }

  /** Worst case, we have different widths _and_ colors specified */
  private void drawIndividualBorders(Canvas canvas) {
    Rect bounds = getBounds();
    // Draw left border.
    if (mState.mBorderLeftWidth > 0 && mState.mBorderLeftColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mState.mBorderLeftColor,
          mState.mBorderLeftWidth,
          bounds.left,
          bounds.top,
          Math.min(bounds.left + mState.mBorderLeftWidth, bounds.right),
          bounds.bottom,
          true);
    }

    // Draw right border.
    if (mState.mBorderRightWidth > 0 && mState.mBorderRightColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mState.mBorderRightColor,
          mState.mBorderRightWidth,
          Math.max(bounds.right - mState.mBorderRightWidth, bounds.left),
          bounds.top,
          bounds.right,
          bounds.bottom,
          true);
    }

    // Draw top border.
    if (mState.mBorderTopWidth > 0 && mState.mBorderTopColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mState.mBorderTopColor,
          mState.mBorderTopWidth,
          bounds.left,
          bounds.top,
          bounds.right,
          Math.min(bounds.top + mState.mBorderTopWidth, bounds.bottom),
          false);
    }

    // Draw bottom border.
    if (mState.mBorderBottomWidth > 0 && mState.mBorderBottomColor != QUICK_REJECT_COLOR) {
      drawBorder(
          canvas,
          mState.mBorderBottomColor,
          mState.mBorderBottomWidth,
          bounds.left,
          Math.max(bounds.bottom - mState.mBorderBottomWidth, bounds.top),
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
    drawBorder(canvas, sDrawBounds, path(), mState.mBorderRadius, mPaint);
    canvas.restoreToCount(saveCount);
  }

  private @Nullable Path path() {
    return mDrawBorderWithPath ? mPath : null;
  }

  @Override
  public void setAlpha(int alpha) {
    if (mPaint != null) {
      mPaint.setAlpha(alpha);
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    if (mPaint != null) {
      mPaint.setColorFilter(colorFilter);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    return equals(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BorderColorDrawable)) {
      return false;
    }

    BorderColorDrawable that = (BorderColorDrawable) o;

    return CommonUtils.equals(mState, that.mState);
  }

  @Override
  public int hashCode() {
    return mState.hashCode();
  }

  static class State {
    float mBorderLeftWidth;
    float mBorderTopWidth;
    float mBorderRightWidth;
    float mBorderBottomWidth;

    @ColorInt int mBorderLeftColor;
    @ColorInt int mBorderTopColor;
    @ColorInt int mBorderRightColor;
    @ColorInt int mBorderBottomColor;

    @Nullable PathEffect mPathEffect;
    float[] mBorderRadius;

    @Override
    public int hashCode() {
      int result = 0;

      result = 31 * result + (int) mBorderLeftWidth;
      result = 31 * result + (int) mBorderTopWidth;
      result = 31 * result + (int) mBorderRightWidth;
      result = 31 * result + (int) mBorderBottomWidth;
      result = 31 * result + mBorderLeftColor;
      result = 31 * result + mBorderTopColor;
      result = 31 * result + mBorderRightColor;
      result = 31 * result + mBorderBottomColor;
      result = 31 * result + (mPathEffect != null ? mPathEffect.hashCode() : 0);
      result = 31 * result + Arrays.hashCode(mBorderRadius);

      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      State state = (State) o;
      return state.mBorderLeftWidth == mBorderLeftWidth
          && state.mBorderTopWidth == mBorderTopWidth
          && state.mBorderRightWidth == mBorderRightWidth
          && state.mBorderBottomWidth == mBorderBottomWidth
          && mBorderLeftColor == state.mBorderLeftColor
          && mBorderTopColor == state.mBorderTopColor
          && mBorderRightColor == state.mBorderRightColor
          && mBorderBottomColor == state.mBorderBottomColor
          && CommonUtils.equals(mPathEffect, state.mPathEffect)
          && Arrays.equals(mBorderRadius, state.mBorderRadius);
    }
  }

  public static class Builder {

    private State mState;

    public Builder() {
      mState = new State();
    }

    public Builder pathEffect(@Nullable PathEffect pathEffect) {
      mState.mPathEffect = pathEffect;
      return this;
    }

    public Builder borderColor(@ColorInt int color) {
      mState.mBorderLeftColor = color;
      mState.mBorderTopColor = color;
      mState.mBorderRightColor = color;
      mState.mBorderBottomColor = color;
      return this;
    }

    public Builder borderLeftColor(@ColorInt int color) {
      mState.mBorderLeftColor = color;
      return this;
    }

    public Builder borderTopColor(@ColorInt int color) {
      mState.mBorderTopColor = color;
      return this;
    }

    public Builder borderRightColor(@ColorInt int color) {
      mState.mBorderRightColor = color;
      return this;
    }

    public Builder borderBottomColor(@ColorInt int color) {
      mState.mBorderBottomColor = color;
      return this;
    }

    public Builder borderWidth(@Px int border) {
      mState.mBorderLeftWidth = border;
      mState.mBorderTopWidth = border;
      mState.mBorderRightWidth = border;
      mState.mBorderBottomWidth = border;
      return this;
    }

    public Builder borderLeftWidth(@Px int borderLeft) {
      mState.mBorderLeftWidth = borderLeft;
      return this;
    }

    public Builder borderTopWidth(@Px int borderTop) {
      mState.mBorderTopWidth = borderTop;
      return this;
    }

    public Builder borderRightWidth(@Px int borderRight) {
      mState.mBorderRightWidth = borderRight;
      return this;
    }

    public Builder borderBottomWidth(@Px int borderBottom) {
      mState.mBorderBottomWidth = borderBottom;
      return this;
    }

    public Builder borderRadius(float[] radius) {
      mState.mBorderRadius = Arrays.copyOf(radius, radius.length);
      return this;
    }

    public BorderColorDrawable build() {
      return new BorderColorDrawable(mState);
    }
  }
}
