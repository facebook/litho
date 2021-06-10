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

package com.facebook.rendercore.text;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.text.TextDirectionHeuristicCompat;
import com.facebook.rendercore.Copyable;

public class TextStyle implements Copyable {
  static final int UNSET = -1;
  @Nullable TextUtils.TruncateAt ellipsize;
  @Nullable CharSequence customEllipsisText;
  boolean includeFontPadding = true;
  int minLines = Integer.MIN_VALUE;
  int maxLines = Integer.MAX_VALUE;
  int minEms = UNSET;
  int maxEms = UNSET;
  int minTextWidth = 0;
  int maxTextWidth = Integer.MAX_VALUE;
  float shadowRadius = 0;
  float shadowDx = 0;
  float shadowDy = 0;
  int shadowColor = Color.GRAY;
  boolean isSingleLine = false;
  @ColorInt int textColor = Color.BLACK;
  @Nullable ColorStateList textColorStateList;
  int linkColor = Color.BLUE;
  int textSize = UNSET;
  float extraSpacing = 0;
  float spacingMultiplier = 1;
  boolean shouldAddExtraSpacingToFistLine;
  float letterSpacing = 0;
  int textStyle = Typeface.DEFAULT.getStyle();
  @Nullable Typeface typeface;
  TextAlignment alignment = TextAlignment.TEXT_START;
  int breakStrategy = UNSET;
  int hyphenationFrequency = 0;
  int justificationMode = 0;
  TextDirectionHeuristicCompat textDirection;
  float lineHeight = Float.MAX_VALUE;
  boolean clipToBounds = true;
  VerticalGravity verticalGravity = VerticalGravity.TOP;
  int highlightColor = Color.TRANSPARENT;
  int highlightStartOffset = UNSET;
  int highlightEndOffset = UNSET;
  int highlightCornerRadius = 0;
  boolean shouldLayoutEmptyText = false;
  int manualBaselineSpacing = Integer.MIN_VALUE;
  int manualCapSpacing = Integer.MIN_VALUE;
  float extraSpacingLeft = 0;
  float extraSpacingRight = 0;

  public void setShouldLayoutEmptyText(boolean shouldLayoutEmptyText) {
    this.shouldLayoutEmptyText = shouldLayoutEmptyText;
  }

  public void setManualBaselineCapSpacing(int manualBaselineSpacing, int manualCapSpacing) {
    this.manualBaselineSpacing = manualBaselineSpacing;
    this.manualCapSpacing = manualCapSpacing;
  }

  @Override
  public TextStyle makeCopy() {
    try {
      return (TextStyle) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public void setTextSize(int pixels) {
    this.textSize = pixels;
  }

  public void setTypeface(Typeface typeface) {
    this.typeface = typeface;
  }

  public void setShouldAddExtraSpacingToFistLine(boolean shouldAddExtraSpacingToFistLine) {
    this.shouldAddExtraSpacingToFistLine = shouldAddExtraSpacingToFistLine;
  }

  public void setSpacingMultiplier(Float lineHeightMultiplier) {
    this.spacingMultiplier = lineHeightMultiplier;
  }

  public void setAlignment(TextAlignment textAlignment) {
    this.alignment = textAlignment;
  }

  public void setTextDirection(TextDirectionHeuristicCompat textDirection) {
    this.textDirection = textDirection;
  }

  public void setVerticalGravity(VerticalGravity verticalGravity) {
    this.verticalGravity = verticalGravity;
  }

  public void setMaxLines(int maxNumberOfLines) {
    this.maxLines = maxNumberOfLines;
  }

  public void setHighlightColor(int color) {
    this.highlightColor = color;
  }

  public void setHighlightCornerRadius(int radius) {
    this.highlightCornerRadius = radius;
  }

  public void setEllipsize(TextUtils.TruncateAt end) {
    this.ellipsize = end;
  }

  public void setTextStyle(int textStyle) {
    this.textStyle = textStyle;
  }

  public void setSingleLine(boolean singleLine) {
    this.isSingleLine = singleLine;
  }

  public void setTextColor(@ColorInt int textColor) {
    this.textColor = textColor;
    this.textColorStateList = null;
  }

  public void setTextColorStateList(ColorStateList textColorStateList) {
    this.textColorStateList = textColorStateList;
    this.textColor = 0;
  }

  public void setCustomEllipsisText(CharSequence customEllipsisText) {
    this.customEllipsisText = customEllipsisText;
  }

  public void setExtraSpacing(float extraSpacing) {
    this.extraSpacing = extraSpacing;
  }

  public void setIncludeFontPadding(boolean includeFontPadding) {
    this.includeFontPadding = includeFontPadding;
  }

  public void setExtraSpacingLeft(float extraSpacingLeft) {
    this.extraSpacingLeft = extraSpacingLeft;
  }

  public void setExtraSpacingRight(float extraSpacingRight) {
    this.extraSpacingRight = extraSpacingRight;
  }

  public void setShadowRadius(float shadowRadius) {
    this.shadowRadius = shadowRadius;
  }

  public void setShadowDx(float shadowDx) {
    this.shadowDx = shadowDx;
  }

  public void setShadowDy(float shadowDy) {
    this.shadowDy = shadowDy;
  }

  public void setShadowColor(int shadowColor) {
    this.shadowColor = shadowColor;
  }
}
