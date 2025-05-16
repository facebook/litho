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

import android.content.res.TypedArray
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.R
import com.facebook.rendercore.text.TextAlignment
import com.facebook.rendercore.text.TextStyle
import com.facebook.rendercore.text.VerticalGravity
import com.facebook.rendercore.utils.SynchronizedTypefaceHelper

object RCTextStyleHelper {

  init {
    SynchronizedTypefaceHelper.setupSynchronizedTypeface()
  }

  private const val DEFAULT_EMS: Int = -1
  private const val DEFAULT_MIN_WIDTH: Int = 0
  private const val DEFAULT_MAX_WIDTH: Int = Int.MAX_VALUE
  // BREAK_STRATEGY_SIMPLE (AOSP Default)
  private const val DEFAULT_BREAK_STRATEGY: Int = 0
  // HYPHENATION_FREQUENCY_NONE (AOSP Default)
  private const val DEFAULT_HYPHENATION_FREQUENCY: Int = 0
  // JUSTIFICATION_MODE_NONE (AOSP Default)
  private const val DEFAULT_JUSTIFICATION_MODE: Int = 0

  private val TRUNCATE_AT: Array<TextUtils.TruncateAt> = TextUtils.TruncateAt.entries.toTypedArray()
  private val textAlignmentDefault: TextAlignment = TextAlignment.TEXT_START

  fun onLoadStyle(context: ComponentContext, style: TextStyle) {
    val theme = context.androidContext.theme

    // check first if provided attributes contain textAppearance
    // check first if provided attributes contain textAppearance. As an analogy to TextView
    // behavior,
    // we will parse textAppearance attributes first and then will override leftovers from main
    // style
    var a: TypedArray = context.obtainStyledAttributes(R.styleable.Text_TextAppearanceAttr, 0)

    val textAppearanceResId =
        a.getResourceId(R.styleable.Text_TextAppearanceAttr_android_textAppearance, -1)
    a.recycle()

    if (textAppearanceResId != -1) {
      a = theme.obtainStyledAttributes(textAppearanceResId, R.styleable.Text)
      resolveStyleAttrsForTypedArray(a, style)
      a.recycle()
    }

    // now (after we parsed textAppearance) we can move on to main style attributes
    a = context.obtainStyledAttributes(R.styleable.Text, 0)
    resolveStyleAttrsForTypedArray(a, style)
    a.recycle()
  }

