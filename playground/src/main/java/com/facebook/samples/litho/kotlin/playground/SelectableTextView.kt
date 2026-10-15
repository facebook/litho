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

package com.facebook.samples.litho.kotlin.playground

import android.widget.TextView
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import kotlin.math.max

/**
 * A PrimitiveComponent that wraps a real Android [TextView] with [setTextIsSelectable] set to true.
 * Used to compare native TextView text selection behavior against RichText's custom selection.
 */
class SelectableTextView(
    private val text: CharSequence,
    private val textSize: Float = 14f,
    private val style: Style = Style,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = SelectableTextViewLayoutBehavior(text, textSize),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> TextView(context) }) {
              bind(text, textSize) { textView ->
                textView.text = text
                textView.textSize = textSize
                textView.setTextIsSelectable(true)
                // Set LinkMovementMethod after setTextIsSelectable so URLSpans are clickable.
                // Text selection still works because it's driven by Editor, not the MovementMethod.
                textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
                onUnbind {
                  textView.text = null
                  textView.setTextIsSelectable(false)
                }
              }
            },
        style = style,
    )
  }
}

private class SelectableTextViewLayoutBehavior(
    private val contentText: CharSequence,
    private val contentTextSize: Float,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val textView =
        TextView(androidContext).apply {
          text = contentText
          textSize = contentTextSize
        }
    textView.measure(sizeConstraints.toWidthSpec(), sizeConstraints.toHeightSpec())
    return PrimitiveLayoutResult(
        size =
            IntSize(
                max(sizeConstraints.minWidth, textView.measuredWidth),
                max(sizeConstraints.minHeight, textView.measuredHeight),
            ),
    )
  }
}
