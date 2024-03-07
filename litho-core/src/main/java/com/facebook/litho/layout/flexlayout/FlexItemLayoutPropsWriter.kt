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

package com.facebook.litho.layout.flexlayout

import com.facebook.flexlayout.styles.FlexItemStyle
import com.facebook.flexlayout.styles.PositionType
import com.facebook.litho.LayoutProps
import com.facebook.litho.LithoFlexLayoutFunction.toAlignSelf
import com.facebook.litho.LithoFlexLayoutFunction.toEdge
import com.facebook.litho.LithoFlexLayoutFunction.toPositionType
import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaPositionType

/** This class is responsible for writing FlexLayout properties to the FlexItemStyle.Builder. */
class FlexItemLayoutPropsWriter(
    private val builder: FlexItemStyle.Builder,
    private val isContainer: Boolean,
    private val isLTR: Boolean,
    positionType: PositionType? = PositionType.RELATIVE
) : LayoutProps {

  private val hasAbsolutionPosition: Boolean

  init {
    // Since flex position relies on position type, we have to read this flag firstly.
    hasAbsolutionPosition = (positionType == PositionType.ABSOLUTE)
    if (hasAbsolutionPosition) {
      FlexItemStyle.Builder.setPositionType(builder, PositionType.ABSOLUTE)
    }
  }

  override fun widthPx(width: Int) {
    FlexItemStyle.Builder.setWidth(builder, width.toFloat())
  }

  override fun widthPercent(percent: Float) {
    FlexItemStyle.Builder.setWidthPercent(builder, percent)
  }

  override fun minWidthPx(minWidth: Int) {
    FlexItemStyle.Builder.setMinWidth(builder, minWidth.toFloat())
  }

  override fun maxWidthPx(maxWidth: Int) {
    FlexItemStyle.Builder.setMaxWidth(builder, maxWidth.toFloat())
  }

  override fun minWidthPercent(percent: Float) {
    FlexItemStyle.Builder.setMinWidthPercent(builder, percent)
  }

  override fun maxWidthPercent(percent: Float) {
    FlexItemStyle.Builder.setMaxWidth(builder, percent)
  }

  override fun heightPx(height: Int) {
    FlexItemStyle.Builder.setHeight(builder, height.toFloat())
  }

  override fun heightPercent(percent: Float) {
    FlexItemStyle.Builder.setHeightPercent(builder, percent)
  }

  override fun minHeightPx(minHeight: Int) {
    FlexItemStyle.Builder.setMinHeight(builder, minHeight.toFloat())
  }

  override fun maxHeightPx(maxHeight: Int) {
    FlexItemStyle.Builder.setMaxHeight(builder, maxHeight.toFloat())
  }

  override fun minHeightPercent(percent: Float) {
    FlexItemStyle.Builder.setHeightPercent(builder, percent)
  }

  override fun maxHeightPercent(percent: Float) {
    FlexItemStyle.Builder.setMaxHeight(builder, percent)
  }

  override fun layoutDirection(direction: LayoutDirection) {
    // TODO: Ignore if this is a container but warning if this is a leaf node.
  }

  override fun alignSelf(alignSelf: YogaAlign) {
    FlexItemStyle.Builder.setAlignSelf(builder, alignSelf.toAlignSelf())
  }

  override fun flex(flex: Float) {
    FlexItemStyle.Builder.setFlex(builder, flex)
  }

  override fun flexGrow(flexGrow: Float) {
    FlexItemStyle.Builder.setFlexGrow(builder, flexGrow)
  }

  override fun flexShrink(flexShrink: Float) {
    FlexItemStyle.Builder.setFlexShrink(builder, flexShrink)
  }

  override fun flexBasisPx(flexBasis: Int) {
    FlexItemStyle.Builder.setFlexBasis(builder, flexBasis.toFloat())
  }

  override fun flexBasisPercent(percent: Float) {
    FlexItemStyle.Builder.setFlexBasisPercent(builder, percent)
  }

  override fun aspectRatio(aspectRatio: Float) {
    FlexItemStyle.Builder.setAspectRatio(builder, aspectRatio)
  }

  override fun positionType(positionType: YogaPositionType) {
    val type = positionType.toPositionType()
    if (type == PositionType.ABSOLUTE) {
      FlexItemStyle.Builder.setPositionType(builder, PositionType.ABSOLUTE)
    }
  }

  override fun positionPx(yogaEdge: YogaEdge, position: Int) {
    if (hasAbsolutionPosition) {
      yogaEdge.toEdge(isLTR) { edge ->
        FlexItemStyle.Builder.setPosition(builder, edge, position.toFloat())
      }
    }
  }

  override fun positionPercent(yogaEdge: YogaEdge, percent: Float) {
    if (hasAbsolutionPosition) {
      yogaEdge.toEdge(isLTR) { edge ->
        FlexItemStyle.Builder.setPositionPercent(builder, edge, percent)
      }
    }
  }

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    if (isContainer) {
      // Skipped because it has been applied to FlexBoxStyle
    } else {
      // TODO: We should use margin instead
    }
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    if (isContainer) {
      // Skipped because it has been applied to FlexBoxStyle
    } else {
      // TODO: We should use margin instead
    }
  }

  override fun marginPx(edge: YogaEdge, margin: Int) {
    edge.toEdge(isLTR) { FlexItemStyle.Builder.setMargin(builder, it, margin.toFloat()) }
  }

  override fun marginPercent(edge: YogaEdge, percent: Float) {
    edge.toEdge(isLTR) { FlexItemStyle.Builder.setMarginPercent(builder, it, percent) }
  }

  override fun marginAuto(edge: YogaEdge) {
    edge.toEdge(isLTR) { FlexItemStyle.Builder.setMarginAuto(builder, it) }
  }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    // Not supported yet
  }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    // Not supported yet
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    // TODO: log error settings
  }

  override fun gap(gutter: YogaGutter, length: Int) {
    // Not supported yet
  }
}
