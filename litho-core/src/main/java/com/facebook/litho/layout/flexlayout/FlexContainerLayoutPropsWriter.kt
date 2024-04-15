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

import com.facebook.flexlayout.styles.FlexBoxStyle
import com.facebook.litho.LayoutProps
import com.facebook.litho.LithoFlexLayoutFunction.toAlignContent
import com.facebook.litho.LithoFlexLayoutFunction.toAlignItems
import com.facebook.litho.LithoFlexLayoutFunction.toEdge
import com.facebook.litho.LithoFlexLayoutFunction.toFlexDirection
import com.facebook.litho.LithoFlexLayoutFunction.toFlexLayoutDirection
import com.facebook.litho.LithoFlexLayoutFunction.toFlexWrap
import com.facebook.litho.LithoFlexLayoutFunction.toJustifyContent
import com.facebook.litho.layout.LayoutDirection
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap

/** This class is responsible for writing FlexLayout properties to the FlexBoxStyle.Builder. */
internal class FlexContainerLayoutPropsWriter(
    private val builder: FlexBoxStyle.Builder,
    private val isLTR: Boolean
) : LayoutProps {

  /** Whether padding is set or not. */
  var isPaddingSet: Boolean = false

  override fun widthPx(width: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun widthPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun minWidthPx(minWidth: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun maxWidthPx(maxWidth: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun minWidthPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun maxWidthPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun heightPx(height: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun heightPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun minHeightPx(minHeight: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun maxHeightPx(maxHeight: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun minHeightPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun maxHeightPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun layoutDirection(direction: LayoutDirection) {
    FlexBoxStyle.Builder.setDirection(builder, direction.toFlexLayoutDirection())
  }

  override fun alignSelf(alignSelf: YogaAlign) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun flex(flex: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun flexGrow(flexGrow: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun flexShrink(flexShrink: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun flexBasisPx(flexBasis: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun flexBasisPercent(percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun aspectRatio(aspectRatio: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun positionType(positionType: YogaPositionType) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun positionPx(edge: YogaEdge, position: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun positionPercent(edge: YogaEdge, percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun paddingPx(edge: YogaEdge, padding: Int) {
    edge.toEdge(isLTR) { FlexBoxStyle.Builder.setPadding(builder, it, padding.toFloat()) }
    isPaddingSet = true
  }

  override fun paddingPercent(edge: YogaEdge, percent: Float) {
    edge.toEdge(isLTR) { FlexBoxStyle.Builder.setPaddingPercent(builder, it, percent) }
    isPaddingSet = true
  }

  override fun marginPx(edge: YogaEdge, margin: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun marginPercent(edge: YogaEdge, percent: Float) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun marginAuto(edge: YogaEdge) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    // Ignore item props while writing FlexBoxStyle
  }

  override fun setBorderWidth(edge: YogaEdge, borderWidth: Float) {
    edge.toEdge(isLTR) { FlexBoxStyle.Builder.setBorder(builder, it, borderWidth) }
  }

  override fun gap(gutter: YogaGutter, length: Int) {
    // Ignore item props while writing FlexBoxStyle
  }

  fun flexDirection(flexDirection: YogaFlexDirection) {
    FlexBoxStyle.Builder.setFlexDirection(builder, flexDirection.toFlexDirection())
  }

  fun justifyContent(justifyContent: YogaJustify) {
    FlexBoxStyle.Builder.setJustifyContent(builder, justifyContent.toJustifyContent())
  }

  fun alignItems(alignItems: YogaAlign) {
    FlexBoxStyle.Builder.setAlignItems(builder, alignItems.toAlignItems())
  }

  fun alignContent(alignContent: YogaAlign) {
    FlexBoxStyle.Builder.setAlignContent(builder, alignContent.toAlignContent())
  }

  fun flexWrap(flexWrap: YogaWrap) {
    FlexBoxStyle.Builder.setWrap(builder, flexWrap.toFlexWrap())
  }
}
