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

import android.graphics.Point
import android.graphics.Rect
import android.util.Pair
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.ComponentsSystrace.traceWithArgs
import com.facebook.litho.YogaLayoutOutput.Companion.getYogaNode
import com.facebook.litho.YogaLayoutOutput.Companion.layoutDirection
import com.facebook.litho.animation.ReflowReadView
import com.facebook.litho.animation.reflowStateId
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.drawable.BorderColorDrawable
import com.facebook.litho.layout.BoxLayoutProps
import com.facebook.litho.layout.ChildBox
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.resolveChildBox
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateReadRecorder
import com.facebook.litho.stats.LithoStats
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.facebook.rendercore.utils.hasEquivalentFields
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaDisplay
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaUnit
import com.facebook.yoga.YogaValue

typealias YogaEdgeIntFunction = ((YogaEdge, Int) -> Unit)

typealias YogaEdgeFloatFunction = ((YogaEdge, Float) -> Unit)

/** Layout function for LithoNodes that lay out their children via Flexbox. */
internal object LithoYogaLayoutFunction {

  private val AUTO: YogaValue = checkNotNull(YogaValue.parse("auto"))

  /** Calculate the layout for a component using Yoga. */
  fun calculateLayout(
      context: LayoutContext<LithoLayoutContext>,
      sizeConstraints: SizeConstraints,
      lithoNode: LithoNode,
  ): LithoLayoutResult {

    applyOverridesRecursive(lithoNode)

    // Reflow collection is scoped to this invocation: a deferred node re-enters this function on
    // the SAME LithoLayoutContext (see Layout.measureDeferredNode), and each pass must pin and
    // capture only the nodes its own build walk collected — otherwise the nested pass re-captures
    // the outer tree's already-pinned nodes and records the pinned size as their natural.
    val lsc = context.renderContext
    val enclosingReflowResults = lsc?.reflowResults
    lsc?.reflowResults = null
    try {
      return calculateLayoutForYogaRoot(context, sizeConstraints, lithoNode)
    } finally {
      lsc?.reflowResults = enclosingReflowResults
    }
  }

  /**
   * Lays [node] out as a `Layout`'s child under [constraints]: a Yoga root of its own, in the box
   * the child's own box props resolve to within them. Those props are kept off its node, which
   * carries only what shapes its content, and the box is written onto the root instead. Its
   * direction was resolved with the tree, from its own or its ancestors'. [diffNode] is its
   * DiffNode from the last pass, which a root would otherwise look up as the whole tree's.
   *
   * Every call builds the subtree afresh, so each measurement owns the layout it returns.
   */
  internal fun layoutSubtree(
      context: LayoutContext<LithoLayoutContext>,
      node: LithoNode,
      constraints: SizeConstraints,
      diffNode: DiffNode?,
  ): kotlin.Pair<LithoLayoutResult, ChildBox> {
    // The `Layout`s in this child's Yoga tree, settled once it is laid out.
    val customLayouts = ArrayList<CustomLayoutResult>(0)
    val result =
        trace({ "layout.buildYogaTree:${node.headComponent.simpleName}" }) {
          buildYogaTree(
              context = context,
              currentNode = node,
              childBoxProps = BoxLayoutProps(),
              inContainer = true,
              rootDiffNode = diffNode,
              customLayouts = customLayouts,
          )
        }
    // Recorded as its tree was built, or carried by the result the cache copied.
    val boxProps = checkNotNull(result.layoutOutput.childBoxProps)
    val box = resolveChildBox(constraints, boxProps, node.layoutDirection.isRTL)
    result.layoutOutput.resolvedMargins = box.margins
    if (isLayoutLeaf(node)) {
      trace({ "layout.leaf:${node.headComponent.simpleName}" }) {
        layOutLeaf(context, result, box)
      }
      return result to box
    }
    if (result is CustomLayoutResult) {
      trace({ "layout.nestedContainer:${node.headComponent.simpleName}" }) {
        layOutNestedContainer(context, result, box)
      }
      return result to box
    }
    val yogaRoot = result.getYogaNode()
    trace({ "layout.yogaCalculateLayout:${node.headComponent.simpleName}" }) {
      yogaRoot.layOutWithin(box)
      settleCustomLayouts(context, customLayouts)
    }
    return result to box
  }

  /**
   * Lays a leaf out within [box] without a Yoga pass: Litho measures its content, as Yoga's measure
   * callback would, and the box is sized as Yoga sizes a leaf root. The geometry goes on its layout
   * output, which reads it in place of its Yoga node's.
   */
  private fun layOutLeaf(
      context: LayoutContext<LithoLayoutContext>,
      result: LithoLayoutResult,
      box: ChildBox,
  ) {
    val output = result.layoutOutput
    val direction =
        if (result.node.layoutDirection.isRTL) LayoutDirection.RTL else LayoutDirection.LTR
    // Set before measuring, with no size yet: a Primitive's layout scope reads the direction.
    output.boxGeometry = BoxGeometry(direction, box.padding, box.border, width = 0, height = 0)
    val size =
        boxSize(box) { widthSpec, heightSpec ->
          val content = measure(context, result, widthSpec, heightSpec)
          content.width to content.height
        }
    output.boxGeometry = BoxGeometry(direction, box.padding, box.border, size.width, size.height)
    LithoStats.customLayoutLeafCount.incrementAndGet()
  }

  /**
   * Lays a `Layout` out within [box] without a Yoga root, doing what a one-node Yoga tree would:
   * its function runs under the room Yoga's measure callback would get, inside the padding and
   * border; its declared size is kept within that room; and the box is sized as Yoga sizes a leaf
   * root, which applies the box's minimum and maximum. It is then placed in that box.
   */
  private fun layOutNestedContainer(
      context: LayoutContext<LithoLayoutContext>,
      result: CustomLayoutResult,
      box: ChildBox,
  ) {
    val output = result.layoutOutput
    val direction =
        if (result.node.layoutDirection.isRTL) LayoutDirection.RTL else LayoutDirection.LTR
    // Set before running the function, with no size yet: it reads the container's direction.
    output.boxGeometry = BoxGeometry(direction, box.padding, box.border, width = 0, height = 0)
    val size =
        boxSize(box) { widthSpec, heightSpec ->
          if (context.renderContext?.isFutureReleased == true) {
            return@boxSize 0 to 0
          }
          val answer =
              result.measure(context, SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
          YogaMeasureOutput.getWidth(answer).toInt() to YogaMeasureOutput.getHeight(answer).toInt()
        }
    output.boxGeometry = BoxGeometry(direction, box.padding, box.border, size.width, size.height)
    LithoStats.customLayoutNestedContainerCount.incrementAndGet()
    // Its tree holds only itself, so it is settled here rather than with a Yoga tree's.
    result.settle(context)
  }

  /** Settles each `Layout` in the Yoga tree just laid out, in the box Yoga gave it. */
  private fun settleCustomLayouts(
      context: LayoutContext<LithoLayoutContext>,
      containers: List<CustomLayoutResult>,
  ) {
    for (container in containers) {
      container.settle(context)
    }
  }

  private fun calculateLayoutForYogaRoot(
      context: LayoutContext<LithoLayoutContext>,
      sizeConstraints: SizeConstraints,
      lithoNode: LithoNode,
  ): LithoLayoutResult {
    // The `Layout`s in this Yoga tree, settled once it is laid out.
    val customLayouts = ArrayList<CustomLayoutResult>(0)
    val layoutResult: LithoLayoutResult =
        trace({ "layout.buildYogaTree:${lithoNode.headComponent.simpleName}" }) {
          buildYogaTree(context = context, currentNode = lithoNode, customLayouts = customLayouts)
        }
    val yogaRoot: YogaNode = layoutResult.getYogaNode()

    val widthSpec = sizeConstraints.toWidthSpec()
    val heightSpec = sizeConstraints.toHeightSpec()

    if (lithoNode.layoutDirection == LayoutDirection.RTL) {
      yogaRoot.setDirection(YogaDirection.RTL)
    }

    applyRootSizeConstraints(layoutResult, sizeConstraints)

    val width: Float =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          YogaConstants.UNDEFINED
        } else {
          SizeSpec.getSize(widthSpec).toFloat()
        }
    val height: Float =
        if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          YogaConstants.UNDEFINED
        } else {
          SizeSpec.getSize(heightSpec).toFloat()
        }

    layoutResult.layoutOutput.setSizeSpec(widthSpec, heightSpec)

    trace({ "layout.yogaCalculateLayout:${lithoNode.headComponent.simpleName}" }) {
      yogaRoot.calculateLayout(width, height)

      // Non-null only when the walk collected a reflow node that did not drive this pass.
      val lsc = context.renderContext
      val reflowResults = lsc?.reflowResults
      if (reflowResults != null) {
        var pinned = false
        val reflowView = lsc.reflowReadView
        for (i in reflowResults.indices) {
          if (pinAndCaptureReflowNode(reflowResults[i], reflowView, lsc)) pinned = true
        }
        if (pinned) {
          yogaRoot.calculateLayout(width, height)
        }
      }

      settleCustomLayouts(context, customLayouts)

      context.renderContext?.rootOffset =
          Point(
              yogaRoot.layoutX.toInt(),
              yogaRoot.layoutY.toInt(),
          )
    }

