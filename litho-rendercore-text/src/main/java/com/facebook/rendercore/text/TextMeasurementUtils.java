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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.MetricAffectingSpan;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.View;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.TextDirectionHeuristicsCompat;
import com.facebook.fbui.textlayoutbuilder.TextLayoutBuilder;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.MountableLayoutResult;
import com.facebook.rendercore.RenderCoreConfig;
import com.facebook.rendercore.utils.LayoutUtils;

public class TextMeasurementUtils {

  private static final String CAP_MEASUREMENT_TEXT = "T";

  @GuardedBy("self")
  // Map from hash(textSize, typeface) -> cap heights array for text
  private static final SparseIntArray sCapHeightsCache = new SparseIntArray();

  private static final SparseIntArray sBaselineCache = new SparseIntArray();

  // The offsets of the returned values within the int array.
  private static final int CAP_HEIGHT_OFFSET_INDEX = 0;
  private static final int BASELINE_OFFSET_INDEX = 1;

  @VisibleForTesting
  public interface DebugMeasureListener {

    void onTextMeasured(
        TextRenderUnit renderUnit, CharSequence text, int widthSpec, int heightSpec);
  }

  @VisibleForTesting public static DebugMeasureListener sDebugMeasureListener;

  public static class TextLayout {

    public Layout layout;
    public CharSequence processedText;
    public float textLayoutTranslationX;
    public float textLayoutTranslationY;
    ClickableSpan[] clickableSpans;
    ImageSpan[] imageSpans;
    TextStyle textStyle;
    boolean isExplicitlyTruncated;
  }

  public static MountableLayoutResult layout(
      LayoutContext context,
      int widthSpec,
      int heightSpec,
      CharSequence text,
      TextRenderUnit renderUnit,
      TextStyle textStyle) {
    return layout(context, widthSpec, heightSpec, text, renderUnit, textStyle, false);
  }

  public static MountableLayoutResult layout(
      LayoutContext context,
      int widthSpec,
      int heightSpec,
      CharSequence text,
      TextRenderUnit renderUnit,
      TextStyle textStyle,
      boolean usePerformantTruncation) {
    final DebugMeasureListener debugListener = sDebugMeasureListener;
    final Context androidContext = context.getAndroidContext();
    if (debugListener != null) {
      debugListener.onTextMeasured(renderUnit, text, widthSpec, heightSpec);
    }

    Pair<Rect, TextLayout> result =
        layout(androidContext, widthSpec, heightSpec, text, textStyle, usePerformantTruncation);
    if (textStyle.roundedBackgroundProps != null && text instanceof Spannable) {
      result =
          calculateLayoutWithBackgroundSpan(
              androidContext, result, textStyle, (Spannable) text, widthSpec, heightSpec);
    }

    return new MountableLayoutResult(
        renderUnit, result.first.width(), result.first.height(), result.second);
  }

