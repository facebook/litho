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

package com.facebook.litho

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

// TODO should be `data` if we want to consider it for comparison as a Prop.
class Style(
    val width: Dp? = null,
    val height: Dp? = null,
    val widthPercent: Float? = null,
    val heightPercent: Float? = null,
    val minWidth: Dp? = null,
    val minHeight: Dp? = null,
    val maxWidth: Dp? = null,
    val maxHeight: Dp? = null,
    val minWidthPercent: Float? = null,
    val minHeightPercent: Float? = null,
    val maxWidthPercent: Float? = null,
    val maxHeightPercent: Float? = null,

    val alignSelf: YogaAlign? = null,

    val flex: Float? = null,
    val flexGrow: Float? = null,
    val flexShrink: Float? = null,
    val flexBasis: Dp? = null,

    val aspectRatio: Float? = null,

    val paddingStart: Dp? = null,
    val paddingTop: Dp? = null,
    val paddingEnd: Dp? = null,
    val paddingBottom: Dp? = null,
    val paddingHorizontal: Dp? = null,
    val paddingVertical: Dp? = null,
    val paddingAll: Dp? = null,

    val marginStart: Dp? = null,
    val marginTop: Dp? = null,
    val marginEnd: Dp? = null,
    val marginBottom: Dp? = null,
    val marginHorizontal: Dp? = null,
    val marginVertical: Dp? = null,
    val marginAll: Dp? = null,

    val positionStart: Dp? = null,
    val positionTop: Dp? = null,
    val positionEnd: Dp? = null,
    val positionBottom: Dp? = null,
    val positionType: YogaPositionType? = null
) {

  operator fun plus(other: Style): Style {
    return Style(
        width = other.width ?: width,
        height = other.height ?: height,
        widthPercent = other.widthPercent ?: widthPercent,
        heightPercent = other.heightPercent ?: heightPercent,
        minWidth = other.minWidth ?: minWidth,
        minHeight = other.minHeight ?: minHeight,
        maxWidth = other.maxWidth ?: maxWidth,
        maxHeight = other.maxHeight ?: maxHeight,
        minWidthPercent = other.minWidthPercent ?: minWidthPercent,
        minHeightPercent = other.minHeightPercent ?: minHeightPercent,
        maxWidthPercent = other.maxWidthPercent ?: maxWidthPercent,
        maxHeightPercent = other.maxHeightPercent ?: maxHeightPercent,

        alignSelf = other.alignSelf ?: alignSelf,

        flex = other.flex ?: flex,
        flexGrow = other.flexGrow ?: flexGrow,
        flexShrink = other.flexShrink ?: flexShrink,
        flexBasis = other.flexBasis ?: flexBasis,

        aspectRatio = other.aspectRatio ?: aspectRatio,

        paddingStart = paddingStart plus other.paddingStart,
        paddingTop = paddingTop plus other.paddingTop,
        paddingEnd = paddingEnd plus other.paddingEnd,
        paddingBottom = paddingBottom plus other.paddingBottom,
        paddingHorizontal = paddingHorizontal plus other.paddingHorizontal,
        paddingVertical = paddingVertical plus other.paddingVertical,
        paddingAll = paddingAll plus other.paddingAll,

        marginStart = marginStart plus other.marginStart,
        marginTop = marginTop plus other.marginTop,
        marginEnd = marginEnd plus other.marginEnd,
        marginBottom = marginBottom plus other.marginBottom,
        marginHorizontal = marginHorizontal plus other.marginHorizontal,
        marginVertical = marginVertical plus other.marginVertical,
        marginAll = marginAll plus other.marginAll,

        positionStart = other.positionStart ?: positionStart,
        positionTop = other.positionTop ?: positionTop,
        positionEnd = other.positionEnd ?: positionEnd,
        positionBottom = other.positionBottom ?: positionBottom,
        positionType = other.positionType ?: positionType
    )
  }
}

infix fun Dp?.plus(other: Dp?) = (this ?: 0.dp) + (other ?: 0.dp)

