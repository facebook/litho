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

import android.graphics.Rect
import androidx.collection.LongSparseArray
import androidx.core.view.ViewCompat
import com.facebook.litho.LithoLayoutResult.Companion.getLayoutBorder
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.incrementalmount.ExcludeFromIncrementalMountBinder
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension
import com.facebook.rendercore.visibility.VisibilityOutput
import com.facebook.yoga.YogaEdge
import kotlin.math.max
import kotlin.math.min

object LithoReducer {

  private const val DUPLICATE_TRANSITION_IDS = "LayoutState:DuplicateTransitionIds"
  private val rootHost: LithoRenderUnit =
      MountSpecLithoRenderUnit.create(
          id = MountState.ROOT_HOST_ID,
          component = HostComponent.create(),
          commonDynamicProps = null,
          context = null,
          nodeInfo = null,
          flags = 0,
          importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
          updateState = MountSpecLithoRenderUnit.STATE_DIRTY,
          debugKey = LithoNodeUtils.getDebugKey("root-host", OutputUnitType.HOST))

  // region public methods
  @JvmStatic
  fun setSizeAfterMeasureAndCollectResults(
      c: ComponentContext,
      lithoLayoutContext: LithoLayoutContext,
      layoutState: LayoutState
  ) {
    if (lithoLayoutContext.isFutureReleased) {
      return
    }
    check(layoutState.mMountableOutputs.isEmpty()) {
      """Attempting to collect results on an already populated LayoutState.
        | Root: ${layoutState.mRootComponentName}"""
          .trimIndent()
    }

    val isTracing: Boolean = ComponentsSystrace.isTracing
    val widthSpec: Int = layoutState.mWidthSpec
    val heightSpec: Int = layoutState.mHeightSpec
    val root: LayoutResult? = layoutState.mLayoutResult
    val rootWidth: Int = root?.width ?: 0
    val rootHeight: Int = root?.height ?: 0

    layoutState.mWidth =
        when (SizeSpec.getMode(widthSpec)) {
          SizeSpec.EXACTLY -> SizeSpec.getSize(widthSpec)
          SizeSpec.AT_MOST -> max(0, min(rootWidth, SizeSpec.getSize(widthSpec)))
          SizeSpec.UNSPECIFIED -> rootWidth
          else -> layoutState.mWidth
        }
    layoutState.mHeight =
        when (SizeSpec.getMode(heightSpec)) {
          SizeSpec.EXACTLY -> SizeSpec.getSize(heightSpec)
          SizeSpec.AT_MOST -> max(0, min(rootHeight, SizeSpec.getSize(heightSpec)))
          SizeSpec.UNSPECIFIED -> rootHeight
          else -> layoutState.mHeight
        }

    if (root == null) {
      return
    }

    var parent: RenderTreeNode? = null
    var hierarchy: DebugHierarchy.Node? = null
    if (c.mLithoConfiguration.mComponentsConfiguration.isShouldAddHostViewForRootComponent) {
      hierarchy = if (root is LithoLayoutResult) root.node.getDebugHierarchy() else null
      addRootHostRenderTreeNode(layoutState, root, hierarchy)
      parent = layoutState.mMountableOutputs[0]
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("collectResults")
    }
    collectResults(
        parentContext = c,
        result = root,
        layoutState = layoutState,
        lithoLayoutContext = lithoLayoutContext,
        x = if (root is LithoLayoutResult) root.x else 0,
        y = if (root is LithoLayoutResult) root.y else 0,
        parent = parent,
        parentHierarchy = hierarchy)
    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("sortMountableOutputs")
    }

    sortTops(layoutState)
    sortBottoms(layoutState)

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    val nodeForSaving: LithoNode? = layoutState.mRoot
    val layoutResultForSaving: LithoLayoutResult? = layoutState.mLayoutResult

    // clean it up for sanity
    layoutState.mRoot = null
    layoutState.mLayoutResult = null

    // enabled for debugging and end to end tests
    if (ComponentsConfiguration.isDebugModeEnabled || ComponentsConfiguration.isEndToEndTestRun) {
      layoutState.mRoot = nodeForSaving
      layoutState.mLayoutResult = layoutResultForSaving
      return
    }

