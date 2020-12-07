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
 * Builder for positioning a child component absolutely within its parent's edges, independent of
 * its siblings. [left], [top], [right], [bottom] specify the offset of the child's respective side
 * from the same side of the parent.
 */
@Deprecated(message = "Use Style.position instead")
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

/** Builder for setting a specific [width] and [height] for a child component. */
@Deprecated(message = "Use Style.size instead")
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

@Deprecated(message = "Use Style.flex instead")
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

@Deprecated(message = "Use Style.alighSelf instead")
inline fun DslScope.Alignment(align: YogaAlign, content: DslScope.() -> Component): Component =
    content().apply { getOrCreateCommonProps.alignSelf(align) }
