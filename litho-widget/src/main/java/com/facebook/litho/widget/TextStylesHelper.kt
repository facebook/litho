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

package com.facebook.litho.widget

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.Output
import com.facebook.litho.R
import com.facebook.rendercore.utils.SynchronizedTypefaceHelper

object TextStylesHelper {
  init {
    SynchronizedTypefaceHelper.setupSynchronizedTypeface()
  }

  private val TRUNCATE_AT = TextUtils.TruncateAt.entries.toTypedArray()

  const val DEFAULT_EMS: Int = -1
  const val DEFAULT_MIN_WIDTH: Int = 0
  const val DEFAULT_MAX_WIDTH: Int = Int.MAX_VALUE
  const val DEFAULT_LINE_HEIGHT: Float = Float.MAX_VALUE
  // BREAK_STRATEGY_SIMPLE (AOSP Default)
  const val DEFAULT_BREAK_STRATEGY: Int = 0
  // HYPHENATION_FREQUENCY_NONE (AOSP Default)
  const val DEFAULT_HYPHENATION_FREQUENCY: Int = 0
  // JUSTIFICATION_MODE_NONE (AOSP Default)
  const val DEFAULT_JUSTIFICATION_MODE: Int = 0

  val textAlignmentDefault: TextAlignment = TextAlignment.TEXT_START

