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

import com.facebook.litho.Border
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
  FLEX_BASIS,
  POSITION_ALL,
  POSITION_START,
  POSITION_TOP,
  POSITION_END,
  POSITION_BOTTOM,
  POSITION_LEFT,
  POSITION_RIGHT,
  POSITION_HORIZONTAL,
  POSITION_VERTICAL,
}

/** Enums for [FlexboxFloatStyleItem]. */
private enum class FlexboxFloatField {
  FLEX,
  FLEX_GROW,
  FLEX_SHRINK,
  ASPECT_RATIO,
}

/** Enums for [FlexboxObjectStyleItem]. */
private enum class FlexboxObjectField {
  ALIGN_SELF,
  BORDER,
  POSITION_TYPE,
}

/** Common style item for all dimen styles. See note on [FlexboxDimenField] about this pattern. */
private class FlexboxDimenStyleItem(val field: FlexboxDimenField, val value: Dimen) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    val pixelValue = value.toPixels(resourceResolver)
    when (field) {
      FlexboxDimenField.FLEX_BASIS -> commonProps.flexBasisPx(pixelValue)
      FlexboxDimenField.POSITION_ALL -> commonProps.positionPx(YogaEdge.ALL, pixelValue)
      FlexboxDimenField.POSITION_START -> commonProps.positionPx(YogaEdge.START, pixelValue)
      FlexboxDimenField.POSITION_END -> commonProps.positionPx(YogaEdge.END, pixelValue)
      FlexboxDimenField.POSITION_TOP -> commonProps.positionPx(YogaEdge.TOP, pixelValue)
      FlexboxDimenField.POSITION_BOTTOM -> commonProps.positionPx(YogaEdge.BOTTOM, pixelValue)
      FlexboxDimenField.POSITION_LEFT -> commonProps.positionPx(YogaEdge.LEFT, pixelValue)
      FlexboxDimenField.POSITION_RIGHT -> commonProps.positionPx(YogaEdge.RIGHT, pixelValue)
      FlexboxDimenField.POSITION_HORIZONTAL ->
          commonProps.positionPx(YogaEdge.HORIZONTAL, pixelValue)
      FlexboxDimenField.POSITION_VERTICAL -> commonProps.positionPx(YogaEdge.VERTICAL, pixelValue)
    }.exhaustive
  }
}

/** Common style item for all float styles. See note on [FlexboxDimenField] about this pattern. */
private class FloatStyleItem(val field: FlexboxFloatField, val value: Float) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
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
      FlexboxObjectField.BORDER -> commonProps.border(value as Border?)
      FlexboxObjectField.POSITION_TYPE ->
          value?.let { commonProps.positionType(it as YogaPositionType) }
    }.exhaustive
  }
}

/**
 * Flex allows you to define how this component should take up space within its parent. It's
 * comprised of the following properties:
 *
 * **flex-grow**: This component should take up remaining space in its parent. If multiple children
 * of the parent have a flex-grow set, the extra space is divided up based on proportions of
 * flex-grow values, i.e. a child with flex-grow of 2 will get twice as much of the space as its
 * sibling with flex-grow of 1.
 *
 * **flex-shrink**: This component should shrink if necessary. Similar to flex-grow, the value
 * determines the proportion of space *taken* from each child. Setting a flex-shink of 0 means the
 * child won't shrink.
 *
 * **flex-basis**: Defines the default size of the component before extra space is distributed. If
 * omitted, the measured size of the content (or the width/height styles) will be used instead.
 *
 * - See https://css-tricks.com/snippets/css/a-guide-to-flexbox/ for more documentation on flexbox
 * properties.
 * - See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
 *
 * Defaults: flex-grow = 0, flex-shrink = 1, flex-basis = null
 */
fun Style.flex(grow: Float? = null, shrink: Float? = null, basis: Dimen? = null) =
    this +
        grow?.let { FloatStyleItem(FlexboxFloatField.FLEX_GROW, it) } +
        shrink?.let { FloatStyleItem(FlexboxFloatField.FLEX_SHRINK, it) } +
        basis?.let { FlexboxDimenStyleItem(FlexboxDimenField.FLEX_BASIS, it) }

/**
 * Defines how a child should be aligned with a Row or Column, overriding the parent's align-items
 * property for this child.
 *
 * - See https://css-tricks.com/snippets/css/a-guide-to-flexbox/ for more documentation on flexbox
 * properties.
 * - See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
 */
fun Style.alignSelf(align: YogaAlign) =
    this + FlexboxObjectStyleItem(FlexboxObjectField.ALIGN_SELF, align)

/**
 * Defines an aspect ratio for this component, meaning the ratio of width to height. This means if
 * aspectRatio is set to 2 and width is calculated to be 50px, then height will be 100px.
 *
 * Note: This property is not part of the flexbox standard.
 */
fun Style.aspectRatio(aspectRatio: Float) =
    this + FloatStyleItem(FlexboxFloatField.ASPECT_RATIO, aspectRatio)

/**
 * Used in conjunction with [positionType] to define how a component should be positioned in its
 * parent.
 *
 * For positionType of ABSOLUTE: the values specified here will define how inset the child is from
 * the same edge on its parent. E.g. for `position(0.px, 0.px, 0.px, 0.px)`, it will be the full
 * size of the parent (no insets). For `position(0.px, 10.px, 0.px, 10.px)`, the child will be the
 * full width of the parent, but inset by 10px on the top and bottom.
 *
 * For positionType of RELATIVE: the values specified here will define how the child is positioned
 * relative to where that edge would have normally been positioned.
 *
 * See https://yogalayout.com/ for a web-based playground for trying out flexbox layouts.
 */
fun Style.position(
    all: Dimen? = null,
    start: Dimen? = null,
    top: Dimen? = null,
    end: Dimen? = null,
    bottom: Dimen? = null,
    left: Dimen? = null,
    right: Dimen? = null,
    vertical: Dimen? = null,
    horizontal: Dimen? = null
) =
    this +
        all?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_ALL, it) } +
        start?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_START, it) } +
        top?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_TOP, it) } +
        end?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_END, it) } +
        bottom?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_BOTTOM, it) } +
        left?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_LEFT, it) } +
        right?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_RIGHT, it) } +
        vertical?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_VERTICAL, it) } +
        horizontal?.let { FlexboxDimenStyleItem(FlexboxDimenField.POSITION_HORIZONTAL, it) }

/** See docs in [position]. */
fun Style.positionType(positionType: YogaPositionType) =
    this + FlexboxObjectStyleItem(FlexboxObjectField.POSITION_TYPE, positionType)

/**
 * Describes how a [Border] should be drawn around this component. Setting this property will cause
 * the Component to be represented as a View at mount time if it wasn't going to already.
 */
fun Style.border(border: Border) = this + FlexboxObjectStyleItem(FlexboxObjectField.BORDER, border)
