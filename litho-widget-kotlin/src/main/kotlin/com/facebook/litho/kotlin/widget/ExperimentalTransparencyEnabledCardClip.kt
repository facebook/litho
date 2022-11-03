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

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.facebook.litho.DynamicValue
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_LEFT
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_RIGHT
import com.facebook.litho.widget.CardClipDrawable.NONE
import com.facebook.litho.widget.CardClipDrawable.TOP_LEFT
import com.facebook.litho.widget.CardClipDrawable.TOP_RIGHT
import com.facebook.litho.widget.TransparencyEnabledCardClipDrawable
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult

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
class ExperimentalTransparencyEnabledCardClip(
    private val cardBackgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
    private val clippingColor: Int = DEFAULT_CLIPPING_COLOR,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false,
    @ColorInt private val dynamicCardBackgroundColor: DynamicValue<Int>? = null,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    dynamicCardBackgroundColor?.bindTo(
        DEFAULT_BACKGROUND_COLOR, TransparencyEnabledCardClipDrawable::setBackgroundColor)

    return MountableRenderResult(
        mountable =
            TransparencyEnabledCardClipMountable(
                cardBackgroundColor = cardBackgroundColor,
                clippingColor = clippingColor,
                cornerRadius = cornerRadius,
                disableClipTopLeft = disableClipTopLeft,
                disableClipTopRight = disableClipTopRight,
                disableClipBottomLeft = disableClipBottomLeft,
                disableClipBottomRight = disableClipBottomRight),
        style = style)
  }
}

internal class TransparencyEnabledCardClipMountable(
    private val cardBackgroundColor: Int,
    private val clippingColor: Int,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean,
    private val disableClipTopRight: Boolean,
    private val disableClipBottomLeft: Boolean,
    private val disableClipBottomRight: Boolean
) : SimpleMountable<TransparencyEnabledCardClipDrawable>(RenderType.DRAWABLE) {

  override fun createContent(context: Context): TransparencyEnabledCardClipDrawable =
      TransparencyEnabledCardClipDrawable()

  override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult = MeasureResult.fromSpecs(widthSpec, heightSpec)

  override fun mount(c: Context, content: TransparencyEnabledCardClipDrawable, layoutData: Any?) {
    content.setBackgroundColor(cardBackgroundColor)
    content.setClippingColor(clippingColor)
    cornerRadius?.let { content.setCornerRadius(cornerRadius) }
    val clipEdge =
        ((if (disableClipTopLeft) TOP_LEFT else NONE) or
            (if (disableClipTopRight) TOP_RIGHT else NONE) or
            (if (disableClipBottomLeft) BOTTOM_LEFT else NONE) or
            if (disableClipBottomRight) BOTTOM_RIGHT else NONE)
    content.setDisableClip(clipEdge)
  }

  override fun unmount(c: Context, content: TransparencyEnabledCardClipDrawable, layoutData: Any?) {
    content.setBackgroundColor(DEFAULT_BACKGROUND_COLOR)
    content.setClippingColor(DEFAULT_CLIPPING_COLOR)
    content.setCornerRadius(0f)
    content.resetCornerPaint()
    content.setDisableClip(NONE)
  }
}
