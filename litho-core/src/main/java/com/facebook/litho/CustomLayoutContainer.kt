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

@file:OptIn(ExperimentalLithoApi::class)

package com.facebook.litho

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.ChildBox
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.Measurable
import com.facebook.litho.layout.MeasureScope
import com.facebook.litho.layout.NO_SIZE_DECLARED
import com.facebook.litho.layout.Placeable
import com.facebook.litho.layout.PlacementScope
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateReadRecorder
import com.facebook.litho.stats.LithoStats
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaMeasureFunction
import com.facebook.yoga.YogaMeasureMode
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode

/**
 * The layout result of a `Layout`. To the Yoga tree that holds it, it is a leaf, measured by
 * running its layout function. Its children are the layouts of the measurements that function
 * placed, each of which owns its own result.
 */
internal class CustomLayoutResult(
    context: ComponentContext,
    node: LithoNode,
    measureOutput: LithoMeasureOutput,
) : LithoLayoutResult(context = context, node = node, measureOutput = measureOutput) {

  /** This pass's invocations of the layout function, oldest first. */
  private val invocations = ArrayList<LayoutInvocation>(2)

  /** Last pass's DiffNodes of this container's children, by the key they were placed under. */
  private var diffChildrenByKey: Map<GlobalKey, DiffNode>? = null

  /**
   * The state the children's measurements nothing placed read, by the key of the node in them that
   * read it, as a placed tree's reads are. Those layouts are in no tree, but the layout cache keeps
   * them: read under the reader's key, the reader is laid out again when the state changes, and not
   * taken from the cache as it was. A deferred holder also counts its content's reads.
   */
  internal var unplacedReads: MutableScatterMap<GlobalKey, MutableScatterSet<StateId>>? = null
    private set

  /** Where it placed each of its children, in child order, left then top: written as it settles. */
  private var childPositions: IntArray = NO_POSITIONS

  /** One invocation of the layout function, under [constraints], and what it declared. */
  private class LayoutInvocation(val constraints: SizeConstraints, val scope: MeasureScope)

  /**
   * The size this container answers under [constraints], the room inside its padding and border:
   * the size its layout function declared there, kept within them. A function that throws answers
   * 0x0, and its container places nothing.
   */
  fun measure(context: LayoutContext<LithoLayoutContext>, constraints: SizeConstraints): Long {
    val invocation =
        invokeLayoutFunction(context, constraints) ?: return YogaMeasureOutput.make(0, 0)
    return YogaMeasureOutput.make(
        invocation.scope.width.coerceIn(constraints.minWidth, constraints.maxWidth),
        invocation.scope.height.coerceIn(constraints.minHeight, constraints.maxHeight),
    )
  }

  /**
   * Invokes the layout function under [constraints], or returns this pass's earlier invocation
   * under the same ones; null once the function has thrown.
   */
  private fun invokeLayoutFunction(
      context: LayoutContext<LithoLayoutContext>,
      constraints: SizeConstraints,
  ): LayoutInvocation? {
    if (failure != null) {
      return null
    }
    invocations
        .firstOrNull { it.constraints == constraints }
        ?.let {
          return it
        }
    val measurables: List<Measurable> =
        List(node.childCount) { i ->
          Measurable(i, node.getChildAt(i), context, owner = this)
        }
    val scope = MeasureScope(this.context.resourceResolver, layoutFunctionDirection())
    LithoStats.count(LithoStats.customLayoutFunctionRunCount)
    // State the function reads lays the container out again when it changes: the function may be
    // its only reader.
    runRecordingReads(context, "layout function") {
          trace({ "layout.layoutFunction:${node.headComponent.simpleName}" }) {
            with(checkNotNull(node.layoutFunction)) { scope.measure(measurables, constraints) }
          }
          checkNotNull(scope.placement) { NO_SIZE_DECLARED }
        }
        .onFailure { e ->
          // Only an exception: `attempt` lets anything else through.
          failure = e as Exception
          collectUnplacedMeasurementReads(context, placed = null)
          return null
        }
    return LayoutInvocation(constraints, scope).also { invocations.add(it) }
  }

  /**
   * The second of a container's two phases. First it is measured, any number of times, as the tree
   * around it lays out: each measurement invokes the layout function under the constraints asked,
   * and answers with the size the function declared. Once its final box is decided, this places its
   * children there: an invocation that declared exactly that box places them; otherwise the
   * function is invoked once more under it. The measurements it places become this container's
   * children.
   */
  fun settle(context: LayoutContext<LithoLayoutContext>) {
    if (context.renderContext?.isFutureReleased == true) {
      return
    }
    val insetLeft = paddingLeft + borderLeft
    val insetTop = paddingTop + borderTop
    val contentWidth = (width - insetLeft - paddingRight - borderRight).coerceAtLeast(0)
    val contentHeight = (height - insetTop - paddingBottom - borderBottom).coerceAtLeast(0)
    val invocation =
        invocations.lastOrNull {
          it.scope.width == contentWidth && it.scope.height == contentHeight
        } ?: invokeLayoutFunction(context, SizeConstraints.exact(contentWidth, contentHeight))
    invocations.clear()
    if (invocation == null) {
      return
    }
    val direction = measureOutput.layoutDirection
    val scope = PlacementScope(
        this.context.resourceResolver,
        node.childCount,
        invocation.scope.width,
        direction,
        owner = this,
    )
    // Where children go can depend on state too, read only here.
    runRecordingReads(context, "layout placement") {
          trace({ "layout.placement:${node.headComponent.simpleName}" }) {
            checkNotNull(invocation.scope.placement).invoke(scope)
          }
        }
        .onFailure { e ->
          failure = e as Exception
          collectUnplacedMeasurementReads(context, placed = null)
          return
        }
    collectUnplacedMeasurementReads(context, placed = scope)
    // The declared box sits at the start of the content box: its right edge in right-to-left.
    val startOffset = if (direction.isRTL) contentWidth - invocation.scope.width else 0
    var placedCount = 0
    for (i in 0 until node.childCount) {
      if (scope.placedWith[i] != null) placedCount++
    }
    // Its children's positions are its own, in the order they are added.
    val positions = if (placedCount == 0) NO_POSITIONS else IntArray(placedCount * 2)
    childPositions = positions
    var slot = 0
    for (i in 0 until node.childCount) {
      val placeable = scope.placedWith[i] ?: continue
      // A reused measurement may already be placed by another measurement of this container, in
      // which case this one places a copy.
      val result = placeable.result.acquireForPlacement(this)
      positions[slot] =
          insetLeft +
              startOffset +
              scope.placements[i * 2] +
              placeable.offsetX +
              placeable.marginLeft
      positions[slot + 1] =
          insetTop + scope.placements[i * 2 + 1] + placeable.offsetY + placeable.marginTop
      slot += 2
      addChild(result)
    }
  }

  /**
   * Runs [block], adding the state it reads to this container's result, and returns whether it
   * threw. The reads are kept when it throws too: a failure that depends on state recovers once
   * that state changes.
   */
  private inline fun runRecordingReads(
      context: LayoutContext<LithoLayoutContext>,
      description: String,
      block: () -> Unit,
  ): Result<Unit> {
    if (!node.tailComponentContext.isReadTrackingEnabled) {
      return attempt(block)
    }
    var result: Result<Unit> = Result.success(Unit)
    val reads =
        StateReadRecorder.record(requireNotNull(context.renderContext).treeId, description) {
          result = attempt(block)
        }
    addStateReads(reads)
    return result
  }

  /**
   * Runs [block] and returns whether it threw. Unlike `runCatching`, it catches exceptions only: an
   * `Error`, such as running out of memory, is not a failure of this layout function.
   */
  private inline fun attempt(block: () -> Unit): Result<Unit> =
      try {
        block()
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }

  /**
   * Adds the state read by every measurement of this container's children that [placed] did not
   * place; all of them when the container failed and places nothing. A placed child's reads reach
   * the layout from its own result, but whether a child fits, and whether the function succeeds at
   * all, can depend on state only a measurement nothing placed read.
   */
  private fun collectUnplacedMeasurementReads(
      context: LayoutContext<LithoLayoutContext>,
      placed: PlacementScope?,
  ) {
    if (!node.tailComponentContext.isReadTrackingEnabled) {
      return
    }
    val measurementsByChild = context.renderContext?.cachedMeasurements ?: return
    var reads: MutableScatterSet<StateId>? = null
    for (i in 0 until node.childCount) {
      val measurements = measurementsByChild[node.getChildAt(i)] ?: continue
      val placedResult = placed?.placedWith?.get(i)?.result
      for (measurement in measurements) {
        val result = measurement.result
        if (result !== placedResult) {
          reads = recordReadsOf(result, reads)
        }
      }
    }
    reads?.let { addStateReads(it) }
  }

  /**
   * Adds the state read anywhere in [result]'s subtree to [into], each read also recorded in
   * [unplacedReads] under its reader's key. A deferred holder's content hangs off it, not under it
   * as a child: its reads count against the holder too, whose size is its content's. A nested
   * `Layout`'s own measurements nothing placed are not its children either: their readers come from
   * its [unplacedReads].
   */
  private fun recordReadsOf(
      result: LithoLayoutResult,
      into: MutableScatterSet<StateId>?,
  ): MutableScatterSet<StateId>? {
    var reads = recordUnplacedReads(result.node.tailComponentKey, result.stateReads, into)
    for (i in 0 until result.childCount) {
      reads = recordReadsOf(result.getChildAt(i), reads)
    }
    (result as? CustomLayoutResult)?.unplacedReads?.forEach { readerKey, readerReads ->
      reads = recordUnplacedReads(readerKey, readerReads, reads)
    }
    (result as? DeferredLithoLayoutResult)?.result?.let { content ->
      // State the content read while it was rendered for this measurement. Placed content hands
      // its pass these at the end of the pass, which an unplaced measurement's content never
      // reaches: without them an update to that state finds nothing dirty.
      val contentReads = recordRenderReads(content.node, recordReadsOf(content, null))
      reads = recordUnplacedReads(result.node.tailComponentKey, contentReads, reads)
    }
    return reads
  }

  private fun recordRenderReads(
      node: LithoNode,
      into: MutableScatterSet<StateId>?,
  ): MutableScatterSet<StateId>? {
    var reads = into
    for (i in 0 until node.childCount) {
      reads = recordRenderReads(node.getChildAt(i), reads)
    }
    Resolver.forEachStateReadOn(node) { state, reader ->
      val allReads = reads ?: MutableScatterSet<StateId>().also { reads = it }
      allReads.add(state)
      unplacedReadsOf(reader).add(state)
    }
    return reads
  }

  /**
   * Records [reads] in [unplacedReads] under [readerKey], their reader's, and adds them to [into].
   */
  private fun recordUnplacedReads(
      readerKey: GlobalKey,
      reads: ScatterSet<StateId>?,
      into: MutableScatterSet<StateId>?,
  ): MutableScatterSet<StateId>? {
    if (reads == null || reads.isEmpty()) {
      return into
    }
    unplacedReadsOf(readerKey).addAll(reads)
    return (into ?: MutableScatterSet<StateId>()).also { it.addAll(reads) }
  }

  /** The reads recorded in [unplacedReads] under [readerKey], created empty the first time. */
  private fun unplacedReadsOf(readerKey: GlobalKey): MutableScatterSet<StateId> {
    val byReader =
        unplacedReads
            ?: MutableScatterMap<GlobalKey, MutableScatterSet<StateId>>().also {
              unplacedReads = it
            }
    return byReader.getOrPut(readerKey) { MutableScatterSet() }
  }

  override fun getXForChildAtIndex(index: Int): Int = childPositions[index * 2]

  override fun getYForChildAtIndex(index: Int): Int = childPositions[index * 2 + 1]

  /**
   * A copy also shares its children's positions, and takes the reads of the measurements this
   * container did not place; the base copy gives it the failure, so it is reported if the copy is
   * placed.
   */
  override fun copyForPlacement(): LithoLayoutResult =
      super.copyForPlacement().also { copy ->
        copy as CustomLayoutResult
        copy.childPositions = childPositions
        copy.takeUnplacedStateFrom(this)
      }

  /**
   * Takes [other]'s unplaced reads, for a copy of it that a second container places. The map is
   * shared with the copy, not copied: nothing writes it after settle.
   */
  private fun takeUnplacedStateFrom(other: CustomLayoutResult) {
    unplacedReads = other.unplacedReads
  }

  override fun onFinalized(lithoLayoutContext: LithoLayoutContext) {
    reportFailure()
  }

  /**
   * Reports what the layout function threw to the nearest error boundary. Called for placed results
   * only, so a failure in a measurement nothing placed is never reported.
   */
  private fun reportFailure() {
    val e = failure ?: return
    failure = null
    ComponentUtils.handle(node.tailComponentContext, e)
  }

  /**
   * [child]'s DiffNode from the last pass. Only the children a container placed have one, so it is
   * found by the child's key rather than by its index, which a container that places some children
   * and not others would hand to the wrong sibling.
   */
  fun previousDiffNode(index: Int, child: LithoNode): DiffNode? {
    val parent = diffNode ?: return null
    val key = child.tailComponentKey
    val atIndex = if (index < parent.childCount) parent.getChildAt(index) else null
    if (atIndex != null && atIndex.componentGlobalKey == key) {
      return atIndex
    }
    val byKey =
        diffChildrenByKey
            ?: HashMap<GlobalKey, DiffNode>(parent.childCount * 2).also { map ->
              for (i in 0 until parent.childCount) {
                parent.getChildAt(i)?.let { map[it.componentGlobalKey] = it }
              }
              diffChildrenByKey = map
            }
    return byKey[key]
  }
}

