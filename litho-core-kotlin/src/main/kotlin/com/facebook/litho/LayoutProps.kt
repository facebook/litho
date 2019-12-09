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

import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType

inline fun <C : Component.Builder<C>> DslScope.Padding(
    all: Dp,
    content: DslScope.() -> C
): C =
    content().paddingDip(YogaEdge.ALL, all.dp)

inline fun <C : Component.Builder<C>> DslScope.Padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .paddingDip(YogaEdge.HORIZONTAL, horizontal.dp)
        .paddingDip(YogaEdge.VERTICAL, vertical.dp)

inline fun <C : Component.Builder<C>> DslScope.Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    content: DslScope.() -> C
): C =
    content()
        .paddingDip(YogaEdge.LEFT, left.dp)
        .paddingDip(YogaEdge.TOP, top.dp)
        .paddingDip(YogaEdge.RIGHT, right.dp)
        .paddingDip(YogaEdge.BOTTOM, bottom.dp)

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
          left?.let { positionDip(YogaEdge.LEFT, it.dp) }
          top?.let { positionDip(YogaEdge.TOP, it.dp) }
          right?.let { positionDip(YogaEdge.RIGHT, it.dp) }
          bottom?.let { positionDip(YogaEdge.BOTTOM, it.dp) }
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
          width?.let { widthDip(it.dp) }
          height?.let { heightDip(it.dp) }
        }
