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

package com.facebook.samples.litho.kotlin.primitives.widgets

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.widget.ProgressView
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator

/**
 * Renders an infinitely spinning progress bar.
 *
 * @param indeterminateDrawable Drawable to be shown to show progress.
 * @param color Tint color for the drawable.
 */
class Progress(
    private val color: Int = Color.TRANSPARENT,
    private val indeterminateDrawable: Drawable? = null,
    private val style: Style? = null
) : PrimitiveComponent() {

  @Suppress("DEPRECATION")
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ProgressLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> ProgressView(context) }) {
              bind(indeterminateDrawable, color) { content ->
                indeterminateDrawable?.let { content.indeterminateDrawable = indeterminateDrawable }
                content.indeterminateDrawable?.let {
                  if (color != Color.TRANSPARENT) {
                    content.indeterminateDrawable
                        .mutate()
                        .setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                  }
                }
                onUnbind {
                  // restore the color first, since it acts on the indeterminateDrawable
                  if (color != Color.TRANSPARENT && content.indeterminateDrawable != null) {
                    content.indeterminateDrawable.mutate().clearColorFilter()
                  }
                  content.indeterminateDrawable = null
                }
              }
            },
        style = style)
  }
}

internal object ProgressLayoutBehavior : LayoutBehavior {
  private const val DEFAULT_SIZE: Int = 50

  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED &&
            SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          MeasureResult(DEFAULT_SIZE, DEFAULT_SIZE)
        } else {
          MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)
        }

    return PrimitiveLayoutResult(result.width, result.height)
  }
}