  public static Pair<Rect, TextLayout> calculateLayoutWithBackgroundSpan(
      final Context context,
      final Pair<Rect, TextLayout> result,
      final TextStyle textStyle,
      final Spannable text,
      int widthSpec,
      int heightSpec) {

    final TextStyle.RoundedBackgroundProps roundedBackgroundProps =
        textStyle.roundedBackgroundProps;

    TextMeasurementUtils.TextLayout textLayout = result.second;
    float topPadding = roundedBackgroundProps.padding.top;
    float bottomPadding = roundedBackgroundProps.padding.bottom;
    float startPadding = roundedBackgroundProps.padding.left;
    float endPadding = roundedBackgroundProps.padding.right;
    float cornerRadius = roundedBackgroundProps.cornerRadius;
    int backgroundColor = roundedBackgroundProps.backgroundColor;

    if (View.MeasureSpec.getMode(heightSpec) != View.MeasureSpec.UNSPECIFIED) {
      // check if the specified vertical paddings can be applied without the background being cut
      // off and adjust if needed
      final int availableHeight = View.MeasureSpec.getSize(heightSpec);
      final float measuredHeight =
          LayoutMeasureUtil.getHeight(textLayout.layout) + topPadding + bottomPadding;
      final float maximumYPadding = (availableHeight - measuredHeight) / 2;
      if (availableHeight < measuredHeight) {
        topPadding = Math.abs(Math.min(topPadding, Math.max(0, maximumYPadding)));
        bottomPadding = Math.abs(Math.min(bottomPadding, Math.max(0, maximumYPadding)));
      }
    }

    if (View.MeasureSpec.getMode(widthSpec) != View.MeasureSpec.UNSPECIFIED) {
      // check if the specified horizontal paddings can be applied without the background being cut
      // off and adjust if needed
      final int availableWidth = View.MeasureSpec.getSize(widthSpec);
      int maxLineWidth = 0;
      final Layout layout = textLayout.layout;
      for (int i = 0; i < layout.getLineCount(); i++) {
        maxLineWidth = (int) Math.max(maxLineWidth, layout.getLineWidth(i));
      }
      final float measuredWidth = maxLineWidth + startPadding + endPadding;
      final int maximumXPadding = (availableWidth - maxLineWidth) / 2;
      if (availableWidth < measuredWidth) {
        startPadding = Math.min(startPadding, Math.max(0, maximumXPadding));
        endPadding = Math.min(endPadding, Math.max(0, maximumXPadding));
      }
    }

    final RoundedBackgroundColorSpan span =
        new RoundedBackgroundColorSpan(
            textLayout.layout,
            backgroundColor,
            startPadding,
            endPadding,
            topPadding,
            bottomPadding,
            cornerRadius);
    text.setSpan(span, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    textStyle.setExtraSpacingLeft(startPadding);
    textStyle.setExtraSpacingRight(endPadding);

    // run layout to ensure that spacing and spans are applied to processed text
    final Pair<Rect, TextLayout> intermediateResult =
        TextMeasurementUtils.layout(context, widthSpec, heightSpec, text, textStyle);
    textLayout = (TextMeasurementUtils.TextLayout) intermediateResult.second;
    // we just ran layout, so textLayoutContext should not be null
    textLayout.textLayoutTranslationY = topPadding;

    switch (textStyle.alignment) {
      case CENTER:
        if (textLayout.layout.getLineCount() == 1) {
          // This is required for padding to work correctly with one line
          textLayout.textLayoutTranslationX = (startPadding + endPadding) / 2f;
        }
        break;
      case TEXT_START:
        textLayout.textLayoutTranslationX = startPadding;
        break;
      case TEXT_END:
        textLayout.textLayoutTranslationX =
            intermediateResult.first.width()
                - (LayoutMeasureUtil.getWidth(textLayout.layout) + endPadding);
        break;
    }

    return new Pair<>(
        new Rect(
            0,
            0,
            intermediateResult.first.width(),
            LayoutMeasureUtil.getHeight(textLayout.layout) + (int) (topPadding + bottomPadding)),
        textLayout);
  }

  public static Pair<Rect, TextLayout> layout(
      Context context, int widthSpec, int heightSpec, CharSequence text, TextStyle textStyle) {
    return layout(
        context, widthSpec, heightSpec, text, textStyle, RenderCoreConfig.usePerformantTruncation);
  }

  public static Pair<Rect, TextLayout> layout(
      Context context,
      int widthSpec,
      int heightSpec,
      CharSequence text,
      TextStyle textStyle,
      boolean usePerformantTruncation) {
    CharSequence processedText = text;
    final TextLayout textLayout = new TextLayout();
    textLayout.textStyle = textStyle;

    if (TextUtils.isEmpty(processedText) && !textStyle.shouldLayoutEmptyText) {
      textLayout.processedText = processedText;
      return new Pair<>(new Rect(0, 0, 0, 0), textLayout);
    }
    Layout layout =
        TextMeasurementUtils.createTextLayout(
            context, textStyle, widthSpec, heightSpec, processedText);

    // check if the layout should truncate based on the size constraints
    int linesWithinConstrainedBounds = -1;
    if (View.MeasureSpec.getMode(heightSpec) != View.MeasureSpec.UNSPECIFIED) {
      final int heightConstraint = View.MeasureSpec.getSize(heightSpec);
      final boolean fitTextToConstraints =
          textStyle.shouldTruncateTextUsingConstraints
              && textStyle.maxLines == Integer.MAX_VALUE
              && LayoutMeasureUtil.getHeight(layout) > heightConstraint;

      if (fitTextToConstraints) {
        linesWithinConstrainedBounds = 1;
        int lineIndex = layout.getLineCount() - 1;
        while (lineIndex >= 0) {
          if (layout.getLineBottom(lineIndex) <= heightConstraint) {
            linesWithinConstrainedBounds = lineIndex + 1;
            break;
          }

          lineIndex -= 1;
        }
      }
    }

    if (linesWithinConstrainedBounds != -1) {
      // we have constrained the number of lines that can fit, so truncate the layout
      textStyle.setMaxLines(linesWithinConstrainedBounds);
      layout =
          TextMeasurementUtils.createTextLayout(
              context, textStyle, widthSpec, heightSpec, processedText);
    }

    final int layoutWidth = resolveWidth(widthSpec, layout, textStyle);

    // Handle custom text truncation:
    if (textStyle.customEllipsisText != null && !textStyle.customEllipsisText.equals("")) {
      final int ellipsizedLineNumber = getEllipsizedLineNumber(layout);
      if (ellipsizedLineNumber != -1) {
        final CharSequence truncated =
            truncateText(
                processedText,
                textStyle.customEllipsisText,
                layout,
                ellipsizedLineNumber,
                layoutWidth,
                usePerformantTruncation,
                textStyle);

        Layout newLayout =
            TextMeasurementUtils.createTextLayout(
                context,
                textStyle,
                View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY),
                heightSpec,
                truncated);

        processedText = truncated;
        layout = newLayout;
        textLayout.isExplicitlyTruncated = true;
      }
    }

    // Adjust height according to the minimum number of lines.
    int preferredHeight = LayoutMeasureUtil.getHeight(layout);
    int extraSpacingHeight = 0;
    if (textStyle.lineHeightMultiplier > 1f && textStyle.shouldAddSpacingExtraToFirstLine) {
      final TextPaint paint = layout.getPaint();

      final int layoutLineHeight = paint.getFontMetricsInt(null);
      extraSpacingHeight = (int) (layoutLineHeight * (textStyle.lineHeightMultiplier - 1.0f));
      preferredHeight += extraSpacingHeight;
    }

    final int lineCount = layout.getLineCount();
    if (lineCount < textStyle.minLines) {
      final TextPaint paint = layout.getPaint();

      final int layoutLineHeight =
          Math.round(
              paint.getFontMetricsInt(null) * textStyle.lineHeightMultiplier
                  + textStyle.lineSpacingExtra);
      preferredHeight += layoutLineHeight * (textStyle.minLines - lineCount);
    }

    final float textHeight = LayoutMeasureUtil.getHeight(layout);
    final int capHeightOffset;

    if (hasManualSpacing(textStyle)) {
      final int[] capHeights = getCapHeightBaselineSpacing(layout.getPaint(), processedText);

      final int baselineOffset = capHeights[BASELINE_OFFSET_INDEX];
      capHeightOffset = capHeights[CAP_HEIGHT_OFFSET_INDEX] - textStyle.manualCapSpacing;
      preferredHeight -= (capHeightOffset + baselineOffset);
      preferredHeight += textStyle.manualBaselineSpacing;
    } else {
      capHeightOffset = 0;
    }

    final int layoutHeight = View.resolveSize(preferredHeight, heightSpec);

    float textLayoutTranslationY;
    switch (textStyle.verticalGravity) {
      case CENTER:
        textLayoutTranslationY =
            (layoutHeight - textHeight) / 2 + extraSpacingHeight - capHeightOffset;
        break;

      case BOTTOM:
        textLayoutTranslationY = layoutHeight - textHeight + extraSpacingHeight - capHeightOffset;
        break;

      default:
        textLayoutTranslationY = extraSpacingHeight - capHeightOffset;
        break;
    }

    textLayout.processedText = processedText;
    textLayout.layout = layout;
    if (textStyle.alignment == TextAlignment.TEXT_START) {
      textLayout.textLayoutTranslationX = textStyle.extraSpacingLeft;
    } else if (textStyle.alignment == TextAlignment.TEXT_END) {
      textLayout.textLayoutTranslationX = -textStyle.extraSpacingRight;
    }
    textLayout.textLayoutTranslationY = textLayoutTranslationY;
    if (processedText instanceof Spanned) {
      Spanned spanned = (Spanned) processedText;
      textLayout.clickableSpans = spanned.getSpans(0, processedText.length(), ClickableSpan.class);
      textLayout.imageSpans = spanned.getSpans(0, processedText.length(), ImageSpan.class);
    }

    return new Pair<>(new Rect(0, 0, layoutWidth, layoutHeight), textLayout);
  }

