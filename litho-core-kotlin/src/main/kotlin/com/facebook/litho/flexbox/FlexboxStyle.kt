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

package com.facebook.litho.flexbox

import com.facebook.litho.Component
import com.facebook.litho.Dimen
import com.facebook.litho.ResourceResolver
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.exhaustive
import com.facebook.litho.getCommonPropsHolder
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

/** Enums for [FlexboxDimenStyleItem]. */
private enum class FlexboxDimenField {
  WIDTH,
  HEIGHT,
  MIN_WIDTH,
  MAX_WIDTH,
  MIN_HEIGHT,
  MAX_HEIGHT,
  FLEX_BASIS,
  PADDING_START,
  PADDING_TOP,
  PADDING_END,
  PADDING_BOTTOM,
  PADDING_HORIZONTAL,
  PADDING_VERTICAL,
  PADDING_ALL,
  MARGIN_START,
  MARGIN_TOP,
  MARGIN_END,
  MARGIN_BOTTOM,
  MARGIN_HORIZONTAL,
  MARGIN_VERTICAL,
  MARGIN_ALL,
  POSITION_START,
  POSITION_TOP,
  POSITION_END,
  POSITION_BOTTOM,
}

/** Enums for [FlexboxFloatStyleItem]. */
private enum class FlexboxFloatField {
  WIDTH_PERCENT,
  HEIGHT_PERCENT,
  MIN_WIDTH_PERCENT,
  MAX_WIDTH_PERCENT,
  MIN_HEIGHT_PERCENT,
  MAX_HEIGHT_PERCENT,
  FLEX,
  FLEX_GROW,
  FLEX_SHRINK,
  ASPECT_RATIO,
}

/** Enums for [FlexboxObjectStyleItem]. */
private enum class FlexboxObjectField {
  ALIGN_SELF,
  POSITION_TYPE,
}

/** Common style item for all dimen styles. See note on [FlexboxDimenField] about this pattern. */
private class FlexboxDimenStyleItem(val field: FlexboxDimenField, val value: Dimen) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    val pixelValue = value.toPixels(resourceResolver)
    when (field) {
      FlexboxDimenField.WIDTH -> commonProps.widthPx(if (value == Dimen.Hairline) 1 else pixelValue)
      FlexboxDimenField.HEIGHT ->
          commonProps.heightPx(if (value == Dimen.Hairline) 1 else pixelValue)
      FlexboxDimenField.MIN_WIDTH -> commonProps.minWidthPx(pixelValue)
      FlexboxDimenField.MAX_WIDTH -> commonProps.maxWidthPx(pixelValue)
      FlexboxDimenField.MIN_HEIGHT -> commonProps.minHeightPx(pixelValue)
      FlexboxDimenField.MAX_HEIGHT -> commonProps.maxHeightPx(pixelValue)
      FlexboxDimenField.FLEX_BASIS -> commonProps.flexBasisPx(pixelValue)
      FlexboxDimenField.PADDING_START -> commonProps.paddingPx(YogaEdge.START, pixelValue)
      FlexboxDimenField.PADDING_TOP -> commonProps.paddingPx(YogaEdge.TOP, pixelValue)
      FlexboxDimenField.PADDING_END -> commonProps.paddingPx(YogaEdge.END, pixelValue)
      FlexboxDimenField.PADDING_BOTTOM -> commonProps.paddingPx(YogaEdge.BOTTOM, pixelValue)
      FlexboxDimenField.PADDING_HORIZONTAL -> commonProps.paddingPx(YogaEdge.HORIZONTAL, pixelValue)
      FlexboxDimenField.PADDING_VERTICAL -> commonProps.paddingPx(YogaEdge.VERTICAL, pixelValue)
      FlexboxDimenField.PADDING_ALL -> commonProps.paddingPx(YogaEdge.ALL, pixelValue)
      FlexboxDimenField.MARGIN_START -> commonProps.marginPx(YogaEdge.START, pixelValue)
      FlexboxDimenField.MARGIN_TOP -> commonProps.marginPx(YogaEdge.TOP, pixelValue)
      FlexboxDimenField.MARGIN_END -> commonProps.marginPx(YogaEdge.END, pixelValue)
      FlexboxDimenField.MARGIN_BOTTOM -> commonProps.marginPx(YogaEdge.BOTTOM, pixelValue)
      FlexboxDimenField.MARGIN_HORIZONTAL -> commonProps.marginPx(YogaEdge.HORIZONTAL, pixelValue)
      FlexboxDimenField.MARGIN_VERTICAL -> commonProps.marginPx(YogaEdge.VERTICAL, pixelValue)
      FlexboxDimenField.MARGIN_ALL -> commonProps.marginPx(YogaEdge.ALL, pixelValue)
      FlexboxDimenField.POSITION_START -> commonProps.positionPx(YogaEdge.START, pixelValue)
      FlexboxDimenField.POSITION_END -> commonProps.positionPx(YogaEdge.END, pixelValue)
      FlexboxDimenField.POSITION_TOP -> commonProps.positionPx(YogaEdge.TOP, pixelValue)
      FlexboxDimenField.POSITION_BOTTOM -> commonProps.positionPx(YogaEdge.BOTTOM, pixelValue)
    }.exhaustive
  }
}

