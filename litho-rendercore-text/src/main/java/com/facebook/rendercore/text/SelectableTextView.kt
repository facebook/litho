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
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayout

/**
 * A TextView subclass that supports text selection via [setTextIsSelectable].
 *
 * This view is used as an alternative to [RCTextView] when text selection is required. It leverages
 * Android's built-in text selection functionality including selection handles, action mode
 * (copy/select all), and clipboard integration.
 *
 * Action mode lifecycle: When selection is triggered programmatically Android's Editor starts an
 * ActionMode internally but doesn't expose a reference to it. Since Litho may unmount and recycle
 * this view while the ActionMode is still active (scroll, re-render), we track the active
 * ActionMode and clean it up in [unmount] and [onDetachedFromWindow] to prevent orphaned toolbars
 * and stale selection state in pooled views.
 */
class SelectableTextView(context: Context) : TextView(context) {

  private var activeActionMode: android.view.ActionMode? = null

  init {
    setTextIsSelectable(true)
  }

  @RequiresApi(23)
  override fun startActionMode(
      callback: android.view.ActionMode.Callback,
      type: Int,
  ): android.view.ActionMode? {
    val mode = super.startActionMode(wrapCallback(callback), type)
    if (mode != null) {
      activeActionMode = mode
    }
    return mode
  }

  @RequiresApi(23)
  override fun startActionMode(
      callback: android.view.ActionMode.Callback,
  ): android.view.ActionMode? {
    val mode = super.startActionMode(wrapCallback(callback))
    if (mode != null) {
      activeActionMode = mode
    }
    return mode
  }

  /**
   * Wraps the [android.view.ActionMode.Callback] to track the active action mode for lifecycle
   * cleanup.
   *
   * Must extend [android.view.ActionMode.Callback2] so that [onGetContentRect] is delegated —
   * Android's Editor uses it to position the floating toolbar near the selection. Wrapping as plain
   * [android.view.ActionMode.Callback] would lose that, causing the toolbar to appear at the top of
   * the view instead of next to the selected text.
   */
  @RequiresApi(23)
  private fun wrapCallback(
      callback: android.view.ActionMode.Callback,
  ): android.view.ActionMode.Callback {
    return object : android.view.ActionMode.Callback2() {
      override fun onCreateActionMode(mode: android.view.ActionMode, menu: Menu): Boolean =
          callback.onCreateActionMode(mode, menu)

      override fun onPrepareActionMode(mode: android.view.ActionMode, menu: Menu): Boolean =
          callback.onPrepareActionMode(mode, menu)

      override fun onActionItemClicked(mode: android.view.ActionMode, item: MenuItem): Boolean =
          callback.onActionItemClicked(mode, item)

      override fun onDestroyActionMode(mode: android.view.ActionMode) {
        callback.onDestroyActionMode(mode)
        activeActionMode = null
      }

      override fun onGetContentRect(
          mode: android.view.ActionMode,
          view: android.view.View,
          outRect: android.graphics.Rect,
      ) {
        if (callback is android.view.ActionMode.Callback2) {
          callback.onGetContentRect(mode, view, outRect)
        } else {
          super.onGetContentRect(mode, view, outRect)
        }
      }
    }
  }

  /** Mounts the text layout to this view, applying all styling from the [TextLayout]. */
  fun mount(textLayout: TextLayout) {
    val style = textLayout.textStyle

    // Apply text content — must use SPANNABLE buffer type because setTextIsSelectable(true)
    // causes Android's Editor to cast getText() to Spannable (e.g. in onDestroyActionMode).
    // Using the default BufferType.NORMAL would store Spanned text as immutable SpannedString,
    // which does not implement Spannable and causes a ClassCastException.
    setText(textLayout.processedText, BufferType.SPANNABLE)

    // Apply text color
    val colorStateList = style.textColorStateList
    if (colorStateList != null) {
      setTextColor(colorStateList)
    } else if (style.textColor != 0) {
      setTextColor(style.textColor)
    }

    // Apply typeface and text style
    val typeface = style.typeface ?: Typeface.DEFAULT
    setTypeface(typeface, style.textStyle)

    // Apply text size
    if (style.textSize != TextStyle.UNSET) {
      setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSize.toFloat())
    }

