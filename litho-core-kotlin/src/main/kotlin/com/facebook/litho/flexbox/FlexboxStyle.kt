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
import com.facebook.litho.ComponentContext
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

/** Sets a specific preferred width for this component when its parent lays it out. */
fun Style.width(width: Dimen) = this + FlexboxDimenStyleItem(FlexboxDimenField.WIDTH, width)

/** Sets a specific preferred height for this component when its parent lays it out. */
fun Style.height(height: Dimen) = this + FlexboxDimenStyleItem(FlexboxDimenField.HEIGHT, height)

/** Sets a specific preferred percent width for this component when its parent lays it out. */
fun Style.widthPercent(widthPercent: Float) =
    this + FloatStyleItem(FlexboxFloatField.WIDTH_PERCENT, widthPercent)

/** Sets a specific preferred percent height for this component when its parent lays it out. */
fun Style.heightPercent(heightPercent: Float) =
    this + FloatStyleItem(FlexboxFloatField.HEIGHT_PERCENT, heightPercent)

/** Sets a preferred minimum width for this component when its parent lays it out. */
fun Style.minWidth(minWidth: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MIN_WIDTH, minWidth)

/** Sets a preferred maximum width for this component when its parent lays it out. */
fun Style.maxWidth(maxWidth: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MAX_WIDTH, maxWidth)

/** Sets a preferred minimum height for this component when its parent lays it out. */
fun Style.minHeight(minHeight: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MIN_HEIGHT, minHeight)

/** Sets a preferred maximum height for this component when its parent lays it out. */
fun Style.maxHeight(maxHeight: Dimen) =
    this + FlexboxDimenStyleItem(FlexboxDimenField.MAX_HEIGHT, maxHeight)

/** Defines padding on the component on a per-edge basis. */
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

/** Defines margin around the component on a per-edge basis. */
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

/** See the [flexboxParams] inline function wrapper for documentation on this class. */
class FlexboxParams(
    private val flexGrow: Float? = null,
    private val flexShrink: Float? = null,
    private val flexBasis: Dimen? = null,
    private val alignSelf: YogaAlign? = null,
    private val positionType: YogaPositionType? = null,
    private val positionStart: Dimen? = null,
    private val positionTop: Dimen? = null,
    private val positionEnd: Dimen? = null,
    private val positionBottom: Dimen? = null,
    private val aspectRatio: Float? = null,
    private val component: Component
) {

  /**
   * Note: This will be rewritten when flexbox containers are no longer treated special in Litho.
   */
  internal fun getComponentWithAppliedParams(context: ComponentContext): Component {
    val commonProps = component.getCommonPropsHolder()
    flexGrow?.let { commonProps.flexGrow(it) }
    flexShrink?.let { commonProps.flexShrink(it) }
    flexBasis?.let { commonProps.flexBasisPx(it.toPixels(context.resourceResolver)) }
    alignSelf?.let { commonProps.alignSelf(it) }
    positionType?.let { commonProps.positionType(it) }
    positionStart?.let {
      commonProps.positionPx(YogaEdge.START, it.toPixels(context.resourceResolver))
    }
    positionTop?.let { commonProps.positionPx(YogaEdge.TOP, it.toPixels(context.resourceResolver)) }
    positionEnd?.let { commonProps.positionPx(YogaEdge.END, it.toPixels(context.resourceResolver)) }
    positionBottom?.let {
      commonProps.positionPx(YogaEdge.BOTTOM, it.toPixels(context.resourceResolver))
    }
    aspectRatio?.let { commonProps.aspectRatio(it) }
    return component
  }
}

/**
 * Returns a [FlexboxParams] wrapping a component.
 *
 * This allows you to specify flexbox properties which only apply when hosted in a flexbox
 * container, i.e. [Row] or [Column]. [Row] and [Column] accept children of the FlexboxParam type,
 * which will add the wrapped Component as a child with the given params.
 *
 * See https://css-tricks.com/snippets/css/a-guide-to-flexbox/ for more documentation on flexbox
 * properties.
 *
 * See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
 */
inline fun flexboxParams(

    /**
     * When positive, this component should take up remaining space in its parent. If multiple
     * children of the parent have a flex-grow set, the extra space is divided up based on
     * proportions of flex-grow values, i.e. a child with flex-grow of 2 will get twice as much of
     * the space as its sibling with flex-grow of 1.
     *
     * Default: 0
     */
    flexGrow: Float? = null,

    /**
     * When positive, this component should shrink if necessary. Similar to flex-grow, the value
     * determines the proportion of space *taken* from each child. Setting a flex-shink of 0 means
     * the child won't shrink.
     *
     * Default: 1
     */
    flexShrink: Float? = null,

    /**
     * Defines the default size of the component before extra space is distributed. If omitted, the
     * measured size of the content (or the width/height styles) will be used instead.
     */
    flexBasis: Dimen? = null,

    /**
     * Defines how a child should be aligned with a Row or Column, overriding the parent's
     * align-items property for this child.
     *
     * - See https://css-tricks.com/snippets/css/a-guide-to-flexbox/ for more documentation on
     * flexbox properties.
     * - See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
     *
     * Default: AUTO (defined by parent)
     */
    alignSelf: YogaAlign? = null,

    /**
     * Used in conjunction with [positionStart], [positionTop], etc to define how a component should
     * be positioned in its parent.
     *
     * For positionType of ABSOLUTE: the values specified here will define how inset the child is
     * from the same edge on its parent. E.g. for `position(0.px, 0.px, 0.px, 0.px)`, it will be the
     * full size of the parent (no insets). For `position(0.px, 10.px, 0.px, 10.px)`, the child will
     * be the full width of the parent, but inset by 10px on the top and bottom.
     *
     * For positionType of RELATIVE: the values specified here will define how the child is
     * positioned relative to where that edge would have normally been positioned.
     *
     * See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
     */
    positionType: YogaPositionType? = null,

    /** See docs in [positionType]. */
    positionStart: Dimen? = null,

    /** See docs in [positionType]. */
    positionTop: Dimen? = null,

    /** See docs in [positionType]. */
    positionEnd: Dimen? = null,

    /** See docs in [positionType]. */
    positionBottom: Dimen? = null,

    /**
     * Defines an aspect ratio for this component, meaning the ratio of width to height. This means
     * if aspectRatio is set to 2 and width is calculated to be 50px, then height will be 100px.
     *
     * Note: This property is not part of the flexbox standard.
     */
    aspectRatio: Float? = null,

    /** A lambda that returns the component that these params apply to. */
    component: () -> Component
) =
    FlexboxParams(
        flexGrow,
        flexShrink,
        flexBasis,
        alignSelf,
        positionType,
        positionStart,
        positionTop,
        positionEnd,
        positionBottom,
        aspectRatio,
        component())
