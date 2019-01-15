/*
 * Copyright 2018-present Facebook, Inc.
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

import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.RequiresApi;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.CommonUtils;
import java.util.Arrays;

/** A comparable gradient drawable. */
@OkToExtend
public class ComparableGradientDrawable extends ComparableDrawableWrapper {

  protected GradientDrawable.Orientation orientation;
  protected int color;
  protected int[] colors;
  protected float cornerRadius;
  protected float[] cornerRadii;
  protected int gradientType = GradientDrawable.LINEAR_GRADIENT;
  protected int gradientRadius;
  protected Rect bounds;
  protected int shape = GradientDrawable.RECTANGLE;
  protected int width = -1;
  protected int height = -1;
  protected int alpha = 0xFF;
  protected int strokeWidth = -1;
  protected float strokeDashWidth = 0.0f;
  protected float strokeDashGap = 0.0f;
  protected int strokeColor;
  protected ColorStateList strokeColorStateList;

  public ComparableGradientDrawable() {
    super(new GradientDrawable());
  }

  public ComparableGradientDrawable(GradientDrawable.Orientation orientation, int[] colors) {
    super(new GradientDrawable(orientation, colors));
    this.orientation = orientation;
    this.colors = colors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ComparableGradientDrawable)) return false;
    ComparableGradientDrawable that = (ComparableGradientDrawable) o;

    return color == that.color
        && Float.compare(that.cornerRadius, cornerRadius) == 0
        && gradientType == that.gradientType
        && gradientRadius == that.gradientRadius
        && shape == that.shape
        && width == that.width
        && height == that.height
        && alpha == that.alpha
        && strokeWidth == that.strokeWidth
        && Float.compare(that.strokeDashWidth, strokeDashWidth) == 0
        && Float.compare(that.strokeDashGap, strokeDashGap) == 0
        && strokeColor == that.strokeColor
        && orientation == that.orientation
        && Arrays.equals(colors, that.colors)
        && Arrays.equals(cornerRadii, that.cornerRadii)
        && CommonUtils.equals(bounds, that.bounds)
        && CommonUtils.equals(strokeColorStateList, that.strokeColorStateList);
  }

  @Override
  public int hashCode() {

    int result =
        Arrays.hashCode(
            new Object[] {
              orientation,
              color,
              cornerRadius,
              gradientType,
              gradientRadius,
              bounds,
              shape,
              width,
              height,
              alpha,
              strokeWidth,
              strokeDashWidth,
              strokeDashGap,
              strokeColor,
              strokeColorStateList
            });
    result = 31 * result + Arrays.hashCode(colors);
    result = 31 * result + Arrays.hashCode(cornerRadii);
    return result;
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    return equals(other);
  }

  public GradientDrawable getGradientDrawable() {
    return (GradientDrawable) super.getWrappedDrawable();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  public void setOrientation(GradientDrawable.Orientation orientation) {
    this.orientation = orientation;
    getGradientDrawable().setOrientation(orientation);
  }

  public void setColor(int color) {
    this.color = color;
    getGradientDrawable().setColor(color);
  }

  public void setColors(int[] colors) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      this.colors = colors;
      getGradientDrawable().setColors(colors);
    }
  }

  public void setCornerRadius(float cornerRadius) {
    this.cornerRadius = cornerRadius;
    getGradientDrawable().setCornerRadius(cornerRadius);
  }

  public void setCornerRadii(float[] cornerRadii) {
    this.cornerRadii = cornerRadii;
    getGradientDrawable().setCornerRadii(cornerRadii);
  }

  public void setGradientType(int gradientType) {
    this.gradientType = gradientType;
    getGradientDrawable().setGradientType(gradientType);
  }

  public void setGradientRadius(int gradientRadius) {
    this.gradientRadius = gradientRadius;
    getGradientDrawable().setGradientRadius(gradientRadius);
  }

  @Override
  public void setBounds(Rect bounds) {
    this.bounds = bounds;
    getGradientDrawable().setBounds(bounds);
  }

  public void setShape(int shape) {
    this.shape = shape;
    getGradientDrawable().setShape(shape);
  }

  @Override
  public void setAlpha(int alpha) {
    this.alpha = alpha;
    getGradientDrawable().setAlpha(alpha);
  }

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    getGradientDrawable().setSize(width, height);
  }

  public void setStroke(int width, @ColorInt int color) {
    setStroke(width, color, 0, 0);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void setStroke(int width, ColorStateList colorStateList) {
    setStroke(width, colorStateList, 0, 0);
  }

  public void setStroke(int width, @ColorInt int color, float dashWidth, float dashGap) {
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColor = color;
    getGradientDrawable().setStroke(width, color, dashWidth, dashGap);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColorStateList = colorStateList;
    getGradientDrawable().setStroke(width, colorStateList, dashWidth, dashGap);
  }
}
