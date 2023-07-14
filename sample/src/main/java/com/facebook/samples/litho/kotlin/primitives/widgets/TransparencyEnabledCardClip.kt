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
import androidx.annotation.ColorInt
import com.facebook.litho.DynamicValue
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.widget.CardClipDrawable
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_LEFT
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_RIGHT
import com.facebook.litho.widget.CardClipDrawable.NONE
import com.facebook.litho.widget.CardClipDrawable.TOP_LEFT
import com.facebook.litho.widget.CardClipDrawable.TOP_RIGHT
import com.facebook.litho.widget.TransparencyEnabledCardClipDrawable
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior

private const val DEFAULT_BACKGROUND_COLOR = Color.WHITE
private const val DEFAULT_CLIPPING_COLOR = Color.TRANSPARENT

/**
 * A component that paints a card with rounded edges to perform a clipping operation on the
 * component being rendered below it. Used in [CardSpec] when transparencyEnabled(true).
 *
 * @param cardBackgroundColor Background color for the drawable.
 * @param clippingColor Color for corner clipping.
 * @param cornerRadius Radius for corner clipping.
 * @param disableClipTopLeft If set, opt out of clipping the top-left corner.
 * @param disableClipTopRight If set, opt out of clipping the top-right corner.
 * @param disableClipBottomLeft If set, opt out of clipping the bottom-left corner.
 * @param disableClipBottomRight If set, opt out of clipping the bottom-right corner.
 */
class TransparencyEnabledCardClip(
    private val cardBackgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
    private val clippingColor: Int = DEFAULT_CLIPPING_COLOR,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false,
    @ColorInt private val dynamicCardBackgroundColor: DynamicValue<Int>? = null,
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ExactSizeConstraintsLayoutBehavior,
        mountBehavior =
            MountBehavior(DrawableAllocator { TransparencyEnabledCardClipDrawable() }) {
              bind(cornerRadius) { content ->
                if (cornerRadius != null) {
                  content.setCornerRadius(cornerRadius)
                }
                onUnbind {
                  if (cornerRadius != null) {
                    content.setCornerRadius(0f)
                  }
                }
              }

              bind(
                  disableClipTopLeft,
                  disableClipTopRight,
                  disableClipBottomLeft,
                  disableClipBottomRight) { content ->
                    val clipEdge =
                        ((if (disableClipTopLeft) TOP_LEFT else NONE) or
                            (if (disableClipTopRight) TOP_RIGHT else NONE) or
                            (if (disableClipBottomLeft) BOTTOM_LEFT else NONE) or
                            if (disableClipBottomRight) BOTTOM_RIGHT else NONE)
                    content.setDisableClip(clipEdge)
                    onUnbind { content.setDisableClip(CardClipDrawable.NONE) }
                  }

              cardBackgroundColor.bindTo(
                  TransparencyEnabledCardClipDrawable::setBackgroundColor, DEFAULT_BACKGROUND_COLOR)

              clippingColor.bindTo(
                  TransparencyEnabledCardClipDrawable::setClippingColor, DEFAULT_CLIPPING_COLOR)

              bindDynamic(
                  dynamicCardBackgroundColor,
                  TransparencyEnabledCardClipDrawable::setBackgroundColor,
                  DEFAULT_BACKGROUND_COLOR)
            },
        style = style)
  }
}
