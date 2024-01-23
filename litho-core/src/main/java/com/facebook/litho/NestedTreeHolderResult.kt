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

/** This is an output only [NestedTreeHolderResult]; this is created by a [NestedTreeHolder]. */
class NestedTreeHolderResult(
    c: ComponentContext,
    internalNode: NestedTreeHolder,
    lithoLayoutOutput: YogaLithoLayoutOutput,
) : LithoLayoutResult(c, internalNode, lithoLayoutOutput) {

  val nestedResult: LithoLayoutResult?
    get() = lithoLayoutOutput.nestedResult

  override val node: NestedTreeHolder
    get() = super.node as NestedTreeHolder

  override fun getXForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("NestedTreeHolder Result has only one child")
    }

    return nestedResult?.lithoLayoutOutput?.x ?: 0
  }

  override fun getYForChildAtIndex(index: Int): Int {
    if (index > 0) {
      throw IllegalArgumentException("NestedTreeHolder Result has only one child")
    }

    return nestedResult?.lithoLayoutOutput?.y ?: 0
  }

  override fun releaseLayoutPhaseData() {
    super.releaseLayoutPhaseData()
    nestedResult?.releaseLayoutPhaseData()
  }
}
