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

package com.facebook.litho.core

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.rendercore.Dimen
import com.facebook.yoga.YogaEdge

/** Enums for [CoreDimenStyleItem]. */
@PublishedApi
internal enum class CoreDimenField : StyleItemField {
  WIDTH,
  HEIGHT,
  MIN_WIDTH,
  MAX_WIDTH,
  MIN_HEIGHT,
  MAX_HEIGHT,
  PADDING_START,
  PADDING_TOP,
  PADDING_END,
  PADDING_BOTTOM,
  PADDING_LEFT,
  PADDING_RIGHT,
  PADDING_HORIZONTAL,
  PADDING_VERTICAL,
  PADDING_ALL,
  MARGIN_START,
  MARGIN_TOP,
  MARGIN_END,
  MARGIN_BOTTOM,
  MARGIN_LEFT,
  MARGIN_RIGHT,
  MARGIN_HORIZONTAL,
  MARGIN_VERTICAL,
  MARGIN_ALL,
}

/** Enums for [CoreFloatStyleItem]. */
@PublishedApi
internal enum class CoreFloatField : StyleItemField {
  WIDTH_PERCENT,
  HEIGHT_PERCENT,
  MIN_WIDTH_PERCENT,
  MAX_WIDTH_PERCENT,
  MIN_HEIGHT_PERCENT,
  MAX_HEIGHT_PERCENT,
  MARGIN_ALL_PERCENT,
  MARGIN_START_PERCENT,
  MARGIN_TOP_PERCENT,
  MARGIN_END_PERCENT,
  MARGIN_BOTTOM_PERCENT,
  MARGIN_LEFT_PERCENT,
  MARGIN_RIGHT_PERCENT,
  MARGIN_HORIZONTAL_PERCENT,
  MARGIN_VERTICAL_PERCENT,
  PADDING_ALL_PERCENT,
  PADDING_START_PERCENT,
  PADDING_TOP_PERCENT,
  PADDING_END_PERCENT,
  PADDING_BOTTOM_PERCENT,
  PADDING_LEFT_PERCENT,
  PADDING_RIGHT_PERCENT,
  PADDING_HORIZONTAL_PERCENT,
  PADDING_VERTICAL_PERCENT,
}

/** Common style item for all core dimen styles. See note on [CoreDimenField] about this pattern. */
@PublishedApi
@DataClassGenerate
internal data class CoreDimenStyleItem(
    override val field: CoreDimenField,
    override val value: Dimen
) : StyleItem<Dimen> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    val pixelValue = value.toPixels(context.resourceResolver)
    when (field) {
      // TODO(t89044330): When yoga is decoupled from Litho, implement these more generically.
      CoreDimenField.WIDTH -> commonProps.widthPx(pixelValue)
      CoreDimenField.HEIGHT -> commonProps.heightPx(pixelValue)
      CoreDimenField.MIN_WIDTH -> commonProps.minWidthPx(pixelValue)
      CoreDimenField.MAX_WIDTH -> commonProps.maxWidthPx(pixelValue)
      CoreDimenField.MIN_HEIGHT -> commonProps.minHeightPx(pixelValue)
      CoreDimenField.MAX_HEIGHT -> commonProps.maxHeightPx(pixelValue)
      CoreDimenField.PADDING_START -> commonProps.paddingPx(YogaEdge.START, pixelValue)
      CoreDimenField.PADDING_TOP -> commonProps.paddingPx(YogaEdge.TOP, pixelValue)
      CoreDimenField.PADDING_END -> commonProps.paddingPx(YogaEdge.END, pixelValue)
      CoreDimenField.PADDING_BOTTOM -> commonProps.paddingPx(YogaEdge.BOTTOM, pixelValue)
      CoreDimenField.PADDING_LEFT -> commonProps.paddingPx(YogaEdge.LEFT, pixelValue)
      CoreDimenField.PADDING_RIGHT -> commonProps.paddingPx(YogaEdge.RIGHT, pixelValue)
      CoreDimenField.PADDING_HORIZONTAL -> commonProps.paddingPx(YogaEdge.HORIZONTAL, pixelValue)
      CoreDimenField.PADDING_VERTICAL -> commonProps.paddingPx(YogaEdge.VERTICAL, pixelValue)
      CoreDimenField.PADDING_ALL -> commonProps.paddingPx(YogaEdge.ALL, pixelValue)
      CoreDimenField.MARGIN_START -> commonProps.marginPx(YogaEdge.START, pixelValue)
      CoreDimenField.MARGIN_TOP -> commonProps.marginPx(YogaEdge.TOP, pixelValue)
      CoreDimenField.MARGIN_END -> commonProps.marginPx(YogaEdge.END, pixelValue)
      CoreDimenField.MARGIN_BOTTOM -> commonProps.marginPx(YogaEdge.BOTTOM, pixelValue)
      CoreDimenField.MARGIN_LEFT -> commonProps.marginPx(YogaEdge.LEFT, pixelValue)
      CoreDimenField.MARGIN_RIGHT -> commonProps.marginPx(YogaEdge.RIGHT, pixelValue)
      CoreDimenField.MARGIN_HORIZONTAL -> commonProps.marginPx(YogaEdge.HORIZONTAL, pixelValue)
      CoreDimenField.MARGIN_VERTICAL -> commonProps.marginPx(YogaEdge.VERTICAL, pixelValue)
      CoreDimenField.MARGIN_ALL -> commonProps.marginPx(YogaEdge.ALL, pixelValue)
    }
  }
}

