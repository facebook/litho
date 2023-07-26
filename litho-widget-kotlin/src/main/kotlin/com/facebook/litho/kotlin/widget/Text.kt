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

package com.facebook.litho.kotlin.widget

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Typeface.DEFAULT
import android.graphics.Typeface.NORMAL
import android.text.TextUtils
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.text.TextDirectionHeuristicCompat
import com.facebook.litho.DynamicValue
import com.facebook.litho.Handle
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.kotlinStyle
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextAlignment
import com.facebook.litho.widget.VerticalGravity
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Temporary builder function for creating [TextSpec] components. In the future it will either be
 * auto-generated or modified to have the final set of parameters.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Text(
    text: CharSequence?,
    style: Style? = null,
    @ColorInt textColor: Int = Color.BLACK,
    textSize: Dimen = 14.sp,
    textStyle: Int = NORMAL,
    typeface: Typeface? = DEFAULT,
    @ColorInt shadowColor: Int = Color.GRAY,
    shadowRadius: Dimen = 0.dp,
    alignment: TextAlignment = TextAlignment.TEXT_START,
    breakStrategy: Int = 0,
    verticalGravity: VerticalGravity = VerticalGravity.TOP,
    isSingleLine: Boolean = false,
    ellipsize: TextUtils.TruncateAt? = null,
    lineSpacingMultiplier: Float = 1f,
    lineHeight: Dimen? = null,
    extraSpacing: Dimen? = null,
    letterSpacing: Float = 0f,
    minLines: Int = 0,
    maxLines: Int = Int.MAX_VALUE,
    includeFontPadding: Boolean = true,
    accessibleClickableSpans: Boolean = false,
    clipToBounds: Boolean = true,
    handle: Handle? = null,
    customEllipsisText: CharSequence? = null,
    @ColorInt backgroundColor: Int? = null,
    @ColorInt highlightColor: Int? = null,
    textDirection: TextDirectionHeuristicCompat? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    dynamicTextColor: DynamicValue<Int>? = null,
    testKey: String? = null
): Text {
  val builder =
      Text.create(context, defStyleAttr, defStyleRes)
          .text(text)
          .dynamicTextColor(dynamicTextColor)
          .textColor(textColor)
          .textSizePx(textSize.toPixels())
          .textStyle(textStyle)
          .typeface(typeface)
          .shadowColor(shadowColor)
          .shadowRadiusPx(shadowRadius.toPixels().toFloat())
          .alignment(alignment)
          .breakStrategy(breakStrategy)
          .verticalGravity(verticalGravity)
          .spacingMultiplier(lineSpacingMultiplier)
          .isSingleLine(isSingleLine)
          .minLines(minLines)
          .maxLines(maxLines)
          .apply { lineHeight?.let { lineHeightPx(it.toPixels().toFloat()) } }
          .apply { extraSpacing?.let { extraSpacingPx(it.toPixels().toFloat()) } }
          .letterSpacing(letterSpacing)
          .shouldIncludeFontPadding(includeFontPadding)
          .accessibleClickableSpans(accessibleClickableSpans)
          .clipToBounds(clipToBounds)
          .apply { customEllipsisText?.let { customEllipsisText(customEllipsisText) } }
          .apply { ellipsize?.let { ellipsize(it) } }
          .handle(handle)
          .apply { backgroundColor?.let { backgroundColor(backgroundColor) } }
          .apply { highlightColor?.let { highlightColor(it) } }
          .textDirection(textDirection)
          .kotlinStyle(style)

  if (testKey != null) {
    builder.testKey(testKey)
  }

  return builder.build()
}
