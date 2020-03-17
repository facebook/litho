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

package com.facebook.litho.drawable;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.litho.CommonUtils;
import java.util.Arrays;

/** A comparable gradient drawable. */
@OkToExtend
public class ComparableGradientDrawable extends GradientDrawable implements ComparableDrawable {

  protected int color;
  protected ColorStateList colorStateList;
  protected int[] colors;
  protected float cornerRadius;
  protected float[] cornerRadii;
  protected int gradientType = GradientDrawable.LINEAR_GRADIENT;
  protected float gradientRadius;
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

  public ComparableGradientDrawable() {}

  public ComparableGradientDrawable(GradientDrawable.Orientation orientation, int[] colors) {
    super(orientation, colors);
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
        && getOrientationOrNullOnAPI15() == that.getOrientationOrNullOnAPI15()
        && Arrays.equals(colors, that.colors)
        && Arrays.equals(cornerRadii, that.cornerRadii)
        && CommonUtils.equals(strokeColorStateList, that.strokeColorStateList);
  }

  @Override
  public int hashCode() {
    int result =
        Arrays.hashCode(
            new Object[] {
              getOrientationOrNullOnAPI15(),
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

  /**
   * On API 15, get/setOrientation didn't exist so we need to not call it. It also wasn't possible
   * to change the orientation so we don't need to compare it anyway.
   */
  private @Nullable Orientation getOrientationOrNullOnAPI15() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      return null;
    }
    return getOrientation();
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    return equals(other);
  }

  @Override
  public void setColor(int color) {
    super.setColor(color);
    this.color = color;
  }

  @Override
  public void setColor(ColorStateList color) {
    super.setColor(color);
    this.colorStateList = color;
  }

  @Override
  public void setColors(int[] colors) {
    super.setColors(colors);
    this.colors = colors;
  }

  @Override
  public void setCornerRadius(float cornerRadius) {
    super.setCornerRadius(cornerRadius);
    this.cornerRadius = cornerRadius;
  }

  @Override
  public void setCornerRadii(float[] cornerRadii) {
    super.setCornerRadii(cornerRadii);
    this.cornerRadii = cornerRadii;
  }

  @Override
  public void setGradientType(int gradientType) {
    super.setGradientType(gradientType);
    this.gradientType = gradientType;
  }

  @Override
  public void setGradientRadius(float gradientRadius) {
    super.setGradientRadius(gradientRadius);
    this.gradientRadius = gradientRadius;
  }

  @Override
  public void setShape(int shape) {
    super.setShape(shape);
    this.shape = shape;
  }

  @Override
  public void setSize(int width, int height) {
    super.setSize(width, height);
    this.width = width;
    this.height = height;
  }

  @Override
  public void setStroke(int width, @ColorInt int color, float dashWidth, float dashGap) {
    super.setStroke(width, color, dashWidth, dashGap);
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColor = color;
  }

  @Override
  public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
    super.setStroke(width, colorStateList, dashWidth, dashGap);
    this.strokeWidth = width;
    this.strokeDashWidth = dashWidth;
    this.strokeDashGap = dashGap;
    this.strokeColorStateList = colorStateList;
  }
}