  @JvmStatic
  fun onLoadStyle(
      c: ComponentContext,
      ellipsize: Output<TextUtils.TruncateAt>?,
      extraSpacing: Output<Float>?,
      shouldIncludeFontPadding: Output<Boolean>?,
      spacingMultiplier: Output<Float>?,
      minLines: Output<Int>?,
      maxLines: Output<Int>?,
      minEms: Output<Int>?,
      maxEms: Output<Int>?,
      minTextWidth: Output<Int>?,
      maxTextWidth: Output<Int>?,
      isSingleLine: Output<Boolean>?,
      text: Output<CharSequence>?,
      textColorStateList: Output<ColorStateList>?,
      linkColor: Output<Int>?,
      highlightColor: Output<Int>?,
      textSize: Output<Int>?,
      textAlignment: Output<TextAlignment>?,
      breakStrategy: Output<Int>?,
      hyphenationFrequency: Output<Int>?,
      justificationMode: Output<Int>?,
      textStyle: Output<Int>?,
      shadowRadius: Output<Float>?,
      shadowDx: Output<Float>?,
      shadowDy: Output<Float>?,
      shadowColor: Output<Int>?,
      verticalGravity: Output<VerticalGravity>?,
      typeface: Output<Typeface>?
  ) {
    val theme = c.androidContext.theme

    // check first if provided attributes contain textAppearance. As an analogy to TextView
    // behavior,
    // we will parse textAppearance attributes first and then will override leftovers from main
    // style
    var a = c.obtainStyledAttributes(R.styleable.Text_TextAppearanceAttr, 0)

    val textAppearanceResId =
        a.getResourceId(R.styleable.Text_TextAppearanceAttr_android_textAppearance, -1)
    a.recycle()
    if (textAppearanceResId != -1) {
      a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.Text)
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
          typeface)
      a.recycle()
    }

    // now (after we parsed textAppearance) we can move on to main style attributes
    a = c.obtainStyledAttributes(R.styleable.Text, 0)
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
        typeface)

    a.recycle()
  }

  private fun resolveStyleAttrsForTypedArray(
      a: TypedArray,
      ellipsize: Output<TextUtils.TruncateAt>?,
      extraSpacing: Output<Float>?,
      shouldIncludeFontPadding: Output<Boolean>?,
      spacingMultiplier: Output<Float>?,
      minLines: Output<Int>?,
      maxLines: Output<Int>?,
      minEms: Output<Int>?,
      maxEms: Output<Int>?,
      minTextWidth: Output<Int>?,
      maxTextWidth: Output<Int>?,
      isSingleLine: Output<Boolean>?,
      text: Output<CharSequence>?,
      textColorStateList: Output<ColorStateList>?,
      linkColor: Output<Int>?,
      highlightColor: Output<Int>?,
      textSize: Output<Int>?,
      textAlignment: Output<TextAlignment>?,
      breakStrategy: Output<Int>?,
      hyphenationFrequency: Output<Int>?,
      justificationMode: Output<Int>?,
      textStyle: Output<Int>?,
      shadowRadius: Output<Float>?,
      shadowDx: Output<Float>?,
      shadowDy: Output<Float>?,
      shadowColor: Output<Int>?,
      verticalGravity: Output<VerticalGravity>?,
      typeface: Output<Typeface>?
  ) {
    var viewTextAlignment = View.TEXT_ALIGNMENT_GRAVITY
    var gravity = Gravity.NO_GRAVITY
    var fontFamily: String? = null

    val size = a.indexCount
    for (i in 0 until size) {
      val attr = a.getIndex(i)

      if (attr == R.styleable.Text_android_text) {
        text?.set(a.getString(attr))
      } else if (attr == R.styleable.Text_android_textColor) {
        textColorStateList?.set(a.getColorStateList(attr))
      } else if (attr == R.styleable.Text_android_textSize) {
        textSize?.set(a.getDimensionPixelSize(attr, 0))
      } else if (attr == R.styleable.Text_android_ellipsize) {
        val index = a.getInteger(attr, 0)
        if (index > 0) {
          ellipsize?.set(TRUNCATE_AT[index - 1])
        }
      } else if (attr == R.styleable.Text_android_textAlignment) {
        viewTextAlignment = a.getInt(attr, -1)
        textAlignment?.set(getTextAlignment(viewTextAlignment, gravity))
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity = a.getInt(attr, -1)
        textAlignment?.set(getTextAlignment(viewTextAlignment, gravity))
        verticalGravity?.set(getVerticalGravity(gravity))
      } else if (attr == R.styleable.Text_android_includeFontPadding) {
        shouldIncludeFontPadding?.set(a.getBoolean(attr, false))
      } else if (attr == R.styleable.Text_android_minLines) {
        minLines?.set(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_maxLines) {
        maxLines?.set(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_singleLine) {
        isSingleLine?.set(a.getBoolean(attr, false))
      } else if (attr == R.styleable.Text_android_textColorLink) {
        linkColor?.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textColorHighlight) {
        highlightColor?.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textStyle) {
        textStyle?.set(a.getInteger(attr, 0))
      } else if (attr == R.styleable.Text_android_lineSpacingExtra) {
        extraSpacing?.set(a.getDimensionPixelOffset(attr, 0).toFloat())
      } else if (attr == R.styleable.Text_android_lineSpacingMultiplier) {
        spacingMultiplier?.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDx) {
        shadowDx?.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDy) {
        shadowDy?.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowRadius) {
        shadowRadius?.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowColor) {
        shadowColor?.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_minEms) {
        minEms?.set(a.getInteger(attr, DEFAULT_EMS))
      } else if (attr == R.styleable.Text_android_maxEms) {
        maxEms?.set(a.getInteger(attr, DEFAULT_EMS))
      } else if (attr == R.styleable.Text_android_minWidth) {
        minTextWidth?.set(a.getDimensionPixelSize(attr, DEFAULT_MIN_WIDTH))
      } else if (attr == R.styleable.Text_android_maxWidth) {
        maxTextWidth?.set(a.getDimensionPixelSize(attr, DEFAULT_MAX_WIDTH))
      } else if (attr == R.styleable.Text_android_fontFamily) {
        fontFamily = a.getString(attr)
      } else if (attr == R.styleable.Text_android_breakStrategy) {
        breakStrategy?.set(a.getInt(attr, DEFAULT_BREAK_STRATEGY))
      } else if (attr == R.styleable.Text_android_hyphenationFrequency) {
        hyphenationFrequency?.set(a.getInt(attr, DEFAULT_HYPHENATION_FREQUENCY))
      } else if (attr == R.styleable.Text_android_justificationMode) {
        justificationMode?.set(a.getInt(attr, DEFAULT_JUSTIFICATION_MODE))
      }
    }

    if (fontFamily != null) {
      val styleValue = textStyle?.get()
      typeface?.set(Typeface.create(fontFamily, styleValue ?: Typeface.NORMAL))
    }
  }

  private fun getTextAlignment(viewTextAlignment: Int, gravity: Int): TextAlignment {
    return when (viewTextAlignment) {
      View.TEXT_ALIGNMENT_TEXT_START -> TextAlignment.TEXT_START
      View.TEXT_ALIGNMENT_TEXT_END -> TextAlignment.TEXT_END
      View.TEXT_ALIGNMENT_CENTER -> TextAlignment.CENTER
      View.TEXT_ALIGNMENT_VIEW_START -> TextAlignment.LAYOUT_START
      View.TEXT_ALIGNMENT_VIEW_END -> TextAlignment.LAYOUT_END
      View.TEXT_ALIGNMENT_INHERIT,
      View.TEXT_ALIGNMENT_GRAVITY -> getTextAlignment(gravity)

      else -> textAlignmentDefault
    }
  }

  private fun getTextAlignment(gravity: Int): TextAlignment {
    return when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
      Gravity.START -> TextAlignment.LAYOUT_START
      Gravity.END -> TextAlignment.LAYOUT_END
      Gravity.LEFT -> TextAlignment.LEFT
      Gravity.RIGHT -> TextAlignment.RIGHT
      Gravity.CENTER_HORIZONTAL -> TextAlignment.CENTER
      else -> textAlignmentDefault
    }
  }

  private fun getVerticalGravity(gravity: Int): VerticalGravity {
    return when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
      Gravity.TOP -> VerticalGravity.TOP
      Gravity.CENTER_VERTICAL -> VerticalGravity.CENTER
      Gravity.BOTTOM -> VerticalGravity.BOTTOM
      else -> TextComponentSpec.verticalGravity
    }
  }
}
