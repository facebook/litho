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

import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import com.facebook.litho.DefaultMeasureOutput
import com.facebook.litho.LithoLayoutContext
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoNode
import com.facebook.litho.NO_POSITIONS
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.borderBoxSizeAround
import com.facebook.litho.contentSpec
import com.facebook.litho.isReadTrackingEnabled
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.Measurable
import com.facebook.litho.layout.MeasureScope
import com.facebook.litho.layout.NO_SIZE_DECLARED
import com.facebook.litho.layout.Placeable
import com.facebook.litho.layout.PlacementScope
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateReadRecorder
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec

/**
 * Lays out a `Layout` for one pass. Each measure runs its layout function, unless its latest run
 * already fits the box; its build places from a run that fits the box it's built in. It holds what
 * the pass makes of its children, their builders and placeables, so a `Measurable` is only data.
 *
 * Its parent decides each box, and the two kinds of parent decide them differently:
 * ```
 *                 parent is a `Layout`                 parent is Yoga
 *   measure(box)  the first time the parent's          each time Yoga's measure callback
 *                 function measures it under each      asks, within the room Yoga offers
 *                 set of constraints
 *   build(box)    the box of the placeable the         its final size, which Yoga may not
 *                 parent's function placed: one it     have measured: a stretch is decided
 *                 was measured in                      after measuring
 * ```
 */