    return layoutResult
  }

  /**
   * Pins a single reflow node so a real commit never flashes the destination, and records its
   * natural size in [LithoLayoutContext.reflowTargetSizes] for mount-time retarget reconciliation.
   * Returns true if its Yoga node was pinned (so the caller re-runs the layout):
   * - new destination (natural != current target): pin each changed axis to the node's current
   *   on-screen size;
   * - still animating toward an unchanged destination (unrelated relayout, or before the first
   *   frame): pin to the current interpolated value so it does not jump to the target and back;
   * - settled (not animating, natural == target): leave it at its natural size.
   *
   * The natural size is recorded for EVERY collected node (not only changed ones), reusing the same
   * [LithoLayoutContext.reflowTargetSizes] map, so the main-thread mount can reconcile the retarget
   * decision against the live store: a background walk may observe a stale target here yet still
   * commit as the newest layout, so the walk-time read is only a pinning hint, not the decision.
   *
   * The "current on-screen size" comes from the reflow read view, keyed by `tailComponentKey`, so
   * it is stable across a sibling insert/remove — unlike `DiffNode.lastMeasured*`, which is aligned
   * by tree position and would hand this node its former neighbor's size after a structural change.
   */
  private fun pinAndCaptureReflowNode(
      result: LithoLayoutResult,
      reflowView: ReflowReadView,
      lsc: LithoLayoutContext,
  ): Boolean {
    val naturalWidth = result.width
    val naturalHeight = result.height
    val naturalSize = IntSize(naturalWidth, naturalHeight)
    val key = result.node.tailComponentKey
    val targetSizes =
        lsc.reflowTargetSizes
            ?: MutableScatterMap<GlobalKey, IntSize>(2).also { lsc.reflowTargetSizes = it }
    targetSizes[key] = naturalSize
    if (reflowView.targetSize(key) != naturalSize) {
      // currentSize is null only on the first-ever commit (no value seeded yet): nothing to pin
      // against, so let it lay out at its natural size.
      val currentSize = reflowView.currentSize(key) ?: return false
      val yogaNode = result.getYogaNode()
      var pinned = false
      if (currentSize.width != naturalWidth) {
        yogaNode.setWidth(currentSize.width.toFloat())
        pinned = true
      }
      if (currentSize.height != naturalHeight) {
        yogaNode.setHeight(currentSize.height.toFloat())
        pinned = true
      }
      return pinned
    } else {
      val sizeToPin = reflowView.sizeToPin(key) ?: return false
      val yogaNode = result.getYogaNode()
      yogaNode.setWidth(sizeToPin.width.toFloat())
      yogaNode.setHeight(sizeToPin.height.toFloat())
      return true
    }
  }

  private fun buildYogaTreeFromCache(
      context: LayoutContext<LithoLayoutContext>,
      cachedResult: LithoLayoutResult,
      diffNode: DiffNode?,
      skipTransitionCapture: Boolean = false,
  ): LithoLayoutResult = cloneCachedSubtree(
      context = context,
      cachedResult = cachedResult,
      // The root is the only node whose clone no parent supplies.
      clonedYogaNode = cachedResult.getYogaNode().cloneWithChildren(),
      currentDiffNode = diffNode,
      skipTransitionCapture = skipTransitionCapture,
  )

  /**
   * Rebuilds a subtree from the result a previous pass cached for it.
   *
   * A cached result belongs to the pass that produced it and cannot be handed out again, so every
   * [LithoLayoutResult] is copied and every [YogaNode] cloned alongside it.
   *
   * The clone for this node arrives as [clonedYogaNode] rather than being taken here, because
   * `cloneWithChildren` is a deep clone: cloning per call would re-clone every descendant at every
   * level and turn a linear walk quadratic. Each level instead hands its children the clones it
   * already made.
   */
  private fun cloneCachedSubtree(
      context: LayoutContext<LithoLayoutContext>,
      cachedResult: LithoLayoutResult,
      clonedYogaNode: YogaNode,
      currentDiffNode: DiffNode?,
      skipTransitionCapture: Boolean = false,
  ): LithoLayoutResult {
    val node: LithoNode = cachedResult.node
    val result: LithoLayoutResult = copyLayoutResult(cachedResult, node, clonedYogaNode)
    clonedYogaNode.data = Pair<Any, Any>(context, result)

    // Set the DiffNode on the cloned result so the measure cache in measureLithoNode can use
    // its cached specs/sizes when Yoga re-measures with changed constraints. Unlike applyDiffNode
    // (used in the non-cached path), this only sets the _diffNode reference — it does not call
    // shouldComponentUpdate or dirty the yoga node, which would cause unnecessary re-measurement.
    //
    // Type validation and isReleased checks are handled by findDiffNode, which resolved
    // currentDiffNode before this function was called. If the type didn't match or the context
    // was released, currentDiffNode is null and the entire subtree gets no DiffNode assignments.
    if (currentDiffNode != null) {
      result.layoutOutput._diffNode = currentDiffNode
    }

    saveLithoLayoutResultIntoCache(context, node, result, skipTransitionCapture)
    // Walk children by index without type validation. This is safe because cloneCachedSubtree is
    // only reached from buildYogaTreeFromCache, which requires !maybeDirty and an identity cache
    // hit — guaranteeing the component tree structure is identical to the DiffNode tree.
    for (i in 0 until cachedResult.childCount) {
      val childDiffNode: DiffNode? = currentDiffNode?.takeIf { i < it.childCount }?.getChildAt(i)

      val child: LithoLayoutResult = cloneCachedSubtree(
          context = context,
          cachedResult = cachedResult.getChildAt(i),
          clonedYogaNode = clonedYogaNode.getChildAt(i),
          currentDiffNode = childDiffNode,
          skipTransitionCapture = skipTransitionCapture,
      )
      result.addChild(child)
    }
    return result
  }

  /**
   * Saves a LithoLayoutResult into the LayoutCache, using the node itself and its ID as keys.
   *
   * [skipTransitionCapture] skips registering the node for a style transition that animates its
   * layout. A `Layout`'s children are laid out as trees of their own, which the transition does not
   * capture, so a node under one is skipped whether it is built afresh or copied from the cache.
   */
  private fun saveLithoLayoutResultIntoCache(
      context: LayoutContext<LithoLayoutContext>,
      node: LithoNode,
      result: LithoLayoutResult,
      skipTransitionCapture: Boolean = false,
  ) {
    val layoutCache: LayoutCache = context.layoutCache
    // TODO(T163437982): Refactor this to build the cache after layout calculation
    val cacheItem = LayoutCache.CacheItem(result, -1, -1)
    layoutCache.put(node, cacheItem)
    layoutCache.put(node.id.toLong(), cacheItem)

    if (!skipTransitionCapture && node.styleTransitionData?.spec?.shouldLayout == true) {
      context.renderContext?.prepareReflowNode(node, result)
    }
  }

  /**
   * Prepares a `Style.transition { bounds(reflow = true) }` node for this layout pass: subscribes
   * it to its animated size, then either applies this frame's interpolated size or enqueues the
   * node for post-layout capture.
   *
   * Runs from [saveLithoLayoutResultIntoCache], after the node's props have been written, so a size
   * this pass forces is applied on top of the node's declared one.
   */
  private fun LithoLayoutContext.prepareReflowNode(node: LithoNode, result: LithoLayoutResult) {
    val key = node.tailComponentKey
    val stateId = reflowStateId(reflowTreeId, key)

    // Subscribe to the reflow value's StateId so an animation frame re-runs this node's layout. The
    // value is minted on the main-thread commit, not here, and this must be recorded every layout:
    // LayoutState.stateReads is rebuilt from the layout output each pass.
    result.layoutOutput.addStateRead(stateId)

    if (isReflowAnimationFrameFor(stateId)) {
      // This node's own value drove the pass: apply the interpolated size so siblings and nested
      // content reflow with it, and skip the capture. The read view holds a live reference to the
      // AnimatedValue, so this is the current frame's value. It is null when the animation ended
      // while this layout was in flight, which leaves nothing to apply on top of whatever size the
      // node arrived with — so fall through and capture instead.
      val size = reflowReadView.currentSizeIfRunning(key)
      if (size != null) {
        val yogaNode = result.getYogaNode()
        yogaNode.setWidth(size.width.toFloat())
        yogaNode.setHeight(size.height.toFloat())
        return
      }
    }

    // The node may have been served from the layout cache — directly, or cloned as part of an
    // ancestor's subtree — still carrying a size an earlier pass forced onto it, from an animation
    // frame or from the post-layout pin. Undo that, or the capture below records it as natural.
    result.restoreDeclaredSize()

    // Enqueue for pin-and-capture once the tree has laid out. Every reflow node that did not drive
    // this pass is collected, changed or not, so the mount reconcile sees the natural size for all.
    (reflowResults ?: ArrayList<LithoLayoutResult>(2).also { reflowResults = it }).add(result)
  }

  /**
   * Rewrites the declared width and height onto this result's Yoga node, undoing a size an earlier
   * pass forced onto a node this one is reusing.
   *
   * Point and percent are rewritten unconditionally — Yoga's setters already no-op, dirty
   * propagation included, when the value is unchanged, so reading the current value back to compare
   * would only cost a JNI round trip and a [YogaValue] allocation per axis. An undeclared dimension
   * has to check first: it can only be handed back as auto, and Yoga compares unit as well as
   * value, so rewriting an already-undefined dimension would dirty the node and propagate on every
   * pass.
   */
  private fun LithoLayoutResult.restoreDeclaredSize() {
    val yogaNode = getYogaNode()
    val declaredWidth = layoutOutput.widthFromStyle
    when (declaredWidth.unit) {
      YogaUnit.POINT -> yogaNode.setWidth(declaredWidth.value)
      YogaUnit.PERCENT -> yogaNode.setWidthPercent(declaredWidth.value)
      else -> if (!yogaNode.width.isUndeclared()) yogaNode.setWidthAuto()
    }
    val declaredHeight = layoutOutput.heightFromStyle
    when (declaredHeight.unit) {
      YogaUnit.POINT -> yogaNode.setHeight(declaredHeight.value)
      YogaUnit.PERCENT -> yogaNode.setHeightPercent(declaredHeight.value)
      else -> if (!yogaNode.height.isUndeclared()) yogaNode.setHeightAuto()
    }
  }

  /**
   * Whether this Yoga dimension carries no size of its own. Yoga defaults a dimension to undefined
   * and `widthAuto()`/`heightAuto()` records no style metadata, so an undeclared dimension reads as
   * either; the two are interchangeable in layout, where every dimension check pairs `isAuto()`
   * with `isUndefined()`.
   */
  private fun YogaValue.isUndeclared(): Boolean =
      unit == YogaUnit.AUTO || unit == YogaUnit.UNDEFINED

  /**
   * Builds the YogaNode tree from this tree of LithoNodes while building the LayoutResult tree and
   * setting it in the data of the corresponding YogaNodes. Each `Layout` in it goes into
   * [customLayouts], to be settled once the tree is laid out.
   */
  private fun buildYogaTree(
      context: LayoutContext<LithoLayoutContext>,
      currentNode: LithoNode,
      parentNode: YogaNode? = null,
      childBoxProps: BoxLayoutProps? = null,
      inContainer: Boolean = false,
      rootDiffNode: DiffNode? = null,
      customLayouts: MutableList<CustomLayoutResult>,
  ): LithoLayoutResult {

    val renderContext: LithoLayoutContext? = context.renderContext
    checkNotNull(renderContext) { "RenderContext cannot be null when building YogaTree." }

    val scopesToUpdate = renderContext.scopesToUpdate
    val tailKey = currentNode.tailComponentKey
    val isDirty = tailKey in scopesToUpdate

    val layoutCache: LayoutCache = context.layoutCache
    var cacheItem: LayoutCache.CacheItem? = layoutCache[currentNode]
    val hasReflowFrameUpdateInDeferredSubtree =
        renderContext.reflowFrameUpdates != null &&
            currentNode is DeferredLithoNode &&
            renderContext.hasReflowFrameUpdateIn(
                // A Component.measure-backed holder does not own the resolved subtree's key.
                (cacheItem?.layoutResult as? LithoLayoutResult)
                    ?.layoutOutput
                    ?.actualDeferredNodeResult
                    ?.node
                    ?.tailComponentKey ?: currentNode.cachedNode?.tailComponentKey ?: tailKey,
            )
    val maybeDirty =
        isDirty || hasReflowFrameUpdateInDeferredSubtree || scopesToUpdate.hasDescendant(tailKey)

    var layoutResult: LithoLayoutResult? = null
    var yogaNode: YogaNode? = null

    // Resolve the DiffNode once — used by both the cached and non-cached paths. A `Layout`'s child
    // is a root of its own, so its container hands it the one it had.
    val diffNode: DiffNode? =
        if (childBoxProps != null) validDiffNode(renderContext, currentNode, rootDiffNode)
        else findDiffNode(renderContext, currentNode, parentNode)

    // A `Layout`'s result holds the layouts its function placed, which no Yoga node beneath it
    // does, so it is never cached, and a cached subtree holding one is never copied whole. Any
    // other node is reused from its own last layout, so in the role it had then: a `Layout`'s child
    // with its box props on its result, a flexbox child with them on its node.
    val isCustomLayout = currentNode.layoutFunction != null
    val usesCache = !isCustomLayout
    if (
        !usesCache ||
            (cacheItem?.layoutResult as? LithoLayoutResult)?.subtreeHasCustomLayout == true
    ) {
      cacheItem = null
    }
    if (cacheItem != null && !maybeDirty) {
      val cachedLayoutResult: LayoutResult = cacheItem.layoutResult
      return trace({ "layout.buildYogaTreeFromCache:${currentNode.headComponent.simpleName}" }) {
        // We can fully reuse the Yoga tree.
        buildYogaTreeFromCache(
            context,
            cachedLayoutResult as LithoLayoutResult,
            diffNode,
            skipTransitionCapture = inContainer,
        )
      }
    }

    cacheItem =
        if (usesCache) {
          layoutCache.get<LayoutCache.CacheItem>(currentNode.id.toLong())
        } else {
          null
        }
    if (cacheItem != null && (!isDirty || hasReflowFrameUpdateInDeferredSubtree)) {
      val cachedLayoutResult = cacheItem.layoutResult as LithoLayoutResult

      // We can partially reuse the Yoga tree.
      val clonedNode: YogaNode = cachedLayoutResult.getYogaNode().cloneWithoutChildren()
      yogaNode = clonedNode
      layoutResult = copyLayoutResult(cachedLayoutResult, currentNode, clonedNode)
      if (hasReflowFrameUpdateInDeferredSubtree) {
        // Keep the resolved subtree so the frame reruns Yoga without resolving the boundary.
        layoutResult.layoutOutput._actualDeferredNodeResult =
            cachedLayoutResult.layoutOutput.actualDeferredNodeResult
        clonedNode.dirty()
      } else if (currentNode is DeferredLithoNode && maybeDirty) {
        // A deferred node is a Yoga leaf, so a dirty scope in its resolved subtree cannot dirty it
        // the way a new child dirties a container. Left clean, Yoga keeps the stale measurement and
        // the subtree is re-resolved at that exact size, so it can never grow or shrink.
        clonedNode.dirty()
      }
    }

    if (layoutResult == null) {
      val writer: YogaLayoutProps = currentNode.createYogaNodeWriter()
      // A `Layout`'s child keeps its box props off its node: its container applies them.
      val props: LayoutProps =
          if (childBoxProps != null) {
            val isDeferred = currentNode is DeferredLithoNode
            ContainerChildLayoutProps(
                box = BoxLayoutPropsRecorder(childBoxProps, recordsOwnEdges = !isDeferred),
                content = writer,
                isDeferred = isDeferred,
            )
          } else {
            writer
          }

      // Transfer the layout props to the YogaNode.
      if (currentNode is DeferredLithoNode) {
        currentNode.writeDeferredNodePropsToYogaNode(
            context,
            writer as DeferredNodeYogaLayoutProps,
            props,
        )
      } else if (currentNode !is NullNode) {
        currentNode.writeNodePropsToYogaNode(context, writer, props)
      }
      yogaNode = writer.node

      // Ideally, the layout data should be created when measure is called on the mount spec or
      // primitive component. However, because of the current implementation of mount specs and the
      // way Yoga works, measure may not be called. A MountSpec may require inter-stage props, so it
      // is necessary to have a non-null InterStagePropsContainer even if the values are
      // uninitialised. Otherwise, this will lead to NPEs.
      //
      // This should get cleaned up once the implementation is general enough for
      // PrimitiveComponents.
      val layoutData =
          if (currentNode.tailComponent is SpecGeneratedComponent) {
            (currentNode.tailComponent as SpecGeneratedComponent).createInterStagePropsContainer()
          } else {
            null
          }

      layoutResult =
          currentNode.createLayoutResult(
              layoutOutput =
                  YogaLayoutOutput(
                      yogaNode = yogaNode,
                      widthFromStyle = writer.widthFromStyle,
                      heightFromStyle = writer.heightFromStyle,
                      minWidthFromStyle = writer.minWidthFromStyle,
                      maxWidthFromStyle = writer.maxWidthFromStyle,
                      minHeightFromStyle = writer.minHeightFromStyle,
                      maxHeightFromStyle = writer.maxHeightFromStyle,
                      _layoutData = layoutData,
                  )
                      .also { output ->
                        val preLayoutStateReads = writer.stateReads
                        if (preLayoutStateReads != null) output.addStateReads(preLayoutStateReads)
                        output.childBoxProps = childBoxProps
                      },
          )
    }

    checkNotNull(yogaNode) { "YogaNode cannot be null when building YogaTree." }

    yogaNode.data = Pair(context, layoutResult)
    if (diffNode != null) {
      applyDiffNode(renderContext, currentNode, yogaNode, diffNode, isDirty)
    }

    // A `Layout` is a leaf to Yoga: its measure callback runs its layout function, which lays its
    // children out as trees of their own, and it is placed once this tree is laid out. It
    // contributes no automatic minimum, as no leaf does by default.
    if (layoutResult is CustomLayoutResult) {
      layoutResult.subtreeHasCustomLayout = true
      yogaNode.setMeasureFunction(CustomLayoutMeasureFunction)
      yogaNode.setMinContentWidth(0f)
      yogaNode.setMinContentHeight(0f)
      customLayouts.add(layoutResult)
      return layoutResult
    }

    saveLithoLayoutResultIntoCache(
        context,
        currentNode,
        layoutResult,
        skipTransitionCapture = inContainer,
    )

    for (i in 0 until currentNode.childCount) {
      val childLayoutResult: LithoLayoutResult = buildYogaTree(
          context = context,
          currentNode = currentNode.getChildAt(i),
          parentNode = yogaNode,
          inContainer = inContainer,
          customLayouts = customLayouts,
      )

      yogaNode.addChildAt(childLayoutResult.getYogaNode(), yogaNode.childCount)
      layoutResult.addChild(childLayoutResult)
      if (childLayoutResult.subtreeHasCustomLayout) {
        layoutResult.subtreeHasCustomLayout = true
      }
    }

    return layoutResult
  }

  /**
   * [diffNode], handed over by a `Layout` for its child, if it is for the same kind of component:
   * the check [findDiffNode] applies to one it looks up.
   */
  private fun validDiffNode(
      renderContext: LithoLayoutContext,
      currentNode: LithoNode,
      diffNode: DiffNode?,
  ): DiffNode? {
    if (diffNode == null || renderContext.isReleased) {
      return null
    }
    return diffNode.takeIf {
      ComponentUtils.isSameComponentType(currentNode.tailComponent, it.component) ||
          Component.isLayoutSpecWithSizeSpec(currentNode.tailComponent)
    }
  }

  /**
   * Applies the root size constraints (such as min/max/exact width and height) to the YogaNode tree
   * before layout calculation. This ensures that the root node respects the provided size
   * constraints during layout.
   *
   * @param layoutResult The root layout result containing the YogaNode to constrain.
   * @param sizeConstraints The size constraints to apply to the root node.
   */
  private fun applyRootSizeConstraints(
      layoutResult: LithoLayoutResult,
      sizeConstraints: SizeConstraints,
  ) {
    val yogaNode: YogaNode = layoutResult.getYogaNode()
    val output = layoutResult.layoutOutput

    // Reset to original style values
    // For new nodes: no-op since current == style.
    // For cached nodes: restore values that may have been modified by a previous layout.

    // Width
    if (output.widthFromStyle != yogaNode.width) {
      when (output.widthFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setWidthPercent(output.widthFromStyle.value)
        YogaUnit.POINT -> yogaNode.setWidth(output.widthFromStyle.value)
        else -> yogaNode.setWidthAuto()
      }
    }

    // Height
    if (output.heightFromStyle != yogaNode.height) {
      when (output.heightFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setHeightPercent(output.heightFromStyle.value)
        YogaUnit.POINT -> yogaNode.setHeight(output.heightFromStyle.value)
        else -> yogaNode.setHeightAuto()
      }
    }

    // Min Width
    if (output.minWidthFromStyle != yogaNode.minWidth) {
      when (output.minWidthFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setMinWidthPercent(output.minWidthFromStyle.value)
        YogaUnit.POINT -> yogaNode.setMinWidth(output.minWidthFromStyle.value)
        else -> yogaNode.setMinWidth(AUTO.value)
      }
    }

    // Max Width
    if (output.maxWidthFromStyle != yogaNode.maxWidth) {
      when (output.maxWidthFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setMaxWidthPercent(output.maxWidthFromStyle.value)
        YogaUnit.POINT -> yogaNode.setMaxWidth(output.maxWidthFromStyle.value)
        else -> yogaNode.setMaxWidth(AUTO.value)
      }
    }

    // Min Height
    if (output.minHeightFromStyle != yogaNode.minHeight) {
      when (output.minHeightFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setMinHeightPercent(output.minHeightFromStyle.value)
        YogaUnit.POINT -> yogaNode.setMinHeight(output.minHeightFromStyle.value)
        else -> yogaNode.setMinHeight(AUTO.value)
      }
    }

    // Max Height
    if (output.maxHeightFromStyle != yogaNode.maxHeight) {
      when (output.maxHeightFromStyle.unit) {
        YogaUnit.PERCENT -> yogaNode.setMaxHeightPercent(output.maxHeightFromStyle.value)
        YogaUnit.POINT -> yogaNode.setMaxHeight(output.maxHeightFromStyle.value)
        else -> yogaNode.setMaxHeight(AUTO.value)
      }
    }

    // Apply root size constraints
    // Note: Yoga doesn't have the concept of AT_MOST constraints like Android's MeasureSpec.
    // We convert AT_MOST to max width/height constraints to bound the layout.
    if (YogaConstants.isUndefined(yogaNode.width.value)) {
      if (sizeConstraints.hasExactWidth) {
        // Width is specified exactly, so set it directly.
        yogaNode.setWidth(sizeConstraints.maxWidth.toFloat())
      } else if (
          sizeConstraints.minWidth > 0 || sizeConstraints.maxWidth != SizeConstraints.Infinity
      ) {
        // Width has an upper bound.
        val maxWidth = sizeConstraints.maxWidth.toFloat()
        if (maxWidth < SizeConstraints.Infinity) {
          val yogaMaxWidth = yogaNode.maxWidth
          if (YogaConstants.isUndefined(yogaMaxWidth.value)) {
            // No existing max width constraint, so apply the size constraint.
            yogaNode.setMaxWidth(maxWidth)
          } else {
            // Leave as it is to let Yoga resolve the constraints
          }
        }

        // Width has a lower bound.
        val minWidth = sizeConstraints.minWidth.toFloat()
        if (minWidth > 0) {
          val yogaMinWidth = yogaNode.minWidth
          if (YogaConstants.isUndefined(yogaMinWidth.value)) {
            // No existing min width constraint, so apply the size constraint.
            yogaNode.setMinWidth(minWidth)
          } else {
            // Leave as it is to let Yoga resolve the constraints
          }
        }
      } else {
        // Width is unbounded, so set it to auto.
        yogaNode.setWidthAuto()
      }
    }

    if (YogaConstants.isUndefined(yogaNode.height.value)) {
      if (sizeConstraints.hasExactHeight) {
        // Height is specified exactly, so set it directly.
        yogaNode.setHeight(sizeConstraints.maxHeight.toFloat())
      } else if (
          sizeConstraints.minHeight > 0 || sizeConstraints.maxHeight != SizeConstraints.Infinity
      ) {
        // Height has an upper bound.
        val maxHeight = sizeConstraints.maxHeight.toFloat()
        if (maxHeight < SizeConstraints.Infinity) {
          val yogaMaxHeight = yogaNode.maxHeight
          if (YogaConstants.isUndefined(yogaMaxHeight.value)) {
            // No existing max height constraint, so apply the size constraint.
            yogaNode.setMaxHeight(maxHeight)
          } else {
            // Leave as it is to let Yoga resolve the constraints
          }
        }

        // Height has a lower bound.
        val minHeight = sizeConstraints.minHeight.toFloat()
        if (minHeight > 0) {
          val yogaMinHeight = yogaNode.minHeight
          if (YogaConstants.isUndefined(yogaMinHeight.value)) {
            // No existing min height constraint, so apply the size constraint.
            yogaNode.setMinHeight(minHeight)
          } else {
            // Leave as it is to let Yoga resolve the constraints
          }
        }
      } else {
        // Height is unbounded, so set it to auto.
        yogaNode.setHeightAuto()
      }
    }
  }

  private fun applyOverridesRecursive(node: LithoNode) {
    if (LithoDebugConfigurations.isDebugModeEnabled) {
      DebugComponent.applyOverrides(node.tailComponentContext, node)
      for (i in 0 until node.childCount) {
        applyOverridesRecursive(node.getChildAt(i))
      }
    }
  }

  /**
   * Finds the DiffNode corresponding to [currentNode] by looking it up in the parent's DiffNode
   * (for children) or from the layout context's diff tree root (for the root node).
   *
   * Returns null if the context is released, no DiffNode is found at the expected position, or the
   * component type doesn't match (unless it's a child deferred node whose type is expected to
   * differ). When null is returned due to type mismatch, the entire subtree should be treated as
   * having no DiffNode — child indices are no longer reliable.
   *
   * Called once in [buildYogaTree] and the result is passed to both [buildYogaTreeFromCache]
   * (cached path) and [applyDiffNode] (non-cached path).
   */
  private fun findDiffNode(
      renderContext: LithoLayoutContext,
      currentNode: LithoNode,
      parentYogaNode: YogaNode?,
  ): DiffNode? {
    if (renderContext.isReleased) {
      return null
    }
    val diffNode: DiffNode? =
        if (parentYogaNode == null) {
          // Root: use the diff tree root, or the deferred node's DiffNode
          if (
              Component.isLayoutSpecWithSizeSpec(currentNode.headComponent) &&
                  renderContext.hasDiffNodeSetForDeferredNode()
          ) {
            renderContext.consumeDiffNodeForDeferredNode()
          } else {
            renderContext.currentDiffTree
          }
        } else {
          // Child: look up from the parent's already-resolved DiffNode
          val parentResult = getLayoutResultFromYogaNode(parentYogaNode)
          val parentDiffNode = parentResult.diffNode ?: return null
          val index = parentResult.node.getChildIndex(currentNode)
          if (index != -1 && index < parentDiffNode.childCount) {
            parentDiffNode.getChildAt(index)
          } else {
            null
          }
        }
    if (diffNode == null) {
      return null
    }
    // Validate that the component type matches. Allow type mismatch for child deferred nodes
    // (LayoutSpecWithSizeSpec) because the DiffNode tree makes them transparent — the DiffNode
    // at this position may have the resolved component's type rather than the placeholder's type.
    val component = currentNode.tailComponent
    val isChildDeferredNode =
        parentYogaNode != null && Component.isLayoutSpecWithSizeSpec(component)
    return if (
        ComponentUtils.isSameComponentType(component, diffNode.component) || isChildDeferredNode
    ) {
      diffNode
    } else {
      null
    }
  }

  /**
   * Applies the given [DiffNode] to the layout result at [currentYogaNode], performing equivalence
   * checks and copying inter-stage props for equivalent components.
   *
   * The DiffNode should already be type-validated by [findDiffNode] before calling this method.
   */
  private fun applyDiffNode(
      current: LithoLayoutContext,
      currentNode: LithoNode,
      currentYogaNode: YogaNode,
      diff: DiffNode,
      isDirty: Boolean,
  ) {
    val result: LithoLayoutResult = getLayoutResultFromYogaNode(currentYogaNode)
    val yogaOutput: YogaLayoutOutput = result.layoutOutput

    val component: Component = currentNode.tailComponent

    yogaOutput._diffNode = diff

    trace({ "layout.shouldRemeasure:${currentNode.headComponent.simpleName}" }) {
      val haveEquivalentProps =
          if (currentNode.primitive != null) {
            allNotNull(currentNode.primitive?.layoutBehavior, diff.primitive?.layoutBehavior) {
                layoutBehavior1,
                layoutBehavior2 ->
              !isDirty && layoutBehavior1.isEquivalentTo(layoutBehavior2)
            } == true
          } else {
            // For MountSpecs, skip the isDirty flag check and rely solely on shouldComponentUpdate
            // to preserve backward-compatible update behavior.
            (!LayoutUtils.shouldComponentUpdate(currentNode, diff)).also { equivalent ->
              if (equivalent && isDirty) {
                DebugInfoReporter.report(
                    "LayoutEquivalenceMismatch",
                    renderStateId = current.treeId,
                ) {
                  put(DebugEventAttribute.Name, component.fullyQualifiedName)
                  put("shouldComponentUpdate", false)
                  put("isDirty", true)
                }
              }
            }
          }
      // A layout may depend on its direction (Text resolves alignment from it), and it isn't part
      // of
      // the props, so a measurement from the other direction can't be reused.
      val isEquivalent = haveEquivalentProps && diff.layoutDirection == currentNode.layoutDirection
      val isMountSpecWithMeasure = component.canMeasure() && Component.isMountSpec(component)
      if (isMountSpecWithMeasure && !isEquivalent) {
        yogaOutput.yogaNode.dirty()
      }
      yogaOutput._cachedMeasuresValid = isEquivalent

      if (currentNode.primitive != null) {
        yogaOutput._layoutData = diff.layoutData
      } else if (isEquivalent) {
        val scopedComponentInfo = currentNode.tailScopedComponentInfo
        val diffNodeScopedComponentInfo = checkNotNull(diff.scopedComponentInfo)
        if (component is SpecGeneratedComponent) {
          component.copyInterStageImpl(
              result.layoutData as InterStagePropsContainer?,
              diff.layoutData as InterStagePropsContainer?,
          )
          component.copyPrepareInterStageImpl(
              scopedComponentInfo.prepareInterStagePropsContainer,
              diffNodeScopedComponentInfo.prepareInterStagePropsContainer,
          )
        }
      }
    }
  }

  fun measure(
      context: LayoutContext<LithoLayoutContext>,
      layoutResult: LithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int,
  ): MeasureResult {
    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }

    var size: MeasureResult
    val yogaOutput = layoutResult.layoutOutput
    yogaOutput._wasMeasured = true
    if (renderContext.isFutureReleased) {

      // If the layout is released, skip measurement.
      size = MeasureResult.error()
    } else {
      val component = layoutResult.node.tailComponent
      traceWithArgs(
          { "measure:${component.simpleName}" },
          { scope ->
            scope.attribute("widthSpec", SizeSpec.toString(widthSpec))
            scope.attribute("heightSpec", SizeSpec.toString(heightSpec))
            scope.attribute("componentId", component.instanceId)
          },
      ) {
        try {
          size =
              if (layoutResult is DeferredLithoLayoutResult) {
                measureDeferredNode(context, widthSpec, heightSpec, layoutResult)
              } else {
                measureLithoNode(context, widthSpec, heightSpec, layoutResult)
              }

          check(!(size.width < 0 || size.height < 0)) {
            ("MeasureOutput not set, Component is: $component " +
                "WidthSpec: ${MeasureSpecUtils.getMeasureSpecDescription(widthSpec)} " +
                "HeightSpec: ${MeasureSpecUtils.getMeasureSpecDescription(heightSpec)} " +
                "Measured width : ${size.width} " +
                "Measured Height: ${size.height}")
          }
        } catch (e: Exception) {

          // Handle the exception.
          ComponentUtils.handle(layoutResult.node.tailComponentContext, e)

          // If the exception is handled, return a zero size to continue layout.
          size = MeasureResult.error()
        }
      }
    }

    // Record the last measured width and height specs.
    yogaOutput.setSizeSpec(widthSpec, heightSpec)

    // If the size of a cached layout has changed, clear size-dependent render units.
    if (
        layoutResult.isCachedLayout &&
            (layoutResult.contentWidth != size.width || layoutResult.contentHeight != size.height)
    ) {
      yogaOutput._backgroundRenderUnit = null
      yogaOutput._foregroundRenderUnit = null
      yogaOutput._borderRenderUnit = null
    }
    yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(size.width, size.height)

    yogaOutput._measureHadExceptions = size.hadExceptions
    return size
  }

  fun onBoundsDefined(layoutResult: LithoLayoutResult) {
    val context = layoutResult.node.tailComponentContext
    val component = layoutResult.node.tailComponent
    val yogaOutput = layoutResult.layoutOutput
    val hasLayoutSizeChanged: Boolean

    // Since `measure` is called without padding and border sizes, save the size without padding and
    // borders to align with layout diffing (DiffNode), which relies directly on the last measured
    // size.
    val newContentWidth =
        (layoutResult.width -
                layoutResult.paddingRight -
                layoutResult.paddingLeft -
                layoutResult.borderRight -
                layoutResult.borderLeft)
            .coerceAtLeast(0)
    val newContentHeight =
        (layoutResult.height -
                layoutResult.paddingTop -
                layoutResult.paddingBottom -
                layoutResult.borderTop -
                layoutResult.borderBottom)
            .coerceAtLeast(0)
    if (Component.isMountSpec(component) && component is SpecGeneratedComponent) {

      hasLayoutSizeChanged =
          if (
              (layoutResult.lastMeasuredSize != Long.MIN_VALUE) &&
                  !layoutResult.wasMeasured &&
                  layoutResult.isCachedLayout
          ) {
            // Two scenarios would skip measurement and fall into this case:
            // 1. A cached result from Yoga (which may also contain a fixed size).
            // 2. A fixed-size result without measurement whose size might change.
            (newContentWidth != layoutResult.contentWidth) ||
                (newContentHeight != layoutResult.contentHeight)
          } else {
            true
          }

      if (hasLayoutSizeChanged) {

        // We should only invoke `onBoundsDefined` if the layout is not cached or its size has
        // changed. Note: MountSpec is always treated as having changed size once `onMeasure` is
        // invoked, regardless of whether the size changed.
        trace({ "layout.onBoundsDefined:${component.getSimpleName()}" }) {
          val layoutData: InterStagePropsContainer?
          // If the LayoutResult was cached but the size has changed, then inter-stage props
          // container (layout data) could be mutated when @OnBoundsDefined is invoked. To avoid
          // that, create a new inter-stage props container (layout data) and copy over the current
          // values.
          if (layoutResult.isCachedLayout || layoutResult.isDiffedLayout) {
            layoutData = component.createInterStagePropsContainer()
            if (layoutData != null && layoutResult.layoutData != null) {
              component.copyInterStageImpl(
                  layoutData,
                  layoutResult.layoutData as InterStagePropsContainer?,
              )
            }
          } else {
            layoutData = layoutResult.layoutData as InterStagePropsContainer?
          }
          try {
            component.onBoundsDefined(
                context,
                SpecGeneratedComponentLayout(
                    layoutOutput = layoutResult.layoutOutput,
                    paddingSet = layoutResult.node.isPaddingSet,
                    backgroundDrawable = layoutResult.node.background,
                ),
                layoutData,
            )
          } catch (e: Exception) {
            ComponentUtils.handleWithHierarchy(context, component, e)
            yogaOutput._measureHadExceptions = true
          }

          // If the layout data has changed, the content render unit should be recreated.
          if (!hasEquivalentFields(layoutResult.layoutData, layoutData)) {
            yogaOutput._contentRenderUnit = null
            yogaOutput._layoutData = layoutData
          }
        }
      }
      if (!layoutResult.wasMeasured) {
        yogaOutput.setSizeSpec(
            MeasureSpecUtils.exactly(newContentWidth),
            MeasureSpecUtils.exactly(newContentHeight),
        )
        yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    } else if (Component.isPrimitive(component)) {

      hasLayoutSizeChanged =
          newContentWidth != layoutResult.contentWidth ||
              newContentHeight != layoutResult.contentHeight

      if (layoutResult.delegate == null || hasLayoutSizeChanged) {

        // Check whether to run measure for a Primitive that was skipped due to a fixed size.
        val layoutContext = getLayoutContextFromYogaNode(layoutResult.getYogaNode())
        measure(
            layoutContext,
            layoutResult,
            MeasureSpecUtils.exactly(newContentWidth),
            MeasureSpecUtils.exactly(newContentHeight),
        )
      }
    } else {
      hasLayoutSizeChanged =
          (layoutResult.lastMeasuredSize == Long.MIN_VALUE) ||
              (layoutResult.isCachedLayout &&
                  (layoutResult.contentWidth != newContentWidth ||
                      layoutResult.contentHeight != newContentHeight))
      if (hasLayoutSizeChanged) {
        yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    }

    // Reuse or recreate additional outputs. Outputs are recreated if the size has changed
    if (layoutResult.contentRenderUnit == null) {
      yogaOutput._contentRenderUnit =
          LithoNodeUtils.createContentRenderUnit(
              layoutResult.node,
              layoutResult.cachedMeasuresValid,
              layoutResult.diffNode,
          )
      adjustRenderUnitBounds(layoutResult)
    } else if (!Component.isPrimitive(component)) {
      // A content unit reused from the last pass kept the bounds it was inset by then, but the
      // padding can change with the size (a percentage of the width). A Primitive whose size
      // changed is measured again, which makes a new unit.
      adjustRenderUnitBounds(layoutResult)
    }
    if (layoutResult.hostRenderUnit == null) {
      yogaOutput._hostRenderUnit = LithoNodeUtils.createHostRenderUnit(layoutResult.node)
    }
    if (layoutResult.backgroundRenderUnit == null || hasLayoutSizeChanged) {
      yogaOutput._backgroundRenderUnit =
          LithoNodeUtils.createBackgroundRenderUnit(
              layoutResult.node,
              layoutResult.width,
              layoutResult.height,
              layoutResult.diffNode,
          )
    }
    if (layoutResult.foregroundRenderUnit == null || hasLayoutSizeChanged) {
      yogaOutput._foregroundRenderUnit =
          LithoNodeUtils.createForegroundRenderUnit(
              layoutResult.node,
              layoutResult.width,
              layoutResult.height,
              layoutResult.diffNode,
          )
    }
    if (
        layoutResult.shouldDrawBorders() &&
            (layoutResult.borderRenderUnit == null || hasLayoutSizeChanged)
    ) {
      yogaOutput._borderRenderUnit =
          LithoNodeUtils.createBorderRenderUnit(
              layoutResult.node,
              createBorderColorDrawable(layoutResult),
              layoutResult.width,
              layoutResult.height,
              layoutResult.diffNode,
          )
    }
  }

  /**
   * Measures a component, reusing the previous measurement from [DiffNode] if the specs match
   * exactly.
   *
   * ## Measure cache behavior
   *
   * The cache reuses the previous result only when `lastWidthSpec == widthSpec && lastHeightSpec ==
   * heightSpec`. This is a strict equality check — any change in the spec value forces
   * re-measurement, even when the result would be identical.
   *
   * This is stricter than necessary. Yoga's internal `YGNodeCanUseCachedMeasurement` implements
   * **spec-compatible** matching: if the previous measured size fits within the new constraint, the
   * cached result is reused.
   *
   * ### When strict equality causes unnecessary re-measurement
   *
   * When a parent's dimension is animated, Yoga passes a new AT_MOST constraint to children every
   * frame. Components whose output doesn't depend on the AT_MOST value (e.g., text that wraps based
   * on width and is far shorter than the height constraint) produce identical results but are
   * re-measured because the specs differ.
   *
   * ### Spec-compatible matching and its edge cases
   *
   * A spec-compatible check would compare the previous *measured size* against the new *constraint*
   * rather than comparing specs directly.
   *
   * Compatibility rules (can skip re-measurement when):
   * - **EXACTLY → EXACTLY**: `oldSize == newSize`
   * - **AT_MOST → AT_MOST**: `measuredSize < min(oldSize, newSize)`
   * - **Any mode change**: never — must re-measure
   *
   * How these rules handle different component behaviors (AT_MOST → AT_MOST):
   * - **Intrinsic size** (e.g., text h=53, AT_MOST 1000→1200): `53 < 1000` → skip. Correct — text
   *   doesn't depend on the constraint.
   * - **Fills constraint** (e.g., scroll h=1000, AT_MOST 1000→1200): `1000 < 1000` is false →
   *   re-measure. Correct — the scroll would grow to 1200.
   * - **Coincidence** (e.g., component naturally h=1000, AT_MOST 1000→1200): `1000 < 1000` is false
   *   → re-measure. Conservative — the result would be the same (h=1000), but we can't distinguish
   *   this from the fills-constraint case without knowing the component's preferred size.
   * - **Fraction of constraint** (e.g., `h = maxHeight / 2` = 500, AT_MOST 1000→1200): `500 < 1000`
   *   → skip. **Wrong** — the result should be 600. No external cache can detect this; such
   *   components should override [Component.shouldAlwaysRemeasure].
   *
   * Key design decisions:
   * - **Strict `<` (not `<=`)**: If `measuredSize == constraintSize`, the component may have been
   *   clamped — it wanted to be larger but was limited by the constraint. A different constraint
   *   could produce a different result.
   * - **No cross-mode transitions** (e.g., EXACTLY → AT_MOST, AT_MOST → EXACTLY): Under EXACTLY,
   *   the component is forced to a specific size. Under AT_MOST, it can choose freely up to the
   *   limit. A component may prefer a completely different size when the mode changes, so the
   *   cached result from one mode is not reliable for a different mode.
   *
   * Edge cases that no external cache can handle:
   * - **Fraction of constraint**: A component that computes its size as a fraction of available
   *   space (e.g., `height = maxHeight / 2`) measures smaller than the constraint but would produce
   *   a different result under a different constraint. Such components should override
   *   [Component.shouldAlwaysRemeasure].
   * - **Layout data depends on constraint**: A component whose measured size is the same but whose
   *   [LithoLayoutOutput.layoutData] would differ under new constraints. Rare but possible with
   *   custom primitives.
   */
  private fun measureLithoNode(
      context: LayoutContext<LithoLayoutContext>,
      widthSpec: Int,
      heightSpec: Int,
      lithoLayoutResult: LithoLayoutResult,
  ): MeasureResult {
    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }

    val node: LithoNode = lithoLayoutResult.node
    val component: Component = node.tailComponent
    val componentScopedContext: ComponentContext = node.tailComponentContext
    val isReadTrackingEnabled = componentScopedContext.isReadTrackingEnabled
    val yogaOutput: YogaLayoutOutput = lithoLayoutResult.layoutOutput
    val diffNode: DiffNode? =
        if (lithoLayoutResult.cachedMeasuresValid) lithoLayoutResult.diffNode else null
    val width: Int
    val height: Int
    val delegate: LayoutResult?
    val layoutData: Any?
    val effects: List<Attachable>?

    // If diff node is set, check if measurements from the previous pass can be reused.
    val specsMatchExactly =
        diffNode?.lastWidthSpec == widthSpec && diffNode.lastHeightSpec == heightSpec
    val specsAreCompatible =
        diffNode != null &&
            isSpecCompatible(diffNode.lastWidthSpec, widthSpec, diffNode.lastMeasuredWidth) &&
            isSpecCompatible(diffNode.lastHeightSpec, heightSpec, diffNode.lastMeasuredHeight)
    if (
        diffNode != null &&
            !shouldAlwaysRemeasure(component) &&
            (specsMatchExactly || specsAreCompatible)
    ) {
      width = diffNode.lastMeasuredWidth
      height = diffNode.lastMeasuredHeight
      layoutData = diffNode.layoutData
      delegate = diffNode.delegate
      yogaOutput._isDiffedLayout = true
      effects = diffNode.effects
      if (isReadTrackingEnabled) {
        diffNode.stateReads?.let { yogaOutput.addStateReads(it) }
      }

      // Measure the component
    } else {
      trace({ "layout.onMeasure:${component.simpleName}" }) {
        val stateReads =
            StateReadRecorder.record(
                renderContext.treeId,
                attribution("layout", component),
            ) {
              val contentLayout = measureContentLayout(
                  context,
                  lithoLayoutResult,
                  widthSpec,
                  heightSpec,
              )
              width = contentLayout.width
              height = contentLayout.height
              delegate = contentLayout.delegate
              effects = contentLayout.effects
              val newLayoutData = contentLayout.layoutData

              // If layout data has changed, the content render unit should be recreated.
              if (!hasEquivalentFields(lithoLayoutResult.layoutData, newLayoutData)) {
                layoutData = newLayoutData
                yogaOutput._contentRenderUnit = null
              } else {
                layoutData = lithoLayoutResult.layoutData
              }
            }
        if (isReadTrackingEnabled) {
          yogaOutput.addStateReads(stateReads)
        }
      }
    }
    yogaOutput._delegate = delegate
    yogaOutput._layoutData = layoutData
    yogaOutput._effects = effects
    return MeasureResult(width, height, layoutData)
  }

  /**
   * Pure leaf measurement for [measureLithoNode]. Writes nothing to [YogaLayoutOutput], the diff
   * cache, or state-reads — callers own that.
   */
  private fun measureContentLayout(
      context: LayoutContext<LithoLayoutContext>,
      lithoLayoutResult: LithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int,
  ): LeafMeasureResult {
    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }
    val node: LithoNode = lithoLayoutResult.node
    val component: Component = node.tailComponent
    val componentScopedContext: ComponentContext = node.tailComponentContext
    val primitive = node.primitive

    if (primitive != null) {
      val contextForLayoutScope = LithoExtraContextForLayoutScope(
          componentContext = componentScopedContext,
          layoutDirection = lithoLayoutResult.layoutDirection,
          layoutVersion = renderContext.layoutVersion,
      )
      context.setPreviousLayoutDataForCurrentNode(lithoLayoutResult.layoutData)
      context.setExtraContextForCurrentNode(contextForLayoutScope)

      @Suppress("UNCHECKED_CAST") val layoutContext = context as LayoutContext<Any?>
      val sizeConstraints = SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec)
      val delegate = primitive.calculateLayout(layoutContext, sizeConstraints)
      return LeafMeasureResult(
          width = delegate.width,
          height = delegate.height,
          layoutData = delegate.layoutData,
          delegate = delegate,
          effects = contextForLayoutScope.effects,
      )
    }

    val specComponent = component as SpecGeneratedComponent
    val specLayout = SpecGeneratedComponentLayout(
        layoutOutput = lithoLayoutResult.layoutOutput,
        paddingSet = lithoLayoutResult.node.isPaddingSet,
        backgroundDrawable = lithoLayoutResult.node.background,
    )

    // Measure into a fresh layout-data container so a cached layout's mounted data isn't mutated.
    val size = Size(Int.MIN_VALUE, Int.MIN_VALUE)
    val newLayoutData = specComponent.createInterStagePropsContainer()
    specComponent.onMeasure(
        componentScopedContext,
        specLayout,
        widthSpec,
        heightSpec,
        size,
        newLayoutData,
    )
    return LeafMeasureResult(
        width = size.width,
        height = size.height,
        layoutData = newLayoutData,
    )
  }

  /**
   * Companion to [measureContentLayout] for the min-content intrinsic-size probe. The probe bounds
   * exactly one axis to AtMost 0 and leaves the cross axis unbounded; only the bounded axis is read
   * back, so only that one is computed. Writes nothing persistent — the caller owns that.
   */
  private fun measureMinContentLayout(
      context: LayoutContext<LithoLayoutContext>,
      lithoLayoutResult: LithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int,
  ): LeafMeasureResult {
    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }
    val node: LithoNode = lithoLayoutResult.node
    val component: Component = node.tailComponent
    val componentScopedContext: ComponentContext = node.tailComponentContext
    val primitive = node.primitive
    val sizeConstraints = SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec)

    if (primitive != null) {
      val contextForLayoutScope = LithoExtraContextForLayoutScope(
          componentContext = componentScopedContext,
          layoutDirection = lithoLayoutResult.layoutDirection,
          layoutVersion = renderContext.layoutVersion,
      )
      context.setPreviousLayoutDataForCurrentNode(lithoLayoutResult.layoutData)
      context.setExtraContextForCurrentNode(contextForLayoutScope)

      @Suppress("UNCHECKED_CAST") val layoutContext = context as LayoutContext<Any?>
      // Transient probe; scope isMinContentMeasure (restored after) so nested layout detects it.
      val prevMinContent = context.isMinContentMeasure
      context.isMinContentMeasure = true
      try {
        return when {
          SizeSpec.getMode(widthSpec) == SizeSpec.AT_MOST ->
              LeafMeasureResult(
                  width = primitive.calculateMinIntrinsicWidth(layoutContext, sizeConstraints),
                  height = 0,
              )
          SizeSpec.getMode(heightSpec) == SizeSpec.AT_MOST ->
              LeafMeasureResult(
                  width = 0,
                  height = primitive.calculateMinIntrinsicHeight(layoutContext, sizeConstraints),
              )
          else -> throw IllegalArgumentException(minContentAxisError(sizeConstraints))
        }
      } finally {
        context.isMinContentMeasure = prevMinContent
      }
    }

    val specComponent = component as SpecGeneratedComponent
    val specLayout = SpecGeneratedComponentLayout(
        layoutOutput = lithoLayoutResult.layoutOutput,
        paddingSet = lithoLayoutResult.node.isPaddingSet,
        backgroundDrawable = lithoLayoutResult.node.background,
    )
    return when {
      SizeSpec.getMode(widthSpec) == SizeSpec.AT_MOST ->
          LeafMeasureResult(
              width =
                  specComponent.onMeasureIntrinsicWidth(
                      componentScopedContext,
                      specLayout,
                      widthSpec,
                      heightSpec,
                  ),
              height = 0,
          )
      SizeSpec.getMode(heightSpec) == SizeSpec.AT_MOST ->
          LeafMeasureResult(
              width = 0,
              height =
                  specComponent.onMeasureIntrinsicHeight(
                      componentScopedContext,
                      specLayout,
                      widthSpec,
                      heightSpec,
                  ),
          )
      else -> throw IllegalArgumentException(minContentAxisError(sizeConstraints))
    }
  }

  private fun minContentAxisError(sizeConstraints: SizeConstraints): String =
      "Min-content intrinsic-size probe expects exactly one axis bounded to AtMost: $sizeConstraints"

  private fun measureDeferredNode(
      context: LayoutContext<LithoLayoutContext>,
      widthSpec: Int,
      heightSpec: Int,
      lithoLayoutResult: DeferredLithoLayoutResult,
  ): MeasureResult {
    val component = lithoLayoutResult.node.tailComponent
    val renderContext = checkNotNull(context.renderContext)

    check(!renderContext.isReleased) {
      (component.simpleName +
          ": To measure a component outside of a layout calculation use" +
          " Component#measureMightNotCacheInternalNode.")
    }

    val count = lithoLayoutResult.node.componentCount
    val parentContext: ComponentContext? =
        if (count == 1) {
          val parentFromNode = lithoLayoutResult.node.parentContext
          parentFromNode ?: renderContext.rootComponentContext
        } else {
          lithoLayoutResult.node.getComponentContextAt(1)
        }

    checkNotNull(parentContext) { component.simpleName + ": Null component context during measure" }

    return trace({ "layout.resolveDeferredNode:${component.simpleName}" }) {
      val actualResult =
          LayoutUtils.measure(
              renderContext,
              parentContext,
              lithoLayoutResult,
              widthSpec,
              heightSpec,
          )

      if (actualResult != null) {
        MeasureResult(actualResult.width, actualResult.height, actualResult.layoutData)
      } else {
        MeasureResult(0, 0)
      }
    }
  }

  private fun adjustRenderUnitBounds(lithoLayoutResult: LithoLayoutResult) {
    val renderUnit: LithoRenderUnit = lithoLayoutResult.contentRenderUnit ?: return
    val bounds = Rect()
    if (Component.isPrimitive(renderUnit.component)) {
      if (!LithoRenderUnit.isMountableView(renderUnit)) {
        if (lithoLayoutResult.wasMeasured) {
          bounds.left += (lithoLayoutResult.paddingLeft + lithoLayoutResult.borderLeft)
          bounds.top += (lithoLayoutResult.paddingTop + lithoLayoutResult.borderTop)
          bounds.right -= (lithoLayoutResult.paddingRight + lithoLayoutResult.borderRight)
          bounds.bottom -= (lithoLayoutResult.paddingBottom + lithoLayoutResult.borderBottom)
        } else {
          // For an exact size, the border doesn't need to be adjusted because it's inside the
          // content bounds.
          bounds.left += lithoLayoutResult.paddingLeft
          bounds.top += lithoLayoutResult.paddingTop
          bounds.right -= lithoLayoutResult.paddingRight
          bounds.bottom -= lithoLayoutResult.paddingBottom
        }
      }
    } else if (!LithoRenderUnit.isMountableView(renderUnit)) {
      bounds.left += lithoLayoutResult.paddingLeft
      bounds.top += lithoLayoutResult.paddingTop
      bounds.right -= lithoLayoutResult.paddingRight
      bounds.bottom -= lithoLayoutResult.paddingBottom
    }
    lithoLayoutResult.layoutOutput._adjustedBounds.set(bounds)
  }

  // This is used when we need to create a new output other than the YogaNode for layout caching.
  private fun copyLayoutResult(
      layoutResult: LithoLayoutResult,
      lithoNode: LithoNode,
      yogaNode: YogaNode,
  ): LithoLayoutResult =
      lithoNode.createLayoutResult(
          layoutOutput =
              layoutResult.layoutOutput
                  .copy(
                      yogaNode = yogaNode,
                      _isCachedLayout = true,
                      _isDiffedLayout = false,
                      _cachedMeasuresValid = true,
                      _wasMeasured = false,
                      _measureHadExceptions = false,
                      _actualDeferredNodeResult = null, // disable deferred node caching
                      _adjustedBounds = Rect(layoutResult.layoutOutput.adjustedBounds),
                  )
                  .inheritStateReads(layoutResult.layoutOutput)
                  .also { it.childBoxProps = layoutResult.layoutOutput.childBoxProps },
      )

  private fun LithoLayoutResult.shouldDrawBorders(): Boolean =
      node.hasBorderColor() &&
          (borderLeft != 0 || borderTop != 0 || borderRight != 0 || borderBottom != 0)

  private fun createBorderColorDrawable(result: LithoLayoutResult): BorderColorDrawable {
    val node = result.node
    val isRtl = recursivelyResolveLayoutDirection(result) == YogaDirection.RTL
    val borderRadius = node.borderRadius
    val borderColors = node.borderColors
    val leftEdge = if (isRtl) YogaEdge.RIGHT else YogaEdge.LEFT
    val rightEdge = if (isRtl) YogaEdge.LEFT else YogaEdge.RIGHT
    return BorderColorDrawable.Builder()
        .pathEffect(node.borderPathEffect)
        .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
        .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
        .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
        .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
        .borderLeftWidth(if (isRtl) result.borderRight else result.borderLeft)
        .borderTopWidth(result.borderTop)
        .borderRightWidth(if (isRtl) result.borderLeft else result.borderRight)
        .borderBottomWidth(result.borderBottom)
        .borderRadius(borderRadius)
        .build()
  }

  private fun recursivelyResolveLayoutDirection(result: LithoLayoutResult): YogaDirection {
    result.layoutOutput.boxGeometry?.let {
      return it.direction.toYogaDirection()
    }
    val direction = result.getYogaNode().layoutDirection
    check(direction != YogaDirection.INHERIT) {
      "Direction cannot be resolved before layout calculation"
    }
    return direction
  }

  private fun shouldAlwaysRemeasure(component: Component): Boolean =
      if (component is SpecGeneratedComponent) {
        component.shouldAlwaysRemeasure()
      } else {
        false
      }

  /**
   * Checks whether a single dimension's previous measurement can be reused under a new spec without
   * re-measuring.
   *
   * Compatibility rules:
   * - Same mode required (no cross-mode transitions)
   * - EXACTLY → EXACTLY: sizes must be equal (already caught by strict equality in caller)
   * - AT_MOST → AT_MOST: measuredSize must be strictly less than both old and new constraint
   * - UNSPECIFIED → UNSPECIFIED: always compatible (no constraint to violate)
   *
   * Strict `<` is used intentionally: if measuredSize == constraintSize, the component may have
   * been clamped by the constraint and could produce a different (larger) result under a different
   * constraint.
   */
  private fun isSpecCompatible(
      oldSpec: Int,
      newSpec: Int,
      measuredSize: Int,
  ): Boolean {
    val oldMode = SizeSpec.getMode(oldSpec)
    val newMode = SizeSpec.getMode(newSpec)
    if (oldMode != newMode) return false
    return when (oldMode) {
      SizeSpec.EXACTLY -> SizeSpec.getSize(oldSpec) == SizeSpec.getSize(newSpec)
      SizeSpec.AT_MOST ->
          measuredSize < SizeSpec.getSize(oldSpec) && measuredSize < SizeSpec.getSize(newSpec)
      SizeSpec.UNSPECIFIED -> true
      else -> false
    }
  }

  @Suppress("UNCHECKED_CAST")
  internal fun getLayoutContextFromYogaNode(yogaNode: YogaNode): LayoutContext<LithoLayoutContext> =
      (yogaNode.data as Pair<*, *>).first as LayoutContext<LithoLayoutContext>

  internal fun getLayoutResultFromYogaNode(yogaNode: YogaNode): LithoLayoutResult =
      (yogaNode.data as Pair<*, *>).second as LithoLayoutResult

  /**
   * Companion to [measure] for the min-content intrinsic-size probe. Opted-in leaf nodes measure
   * via [measureMinContentLayout] (transient — not persisted); deferred or non-opted-in nodes fall
   * back to [measure].
   */
  fun measureMinContentSize(
      context: LayoutContext<LithoLayoutContext>,
      layoutResult: LithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int,
  ): MeasureResult {
    val node = layoutResult.node
    // Only leaves that opted into intrinsic sizing have a min-content path; others re-measure.
    val hasIntrinsicSize =
        node.primitive?.layoutBehavior?.hasIntrinsicSize
            ?: Component.hasIntrinsicSize(node.tailComponent)
    if (node is DeferredLithoNode || !hasIntrinsicSize) {
      return measure(context, layoutResult, widthSpec, heightSpec)
    }

    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }
    if (renderContext.isFutureReleased) {
      return MeasureResult.error()
    }

    val component = node.tailComponent
    var size: MeasureResult
    traceWithArgs(
        { "measureMinContentSize:${component.simpleName}" },
        { scope ->
          scope.attribute("widthSpec", SizeSpec.toString(widthSpec))
          scope.attribute("heightSpec", SizeSpec.toString(heightSpec))
          scope.attribute("componentId", component.instanceId)
        },
    ) {
      try {
        val leaf = measureMinContentLayout(
            context,
            layoutResult,
            widthSpec,
            heightSpec,
        )
        check(!(leaf.width < 0 || leaf.height < 0)) {
          ("MeasureOutput not set, Component is: $component " +
              "WidthSpec: ${MeasureSpecUtils.getMeasureSpecDescription(widthSpec)} " +
              "HeightSpec: ${MeasureSpecUtils.getMeasureSpecDescription(heightSpec)} " +
              "Measured width : ${leaf.width} " +
              "Measured Height: ${leaf.height}")
        }
        size = MeasureResult(leaf.width, leaf.height)
      } catch (e: Exception) {
        ComponentUtils.handle(node.tailComponentContext, e)
        size = MeasureResult.error()
      }
    }
    return size
  }

  private fun LithoNode.createYogaNodeWriter(): YogaLayoutProps {
    return when (this) {
      is DeferredLithoNode -> {
        DeferredNodeYogaLayoutProps(NodeConfig.createYogaNode())
      }
      is NullNode -> {
        NullWriter().apply { node.display = YogaDisplay.NONE }
      }
      else -> {
        YogaLayoutProps(NodeConfig.createYogaNode())
      }
    }
  }

  private fun LithoNode.writeNodePropsToYogaNode(
      context: LayoutContext<LithoLayoutContext>,
      writer: YogaLayoutProps,
      props: LayoutProps = writer,
  ) {
    val node: YogaNode = writer.node
    val isReadTrackingEnabled = tailComponentContext.isReadTrackingEnabled

    // Apply the extra layout props
    layoutDirection.let { node.setDirection(it.toYogaDirection()) }
    flexDirection?.let { node.flexDirection = it }
    justifyContent?.let { node.justifyContent = it }
    alignContent?.let { node.alignContent = it }
    alignItems?.let { node.alignItems = it }
    yogaWrap?.let { node.wrap = it }
    if (columnGapPx > 0) node.setGap(YogaGutter.COLUMN, columnGapPx.toFloat())
    if (rowGapPx > 0) node.setGap(YogaGutter.ROW, rowGapPx.toFloat())
    if (allGapPx > 0) node.setGap(YogaGutter.ALL, allGapPx.toFloat())
    yogaMeasureFunction?.let { node.setMeasureFunction(it) }
    yogaMinContentMeasureFunction?.let { node.setMinContentMeasureFunction(it) }
    // CSS Flexbox §4.5 auto-min is opt-in. By default a leaf measurer contributes 0 min-content: a
    // defined static 0 short-circuits Yoga's probe at this node, preserving today's behaviour at no
    // extra measure cost. A Primitive ([LayoutBehavior.hasIntrinsicSize]) or MountSpec
    // (`@MountSpec(hasIntrinsicSize = true)`) opts in to a real measurement. Containers have no
    // measure function, so they are left undefined and Yoga recurses into their children.
    val isLeafMeasurer = yogaMeasureFunction != null || primitive != null
    val hasIntrinsicSize =
        primitive?.layoutBehavior?.hasIntrinsicSize ?: Component.hasIntrinsicSize(tailComponent)
    if (isLeafMeasurer && !hasIntrinsicSize) {
      node.setMinContentWidth(0f)
      node.setMinContentHeight(0f)
    }

    val stateReads = copyLayoutPropsInto(context, props)
    if (isReadTrackingEnabled) {
      writer.addStateReads(stateReads)
    }
    isPaddingSet = writer.isPaddingSet || (props is ContainerChildLayoutProps && props.isPaddingSet)
  }

  private fun DeferredLithoNode.writeDeferredNodePropsToYogaNode(
      context: LayoutContext<LithoLayoutContext>,
      writer: DeferredNodeYogaLayoutProps,
      props: LayoutProps = writer,
  ) {
    writeNodePropsToYogaNode(context, writer, props)
    deferredBorders = writer.borderWidth
    deferredPaddings = writer.padding
    deferredIsPaddingPercentages = writer.isPaddingPercentage
  }
}