    // Apply line spacing
    if (style.lineHeight != Float.MAX_VALUE) {
      setLineSpacing(0f, 1f)
      // Use line height directly
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        lineHeight = style.lineHeight.toInt()
      }
    } else if (style.lineHeightMultiplier != 1f || style.lineSpacingExtra != 0f) {
      setLineSpacing(style.lineSpacingExtra, style.lineHeightMultiplier)
    }

    // Apply letter spacing
    if (style.letterSpacing != 0f) {
      letterSpacing = style.letterSpacing
    }

    // Apply max/min lines
    if (style.maxLines != Int.MAX_VALUE) {
      maxLines = style.maxLines
    }
    if (style.minLines != Int.MIN_VALUE) {
      minLines = style.minLines
    }

    // Apply ellipsize
    ellipsize = style.ellipsize

    // Apply text alignment
    gravity =
        when (style.alignment) {
          TextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
          TextAlignment.TEXT_END -> Gravity.END
          TextAlignment.LAYOUT_END -> Gravity.END
          else -> Gravity.START
        }

    // Apply vertical gravity
    gravity =
        gravity or
            when (style.verticalGravity) {
              VerticalGravity.CENTER -> Gravity.CENTER_VERTICAL
              VerticalGravity.BOTTOM -> Gravity.BOTTOM
              else -> Gravity.TOP
            }

    // Apply break strategy
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && style.breakStrategy != TextStyle.UNSET) {
      breakStrategy = style.breakStrategy
    }

    // Apply hyphenation frequency
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      hyphenationFrequency = style.hyphenationFrequency
    }

    // Apply shadow
    if (style.shadowRadius > 0f) {
      setShadowLayer(style.shadowRadius, style.shadowDx, style.shadowDy, style.shadowColor)
    }

    // Apply highlight color for selection
    if (style.highlightColor != 0) {
      highlightColor = style.highlightColor
    }

    // Apply include font padding
    includeFontPadding = style.includeFontPadding

    // Apply content description for accessibility
    style.accessibilityLabel?.let { contentDescription = it }

    // Apply translation offsets
    translationX = textLayout.textLayoutTranslationX
    translationY = textLayout.textLayoutTranslationY
  }

  /** Unmounts the view, clearing all text and resetting state. */
  fun unmount() {
    activeActionMode?.finish()
    activeActionMode = null
    customSelectionActionModeCallback = null
    (text as? Spannable)?.let { Selection.removeSelection(it) }
    clearFocus()
    text = null

    // Reset text color
    setTextColor(Color.BLACK)

    // Reset typeface
    setTypeface(Typeface.DEFAULT, Typeface.DEFAULT.style)

    // Reset text size to default (will be overwritten on next mount if needed)
    setTextSize(TypedValue.COMPLEX_UNIT_PX, 0f)

    // Reset line spacing
    setLineSpacing(0f, 1f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // Reset lineHeight to a safe value; next mount will set it properly
      lineHeight = textSize.toInt().coerceAtLeast(1)
    }

    // Reset letter spacing
    letterSpacing = 0f

    // Reset max/min lines
    maxLines = Int.MAX_VALUE
    minLines = 1

    // Reset ellipsize
    ellipsize = null

    // Reset gravity
    gravity = Gravity.START or Gravity.TOP

    // Reset break strategy and hyphenation frequency
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      breakStrategy = LineBreaker.BREAK_STRATEGY_HIGH_QUALITY
      hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
    }

    // Reset shadow
    setShadowLayer(0f, 0f, 0f, 0)

    // Reset include font padding
    includeFontPadding = true

    // Reset content description
    contentDescription = null

    // Reset translation
    translationX = 0f
    translationY = 0f
  }

  override fun onDetachedFromWindow() {
    activeActionMode?.finish()
    activeActionMode = null
    customSelectionActionModeCallback = null
    super.onDetachedFromWindow()
  }
}
