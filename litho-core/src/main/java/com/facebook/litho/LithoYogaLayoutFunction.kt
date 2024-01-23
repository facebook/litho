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
import android.view.View
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.LithoLayoutResult.Companion.getLayoutBorder
import com.facebook.litho.LithoLayoutResult.Companion.shouldDrawBorders
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.facebook.rendercore.utils.hasEquivalentFields
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode

/** Layout function for LithoNode that layout their children via Flexbox. */
internal object LithoYogaLayoutFunction {

  /** Calculate the layout for a component using Yoga. */
  fun calculateLayout(
      context: LayoutContext<LithoLayoutContext>,
      sizeConstraints: SizeConstraints,
      lithoNode: LithoNode
  ): LithoLayoutResult {

    val isTracing: Boolean = ComponentsSystrace.isTracing

    applyOverridesRecursive(lithoNode)

    if (isTracing) {
      ComponentsSystrace.beginSection("buildYogaTree:${lithoNode.headComponent.simpleName}")
    }

    val layoutResult: LithoLayoutResult = buildYogaTree(context = context, currentNode = lithoNode)
    val yogaRoot: YogaNode = layoutResult.lithoLayoutOutput.yogaNode

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    val widthSpec = sizeConstraints.toWidthSpec()
    val heightSpec = sizeConstraints.toHeightSpec()

    if (lithoNode.isLayoutDirectionInherit && Layout.isLayoutDirectionRTL(context.androidContext)) {
      yogaRoot.setDirection(YogaDirection.RTL)
    }
    if (YogaConstants.isUndefined(yogaRoot.width.value)) {
      Layout.setStyleWidthFromSpec(yogaRoot, widthSpec)
    }
    if (YogaConstants.isUndefined(yogaRoot.height.value)) {
      Layout.setStyleHeightFromSpec(yogaRoot, heightSpec)
    }

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

    if (isTracing) {
      ComponentsSystrace.beginSection("yogaCalculateLayout:${lithoNode.headComponent.simpleName}")
    }

    yogaRoot.calculateLayout(width, height)

    layoutResult.setSizeSpec(widthSpec, heightSpec)

    context.renderContext?.rootOffset =
        Point(
            yogaRoot.layoutX.toInt(),
            yogaRoot.layoutY.toInt(),
        )

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    return layoutResult
  }

  private fun buildYogaTreeFromCache(
      context: LayoutContext<LithoLayoutContext>,
      cachedLayoutResult: LithoLayoutResult,
      isTracing: Boolean
  ): LithoLayoutResult {
    if (isTracing) {
      ComponentsSystrace.beginSection(
          "cloneYogaNodeTree:${cachedLayoutResult.node.headComponent.simpleName}")
    }
    val clonedNode: YogaNode = cachedLayoutResult.lithoLayoutOutput.yogaNode.cloneWithChildren()
    if (isTracing) {
      ComponentsSystrace.endSection()
    }
    return cloneLayoutResultsRecursively(context, cachedLayoutResult, clonedNode, isTracing)
  }

  private fun cloneLayoutResultsRecursively(
      context: LayoutContext<LithoLayoutContext>,
      cachedLayoutResult: LithoLayoutResult,
      clonedYogaNode: YogaNode,
      isTracing: Boolean
  ): LithoLayoutResult {
    if (isTracing) {
      ComponentsSystrace.beginSection("copyLayoutResult")
    }
    val node: LithoNode = cachedLayoutResult.node
    val result: LithoLayoutResult = cachedLayoutResult.copyLayoutResult(node, clonedYogaNode)
    clonedYogaNode.data = Pair<Any, Any>(context, result)
    saveLithoLayoutResultIntoCache(context, node, result)
    if (isTracing) {
      ComponentsSystrace.endSection()
    }
    for (i in 0 until cachedLayoutResult.childCount) {
      val child: LithoLayoutResult =
          cloneLayoutResultsRecursively(
              context, cachedLayoutResult.getChildAt(i), clonedYogaNode.getChildAt(i), isTracing)
      result.addChild(child)
    }
    return result
  }

