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

import android.graphics.drawable.ColorDrawable
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.withPreferredSize

class SimpleMountSpecTester(
    private val color: Int = 0,
    private val measuredWidth: Int? = null,
    private val measuredHeight: Int? = null,
    private val style: Style = Style,
) : PrimitiveComponent() {

  constructor(style: Style) : this(0, null, null, style)

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = SimpleMountSpecTesterLayoutBehavior(measuredWidth, measuredHeight),
        mountBehavior =
            MountBehavior(DrawableAllocator { ColorDrawable() }) {
              bind(color) { content ->
                content.color = color
                onUnbind { content.color = 0 }
              }
            },
        style = style,
    )
  }
}

private class SimpleMountSpecTesterLayoutBehavior(
    private val measuredWidth: Int?,
    private val measuredHeight: Int?,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return if (measuredWidth == null && measuredHeight == null) {
      PrimitiveLayoutResult(
          width = SizeSpec.getSize(sizeConstraints.toWidthSpec()),
          height = SizeSpec.getSize(sizeConstraints.toHeightSpec()),
      )
    } else {
      val width = measuredWidth ?: SizeSpec.UNSPECIFIED
      val height = measuredHeight ?: SizeSpec.UNSPECIFIED
      PrimitiveLayoutResult(IntSize.withPreferredSize(sizeConstraints, width, height))
    }
  }
}