  private static int resolveWidth(int widthSpec, Layout layout, TextStyle textStyle) {
    int fullWidth =
        View.resolveSize(
            layout.getWidth()
                + Math.round(textStyle.extraSpacingLeft + textStyle.extraSpacingRight),
            widthSpec);
    if (textStyle.minimallyWide && layout.getLineCount() > 1) {
      float leftMost = fullWidth;
      float rightMost = 0;
      for (int i = 0, count = layout.getLineCount(); i < count; i++) {
        leftMost = Math.min(leftMost, layout.getLineLeft(i));
        rightMost = Math.max(rightMost, layout.getLineRight(i));
      }
      // To determine the width of the longest line, which is also the minimum width we desire,
      // without leading and trailing whitespaces.
      final int minimalWidth = View.resolveSize((int) (rightMost - leftMost), widthSpec);

      if (fullWidth - minimalWidth > textStyle.minimallyWideThreshold) {
        return minimalWidth;
      }
    }

    return fullWidth;
  }

  static Layout createTextLayout(
      Context context, TextStyle textStyle, int widthSpec, int heightSpec, CharSequence text) {
    TextLayoutBuilder layoutBuilder = new TextLayoutBuilder();
    layoutBuilder.setShouldCacheLayout(false);

    @TextLayoutBuilder.MeasureMode final int textMeasureMode;
    switch (View.MeasureSpec.getMode(widthSpec)) {
      case View.MeasureSpec.UNSPECIFIED:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_UNSPECIFIED;
        break;
      case View.MeasureSpec.EXACTLY:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_EXACTLY;
        break;
      case View.MeasureSpec.AT_MOST:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_AT_MOST;
        break;
      default:
        throw new IllegalStateException(
            "Unexpected size mode: " + View.MeasureSpec.getMode(widthSpec));
    }

    final TextUtils.TruncateAt actualEllipsize;
    if (textStyle.ellipsize == null && textStyle.maxLines != Integer.MAX_VALUE) {
      // On recent apis (> 24) max lines is no longer considered for calculating layout height if an
      // ellipsize method isn't specified. To keep consistent behavior across platforms we default
      // to end if you specify maxLines but not ellipsize.
      actualEllipsize = TextUtils.TruncateAt.END;
    } else {
      actualEllipsize = textStyle.ellipsize;
    }

    final boolean includeFontPadding = textStyle.includeFontPadding && !hasManualSpacing(textStyle);

    layoutBuilder
        .setDensity(context.getResources().getDisplayMetrics().density)
        .setEllipsize(actualEllipsize)
        .setMaxLines(textStyle.maxLines)
        .setShadowLayer(
            textStyle.shadowRadius, textStyle.shadowDx, textStyle.shadowDy, textStyle.shadowColor)
        .setSingleLine(textStyle.isSingleLine)
        .setText(text)
        .setTextSize(textStyle.textSize)
        .setWidth(View.MeasureSpec.getSize(widthSpec), textMeasureMode)
        .setIncludeFontPadding(includeFontPadding)
        .setTextSpacingExtra(textStyle.lineSpacingExtra)
        .setTextSpacingMultiplier(textStyle.lineHeightMultiplier)
        .setLinkColor(textStyle.linkColor)
        .setJustificationMode(textStyle.justificationMode)
        .setBreakStrategy(textStyle.breakStrategy)
        .setHyphenationFrequency(textStyle.hyphenationFrequency)
        .setShouldLayoutZeroLengthText(textStyle.shouldLayoutEmptyText);

    if (textStyle.lineHeight != Float.MAX_VALUE) {
      layoutBuilder.setLineHeight(textStyle.lineHeight);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      layoutBuilder.setLetterSpacing(textStyle.letterSpacing);
    }

    if (textStyle.minEms != TextStyle.UNSET) {
      layoutBuilder.setMinEms(textStyle.minEms);
    } else {
      layoutBuilder.setMinWidth(textStyle.minTextWidth);
    }

    if (textStyle.maxEms != TextStyle.UNSET) {
      layoutBuilder.setMaxEms(textStyle.maxEms);
    } else {
      layoutBuilder.setMaxWidth(textStyle.maxTextWidth);
    }

    if (textStyle.textColor != 0) {
      layoutBuilder.setTextColor(textStyle.textColor);
    } else {
      layoutBuilder.setTextColor(textStyle.textColorStateList);
    }

    if (textStyle.typeface != null) {
      layoutBuilder.setTypeface(textStyle.typeface);
    } else {
      layoutBuilder.setTextStyle(textStyle.textStyle);
    }

    final boolean isLocaleDirectionRTL = LayoutUtils.isLayoutDirectionRTL(context);
    // We ignore the locale direction if the text direction is manually set.
    final boolean ignoreLocaleDirection;
    if (textStyle.textDirection == null) {
      ignoreLocaleDirection = false;
      textStyle.textDirection =
          isLocaleDirectionRTL
              ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
              : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR;
    } else {
      ignoreLocaleDirection = true;
    }
    layoutBuilder.setTextDirection(textStyle.textDirection);

    final boolean isTextDirectionRTL = textStyle.textDirection.isRtl(text, 0, text.length());

    final Layout.Alignment textAlignment;
    switch (textStyle.alignment) {
      default:
      case TEXT_START:
        textAlignment =
            ignoreLocaleDirection || (isLocaleDirectionRTL == isTextDirectionRTL)
                ? Layout.Alignment.ALIGN_NORMAL
                : Layout.Alignment.ALIGN_OPPOSITE;
        break;
      case TEXT_END:
        textAlignment =
            ignoreLocaleDirection || (isLocaleDirectionRTL == isTextDirectionRTL)
                ? Layout.Alignment.ALIGN_OPPOSITE
                : Layout.Alignment.ALIGN_NORMAL;
        break;
      case LAYOUT_START:
        textAlignment =
            (isLocaleDirectionRTL == isTextDirectionRTL)
                ? Layout.Alignment.ALIGN_NORMAL
                : Layout.Alignment.ALIGN_OPPOSITE;
        break;
      case LAYOUT_END:
        textAlignment =
            (isLocaleDirectionRTL == isTextDirectionRTL)
                ? Layout.Alignment.ALIGN_OPPOSITE
                : Layout.Alignment.ALIGN_NORMAL;
        break;
      case LEFT:
        textAlignment =
            isTextDirectionRTL ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
        break;
      case RIGHT:
        textAlignment =
            isTextDirectionRTL ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
        break;
      case CENTER:
        textAlignment = Layout.Alignment.ALIGN_CENTER;
        break;
    }
    layoutBuilder.setAlignment(textAlignment);

    return layoutBuilder.build();
  }

