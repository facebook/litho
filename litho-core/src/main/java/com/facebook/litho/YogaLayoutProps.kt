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

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.state.StateId
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaUnit
import com.facebook.yoga.YogaValue
import com.facebook.yoga.YogaWrap

open class YogaLayoutProps(val node: YogaNode) : LayoutProps {

  @JvmField var isPaddingSet: Boolean = false

  @JvmField var widthFromStyle: YogaValue = YogaValue.UNDEFINED
  @JvmField var minWidthFromStyle: YogaValue = YogaValue.UNDEFINED
  @JvmField var maxWidthFromStyle: YogaValue = YogaValue.UNDEFINED
  @JvmField var heightFromStyle: YogaValue = YogaValue.UNDEFINED
  @JvmField var minHeightFromStyle: YogaValue = YogaValue.UNDEFINED
  @JvmField var maxHeightFromStyle: YogaValue = YogaValue.UNDEFINED

  private var _stateReads: MutableScatterSet<StateId>? = null
  val stateReads: ScatterSet<StateId>?
    get() = _stateReads

  internal fun addStateReads(stateIds: ScatterSet<StateId>) {
    if (stateIds.isEmpty()) return
    val stateReads = _stateReads ?: MutableScatterSet<StateId>().also { _stateReads = it }
    stateReads.addAll(stateIds)
  }

  override fun widthPx(width: Int) {
    setPointSizeAndApply(width, node::setWidth) { yogaValue ->
      widthFromStyle = yogaValue
    }
  }

  override fun widthPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setWidthPercent) { yogaValue ->
      widthFromStyle = yogaValue
    }
  }

  override fun minWidthPx(minWidth: Int) {
    setPointSizeAndApply(minWidth, node::setMinWidth) { yogaValue ->
      minWidthFromStyle = yogaValue
    }
  }

  override fun minWidthPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setMinWidthPercent) { yogaValue ->
      minWidthFromStyle = yogaValue
    }
  }

  override fun maxWidthPx(maxWidth: Int) {
    setPointSizeAndApply(maxWidth, node::setMaxWidth) { yogaValue ->
      maxWidthFromStyle = yogaValue
    }
  }

  override fun maxWidthPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setMaxWidthPercent) { yogaValue ->
      maxWidthFromStyle = yogaValue
    }
  }

  override fun heightPx(height: Int) {
    setPointSizeAndApply(height, node::setHeight) { yogaValue ->
      heightFromStyle = yogaValue
    }
  }

  override fun heightPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setHeightPercent) { yogaValue ->
      heightFromStyle = yogaValue
    }
  }

  override fun minHeightPx(minHeight: Int) {
    setPointSizeAndApply(minHeight, node::setMinHeight) { yogaValue ->
      minHeightFromStyle = yogaValue
    }
  }

  override fun minHeightPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setMinHeightPercent) { yogaValue ->
      minHeightFromStyle = yogaValue
    }
  }

  override fun maxHeightPx(maxHeight: Int) {
    setPointSizeAndApply(maxHeight, node::setMaxHeight) { yogaValue ->
      maxHeightFromStyle = yogaValue
    }
  }

  override fun maxHeightPercent(percent: Float) {
    setPercentSizeAndApply(percent, node::setMaxHeightPercent) { yogaValue ->
      maxHeightFromStyle = yogaValue
    }
  }

  override fun layoutDirection(direction: LayoutDirection) {
    node.setDirection(direction.toYogaDirection())
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

  override fun widthAuto() {
    node.setWidthAuto()
  }

  override fun heightAuto() {
    node.setHeightAuto()
  }

  companion object {
    @JvmStatic
    fun setLayoutDirection(commonProps: CommonProps, direction: YogaDirection) {
      commonProps.layoutDirection(direction.toLayoutDirection())
    }

    private inline fun setPercentSizeAndApply(
        value: Float,
        styleProperty: (Float) -> Unit,
        nodeOperation: (YogaValue) -> Unit,
    ) = setStyleAndApply(YogaValue(value, YogaUnit.PERCENT), styleProperty, nodeOperation)

    private inline fun setPointSizeAndApply(
        value: Int,
        styleProperty: (Float) -> Unit,
        nodeOperation: (YogaValue) -> Unit,
    ) = setStyleAndApply(YogaValue(value.toFloat(), YogaUnit.POINT), styleProperty, nodeOperation)

    private inline fun setStyleAndApply(
        yogaValue: YogaValue,
        styleProperty: (Float) -> Unit,
        nodeOperation: (YogaValue) -> Unit,
    ) {
      styleProperty(yogaValue.value)
      nodeOperation(yogaValue)
    }
  }
}

fun YogaDirection.toLayoutDirection(): LayoutDirection {
  return when (this) {
    YogaDirection.LTR -> LayoutDirection.LTR
    YogaDirection.RTL -> LayoutDirection.RTL
    YogaDirection.INHERIT -> LayoutDirection.INHERIT
    else -> throw IllegalArgumentException("Unknown YogaDirection $this")
  }
}

fun LayoutDirection.toYogaDirection(): YogaDirection {
  return when (this) {
    LayoutDirection.LTR -> YogaDirection.LTR
    LayoutDirection.RTL -> YogaDirection.RTL
    LayoutDirection.INHERIT -> YogaDirection.INHERIT
    else -> throw IllegalArgumentException("Unknown LayoutDirection $this")
  }
}
