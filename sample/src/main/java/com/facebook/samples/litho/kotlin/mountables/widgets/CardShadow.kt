// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

package com.facebook.samples.litho.kotlin.mountables.widgets

import android.content.Context
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.litho.widget.CardShadowDrawable
import com.facebook.rendercore.MeasureResult

/**
 * A component that is able to render the card's shadow.
 *
 * @param shadowStartColor Start color for the shadow.
 * @param shadowEndColor End color for the shadow.
 * @param cornerRadius Corner radius for the card that shows the shadow.
 * @param shadowSize Size of the shadow.
 * @param shadowDx The x offset of the shadow.
 * @param shadowDy The y offset of the shadow.
 */
class CardShadow(
    private val shadowStartColor: Int? = null,
    private val shadowEndColor: Int? = null,
    private val cornerRadius: Float? = null,
    private val shadowSize: Float? = null,
    private val shadowDx: Float = CardShadowDrawable.UNDEFINED,
    private val shadowDy: Float = CardShadowDrawable.UNDEFINED,
    private val hideTopShadow: Boolean = false,
    private val hideBottomShadow: Boolean = false,
    private val shadowLeftSizeOverride: Float = CardShadowDrawable.UNDEFINED,
    private val shadowRightSizeOverride: Float = CardShadowDrawable.UNDEFINED,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(
        CardShadowMountable(
            shadowStartColor = shadowStartColor,
            shadowEndColor = shadowEndColor,
            cornerRadius = cornerRadius,
            shadowSize = shadowSize,
            shadowDx = shadowDx,
            shadowDy = shadowDy,
            hideTopShadow = hideTopShadow,
            hideBottomShadow = hideBottomShadow,
            shadowLeftSizeOverride = shadowLeftSizeOverride,
            shadowRightSizeOverride = shadowRightSizeOverride),
        style)
  }
}

internal class CardShadowMountable(
    private val shadowStartColor: Int?,
    private val shadowEndColor: Int?,
    private val cornerRadius: Float?,
    private val shadowSize: Float?,
    private val shadowDx: Float,
    private val shadowDy: Float,
    private val hideTopShadow: Boolean,
    private val hideBottomShadow: Boolean,
    private val shadowLeftSizeOverride: Float,
    private val shadowRightSizeOverride: Float,
) : SimpleMountable<CardShadowDrawable>(RenderType.DRAWABLE) {

  override fun createContent(context: Context): CardShadowDrawable = CardShadowDrawable()

  override fun mount(c: Context, content: CardShadowDrawable, layoutData: Any?) {
    shadowStartColor?.let { content.setShadowStartColor(shadowStartColor) }
    shadowEndColor?.let { content.setShadowEndColor(shadowEndColor) }
    cornerRadius?.let { content.setCornerRadius(cornerRadius) }
    shadowSize?.let { content.setShadowSize(shadowSize) }
    content.setHideTopShadow(hideTopShadow)
    content.setHideBottomShadow(hideBottomShadow)
    content.setShadowLeftSizeOverride(shadowLeftSizeOverride)
    content.setShadowRightSizeOverride(shadowRightSizeOverride)
    content.setShadowDx(shadowDx)
    content.setShadowDy(shadowDy)
  }

  override fun unmount(c: Context, content: CardShadowDrawable, layoutData: Any?) {
    content.setCornerRadius(0f)
    content.setShadowSize(0f)
    content.setHideTopShadow(false)
    content.setHideBottomShadow(false)
    content.setShadowLeftSizeOverride(0f)
    content.setShadowRightSizeOverride(0f)
    content.setShadowDx(0f)
    content.setShadowDy(0f)
  }

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult =
      fromSpecs(widthSpec, heightSpec)
}