internal fun DslScope.copyStyleToProps(style: Style, props: CommonProps) {
  props.apply {
    style.width?.let { widthPx(if (it == Dp.Hairline) 1 else it.toPx().value) }
    style.height?.let { heightPx(if (it == Dp.Hairline) 1 else it.toPx().value) }
    style.widthPercent?.let { widthPercent(it) }
    style.heightPercent?.let { heightPercent(it) }

    style.minWidth?.let { minWidthPx(it.toPx().value) }
    style.minHeight?.let { minHeightPx(it.toPx().value) }
    style.maxWidth?.let { maxWidthPx(it.toPx().value) }
    style.maxHeight?.let { maxHeightPx(it.toPx().value) }
    style.minWidthPercent?.let { minWidthPercent(it) }
    style.minHeightPercent?.let { minHeightPercent(it) }
    style.maxWidthPercent?.let { maxWidthPercent(it) }
    style.maxHeightPercent?.let { maxHeightPercent(it) }

    style.alignSelf?.let { alignSelf(it) }

    style.flex?.let { flex(it) }
    style.flexGrow?.let { flexGrow(it) }
    style.flexShrink?.let { flexShrink(it) }
    style.flexBasis?.let { flexBasisPx(it.toPx().value) }

    style.aspectRatio?.let { aspectRatio(it) }

    style.paddingStart?.let { paddingPx(YogaEdge.START, it.toPx().value) }
    style.paddingTop?.let { paddingPx(YogaEdge.TOP, it.toPx().value) }
    style.paddingEnd?.let { paddingPx(YogaEdge.END, it.toPx().value) }
    style.paddingBottom?.let { paddingPx(YogaEdge.BOTTOM, it.toPx().value) }
    style.paddingHorizontal?.let { paddingPx(YogaEdge.HORIZONTAL, it.toPx().value) }
    style.paddingVertical?.let { paddingPx(YogaEdge.VERTICAL, it.toPx().value) }
    style.paddingAll?.let { paddingPx(YogaEdge.ALL, it.toPx().value) }

    style.marginStart?.let { marginPx(YogaEdge.START, it.toPx().value) }
    style.marginTop?.let { marginPx(YogaEdge.TOP, it.toPx().value) }
    style.marginEnd?.let { marginPx(YogaEdge.END, it.toPx().value) }
    style.marginBottom?.let { marginPx(YogaEdge.BOTTOM, it.toPx().value) }
    style.marginHorizontal?.let { marginPx(YogaEdge.HORIZONTAL, it.toPx().value) }
    style.marginVertical?.let { marginPx(YogaEdge.VERTICAL, it.toPx().value) }
    style.marginAll?.let { marginPx(YogaEdge.ALL, it.toPx().value) }

    style.positionStart?.let { positionPx(YogaEdge.START, it.toPx().value) }
    style.positionTop?.let { positionPx(YogaEdge.TOP, it.toPx().value) }
    style.positionEnd?.let { positionPx(YogaEdge.END, it.toPx().value) }
    style.positionBottom?.let { positionPx(YogaEdge.BOTTOM, it.toPx().value) }
    style.positionType?.let { positionType(it) }
  }
}

fun size(size: Dp) = Style(width = size, height = size)

fun size(width: Dp? = null, height: Dp? = null) = Style(width, height)

fun width(minWidth: Dp? = null, maxWidth: Dp? = null) =
    Style(minWidth = minWidth, maxWidth = maxWidth)

fun height(minHeight: Dp? = null, maxHeight: Dp? = null) =
    Style(minHeight = minHeight, maxHeight = maxHeight)

fun flex(grow: Float? = null, shrink: Float? = null, basis: Dp? = null) =
    Style(flexGrow = grow, flexShrink = shrink, flexBasis = basis)

fun aspectRatio(aspectRatio: Float) = Style(aspectRatio = aspectRatio)

fun padding(all: Dp) = Style(paddingAll = all)

fun padding(horizontal: Dp? = null, vertical: Dp? = null) =
    Style(
        paddingStart = horizontal,
        paddingTop = vertical,
        paddingEnd = horizontal,
        paddingBottom = vertical)

fun padding(start: Dp? = null, top: Dp? = null, end: Dp? = null, bottom: Dp? = null) =
    Style(paddingStart = start, paddingTop = top, paddingEnd = end, paddingBottom = bottom)

fun margin(all: Dp) = Style(marginAll = all)

fun margin(horizontal: Dp? = null, vertical: Dp? = null) =
    Style(
        marginStart = horizontal,
        marginTop = vertical,
        marginEnd = horizontal,
        marginBottom = vertical)

fun margin(start: Dp? = null, top: Dp? = null, end: Dp? = null, bottom: Dp? = null) =
    Style(marginStart = start, marginTop = top, marginEnd = end, marginBottom = bottom)

fun position(start: Dp? = null, top: Dp? = null, end: Dp? = null, bottom: Dp? = null) =
    Style(
        positionStart = start,
        positionTop = top,
        positionEnd = end,
        positionBottom = bottom,
        positionType = YogaPositionType.ABSOLUTE)

fun positionRelative(start: Dp? = null, top: Dp? = null, end: Dp? = null, bottom: Dp? = null) =
    Style(
        positionStart = start,
        positionTop = top,
        positionEnd = end,
        positionBottom = bottom,
        positionType = YogaPositionType.RELATIVE)