/** Common style item for all core float styles. See note on [CoreDimenField] about this pattern. */
@PublishedApi
internal class CoreFloatStyleItem(override val field: CoreFloatField, override val value: Float) :
    StyleItem<Float> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      // TODO(t89044330): When yoga is decoupled from Litho, implement these more generically.
      CoreFloatField.WIDTH_PERCENT -> commonProps.widthPercent(value)
      CoreFloatField.HEIGHT_PERCENT -> commonProps.heightPercent(value)
      CoreFloatField.MIN_WIDTH_PERCENT -> commonProps.minWidthPercent(value)
      CoreFloatField.MAX_WIDTH_PERCENT -> commonProps.maxWidthPercent(value)
      CoreFloatField.MIN_HEIGHT_PERCENT -> commonProps.minHeightPercent(value)
      CoreFloatField.MAX_HEIGHT_PERCENT -> commonProps.maxHeightPercent(value)
      CoreFloatField.MARGIN_ALL_PERCENT -> commonProps.marginPercent(YogaEdge.ALL, value)
      CoreFloatField.MARGIN_START_PERCENT -> commonProps.marginPercent(YogaEdge.START, value)
      CoreFloatField.MARGIN_TOP_PERCENT -> commonProps.marginPercent(YogaEdge.TOP, value)
      CoreFloatField.MARGIN_END_PERCENT -> commonProps.marginPercent(YogaEdge.END, value)
      CoreFloatField.MARGIN_BOTTOM_PERCENT -> commonProps.marginPercent(YogaEdge.BOTTOM, value)
      CoreFloatField.MARGIN_LEFT_PERCENT -> commonProps.marginPercent(YogaEdge.LEFT, value)
      CoreFloatField.MARGIN_RIGHT_PERCENT -> commonProps.marginPercent(YogaEdge.RIGHT, value)
      CoreFloatField.MARGIN_HORIZONTAL_PERCENT ->
          commonProps.marginPercent(YogaEdge.HORIZONTAL, value)
      CoreFloatField.MARGIN_VERTICAL_PERCENT -> commonProps.marginPercent(YogaEdge.VERTICAL, value)
      CoreFloatField.PADDING_ALL_PERCENT -> commonProps.paddingPercent(YogaEdge.ALL, value)
      CoreFloatField.PADDING_START_PERCENT -> commonProps.paddingPercent(YogaEdge.START, value)
      CoreFloatField.PADDING_TOP_PERCENT -> commonProps.paddingPercent(YogaEdge.TOP, value)
      CoreFloatField.PADDING_END_PERCENT -> commonProps.paddingPercent(YogaEdge.END, value)
      CoreFloatField.PADDING_BOTTOM_PERCENT -> commonProps.paddingPercent(YogaEdge.BOTTOM, value)
      CoreFloatField.PADDING_LEFT_PERCENT -> commonProps.paddingPercent(YogaEdge.LEFT, value)
      CoreFloatField.PADDING_RIGHT_PERCENT -> commonProps.paddingPercent(YogaEdge.RIGHT, value)
      CoreFloatField.PADDING_HORIZONTAL_PERCENT ->
          commonProps.paddingPercent(YogaEdge.HORIZONTAL, value)
      CoreFloatField.PADDING_VERTICAL_PERCENT ->
          commonProps.paddingPercent(YogaEdge.VERTICAL, value)
    }
  }
}

