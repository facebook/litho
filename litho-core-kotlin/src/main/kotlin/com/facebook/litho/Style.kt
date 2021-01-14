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

import android.graphics.drawable.Drawable
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

// TODO should be `data` if we want to consider it for comparison as a Prop.
open class Style(
    val width: Dimen? = null,
    val height: Dimen? = null,
    val widthPercent: Float? = null,
    val heightPercent: Float? = null,
    val minWidth: Dimen? = null,
    val minHeight: Dimen? = null,
    val maxWidth: Dimen? = null,
    val maxHeight: Dimen? = null,
    val minWidthPercent: Float? = null,
    val minHeightPercent: Float? = null,
    val maxWidthPercent: Float? = null,
    val maxHeightPercent: Float? = null,
    val alignSelf: YogaAlign? = null,
    val flex: Float? = null,
    val flexGrow: Float? = null,
    val flexShrink: Float? = null,
    val flexBasis: Dimen? = null,
    val aspectRatio: Float? = null,
    val paddingStart: Dimen? = null,
    val paddingTop: Dimen? = null,
    val paddingEnd: Dimen? = null,
    val paddingBottom: Dimen? = null,
    val paddingHorizontal: Dimen? = null,
    val paddingVertical: Dimen? = null,
    val paddingAll: Dimen? = null,
    val marginStart: Dimen? = null,
    val marginTop: Dimen? = null,
    val marginEnd: Dimen? = null,
    val marginBottom: Dimen? = null,
    val marginHorizontal: Dimen? = null,
    val marginVertical: Dimen? = null,
    val marginAll: Dimen? = null,
    val positionStart: Dimen? = null,
    val positionTop: Dimen? = null,
    val positionEnd: Dimen? = null,
    val positionBottom: Dimen? = null,
    val positionType: YogaPositionType? = null,
    val background: Drawable? = null,
    val foreground: Drawable? = null,
    val wrapInView: Boolean? = null,
) {

  operator fun plus(other: Style?): Style {
    if (other == null) {
      return this
    }
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
        paddingStart = other.paddingStart ?: paddingStart,
        paddingTop = other.paddingTop ?: paddingTop,
        paddingEnd = other.paddingEnd ?: paddingEnd,
        paddingBottom = other.paddingBottom ?: paddingBottom,
        paddingHorizontal = other.paddingHorizontal ?: paddingHorizontal,
        paddingVertical = other.paddingVertical ?: paddingVertical,
        paddingAll = other.paddingAll ?: paddingAll,
        marginStart = other.marginStart ?: marginStart,
        marginTop = other.marginTop ?: marginTop,
        marginEnd = other.marginEnd ?: marginEnd,
        marginBottom = other.marginBottom ?: marginBottom,
        marginHorizontal = other.marginHorizontal ?: marginHorizontal,
        marginVertical = other.marginVertical ?: marginVertical,
        marginAll = other.marginAll ?: marginAll,
        positionStart = other.positionStart ?: positionStart,
        positionTop = other.positionTop ?: positionTop,
        positionEnd = other.positionEnd ?: positionEnd,
        positionBottom = other.positionBottom ?: positionBottom,
        positionType = other.positionType ?: positionType,
        background = other.background ?: background,
        foreground = other.foreground ?: foreground,
        wrapInView = other.wrapInView ?: wrapInView)
  }

  companion object : Style()
}

internal fun DslScope.copyStyleToProps(style: Style, props: CommonProps) {
  props.apply {
    style.width?.let { widthPx(if (it == Dimen.Hairline) 1 else it.toPixels()) }
    style.height?.let { heightPx(if (it == Dimen.Hairline) 1 else it.toPixels()) }
    style.widthPercent?.let { widthPercent(it) }
    style.heightPercent?.let { heightPercent(it) }

    style.minWidth?.let { minWidthPx(it.toPixels()) }
    style.minHeight?.let { minHeightPx(it.toPixels()) }
    style.maxWidth?.let { maxWidthPx(it.toPixels()) }
    style.maxHeight?.let { maxHeightPx(it.toPixels()) }
    style.minWidthPercent?.let { minWidthPercent(it) }
    style.minHeightPercent?.let { minHeightPercent(it) }
    style.maxWidthPercent?.let { maxWidthPercent(it) }
    style.maxHeightPercent?.let { maxHeightPercent(it) }

    style.alignSelf?.let { alignSelf(it) }

    style.flex?.let { flex(it) }
    style.flexGrow?.let { flexGrow(it) }
    style.flexShrink?.let { flexShrink(it) }
    style.flexBasis?.let { flexBasisPx(it.toPixels()) }

    style.aspectRatio?.let { aspectRatio(it) }

    style.paddingStart?.let { paddingPx(YogaEdge.START, it.toPixels()) }
    style.paddingTop?.let { paddingPx(YogaEdge.TOP, it.toPixels()) }
    style.paddingEnd?.let { paddingPx(YogaEdge.END, it.toPixels()) }
    style.paddingBottom?.let { paddingPx(YogaEdge.BOTTOM, it.toPixels()) }
    style.paddingHorizontal?.let { paddingPx(YogaEdge.HORIZONTAL, it.toPixels()) }
    style.paddingVertical?.let { paddingPx(YogaEdge.VERTICAL, it.toPixels()) }
    style.paddingAll?.let { paddingPx(YogaEdge.ALL, it.toPixels()) }

    style.marginStart?.let { marginPx(YogaEdge.START, it.toPixels()) }
    style.marginTop?.let { marginPx(YogaEdge.TOP, it.toPixels()) }
    style.marginEnd?.let { marginPx(YogaEdge.END, it.toPixels()) }
    style.marginBottom?.let { marginPx(YogaEdge.BOTTOM, it.toPixels()) }
    style.marginHorizontal?.let { marginPx(YogaEdge.HORIZONTAL, it.toPixels()) }
    style.marginVertical?.let { marginPx(YogaEdge.VERTICAL, it.toPixels()) }
    style.marginAll?.let { marginPx(YogaEdge.ALL, it.toPixels()) }

    style.positionStart?.let { positionPx(YogaEdge.START, it.toPixels()) }
    style.positionTop?.let { positionPx(YogaEdge.TOP, it.toPixels()) }
    style.positionEnd?.let { positionPx(YogaEdge.END, it.toPixels()) }
    style.positionBottom?.let { positionPx(YogaEdge.BOTTOM, it.toPixels()) }
    style.positionType?.let { positionType(it) }

    style.background?.let { background(it) }
    style.foreground?.let { foreground(it) }
    if (style.wrapInView == true) {
      wrapInView()
    }
  }
}

