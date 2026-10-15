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

import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.ChildBox
import com.facebook.litho.layout.LayoutFunction
import com.facebook.litho.layout.LayoutScope
import com.facebook.litho.layout.Measurable
import com.facebook.litho.layout.NO_SIZE_DECLARED
import com.facebook.litho.layout.Placeable
import com.facebook.litho.layout.PlaceableScope
import com.facebook.litho.state.StateReadRecorder
import com.facebook.litho.stats.LithoStats
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
    layoutOutput: YogaLayoutOutput,
) : LithoLayoutResult(context = context, node = node, layoutOutput = layoutOutput) {

  /** This pass's invocations of the layout function, oldest first. */
  private val invocations = ArrayList<LayoutInvocation>(2)

  /** Last pass's DiffNodes of this container's children, by the key they were placed under. */
  private var diffChildrenByKey: Map<GlobalKey, DiffNode>? = null

  /** What the layout function threw, reported if this container is placed. */
  private var failure: Exception? = null

  /** One invocation of the layout function, under [constraints], and what it declared. */
  private class LayoutInvocation(val constraints: SizeConstraints, val scope: LayoutScope)

  private val layoutFunction: LayoutFunction
    get() = checkNotNull(node.layoutFunction)

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
          ChildMeasurable(i, node.getChildAt(i), context, owner = this)
        }
    val scope = LayoutScope()
    LithoStats.customLayoutFunctionRunCount.incrementAndGet()
    // State the function reads lays the container out again when it changes: the function may be
    // its only reader.
    runRecordingReads(context, "layout function") {
          trace({ "layout.layoutFunction:${node.headComponent.simpleName}" }) {
            with(layoutFunction) { scope.measure(measurables, constraints) }
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
    val direction = layoutOutput.layoutDirection
    val scope = PlaceableScope(node.childCount, invocation.scope.width, direction, owner = this)
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
    for (i in 0 until node.childCount) {
      val placeable = scope.placedWith[i] ?: continue
      // An answer from memory may already be placed by another measurement of this container, in
      // which case this one places a copy.
      val result = checkNotNull(placeable.result).acquireForPlacement(this)
      result.layoutOutput.placedX =
          insetLeft +
              startOffset +
              scope.placements[i * 2] +
              placeable.offsetX +
              placeable.marginLeft
      result.layoutOutput.placedY =
          insetTop + scope.placements[i * 2 + 1] + placeable.offsetY + placeable.marginTop
      addChild(result)
    }
  }

  /**
   * Runs [block], adding the state it reads to this container's output, and returns whether it
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
    layoutOutput.addStateReads(reads)
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
      placed: PlaceableScope?,
  ) {
    if (!node.tailComponentContext.isReadTrackingEnabled) {
      return
    }
    val measurementsByChild = context.renderContext?.cachedMeasurements ?: return
    for (i in 0 until node.childCount) {
      val measurements = measurementsByChild[node.getChildAt(i)] ?: continue
      val placedResult = placed?.placedWith?.get(i)?.result
      for (measurement in measurements) {
        val result = measurement.result
        if (result != null && result !== placedResult) {
          collectSubtreeReads(result)
        }
      }
    }
  }

  /** Adds the state read anywhere in [result]'s subtree to this container's output. */
  private fun collectSubtreeReads(result: LithoLayoutResult) {
    result.layoutOutput.stateReads?.let { layoutOutput.addStateReads(it) }
    for (i in 0 until result.childCount) {
      collectSubtreeReads(result.getChildAt(i))
    }
  }

  /** A copy also takes this container's failure, so it is reported if the copy is placed. */
  override fun copyForPlacement(): LithoLayoutResult =
      super.copyForPlacement().also { (it as CustomLayoutResult).carryFailureFrom(this) }

  /** Takes [other]'s failure, for a copy of it that a second container places. */
  private fun carryFailureFrom(other: CustomLayoutResult) {
    failure = other.failure
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
 * Yoga's measure callback for a `Layout`: runs its layout function under the room Yoga offers, and
 * answers the size it declared, kept within that room. A failure answers 0x0 and is held on the
 * result, so nothing a layout function throws crosses into Yoga's native code.
 */
internal object CustomLayoutMeasureFunction : YogaMeasureFunction {

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
 * A `Layout`'s child, as its layout function sees it. Each measurement applies the child's own box
 * props within the constraints it was given and lays the child out as a tree of its own, so the
 * placeable it returns owns that layout.
 */
internal class ChildMeasurable(
    private val index: Int,
    private val child: LithoNode,
    private val context: LayoutContext<LithoLayoutContext>,
    private val owner: CustomLayoutResult,
) : Measurable {

  override fun measure(constraints: SizeConstraints): Placeable {
    // A child is measured only during a layout pass, whose context holds its measurements so far.
    val lsc = checkNotNull(context.renderContext)
    val measurements = lsc.cachedMeasurementsFor(child)
    val cached = measurements.firstOrNull { it.constraints == constraints }
    if (cached != null) {
      LithoStats.customLayoutAnswerReusedCount.incrementAndGet()
      return cached.forContainer(owner)
    }
    return layOut(constraints).also { measurements.add(it) }
  }

  private fun layOut(constraints: SizeConstraints): Placeable = StateReadRecorder.untracked {
    layOutChild(constraints)
  }

  private fun layOutChild(constraints: SizeConstraints): Placeable {
    LithoStats.customLayoutChildLayoutCount.incrementAndGet()
    val (result, box) =
        trace({ "layout.measureChild:${child.headComponent.simpleName}" }) {
          LithoYogaLayoutFunction.layoutSubtree(
              context,
              child,
              constraints,
              owner.previousDiffNode(index, child),
          )
        }
    val margins = box.margins
    // A child's margins are part of what it occupies, and a size below 0 means nothing to a layout
    // function, which would hand it on to `layout`.
    val measuredWidth = (result.width + margins.horizontal).coerceAtLeast(0)
    val measuredHeight = (result.height + margins.vertical).coerceAtLeast(0)
    return Placeable(
        width = measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
        height = measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight),
        childIndex = index,
        measuredWidth = measuredWidth,
        measuredHeight = measuredHeight,
        result = result,
        marginLeft = margins.left,
        marginTop = margins.top,
        owner = owner,
        constraints = constraints,
    )
  }
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
