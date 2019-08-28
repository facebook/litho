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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.Output;
import com.facebook.litho.R;
import com.facebook.litho.config.ComponentsConfiguration;

import static com.facebook.litho.widget.VerticalGravity.BOTTOM;
import static com.facebook.litho.widget.VerticalGravity.CENTER;
import static com.facebook.litho.widget.VerticalGravity.TOP;

public final class TextStylesHelper {
  static {
    SynchronizedTypefaceHelper.setupSynchronizedTypeface();
  }

  private static final TruncateAt[] TRUNCATE_AT = TruncateAt.values();

  public static final int DEFAULT_EMS = -1;
  public static final int DEFAULT_MIN_WIDTH = 0;
  public static final int DEFAULT_MAX_WIDTH = Integer.MAX_VALUE;
  // BREAK_STRATEGY_SIMPLE (AOSP Default)
  public static final int DEFAULT_BREAK_STRATEGY = 0;
  // HYPHENATION_FREQUENCY_NONE (AOSP Default)
  public static final int DEFAULT_HYPHENATION_FREQUENCY = 0;
  // JUSTIFICATION_MODE_NONE (AOSP Default)
  public static final int DEFAULT_JUSTIFICATION_MODE = 0;

  public static final TextAlignment textAlignmentDefault = TextAlignment.TEXT_START;