/**
 * The direction the layout function measures in: the node's own, since Yoga's reaches Java only
 * once the layout is done.
 */
private fun LithoLayoutResult.layoutFunctionDirection(): LayoutDirection =
    if (node.layoutDirection.isRTL) LayoutDirection.RTL else LayoutDirection.LTR

/**
 * Yoga's measure callback for a `Layout`: runs its layout function under the room Yoga offers, and
 * answers the size it declared, kept within that room. A failure answers 0x0 and is held on the
 * result, so nothing a layout function throws crosses into Yoga's native code.
 */
internal object DefaultLayoutMeasureFunction : YogaMeasureFunction {

  override fun measure(
      cssNode: YogaNode,
      width: Float,
      widthMode: YogaMeasureMode,
      height: Float,
      heightMode: YogaMeasureMode,
  ): Long {
    val context = LithoYogaLayoutFunction.getLayoutContextFromYogaNode(cssNode)
    if (context.renderContext?.isFutureReleased == true) {
      return YogaMeasureOutput.make(0, 0)
    }
    val result = LithoYogaLayoutFunction.getLayoutResultFromYogaNode(cssNode) as CustomLayoutResult
    return result.measure(
        context,
        SizeConstraints.fromMeasureSpecs(
            widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode),
            heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode),
        ),
    )
  }
}

