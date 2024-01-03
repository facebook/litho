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

import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap
import kotlin.jvm.JvmField

open class YogaLayoutProps(val node: YogaNode) : LayoutProps {

  @JvmField var isPaddingSet: Boolean = false
  @JvmField var widthFromStyle: Float = YogaConstants.UNDEFINED
  @JvmField var heightFromStyle: Float = YogaConstants.UNDEFINED

  override fun widthPx(width: Int) {
    widthFromStyle = width.toFloat()
    node.setWidth(widthFromStyle)
  }

  override fun widthPercent(percent: Float) {
    node.setWidthPercent(percent)
  }

  override fun minWidthPx(minWidth: Int) {
    node.setMinWidth(minWidth.toFloat())
  }

  override fun maxWidthPx(maxWidth: Int) {
    node.setMaxWidth(maxWidth.toFloat())
  }

  override fun minWidthPercent(percent: Float) {
    node.setMinWidthPercent(percent)
  }

  override fun maxWidthPercent(percent: Float) {
    node.setMaxWidthPercent(percent)
  }

  override fun heightPx(height: Int) {
    heightFromStyle = height.toFloat()
    node.setHeight(heightFromStyle)
  }

  override fun heightPercent(percent: Float) {
    node.setHeightPercent(percent)
  }

  override fun minHeightPx(minHeight: Int) {
    node.setMinHeight(minHeight.toFloat())
  }

  override fun maxHeightPx(maxHeight: Int) {
    node.setMaxHeight(maxHeight.toFloat())
  }

  override fun minHeightPercent(percent: Float) {
    node.setMinHeightPercent(percent)
  }

  override fun maxHeightPercent(percent: Float) {
    node.setMaxHeightPercent(percent)
  }

  override fun layoutDirection(direction: YogaDirection) {
    node.setDirection(direction)
  }

  override fun alignSelf(alignSelf: YogaAlign) {
    node.alignSelf = alignSelf
  }

  override fun flex(flex: Float) {
    node.flex = flex
  }

  override fun flexGrow(flexGrow: Float) {
    node.flexGrow = flexGrow
  }

  override fun flexShrink(flexShrink: Float) {
    node.flexShrink = flexShrink
  }

  override fun flexBasisPx(flexBasis: Int) {
    node.setFlexBasis(flexBasis.toFloat())
  }

  override fun flexBasisPercent(percent: Float) {
    node.setFlexBasisPercent(percent)
  }

  override fun aspectRatio(aspectRatio: Float) {
    node.aspectRatio = aspectRatio
  }

  override fun positionType(positionType: YogaPositionType) {
    node.positionType = positionType
  }

  override fun positionPx(edge: YogaEdge, position: Int) {
    node.setPosition(edge, position.toFloat())
  }

  override fun positionPercent(edge: YogaEdge, percent: Float) {
    node.setPositionPercent(edge, percent)
  }

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    isPaddingSet = true
    node.setPadding(edge, padding.toFloat())
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    isPaddingSet = true
    node.setPaddingPercent(edge, percent)
  }

  override fun marginPx(edge: YogaEdge, margin: Int) {
    node.setMargin(edge, margin.toFloat())
  }

  override fun marginPercent(edge: YogaEdge, percent: Float) {
    node.setMarginPercent(edge, percent)
  }

  override fun marginAuto(edge: YogaEdge) {
    node.setMarginAuto(edge)
  }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    node.setIsReferenceBaseline(isReferenceBaseline)
  }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    if (useHeightAsBaseline) {
      node.setBaselineFunction { _, _, height -> height }
    }
  }

  override fun heightAuto() {
    node.setHeightAuto()
  }

  override fun widthAuto() {
    node.setWidthAuto()
  }

  override fun flexBasisAuto() {
    node.setFlexBasisAuto()
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    node.setBorder(edge, borderWidth)
  }

  override fun gap(gutter: YogaGutter, length: Int) {
    node.setGap(gutter, length.toFloat())
  }

  open fun flexDirection(direction: YogaFlexDirection) {
    node.flexDirection = direction
  }

  open fun wrap(wrap: YogaWrap) {
    node.wrap = wrap
  }

  open fun justifyContent(justify: YogaJustify) {
    node.justifyContent = justify
  }

  open fun alignItems(align: YogaAlign) {
    node.alignItems = align
  }
}
