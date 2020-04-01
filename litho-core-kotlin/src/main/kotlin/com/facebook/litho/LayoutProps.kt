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

@file:Suppress("FunctionName")

package com.facebook.litho

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

/**
 * Builder for setting the same [all] padding to each edge of the child component.
 */
inline fun DslScope.Padding(
    all: Dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.paddingPx(YogaEdge.ALL, all.toPx().value)
    }

/**
 * Builder for setting [horizontal] and [vertical] paddings to the child component.
 */
inline fun DslScope.Padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        paddingPx(YogaEdge.HORIZONTAL, horizontal.toPx().value)
        paddingPx(YogaEdge.VERTICAL, vertical.toPx().value)
      }
    }

/**
 * Builder for setting granular [left], [top], [right] and [bottom] paddings to the child component.
 */
inline fun DslScope.Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        paddingPx(YogaEdge.LEFT, left.toPx().value)
        paddingPx(YogaEdge.TOP, top.toPx().value)
        paddingPx(YogaEdge.RIGHT, right.toPx().value)
        paddingPx(YogaEdge.BOTTOM, bottom.toPx().value)
      }
    }

/**
 * Builder for setting the same [all] margin for each edge of the child component.
 */
inline fun DslScope.Margin(
    all: Dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.marginPx(YogaEdge.ALL, all.toPx().value)
    }

/**
 * Builder for setting [horizontal] and [vertical] margins for the child component.
 */
inline fun DslScope.Margin(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        marginPx(YogaEdge.HORIZONTAL, horizontal.toPx().value)
        marginPx(YogaEdge.VERTICAL, vertical.toPx().value)
      }
    }

/**
 * Builder for setting granular [left], [top], [right] and [bottom] paddings to the child component.
 */
inline fun DslScope.Margin(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        marginPx(YogaEdge.LEFT, left.toPx().value)
        marginPx(YogaEdge.TOP, top.toPx().value)
        marginPx(YogaEdge.RIGHT, right.toPx().value)
        marginPx(YogaEdge.BOTTOM, bottom.toPx().value)
      }
    }

/**
 * Builder for positioning a child component absolutely within its parent's edges, independent of
 * its siblings. [left], [top], [right], [bottom] specify the offset of the child's respective side
 * from the same side of the parent.
 */
inline fun DslScope.Position(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        positionType(YogaPositionType.ABSOLUTE)
        left?.let { positionPx(YogaEdge.LEFT, it.toPx().value) }
        top?.let { positionPx(YogaEdge.TOP, it.toPx().value) }
        right?.let { positionPx(YogaEdge.RIGHT, it.toPx().value) }
        bottom?.let { positionPx(YogaEdge.BOTTOM, it.toPx().value) }
      }
    }

/**
 * Builder for setting a specific [width] and [height] for a child component.
 */
inline fun DslScope.FixedSize(
    width: Dp? = null,
    height: Dp? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        width?.let { widthPx(if (it == Dp.Hairline) 1 else it.toPx().value) }
        height?.let { heightPx(if (it == Dp.Hairline) 1 else it.toPx().value) }
      }
    }

inline fun DslScope.Flex(
    grow: Float = 0f,
    shrink: Float = 1f,
    basis: Dp? = null,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.apply {
        flexGrow(grow)
        flexShrink(shrink)
        basis?.let { flexBasisPx(it.toPx().value) }
      }
    }

inline fun DslScope.Alignment(
    align: YogaAlign,
    content: DslScope.() -> Component
): Component =
    content().apply {
      getOrCreateCommonProps.alignSelf(align)
    }
