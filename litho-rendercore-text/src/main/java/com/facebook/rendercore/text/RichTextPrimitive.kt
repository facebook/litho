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

import android.graphics.Rect
import androidx.core.util.component1
import androidx.core.util.component2
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.text.TextMeasurementUtils.TextLayout
import com.facebook.rendercore.text.TextMeasurementUtils.layout
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import java.lang.Integer.max

fun RichTextPrimitive(
    id: Long,
    text: CharSequence,
    style: TextStyle,
    touchableSpanListener: TouchableSpanListener? = null,
    clickableSpanListener: ClickableSpanListener? = null,
    usePerformantTruncation: Boolean = RenderCoreConfig.usePerformantTruncation,
    useTruncationCaching: Boolean = RenderCoreConfig.useTruncationCaching,
): Primitive {
  return Primitive(
      layoutBehavior =
          RichTextLayoutBehavior(text, style, usePerformantTruncation, useTruncationCaching),
      mountBehavior =
          MountBehavior(
              id = id,
              contentAllocator =
                  ViewAllocator(
                      canPreallocate = RichTextPrimitiveConfig.canPreallocation,
                      poolSize = RichTextPrimitiveConfig.poolSize,
                  ) { c ->
                    RCTextView(c)
                  }) {
                bindWithLayoutData<RichTextLayoutData>(Unit) { content, layoutData ->
                  content.mount(layoutData.textLayout)
                  onUnbind { content.unmount() }
                }
                touchableSpanListener.bindTo(RCTextView::setTouchableSpanListener)
                clickableSpanListener.bindTo(RCTextView::setClickableSpanListener)
              })
}

private class RichTextLayoutBehavior(
    val text: CharSequence,
    val style: TextStyle,
    val usePerformantTruncation: Boolean,
    val useTruncationCaching: Boolean,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val previousRichTextLayoutData = previousLayoutData as? RichTextLayoutData
    if (useTruncationCaching &&
        previousRichTextLayoutData != null &&
        canReuseTextLayout(previousRichTextLayoutData, text, style, sizeConstraints)) {
      return PrimitiveLayoutResult(
          width = max(previousRichTextLayoutData.size.width(), sizeConstraints.minWidth),
          height = max(previousRichTextLayoutData.size.height(), sizeConstraints.minHeight),
          // TODO: This should copy the textLayout with new clickable spans
          layoutData =
              RichTextLayoutData(
                  previousRichTextLayoutData.textLayout,
                  previousRichTextLayoutData.size,
                  style,
                  sizeConstraints))
    }

    val (size, textLayout) =
        layout(
            androidContext,
            sizeConstraints.toWidthSpec(),
            sizeConstraints.toHeightSpec(),
            text,
            style,
            usePerformantTruncation)
    return PrimitiveLayoutResult(
        width = max(size.width(), sizeConstraints.minWidth),
        height = max(size.height(), sizeConstraints.minHeight),
        layoutData = RichTextLayoutData(textLayout, size, style, sizeConstraints))
  }

  private fun canReuseTextLayout(
      previousLayoutData: RichTextLayoutData,
      text: CharSequence,
      style: TextStyle,
      sizeConstraints: SizeConstraints
  ): Boolean {
    return previousLayoutData.textLayout.isExplicitlyTruncated &&
        previousLayoutData.sizeConstraints == sizeConstraints &&
        hasEquivalentStyle(style, previousLayoutData.style) &&
        hasEquivalentTruncatedText(previousLayoutData.textLayout, text, style)
  }

  // TODO: this should be moved to TextStyle and emcompass all properties
  fun hasEquivalentStyle(style1: TextStyle, style2: TextStyle): Boolean {
    return style1.textSize == style2.textSize &&
        style1.textColor == style2.textColor &&
        style1.textDirection == style2.textDirection &&
        style1.textStyle == style2.textStyle &&
        style1.truncationStyle == style2.truncationStyle
  }

  private fun hasEquivalentTruncatedText(
      textLayout: TextLayout,
      text: CharSequence,
      style: TextStyle
  ): Boolean {
    if (!textLayout.isExplicitlyTruncated) {
      return false
    }
    val textLayoutText = textLayout.layout.text
    val suffix = style.customEllipsisText ?: ""
    return textLayoutText.endsWith(suffix) &&
        text.startsWith(textLayoutText.subSequence(0, textLayoutText.length - suffix.length))
  }
}

private class RichTextLayoutData(
    val textLayout: TextLayout,
    val size: Rect,
    val style: TextStyle,
    val sizeConstraints: SizeConstraints
)

object RichTextPrimitiveConfig {
  val canPreallocation: Boolean = true
  val poolSize: Int = 10
}
