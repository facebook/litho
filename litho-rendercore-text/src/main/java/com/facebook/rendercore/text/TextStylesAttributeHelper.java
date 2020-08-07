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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static com.facebook.rendercore.text.VerticalGravity.BOTTOM;
import static com.facebook.rendercore.text.VerticalGravity.CENTER;
import static com.facebook.rendercore.text.VerticalGravity.TOP;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import com.facebook.rendercore.RenderCoreSystrace;
import java.util.WeakHashMap;

public class TextStylesAttributeHelper {

  private static final TextUtils.TruncateAt[] TRUNCATE_AT = TextUtils.TruncateAt.values();

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

  private static final WeakHashMap<Resources.Theme, TextStyle> sThemedRenderUnitCache =
      new WeakHashMap<>();

  public static TextStyle createThemedTextStyle(Context c) {
    final Resources.Theme theme = c.getTheme();
    TextStyle textStyle;

    synchronized (sThemedRenderUnitCache) {
      textStyle = sThemedRenderUnitCache.get(theme);
    }

    if (textStyle == null) {
      textStyle = new TextStyle();
      RenderCoreSystrace.beginSection("LoadTextStyle");
      onLoadStyle(c, textStyle);
      RenderCoreSystrace.endSection();
      synchronized (sThemedRenderUnitCache) {
        // It's fine if we end up overriding this.
        sThemedRenderUnitCache.put(theme, textStyle);
      }
    }

    return textStyle.makeCopy();
  }

  public static void warmTextForTheme(Context c) {
    createThemedTextStyle(c);
  }

