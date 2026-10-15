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

import com.facebook.litho.layout.ChildBox
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.rendercore.SizeConstraints

/**
 * A result's geometry when its `Layout` laid it out without a Yoga pass: its layout output reads it
 * in place of its Yoga node's, which no layout pass touched.
 */
internal class BoxGeometry(
    val direction: LayoutDirection,
    val padding: IntEdges,
    val border: IntEdges,
    val width: Int,
    val height: Int,
)

/** The size a child's border box settled on. */
internal data class MeasuredSize(val width: Int, val height: Int)

/**
 * Whether [node] is a leaf Litho measures itself — a measurable MountSpec or a Primitive, which the
 * resolver gives Litho's measure function — and so can be laid out without a Yoga pass. Not
 * deferred content, an absent child or a `Layout`.
 */
internal fun isLayoutLeaf(node: LithoNode): Boolean =
    node !is DeferredLithoNode &&
        node !is NullNode &&
        node.layoutFunction == null &&
        node.childCount == 0 &&
        node.yogaMeasureFunction is LithoYogaMeasureFunction

/**
 * The border box a leaf in [box] settles on, given [measure], which measures its content under a
 * width and a height spec. [measure] is not called when both axes are exact, as Yoga does not call
 * a leaf's measure function then.
 *
 * On an exact axis the size is the constraint; otherwise it is the content plus the padding and
 * border, within the box's minimum and maximum. Either way it is at least the padding and border.
 */
internal inline fun boxSize(
    box: ChildBox,
    measure: (Int, Int) -> Pair<Int, Int>,
): MeasuredSize {
  val room = box.borderBoxConstraints
  val edgesAcross = box.padding.horizontal + box.border.horizontal
  val edgesDown = box.padding.vertical + box.border.vertical
  if (room.hasExactWidth && room.hasExactHeight) {
    return MeasuredSize(
        width = maxOf(room.maxWidth, edgesAcross),
        height = maxOf(room.maxHeight, edgesDown),
    )
  }
  val (contentWidth, contentHeight) =
      measure(
          contentSpec(room.hasExactWidth, room.hasBoundedWidth, room.maxWidth, edgesAcross),
          contentSpec(room.hasExactHeight, room.hasBoundedHeight, room.maxHeight, edgesDown),
      )
  return MeasuredSize(
      width = axisSize(room.hasExactWidth, room.minWidth, room.maxWidth, contentWidth, edgesAcross),
      height =
          axisSize(room.hasExactHeight, room.minHeight, room.maxHeight, contentHeight, edgesDown),
  )
}

/** The measure spec for the content: the room inside the padding and border. */
internal fun contentSpec(exact: Boolean, bounded: Boolean, max: Int, edges: Int): Int =
    when {
      exact -> SizeSpec.makeSizeSpec(maxOf(0, max - edges), SizeSpec.EXACTLY)
      bounded -> SizeSpec.makeSizeSpec(maxOf(0, max - edges), SizeSpec.AT_MOST)
      else -> SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED)
    }

/** The border box along one axis, as Yoga bounds a measured leaf root. */
internal fun axisSize(exact: Boolean, min: Int, max: Int, content: Int, edges: Int): Int {
  if (exact) {
    return maxOf(max, edges)
  }
  var size = content + edges
  if (max != SizeConstraints.Infinity && size > max) {
    size = max
  } else if (min > 0 && size < min) {
    size = min
  }
  return maxOf(size, edges)
}
