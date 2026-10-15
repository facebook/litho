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

import com.facebook.litho.layout.BoxEdge
import com.facebook.litho.layout.BoxLayoutProps
import com.facebook.litho.layout.BoxLength
import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaPositionType

/**
 * Records a `Layout` child's box props into [props], from the same replay of layout props any
 * node's writer receives, so a box prop is recorded however it was set. This is the one place a
 * [LayoutProps] call's Yoga types turn into [BoxLength] and [BoxEdge].
 *
 * Flexbox item props are dropped: a `Layout` places its children itself and reads none of them.
 *
 * A deferred child's padding and border belong to the content it renders, which applies them
 * itself, so with [recordsOwnEdges] false they are not recorded.
 */
internal class BoxLayoutPropsRecorder(
    private val props: BoxLayoutProps,
    private val recordsOwnEdges: Boolean,
) : LayoutProps {

  /** Whether a padding was recorded, which the node reports as it would for padding it carries. */
  var isPaddingSet: Boolean = false
    private set

  override fun widthPx(width: Int) {
    props.width = px(width)
  }

  override fun widthPercent(percent: Float) {
    props.width = BoxLength.Percent(percent)
  }

  override fun minWidthPx(minWidth: Int) {
    props.minWidth = px(minWidth)
  }

  override fun maxWidthPx(maxWidth: Int) {
    props.maxWidth = px(maxWidth)
  }

  override fun minWidthPercent(percent: Float) {
    props.minWidth = BoxLength.Percent(percent)
  }

  override fun maxWidthPercent(percent: Float) {
    props.maxWidth = BoxLength.Percent(percent)
  }

  override fun heightPx(height: Int) {
    props.height = px(height)
  }

  override fun heightPercent(percent: Float) {
    props.height = BoxLength.Percent(percent)
  }

  override fun minHeightPx(minHeight: Int) {
    props.minHeight = px(minHeight)
  }

  override fun maxHeightPx(maxHeight: Int) {
    props.maxHeight = px(maxHeight)
  }

  override fun minHeightPercent(percent: Float) {
    props.minHeight = BoxLength.Percent(percent)
  }

  override fun maxHeightPercent(percent: Float) {
    props.maxHeight = BoxLength.Percent(percent)
  }

  // `auto` is no size of its own: it clears an earlier size, as a later write would on a Yoga node.

  override fun widthAuto() {
    props.width = BoxLength.Unset
  }

  override fun heightAuto() {
    props.height = BoxLength.Unset
  }

  override fun aspectRatio(aspectRatio: Float) {
    props.aspectRatio = aspectRatio
  }

  override fun marginPx(edge: YogaEdge, margin: Int) {
    props.margin[edge.toBoxEdge()] = px(margin)
  }

  override fun marginPercent(edge: YogaEdge, percent: Float) {
    props.margin[edge.toBoxEdge()] = BoxLength.Percent(percent)
  }

  // A `Layout` places its children itself, so an `auto` margin has no free space to take up.
  override fun marginAuto(edge: YogaEdge) {
    props.margin[edge.toBoxEdge()] = BoxLength.Px(0f)
  }

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    if (recordsOwnEdges) {
      isPaddingSet = true
      props.padding[edge.toBoxEdge()] = px(padding)
    }
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    if (recordsOwnEdges) {
      isPaddingSet = true
      props.padding[edge.toBoxEdge()] = BoxLength.Percent(percent)
    }
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    if (recordsOwnEdges) {
      props.border[edge.toBoxEdge()] = BoxLength.Px(borderWidth)
    }
  }

  override fun alignSelf(alignSelf: YogaAlign) = Unit

  override fun flex(flex: Float) = Unit

  override fun flexGrow(flexGrow: Float) = Unit

  override fun flexShrink(flexShrink: Float) = Unit

  override fun flexBasisPx(flexBasis: Int) = Unit

  override fun flexBasisPercent(percent: Float) = Unit

  override fun positionType(positionType: YogaPositionType) = Unit

  override fun positionPx(edge: YogaEdge, position: Int) = Unit

  override fun positionPercent(edge: YogaEdge, percent: Float) = Unit

  // What shapes the child's content rather than its box: the writer for its node takes these.

  override fun layoutDirection(direction: LayoutDirection) = Unit

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) = Unit

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) = Unit

  override fun gap(gutter: YogaGutter, length: Int) = Unit

  private fun px(value: Int): BoxLength = BoxLength.Px(value.toFloat())
}

/**
 * The layout props of a `Layout`'s child, split between its box and its content: box props and
 * flexbox item props go to [box], which keeps them off the child's node, and what shapes the
 * child's own content goes on to [content], the writer for its node. A deferred child's padding and
 * border shape the content it renders, so they go on too.
 */
internal class ContainerChildLayoutProps(
    private val box: BoxLayoutPropsRecorder,
    private val content: LayoutProps,
    private val isDeferred: Boolean,
) : LayoutProps by box {

  /** Whether a padding was recorded, which the node reports as it would for padding it carries. */
  val isPaddingSet: Boolean
    get() = box.isPaddingSet

  override fun layoutDirection(direction: LayoutDirection) {
    content.layoutDirection(direction)
  }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    content.isReferenceBaseline(isReferenceBaseline)
  }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    content.useHeightAsBaseline(useHeightAsBaseline)
  }

  override fun gap(gutter: YogaGutter, length: Int) {
    content.gap(gutter, length)
  }

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    if (isDeferred) content.paddingPx(edge, padding) else box.paddingPx(edge, padding)
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    if (isDeferred) content.paddingPercent(edge, percent) else box.paddingPercent(edge, percent)
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    if (isDeferred) content.setBorderWidth(edge, borderWidth)
    else box.setBorderWidth(edge, borderWidth)
  }
}

/** The [BoxEdge] of the same name. */
internal fun YogaEdge.toBoxEdge(): BoxEdge =
    when (this) {
      YogaEdge.LEFT -> BoxEdge.LEFT
      YogaEdge.TOP -> BoxEdge.TOP
      YogaEdge.RIGHT -> BoxEdge.RIGHT
      YogaEdge.BOTTOM -> BoxEdge.BOTTOM
      YogaEdge.START -> BoxEdge.START
      YogaEdge.END -> BoxEdge.END
      YogaEdge.HORIZONTAL -> BoxEdge.HORIZONTAL
      YogaEdge.VERTICAL -> BoxEdge.VERTICAL
      YogaEdge.ALL -> BoxEdge.ALL
    }
