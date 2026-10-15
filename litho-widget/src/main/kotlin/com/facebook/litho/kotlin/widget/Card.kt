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

import android.graphics.Color
import androidx.annotation.ColorInt
import com.facebook.litho.Component
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.widget.PrimitiveCard
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp

/** Builder function for creating Card components. */
@Suppress("FunctionName")
inline fun ResourcesScope.Card(
    style: Style = Style,
    @ColorInt cardBackgroundColor: Int = Color.WHITE,
    cornerRadius: Dimen = 2.dp,
    elevation: Dimen = 2.dp,
    @ColorInt clippingColor: Int = Integer.MIN_VALUE,
    @ColorInt shadowStartColor: Int = 0x37000000,
    @ColorInt shadowEndColor: Int = 0x03000000,
    shadowBottomOverride: Dimen? = null,
    disableClipTopLeft: Boolean = false,
    disableClipTopRight: Boolean = false,
    disableClipBottomLeft: Boolean = false,
    disableClipBottomRight: Boolean = false,
    transparencyEnabled: Boolean = false,
    crossinline child: ResourcesScope.() -> Component,
): Component = PrimitiveCard(
    content = child(),
    cardBackgroundColor = cardBackgroundColor,
    clippingColor = clippingColor,
    shadowStartColor = shadowStartColor,
    shadowEndColor = shadowEndColor,
    cornerRadius = cornerRadius,
    elevation = elevation,
    shadowBottomOverride = shadowBottomOverride,
    transparencyEnabled = transparencyEnabled,
    disableClipTopLeft = disableClipTopLeft,
    disableClipTopRight = disableClipTopRight,
    disableClipBottomLeft = disableClipBottomLeft,
    disableClipBottomRight = disableClipBottomRight,
    style = style,
)
