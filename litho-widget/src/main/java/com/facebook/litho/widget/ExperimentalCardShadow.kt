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
import androidx.annotation.ColorInt
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
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
class ExperimentalCardShadow(
    @ColorInt private val shadowStartColor: Int = Color.TRANSPARENT,
    @ColorInt private val shadowEndColor: Int = Color.TRANSPARENT,
    private val cornerRadius: Float = 0f,
    private val shadowSize: Float = 0f,
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
              shadowStartColor.bindTo(CardShadowDrawable::setShadowStartColor, Color.TRANSPARENT)
              shadowEndColor.bindTo(CardShadowDrawable::setShadowEndColor, Color.TRANSPARENT)
              cornerRadius.bindTo(CardShadowDrawable::setCornerRadius, 0f)
              shadowSize.bindTo(CardShadowDrawable::setShadowSize, 0f)
            },
        style = style)
  }
}