/** Sets a specific preferred width for this component when its parent lays it out. */
inline fun Style.width(width: Dimen): Style = this + CoreDimenStyleItem(CoreDimenField.WIDTH, width)

/** Sets a specific preferred height for this component when its parent lays it out. */
inline fun Style.height(height: Dimen): Style =
    this + CoreDimenStyleItem(CoreDimenField.HEIGHT, height)

/** Sets a specific preferred percent width for this component when its parent lays it out. */
inline fun Style.widthPercent(widthPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.WIDTH_PERCENT, widthPercent)

/** Sets a specific preferred percent height for this component when its parent lays it out. */
inline fun Style.heightPercent(heightPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.HEIGHT_PERCENT, heightPercent)

/** Sets a preferred minimum width for this component when its parent lays it out. */
inline fun Style.minWidth(minWidth: Dimen): Style =
    this + CoreDimenStyleItem(CoreDimenField.MIN_WIDTH, minWidth)

/** Sets a preferred maximum width for this component when its parent lays it out. */
inline fun Style.maxWidth(maxWidth: Dimen): Style =
    this + CoreDimenStyleItem(CoreDimenField.MAX_WIDTH, maxWidth)

/** Sets a preferred minimum height for this component when its parent lays it out. */
inline fun Style.minHeight(minHeight: Dimen): Style =
    this + CoreDimenStyleItem(CoreDimenField.MIN_HEIGHT, minHeight)

/** Sets a preferred maximum height for this component when its parent lays it out. */
inline fun Style.maxHeight(maxHeight: Dimen): Style =
    this + CoreDimenStyleItem(CoreDimenField.MAX_HEIGHT, maxHeight)

/** Sets a specific minimum percent width for this component when its parent lays it out. */
inline fun Style.minWidthPercent(minWidthPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.MIN_WIDTH_PERCENT, minWidthPercent)

/** Sets a specific maximum percent width for this component when its parent lays it out. */
inline fun Style.maxWidthPercent(maxWidthPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.MAX_WIDTH_PERCENT, maxWidthPercent)

/** Sets a specific minimum percent height for this component when its parent lays it out. */
inline fun Style.minHeightPercent(minHeightPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.MIN_HEIGHT_PERCENT, minHeightPercent)

/** Sets a specific maximum percent height for this component when its parent lays it out. */
inline fun Style.maxHeightPercent(maxHeightPercent: Float): Style =
    this + CoreFloatStyleItem(CoreFloatField.MAX_HEIGHT_PERCENT, maxHeightPercent)

/** Defines padding on the component on a per-edge basis. */
inline fun Style.padding(
    all: Dimen? = null,
    horizontal: Dimen? = null,
    vertical: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null,
    left: Dimen? = null,
    right: Dimen? = null,
): Style =
    this +
        all?.let { CoreDimenStyleItem(CoreDimenField.PADDING_ALL, it) } +
        horizontal?.let { CoreDimenStyleItem(CoreDimenField.PADDING_HORIZONTAL, it) } +
        vertical?.let { CoreDimenStyleItem(CoreDimenField.PADDING_VERTICAL, it) } +
        start?.let { CoreDimenStyleItem(CoreDimenField.PADDING_START, it) } +
        top?.let { CoreDimenStyleItem(CoreDimenField.PADDING_TOP, it) } +
        end?.let { CoreDimenStyleItem(CoreDimenField.PADDING_END, it) } +
        bottom?.let { CoreDimenStyleItem(CoreDimenField.PADDING_BOTTOM, it) } +
        left?.let { CoreDimenStyleItem(CoreDimenField.PADDING_LEFT, it) } +
        right?.let { CoreDimenStyleItem(CoreDimenField.PADDING_RIGHT, it) }