  private fun resolveStyleAttrsForTypedArray(a: TypedArray, style: TextStyle) {
    var viewTextAlignment = View.TEXT_ALIGNMENT_GRAVITY
    var gravity = Gravity.NO_GRAVITY
    var fontFamily: String? = null

    var i = 0
    val size = a.indexCount
    while (i < size) {
      val attr = a.getIndex(i)

      if (attr == R.styleable.Text_android_textColor) {
        val colorStateList = a.getColorStateList(attr)
        if (colorStateList != null) {
          style.setTextColorStateList(colorStateList)
        }
      } else if (attr == R.styleable.Text_android_textSize) {
        style.setTextSize(a.getDimensionPixelSize(attr, 0))
      } else if (attr == R.styleable.Text_android_ellipsize) {
        val index = a.getInteger(attr, 0)
        if (index > 0) {
          style.setEllipsize(TRUNCATE_AT[index - 1])
        }
      } else if (attr == R.styleable.Text_android_textAlignment) {
        viewTextAlignment = a.getInt(attr, -1)
        style.setAlignment(getTextAlignment(viewTextAlignment, gravity))
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity = a.getInt(attr, -1)
        getVerticalGravity(gravity)?.let { style.setVerticalGravity(it) }
      } else if (attr == R.styleable.Text_android_includeFontPadding) {
        style.setIncludeFontPadding(a.getBoolean(attr, false))
      } else if (attr == R.styleable.Text_android_minLines) {
        style.setMinLines(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_maxLines) {
        style.setMaxLines(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_singleLine) {
        style.setSingleLine(a.getBoolean(attr, false))
      } else if (attr == R.styleable.Text_android_textColorLink) {
        style.setLinkColor(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textColorHighlight) {
        style.setHighlightColor(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textStyle) {
        style.setTextStyle(a.getInteger(attr, 0))
      } else if (attr == R.styleable.Text_android_lineSpacingExtra) {
        style.setLineSpacingExtra(a.getDimensionPixelOffset(attr, 0).toFloat())
      } else if (attr == R.styleable.Text_android_lineSpacingMultiplier) {
        style.setLineHeightMultiplier(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDx) {
        style.setShadowDx(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDy) {
        style.setShadowDy(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowRadius) {
        style.setShadowRadius(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowColor) {
        style.setShadowColor(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_minEms) {
        style.setMinEms(a.getInteger(attr, DEFAULT_EMS))
      } else if (attr == R.styleable.Text_android_maxEms) {
        style.setMaxEms(a.getInteger(attr, DEFAULT_EMS))
      } else if (attr == R.styleable.Text_android_minWidth) {
        style.setMinTextWidth(a.getDimensionPixelSize(attr, DEFAULT_MIN_WIDTH))
      } else if (attr == R.styleable.Text_android_maxWidth) {
        style.setMaxTextWidth(a.getDimensionPixelSize(attr, DEFAULT_MAX_WIDTH))
      } else if (attr == R.styleable.Text_android_fontFamily) {
        fontFamily = a.getString(attr)
        if (fontFamily != null) {
          style.createTypeface(fontFamily)
        }
      } else if (attr == R.styleable.Text_android_breakStrategy) {
        style.setBreakStrategy(a.getInt(attr, DEFAULT_BREAK_STRATEGY))
      } else if (attr == R.styleable.Text_android_hyphenationFrequency) {
        style.setHyphenationFrequency(a.getInt(attr, DEFAULT_HYPHENATION_FREQUENCY))
      } else if (attr == R.styleable.Text_android_justificationMode) {
        style.setJustificationMode(a.getInt(attr, DEFAULT_JUSTIFICATION_MODE))
      }
      i++
    }
  }

  private fun getTextAlignment(viewTextAlignment: Int, gravity: Int): TextAlignment {
    val alignment: TextAlignment
    when (viewTextAlignment) {
      View.TEXT_ALIGNMENT_TEXT_START -> alignment = TextAlignment.TEXT_START
      View.TEXT_ALIGNMENT_TEXT_END -> alignment = TextAlignment.TEXT_END
      View.TEXT_ALIGNMENT_CENTER -> alignment = TextAlignment.CENTER
      View.TEXT_ALIGNMENT_VIEW_START -> alignment = TextAlignment.LAYOUT_START
      View.TEXT_ALIGNMENT_VIEW_END -> alignment = TextAlignment.LAYOUT_END
      View.TEXT_ALIGNMENT_INHERIT,
      View.TEXT_ALIGNMENT_GRAVITY -> alignment = getTextAlignment(gravity)
      else -> alignment = textAlignmentDefault
    }
    return alignment
  }

  private fun getTextAlignment(gravity: Int): TextAlignment {
    val alignment: TextAlignment =
        when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
          Gravity.START -> TextAlignment.LAYOUT_START
          Gravity.END -> TextAlignment.LAYOUT_END
          Gravity.LEFT -> TextAlignment.LEFT
          Gravity.RIGHT -> TextAlignment.RIGHT
          Gravity.CENTER_HORIZONTAL -> TextAlignment.CENTER
          else -> textAlignmentDefault
        }
    return alignment
  }

  private fun getVerticalGravity(gravity: Int): VerticalGravity? {
    val verticalGravity =
        when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
          Gravity.TOP -> VerticalGravity.TOP
          Gravity.CENTER_VERTICAL -> VerticalGravity.CENTER
          Gravity.BOTTOM -> VerticalGravity.BOTTOM
          else -> null // Default value needs to be set by caller
        }
    return verticalGravity
  }
}