/** Common style item for all float styles. See note on [FlexboxDimenField] about this pattern. */
private class FloatStyleItem(val field: FlexboxFloatField, val value: Float) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      FlexboxFloatField.WIDTH_PERCENT -> commonProps.widthPercent(value)
      FlexboxFloatField.HEIGHT_PERCENT -> commonProps.heightPercent(value)
      FlexboxFloatField.MIN_WIDTH_PERCENT -> commonProps.minWidthPercent(value)
      FlexboxFloatField.MAX_WIDTH_PERCENT -> commonProps.maxWidthPercent(value)
      FlexboxFloatField.MIN_HEIGHT_PERCENT -> commonProps.minHeightPercent(value)
      FlexboxFloatField.MAX_HEIGHT_PERCENT -> commonProps.maxHeightPercent(value)
      FlexboxFloatField.FLEX -> commonProps.flex(value)
      FlexboxFloatField.FLEX_GROW -> commonProps.flexGrow(value)
      FlexboxFloatField.FLEX_SHRINK -> commonProps.flexShrink(value)
      FlexboxFloatField.ASPECT_RATIO -> commonProps.aspectRatio(value)
    }.exhaustive
  }
}

/** Common style item for all object styles. See note on [FlexboxDimenField] about this pattern. */
private class FlexboxObjectStyleItem(val field: FlexboxObjectField, val value: Any?) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      FlexboxObjectField.ALIGN_SELF -> value?.let { commonProps.alignSelf(it as YogaAlign) }
      FlexboxObjectField.POSITION_TYPE ->
          value?.let { commonProps.positionType(it as YogaPositionType) }
    }.exhaustive
  }
}

fun Style.width(width: Dimen) = this + FlexboxDimenStyleItem(FlexboxDimenField.WIDTH, width)

fun Style.height(height: Dimen) = this + FlexboxDimenStyleItem(FlexboxDimenField.HEIGHT, height)

fun Style.minWidth(minWidth: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MIN_WIDTH, minWidth)

fun Style.maxWidth(maxWidth: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MAX_WIDTH, maxWidth)

fun Style.minHeight(minHeight: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MIN_HEIGHT, minHeight)

fun Style.maxHeight(maxHeight: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MAX_HEIGHT, maxHeight)

fun Style.flex(grow: Float? = null, shrink: Float? = null, basis: Dimen? = null) =
    this +
        grow?.let { FloatStyleItem(FlexboxFloatField.FLEX_GROW, it) } +
        shrink?.let { FloatStyleItem(FlexboxFloatField.FLEX_SHRINK, it) } +
        basis?.let { FlexboxDimenStyleItem(FlexboxDimenField.FLEX_BASIS, it) }

fun Style.alignSelf(align: YogaAlign) =
    this + FlexboxObjectStyleItem(FlexboxObjectField.ALIGN_SELF, align)

fun Style.aspectRatio(aspectRatio: Float) =
    this + FloatStyleItem(FlexboxFloatField.ASPECT_RATIO, aspectRatio)

fun Style.padding(
    all: Dimen? = null,
    horizontal: Dimen? = null,
    vertical: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) =
    this +
        all?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_ALL, it) } +
        horizontal?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_HORIZONTAL, it) } +
        vertical?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_VERTICAL, it) } +
        start?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_START, it) } +
        top?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_TOP, it) } +
        end?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_END, it) } +
        bottom?.let { FlexboxDimenStyleItem(FlexboxDimenField.PADDING_BOTTOM, it) }

fun Style.margin(
    all: Dimen? = null,
    horizontal: Dimen? = null,
    vertical: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) =
    this +
        all?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_ALL, it) } +
        horizontal?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_HORIZONTAL, it) } +
        vertical?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_VERTICAL, it) } +
        start?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_START, it) } +
        top?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_TOP, it) } +
        end?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_END, it) } +
        bottom?.let { FlexboxDimenStyleItem(FlexboxDimenField.MARGIN_BOTTOM, it) }

fun Style.position(
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null
) =
    this +
        start?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_START, it) } +
        top?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_TOP, it) } +
        end?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_END, it) } +
        bottom?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_BOTTOM, it) }

fun Style.positionType(positionType: YogaPositionType) =
    this + FlexboxObjectStyleItem(FlexboxObjectField.POSITION_TYPE, positionType)