fun size(size: Dimen) = Style(width = size, height = size)

fun Style.size(size: Dimen) = this + com.facebook.litho.size(size)

fun size(width: Dimen? = null, height: Dimen? = null) = Style(width, height)

fun Style.size(width: Dimen? = null, height: Dimen? = null) =
    this + com.facebook.litho.size(width, height)

fun width(minWidth: Dimen? = null, maxWidth: Dimen? = null) =
    Style(minWidth = minWidth, maxWidth = maxWidth)

fun Style.width(minWidth: Dimen? = null, maxWidth: Dimen? = null) =
    this + com.facebook.litho.width(minWidth, maxWidth)

fun height(minHeight: Dimen? = null, maxHeight: Dimen? = null) =
    Style(minHeight = minHeight, maxHeight = maxHeight)

fun Style.height(minHeight: Dimen? = null, maxHeight: Dimen? = null) =
    this + com.facebook.litho.height(minHeight, maxHeight)

fun flex(grow: Float? = null, shrink: Float? = null, basis: Dimen? = null) =
    Style(flexGrow = grow, flexShrink = shrink, flexBasis = basis)

fun Style.flex(grow: Float? = null, shrink: Float? = null, basis: Dimen? = null) =
    this + com.facebook.litho.flex(grow, shrink, basis)

fun aspectRatio(aspectRatio: Float) = Style(aspectRatio = aspectRatio)

fun Style.aspectRatio(aspectRatio: Float) = this + com.facebook.litho.aspectRatio(aspectRatio)

fun padding(all: Dimen) = Style(paddingAll = all)

fun Style.padding(all: Dimen) = this + com.facebook.litho.padding(all)

fun padding(horizontal: Dimen? = null, vertical: Dimen? = null) =
    Style(
        paddingStart = horizontal,
        paddingTop = vertical,
        paddingEnd = horizontal,
        paddingBottom = vertical)

fun Style.padding(horizontal: Dimen? = null, vertical: Dimen? = null) =
    this + com.facebook.litho.padding(horizontal, vertical)

fun padding(start: Dimen? = null, top: Dimen? = null, end: Dimen? = null, bottom: Dimen? = null) =
    Style(paddingStart = start, paddingTop = top, paddingEnd = end, paddingBottom = bottom)

fun Style.padding(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) = this + com.facebook.litho.padding(start, top, end, bottom)

fun margin(all: Dimen) = Style(marginAll = all)

fun Style.margin(all: Dimen) = this + com.facebook.litho.margin(all)

fun margin(horizontal: Dimen? = null, vertical: Dimen? = null) =
    Style(
        marginStart = horizontal,
        marginTop = vertical,
        marginEnd = horizontal,
        marginBottom = vertical)

fun Style.margin(horizontal: Dimen? = null, vertical: Dimen? = null) =
    this + com.facebook.litho.margin(horizontal, vertical)

fun margin(start: Dimen? = null, top: Dimen? = null, end: Dimen? = null, bottom: Dimen? = null) =
    Style(marginStart = start, marginTop = top, marginEnd = end, marginBottom = bottom)

fun Style.margin(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) = this + com.facebook.litho.margin(start, top, end, bottom)

fun position(start: Dimen? = null, top: Dimen? = null, end: Dimen? = null, bottom: Dimen? = null) =
    Style(
        positionStart = start,
        positionTop = top,
        positionEnd = end,
        positionBottom = bottom,
        positionType = YogaPositionType.ABSOLUTE)

fun Style.position(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) = this + com.facebook.litho.position(start, top, end, bottom)

fun positionRelative(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) =
    Style(
        positionStart = start,
        positionTop = top,
        positionEnd = end,
        positionBottom = bottom,
        positionType = YogaPositionType.RELATIVE)

fun Style.positionRelative(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) = this + com.facebook.litho.positionRelative(start, top, end, bottom)

fun background(background: Drawable?) = Style(background = background)

fun Style.background(background: Drawable?) = this + com.facebook.litho.background(background)

fun foreground(foreground: Drawable?) = Style(foreground = foreground)

fun Style.foreground(foreground: Drawable?) = this + com.facebook.litho.foreground(foreground)

fun alignSelf(align: YogaAlign) = Style(alignSelf = align)

fun Style.alignSelf(align: YogaAlign) = this + com.facebook.litho.alignSelf(align)

fun wrapInView() = Style(wrapInView = true)

fun Style.wrapInView() = this + com.facebook.litho.wrapInView()