/**
 * A wrapper around [YogaNode] that implements [LithoLayoutOutput], which will be used internally by
 * [LithoYogaLayoutFunction].
 */
@DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
data class YogaLayoutOutput(
    val yogaNode: YogaNode,
    val widthFromStyle: YogaValue,
    val heightFromStyle: YogaValue,
    val minWidthFromStyle: YogaValue,
    val maxWidthFromStyle: YogaValue,
    val minHeightFromStyle: YogaValue,
    val maxHeightFromStyle: YogaValue,
    internal var _widthSpec: Int = UNSPECIFIED,
    internal var _heightSpec: Int = UNSPECIFIED,
    internal var _lastMeasuredSize: Long = Long.MIN_VALUE,
    internal var _isCachedLayout: Boolean = false,
    internal var _isDiffedLayout: Boolean = false,
    internal var _layoutData: Any? = null,
    internal var _wasMeasured: Boolean = false,
    internal var _cachedMeasuresValid: Boolean = false,
    internal var _measureHadExceptions: Boolean = false,
    internal var _contentRenderUnit: LithoRenderUnit? = null,
    internal var _hostRenderUnit: LithoRenderUnit? = null,
    internal var _backgroundRenderUnit: LithoRenderUnit? = null,
    internal var _foregroundRenderUnit: LithoRenderUnit? = null,
    internal var _borderRenderUnit: LithoRenderUnit? = null,
    internal var _diffNode: DiffNode? = null,
    internal var _delegate: LayoutResult? = null,
    internal var _actualDeferredNodeResult: LithoLayoutResult? = null,
    internal val _adjustedBounds: Rect = Rect(),
    internal var _effects: List<Attachable>? = null,
) : LithoLayoutOutput {

  private var _stateReads: MutableScatterSet<StateId>? = null
  val stateReads: ScatterSet<StateId>?
    get() = _stateReads

  /** Takes [from]'s state reads too, for a copy of it. */
  internal fun inheritStateReads(from: YogaLayoutOutput): YogaLayoutOutput = apply {
    // Concurrent layouts can still reach the source, so keep independent mutable storage.
    from.stateReads?.let(::addStateReads)
  }

  internal fun addStateReads(stateIds: ScatterSet<StateId>) {
    if (stateIds.isEmpty()) return
    val stateReads =
        _stateReads ?: MutableScatterSet<StateId>(stateIds.size).also { _stateReads = it }
    stateReads.addAll(stateIds)
  }

  internal fun addStateRead(stateId: StateId) {
    val stateReads = _stateReads ?: MutableScatterSet<StateId>(1).also { _stateReads = it }
    stateReads.add(stateId)
  }

  /**
   * Where a `Layout` placed this result, in its border box. Set only for a `Layout`'s child, a Yoga
   * root of its own, which has no position in its container's Yoga tree.
   */
  internal var placedX: Int? = null
  internal var placedY: Int? = null

  /**
   * Set for a leaf its `Layout` laid out without a Yoga pass: its geometry is read from here, since
   * no layout pass touched [yogaNode]. Null for any other result.
   */
  internal var boxGeometry: BoxGeometry? = null

  /**
   * The box props of a `Layout`'s child, recorded as its tree was built and kept off its node,
   * which its container resolves within the constraints of each measurement. Null for any other
   * result.
   */
  internal var childBoxProps: BoxLayoutProps? = null

  /** The margins a `Layout` resolved for this result, its child; null for any other result. */
  internal var resolvedMargins: IntEdges? = null

  override val x: Int
    get() = placedX ?: if (boxGeometry != null) 0 else yogaNode.layoutX.toInt()

  override val y: Int
    get() = placedY ?: if (boxGeometry != null) 0 else yogaNode.layoutY.toInt()

  override val width: Int
    get() = boxGeometry?.width ?: yogaNode.layoutWidth.toInt()

  override val height: Int
    get() = boxGeometry?.height ?: yogaNode.layoutHeight.toInt()

  override val contentWidth: Int
    get() = YogaMeasureOutput.getWidth(_lastMeasuredSize).toInt()

  override val contentHeight: Int
    get() = YogaMeasureOutput.getHeight(_lastMeasuredSize).toInt()

  override val paddingLeft: Int
    get() = boxGeometry?.padding?.left ?: FastMath.round(yogaNode.getLayoutPadding(YogaEdge.LEFT))

  override val paddingTop: Int
    get() = boxGeometry?.padding?.top ?: FastMath.round(yogaNode.getLayoutPadding(YogaEdge.TOP))

  override val paddingRight: Int
    get() = boxGeometry?.padding?.right ?: FastMath.round(yogaNode.getLayoutPadding(YogaEdge.RIGHT))

  override val paddingBottom: Int
    get() =
        boxGeometry?.padding?.bottom ?: FastMath.round(yogaNode.getLayoutPadding(YogaEdge.BOTTOM))

  override val borderLeft: Int
    get() = boxGeometry?.border?.left ?: FastMath.round(yogaNode.getLayoutBorder(YogaEdge.LEFT))

  override val borderTop: Int
    get() = boxGeometry?.border?.top ?: FastMath.round(yogaNode.getLayoutBorder(YogaEdge.TOP))

  override val borderRight: Int
    get() = boxGeometry?.border?.right ?: FastMath.round(yogaNode.getLayoutBorder(YogaEdge.RIGHT))

  override val borderBottom: Int
    get() = boxGeometry?.border?.bottom ?: FastMath.round(yogaNode.getLayoutBorder(YogaEdge.BOTTOM))

  override val layoutDirection: LayoutDirection
    get() = boxGeometry?.direction ?: yogaNode.layoutDirection.toLayoutDirection()

  override val widthSpec: Int
    get() = _widthSpec

  override val heightSpec: Int
    get() = _heightSpec

  override val lastMeasuredSize: Long
    get() = _lastMeasuredSize

  override val layoutData: Any?
    get() = _layoutData

  override val wasMeasured: Boolean
    get() = _wasMeasured

  override val cachedMeasuresValid: Boolean
    get() = _cachedMeasuresValid

  override val measureHadExceptions: Boolean
    get() = _measureHadExceptions

  override val contentRenderUnit: LithoRenderUnit?
    get() = _contentRenderUnit

  override val hostRenderUnit: LithoRenderUnit?
    get() = _hostRenderUnit

  override val backgroundRenderUnit: LithoRenderUnit?
    get() = _backgroundRenderUnit

  override val foregroundRenderUnit: LithoRenderUnit?
    get() = _foregroundRenderUnit

  override val borderRenderUnit: LithoRenderUnit?
    get() = _borderRenderUnit

  override val effects: List<Attachable>?
    get() = _effects

  override val isCachedLayout: Boolean
    get() = _isCachedLayout

  override val diffNode: DiffNode?
    get() = _diffNode

  override val actualDeferredNodeResult: LithoLayoutResult?
    get() = _actualDeferredNodeResult

  override val delegate: LayoutResult?
    get() = _delegate

  /**
   * To avoid redundant calculations in [LithoYogaLayoutFunction.adjustRenderUnitBounds], we save
   * the adjustments in a Rect that is initialised during layout, specifically inside
   * [LithoYogaLayoutFunction.onBoundsDefined].
   */
  override val adjustedBounds: Rect
    get() = _adjustedBounds

  fun setSizeSpec(widthSpec: Int, heightSpec: Int) {
    _widthSpec = widthSpec
    _heightSpec = heightSpec
  }

  fun clear() {
    _diffNode = null
    yogaNode.data = null
  }

  companion object {
    private const val UNSPECIFIED: Int = -1

    fun LithoLayoutResult.getYogaNode(): YogaNode = layoutOutput.yogaNode

    val LithoLayoutResult.layoutDirection: LayoutDirection
      get() = layoutOutput.layoutDirection
  }
}

/** Raw result of a single leaf measurement. */
private class LeafMeasureResult(
    val width: Int,
    val height: Int,
    val layoutData: Any? = null,
    val delegate: LayoutResult? = null,
    val effects: List<Attachable>? = null,
)
