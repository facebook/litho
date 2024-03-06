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

package com.facebook.litho

import com.facebook.flexlayout.styles.AlignContent
import com.facebook.flexlayout.styles.AlignItems
import com.facebook.flexlayout.styles.AlignSelf
import com.facebook.flexlayout.styles.Direction
import com.facebook.flexlayout.styles.Edge
import com.facebook.flexlayout.styles.FlexDirection
import com.facebook.flexlayout.styles.Justify
import com.facebook.flexlayout.styles.PositionType
import com.facebook.flexlayout.styles.Wrap
import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap

/**
 * This class is a wrapper around the FlexLayout, which is basically an adapter that bridges the gap
 * between Yoga and FlexLayout.
 */
internal object LithoFlexLayoutFunction {

  fun YogaFlexDirection.toFlexDirection(): FlexDirection {
    return when (this) {
      YogaFlexDirection.COLUMN -> FlexDirection.COLUMN
      YogaFlexDirection.COLUMN_REVERSE -> FlexDirection.COLUMN_REVERSE
      YogaFlexDirection.ROW -> FlexDirection.ROW
      YogaFlexDirection.ROW_REVERSE -> FlexDirection.ROW_REVERSE
      else -> throw IllegalArgumentException("Unknown YogaFlexDirection: $this")
    }
  }

  fun YogaWrap.toFlexWrap(): Wrap {
    return when (this) {
      YogaWrap.NO_WRAP -> Wrap.NO_WRAP
      YogaWrap.WRAP -> Wrap.WRAP
      YogaWrap.WRAP_REVERSE -> Wrap.WRAP_REVERSE
      else -> throw IllegalArgumentException("Unknown YogaWrap: $this")
    }
  }

  fun YogaJustify.toJustifyContent(): Justify {
    return when (this) {
      YogaJustify.FLEX_START -> Justify.FLEX_START
      YogaJustify.FLEX_END -> Justify.FLEX_END
      YogaJustify.CENTER -> Justify.CENTER
      YogaJustify.SPACE_BETWEEN -> Justify.SPACE_BETWEEN
      YogaJustify.SPACE_EVENLY -> Justify.SPACE_EVENLY
      YogaJustify.SPACE_AROUND -> Justify.SPACE_AROUND
      else -> throw IllegalArgumentException("Unknown YogaJustify: $this")
    }
  }

  fun YogaAlign.toAlignItems(): AlignItems {
    return when (this) {
      YogaAlign.FLEX_START -> AlignItems.FLEX_START
      // Setting either Auto, SpaceBetween or SpaceAround for alignItems is not valid according to
      // W3C Spec: http://developer.mozilla.org/en-US/docs/Web/CSS/align-items#values
      // The existing behaviour in Yoga 1 is to treat them as FlexEnd (see also parseFlexWrap()).
      YogaAlign.AUTO,
      YogaAlign.SPACE_BETWEEN,
      YogaAlign.SPACE_AROUND,
      YogaAlign.FLEX_END -> AlignItems.FLEX_END
      YogaAlign.CENTER -> AlignItems.CENTER
      YogaAlign.BASELINE -> AlignItems.BASELINE
      YogaAlign.STRETCH -> AlignItems.STRETCH
      else -> AlignItems.STRETCH
    }
  }

  fun YogaAlign.toAlignSelf(): AlignSelf {
    return when (this) {
      YogaAlign.FLEX_START -> AlignSelf.FLEX_START
      YogaAlign.FLEX_END -> AlignSelf.FLEX_END
      YogaAlign.CENTER -> AlignSelf.CENTER
      YogaAlign.BASELINE -> AlignSelf.BASELINE
      YogaAlign.STRETCH -> AlignSelf.STRETCH
      // Setting either SpaceBetween or SpaceAround for alignSelf is not valid according to
      // W3C Spec: https://developer.mozilla.org/en-US/docs/Web/CSS/align-self#values
      // The existing behaviour in Yoga 1 is to treat them as Auto.
      YogaAlign.AUTO,
      YogaAlign.SPACE_BETWEEN,
      YogaAlign.SPACE_AROUND -> AlignSelf.AUTO
      else -> AlignSelf.AUTO
    }
  }

  fun YogaAlign.toAlignContent(): AlignContent {
    return when (this) {
      YogaAlign.FLEX_END -> AlignContent.FLEX_END
      YogaAlign.CENTER -> AlignContent.CENTER
      YogaAlign.SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN
      YogaAlign.SPACE_AROUND -> AlignContent.SPACE_AROUND
      YogaAlign.BASELINE -> AlignContent.BASELINE
      YogaAlign.STRETCH -> AlignContent.STRETCH
      // Setting Auto for alignContent is not valid according to W3C Spec:
      // http://developer.mozilla.org/en-US/docs/Web/CSS/align-content#values
      // The existing behaviour in Yoga 1 is to treat it as FlexStart
      YogaAlign.FLEX_START,
      YogaAlign.AUTO -> AlignContent.FLEX_START
      else -> AlignContent.FLEX_START
    }
  }

  fun YogaPositionType.toPositionType(): PositionType {
    return when (this) {
      YogaPositionType.ABSOLUTE -> PositionType.ABSOLUTE
      YogaPositionType.RELATIVE -> PositionType.RELATIVE
      else -> PositionType.RELATIVE
    }
  }

  fun LayoutDirection.toFlexLayoutDirection(): Direction {
    return when (this) {
      LayoutDirection.LTR -> Direction.LTR
      LayoutDirection.RTL -> Direction.RTL
      LayoutDirection.INHERIT -> Direction.INHERIT
      else -> throw IllegalArgumentException("Unknown LayoutDirection $this")
    }
  }

  /** Util function to apply props related to Edge. */
  fun <T> Array<T?>.applyEdgeProps(block: (Edge, T) -> Unit) {
    for (index in 0 until Edge.values().size) {
      this[index]?.let { value -> block.invoke(Edge.fromInt(index), value) }
    }
  }

  /** Since FlexLayout doesn't support HORIZONTAL, VERTICAL and ALL, we have to translate them. */
  fun YogaEdge.toEdge(isLTR: Boolean): List<Edge> =
      when (this) {
        YogaEdge.LEFT -> listOf(Edge.LEFT)
        YogaEdge.TOP -> listOf(Edge.TOP)
        YogaEdge.RIGHT -> listOf(Edge.RIGHT)
        YogaEdge.BOTTOM -> listOf(Edge.BOTTOM)
        YogaEdge.START -> {
          val startEdge = if (isLTR) Edge.LEFT else Edge.RIGHT
          listOf(startEdge)
        }
        YogaEdge.END -> {
          val endEdge = if (isLTR) Edge.RIGHT else Edge.LEFT
          listOf(endEdge)
        }
        YogaEdge.HORIZONTAL -> listOf(Edge.LEFT, Edge.RIGHT)
        YogaEdge.VERTICAL -> listOf(Edge.TOP, Edge.BOTTOM)
        YogaEdge.ALL -> listOf(Edge.LEFT, Edge.RIGHT, Edge.TOP, Edge.BOTTOM)
      }
}