/**
 * Measures a `Layout`'s child under [constraints]. Each measurement applies the child's own box
 * props within the constraints it was given and lays the child out as a tree of its own, so the
 * placeable it returns owns that layout.
 *
 * A layout this pass already made of its node under the same constraints, by an earlier measure, an
 * earlier run of its container's function, or another container, is reused; otherwise it is laid
 * out. The placeable keeps the size it measured then, whatever lays the child out afterwards.
 */
internal fun Measurable.measureChild(constraints: SizeConstraints): Placeable {
  // A child is measured only during a layout pass, whose context holds its measurements so far.
  val lsc = checkNotNull(context.renderContext)
  val measurements = lsc.cachedMeasurementsFor(node)
  val cached = measurements.firstOrNull { it.constraints == constraints }
  if (cached != null) {
    LithoStats.count(LithoStats.customLayoutMeasurementReusedCount)
    return cached.forMeasurable(this)
  }
  return layOut(constraints).also { measurements.add(it) }
}

/**
 * This placeable, handed to [measurable] for the same child in another container: the same layout,
 * placeable there.
 */
internal fun Placeable.forMeasurable(measurable: Measurable): Placeable =
    if (measurable === this.measurable) this
    else
        Placeable(
            measurable,
            constraints,
            IntSize(measuredWidth, measuredHeight),
            marginLeft,
            marginTop,
            result,
        )