  public static void onLoadStyle(Context c, TextStyle textStyle) {

    final Resources.Theme theme = c.getTheme();

    // check first if provided attributes contain textAppearance. As an analogy to TextView
    // behavior,
    // we will parse textAppearance attributes first and then will override leftovers from main
    // style
    TypedArray a;
    if (SDK_INT <= LOLLIPOP_MR1) {
      synchronized (theme) {
        a = c.obtainStyledAttributes(null, R.styleable.Text_RenderCoreTextAppearanceAttr, 0, 0);
      }
    } else {
      a = c.obtainStyledAttributes(null, R.styleable.Text_RenderCoreTextAppearanceAttr, 0, 0);
    }

    int textAppearanceResId =
        a.getResourceId(R.styleable.Text_RenderCoreTextAppearanceAttr_android_textAppearance, -1);
    a.recycle();
    if (textAppearanceResId != -1) {
      if (SDK_INT <= LOLLIPOP_MR1) {
        synchronized (theme) {
          a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.RenderCoreText);
        }
      } else {
        a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.RenderCoreText);
      }
      resolveStyleAttrsForTypedArray(a, textStyle);
      a.recycle();
    }

    // Now read the textViewStyle set on this theme. We do this to be able to just read any
    // attribute set for a TextView
    if (SDK_INT <= LOLLIPOP_MR1) {
      synchronized (theme) {
        a = c.obtainStyledAttributes(null, R.styleable.Text_RenderCoreTextStyleAttr, 0, 0);
      }
    } else {
      a = c.obtainStyledAttributes(null, R.styleable.Text_RenderCoreTextStyleAttr, 0, 0);
    }
    int textStyleResId =
        a.getResourceId(R.styleable.Text_RenderCoreTextStyleAttr_android_textViewStyle, -1);
    a.recycle();

    if (textStyleResId != -1) {
      if (SDK_INT <= LOLLIPOP_MR1) {
        synchronized (theme) {
          a = theme.obtainStyledAttributes(textStyleResId, R.styleable.RenderCoreText);
        }
      } else {
        a = theme.obtainStyledAttributes(textStyleResId, R.styleable.RenderCoreText);
      }
      resolveStyleAttrsForTypedArray(a, textStyle);
      a.recycle();
    }
  }

  private static void resolveStyleAttrsForTypedArray(TypedArray a, TextStyle textStyle) {
    int viewTextAlignment = View.TEXT_ALIGNMENT_GRAVITY;
    int gravity = Gravity.NO_GRAVITY;
    String fontFamily = null;

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);
      int textStyleInt = 0;

      if (attr == R.styleable.RenderCoreText_android_textColor) {
        textStyle.setTextColorStateList(a.getColorStateList(attr));
      } else if (attr == R.styleable.RenderCoreText_android_textSize) {
        textStyle.textSize = a.getDimensionPixelSize(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_ellipsize) {
        final int index = a.getInteger(attr, 0);
        if (index > 0) {
          textStyle.ellipsize = TRUNCATE_AT[index - 1];
        }
      } else if (attr == R.styleable.RenderCoreText_android_textAlignment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          viewTextAlignment = a.getInt(attr, -1);
          textStyle.alignment = getTextAlignment(viewTextAlignment, gravity);
        }
      } else if (attr == R.styleable.RenderCoreText_android_gravity) {
        gravity = a.getInt(attr, -1);
        textStyle.alignment = getTextAlignment(viewTextAlignment, gravity);
        textStyle.verticalGravity = getVerticalGravity(gravity);
      } else if (attr == R.styleable.RenderCoreText_android_includeFontPadding) {
        textStyle.includeFontPadding = a.getBoolean(attr, false);
      } else if (attr == R.styleable.RenderCoreText_android_minLines) {
        textStyle.minLines = a.getInteger(attr, -1);
      } else if (attr == R.styleable.RenderCoreText_android_maxLines) {
        textStyle.maxLines = a.getInteger(attr, -1);
      } else if (attr == R.styleable.RenderCoreText_android_singleLine) {
        textStyle.isSingleLine = a.getBoolean(attr, false);
      } else if (attr == R.styleable.RenderCoreText_android_textColorLink) {
        textStyle.linkColor = a.getColor(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_textColorHighlight) {
        textStyle.highlightColor = a.getColor(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_textStyle) {
        textStyleInt = a.getInteger(attr, 0);
        textStyle.textStyle = textStyleInt;
      } else if (attr == R.styleable.RenderCoreText_android_lineSpacingExtra) {
        textStyle.extraSpacing = a.getDimensionPixelOffset(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_lineSpacingMultiplier) {
        textStyle.spacingMultiplier = a.getFloat(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_shadowDx) {
        textStyle.shadowDx = a.getFloat(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_shadowDy) {
        textStyle.shadowDy = a.getFloat(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_shadowRadius) {
        textStyle.shadowRadius = a.getFloat(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_shadowColor) {
        textStyle.shadowColor = a.getColor(attr, 0);
      } else if (attr == R.styleable.RenderCoreText_android_minEms) {
        textStyle.minEms = a.getInteger(attr, DEFAULT_EMS);
      } else if (attr == R.styleable.RenderCoreText_android_maxEms) {
        textStyle.maxEms = a.getInteger(attr, DEFAULT_EMS);
      } else if (attr == R.styleable.RenderCoreText_android_minWidth) {
        textStyle.minTextWidth = a.getDimensionPixelSize(attr, DEFAULT_MIN_WIDTH);
      } else if (attr == R.styleable.RenderCoreText_android_maxWidth) {
        textStyle.maxTextWidth = a.getDimensionPixelSize(attr, DEFAULT_MAX_WIDTH);
      } else if (attr == R.styleable.RenderCoreText_android_fontFamily) {
        fontFamily = a.getString(attr);
        if (fontFamily != null) {
          textStyle.typeface = Typeface.create(fontFamily, textStyleInt);
        }
      } else if (attr == R.styleable.RenderCoreText_android_breakStrategy) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          textStyle.breakStrategy = a.getInt(attr, DEFAULT_BREAK_STRATEGY);
        }
      } else if (attr == R.styleable.RenderCoreText_android_hyphenationFrequency) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          textStyle.hyphenationFrequency = a.getInt(attr, DEFAULT_HYPHENATION_FREQUENCY);
        }
      } else if (attr == R.styleable.RenderCoreText_android_justificationMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          textStyle.justificationMode = a.getInt(attr, DEFAULT_JUSTIFICATION_MODE);
        }
      }
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
        verticalGravity = TOP;
        break;
    }
    return verticalGravity;
  }
}
