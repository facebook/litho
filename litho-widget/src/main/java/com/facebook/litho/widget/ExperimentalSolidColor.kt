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
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.useCached
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FillLayoutBehavior
import kotlin.math.min

/**
 * A component that renders a solid color.
 *
 * @uidocs
 * @prop color Color to be shown.
 * @prop alpha The alpha of the color, in the range [0.0, 1.0]
 */
@ExperimentalLithoApi
class ExperimentalSolidColor(
    @ColorInt private val color: Int,
    private val alpha: Float = -1.0f,
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val colorWithAlpha =
        useCached(color, alpha) {
          if (alpha >= 0f) {
            val clampedAlpha = min(1.0f, alpha)
            ColorUtils.setAlphaComponent(color, (clampedAlpha * 255f).toInt())
          } else {
            color
          }
        }

    return LithoPrimitive(
        layoutBehavior = FillLayoutBehavior(defaultWidth = 0, defaultHeight = 0),
        mountBehavior =
            MountBehavior(DrawableAllocator { ColorDrawable() }) {
              bind(colorWithAlpha) { content ->
                val defaultColor = content.color
                content.color = colorWithAlpha
                onUnbind { content.color = defaultColor }
              }
            },
        style = style)
  }
}
