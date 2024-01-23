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

import android.graphics.drawable.Drawable
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaValue

/**
 * A DebugLayoutNode is a wrapper around InternalNode which allows debug tools to inspect and mutate
 * internal nodes without making InternalNode a public class. This class should never be used in
 * production and only for building debug tools.
 */
class DebugLayoutNode internal constructor(private val result: LithoLayoutResult) {

  private val node: LithoNode = result.node
  val foreground: Drawable?
    get() = node.foreground

  val background: Drawable?
    get() = node.background

  fun hasViewOutput(): Boolean = LithoNode.hasViewOutput(result.node)

  val rotation: Float
    get() = node.nodeInfo?.rotation ?: 0f

  val alpha: Float
    get() = node.nodeInfo?.alpha ?: 1f

  val scale: Float
    get() = node.nodeInfo?.scale ?: 1f

  val importantForAccessibility: Int
    get() = node.importantForAccessibility

  val focusable: Boolean
    get() = node.nodeInfo?.focusState == NodeInfo.FOCUS_SET_TRUE

  val contentDescription: CharSequence?
    get() = node.nodeInfo?.contentDescription

  val layoutDirection: YogaDirection
    get() = result.lithoLayoutOutput.yogaNode.styleDirection

  val flexDirection: YogaFlexDirection
    get() = result.lithoLayoutOutput.yogaNode.flexDirection

  val justifyContent: YogaJustify
    get() = result.lithoLayoutOutput.yogaNode.justifyContent

  val alignItems: YogaAlign
    get() = result.lithoLayoutOutput.yogaNode.alignItems

  val alignSelf: YogaAlign
    get() = result.lithoLayoutOutput.yogaNode.alignSelf

  val alignContent: YogaAlign
    get() = result.lithoLayoutOutput.yogaNode.alignContent

  val positionType: YogaPositionType
    get() = result.lithoLayoutOutput.yogaNode.positionType

  val flexGrow: Float
    get() = result.lithoLayoutOutput.yogaNode.flexGrow

  val flexShrink: Float
    get() = result.lithoLayoutOutput.yogaNode.flexShrink

  val flexBasis: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.flexBasis

  val width: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.width

  val minWidth: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.minWidth

  val maxWidth: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.maxWidth

  val height: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.height

  val minHeight: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.minHeight

  val maxHeight: YogaValue
    get() = result.lithoLayoutOutput.yogaNode.maxHeight

  val aspectRatio: Float
    get() = result.lithoLayoutOutput.yogaNode.aspectRatio

  fun getMargin(edge: YogaEdge): YogaValue = result.lithoLayoutOutput.yogaNode.getMargin(edge)

  fun getPadding(edge: YogaEdge): YogaValue = result.lithoLayoutOutput.yogaNode.getPadding(edge)

  fun getPosition(edge: YogaEdge): YogaValue = result.lithoLayoutOutput.yogaNode.getPosition(edge)

  fun getBorderWidth(edge: YogaEdge): Float = result.lithoLayoutOutput.yogaNode.getBorder(edge)

  val clickHandler: EventHandler<ClickEvent>?
    get() = node.nodeInfo?.clickHandler

  val layoutWidth: Float
    get() = result.lithoLayoutOutput.yogaNode.layoutWidth

  val layoutHeight: Float
    get() = result.lithoLayoutOutput.yogaNode.layoutHeight

  fun getLayoutMargin(edge: YogaEdge): Float =
      result.lithoLayoutOutput.yogaNode.getLayoutMargin(edge)

  fun getLayoutPadding(edge: YogaEdge): Float =
      result.lithoLayoutOutput.yogaNode.getLayoutPadding(edge)

  fun getLayoutBorderWidth(edge: YogaEdge): Float =
      result.lithoLayoutOutput.yogaNode.getLayoutBorder(edge)
}