  /** Save LithoLayoutResult into LayoutCache, using node itself and id as keys. */
  private fun saveLithoLayoutResultIntoCache(
      context: LayoutContext<LithoLayoutContext>,
      node: LithoNode,
      result: LithoLayoutResult
  ) {
    if (!node.tailComponentContext.shouldCacheLayouts()) {
      return
    }
    val layoutCache: LayoutCache = context.layoutCache
    // TODO(T163437982): Refactor this to build the cache after layout calculation
    val cacheItem = LayoutCache.CacheItem(result, -1, -1)
    layoutCache.put(node, cacheItem)
    layoutCache.put(node.id.toLong(), cacheItem)
  }

  /**
   * Builds the YogaNode tree from this tree of LithoNodes. At the same time, builds the
   * LayoutResult tree and sets it in the data of the corresponding YogaNodes.
   */
  private fun buildYogaTree(
      context: LayoutContext<LithoLayoutContext>,
      currentNode: LithoNode,
      parentNode: YogaNode? = null,
  ): LithoLayoutResult {

    val isTracing: Boolean = ComponentsSystrace.isTracing
    var layoutResult: LithoLayoutResult? = null
    var yogaNode: YogaNode? = null

    if (currentNode.tailComponentContext.shouldCacheLayouts()) {
      val layoutCache: LayoutCache = context.layoutCache
      var cacheItem: LayoutCache.CacheItem? = layoutCache[currentNode]
      if (cacheItem != null) {
        val cachedLayoutResult: LayoutResult = cacheItem.layoutResult
        if (isTracing) {
          ComponentsSystrace.beginSection(
              "buildYogaTreeFromCache:${currentNode.headComponent.simpleName}")
        }

        // The situation that we can fully reuse the yoga tree
        val lithoLayoutResult =
            buildYogaTreeFromCache(context, cachedLayoutResult as LithoLayoutResult, isTracing)
        resetSizeIfNecessary(parentNode, lithoLayoutResult)
        if (isTracing) {
          ComponentsSystrace.endSection()
        }
        return lithoLayoutResult
      }

      cacheItem = layoutCache.get(currentNode.id.toLong())
      if (cacheItem != null) {
        val cachedLayoutResult: LayoutResult = cacheItem.layoutResult

        // The situation that we can partially reuse the yoga tree
        val clonedNode: YogaNode =
            (cachedLayoutResult as LithoLayoutResult)
                .lithoLayoutOutput
                .yogaNode
                .cloneWithoutChildren()
        yogaNode = clonedNode
        layoutResult = cachedLayoutResult.copyLayoutResult(currentNode, clonedNode)
        resetSizeIfNecessary(parentNode, layoutResult)
      }
    }

    if (layoutResult == null) {
      val writer: YogaLayoutProps = currentNode.createYogaNodeWriter()

      // return am empty layout result if the writer is null
      if (isTracing) {
        ComponentsSystrace.beginSection("createYogaNode:${currentNode.headComponent.simpleName}")
      }

      // Transfer the layout props to YogaNode
      currentNode.writeToYogaNode(writer)
      yogaNode = writer.node
      layoutResult =
          currentNode.createLayoutResult(
              lithoLayoutOutput =
                  YogaLithoLayoutOutput(
                      yogaNode = yogaNode,
                      widthFromStyle = writer.widthFromStyle,
                      heightFromStyle = writer.heightFromStyle))

      if (isTracing) {
        ComponentsSystrace.endSection()
      }
    }

    checkNotNull(yogaNode) { "YogaNode cannot be null when building YogaTree." }
    val renderContext: LithoLayoutContext? = context.renderContext
    checkNotNull(renderContext) { "RenderContext cannot be null when building YogaTree." }

    yogaNode.data = Pair(context, layoutResult)
    applyDiffNode(renderContext, currentNode, yogaNode, parentNode)
    saveLithoLayoutResultIntoCache(context, currentNode, layoutResult)

    for (i in 0 until currentNode.childCount) {
      val childLayoutResult: LithoLayoutResult =
          buildYogaTree(
              context = context, currentNode = currentNode.getChildAt(i), parentNode = yogaNode)

      yogaNode.addChildAt(childLayoutResult.lithoLayoutOutput.yogaNode, yogaNode.childCount)
      layoutResult.addChild(childLayoutResult)
    }

    return layoutResult
  }