  private static boolean hasManualSpacing(TextStyle textStyle) {
    return textStyle.manualCapSpacing != Integer.MIN_VALUE
        && textStyle.manualBaselineSpacing != Integer.MIN_VALUE;
  }

  private static boolean hasNoWordsOrNumbers(CharSequence line) {
    for (int i = 0; i < line.length(); ++i) {
      char character = line.charAt(i);
      if (Character.isAlphabetic(character) || Character.isDigit(character)) {
        return false;
      }
    }
    return true;
  }

  private static boolean shouldBeForcedInline(
      Layout newLayout, int ellipsizedLineNumber, int ellipsisOffset, CharSequence text) {
    int ellipsizedLineStart = newLayout.getLineStart(ellipsizedLineNumber);
    CharSequence trimmedLine =
        text.subSequence(ellipsizedLineStart, Math.max(ellipsisOffset, ellipsizedLineStart));
    return hasNoWordsOrNumbers(trimmedLine) && ellipsizedLineNumber - 1 >= 0;
  }

  /**
   * Truncates text which is too long and appends the given custom ellipsis CharSequence to the end
   * of the visible text.
   *
   * @param text Text to truncate
   * @param customEllipsisText Text to append to the end to indicate truncation happened
   * @param newLayout A Layout object populated with measurement information for this text
   * @param ellipsizedLineNumber The line number within the text at which truncation occurs (i.e.
   *     the last visible line).
   * @param fullLayoutWidth The width of the text from the WidthSpec before subtracting extra
   *     spacing
   * @param usePerformantTruncation Whether to use getOffsetForAdvance rather than
   *     getOffsetForHorizontal to prevent ANRs
   * @param textStyle A TextStyle object that is used to determine extra spacing and the truncation
   *     style
   * @return The provided text truncated in such a way that the 'customEllipsisText' can appear at
   *     the end.
   */
  private static CharSequence truncateText(
      CharSequence text,
      CharSequence customEllipsisText,
      Layout newLayout,
      int ellipsizedLineNumber,
      float fullLayoutWidth,
      boolean usePerformantTruncation,
      TextStyle textStyle) {
    // Identify the X position at which to truncate the final line:
    // Note: The left position of the line is needed for the case of RTL text.
    final float layoutWidth =
        fullLayoutWidth - textStyle.extraSpacingLeft - textStyle.extraSpacingRight;
    final float ellipsisTextWidth =
        BoringLayout.getDesiredWidth(
            customEllipsisText, 0, customEllipsisText.length(), newLayout.getPaint());
    final boolean isRTL =
        newLayout.getParagraphDirection(ellipsizedLineNumber) == Layout.DIR_RIGHT_TO_LEFT;

    // From local tests, correct truncation with getOffsetForAdvance doesn't depend on
    // newLayout.getLineLeft(ellipsizedLineNumber) and including it can cause incorrect rendering.
    // I suspect it was also incorrect for the previous getOffsetForAdvance. Regardless, we want to
    // test it so are putting it behind the same flag.
    final float ellipsisTarget;
    if (usePerformantTruncation) {
      ellipsisTarget =
          isRTL
              ? ellipsisTextWidth
              : layoutWidth
                  - ellipsisTextWidth
                  - newLayout.getLineLeft(ellipsizedLineNumber)
                  - newLayout.getParagraphLeft(ellipsizedLineNumber);
    } else {
      ellipsisTarget =
          isRTL
              ? ellipsisTextWidth
              : layoutWidth - ellipsisTextWidth + newLayout.getLineLeft(ellipsizedLineNumber);
    }

    // Get character offset number corresponding to that X position:
    int ellipsisOffset;
    if (usePerformantTruncation && Build.VERSION.SDK_INT >= 23) {
      int lineTextStart = newLayout.getLineStart(ellipsizedLineNumber);
      int lineTextEnd = newLayout.getLineEnd(ellipsizedLineNumber);
      ellipsisOffset =
          newLayout
              .getPaint()
              .getOffsetForAdvance(
                  text,
                  lineTextStart,
                  lineTextEnd,
                  lineTextStart,
                  lineTextEnd,
                  isRTL,
                  ellipsisTarget);
    } else {
      ellipsisOffset = newLayout.getOffsetForHorizontal(ellipsizedLineNumber, ellipsisTarget);
    }

    if (ellipsisOffset > 0) {
      // getOffsetForHorizontal returns the closest character, but we need to guarantee no
      // truncation, so subtract 1 from the result:
      ellipsisOffset -= 1;

      // Ensure that we haven't chosen an ellipsisOffset that's past the end of the ellipsis start.
      // This can occur in several cases, including when the width of the customEllipsisText is less
      // than the width of the default ellipsis character, and when in RTL mode and there is
      // whitespace to the left of the text. In these cases, getOffsetForHorizontal will return the
      // end of the string because our ellipsisTarget was in the middle of the ellipsis character.
      if (newLayout.getEllipsisCount(ellipsizedLineNumber) > 0) {
        final int ellipsisStart =
            newLayout.getLineStart(ellipsizedLineNumber)
                + newLayout.getEllipsisStart(ellipsizedLineNumber);
        if (ellipsisOffset > ellipsisStart) {
          ellipsisOffset = ellipsisStart;
        }
      }
      if (ellipsisOffset >= 0 && ellipsisOffset < text.length()) {
        if (textStyle.truncationStyle == TruncationStyle.FORCE_INLINE_TRUNCATION
            && shouldBeForcedInline(newLayout, ellipsizedLineNumber, ellipsisOffset, text)) {
          return truncateText(
              text,
              customEllipsisText,
              newLayout,
              ellipsizedLineNumber - 1,
              fullLayoutWidth,
              usePerformantTruncation,
              textStyle);
        } else {
          return TextUtils.concat(text.subSequence(0, ellipsisOffset), customEllipsisText);
        }
      } else {
        return text;
      }
    } else {
      return text;
    }
  }