  public static void onLoadStyle(
      ComponentContext c,
      Output<TruncateAt> ellipsize,
      Output<Float> extraSpacing,
      Output<Boolean> shouldIncludeFontPadding,
      Output<Float> spacingMultiplier,
      Output<Integer> minLines,
      Output<Integer> maxLines,
      Output<Integer> minEms,
      Output<Integer> maxEms,
      Output<Integer> minTextWidth,
      Output<Integer> maxTextWidth,
      Output<Boolean> isSingleLine,
      Output<CharSequence> text,
      Output<ColorStateList> textColorStateList,
      Output<Integer> linkColor,
      Output<Integer> highlightColor,
      Output<Integer> textSize,
      Output<TextAlignment> textAlignment,
      Output<Integer> breakStrategy,
      Output<Integer> hyphenationFrequency,
      Output<Integer> justificationMode,
      Output<Integer> textStyle,
      Output<Float> shadowRadius,
      Output<Float> shadowDx,
      Output<Float> shadowDy,
      Output<Integer> shadowColor,
      Output<VerticalGravity> verticalGravity,
      Output<Typeface> typeface) {

    final Resources.Theme theme = c.getAndroidContext().getTheme();

    // check first if provided attributes contain textAppearance. As an analogy to TextView
    // behavior,
    // we will parse textAppearance attributes first and then will override leftovers from main
    // style
    TypedArray a;
    if (ComponentsConfiguration.NEEDS_THEME_SYNCHRONIZATION) {
      synchronized (theme) {
        a = c.obtainStyledAttributes(R.styleable.Text_TextAppearanceAttr, 0);
      }
    } else {
      a = c.obtainStyledAttributes(R.styleable.Text_TextAppearanceAttr, 0);
    }

    int textAppearanceResId =
        a.getResourceId(R.styleable.Text_TextAppearanceAttr_android_textAppearance, -1);
    a.recycle();
    if (textAppearanceResId != -1) {
      if (ComponentsConfiguration.NEEDS_THEME_SYNCHRONIZATION) {
        synchronized (theme) {
          a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.Text);
        }
      } else {
        a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.Text);
      }
      resolveStyleAttrsForTypedArray(
          a,
          ellipsize,
          extraSpacing,
          shouldIncludeFontPadding,
          spacingMultiplier,
          minLines,
          maxLines,
          minEms,
          maxEms,
          minTextWidth,
          maxTextWidth,
          isSingleLine,
          text,
          textColorStateList,
          linkColor,
          highlightColor,
          textSize,
          textAlignment,
          breakStrategy,
          hyphenationFrequency,
          justificationMode,
          textStyle,
          shadowRadius,
          shadowDx,
          shadowDy,
          shadowColor,
          verticalGravity,
          typeface);
      a.recycle();
    }

    // now (after we parsed textAppearance) we can move on to main style attributes
    if (ComponentsConfiguration.NEEDS_THEME_SYNCHRONIZATION) {
      synchronized (theme) {
        a = c.obtainStyledAttributes(R.styleable.Text, 0);
      }
    } else {
      a = c.obtainStyledAttributes(R.styleable.Text, 0);
    }
    resolveStyleAttrsForTypedArray(
        a,
        ellipsize,
        extraSpacing,
        shouldIncludeFontPadding,
        spacingMultiplier,
        minLines,
        maxLines,
        minEms,
        maxEms,
        minTextWidth,
        maxTextWidth,
        isSingleLine,
        text,
        textColorStateList,
        linkColor,
        highlightColor,
        textSize,
        textAlignment,
        breakStrategy,
        hyphenationFrequency,
        justificationMode,
        textStyle,
        shadowRadius,
        shadowDx,
        shadowDy,
        shadowColor,
        verticalGravity,
        typeface);

    a.recycle();
  }

  private static void resolveStyleAttrsForTypedArray(
      TypedArray a,
      Output<TruncateAt> ellipsize,
      Output<Float> extraSpacing,
      Output<Boolean> shouldIncludeFontPadding,
      Output<Float> spacingMultiplier,
      Output<Integer> minLines,
      Output<Integer> maxLines,
      Output<Integer> minEms,
      Output<Integer> maxEms,
      Output<Integer> minTextWidth,
      Output<Integer> maxTextWidth,
      Output<Boolean> isSingleLine,
      Output<CharSequence> text,
      Output<ColorStateList> textColorStateList,
      Output<Integer> linkColor,
      Output<Integer> highlightColor,
      Output<Integer> textSize,
      Output<TextAlignment> textAlignment,
      Output<Integer> breakStrategy,
      Output<Integer> hyphenationFrequency,
      Output<Integer> justificationMode,
      Output<Integer> textStyle,
      Output<Float> shadowRadius,
      Output<Float> shadowDx,
      Output<Float> shadowDy,
      Output<Integer> shadowColor,
      Output<VerticalGravity> verticalGravity,
      Output<Typeface> typeface) {
    int viewTextAlignment = View.TEXT_ALIGNMENT_GRAVITY;
    int gravity = Gravity.NO_GRAVITY;
    String fontFamily = null;

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.Text_android_text) {
        text.set(a.getString(attr));
      } else if (attr == R.styleable.Text_android_textColor) {
        textColorStateList.set(a.getColorStateList(attr));
      } else if (attr == R.styleable.Text_android_textSize) {
        textSize.set(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.Text_android_ellipsize) {
        final int index = a.getInteger(attr, 0);
        if (index > 0) {
          ellipsize.set(TRUNCATE_AT[index - 1]);
        }
      } else if (attr == R.styleable.Text_android_textAlignment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          viewTextAlignment = a.getInt(attr, -1);
          textAlignment.set(getTextAlignment(viewTextAlignment, gravity));
        }
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity = a.getInt(attr, -1);
        textAlignment.set(getTextAlignment(viewTextAlignment, gravity));
        verticalGravity.set(getVerticalGravity(gravity));
      } else if (attr == R.styleable.Text_android_includeFontPadding) {
        shouldIncludeFontPadding.set(a.getBoolean(attr, false));
      } else if (attr == R.styleable.Text_android_minLines) {
        minLines.set(a.getInteger(attr, -1));
      } else if (attr == R.styleable.Text_android_maxLines) {
        maxLines.set(a.getInteger(attr, -1));
      } else if (attr == R.styleable.Text_android_singleLine) {
        isSingleLine.set(a.getBoolean(attr, false));
      } else if (attr == R.styleable.Text_android_textColorLink) {
        linkColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_textColorHighlight) {
        highlightColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_textStyle) {
        textStyle.set(a.getInteger(attr, 0));
      } else if (attr == R.styleable.Text_android_lineSpacingExtra) {
        extraSpacing.set(Float.valueOf(a.getDimensionPixelOffset(attr, 0)));
      } else if (attr == R.styleable.Text_android_lineSpacingMultiplier) {
        spacingMultiplier.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowDx) {
        shadowDx.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowDy) {
        shadowDy.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowRadius) {
        shadowRadius.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowColor) {
        shadowColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_minEms) {
        minEms.set(a.getInteger(attr, DEFAULT_EMS));
      } else if (attr == R.styleable.Text_android_maxEms) {
        maxEms.set(a.getInteger(attr, DEFAULT_EMS));
      } else if (attr == R.styleable.Text_android_minWidth) {
        minTextWidth.set(a.getDimensionPixelSize(attr, DEFAULT_MIN_WIDTH));
      } else if (attr == R.styleable.Text_android_maxWidth) {
        maxTextWidth.set(a.getDimensionPixelSize(attr, DEFAULT_MAX_WIDTH));
      } else if (attr == R.styleable.Text_android_fontFamily) {
        fontFamily = a.getString(attr);
      } else if (attr == R.styleable.Text_android_breakStrategy) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          breakStrategy.set(a.getInt(attr, DEFAULT_BREAK_STRATEGY));
        }
      } else if (attr == R.styleable.Text_android_hyphenationFrequency) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          hyphenationFrequency.set(a.getInt(attr, DEFAULT_HYPHENATION_FREQUENCY));
        }
      } else if (attr == R.styleable.Text_android_justificationMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          justificationMode.set(a.getInt(attr, DEFAULT_JUSTIFICATION_MODE));
        }
      }
    }

    if (fontFamily != null) {
      final Integer styleValue = textStyle.get();
      typeface.set(Typeface.create(fontFamily, styleValue == null ? -1 : styleValue));
    }
  }

  private static TextAlignment getTextAlignment(int viewTextAlignment, int gravity) {
    final TextAlignment alignment;
    switch (viewTextAlignment) {
      case View.TEXT_ALIGNMENT_TEXT_START:
        alignment = TextAlignment.TEXT_START;
        break;
      case View.TEXT_ALIGNMENT_TEXT_END:
        alignment = TextAlignment.TEXT_END;
        break;
      case View.TEXT_ALIGNMENT_CENTER:
        alignment = TextAlignment.CENTER;
        break;
      case View.TEXT_ALIGNMENT_VIEW_START:
        alignment = TextAlignment.LAYOUT_START;
        break;
      case View.TEXT_ALIGNMENT_VIEW_END:
        alignment = TextAlignment.LAYOUT_END;
        break;
      case View.TEXT_ALIGNMENT_INHERIT: // unsupported, default to gravity
      case View.TEXT_ALIGNMENT_GRAVITY:
        alignment = getTextAlignment(gravity);
        break;
      default:
        alignment = textAlignmentDefault;
        break;
    }
    return alignment;
  }

  private static TextAlignment getTextAlignment(int gravity) {
    final TextAlignment alignment;
    switch (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
      case Gravity.START:
        alignment = TextAlignment.LAYOUT_START;
        break;
      case Gravity.END:
        alignment = TextAlignment.LAYOUT_END;
        break;
      case Gravity.LEFT:
        alignment = TextAlignment.LEFT;
        break;
      case Gravity.RIGHT:
        alignment = TextAlignment.RIGHT;
        break;
      case Gravity.CENTER_HORIZONTAL:
        alignment = TextAlignment.CENTER;
        break;
      default:
        alignment = textAlignmentDefault;
        break;
    }
    return alignment;
  }

  private static VerticalGravity getVerticalGravity(int gravity) {
    final VerticalGravity verticalGravity;
    switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
      case Gravity.TOP:
        verticalGravity = TOP;
        break;
      case Gravity.CENTER_VERTICAL:
        verticalGravity = CENTER;
        break;
      case Gravity.BOTTOM:
        verticalGravity = BOTTOM;
        break;
      default:
        verticalGravity = TextSpec.verticalGravity;
        break;
    }
    return verticalGravity;
  }
}
