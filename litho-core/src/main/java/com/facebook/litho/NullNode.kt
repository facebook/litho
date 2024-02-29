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

import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap

/**
 * This [LithoNode] represents a component that renders to `null`. This is required to support
 * reconciliation of state, and transitions on a component that conditionally renders to null.
 */
class NullNode : LithoNode() {

  override fun createLayoutResult(layoutOutput: YogaLayoutOutput): LithoLayoutResult {
    return NullLithoLayoutResult(c = tailComponentContext, node = this, layoutOutput = layoutOutput)
  }
}

class NullLithoLayoutResult
internal constructor(c: ComponentContext, node: NullNode, layoutOutput: YogaLayoutOutput) :
    LithoLayoutResult(context = c, node = node, layoutOutput = layoutOutput)

class NullWriter internal constructor() : YogaLayoutProps(node = NodeConfig.createYogaNode()) {

  override fun widthPx(width: Int) = Unit

  override fun widthPercent(percent: Float) = Unit

  override fun minWidthPx(minWidth: Int) = Unit

  override fun maxWidthPx(maxWidth: Int) = Unit

  override fun minWidthPercent(percent: Float) = Unit

  override fun maxWidthPercent(percent: Float) = Unit

  override fun heightPx(height: Int) = Unit

  override fun heightPercent(percent: Float) = Unit

  override fun minHeightPx(minHeight: Int) = Unit

  override fun maxHeightPx(maxHeight: Int) = Unit

  override fun minHeightPercent(percent: Float) = Unit

  override fun maxHeightPercent(percent: Float) = Unit

  override fun layoutDirection(direction: LayoutDirection) = Unit

  override fun alignSelf(alignSelf: YogaAlign) = Unit

  override fun flex(flex: Float) = Unit

  override fun flexGrow(flexGrow: Float) = Unit

  override fun flexShrink(flexShrink: Float) = Unit

  override fun flexBasisPx(flexBasis: Int) = Unit

  override fun flexBasisPercent(percent: Float) = Unit

  override fun aspectRatio(aspectRatio: Float) = Unit

  override fun positionType(positionType: YogaPositionType) = Unit

  override fun positionPx(edge: YogaEdge, position: Int) = Unit

  override fun positionPercent(edge: YogaEdge, percent: Float) = Unit

  override fun paddingPx(edge: YogaEdge, padding: Int) = Unit

  override fun paddingPercent(edge: YogaEdge, percent: Float) = Unit

  override fun marginPx(edge: YogaEdge, margin: Int) = Unit

  override fun marginPercent(edge: YogaEdge, percent: Float) = Unit

  override fun marginAuto(edge: YogaEdge) = Unit

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) = Unit

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) = Unit

  override fun heightAuto() = Unit

  override fun widthAuto() = Unit

  override fun flexBasisAuto() = Unit

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) = Unit

  override fun gap(gutter: YogaGutter, length: Int) = Unit

  override fun flexDirection(direction: YogaFlexDirection) = Unit

  override fun wrap(wrap: YogaWrap) = Unit

  override fun justifyContent(justify: YogaJustify) = Unit

  override fun alignItems(align: YogaAlign) = Unit
}