/** Lays this child out under [constraints]: its placeable keeps the size it measured there. */
private fun Measurable.layOut(constraints: SizeConstraints): Placeable =
    StateReadRecorder.untracked {
      LithoStats.count(LithoStats.customLayoutChildLayoutCount)
      val (result, box) =
          trace({ "layout.measureChild:${node.headComponent.simpleName}" }) {
            LithoYogaLayoutFunction.layoutSubtree(
                context,
                node,
                constraints,
                (owner as CustomLayoutResult).previousDiffNode(index, node),
            )
          }
      val margins = box.margins
      Placeable(
          this,
          constraints,
          IntSize(
              (result.width + margins.horizontal).coerceAtLeast(0),
              (result.height + margins.vertical).coerceAtLeast(0),
          ),
          margins.left,
          margins.top,
          result,
      )
    }

/**
 * Lays this Yoga root out within [box], the room its container resolved from the child's own box
 * props, which its node does not carry: only [ChildBox.borderBoxConstraints] and the resolved
 * padding and border are written here.
 *
 * **An upper bound is not a size.** With no size or maximum of its own, Yoga stretches a root to a
 * definite owner dimension. So a bound is written as the root's maximum, which puts Yoga on its
 * fit-to-content path, and only then handed on as the owner dimension; an unbounded axis is handed
 * on as undefined.
 */
