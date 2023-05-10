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
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.widget.CardClipDrawable
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior

/**
 * A component that paints rounded edges to mimic a clipping operation on the component being
 * rendered below it. Used in {@link CardSpec}.
 *
 * @param clippingColor Color for corner clipping.
 * @param cornerRadius Radius for corner clipping.
 * @param disableClipTopLeft If set, opt out of clipping the top-left corner
 * @param disableClipTopRight If set, opt out of clipping the top-right corner
 * @param disableClipBottomLeft If set, opt out of clipping the bottom-left corner
 * @param disableClipBottomRight If set, opt out of clipping the bottom-right corner
 */
class CardClip(
    private val clippingColor: Int = Color.WHITE,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false,
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ExactSizeConstraintsLayoutBehavior,
        mountBehavior =
            MountBehavior(DrawableAllocator { CardClipDrawable() }) {
              clippingColor.bindTo(CardClipDrawable::setClippingColor, Color.WHITE)
              bind(cornerRadius) { content ->
                cornerRadius?.let { content.setCornerRadius(it) }
                onUnbind { content.setCornerRadius(0f) }
              }
              bind(
                  disableClipTopLeft,
                  disableClipTopRight,
                  disableClipBottomLeft,
                  disableClipBottomRight) { content ->
                    val clipEdge =
                        ((if (disableClipTopLeft) CardClipDrawable.TOP_LEFT
                        else CardClipDrawable.NONE) or
                            (if (disableClipTopRight) CardClipDrawable.TOP_RIGHT
                            else CardClipDrawable.NONE) or
                            (if (disableClipBottomLeft) CardClipDrawable.BOTTOM_LEFT
                            else CardClipDrawable.NONE) or
                            if (disableClipBottomRight) CardClipDrawable.BOTTOM_RIGHT
                            else CardClipDrawable.NONE)
                    content.setDisableClip(clipEdge)
                    onUnbind { content.setDisableClip(CardClipDrawable.NONE) }
                  }
            },
        style)
  }
}