  /**
   * @param layout A prepared text layout object
   * @return The (zero-indexed) line number at which the text in this layout will be ellipsized, or
   *     -1 if no line will be ellipsized.
   */
  public static int getEllipsizedLineNumber(Layout layout) {
    for (int i = 0; i < layout.getLineCount(); ++i) {
      if (layout.getEllipsisCount(i) > 0) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the width of the layout.
   *
   * @param layout The layout.
   * @return The width of the layout.
   */
  public static int getWidth(Layout layout) {
    if (layout == null) {
      return 0;
    }

    // Supplying VERY_WIDE will make layout.getWidth() return a very large value.
    int count = layout.getLineCount();
    int maxWidth = 0;

    for (int i = 0; i < count; i++) {
      maxWidth = Math.max(maxWidth, (int) layout.getLineRight(i));
    }

    return maxWidth;
  }

  /**
   * Prior to version 20, If the Layout specifies extra space between lines (either by spacingmult
   * or spacingadd) the StaticLayout would erroneously add this space after the last line as well.
   * This bug was fixed in version 20. This method calculates the extra space and reduces the height
   * by that amount.
   *
   * @param layout The layout.
   * @return The height of the layout.
   */
  public static int getHeight(@Nullable Layout layout) {
    if (layout == null) {
      return 0;
    }

    int extra = 0;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH
        && layout instanceof StaticLayout) {
      int line = Math.max(0, layout.getLineCount() - 1);
      int above = layout.getLineAscent(line);
      int below = layout.getLineDescent(line);
      float originalSize = (below - above - layout.getSpacingAdd()) / layout.getSpacingMultiplier();
      float ex = below - above - originalSize;
      if (ex >= 0) {
        extra = (int) (ex + 0.5);
      } else {
        extra = -(int) (-ex + 0.5);
      }
    }
    return layout.getHeight() - extra;
  }

  /**
   * Return the cap height values array {cap_height, cap_height_offset, baseline_offset} for the
   * given text size and typeface.
   */
  private static @Size(2) int[] getCapHeightBaselineSpacing(Paint paint, CharSequence text) {
    final TextPaint capTextPaint = new TextPaint(paint);
    Paint.FontMetricsInt fontMetricsInt = null;

    if (text instanceof Spanned && text.length() > 0) {
      MetricAffectingSpan[] spans = ((Spanned) text).getSpans(0, 0, MetricAffectingSpan.class);
      for (int i = 0; i < spans.length; i++) {
        spans[i].updateMeasureState(capTextPaint);
      }
    }

    final int hashCode = getKey(capTextPaint.getTextSize(), capTextPaint.getTypeface());

    int capHeight;
    synchronized (sCapHeightsCache) {
      capHeight = sCapHeightsCache.get(hashCode, Integer.MIN_VALUE);
    }

    if (capHeight == Integer.MIN_VALUE) {
      final Rect rect = new Rect();
      fontMetricsInt = new Paint.FontMetricsInt();
      capTextPaint.getFontMetricsInt(fontMetricsInt);
      capTextPaint.getTextBounds(CAP_MEASUREMENT_TEXT, 0, CAP_MEASUREMENT_TEXT.length(), rect);
      capHeight = (-1 * fontMetricsInt.ascent) - rect.height();
      synchronized (sCapHeightsCache) {
        sCapHeightsCache.put(hashCode, capHeight);
      }
    }
    final TextPaint baselineTextPaint = new TextPaint(paint);
    if (text instanceof Spanned && text.length() > 0) {
      MetricAffectingSpan[] spans =
          ((Spanned) text)
              .getSpans(text.length() - 1, text.length() - 1, MetricAffectingSpan.class);
      for (int i = 0; i < spans.length; i++) {
        spans[i].updateMeasureState(baselineTextPaint);
      }
    }
    final int baselineHashCode =
        getKey(baselineTextPaint.getTextSize(), baselineTextPaint.getTypeface());
    int baseline;
    synchronized (sBaselineCache) {
      baseline = sBaselineCache.get(baselineHashCode, Integer.MIN_VALUE);
    }
    if (baseline == Integer.MIN_VALUE) {
      if (fontMetricsInt == null
          || capTextPaint.getTextSize() != baselineTextPaint.getTextSize()
          || capTextPaint.getTypeface() != baselineTextPaint.getTypeface()) {
        fontMetricsInt = new Paint.FontMetricsInt();
        baselineTextPaint.getFontMetricsInt(fontMetricsInt);
      }

      baseline = fontMetricsInt.descent;
      synchronized (sBaselineCache) {
        sBaselineCache.put(baselineHashCode, baseline);
      }
    }

    return new int[] {capHeight, baseline};
  }

  /**
   * Get the key for a given size + typeface.
   *
   * <p>We use the size + typeface as a key without checking for collisions. In theory this could
   * collide, in which case we will return incorrect values.
   */
  private static int getKey(float textSizePx, Typeface typeface) {
    // Hash code implementation, see https://stackoverflow.com/questions/113511/
    return 31 * (int) textSizePx + typeface.hashCode();
  }
}
