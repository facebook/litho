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

import com.facebook.flexlayout.styles.AlignSelf
import com.facebook.flexlayout.styles.Edge
import com.facebook.flexlayout.styles.PositionType
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode

/**
 * The property set that should apply to a FlexLayout item. Please refer to the W3C
 * [specification](https://www.w3.org/TR/css-flexbox-1/#box-model) of Flexible Box Layout for more
 * details.
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.OMIT)
internal data class FlexItemLayoutProps(
    val widthPx: Int?,
    val widthPercent: Float?,
    val heightPx: Int?,
    val heightPercent: Float?,
    val minWidthPx: Int?,
    val minWidthPercent: Float?,
    val minHeightPx: Int?,
    val minHeightPercent: Float?,
    val maxWidthPx: Int?,
    val maxWidthPercent: Float?,
    val maxHeightPx: Int?,
    val maxHeightPercent: Float?,
    val widthAuto: Boolean?,
    val heightAuto: Boolean?,
    val positionType: PositionType,
    val positionPx: Array<Int?>,
    val positionPercent: Array<Float?>,
    val marginPx: Array<Int?>,
    val marginPercent: Array<Float?>,
    val marginAutos: Array<Boolean?>,
    val paddingPx: Array<Int?>,
    val paddingPercent: Array<Float?>,
    val aspectRatio: Float?,
    val flex: Float?,
    val flexGrow: Float,
    val flexShrink: Float,
    val alignSelf: AlignSelf?,
    val flexBasisPx: Int?,
    val flexBasisPercent: Float?,
    val flexBasisAuto: Boolean?,
    val isReferenceBaseline: Boolean?,
    val useHeightAsBaseline: Boolean?
) {

  internal class Builder {
    private val edgeSize = Edge.values().size
    var widthPx: Int? = null
    var widthPercent: Float? = null
    var heightPx: Int? = null
    var heightPercent: Float? = null
    var minWidthPx: Int? = null
    var minWidthPercent: Float? = null
    var minHeightPx: Int? = null
    var minHeightPercent: Float? = null
    var maxWidthPx: Int? = null
    var maxWidthPercent: Float? = null
    var maxHeightPx: Int? = null
    var maxHeightPercent: Float? = null
    var widthAuto: Boolean? = null
    var heightAuto: Boolean? = null
    var positionType: PositionType = PositionType.RELATIVE
    var positionPx: Array<Int?> = arrayOfNulls(edgeSize)
    var positionPercent: Array<Float?> = arrayOfNulls(edgeSize)
    var marginPx: Array<Int?> = arrayOfNulls(edgeSize)
    var marginPercent: Array<Float?> = arrayOfNulls(edgeSize)
    var marginAutos: Array<Boolean?> = arrayOfNulls(edgeSize)
    // Padding only belongs to containers in FlexBox API, this is to compatible with Yoga 1.
    val paddingPx: Array<Int?> = arrayOfNulls(edgeSize)
    val paddingPercent: Array<Float?> = arrayOfNulls(edgeSize)
    var aspectRatio: Float? = null
    var flex: Float? = null
    var flexGrow: Float = 0f
    var flexShrink: Float = 1f
    var alignSelf: AlignSelf = AlignSelf.AUTO
    var flexBasisPx: Int? = null
    var flexBasisPercent: Float? = null
    var flexBasisAuto: Boolean? = null
    var isReferenceBaseline: Boolean? = null
    var useHeightAsBaseline: Boolean? = null

    fun build(): FlexItemLayoutProps =
        FlexItemLayoutProps(
            widthPx = widthPx,
            widthPercent = widthPercent,
            heightPx = heightPx,
            heightPercent = heightPercent,
            minWidthPx = minWidthPx,
            minWidthPercent = minWidthPercent,
            minHeightPx = minHeightPx,
            minHeightPercent = minHeightPercent,
            maxWidthPx = maxWidthPx,
            maxWidthPercent = maxWidthPercent,
            maxHeightPx = maxHeightPx,
            maxHeightPercent = maxHeightPercent,
            widthAuto = widthAuto,
            heightAuto = heightAuto,
            positionType = positionType,
            positionPx = positionPx,
            positionPercent = positionPercent,
            marginPx = marginPx,
            marginPercent = marginPercent,
            marginAutos = marginAutos,
            paddingPx = paddingPx,
            paddingPercent = paddingPercent,
            aspectRatio = aspectRatio,
            flex = flex,
            flexGrow = flexGrow,
            flexShrink = flexShrink,
            alignSelf = alignSelf,
            flexBasisPx = flexBasisPx,
            flexBasisPercent = flexBasisPercent,
            flexBasisAuto = flexBasisAuto,
            isReferenceBaseline = isReferenceBaseline,
            useHeightAsBaseline = useHeightAsBaseline)
  }
}
