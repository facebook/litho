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

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.utils.withEqualDimensions

/**
 * Renders an infinitely spinning progress bar.
 *
 * @param indeterminateDrawable Drawable to be shown to show progress.
 * @param color Tint color for the drawable.
 */
class ProgressPrimitiveComponent(
    private val color: Int = Color.TRANSPARENT,
    private val indeterminateDrawable: Drawable? = null,
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ProgressLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> ProgressView(context) }) {
              bind(indeterminateDrawable, color) { content ->
                val defaultIndeterminateDrawable = content.indeterminateDrawable

                indeterminateDrawable?.let { content.indeterminateDrawable = indeterminateDrawable }
                content.indeterminateDrawable?.let {
                  if (color != Color.TRANSPARENT) {
                    content.indeterminateDrawable.mutate().colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            color, BlendModeCompat.MODULATE)
                  }
                }
                onUnbind {
                  // restore the color first, since it acts on the indeterminateDrawable
                  if (color != Color.TRANSPARENT && content.indeterminateDrawable != null) {
                    content.indeterminateDrawable.mutate().clearColorFilter()
                  }

                  content.indeterminateDrawable = defaultIndeterminateDrawable
                }
              }
            },
        style = style)
  }

  companion object {
    const val DEFAULT_SIZE: Int = 50
  }
}

internal object ProgressLayoutBehavior : LayoutBehavior {

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
              Size(ProgressPrimitiveComponent.DEFAULT_SIZE, ProgressPrimitiveComponent.DEFAULT_SIZE)
            } else {
              Size.withEqualDimensions(sizeConstraints)
            })
  }
}
