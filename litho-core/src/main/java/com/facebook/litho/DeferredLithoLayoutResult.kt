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

import com.facebook.rendercore.LayoutResult

val LayoutResult.isDeferredLayoutResult: Boolean
  get() = this is DeferredLithoLayoutResult

val LayoutResult.measuredLayoutResult: LithoLayoutResult?
  get() = (this as? DeferredLithoLayoutResult)?.result

/** This is an output only [DeferredLithoLayoutResult]; this is created by a [DeferredLithoNode]. */
class DeferredLithoLayoutResult
constructor(
    c: ComponentContext,
    deferredNode: DeferredLithoNode,
    layoutOutput: YogaLayoutOutput,
) : LithoLayoutResult(c, deferredNode, layoutOutput) {

  val result: LithoLayoutResult?
    get() = layoutOutput.actualDeferredNodeResult

  override val node: DeferredLithoNode
    get() = super.node as DeferredLithoNode

  override fun getXForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("Deferred Node Result has only one child")
    }

    return result?.layoutOutput?.x ?: 0
  }

  override fun getYForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("Deferred Node Result has only one child")
    }

    return result?.layoutOutput?.y ?: 0
  }

  override fun releaseLayoutPhaseData() {
    super.releaseLayoutPhaseData()
    result?.releaseLayoutPhaseData()
  }
}
