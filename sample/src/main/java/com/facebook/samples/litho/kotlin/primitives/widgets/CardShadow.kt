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
import com.facebook.litho.widget.CardShadowDrawable
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior

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
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ExactSizeConstraintsLayoutBehavior,
        mountBehavior =
            MountBehavior(DrawableAllocator { CardShadowDrawable() }) {
              hideTopShadow.bindTo(CardShadowDrawable::setHideTopShadow, false)
              hideBottomShadow.bindTo(CardShadowDrawable::setHideBottomShadow, false)
              shadowLeftSizeOverride.bindTo(
                  CardShadowDrawable::setShadowLeftSizeOverride, CardShadowDrawable.UNDEFINED)
              shadowRightSizeOverride.bindTo(
                  CardShadowDrawable::setShadowRightSizeOverride, CardShadowDrawable.UNDEFINED)
              shadowDx.bindTo(CardShadowDrawable::setShadowDx, CardShadowDrawable.UNDEFINED)
              shadowDy.bindTo(CardShadowDrawable::setShadowDy, CardShadowDrawable.UNDEFINED)
              bind(shadowStartColor) { content ->
                shadowStartColor?.let { content.setShadowStartColor(it) }
                onUnbind { content.setShadowStartColor(Color.BLACK) }
              }
              bind(shadowEndColor) { content ->
                shadowEndColor?.let { content.setShadowEndColor(it) }
                onUnbind { content.setShadowEndColor(Color.BLACK) }
              }
              bind(cornerRadius) { content ->
                cornerRadius?.let { content.setCornerRadius(it) }
                onUnbind { content.setCornerRadius(0f) }
              }
              bind(shadowSize) { content ->
                shadowSize?.let { content.setShadowSize(it) }
                onUnbind { content.setShadowSize(0f) }
              }
            },
        style = style)
  }
}