@OptIn(ExperimentalLithoApi::class)
internal class DefaultLayoutBuilder(
    private val context: LayoutContext<LithoLayoutContext>,
    private val node: LithoNode,
) : LithoLayoutBuilder {

  private val layoutDirection: LayoutDirection =
      if (node.layoutDirection.isRTL) LayoutDirection.RTL else LayoutDirection.LTR

  private val measurables: List<Measurable> =
      List(node.childCount) { index ->
        Measurable(index, node.getChildAt(index), context, owner = this)
      }

  // By child index. Most children are measured under one set of constraints, so their first
  // placeable needs no map.
  private val childBuilders: Array<LithoLayoutBuilder?> = arrayOfNulls(node.childCount)
  private val firstPlaceables: Array<Placeable?> = arrayOfNulls(node.childCount)
  private var morePlaceables: Array<MutableLongObjectMap<Placeable>?>? = null

  // The latest run of its layout function: the content constraints it ran under, and what it
  // declared or what it threw.
  private var declaredUnder: SizeConstraints = SizeConstraints()
  private var declared: MeasureScope? = null
  private var functionFailure: Exception? = null

  private var stateReads: MutableScatterSet<StateId>? = null
  private var isDone = false

  override fun measure(box: BoxConstraints): IntSize {
    check(!isDone) { "${node.headComponent.simpleName} measured after it was built or discarded" }
    declareIn(box)
    return borderBoxSizeIn(box)
  }

  override fun build(box: BoxConstraints): LithoLayoutResult {
    check(!isDone) { "${node.headComponent.simpleName} built twice, or after it was discarded" }
    isDone = true
    declareIn(box)
    val size = borderBoxSizeIn(box)

    var failure = functionFailure
    var placed: PlacementScope? = null
    val declared = declared
    if (declared != null) {
      val scope = PlacementScope(
          node.tailComponentContext.resourceResolver,
          node.childCount,
          declared.width,
          layoutDirection,
          owner = this,
      )
      failure = recordingReads("layout placement") { checkNotNull(declared.placement)(scope) }
      if (failure == null) placed = scope
    }

    // A child it doesn't build never reaches the final tree, so this `Layout` carries its reads.
    val tracksReads = node.tailComponentContext.isReadTrackingEnabled
    var builtCount = 0
    for (i in 0 until node.childCount) {
      if (placed?.placedWith?.get(i) != null) {
        builtCount++
      } else {
        childBuilders[i]?.discard(if (tracksReads) ownReads() else null)
      }
    }

    val positions = if (builtCount == 0) NO_POSITIONS else IntArray(builtCount * 2)
    val output = DefaultMeasureOutput(
        size.width,
        size.height,
        box.padding,
        box.border,
        layoutDirection,
        positions = positions,
    )
    val result = LithoLayoutResult(node.tailComponentContext, node, output)
    val contentWidth = (size.width - box.edgesAcross).coerceAtLeast(0)
    val contentHeight = (size.height - box.edgesDown).coerceAtLeast(0)
    result.recordMeasurement(
        declaredUnder.toWidthSpec(),
        declaredUnder.toHeightSpec(),
        contentWidth,
        contentHeight,
    )
    result.failure = failure
    stateReads?.let(result::adoptStateReads)
    // The reducer and the layout cache treat a subtree holding a `Layout` apart.
    result.subtreeHasCustomLayout = true
    if (placed == null) {
      return result
    }
    val left = box.padding.left + box.border.left
    val top = box.padding.top + box.border.top
    // Where a child's border box sits across; down is the same, with no start:
    //
    //   |border+padding|<-------------------- content width -------------------->|
    //   |   = left     |<-- start -->|<------------- declared width ------------>|
    //   |              |             |<-- x -->|<- offsetX ->|<- margin ->|child
    //
    // start is 0 left to right; right to left, the declared width ends at the content's right
    // edge. x is where the placement put the child's placeable. offsetX centres what the child
    // measured within its placeable's width, which differ when the measured size didn't fit the
    // child's constraints. Its border box starts after its left margin.
    val start = if (layoutDirection.isRTL) contentWidth - checkNotNull(declared).width else 0
    for (i in 0 until node.childCount) {
      val placeable = placed.placedWith[i] ?: continue
      val slot = result.childCount * 2
      positions[slot] =
          left + start + placed.placements[i * 2] + placeable.offsetX + placeable.marginLeft
      positions[slot + 1] =
          top + placed.placements[i * 2 + 1] + placeable.offsetY + placeable.marginTop
      result.addChild(buildChildUnder(i, placeable.constraints))
    }
    return result
  }

  override fun discard(reads: MutableScatterSet<StateId>?) {
    if (isDone) return
    isDone = true
    if (reads != null) stateReads?.let(reads::addAll)
    for (builder in childBuilders) builder?.discard(reads)
  }

  /** [child] measured under [constraints]; measured again under them, the same placeable. */
  internal fun measureChildUnder(child: Measurable, constraints: SizeConstraints): Placeable =
      placeableUnder(child.index, constraints)
          ?: newPlaceableUnder(child.index, constraints).also { placeable ->
            remember(child.index, placeable)
          }

  /**
   * Child [index] measured under [constraints] by its builder, which its first measure makes.
   * Untracked: the child's reads land on its own result, not on this `Layout`'s.
   */
  private fun newPlaceableUnder(index: Int, constraints: SizeConstraints): Placeable = TODO()

  /** Child [index]'s result, built by its builder within the box of [constraints]. */
  private fun buildChildUnder(index: Int, constraints: SizeConstraints): LithoLayoutResult = TODO()

  private fun placeableUnder(index: Int, constraints: SizeConstraints): Placeable? {
    val first = firstPlaceables[index] ?: return null
    if (first.constraints == constraints) {
      return first
    }
    return morePlaceables?.get(index)?.get(constraints.encodedValue)
  }

  private fun remember(index: Int, placeable: Placeable) {
    if (firstPlaceables[index] == null) {
      firstPlaceables[index] = placeable
      return
    }
    val more =
        morePlaceables
            ?: arrayOfNulls<MutableLongObjectMap<Placeable>>(node.childCount).also { made ->
              morePlaceables = made
            }
    val placeables =
        more[index] ?: MutableLongObjectMap<Placeable>(2).also { made -> more[index] = made }
    placeables[placeable.constraints.encodedValue] = placeable
  }

  /**
   * Runs its layout function within [box], unless the latest run was under the box's content
   * constraints or, when they're exact, declared exactly that size. A failure is final for the
   * pass, as it was before builders: the parent already laid out around its 0 × 0, so a later run
   * that succeeds would be placed at a size nothing measured.
   *
   * Only the latest run is kept, and a build usually finds it fits (content widths, no padding;
   * heights are exact and equal throughout):
   * ```
   *   under a `Layout`: its function usually places what it measured last
   *     measure(at most 300)  -> run A, declares 200
   *     measure(exactly 250)  -> run B, declares 250          latest: B
   *     then one of:
   *     build(exactly 250)    -> B ran under those constraints: fits, no run
   *     build(at most 300)    -> B didn't: runs again (a function that places an earlier
   *                              measurement pays one more run; its children don't)
   *
   *   under Yoga: the final box is decided after measuring, and may be a stretch
   *     measure(at most 300)  -> run A, declares 250          latest: A
   *     then one of:
   *     build(exactly 250)    -> A declared exactly 250: fits, no run (Yoga's own rule:
   *                              a size measured is valid at exactly that size)
   *     build(exactly 280)    -> stretched past what A declared: runs again, exactly
   * ```
   */
  private fun declareIn(box: BoxConstraints) {
    if (functionFailure != null) return
    val constraints = box.contentConstraints()
    val latest = declared
    if (
        latest != null &&
            (declaredUnder == constraints ||
                (constraints.hasExactWidth &&
                    constraints.hasExactHeight &&
                    latest.width == constraints.maxWidth &&
                    latest.height == constraints.maxHeight))
    ) {
      return
    }
    val scope = MeasureScope(node.tailComponentContext.resourceResolver, layoutDirection)
    val failure =
        recordingReads("layout function") {
          with(checkNotNull(node.layoutFunction)) { scope.measure(measurables, constraints) }
          check(scope.placement != null) { NO_SIZE_DECLARED }
        }
    declaredUnder = constraints
    declared = if (failure == null) scope else null
    functionFailure = failure
  }

  /** Its border-box size in [box], around what its latest run declared: none if it failed. */
  private fun borderBoxSizeIn(box: BoxConstraints): IntSize =
      borderBoxSizeAround(declared?.width ?: 0, declared?.height ?: 0, box)

  /**
   * Runs [block], recording the state it reads as this `Layout`'s; returns what it threw. Reads are
   * kept when it throws too: a failure that depends on state recovers when that state changes.
   */
  private inline fun recordingReads(description: String, block: () -> Unit): Exception? {
    if (!node.tailComponentContext.isReadTrackingEnabled) {
      return attempt(block)
    }
    var failure: Exception? = null
    val reads =
        StateReadRecorder.record(requireNotNull(context.renderContext).treeId, description) {
          failure = attempt(block)
        }
    if (reads.isNotEmpty()) ownReads().addAll(reads)
    return failure
  }

  /** Only an `Exception` is a layout's failure: an `Error` propagates. */
  private inline fun attempt(block: () -> Unit): Exception? =
      try {
        block()
        null
      } catch (failure: Exception) {
        failure
      }

  private fun ownReads(): MutableScatterSet<StateId> =
      stateReads ?: mutableScatterSetOf<StateId>().also { made -> stateReads = made }
}

/**
 * The constraints its layout function runs under: the room inside the padding and border. The
 * parent resolves the box from this `Layout`'s own size props; its function sees only the content.
 * They're a leaf's content spec, so a minimum reaches it only on an exact axis, as Yoga's measure
 * specs carry none: a function that hands its constraints to a child would otherwise force that
 * child up to it. [borderBoxSizeAround] applies the minimum to what it declares.
 *
 * ```
 *   +------------- border box, within box.borderBox --------------+
 *   |  +----------------- padding box: inside the border -----------------+  |
 *   |  |  +---------- content box, within what this returns -----------+  |  |
 *   |  |  |                                                            |  |  |
 *   |  |  +------------------------------------------------------------+  |  |
 *   |  +------------------------------------------------------------------+  |
 *   +------------------------------------------------------------------------+
 * ```
 */
private fun BoxConstraints.contentConstraints(): SizeConstraints {
  val room = borderBox
  return SizeConstraints.fromMeasureSpecs(
      contentSpec(room.hasExactWidth, room.hasBoundedWidth, room.maxWidth, edgesAcross),
      contentSpec(room.hasExactHeight, room.hasBoundedHeight, room.maxHeight, edgesDown),
  )
}
