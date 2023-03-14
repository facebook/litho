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

class LegacyRenderFuture(
    private val context: ComponentContext,
    private val root: Component,
    private val treeState: TreeState,
    private val widthSpec: Int,
    private val heightSpec: Int,
    private val layoutVersion: Int,
    private val treeId: Int,
    private val currentNode: LithoNode?,
    private val isLayoutDiffingEnabled: Boolean,
    private val currentLayoutState: LayoutState?,
    private val diffNodeRoot: DiffNode?,
    private val extraAttribution: String?,
    private val perfEventLogger: PerfEvent?,
    interruptable: Boolean,
) : TreeFuture<LegacyPotentiallyPartialResult>(interruptable) {

  override fun calculate(): LegacyPotentiallyPartialResult {
    val resolveResult =
        ResolveTreeFuture.resolve(
            context,
            root,
            treeState,
            layoutVersion,
            treeId,
            currentNode,
            extraAttribution,
            this,
            perfEventLogger)

    if (resolveResult.isPartialResult) {
      return LegacyPotentiallyPartialResult(partial = resolveResult)
    }

    val layoutState =
        LayoutTreeFuture.layout(
            resolveResult,
            widthSpec,
            heightSpec,
            layoutVersion,
            treeId,
            isLayoutDiffingEnabled,
            currentLayoutState,
            diffNodeRoot,
            this,
            perfEventLogger)

    return LegacyPotentiallyPartialResult(actual = layoutState)
  }

  override fun resumeCalculation(
      partialResult: LegacyPotentiallyPartialResult
  ): LegacyPotentiallyPartialResult {

    val partialResolveResult = checkNotNull(partialResult.partial)

    val resolveResult = ResolveTreeFuture.resume(partialResolveResult, extraAttribution)

    val layoutState =
        LayoutTreeFuture.layout(
            resolveResult,
            widthSpec,
            heightSpec,
            layoutVersion,
            treeId,
            isLayoutDiffingEnabled,
            currentLayoutState,
            diffNodeRoot,
            this,
            perfEventLogger)

    return LegacyPotentiallyPartialResult(actual = layoutState)
  }

  override fun getDescription(): String = "render"

  override fun getVersion(): Int = layoutVersion

  override fun isEquivalentTo(that: TreeFuture<*>?): Boolean {

    if (that !is LegacyRenderFuture) {
      return false
    }

    if (this === that) {
      return true
    }

    val thatLsf: LegacyRenderFuture = that

    if (widthSpec != thatLsf.widthSpec) {
      return false
    }
    if (heightSpec != thatLsf.heightSpec) {
      return false
    }
    if (context != thatLsf.context) {
      return false
    }
    if (root.id != thatLsf.root.id) {
      return false
    }

    return true
  }
}

class LegacyPotentiallyPartialResult(
    val actual: LayoutState? = null,
    val partial: ResolveResult? = null,
) : PotentiallyPartialResult {
  override val isPartialResult: Boolean
    get() = partial?.isPartialResult ?: false
}