/**
 * Defines padding on the component on a per-edge basis, with a percent value of container's size.
 * See
 * [https://www.yogalayout.dev/docs/styling/margin-padding-border](https://www.yogalayout.dev/docs/styling/margin-padding-border)
 * for more information.
 */
inline fun Style.paddingPercent(
    all: Float? = null,
    horizontal: Float? = null,
    vertical: Float? = null,
    start: Float? = null,
    top: Float? = null,
    end: Float? = null,
    bottom: Float? = null,
    left: Float? = null,
    right: Float? = null,
): Style =
    this +
        all?.let { CoreFloatStyleItem(CoreFloatField.PADDING_ALL_PERCENT, it) } +
        horizontal?.let { CoreFloatStyleItem(CoreFloatField.PADDING_HORIZONTAL_PERCENT, it) } +
        vertical?.let { CoreFloatStyleItem(CoreFloatField.PADDING_VERTICAL_PERCENT, it) } +
        start?.let { CoreFloatStyleItem(CoreFloatField.PADDING_START_PERCENT, it) } +
        top?.let { CoreFloatStyleItem(CoreFloatField.PADDING_TOP_PERCENT, it) } +
        end?.let { CoreFloatStyleItem(CoreFloatField.PADDING_END_PERCENT, it) } +
        bottom?.let { CoreFloatStyleItem(CoreFloatField.PADDING_BOTTOM_PERCENT, it) } +
        left?.let { CoreFloatStyleItem(CoreFloatField.PADDING_LEFT_PERCENT, it) } +
        right?.let { CoreFloatStyleItem(CoreFloatField.PADDING_RIGHT_PERCENT, it) }

/** Defines margin around the component on a per-edge basis. */
inline fun Style.margin(
    all: Dimen? = null,
    horizontal: Dimen? = null,
    vertical: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null,
    left: Dimen? = null,
    right: Dimen? = null,
): Style =
    this +
        all?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_ALL, it) } +
        horizontal?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_HORIZONTAL, it) } +
        vertical?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_VERTICAL, it) } +
        start?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_START, it) } +
        top?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_TOP, it) } +
        end?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_END, it) } +
        bottom?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_BOTTOM, it) } +
        left?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_LEFT, it) } +
        right?.let { CoreDimenStyleItem(CoreDimenField.MARGIN_RIGHT, it) }

/**
 * Defines margin around the component on a per-edge basis, with a percent value of container's
 * size. See
 * [https://www.yogalayout.dev/docs/styling/margin-padding-border](https://www.yogalayout.dev/docs/styling/margin-padding-border)
 * for more information.
 */
inline fun Style.marginPercent(
    all: Float? = null,
    horizontal: Float? = null,
    vertical: Float? = null,
    start: Float? = null,
    top: Float? = null,
    end: Float? = null,
    bottom: Float? = null,
    left: Float? = null,
    right: Float? = null,
): Style =
    this +
        all?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_ALL_PERCENT, it) } +
        horizontal?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_HORIZONTAL_PERCENT, it) } +
        vertical?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_VERTICAL_PERCENT, it) } +
        start?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_START_PERCENT, it) } +
        top?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_TOP_PERCENT, it) } +
        end?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_END_PERCENT, it) } +
        bottom?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_BOTTOM_PERCENT, it) } +
        left?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_LEFT_PERCENT, it) } +
        right?.let { CoreFloatStyleItem(CoreFloatField.MARGIN_RIGHT_PERCENT, it) }