    // override used by analytics teams
    if (ComponentsConfiguration.keepLayoutResults) {
      layoutState.mLayoutResult = layoutResultForSaving
    }
  }

  @JvmStatic
  fun addRootHostRenderTreeNode(
      layoutState: LayoutState,
      result: LayoutResult? = null,
      hierarchy: DebugHierarchy.Node? = null,
  ) {
    val width: Int = result?.width ?: 0
    val height: Int = result?.height ?: 0
    val debugNode: DebugHierarchy.Node? = hierarchy?.mutateType(OutputUnitType.HOST)

    val node: RenderTreeNode =
        create(
            unit = rootHost,
            bounds = Rect(0, 0, width, height),
            layoutData =
                LithoLayoutData(
                    width = width,
                    height = height,
                    currentLayoutStateId = layoutState.mId,
                    previousLayoutStateId = layoutState.mPreviousLayoutStateId,
                    expandedTouchBounds = null,
                    layoutData = null,
                    debugHierarchy = debugNode),
            parent = null)
    addRenderTreeNode(
        layoutState = layoutState,
        node = node,
        result = result,
        unit = rootHost,
        type = OutputUnitType.HOST)
  }

  @JvmStatic
  fun createAnimatableItem(
      unit: LithoRenderUnit,
      absoluteBounds: Rect,
      @OutputUnitType outputType: Int,
      transitionId: TransitionId? = null,
  ): AnimatableItem =
      LithoAnimtableItem(unit.id, absoluteBounds, outputType, unit.nodeInfo, transitionId)

  @JvmStatic
  fun createDiffNode(tail: ScopedComponentInfo, parent: DiffNode? = null): DiffNode {
    val diffNode = DefaultDiffNode(tail.component, tail.context.globalKey, tail)
    parent?.addChild(diffNode)
    return diffNode
  }
  // endregion

  /**
   * Acquires a new layout output for the internal node and its associated component. It returns
   * null if there's no component associated with the node as the mount pass only cares about nodes
   * that will potentially mount content into the component host.
   */
  private fun createContentRenderTreeNode(
      result: LayoutResult,
      node: LithoNode,
      layoutState: LayoutState,
      parent: RenderTreeNode? = null,
      debugHierarchyNode: DebugHierarchy.Node? = null,
  ): RenderTreeNode? {

    if (Component.isLayoutSpec(node.tailComponent) ||
        (result !is LithoLayoutResult) ||
        result.measureHadExceptions) {
      // back out when dealing with Layout Specs or if there was an error during measure
      return null
    }

    val unit: LithoRenderUnit = result.contentRenderUnit ?: return null
    val layoutData: Any? = result.layoutData
    val debugNode: DebugHierarchy.Node? = debugHierarchyNode?.mutateType(OutputUnitType.CONTENT)
    return createRenderTreeNode(
        unit = unit,
        layoutState = layoutState,
        result = result,
        useNodePadding = true,
        hasExactSize = !result.wasMeasured,
        layoutData = layoutData,
        parent = parent,
        debugHierarchyNode = debugNode)
  }

  private fun createHostRenderTreeNode(
      unit: LithoRenderUnit,
      layoutState: LayoutState,
      result: LayoutResult,
      parent: RenderTreeNode? = null,
      hierarchy: DebugHierarchy.Node? = null,
  ): RenderTreeNode =
      createRenderTreeNode(
          unit = unit,
          layoutState = layoutState,
          result = result as LithoLayoutResult,
          useNodePadding = false,
          hasExactSize = false,
          parent = parent,
          debugHierarchyNode = hierarchy?.mutateType(OutputUnitType.HOST))

  private fun createRenderTreeNode(
      unit: LithoRenderUnit,
      layoutState: LayoutState,
      result: LithoLayoutResult,
      useNodePadding: Boolean,
      hasExactSize: Boolean,
      layoutData: Any? = null,
      parent: RenderTreeNode? = null,
      debugHierarchyNode: DebugHierarchy.Node? = null,
  ): RenderTreeNode {

    val hostTranslationX: Int
    val hostTranslationY: Int
    if (parent != null) {
      hostTranslationX = parent.absoluteX
      hostTranslationY = parent.absoluteY
    } else {
      hostTranslationX = 0
      hostTranslationY = 0
    }

    var l: Int = layoutState.mCurrentX - hostTranslationX + result.x
    var t: Int = layoutState.mCurrentY - hostTranslationY + result.y
    var r: Int = l + result.width
    var b: Int = t + result.height

    if (useNodePadding) {
      if (Component.isPrimitive(unit.component)) {
        if (!LithoRenderUnit.isMountableView(unit)) {
          if (!hasExactSize) {
            l += (result.paddingLeft + result.getLayoutBorder(YogaEdge.LEFT))
            t += (result.paddingTop + result.getLayoutBorder(YogaEdge.TOP))
            r -= (result.paddingRight + result.getLayoutBorder(YogaEdge.RIGHT))
            b -= (result.paddingBottom + result.getLayoutBorder(YogaEdge.BOTTOM))
          } else {
            // for exact size the border doesn't need to be adjusted since it's inside the bounds of
            // the content
            l += result.paddingLeft
            t += result.paddingTop
            r -= result.paddingRight
            b -= result.paddingBottom
          }
        }
      } else if (!LithoRenderUnit.isMountableView(unit)) {
        l += result.paddingLeft
        t += result.paddingTop
        r -= result.paddingRight
        b -= result.paddingBottom
      }
    }

    val bounds = Rect(l, t, r, b)

    return create(
        unit = unit,
        bounds = bounds,
        layoutData =
            LithoLayoutData(
                width = bounds.width(),
                height = bounds.height(),
                currentLayoutStateId = layoutState.mId,
                previousLayoutStateId = layoutState.mPreviousLayoutStateId,
                expandedTouchBounds = result.expandedTouchBounds,
                layoutData = layoutData,
                debugHierarchy = debugHierarchyNode),
        parent = parent)
  }

  /**
   * Acquires a [VisibilityOutput] object and computes the bounds for it using the information
   * stored in the [LithoNode].
   */
  private fun createVisibilityOutput(
      result: LayoutResult,
      node: LithoNode,
      layoutState: LayoutState,
      x: Int = 0,
      y: Int = 0,
      renderTreeNode: RenderTreeNode?
  ): VisibilityOutput {

    val l: Int = layoutState.mCurrentX + x
    val t: Int = layoutState.mCurrentY + y
    val r: Int = l + result.width
    val b: Int = t + result.height

    val visibleHandler: EventHandler<VisibleEvent>? = node.visibleHandler
    val focusedHandler: EventHandler<FocusedVisibleEvent>? = node.focusedHandler
    val unfocusedHandler: EventHandler<UnfocusedVisibleEvent>? = node.unfocusedHandler
    val fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>? =
        node.fullImpressionHandler
    val invisibleHandler: EventHandler<InvisibleEvent>? = node.invisibleHandler
    val visibleRectChangedEventHandler: EventHandler<VisibilityChangedEvent>? =
        node.visibilityChangedHandler
    val component: Component = node.tailComponent
    val componentGlobalKey: String = node.tailComponentKey

    return VisibilityOutput(
        componentGlobalKey,
        component.simpleName,
        Rect(l, t, r, b),
        renderTreeNode != null,
        renderTreeNode?.renderUnit?.id ?: 0,
        node.visibleHeightRatio,
        node.visibleWidthRatio,
        visibleHandler,
        invisibleHandler,
        focusedHandler,
        unfocusedHandler,
        fullImpressionHandler,
        visibleRectChangedEventHandler)
  }

  private fun createTestOutput(
      result: LayoutResult,
      node: LithoNode,
      layoutState: LayoutState,
      x: Int = 0,
      y: Int = 0,
      renderUnit: LithoRenderUnit? = null
  ): TestOutput {

    val l: Int = layoutState.mCurrentX + x
    val t: Int = layoutState.mCurrentY + y
    val r: Int = l + result.width
    val b: Int = t + result.height

    val output = TestOutput()
    output.testKey = checkNotNull(node.testKey)
    output.setBounds(l, t, r, b)
    if (renderUnit != null) {
      output.layoutOutputId = renderUnit.id
    }
    return output
  }

  /**
   * Collects layout outputs and release the layout tree. The layout outputs hold necessary
   * information to be used by [MountState] to mount components into a [ComponentHost].
   *
   * Whenever a component has view content (view tags, click handler, etc), a new host 'marker' is
   * added for it. The mount pass will use the markers to decide which host should be used for each
   * layout output. The root node unconditionally generates a layout output corresponding to the
   * root host.
   *
   * The order of layout outputs follows a depth-first traversal in the tree to ensure the hosts
   * will be created at the right order when mounting. The host markers will be define which host
   * each mounted artifacts will be attached to.
   *
   * At this stage all the [LithoNode] for which we have LayoutOutputs that can be recycled will
   * have a DiffNode associated. If the CachedMeasures are valid we'll try to recycle both the host
   * and the contents (including background/foreground). In all other cases instead we'll only try
   * to re-use the hosts. In some cases the host's structure might change between two updates even
   * if the component is of the same type. This can happen for example when a click listener is
   * added. To avoid trying to re-use the wrong host type we explicitly check that after all the
   * children for a subtree have been added (this is when the actual host type is resolved). If the
   * host type changed compared to the one in the DiffNode we need to refresh the ids for the whole
   * subtree in order to ensure that the MountState will unmount the subtree and mount it again on
   * the correct host.
   *
   * @param parentContext the parent component context
   * @param result InternalNode to process.
   * @param layoutState the LayoutState currently operating.
   * @param parent the parent render tree node.
   * @param parentDiffNode whether this method also populates the diff tree and assigns the root
   * @param parentHierarchy The parent hierarchy linked list or null.
   */
  private fun collectResults(
      parentContext: ComponentContext,
      result: LayoutResult,
      layoutState: LayoutState,
      lithoLayoutContext: LithoLayoutContext,
      x: Int = 0,
      y: Int = 0,
      parent: RenderTreeNode? = null,
      parentDiffNode: DiffNode? = null,
      parentHierarchy: DebugHierarchy.Node? = null
  ) {
    if (lithoLayoutContext.isFutureReleased ||
        result !is LithoLayoutResult ||
        result.measureHadExceptions) {
      // Exit early if the layout future as been released or if this result had exceptions.
      return
    }

    val node: LithoNode = result.node
    val hierarchy: DebugHierarchy.Node? = node.getDebugHierarchy(parentHierarchy)

    if (result is NestedTreeHolderResult) {
      val size: Int = node.componentCount
      val immediateParentContext: ComponentContext =
          if (size == 1) {
            parentContext
          } else {
            node.getComponentContextAt(1)
          }

      val nestedTree: LithoLayoutResult = result.nestedResult ?: return

      // Account for position of the holder node.
      layoutState.mCurrentX += result.x
      layoutState.mCurrentY += result.y

      collectResults(
          parentContext = immediateParentContext,
          result = nestedTree,
          layoutState = layoutState,
          lithoLayoutContext = lithoLayoutContext,
          x = nestedTree.x,
          y = nestedTree.y,
          parent = parent,
          parentDiffNode = parentDiffNode,
          parentHierarchy = hierarchy)

      layoutState.mCurrentX -= result.x
      layoutState.mCurrentY -= result.y
      return
    }

    val tail: ScopedComponentInfo = node.tailScopedComponentInfo
    val context: ComponentContext = tail.context
    val shouldGenerateDiffTree: Boolean = layoutState.mShouldGenerateDiffTree
    val diffNode: DiffNode?
    if (shouldGenerateDiffTree) {
      diffNode = createDiffNode(tail, parentDiffNode)
      if (parentDiffNode == null) {
        layoutState.mDiffTreeRoot = diffNode
      }
    } else {
      diffNode = null
    }

    var parentRenderTreeNode: RenderTreeNode? = parent
    val hostRenderUnit: LithoRenderUnit? =
        if (parentRenderTreeNode == null /* isRoot */) {
          LithoNodeUtils.createRootHostRenderUnit(result.node)
        } else {
          result.hostRenderUnit
        }
    val needsHostView: Boolean = (hostRenderUnit != null)
    val currentTransitionId: TransitionId? = layoutState.mCurrentTransitionId
    val currentLayoutOutputAffinityGroup: OutputUnitsAffinityGroup<AnimatableItem>? =
        layoutState.mCurrentLayoutOutputAffinityGroup

    layoutState.mCurrentTransitionId = node.transitionId

    layoutState.mCurrentLayoutOutputAffinityGroup =
        if (layoutState.mCurrentTransitionId != null) OutputUnitsAffinityGroup() else null

    // 1. Insert a host LayoutOutput if we have some interactive content to be attached to.
    if (hostRenderUnit != null) {
      val hostLayoutPosition =
          addHostRenderTreeNode(
              hostRenderUnit = hostRenderUnit,
              parent = parentRenderTreeNode,
              result = result,
              node = node,
              layoutState = layoutState,
              diffNode = diffNode,
              hierarchy = hierarchy)
      addCurrentAffinityGroupToTransitionMapping(layoutState)

      parentRenderTreeNode = layoutState.mMountableOutputs[hostLayoutPosition]
    }

    // 2. Add background if defined.
    if (!context.mLithoConfiguration.mComponentsConfiguration.isShouldDisableBgFgOutputs) {
      result.backgroundRenderUnit?.let { backgroundRenderUnit ->
        val backgroundRenderTreeNode =
            addDrawableRenderTreeNode(
                unit = backgroundRenderUnit,
                parent = parentRenderTreeNode,
                result = result,
                layoutState = layoutState,
                hierarchy = hierarchy,
                type = OutputUnitType.BACKGROUND,
                matchHostBoundsTransitions = needsHostView)

        diffNode?.backgroundOutput = backgroundRenderTreeNode.renderUnit as LithoRenderUnit
      }
    }

    // Generate the RenderTreeNode for the given node.
    val contentRenderTreeNode: RenderTreeNode? =
        createContentRenderTreeNode(
            result = result,
            node = node,
            layoutState = layoutState,
            parent = parentRenderTreeNode,
            debugHierarchyNode = hierarchy)

    // 3. Now add the MountSpec (either View or Drawable) to the outputs.
    contentRenderTreeNode?.let { treeNode ->
      val contentRenderUnit: LithoRenderUnit = treeNode.renderUnit as LithoRenderUnit

      addRenderTreeNode(
          layoutState = layoutState,
          node = treeNode,
          result = result,
          unit = contentRenderUnit,
          type = OutputUnitType.CONTENT,
          transitionId = if (!needsHostView) layoutState.mCurrentTransitionId else null,
          parent = parentRenderTreeNode)

      diffNode?.contentOutput = contentRenderUnit
    }

    // Set the measurements, and the layout data on the diff node
    if (diffNode != null) {
      diffNode.lastWidthSpec = result.widthSpec
      diffNode.lastHeightSpec = result.heightSpec
      diffNode.lastMeasuredWidth = result.contentWidth.toFloat()
      diffNode.lastMeasuredHeight = result.contentHeight.toFloat()
      diffNode.layoutData = result.layoutData
      diffNode.primitive = result.node.primitive
      diffNode.delegate = result.delegate
    }

    layoutState.mCurrentX += x
    layoutState.mCurrentY += y

    // We must process the nodes in order so that the layout state output order is correct.
    for (i in 0 until result.childCount) {
      val child: LithoLayoutResult = result.getChildAt(i)
      collectResults(
          parentContext = context,
          result = child,
          layoutState = layoutState,
          lithoLayoutContext = lithoLayoutContext,
          x = child.x,
          y = child.y,
          parent = parentRenderTreeNode,
          parentDiffNode = diffNode,
          parentHierarchy = hierarchy)
    }

    layoutState.mCurrentX -= x
    layoutState.mCurrentY -= y

    // 5. Add border color if defined.
    result.borderRenderUnit?.let { borderRenderUnit ->
      val borderRenderTreeNode: RenderTreeNode =
          addDrawableRenderTreeNode(
              unit = borderRenderUnit,
              parent = parentRenderTreeNode,
              result = result,
              layoutState = layoutState,
              hierarchy = hierarchy,
              type = OutputUnitType.BORDER,
              matchHostBoundsTransitions = needsHostView)

      diffNode?.borderOutput = borderRenderTreeNode.renderUnit as LithoRenderUnit
    }

    // 6. Add foreground if defined.
    if (!context.mLithoConfiguration.mComponentsConfiguration.isShouldDisableBgFgOutputs) {
      result.foregroundRenderUnit?.let { foregroundRenderUnit ->
        val foregroundRenderTreeNode: RenderTreeNode =
            addDrawableRenderTreeNode(
                unit = foregroundRenderUnit,
                parent = parentRenderTreeNode,
                result = result,
                layoutState = layoutState,
                hierarchy = hierarchy,
                type = OutputUnitType.FOREGROUND,
                matchHostBoundsTransitions = needsHostView)

        diffNode?.foregroundOutput = foregroundRenderTreeNode.renderUnit as LithoRenderUnit
      }
    }

    // 7. Add VisibilityOutputs if any visibility-related event handlers are present.
    if (node.hasVisibilityHandlers()) {
      val visibilityOutput: VisibilityOutput =
          createVisibilityOutput(
              result = result,
              node = node,
              x = x,
              y = y,
              layoutState = layoutState,
              renderTreeNode =
                  contentRenderTreeNode ?: if (needsHostView) parentRenderTreeNode else null)

      layoutState.mVisibilityOutputs.add(visibilityOutput)
      diffNode?.visibilityOutput = visibilityOutput
    }

    // 8. If we're in a testing environment, maintain an additional data structure with
    // information about nodes that we can query later.
    if (layoutState.mTestOutputs != null && !node.testKey.isNullOrEmpty()) {
      val testOutput: TestOutput =
          createTestOutput(
              result = result,
              node = node,
              layoutState = layoutState,
              renderUnit = contentRenderTreeNode?.renderUnit as? LithoRenderUnit)
      layoutState.mTestOutputs.add(testOutput)
    }

    val rect: Rect
    if (contentRenderTreeNode != null) {
      rect = contentRenderTreeNode.getAbsoluteBounds(Rect())
    } else {
      rect = Rect()
      rect.left = layoutState.mCurrentX + x
      rect.top = layoutState.mCurrentY + y
      rect.right = rect.left + result.width
      rect.bottom = rect.top + result.height
    }

    for (i in 0 until node.componentCount) {
      val delegate: Component = node.getComponentAt(i)
      val delegateKey: String? = node.getGlobalKeyAt(i)
      // Keep a list of the components we created during this layout calculation. If the layout is
      // valid, the ComponentTree will update the event handlers that have been created in the
      // previous ComponentTree with the new component dispatched, otherwise Section children
      // might not be accessing the correct props and state on the event handlers. The null
      // checkers cover tests, the scope and tree should not be null at this point of the layout
      // calculation.
      node.getComponentContextAt(i)?.let { delegateScopedContext ->
        if (delegate is SpecGeneratedComponent) {
          layoutState.mScopedSpecComponentInfos?.add(delegateScopedContext.scopedComponentInfo)
        }
      }
      if (delegateKey != null || delegate.hasHandle()) {
        val copyRect = Rect(rect)
        if (delegateKey != null) {
          layoutState.mComponentKeyToBounds[delegateKey] = copyRect
        }
        if (delegate.hasHandle()) {
          layoutState.mComponentHandleToBounds[delegate.handle] = copyRect
        }
      }
    }

    addCurrentAffinityGroupToTransitionMapping(layoutState)
    layoutState.mCurrentTransitionId = currentTransitionId
    layoutState.mCurrentLayoutOutputAffinityGroup = currentLayoutOutputAffinityGroup
  }

  private fun addDrawableRenderTreeNode(
      unit: LithoRenderUnit,
      parent: RenderTreeNode? = null,
      result: LayoutResult,
      layoutState: LayoutState,
      hierarchy: DebugHierarchy.Node? = null,
      @OutputUnitType type: Int,
      matchHostBoundsTransitions: Boolean
  ): RenderTreeNode {

    val debugNode: DebugHierarchy.Node? = hierarchy?.mutateType(type)
    val renderTreeNode: RenderTreeNode =
        createRenderTreeNode(
            unit = unit,
            layoutState = layoutState,
            result = result as LithoLayoutResult,
            useNodePadding = false,
            hasExactSize = false,
            parent = parent,
            debugHierarchyNode = debugNode)
    val drawableRenderUnit: LithoRenderUnit = renderTreeNode.renderUnit as LithoRenderUnit

    addRenderTreeNode(
        layoutState = layoutState,
        node = renderTreeNode,
        result = result,
        unit = drawableRenderUnit,
        type = type,
        transitionId = if (!matchHostBoundsTransitions) layoutState.mCurrentTransitionId else null,
        parent = parent)

    return renderTreeNode
  }

  private fun addLayoutOutputIdToPositionsMap(
      outputsIdToPositionMap: LongSparseArray<Int>,
      unit: LithoRenderUnit,
      position: Int
  ) = outputsIdToPositionMap.put(unit.id, position)

  private fun maybeAddLayoutOutputToAffinityGroup(
      group: OutputUnitsAffinityGroup<AnimatableItem>? = null,
      @OutputUnitType outputType: Int,
      animatableItem: AnimatableItem
  ) = group?.add(outputType, animatableItem)

  private fun addCurrentAffinityGroupToTransitionMapping(layoutState: LayoutState) {
    val group: OutputUnitsAffinityGroup<AnimatableItem>? =
        layoutState.mCurrentLayoutOutputAffinityGroup
    if (group == null || group.isEmpty) {
      return
    }

    val transitionId = layoutState.mCurrentTransitionId ?: return

    if (transitionId.mType == TransitionId.Type.AUTOGENERATED) {
      // Check if the duplications of this key has been found before, if so, just ignore it
      if (!layoutState.mDuplicatedTransitionIds.contains(transitionId)) {
        if (layoutState.mTransitionIdMapping.put(transitionId, group) != null) {
          // Already seen component with the same generated transition key, remove it from the
          // mapping and ignore in the future
          layoutState.mTransitionIdMapping.remove(transitionId)
          layoutState.mDuplicatedTransitionIds.add(transitionId)
        }
      }
    } else {
      if (layoutState.mTransitionIdMapping.put(transitionId, group) != null) {
        // Already seen component with the same manually set transition key
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.FATAL,
            DUPLICATE_TRANSITION_IDS,
            """The transitionId '$transitionId' is defined multiple times in the same layout. TransitionIDs must be unique.
                  Tree:
                  ${ComponentUtils.treeToString(layoutState.mRoot)}
                  """
                .trimIndent())
      }
    }
    layoutState.mCurrentLayoutOutputAffinityGroup = null
    layoutState.mCurrentTransitionId = null
  }

  /**
   * If we have an interactive LayoutSpec or a MountSpec Drawable, we need to insert an
   * HostComponent in the Outputs such as it will be used as a HostView at Mount time. View
   * MountSpec are not allowed.
   *
   * @return The position the HostLayoutOutput was inserted.
   */
  private fun addHostRenderTreeNode(
      hostRenderUnit: LithoRenderUnit,
      parent: RenderTreeNode? = null,
      result: LithoLayoutResult,
      node: LithoNode,
      layoutState: LayoutState,
      diffNode: DiffNode? = null,
      hierarchy: DebugHierarchy.Node? = null,
  ): Int {

    // Only the root host is allowed to wrap view mount specs as a layout output
    // is unconditionally added for it.
    require(!(node.willMountView() && !layoutState.isLayoutRoot(result))) {
      "We shouldn't insert a host as a parent of a View"
    }
    val hostRenderTreeNode: RenderTreeNode =
        createHostRenderTreeNode(
            unit = hostRenderUnit,
            layoutState = layoutState,
            result = result,
            parent = parent,
            hierarchy = hierarchy)

    diffNode?.hostOutput = hostRenderUnit

    // The component of the hostLayoutOutput will be set later after all the
    // children got processed.
    addRenderTreeNode(
        layoutState = layoutState,
        node = hostRenderTreeNode,
        result = result,
        unit = hostRenderUnit,
        type = OutputUnitType.HOST,
        transitionId = layoutState.mCurrentTransitionId,
        parent = parent)

    return layoutState.mMountableOutputs.size - 1
  }

  private fun sortTops(layoutState: LayoutState) {
    val unsorted: List<IncrementalMountOutput> = ArrayList(layoutState.mMountableOutputTops)
    try {
      layoutState.mMountableOutputTops.sortWith(IncrementalMountRenderCoreExtension.sTopsComparator)
    } catch (e: IllegalArgumentException) {
      val errorMessage = StringBuilder()
      errorMessage.append(e.message).append("\n")
      val size = unsorted.size
      errorMessage.append("Error while sorting LayoutState tops. Size: $size").append("\n")
      val rect = Rect()
      for (i in 0 until size) {
        val node: RenderTreeNode = layoutState.getMountableOutputAt(i)
        errorMessage.append("   Index $i top: ${node.getAbsoluteBounds(rect).top}").append("\n")
      }
      throw IllegalStateException(errorMessage.toString())
    }
  }

  private fun sortBottoms(layoutState: LayoutState) {
    val unsorted: List<IncrementalMountOutput> = ArrayList(layoutState.mMountableOutputBottoms)
    try {
      layoutState.mMountableOutputBottoms.sortWith(
          IncrementalMountRenderCoreExtension.sBottomsComparator)
    } catch (e: IllegalArgumentException) {
      val errorMessage = StringBuilder()
      errorMessage.append(e.message).append("\n")
      val size = unsorted.size
      errorMessage.append("Error while sorting LayoutState bottoms. Size: $size").append("\n")
      val rect = Rect()
      for (i in 0 until size) {
        val node: RenderTreeNode = layoutState.getMountableOutputAt(i)
        errorMessage
            .append("   Index $i bottom: ${node.getAbsoluteBounds(rect).bottom}")
            .append("\n")
      }
      throw IllegalStateException(errorMessage.toString())
    }
  }

  private fun addRenderTreeNode(
      layoutState: LayoutState,
      node: RenderTreeNode,
      result: LayoutResult? = null,
      unit: LithoRenderUnit,
      @OutputUnitType type: Int,
      transitionId: TransitionId? = null,
      parent: RenderTreeNode? = null,
  ) {

    parent?.child(node)

    val component: Component = unit.component
    if (component is SpecGeneratedComponent &&
        component.implementsExtraAccessibilityNodes() &&
        unit.isAccessible &&
        parent != null) {
      val parentUnit: LithoRenderUnit = LithoRenderUnit.getRenderUnit(parent)
      (parentUnit.component as HostComponent).setImplementsVirtualViews()
    }

    val position: Int = layoutState.mMountableOutputs.size
    val absoluteBounds: Rect = node.getAbsoluteBounds(Rect())
    val shouldExcludePrimitiveFromIncrementalMount: Boolean =
        unit.findAttachBinderByClass(ExcludeFromIncrementalMountBinder::class.java) != null
    val shouldExcludeSpecGeneratedComponentFromIncrementalMount: Boolean =
        component is SpecGeneratedComponent && component.excludeFromIncrementalMount()

    val incrementalMountOutput =
        IncrementalMountOutput(
            node.renderUnit.id,
            position,
            absoluteBounds,
            shouldExcludeSpecGeneratedComponentFromIncrementalMount ||
                shouldExcludePrimitiveFromIncrementalMount,
            if (parent != null) layoutState.mIncrementalMountOutputs[parent.renderUnit.id]
            else null)
    if (shouldExcludeSpecGeneratedComponentFromIncrementalMount ||
        shouldExcludePrimitiveFromIncrementalMount) {
      layoutState.mHasComponentsExcludedFromIncrementalMount = true
    }

    val id: Long = node.renderUnit.id
    layoutState.mMountableOutputs.add(node)
    layoutState.mIncrementalMountOutputs[id] = incrementalMountOutput
    layoutState.mMountableOutputTops.add(incrementalMountOutput)
    layoutState.mMountableOutputBottoms.add(incrementalMountOutput)

    if ((component is SpecGeneratedComponent && component.hasChildLithoViews()) ||
        node.renderUnit.doesMountRenderTreeHosts()) {
      layoutState.mRenderUnitIdsWhichHostRenderTrees.add(id)
    }

    val attrs: ViewAttributes? =
        LithoNodeUtils.createViewAttributes(
            unit,
            component,
            result,
            type,
            unit.importantForAccessibility,
            layoutState.mContext.componentsConfiguration.isShouldDisableBgFgOutputs)

    if (attrs != null) {
      layoutState.mRenderUnitsWithViewAttributes[id] = attrs
    }

    if (node.renderUnit is LithoRenderUnit) {
      val lithoRenderUnit: LithoRenderUnit = node.renderUnit as LithoRenderUnit
      lithoRenderUnit.commonDynamicProps?.let { commonDynamicProps ->
        layoutState.mDynamicValueOutputs.put(
            lithoRenderUnit.id,
            DynamicValueOutput(
                component = lithoRenderUnit.component,
                scopedContext = lithoRenderUnit.componentContext,
                commonDynamicProps = commonDynamicProps))
      }
    }

    val animatableItem = createAnimatableItem(unit, absoluteBounds, type, transitionId)
    layoutState.mAnimatableItems.put(node.renderUnit.id, animatableItem)
    addLayoutOutputIdToPositionsMap(layoutState.mOutputsIdToPositionMap, unit, position)
    maybeAddLayoutOutputToAffinityGroup(
        layoutState.mCurrentLayoutOutputAffinityGroup, type, animatableItem)
  }

  private fun LithoNode.getDebugHierarchy(
      parentHierarchy: DebugHierarchy.Node? = null,
  ): DebugHierarchy.Node? {
    if (!ComponentsConfiguration.isDebugHierarchyEnabled) {
      return null
    }
    val infos: List<ScopedComponentInfo> = scopedComponentInfos
    val components: MutableList<Component> = ArrayList(infos.size)
    for (info in infos) {
      components.add(info.component)
    }
    return DebugHierarchy.newNode(parentHierarchy, tailComponent, components)
  }
}
