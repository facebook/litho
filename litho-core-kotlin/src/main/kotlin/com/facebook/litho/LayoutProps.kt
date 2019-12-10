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
inline fun <C : Component.Builder<C>> DslScope.Padding(
    all: Dp,
    content: DslScope.() -> C
): C =
    content().paddingDip(YogaEdge.ALL, all.value)

/**
 * Builder for setting [horizontal] and [vertical] paddings to the child component.
 */
inline fun <C : Component.Builder<C>> DslScope.Padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .paddingDip(YogaEdge.HORIZONTAL, horizontal.value)
        .paddingDip(YogaEdge.VERTICAL, vertical.value)

/**
 * Builder for setting granular [left], [top], [right] and [bottom] paddings to the child component.
 */
inline fun <C : Component.Builder<C>> DslScope.Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .paddingDip(YogaEdge.LEFT, left.value)
        .paddingDip(YogaEdge.TOP, top.value)
        .paddingDip(YogaEdge.RIGHT, right.value)
        .paddingDip(YogaEdge.BOTTOM, bottom.value)

/**
 * Builder for setting the same [all] margin for each edge of the child component.
 */
inline fun <C : Component.Builder<C>> DslScope.Margin(
    all: Dp,
    content: DslScope.() -> C
): C =
    content().marginDip(YogaEdge.ALL, all.value)

/**
 * Builder for setting [horizontal] and [vertical] margins for the child component.
 */
inline fun <C : Component.Builder<C>> DslScope.Margin(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .marginDip(YogaEdge.HORIZONTAL, horizontal.value)
        .marginDip(YogaEdge.VERTICAL, vertical.value)

/**
 * Builder for setting granular [left], [top], [right] and [bottom] paddings to the child component.
 */
inline fun <C : Component.Builder<C>> DslScope.Margin(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .marginDip(YogaEdge.LEFT, left.value)
        .marginDip(YogaEdge.TOP, top.value)
        .marginDip(YogaEdge.RIGHT, right.value)
        .marginDip(YogaEdge.BOTTOM, bottom.value)

/**
 * Builder for positioning a child component absolutely within its parent's edges, independent of
 * its siblings. [left], [top], [right], [bottom] specify the offset of the child's respective side
 * from the same side of the parent.
 */
inline fun <C : Component.Builder<C>> DslScope.Position(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
    content: DslScope.() -> C
): C =
    content()
        .positionType(YogaPositionType.ABSOLUTE)
        .apply {
          left?.let { positionDip(YogaEdge.LEFT, it.value) }
          top?.let { positionDip(YogaEdge.TOP, it.value) }
          right?.let { positionDip(YogaEdge.RIGHT, it.value) }
          bottom?.let { positionDip(YogaEdge.BOTTOM, it.value) }
        }

/**
 * Builder for setting a specific [width] and [height] for a child component.
 */
inline fun <C : Component.Builder<C>> DslScope.FixedSize(
    width: Dp? = null,
    height: Dp? = null,
    content: DslScope.() -> C
): C =
    content()
        .apply {
          width?.let { widthDip(it.value) }
          height?.let { heightDip(it.value) }
        }

inline fun <C : Component.Builder<C>> DslScope.Flex(
    grow: Float = 0f,
    shrink: Float = 1f,
    basis: Dp? = null,
    content: DslScope.() -> C
): C =
    content()
        .flexGrow(grow)
        .flexShrink(shrink)
        .apply {
          basis?.let { flexBasisDip(it.value) }
        }

inline fun <C : Component.Builder<C>> DslScope.Alignment(
    align: YogaAlign,
    content: DslScope.() -> C
): C =
    content().alignSelf(align)
