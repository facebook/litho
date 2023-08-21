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

package com.facebook.rendercore.text;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.text.TextDirectionHeuristicCompat;
import androidx.core.text.TextDirectionHeuristicsCompat;

public class TextStyle implements Cloneable {
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
  boolean shouldAddSpacingExtraToFirstLine;
  float lineSpacingExtra = 0;
  float lineHeight = Float.MAX_VALUE;
  float lineHeightMultiplier = 1;
  float letterSpacing = 0;
  int textStyle = Typeface.DEFAULT.getStyle();
  @Nullable Typeface typeface;
  TextAlignment alignment = TextAlignment.TEXT_START;
  int breakStrategy = UNSET;
  int hyphenationFrequency = 0;
  int justificationMode = 0;
  TextDirectionHeuristicCompat textDirection;
  boolean clipToBounds = true;
  VerticalGravity verticalGravity = VerticalGravity.TOP;
  int highlightColor = Color.TRANSPARENT;
  int highlightStartOffset = UNSET;
  int highlightEndOffset = UNSET;
  int highlightCornerRadius = 0;
  boolean shouldLayoutEmptyText = false;

  boolean shouldTruncateTextUsingConstraints = false;
  int manualBaselineSpacing = Integer.MIN_VALUE;
  int manualCapSpacing = Integer.MIN_VALUE;
  float extraSpacingLeft = 0;
  float extraSpacingRight = 0;
  RoundedBackgroundProps roundedBackgroundProps = null;
  @Nullable String accessibilityLabel;

  public void setAccessibilityLabel(String accessibilityLabel) {
    this.accessibilityLabel = accessibilityLabel;
  }

  public void setShouldLayoutEmptyText(boolean shouldLayoutEmptyText) {
    this.shouldLayoutEmptyText = shouldLayoutEmptyText;
  }

  public void setShouldTruncateTextUsingConstraints(boolean shouldTruncateTextUsingConstraints) {
    this.shouldTruncateTextUsingConstraints = shouldTruncateTextUsingConstraints;
  }

  public void setManualBaselineCapSpacing(int manualBaselineSpacing, int manualCapSpacing) {
    this.manualBaselineSpacing = manualBaselineSpacing;
    this.manualCapSpacing = manualCapSpacing;
  }

  public TextStyle makeCopy() {
    try {
      return (TextStyle) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public static TextStyle createDefaultConfiguredTextStyle(final Context context) {
    final TextStyle textStyle = TextStylesAttributeHelper.createThemedTextStyle(context);
    textStyle.setShouldLayoutEmptyText(true);
    textStyle.setHighlightColor(Color.TRANSPARENT);
    return textStyle;
  }

  public static void maybeSetTextAlignment(final TextStyle textStyle, final Integer textAlign) {
    if (textAlign != null) {
      final TextAlignment textAlignment;
      switch (textAlign) {
        case Gravity.CENTER_HORIZONTAL:
          textAlignment = TextAlignment.CENTER;
          break;
        case Gravity.START:
          textAlignment = TextAlignment.TEXT_START;
          break;
        case Gravity.END:
          textAlignment = TextAlignment.TEXT_END;
          break;
        default:
          textAlignment = TextAlignment.TEXT_START;
      }
      textStyle.setAlignment(textAlignment);
    }
  }

  public static void maybeSetTextDirection(
      final TextStyle textStyle, final @Nullable String textDirectionStr, final boolean isRTL) {
    if (textDirectionStr != null) {
      final TextDirectionHeuristicCompat textDirection;

      switch (textDirectionStr) {
        case "device_locale":
          textDirection = TextDirectionHeuristicsCompat.LOCALE;
          break;
        case "text_first_strong":
          textDirection =
              isRTL
                  ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
                  : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR;
          break;
        default:
          textDirection = TextDirectionHeuristicsCompat.LOCALE;
      }
      textStyle.setTextDirection(textDirection);
    }
  }

  public static void maybeSetLineHeight(final TextStyle textStyle, final float lineHeight) {
    if (lineHeight >= 0) {
      textStyle.setLineHeight(lineHeight);
    }
  }

  public static void maybeSetLineHeightMultiplier(
      final TextStyle textStyle, final float lineHeightMultiplier) {
    maybeSetLineHeightMultiplier(textStyle, lineHeightMultiplier, true);
  }

  public static void maybeSetMaxNumberOfLines(
      final TextStyle textStyle, final int maxNumberOfLines) {
    if (maxNumberOfLines > -1) {
      textStyle.setMaxLines(maxNumberOfLines);
      textStyle.setEllipsize(TextUtils.TruncateAt.END);
    }
  }

  public static void maybeSetLineHeightMultiplier(
      final TextStyle textStyle, final float lineHeightMultiplier, boolean applyToFirstLine) {
    if (lineHeightMultiplier > 0) {
      // This is a special behavior to match Bloks classic and the iOS behavior.
      textStyle.setShouldAddSpacingExtraToFirstLine(applyToFirstLine);
      textStyle.setLineHeightMultiplier(lineHeightMultiplier);
    }
  }

  public void setTextSize(int pixels) {
    this.textSize = pixels;
  }

  public void setTypeface(Typeface typeface) {
    this.typeface = typeface;
  }

  public void setShouldAddSpacingExtraToFirstLine(boolean shouldAddSpacingExtraToFirstLine) {
    this.shouldAddSpacingExtraToFirstLine = shouldAddSpacingExtraToFirstLine;
  }

  public void setLineSpacingExtra(float lineSpacingExtra) {
    this.lineSpacingExtra = lineSpacingExtra;
  }

  public void setLineHeight(float lineHeight) {
    this.lineHeight = lineHeight;
  }

  public void setLineHeightMultiplier(Float lineHeightMultiplier) {
    this.lineHeightMultiplier = lineHeightMultiplier;
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

  public void setRoundedBackgroundColor(RoundedBackgroundProps roundedBackgroundProps) {
    this.roundedBackgroundProps = roundedBackgroundProps;
  }

  public static class RoundedBackgroundProps {
    final RectF padding;
    final float cornerRadius;
    final @ColorInt int backgroundColor;

    public RoundedBackgroundProps(RectF padding, float cornerRadius, int backgroundColor) {
      this.padding = padding;
      this.cornerRadius = cornerRadius;
      this.backgroundColor = backgroundColor;
    }
  }
}
