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

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.MetricAffectingSpan;
import android.util.SparseIntArray;
import android.view.View;
import androidx.annotation.GuardedBy;
import androidx.annotation.Size;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.TextDirectionHeuristicsCompat;
import com.facebook.fbui.textlayoutbuilder.TextLayoutBuilder;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;
import com.facebook.rendercore.MeasureResult;
import com.facebook.rendercore.RenderState;
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

  public static class TextLayoutContext {
    public Layout layout;
    CharSequence processedText;
    float textLayoutTranslationY;
    ClickableSpan[] clickableSpans;
    ImageSpan[] imageSpans;
    TextStyle textStyle;
  }

  public static MeasureResult layout(
      RenderState.LayoutContext context,
      int widthSpec,
      int heightSpec,
      CharSequence text,
      TextRenderUnit renderUnit,
      TextStyle textStyle) {
    final DebugMeasureListener debugListener = sDebugMeasureListener;
    final Context androidContext = context.getAndroidContext();
    if (debugListener != null) {
      debugListener.onTextMeasured(renderUnit, text, widthSpec, heightSpec);
    }
    final TextLayoutContext textLayoutContext = new TextLayoutContext();
    textLayoutContext.textStyle = textStyle;
    if (TextUtils.isEmpty(text) && !textStyle.shouldLayoutEmptyText) {
      textLayoutContext.processedText = text;
      return new MeasureResult(renderUnit, widthSpec, heightSpec, 0, 0, textLayoutContext);
    }

    Layout layout =
        TextMeasurementUtils.createTextLayout(
            androidContext, textStyle, widthSpec, heightSpec, text);

    final int layoutWidth = View.resolveSize(layout.getWidth(), widthSpec);

    // Adjust height according to the minimum number of lines.
    int preferredHeight = LayoutMeasureUtil.getHeight(layout);
    int extraSpacingHeight = 0;
    if (textStyle.spacingMultiplier > 1f && textStyle.shouldAddExtraSpacingToFistLine) {
      final TextPaint paint = layout.getPaint();

      final int layoutLineHeight = paint.getFontMetricsInt(null);
      extraSpacingHeight = (int) (layoutLineHeight * (textStyle.spacingMultiplier - 1.0f));
      preferredHeight += extraSpacingHeight;
    }

    final int lineCount = layout.getLineCount();
    if (lineCount < textStyle.minLines) {
      final TextPaint paint = layout.getPaint();

      final int layoutLineHeight =
          Math.round(
              paint.getFontMetricsInt(null) * textStyle.spacingMultiplier + textStyle.extraSpacing);
      preferredHeight += layoutLineHeight * (textStyle.minLines - lineCount);
    }

    final float textHeight = LayoutMeasureUtil.getHeight(layout);
    final int capHeightOffset;

    if (hasManualSpacing(textStyle)) {
      final int[] capHeights = getCapHeightBaselineSpacing(layout.getPaint(), text);

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

    // Handle custom text truncation:
    CharSequence processedText = text;
    if (textStyle.customEllipsisText != null && !textStyle.customEllipsisText.equals("")) {
      final int ellipsizedLineNumber = getEllipsizedLineNumber(layout);
      if (ellipsizedLineNumber != -1) {
        final CharSequence truncated =
            truncateText(
                text, textStyle.customEllipsisText, layout, ellipsizedLineNumber, layoutWidth);

        Layout newLayout =
            TextMeasurementUtils.createTextLayout(
                androidContext,
                textStyle,
                View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY),
                heightSpec,
                truncated);

        processedText = truncated;
        layout = newLayout;
      }
    }

    textLayoutContext.processedText = processedText;
    textLayoutContext.layout = layout;
    textLayoutContext.textLayoutTranslationY = textLayoutTranslationY;
    if (processedText instanceof Spanned) {
      Spanned spanned = (Spanned) processedText;
      textLayoutContext.clickableSpans =
          spanned.getSpans(0, processedText.length(), ClickableSpan.class);
      textLayoutContext.imageSpans = spanned.getSpans(0, processedText.length(), ImageSpan.class);
    }

    return new MeasureResult(
        renderUnit, widthSpec, heightSpec, layoutWidth, layoutHeight, textLayoutContext);
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
        .setTextSpacingExtra(textStyle.extraSpacing)
        .setTextSpacingMultiplier(textStyle.spacingMultiplier)
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

    final boolean isRTL = LayoutUtils.isLayoutDirectionRTL(context);

    if (textStyle.textDirection == null) {
      textStyle.textDirection =
          isRTL
              ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
              : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR;
    }
    layoutBuilder.setTextDirection(textStyle.textDirection);

    final Layout.Alignment textAlignment;
    final boolean textIsRTL;
    switch (textStyle.alignment) {
      default:
      case TEXT_START:
        textAlignment = Layout.Alignment.ALIGN_NORMAL;
        break;
      case TEXT_END:
        textAlignment = Layout.Alignment.ALIGN_OPPOSITE;
        break;
      case LAYOUT_START:
        textIsRTL = (textStyle.textDirection.isRtl(text, 0, text.length()));
        textAlignment =
            (isRTL == textIsRTL) ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE;
        break;
      case LAYOUT_END:
        textIsRTL = (textStyle.textDirection.isRtl(text, 0, text.length()));
        textAlignment =
            (isRTL == textIsRTL) ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
        break;
      case LEFT:
        textAlignment =
            textStyle.textDirection.isRtl(text, 0, text.length())
                ? Layout.Alignment.ALIGN_OPPOSITE
                : Layout.Alignment.ALIGN_NORMAL;
        break;
      case RIGHT:
        textAlignment =
            textStyle.textDirection.isRtl(text, 0, text.length())
                ? Layout.Alignment.ALIGN_NORMAL
                : Layout.Alignment.ALIGN_OPPOSITE;
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

  /**
   * Truncates text which is too long and appends the given custom ellipsis CharSequence to the end
   * of the visible text.
   *
   * @param text Text to truncate
   * @param customEllipsisText Text to append to the end to indicate truncation happened
   * @param newLayout A Layout object populated with measurement information for this text
   * @param ellipsizedLineNumber The line number within the text at which truncation occurs (i.e.
   *     the last visible line).
   * @return The provided text truncated in such a way that the 'customEllipsisText' can appear at
   *     the end.
   */
  private static CharSequence truncateText(
      CharSequence text,
      CharSequence customEllipsisText,
      Layout newLayout,
      int ellipsizedLineNumber,
      float layoutWidth) {
    // Identify the X position at which to truncate the final line:
    // Note: The left position of the line is needed for the case of RTL text.
    final float ellipsisTarget =
        layoutWidth
            - BoringLayout.getDesiredWidth(
                customEllipsisText, 0, customEllipsisText.length(), newLayout.getPaint())
            + newLayout.getLineLeft(ellipsizedLineNumber);
    // Get character offset number corresponding to that X position:
    int ellipsisOffset = newLayout.getOffsetForHorizontal(ellipsizedLineNumber, ellipsisTarget);
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
      return TextUtils.concat(text.subSequence(0, ellipsisOffset), customEllipsisText);
    } else {
      return text;
    }
  }

  /**
   * @param layout A prepared text layout object
   * @return The (zero-indexed) line number at which the text in this layout will be ellipsized, or
   *     -1 if no line will be ellipsized.
   */
  private static int getEllipsizedLineNumber(Layout layout) {
    for (int i = 0; i < layout.getLineCount(); ++i) {
      if (layout.getEllipsisCount(i) > 0) {
        return i;
      }
    }
    return -1;
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