internal fun YogaNode.layOutWithin(box: ChildBox) {
  val room = box.borderBoxConstraints
  if (room.hasExactWidth) {
    setWidth(room.maxWidth.toFloat())
    setMinWidth(YogaConstants.UNDEFINED)
    setMaxWidth(YogaConstants.UNDEFINED)
  } else {
    setWidthAuto()
    setMinWidth(if (room.minWidth > 0) room.minWidth.toFloat() else YogaConstants.UNDEFINED)
    setMaxWidth(if (room.hasBoundedWidth) room.maxWidth.toFloat() else YogaConstants.UNDEFINED)
  }
  if (room.hasExactHeight) {
    setHeight(room.maxHeight.toFloat())
    setMinHeight(YogaConstants.UNDEFINED)
    setMaxHeight(YogaConstants.UNDEFINED)
  } else {
    setHeightAuto()
    setMinHeight(if (room.minHeight > 0) room.minHeight.toFloat() else YogaConstants.UNDEFINED)
    setMaxHeight(if (room.hasBoundedHeight) room.maxHeight.toFloat() else YogaConstants.UNDEFINED)
  }
  setPadding(YogaEdge.LEFT, box.padding.left.toFloat())
  setPadding(YogaEdge.TOP, box.padding.top.toFloat())
  setPadding(YogaEdge.RIGHT, box.padding.right.toFloat())
  setPadding(YogaEdge.BOTTOM, box.padding.bottom.toFloat())
  setBorder(YogaEdge.LEFT, box.border.left.toFloat())
  setBorder(YogaEdge.TOP, box.border.top.toFloat())
  setBorder(YogaEdge.RIGHT, box.border.right.toFloat())
  setBorder(YogaEdge.BOTTOM, box.border.bottom.toFloat())
  calculateLayout(
      if (room.hasBoundedWidth) room.maxWidth.toFloat() else Float.NaN,
      if (room.hasBoundedHeight) room.maxHeight.toFloat() else Float.NaN,
  )
}
