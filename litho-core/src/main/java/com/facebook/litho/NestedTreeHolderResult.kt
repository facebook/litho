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

import com.facebook.yoga.YogaNode

/** This is an output only [NestedTreeHolderResult]; this is created by a [NestedTreeHolder]. */
class NestedTreeHolderResult(
    c: ComponentContext,
    internalNode: NestedTreeHolder,
    yogaNode: YogaNode,
    widthFromStyle: Float,
    heightFromStyle: Float,
) : LithoLayoutResult(c, internalNode, yogaNode, widthFromStyle, heightFromStyle) {

  var nestedResult: LithoLayoutResult? = null

  override val node: NestedTreeHolder
    get() = super.node as NestedTreeHolder

  override fun getXForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("NestedTreeHolder Result has only one child")
    }

    return nestedResult?.yogaNode?.layoutX?.toInt() ?: 0
  }

  override fun getYForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("NestedTreeHolder Result has only one child")
    }

    return nestedResult?.yogaNode?.layoutY?.toInt() ?: 0
  }

  override fun releaseLayoutPhaseData() {
    super.releaseLayoutPhaseData()
    nestedResult?.releaseLayoutPhaseData()
  }
}
