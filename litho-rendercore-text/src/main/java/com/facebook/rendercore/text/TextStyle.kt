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

package com.facebook.rendercore.text

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.core.text.TextDirectionHeuristicCompat
import androidx.core.text.TextDirectionHeuristicsCompat
import kotlin.jvm.JvmField

class TextStyle : Cloneable {

  @JvmField var ellipsize: TextUtils.TruncateAt? = null
  @JvmField var customEllipsisText: CharSequence? = null
  @JvmField var includeFontPadding: Boolean = true
  @JvmField var minLines: Int = Int.MIN_VALUE
  @JvmField var maxLines: Int = Int.MAX_VALUE
  @JvmField var minEms: Int = UNSET
  @JvmField var maxEms: Int = UNSET
  @JvmField var minTextWidth: Int = 0
  @JvmField var maxTextWidth: Int = Int.MAX_VALUE
  @JvmField var shadowRadius: Float = 0f
  @JvmField var shadowDx: Float = 0f
  @JvmField var shadowDy: Float = 0f
  @ColorInt @JvmField var shadowColor: Int = Color.GRAY
  @JvmField var isSingleLine: Boolean = false
  @ColorInt @JvmField var linkColor: Int = Color.BLUE
  @JvmField var textSize: Int = UNSET
  @JvmField var shouldAddSpacingExtraToFirstLine: Boolean = false
  @JvmField var lineSpacingExtra: Float = 0f
  @JvmField var lineHeight: Float = Float.MAX_VALUE
  @JvmField var lineHeightMultiplier: Float = 1f
  @JvmField var letterSpacing: Float = 0f
  @JvmField var textStyle: Int = Typeface.DEFAULT.style
  @JvmField var typeface: Typeface? = null
  @JvmField var alignment: TextAlignment = TextAlignment.TEXT_START
  @JvmField var breakStrategy: Int = UNSET
  @JvmField var hyphenationFrequency: Int = 0
  @JvmField var justificationMode: Int = 0
  @JvmField var textDirection: TextDirectionHeuristicCompat? = null
  @JvmField var clipToBounds: Boolean = true
  @JvmField var verticalGravity: VerticalGravity = VerticalGravity.TOP
  @ColorInt @JvmField var highlightColor: Int = Color.TRANSPARENT
  @ColorInt @JvmField var keyboardHighlightColor: Int = Color.TRANSPARENT
  @JvmField var highlightStartOffset: Int = UNSET
  @JvmField var highlightEndOffset: Int = UNSET
  @JvmField var highlightCornerRadius: Int = 0
  @JvmField var shouldLayoutEmptyText: Boolean = false
  @JvmField var minimallyWide: Boolean = false
  @JvmField var minimallyWideThreshold: Int = 0

  /**
   * Click offset amount to determine how far off the ClickableSpan bounds user can click to be able
   * to trigger ClickableSpan's click action. This could be useful in a densely lined text with
   * links like 'Continue reading ...' in NewsFeed to be able to click that easily.
   */
  @JvmField var clickableSpanExpandedOffset: Float = 0f
  @JvmField var shouldTruncateTextUsingConstraints: Boolean = false
  @JvmField var manualBaselineSpacing: Int = Int.MIN_VALUE
  @JvmField var manualCapSpacing: Int = Int.MIN_VALUE
  @JvmField var extraSpacingLeft: Float = 0f
  @JvmField var extraSpacingRight: Float = 0f
  @JvmField var outlineWidth: Float = 0f
  @ColorInt @JvmField var outlineColor: Int = Color.TRANSPARENT
  @JvmField var roundedBackgroundProps: RoundedBackgroundProps? = null
  @JvmField var accessibilityLabel: String? = null
  @JvmField var truncationStyle: TruncationStyle = TruncationStyle.USE_MAX_LINES

  @ColorInt private var _textColor: Int = Color.BLACK
  private var _textColorStateList: ColorStateList? = null

  var textColor: Int
    @ColorInt get() = _textColor
    set(@ColorInt value) {
      _textColor = value
      _textColorStateList = null
    }

  var textColorStateList: ColorStateList?
    get() = _textColorStateList
    set(value) {
      _textColorStateList = value
      _textColor = 0
    }

  fun makeCopy(): TextStyle {
    try {
      return super.clone() as TextStyle
    } catch (e: CloneNotSupportedException) {
      throw RuntimeException(e)
    }
  }

  class RoundedBackgroundProps(
      @JvmField val padding: RectF,
      @JvmField val cornerRadius: Float,
      @field:ColorInt @JvmField val backgroundColor: Int
  )

  companion object {
    const val UNSET: Int = -1

    @JvmStatic
    fun createDefaultConfiguredTextStyle(context: Context): TextStyle =
        TextStylesAttributeHelper.createThemedTextStyle(context).apply {
          shouldLayoutEmptyText = true
          highlightColor = Color.TRANSPARENT
        }

    @JvmStatic
    fun maybeSetTextAlignment(textStyle: TextStyle, textAlign: Int?) {
      if (textAlign != null) {
        val textAlignment =
            when (textAlign) {
              Gravity.CENTER_HORIZONTAL -> TextAlignment.CENTER
              Gravity.START -> TextAlignment.TEXT_START
              Gravity.END -> TextAlignment.TEXT_END
              else -> TextAlignment.TEXT_START
            }
        textStyle.alignment = textAlignment
      }
    }

    @JvmStatic
    fun maybeSetTextDirection(textStyle: TextStyle, textDirectionStr: String?, isRTL: Boolean) {
      if (textDirectionStr != null) {
        val textDirection =
            when (textDirectionStr) {
              "device_locale" -> TextDirectionHeuristicsCompat.LOCALE
              "text_first_strong" ->
                  if (isRTL) TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
                  else TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR

              else -> TextDirectionHeuristicsCompat.LOCALE
            }
        textStyle.textDirection = textDirection
      }
    }

    @JvmStatic
    fun maybeSetLineHeight(textStyle: TextStyle, lineHeight: Float) {
      if (lineHeight >= 0) {
        textStyle.lineHeight = lineHeight
      }
    }

    @JvmStatic
    fun maybeSetMaxNumberOfLines(textStyle: TextStyle, maxNumberOfLines: Int) {
      if (maxNumberOfLines > -1) {
        textStyle.maxLines = maxNumberOfLines
        textStyle.ellipsize = TextUtils.TruncateAt.END
      }
    }

    @JvmStatic
    @JvmOverloads
    fun maybeSetLineHeightMultiplier(
        textStyle: TextStyle,
        lineHeightMultiplier: Float,
        applyToFirstLine: Boolean = true
    ) {
      if (lineHeightMultiplier > 0) {
        // This is a special behavior to match Bloks classic and the iOS behavior.
        textStyle.shouldAddSpacingExtraToFirstLine = applyToFirstLine
        textStyle.lineHeightMultiplier = lineHeightMultiplier
      }
    }

    @JvmStatic
    fun addThemedTextStyleForContextToCache(context: Context) {
      TextStylesAttributeHelper.addThemedTextStyleForContext(context)
    }
  }
}
