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

package com.facebook.litho.layout.flexlayout

import com.facebook.flexlayout.styles.AlignContent
import com.facebook.flexlayout.styles.AlignItems
import com.facebook.flexlayout.styles.Direction
import com.facebook.flexlayout.styles.Edge
import com.facebook.flexlayout.styles.FlexDirection
import com.facebook.flexlayout.styles.Justify
import com.facebook.flexlayout.styles.Wrap
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode

/**
 * The property set that should apply to a FlexLayout container. Please refer to the W3C
 * [specification](https://www.w3.org/TR/css-flexbox-1/#box-model) of Flexible Box Layout for more
 * details.
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.OMIT)
internal data class FlexContainerLayoutProps(
    val layoutDirection: Direction,
    val flexDirection: FlexDirection,
    val flexWrap: Wrap,
    val justifyContent: Justify,
    val alignItems: AlignItems,
    val alignContent: AlignContent,
    val paddingPx: Array<Int?>,
    val paddingPercent: Array<Float?>,
    val borderWidth: Array<Float?>
) {

  internal class Builder {
    private val edgeSize = Edge.values().size
    var layoutDirection: Direction = Direction.INHERIT
    var flexDirection: FlexDirection = FlexDirection.ROW
    var flexWrap: Wrap = Wrap.NO_WRAP
    var justifyContent: Justify = Justify.FLEX_START
    var alignItems: AlignItems = AlignItems.STRETCH
    var alignContent: AlignContent = AlignContent.STRETCH
    val paddingPx: Array<Int?> = arrayOfNulls(edgeSize)
    val paddingPercent: Array<Float?> = arrayOfNulls(edgeSize)
    var borderWidth: Array<Float?> = arrayOfNulls(edgeSize)

    fun build(): FlexContainerLayoutProps =
        FlexContainerLayoutProps(
            layoutDirection = layoutDirection,
            flexDirection = flexDirection,
            flexWrap = flexWrap,
            justifyContent = justifyContent,
            alignItems = alignItems,
            alignContent = alignContent,
            paddingPx = paddingPx,
            paddingPercent = paddingPercent,
            borderWidth = borderWidth)
  }
}
