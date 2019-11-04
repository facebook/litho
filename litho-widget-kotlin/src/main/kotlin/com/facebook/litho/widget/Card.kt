/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Dp
import com.facebook.litho.dp

/**
 * Builder function for creating [CardSpec] components.
 */
@Suppress("FunctionName")
inline fun ComponentContext.Card(
    @ColorInt cardBackgroundColor: Int = Color.WHITE,
    cornerRadius: Dp = 2.dp,
    elevation: Dp = 2.dp,
    @ColorInt clippingColor: Int = Color.WHITE,
    @ColorInt shadowStartColor: Int = 0x37000000,
    @ColorInt shadowEndColor: Int = 0x03000000,
    shadowBottomOverride: Dp? = null,
    disableClipTopLeft: Boolean = false,
    disableClipTopRight: Boolean = false,
    disableClipBottomLeft: Boolean = false,
    disableClipBottomRight: Boolean = false,
    child: ComponentContext.() -> Component.Builder<*>
): Card.Builder =
    Card.create(this)
        .cardBackgroundColor(cardBackgroundColor)
        .cornerRadiusDip(cornerRadius.dp)
        .elevationDip(elevation.dp)
        .clippingColor(clippingColor)
        .shadowStartColor(shadowStartColor)
        .shadowEndColor(shadowEndColor)
        .shadowBottomOverridePx(shadowBottomOverride?.toPx(this)?.px ?: -1)
        .disableClipTopLeft(disableClipTopLeft)
        .disableClipTopRight(disableClipTopRight)
        .disableClipBottomLeft(disableClipBottomLeft)
        .disableClipBottomRight(disableClipBottomRight)
        .content(child())
