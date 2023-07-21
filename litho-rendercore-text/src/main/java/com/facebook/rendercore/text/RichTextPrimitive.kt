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

import androidx.core.util.component1
import androidx.core.util.component2
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

fun RichTextPrimitive(id: Long, text: CharSequence, style: TextStyle): Primitive {
  return Primitive(
      layoutBehavior = RichTextLayoutBehavior(text, style),
      mountBehavior =
          MountBehavior(id = id, contentAllocator = ViewAllocator { c -> RCTextView(c) }) {
            bindWithLayoutData<TextLayout>(Unit) { content, textLayout ->
              content.mount(textLayout)
              onUnbind { content.unmount() }
            }
          })
}

private class RichTextLayoutBehavior(val text: CharSequence, val style: TextStyle) :
    LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val (size, textLayout) =
        layout(
            androidContext,
            sizeConstraints.toWidthSpec(),
            sizeConstraints.toHeightSpec(),
            text,
            style)

    return PrimitiveLayoutResult(
        width = max(size.width(), sizeConstraints.minWidth),
        height = max(size.height(), sizeConstraints.minHeight),
        layoutData = textLayout)
  }
}
