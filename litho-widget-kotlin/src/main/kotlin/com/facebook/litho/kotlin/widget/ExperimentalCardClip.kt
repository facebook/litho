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
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.litho.widget.CardClipDrawable
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_LEFT
import com.facebook.litho.widget.CardClipDrawable.BOTTOM_RIGHT
import com.facebook.litho.widget.CardClipDrawable.NONE
import com.facebook.litho.widget.CardClipDrawable.TOP_LEFT
import com.facebook.litho.widget.CardClipDrawable.TOP_RIGHT
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult

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
class ExperimentalCardClip(
    private val clippingColor: Int = Color.WHITE,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean = false,
    private val disableClipTopRight: Boolean = false,
    private val disableClipBottomLeft: Boolean = false,
    private val disableClipBottomRight: Boolean = false,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult =
      MountableRenderResult(
          CardClipMountable(
              clippingColor = clippingColor,
              cornerRadius = cornerRadius,
              disableClipTopLeft = disableClipTopLeft,
              disableClipTopRight = disableClipTopRight,
              disableClipBottomLeft = disableClipBottomLeft,
              disableClipBottomRight = disableClipBottomRight),
          style)
}

internal class CardClipMountable(
    private val clippingColor: Int,
    private val cornerRadius: Float? = null,
    private val disableClipTopLeft: Boolean,
    private val disableClipTopRight: Boolean,
    private val disableClipBottomLeft: Boolean,
    private val disableClipBottomRight: Boolean,
) : SimpleMountable<CardClipDrawable>(RenderType.DRAWABLE) {

  override fun createContent(context: Context): CardClipDrawable = CardClipDrawable()

  override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult = MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)

  override fun mount(c: Context, content: CardClipDrawable, layoutData: Any?) {
    clippingColor?.let { content.setClippingColor(clippingColor) }
    cornerRadius?.let { content.setCornerRadius(cornerRadius) }
    val clipEdge =
        ((if (disableClipTopLeft) TOP_LEFT else NONE) or
            (if (disableClipTopRight) TOP_RIGHT else NONE) or
            (if (disableClipBottomLeft) BOTTOM_LEFT else NONE) or
            if (disableClipBottomRight) BOTTOM_RIGHT else NONE)
    content.setDisableClip(clipEdge)
  }

  override fun unmount(c: Context, content: CardClipDrawable, layoutData: Any?) {
    content.setCornerRadius(0f)
    content.setClippingColor(Color.WHITE)
    content.setDisableClip(NONE)
  }
}