  // Since we could potentially change with/maxWidth and height/maxHeight, we should reset them to
  // default value before we re-measure with the latest size specs.
  // We don't need to reset the size if last measured size equals to the original specified size.
  private fun resetSizeIfNecessary(parent: YogaNode?, layoutResult: LithoLayoutResult) {
    if (parent != null) {
      return
    }
    val yogaNode: YogaNode = layoutResult.lithoLayoutOutput.yogaNode
    if (layoutResult.lithoLayoutOutput.widthFromStyle.compareTo(yogaNode.width.value) != 0) {
      yogaNode.setWidthAuto()
    }
    if (layoutResult.lithoLayoutOutput.heightFromStyle.compareTo(yogaNode.height.value) != 0) {
      yogaNode.setHeightAuto()
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
   * Only add the YogaNode and LayoutResult if the node renders something. If it does not render
   * anything then it should not participate in grow/shrink behaviours.
   */
  private fun applyDiffNode(
      current: LithoLayoutContext,
      currentNode: LithoNode,
      currentYogaNode: YogaNode,
      parentYogaNode: YogaNode? = null
  ) {
    if (current.isReleased) {
      return // Cannot apply diff nodes with a released LayoutStateContext
    }

    val parent: LithoLayoutResult? =
        parentYogaNode?.let { LithoLayoutResult.getLayoutResultFromYogaNode(it) }
    val result: LithoLayoutResult = LithoLayoutResult.getLayoutResultFromYogaNode(currentYogaNode)

    val diff: DiffNode =
        when {
          (parent == null) -> { // If root, then get diff node root from the current layout state
            if (Component.isLayoutSpecWithSizeSpec(currentNode.headComponent) &&
                current.hasNestedTreeDiffNodeSet()) {
              current.consumeNestedTreeDiffNode()
            } else {
              current.currentDiffTree
            }
          }
          (parent.diffNode != null) -> { // Otherwise get it from the parent
            val parentDiffNode: DiffNode = parent.diffNode ?: return
            val index: Int = parent.node.getChildIndex(currentNode)
            if (index != -1 && index < parentDiffNode.childCount) {
              parentDiffNode.getChildAt(index)
            } else {
              null
            }
          }
          else -> {
            null
          }
        } ?: return // Return if no diff node to apply.

    val component: Component = currentNode.tailComponent
    if (!ComponentUtils.isSameComponentType(component, diff.component) &&
        !(parent != null && Component.isLayoutSpecWithSizeSpec(component))) {
      return
    }

    result.diffNode = diff

    val isTracing: Boolean = ComponentsSystrace.isTracing

    if (isTracing) {
      ComponentsSystrace.beginSection("shouldRemeasure:${currentNode.headComponent.simpleName}")
    }

    val isPrimitiveBehaviorEquivalent: Boolean =
        allNotNull(currentNode.primitive?.layoutBehavior, diff.primitive?.layoutBehavior) {
            layoutBehavior1,
            layoutBehavior2 ->
          layoutBehavior1.isEquivalentTo(layoutBehavior2)
        } == true

    if (isPrimitiveBehaviorEquivalent) {
      result.layoutData = diff.layoutData
      result.lithoLayoutOutput._cachedMeasuresValid = true
    } else if (!Layout.shouldComponentUpdate(currentNode, diff)) {
      val scopedComponentInfo = currentNode.tailScopedComponentInfo
      val diffNodeScopedComponentInfo = checkNotNull(diff.scopedComponentInfo)
      if (component is SpecGeneratedComponent) {
        component.copyInterStageImpl(
            result.layoutData as InterStagePropsContainer?,
            diff.layoutData as InterStagePropsContainer?)
        component.copyPrepareInterStageImpl(
            scopedComponentInfo.prepareInterStagePropsContainer,
            diffNodeScopedComponentInfo.prepareInterStagePropsContainer)
      }
      result.lithoLayoutOutput._cachedMeasuresValid = true
    }

    if (isTracing) {
      ComponentsSystrace.endSection()
    }
  }

  fun measure(
      context: LayoutContext<LithoLayoutContext>,
      layoutResult: LithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int
  ): MeasureResult {
    val renderContext =
        requireNotNull(context.renderContext) { "render context should not be null" }

    val isTracing = ComponentsSystrace.isTracing
    var size: MeasureResult
    layoutResult.wasMeasured = true
    if (renderContext.isFutureReleased) {

      // If layout is released then skip measurement
      size = MeasureResult.error()
    } else {
      val component = layoutResult.node.tailComponent
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("measure:${component.simpleName}")
            .arg("widthSpec", SizeSpec.toString(widthSpec))
            .arg("heightSpec", SizeSpec.toString(heightSpec))
            .arg("componentId", component.id)
            .flush()
      }
      try {
        size =
            if (layoutResult is NestedTreeHolderResult) {
              measureNestedTreeHolder(context, widthSpec, heightSpec, layoutResult)
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

        // Handle then exception
        ComponentUtils.handle(layoutResult.node.tailComponentContext, e)

        // If the exception is handled then return 0 size to continue layout.
        size = MeasureResult.error()
      }
    }

    // Record the last measured width, and height spec
    layoutResult.widthSpec = widthSpec
    layoutResult.heightSpec = heightSpec

    // If the size of a cached layout has changed then clear size dependant render units
    if (layoutResult.isCachedLayout &&
        (layoutResult.contentWidth != size.width || layoutResult.contentHeight != size.height)) {
      layoutResult.backgroundRenderUnit = null
      layoutResult.foregroundRenderUnit = null
      layoutResult.borderRenderUnit = null
    }
    layoutResult.lithoLayoutOutput._lastMeasuredSize =
        YogaMeasureOutput.make(size.width, size.height)

    if (isTracing) {
      ComponentsSystrace.endSection()
    }
    layoutResult.measureHadExceptions = size.hadExceptions
    return size
  }

  fun onBoundsDefined(layoutResult: LithoLayoutResult) {
    val context = layoutResult.node.tailComponentContext
    val component = layoutResult.node.tailComponent
    val hasLayoutSizeChanged: Boolean

    // Since `measure` would be called without padding and border size, in order to align with this
    // behavior for layout diffing(DiffNode) which relies on the last measured size directly, we're
    // going to save the size without padding and border.
    val newContentWidth =
        (layoutResult.width -
            layoutResult.paddingRight -
            layoutResult.paddingLeft -
            layoutResult.getLayoutBorder(YogaEdge.RIGHT) -
            layoutResult.getLayoutBorder(YogaEdge.LEFT))
    val newContentHeight =
        (layoutResult.height -
            layoutResult.paddingTop -
            layoutResult.paddingBottom -
            layoutResult.getLayoutBorder(YogaEdge.TOP) -
            layoutResult.getLayoutBorder(YogaEdge.BOTTOM))
    if (Component.isMountSpec(component) && component is SpecGeneratedComponent) {

      hasLayoutSizeChanged =
          if ((layoutResult.lastMeasuredSize != Long.MIN_VALUE) &&
              !layoutResult.wasMeasured &&
              layoutResult.isCachedLayout) {
            // Two scenarios would skip measurement and fall into this case:
            // 1. cached result from Yoga (may also contain fixed size)
            // 2. fixed size without measurement which size might change
            (newContentWidth != layoutResult.contentWidth) ||
                (newContentHeight != layoutResult.contentHeight)
          } else {
            true
          }

      if (hasLayoutSizeChanged) {

        // We should only invoke `onBoundsDefined` if layout is non cached or size has changed.
        // Note: MountSpec is always treated as size changed once `onMeasure` is invoked no matter
        // if the size changed or not.
        val isTracing = ComponentsSystrace.isTracing
        if (isTracing) {
          ComponentsSystrace.beginSection("onBoundsDefined:${component.getSimpleName()}")
        }
        val layoutData: InterStagePropsContainer?
        // If the Layout Result was cached, but the size has changed, then interstage props
        // container (layout data) could be mutated when @OnBoundsDefined is invoked. To avoid that
        // create new interstage props container (layout data), and copy over the current values.
        if (layoutResult.isCachedLayout) {
          layoutData = component.createInterStagePropsContainer()
          if (layoutData != null && layoutResult.layoutData != null) {
            component.copyInterStageImpl(
                layoutData, layoutResult.layoutData as InterStagePropsContainer?)
          }
        } else {
          layoutData = layoutResult.layoutData as InterStagePropsContainer?
        }
        try {
          component.onBoundsDefined(
              context,
              SpecGeneratedComponentLayout(
                  yogaNode = layoutResult.lithoLayoutOutput.yogaNode,
                  paddingSet = layoutResult.node.isPaddingSet,
                  background = layoutResult.node.background,
              ),
              layoutData)
        } catch (e: Exception) {
          ComponentUtils.handleWithHierarchy(context, component, e)
          layoutResult.measureHadExceptions = true
        } finally {
          if (isTracing) {
            ComponentsSystrace.endSection()
          }
        }

        // If layout data has changed then content render unit should be recreated
        if (!hasEquivalentFields(layoutResult.layoutData, layoutData)) {
          layoutResult.contentRenderUnit = null
          layoutResult.layoutData = layoutData
        }
      }
      if (!layoutResult.wasMeasured) {
        layoutResult.widthSpec = MeasureSpecUtils.exactly(newContentWidth)
        layoutResult.heightSpec = MeasureSpecUtils.exactly(newContentHeight)
        layoutResult.lithoLayoutOutput._lastMeasuredSize =
            YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    } else if (Component.isPrimitive(component)) {

      hasLayoutSizeChanged =
          (layoutResult.isCachedLayout &&
              (newContentWidth != layoutResult.contentWidth ||
                  newContentHeight != layoutResult.contentHeight))

      if (layoutResult.delegate == null || hasLayoutSizeChanged) {

        // Check if we need to run measure for Primitive that was skipped due to with fixed size
        val layoutContext =
            LithoLayoutResult.getLayoutContextFromYogaNode(layoutResult.lithoLayoutOutput.yogaNode)
        measure(
            layoutContext,
            layoutResult,
            MeasureSpecUtils.exactly(newContentWidth),
            MeasureSpecUtils.exactly(newContentHeight))
      }
    } else {
      hasLayoutSizeChanged =
          (layoutResult.lastMeasuredSize == Long.MIN_VALUE) ||
              (layoutResult.isCachedLayout &&
                  (layoutResult.contentWidth != newContentWidth ||
                      layoutResult.contentHeight != newContentHeight))
      if (hasLayoutSizeChanged) {
        layoutResult.lithoLayoutOutput._lastMeasuredSize =
            YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    }

    // Reuse or recreate additional outputs. Outputs are recreated if the size has changed
    if (layoutResult.contentRenderUnit == null) {
      layoutResult.contentRenderUnit =
          LithoNodeUtils.createContentRenderUnit(
              layoutResult.node, layoutResult.cachedMeasuresValid, layoutResult.diffNode)
      adjustRenderUnitBounds(layoutResult)
    }
    if (layoutResult.hostRenderUnit == null) {
      layoutResult.hostRenderUnit = LithoNodeUtils.createHostRenderUnit(layoutResult.node)
    }
    if (layoutResult.backgroundRenderUnit == null || hasLayoutSizeChanged) {
      layoutResult.backgroundRenderUnit =
          LithoNodeUtils.createBackgroundRenderUnit(
              layoutResult.node, layoutResult.width, layoutResult.height, layoutResult.diffNode)
    }
    if (layoutResult.foregroundRenderUnit == null || hasLayoutSizeChanged) {
      layoutResult.foregroundRenderUnit =
          LithoNodeUtils.createForegroundRenderUnit(
              layoutResult.node, layoutResult.width, layoutResult.height, layoutResult.diffNode)
    }
    if (layoutResult.shouldDrawBorders() &&
        (layoutResult.borderRenderUnit == null || hasLayoutSizeChanged)) {
      layoutResult.borderRenderUnit =
          LithoNodeUtils.createBorderRenderUnit(
              layoutResult.node,
              LithoLayoutResult.createBorderColorDrawable(layoutResult),
              layoutResult.width,
              layoutResult.height,
              layoutResult.diffNode)
    }
  }

  private fun measureLithoNode(
      context: LayoutContext<LithoLayoutContext>,
      widthSpec: Int,
      heightSpec: Int,
      lithoLayoutResult: LithoLayoutResult
  ): MeasureResult {
    val isTracing: Boolean = ComponentsSystrace.isTracing
    val node: LithoNode = lithoLayoutResult.node
    val component: Component = node.tailComponent
    val componentScopedContext: ComponentContext = node.tailComponentContext
    val diffNode: DiffNode? =
        if (lithoLayoutResult.cachedMeasuresValid) lithoLayoutResult.diffNode else null
    val width: Int
    val height: Int
    val delegate: LayoutResult?
    val layoutData: Any?

    // If diff node is set check if measurements from the previous pass can be reused
    if (diffNode?.lastWidthSpec == widthSpec &&
        diffNode.lastHeightSpec == heightSpec &&
        !LithoLayoutResult.shouldAlwaysRemeasure(component)) {
      width = diffNode.lastMeasuredWidth
      height = diffNode.lastMeasuredHeight
      layoutData = diffNode.layoutData
      delegate = diffNode.delegate

      // Measure the component
    } else {
      if (isTracing) {
        ComponentsSystrace.beginSection("onMeasure:${component.simpleName}")
      }
      try {
        val primitive = node.primitive
        val newLayoutData: Any?
        // measure Primitive
        if (primitive != null) {
          context.setPreviousLayoutDataForCurrentNode(lithoLayoutResult.layoutData)
          context.layoutContextExtraData =
              LithoLayoutContextExtraData(lithoLayoutResult.lithoLayoutOutput.yogaNode)
          @Suppress("UNCHECKED_CAST")
          delegate =
              primitive.calculateLayout(context as LayoutContext<Any?>, widthSpec, heightSpec)
          width = delegate.width
          height = delegate.height
          newLayoutData = delegate.layoutData
        } else {
          val size = Size(Int.MIN_VALUE, Int.MIN_VALUE)
          // If the Layout Result was cached, but the size specs changed, then layout data
          // will be mutated. To avoid that create new (layout data) interstage props container
          // for mount specs to avoid mutating the currently mount layout data.
          newLayoutData = (component as SpecGeneratedComponent).createInterStagePropsContainer()
          component.onMeasure(
              componentScopedContext,
              SpecGeneratedComponentLayout(
                  yogaNode = lithoLayoutResult.lithoLayoutOutput.yogaNode,
                  paddingSet = node.isPaddingSet,
                  background = node.background,
              ),
              widthSpec,
              heightSpec,
              size,
              newLayoutData)
          delegate = null
          width = size.width
          height = size.height
        }

        // If layout data has changed then content render unit should be recreated
        if (!hasEquivalentFields(lithoLayoutResult.layoutData, newLayoutData)) {
          layoutData = newLayoutData
          lithoLayoutResult.contentRenderUnit = null
        } else {
          layoutData = lithoLayoutResult.layoutData
        }
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection()
        }
      }
    }
    lithoLayoutResult.delegate = delegate
    lithoLayoutResult.layoutData = layoutData
    return MeasureResult(width, height, layoutData)
  }

  private fun measureNestedTreeHolder(
      context: LayoutContext<LithoLayoutContext>,
      widthSpec: Int,
      heightSpec: Int,
      lithoLayoutResult: NestedTreeHolderResult
  ): MeasureResult {
    val isTracing = ComponentsSystrace.isTracing
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

    if (isTracing) {
      ComponentsSystrace.beginSection("resolveNestedTree:" + component.simpleName)
    }

    return try {
      val nestedTree =
          Layout.measure(
              renderContext,
              parentContext,
              lithoLayoutResult,
              widthSpec,
              heightSpec,
          )

      if (nestedTree != null) {
        MeasureResult(nestedTree.width, nestedTree.height, nestedTree.layoutData)
      } else {
        MeasureResult(0, 0)
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection()
      }
    }
  }

  private fun adjustRenderUnitBounds(lithoLayoutResult: LithoLayoutResult) {
    val renderUnit: LithoRenderUnit = lithoLayoutResult.contentRenderUnit ?: return
    val bounds = Rect()
    if (Component.isPrimitive(renderUnit.component)) {
      if (!LithoRenderUnit.isMountableView(renderUnit)) {
        if (lithoLayoutResult.wasMeasured) {
          bounds.left +=
              (lithoLayoutResult.paddingLeft + lithoLayoutResult.getLayoutBorder(YogaEdge.LEFT))
          bounds.top +=
              (lithoLayoutResult.paddingTop + lithoLayoutResult.getLayoutBorder(YogaEdge.TOP))
          bounds.right -=
              (lithoLayoutResult.paddingRight + lithoLayoutResult.getLayoutBorder(YogaEdge.RIGHT))
          bounds.bottom -=
              (lithoLayoutResult.paddingBottom + lithoLayoutResult.getLayoutBorder(YogaEdge.BOTTOM))
        } else {
          // for exact size the border doesn't need to be adjusted since it's inside the bounds of
          // the content
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
    lithoLayoutResult.adjustedBounds.set(bounds)
  }
}

/**
 * A wrapper around [YogaNode] that implements [LithoLayoutOutput], which will be used internally by
 * [LithoYogaLayoutFunction].
 */
@DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
data class YogaLithoLayoutOutput(
    val yogaNode: YogaNode,
    val widthFromStyle: Float = YogaConstants.UNDEFINED,
    val heightFromStyle: Float = YogaConstants.UNDEFINED,
    var _widthSpec: Int = UNSPECIFIED,
    var _heightSpec: Int = UNSPECIFIED,
    internal var _lastMeasuredSize: Long = Long.MIN_VALUE,
    var _isCachedLayout: Boolean = false,
    var _layoutData: Any? = null,
    var _wasMeasured: Boolean = false,
    var _cachedMeasuresValid: Boolean = false,
    var _measureHadExceptions: Boolean = false,
    var _contentRenderUnit: LithoRenderUnit? = null,
    var _hostRenderUnit: LithoRenderUnit? = null,
    var _backgroundRenderUnit: LithoRenderUnit? = null,
    var _foregroundRenderUnit: LithoRenderUnit? = null,
    var _borderRenderUnit: LithoRenderUnit? = null,
    var _diffNode: DiffNode? = null,
    var _nestedResult: LithoLayoutResult? = null,
    var _adjustedBounds: Rect = Rect(),
) : LithoLayoutOutput {

  override val x: Int
    get() = yogaNode.layoutX.toInt()

  override val y: Int
    get() = yogaNode.layoutY.toInt()

  override val width: Int
    get() = yogaNode.layoutWidth.toInt()

  override val height: Int
    get() = yogaNode.layoutHeight.toInt()

  override val contentWidth: Int
    get() = YogaMeasureOutput.getWidth(_lastMeasuredSize).toInt()

  override val contentHeight: Int
    get() = YogaMeasureOutput.getHeight(_lastMeasuredSize).toInt()

  override val paddingLeft: Int
    get() = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.LEFT))

  override val paddingTop: Int
    get() = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.TOP))

  override val paddingRight: Int
    get() = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.RIGHT))

  override val paddingBottom: Int
    get() = FastMath.round(yogaNode.getLayoutPadding(YogaEdge.BOTTOM))

  override val layoutDirection: Int
    get() =
        when (yogaNode.layoutDirection) {
          YogaDirection.LTR -> View.LAYOUT_DIRECTION_LTR
          YogaDirection.RTL -> View.LAYOUT_DIRECTION_RTL
          else -> View.LAYOUT_DIRECTION_INHERIT
        }

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

  override val isCachedLayout: Boolean
    get() = _isCachedLayout

  override val diffNode: DiffNode?
    get() = _diffNode

  override val nestedResult: LithoLayoutResult?
    get() = _nestedResult

  override val adjustedBounds: Rect
    get() = _adjustedBounds

  /**
   * Returns a copy of this output that has the same values except for the YogaNode. This is used
   * when we need to create a new output but keep the old one around for layout caching.
   */
  fun cloneWithYogaNode(yogaNode: YogaNode): YogaLithoLayoutOutput = copy(yogaNode = yogaNode)

  companion object {
    private const val UNSPECIFIED: Int = -1
  }
}
