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
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.CommonUtils;
import java.util.Arrays;

/** A comparable gradient drawable. */
@OkToExtend
public class ComparableGradientDrawable extends ComparableDrawableWrapper {

  protected GradientDrawable.Orientation orientation;
  protected int color;
  protected ColorStateList colorStateList;
  protected int[] colors;
  protected float cornerRadius;
  protected float[] cornerRadii;
  protected int gradientType = GradientDrawable.LINEAR_GRADIENT;
  protected int gradientRadius;
  protected int shape = GradientDrawable.RECTANGLE;
  protected int width = -1;
  protected int height = -1;
  protected int strokeWidth = -1;
  protected float strokeDashWidth = 0.0f;
  protected float strokeDashGap = 0.0f;
  protected int strokeColor;
  protected ColorStateList strokeColorStateList;

  public static ComparableGradientDrawable create() {
    return new ComparableGradientDrawable();
  }

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
        && CommonUtils.equals(colorStateList, that.colorStateList)
        && cornerRadius == that.cornerRadius
        && gradientType == that.gradientType
        && gradientRadius == that.gradientRadius
        && shape == that.shape
        && width == that.width
        && height == that.height
        && strokeWidth == that.strokeWidth
        && strokeDashWidth == that.strokeDashWidth
        && strokeDashGap == that.strokeDashGap
        && strokeColor == that.strokeColor
        && orientation == that.orientation
        && Arrays.equals(colors, that.colors)
        && Arrays.equals(cornerRadii, that.cornerRadii)
        && CommonUtils.equals(strokeColorStateList, that.strokeColorStateList);
  }

  @Override
  public int hashCode() {

    int result =
        Arrays.hashCode(
            new Object[] {
              orientation,
              color,
              colorStateList,
              cornerRadius,
              gradientType,
              gradientRadius,
              shape,
              width,
              height,
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
  public ComparableGradientDrawable setOrientation(GradientDrawable.Orientation orientation) {
    this.orientation = orientation;
    getGradientDrawable().setOrientation(orientation);
    return this;
  }

  public ComparableGradientDrawable setColor(int color) {
    this.color = color;
    getGradientDrawable().setColor(color);
    return this;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public ComparableGradientDrawable setColor(ColorStateList color) {
    this.colorStateList = color;
    getGradientDrawable().setColor(color);
    return this;
  }

  public ComparableGradientDrawable setColors(int[] colors) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      this.colors = colors;
      getGradientDrawable().setColors(colors);
    }
    return this;
  }

  public ComparableGradientDrawable setCornerRadius(float cornerRadius) {
    this.cornerRadius = cornerRadius;
    getGradientDrawable().setCornerRadius(cornerRadius);
    return this;
  }

  public ComparableGradientDrawable setCornerRadii(float[] cornerRadii) {
    this.cornerRadii = cornerRadii;
    getGradientDrawable().setCornerRadii(cornerRadii);
    return this;
  }

  public ComparableGradientDrawable setGradientType(int gradientType) {
    this.gradientType = gradientType;
    getGradientDrawable().setGradientType(gradientType);
    return this;
  }

  public ComparableGradientDrawable setGradientRadius(int gradientRadius) {
    this.gradientRadius = gradientRadius;
    getGradientDrawable().setGradientRadius(gradientRadius);
    return this;
  }

  public ComparableGradientDrawable setShape(int shape) {
    this.shape = shape;
    getGradientDrawable().setShape(shape);
    return this;
  }

  public ComparableGradientDrawable setSize(int width, int height) {
    this.width = width;
    this.height = height;
    getGradientDrawable().setSize(width, height);
    return this;
  }

  public ComparableGradientDrawable setStroke(int width, @ColorInt int color) {
    setStroke(width, color, 0, 0);
    return this;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public ComparableGradientDrawable setStroke(int width, ColorStateList colorStateList) {
    setStroke(width, colorStateList, 0, 0);
    return this;
  }

  public ComparableGradientDrawable setStroke(
      int width, @ColorInt int color, float dashWidth, float dashGap) {
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColor = color;
    getGradientDrawable().setStroke(width, color, dashWidth, dashGap);
    return this;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public ComparableGradientDrawable setStroke(
      int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColorStateList = colorStateList;
    getGradientDrawable().setStroke(width, colorStateList, dashWidth, dashGap);
    return this;
  }
}
