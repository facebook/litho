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
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaUnit
import com.facebook.yoga.YogaValue

class DebugLayoutNodeEditor(private val node: LithoNode) {

  fun setForegroundColor(color: Int) {
    node.foregroundColor(color)
  }

  fun setBackgroundColor(color: Int) {
    node.backgroundColor(color)
  }

  fun setRotation(value: Float) {
    node.nodeInfo?.rotation = value
  }

  fun setAlpha(value: Float) {
    node.nodeInfo?.alpha = value
  }

  fun setScale(value: Float) {
    node.nodeInfo?.scale = value
  }

  fun setImportantForAccessibility(importantForAccessibility: Int) {
    node.importantForAccessibility(importantForAccessibility)
  }

  fun setFocusable(focusable: Boolean) {
    node.mutableNodeInfo().setFocusable(focusable)
  }

  fun setContentDescription(contentDescription: CharSequence?) {
    node.mutableNodeInfo().contentDescription = contentDescription
  }

  fun setLayoutDirection(yogaDirection: YogaDirection) {
    node.debugLayoutEditor.layoutDirection(yogaDirection)
  }

  fun setFlexDirection(direction: YogaFlexDirection) {
    node.flexDirection(direction)
  }

  fun setJustifyContent(yogaJustify: YogaJustify) {
    node.justifyContent(yogaJustify)
  }

  fun setAlignItems(yogaAlign: YogaAlign) {
    node.alignItems(yogaAlign)
  }

  fun setAlignSelf(yogaAlign: YogaAlign) {
    node.debugLayoutEditor.alignSelf(yogaAlign)
  }

  fun setAlignContent(yogaAlign: YogaAlign?) {
    node.alignContent(yogaAlign)
  }

  fun setPositionType(yogaPositionType: YogaPositionType) {
    node.debugLayoutEditor.positionType(yogaPositionType)
  }

  fun setFlexGrow(value: Float) {
    node.debugLayoutEditor.flexGrow(value)
  }

  fun setFlexShrink(value: Float) {
    node.debugLayoutEditor.flexShrink(value)
  }

  fun setFlexBasis(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.flexBasisAuto()
      YogaUnit.PERCENT -> node.debugLayoutEditor.flexBasisPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.flexBasisPx(value.value.toInt())
    }
  }

  fun setWidth(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.widthAuto()
      YogaUnit.PERCENT -> node.debugLayoutEditor.widthPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.widthPx(value.value.toInt())
    }
  }

  fun setMinWidth(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.minWidthPx(Int.MIN_VALUE)
      YogaUnit.PERCENT -> node.debugLayoutEditor.minWidthPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.minWidthPx(value.value.toInt())
    }
  }

  fun setMaxWidth(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.maxWidthPx(Int.MAX_VALUE)
      YogaUnit.PERCENT -> node.debugLayoutEditor.maxWidthPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.maxWidthPx(value.value.toInt())
    }
  }

  fun setHeight(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.heightAuto()
      YogaUnit.PERCENT -> node.debugLayoutEditor.heightPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.heightPx(value.value.toInt())
    }
  }

  fun setMinHeight(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.minHeightPx(Int.MIN_VALUE)
      YogaUnit.PERCENT -> node.debugLayoutEditor.minHeightPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.minHeightPx(value.value.toInt())
    }
  }

  fun setMaxHeight(value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.maxHeightPx(Int.MAX_VALUE)
      YogaUnit.PERCENT -> node.debugLayoutEditor.maxHeightPercent(value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.maxHeightPx(value.value.toInt())
    }
  }

  fun setAspectRatio(aspectRatio: Float) {
    node.debugLayoutEditor.aspectRatio(aspectRatio)
  }

  fun setMargin(edge: YogaEdge, value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED -> node.debugLayoutEditor.marginPx(edge, 0)
      YogaUnit.AUTO -> node.debugLayoutEditor.marginAuto(edge)
      YogaUnit.PERCENT -> node.debugLayoutEditor.marginPercent(edge, value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.marginPx(edge, value.value.toInt())
    }
  }

  fun setPadding(edge: YogaEdge, value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.paddingPx(edge, 0)
      YogaUnit.PERCENT -> node.debugLayoutEditor.paddingPercent(edge, value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.paddingPx(edge, value.value.toInt())
    }
  }

  fun setPosition(edge: YogaEdge, value: YogaValue) {
    when (value.unit) {
      YogaUnit.UNDEFINED,
      YogaUnit.AUTO -> node.debugLayoutEditor.positionPercent(edge, YogaConstants.UNDEFINED)
      YogaUnit.PERCENT -> node.debugLayoutEditor.positionPercent(edge, value.value)
      YogaUnit.POINT -> node.debugLayoutEditor.positionPx(edge, value.value.toInt())
    }
  }

  fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    node.debugLayoutEditor.isReferenceBaseline(isReferenceBaseline)
  }

  fun setBorderWidth(edge: YogaEdge, value: Float) {
    node.debugLayoutEditor.setBorderWidth(edge, value.toInt().toFloat())
  }
}
