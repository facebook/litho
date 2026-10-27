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

package com.facebook.litho.layout.internal

import androidx.collection.ScatterSet
import com.facebook.litho.DiffNode
import com.facebook.litho.LeafLayoutOutput
import com.facebook.litho.LithoLayoutContext
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoNode
import com.facebook.litho.LithoYogaLayoutFunction
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.layout.BoxLayoutProps
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.borderBoxSize
import com.facebook.litho.state.StateId
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.getOrElse

/**
 * Lays out a `Layout`'s leaf child with no Yoga node: its content is measured as Yoga's measure
 * callback measures a leaf's, reusing its last pass's measurement through its DiffNode when its
 * specs allow, and its border box is sized as Yoga sizes a leaf root. Its result is made once, and
 * its content is measured again only under specs none of its last measurements stands for, as Yoga
 * keeps a leaf's (`LeafLayoutOutput.contentMeasuredUnder`).
 *
 * A result it builds is written to the layout cache, and copied from it in a later pass that
 * updates nothing in its scope, as Yoga's tree builder copies a cached leaf.
 */
internal class LeafLayoutBuilder(
    private val context: LayoutContext<LithoLayoutContext>,
    private val node: LithoNode,
    private val previousDiffNode: DiffNode?,
) : LithoLayoutBuilder {

  private var result: LithoLayoutResult? = null
  private var isDone = false

  override val boxProps: BoxLayoutProps
    get() {
      check(!isDone) {
        "${node.headComponent.simpleName} box props read after it was built or discarded"
      }
      return checkNotNull(layoutResult().childBoxProps)
    }

  override fun measure(box: BoxConstraints): IntSize {
    check(!isDone) { "${node.headComponent.simpleName} measured after it was built or discarded" }
    return sizeIn(box)
  }

  override fun build(box: BoxConstraints): LithoLayoutResult {
    check(!isDone) { "${node.headComponent.simpleName} built twice, or after it was discarded" }
    val size = sizeIn(box)
    isDone = true
    val result = checkNotNull(result)
    this.result = null
    (result.measureOutput as LeafLayoutOutput).setBorderBox(size, box.padding, box.border)
    result.resolvedMargins = box.margins
    LithoYogaLayoutFunction.saveTreeIntoCache(context, result)
    return result
  }

  override val unbuiltReads: ScatterSet<StateId>?
    get() = null

  override fun discard(): ScatterSet<StateId>? {
    if (isDone) return null
    isDone = true
    val result = result ?: return null
    this.result = null
    val reads = result.stateReads
    result.releaseLayoutPhaseData()
    return reads
  }

  /**
   * Its border-box size within [box] ([borderBoxSize]), measuring its content only under specs no
   * kept measurement stands for.
   */
  private fun sizeIn(box: BoxConstraints): IntSize {
    val result = layoutResult()
    val leafOutput = result.measureOutput as LeafLayoutOutput
    val matchesCompatibleSpecs = !LithoYogaLayoutFunction.shouldAlwaysRemeasure(node.tailComponent)
    return borderBoxSize(box) { widthSpec, heightSpec ->
      // Yoga measures no content for a leaf with no measure function: its size is its box's.
      if (node.yogaMeasureFunction == null) {
        IntSize(0, 0)
      } else {
        // A kept measurement: this pass's, in this box or another, or, copied from the
        // cache, an earlier pass's.
        leafOutput.contentMeasuredUnder(widthSpec, heightSpec, matchesCompatibleSpecs).getOrElse {
          // TODO: content that throws reports it as it's measured, as in a Yoga tree, even if
          // it's never placed; hold it on its result instead, until it's built.
          val content = LithoYogaLayoutFunction.measure(context, result, widthSpec, heightSpec)
          IntSize(content.width, content.height).also { measured ->
            leafOutput.recordMeasurement(widthSpec, heightSpec, measured)
          }
        }
      }
    }
  }

  /**
   * Its result, made the first time it's asked for, as Yoga's tree builder makes a leaf's: copied
   * from the layout cache if nothing in its scope is updated, else made with its box props recorded
   * and its last pass's DiffNode applied.
   */
  private fun layoutResult(): LithoLayoutResult {
    result?.let {
      return it
    }
    val renderContext = checkNotNull(context.renderContext)
    val diffNode = LithoYogaLayoutFunction.validDiffNode(renderContext, node, previousDiffNode)
    val layoutResult =
        layoutResultFromCache(renderContext, diffNode)
            ?: createLayoutResult(renderContext, diffNode)
    result = layoutResult
    return layoutResult
  }

  private fun createLayoutResult(
      renderContext: LithoLayoutContext,
      diffNode: DiffNode?,
  ): LithoLayoutResult {
    val direction = if (node.layoutDirection.isRTL) LayoutDirection.RTL else LayoutDirection.LTR
    val layoutResult = node.createLayoutResult(LeafLayoutOutput(direction, context))
    layoutResult.layoutData = LithoYogaLayoutFunction.newInterStagePropsOf(node)
    layoutResult.childBoxProps =
        recordBoxPropsOf(context, node) { reads -> layoutResult.addStateReads(reads) }
    if (diffNode != null) {
      val isDirty = node.tailComponentKey in renderContext.scopesToUpdate
      LithoYogaLayoutFunction.applyDiffNodeTo(renderContext, node, layoutResult, diffNode, isDirty)
    }
    return layoutResult
  }

  /** A copy of the result the last pass built for its node, unless its scope is updated. */
  private fun layoutResultFromCache(
      renderContext: LithoLayoutContext,
      diffNode: DiffNode?,
  ): LithoLayoutResult? {
    val key = node.tailComponentKey
    if (key in renderContext.scopesToUpdate || renderContext.scopesToUpdate.hasDescendant(key)) {
      return null
    }
    val cachedResult = context.layoutCache.get<LayoutCache.CacheItem>(node)?.layoutResult
    if (cachedResult !is LithoLayoutResult) return null
    val cachedOutput = cachedResult.measureOutput as? LeafLayoutOutput ?: return null
    val copy = node.createLayoutResult(cachedOutput.cachedCopy(context))
    copy.copyCachedFrom(cachedResult)
    if (diffNode != null) copy.diffNode = diffNode
    renderContext.addWholeSubtreeCopy(copy)
    return copy
  }
}
