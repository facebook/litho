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
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.LithoNode.Companion.applyBorderWidth
import com.facebook.litho.LithoNode.Companion.applyNestedPadding
import com.facebook.litho.LithoNode.Companion.writeStyledAttributesToLayoutProps
import com.facebook.litho.YogaLayoutOutput.Companion.getYogaNode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.drawable.BorderColorDrawable
import com.facebook.litho.layout.LayoutDirection
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
import com.facebook.yoga.YogaDisplay
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode

typealias YogaEdgeIntFunction = ((YogaEdge, Int) -> Unit)

typealias YogaEdgeFloatFunction = ((YogaEdge, Float) -> Unit)

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
    val yogaRoot: YogaNode = layoutResult.getYogaNode()

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    val widthSpec = sizeConstraints.toWidthSpec()
    val heightSpec = sizeConstraints.toHeightSpec()

    if (lithoNode.layoutDirection == LayoutDirection.RTL) {
      yogaRoot.setDirection(YogaDirection.RTL)
    }
    if (YogaConstants.isUndefined(yogaRoot.width.value)) {
      setStyleWidthFromSpec(yogaRoot, widthSpec)
    }
    if (YogaConstants.isUndefined(yogaRoot.height.value)) {
      setStyleHeightFromSpec(yogaRoot, heightSpec)
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

    layoutResult.layoutOutput.setSizeSpec(widthSpec, heightSpec)

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
    val clonedNode: YogaNode = cachedLayoutResult.getYogaNode().cloneWithChildren()
    return cloneLayoutResultsRecursively(context, cachedLayoutResult, clonedNode, isTracing)
  }

  private fun cloneLayoutResultsRecursively(
      context: LayoutContext<LithoLayoutContext>,
      cachedLayoutResult: LithoLayoutResult,
      clonedYogaNode: YogaNode,
      isTracing: Boolean
  ): LithoLayoutResult {
    val node: LithoNode = cachedLayoutResult.node
    val result: LithoLayoutResult = copyLayoutResult(cachedLayoutResult, node, clonedYogaNode)
    clonedYogaNode.data = Pair<Any, Any>(context, result)
    saveLithoLayoutResultIntoCache(context, node, result)
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
          (cachedLayoutResult as LithoLayoutResult).getYogaNode().cloneWithoutChildren()
      yogaNode = clonedNode
      layoutResult = copyLayoutResult(cachedLayoutResult, currentNode, clonedNode)
      resetSizeIfNecessary(parentNode, layoutResult)
    }

    if (layoutResult == null) {
      val writer: YogaLayoutProps = currentNode.createYogaNodeWriter()

      // Transfer the layout props to YogaNode
      if (currentNode is NestedTreeHolder) {
        currentNode.writeNestedTreePropsToYogaNode(writer as NestedTreeYogaLayoutProps)
      } else if (currentNode !is NullNode) {
        currentNode.writeNodePropsToYogaNode(writer)
      }
      yogaNode = writer.node

      // Ideally the layout data should be created when measure is called on the mount spec or
      // primitive component, but because of the current implementation of mount specs, and the way
      // Yoga works it a possibility that measure may not be called, and a MountSpec [may] require
      // inter stage props, then it is necessary to have a non-null InterStagePropsContainer even if
      // the values are uninitialised. Otherwise it will lead to NPEs.
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
                      _layoutData = layoutData))
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

      yogaNode.addChildAt(childLayoutResult.getYogaNode(), yogaNode.childCount)
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
    val yogaNode: YogaNode = layoutResult.getYogaNode()
    if (layoutResult.layoutOutput.widthFromStyle.compareTo(yogaNode.width.value) != 0) {
      yogaNode.setWidthAuto()
    }
    if (layoutResult.layoutOutput.heightFromStyle.compareTo(yogaNode.height.value) != 0) {
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

    val parent: LithoLayoutResult? = parentYogaNode?.let { getLayoutResultFromYogaNode(it) }
    val result: LithoLayoutResult = getLayoutResultFromYogaNode(currentYogaNode)
    val yogaOutput: YogaLayoutOutput = result.layoutOutput

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

    yogaOutput._diffNode = diff

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
      yogaOutput._layoutData = diff.layoutData
      yogaOutput._cachedMeasuresValid = true
    } else if (currentNode.primitive != null) {
      yogaOutput._layoutData = diff.layoutData
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
      yogaOutput._cachedMeasuresValid = true
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
    val yogaOutput = layoutResult.layoutOutput
    yogaOutput._wasMeasured = true
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
    yogaOutput.setSizeSpec(widthSpec, heightSpec)

    // If the size of a cached layout has changed then clear size dependant render units
    if (layoutResult.isCachedLayout &&
        (layoutResult.contentWidth != size.width || layoutResult.contentHeight != size.height)) {
      yogaOutput._backgroundRenderUnit = null
      yogaOutput._foregroundRenderUnit = null
      yogaOutput._borderRenderUnit = null
    }
    yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(size.width, size.height)

    if (isTracing) {
      ComponentsSystrace.endSection()
    }
    yogaOutput._measureHadExceptions = size.hadExceptions
    return size
  }

  fun onBoundsDefined(layoutResult: LithoLayoutResult) {
    val context = layoutResult.node.tailComponentContext
    val component = layoutResult.node.tailComponent
    val yogaOutput = layoutResult.layoutOutput
    val hasLayoutSizeChanged: Boolean

    // Since `measure` would be called without padding and border size, in order to align with this
    // behavior for layout diffing(DiffNode) which relies on the last measured size directly, we're
    // going to save the size without padding and border.
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
        if (layoutResult.isCachedLayout || layoutResult.isDiffedLayout) {
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
                  layoutOutput = layoutResult.layoutOutput,
                  paddingSet = layoutResult.node.isPaddingSet,
                  backgroundDrawable = layoutResult.node.background),
              layoutData)
        } catch (e: Exception) {
          ComponentUtils.handleWithHierarchy(context, component, e)
          yogaOutput._measureHadExceptions = true
        } finally {
          if (isTracing) {
            ComponentsSystrace.endSection()
          }
        }

        // If layout data has changed then content render unit should be recreated
        if (!hasEquivalentFields(layoutResult.layoutData, layoutData)) {
          yogaOutput._contentRenderUnit = null
          yogaOutput._layoutData = layoutData
        }
      }
      if (!layoutResult.wasMeasured) {
        yogaOutput.setSizeSpec(
            MeasureSpecUtils.exactly(newContentWidth), MeasureSpecUtils.exactly(newContentHeight))
        yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    } else if (Component.isPrimitive(component)) {

      hasLayoutSizeChanged =
          ((layoutResult.isCachedLayout || ComponentsConfiguration.enablePrimitiveMeasurementFix) &&
              (newContentWidth != layoutResult.contentWidth ||
                  newContentHeight != layoutResult.contentHeight))

      if (layoutResult.delegate == null || hasLayoutSizeChanged) {

        // Check if we need to run measure for Primitive that was skipped due to with fixed size
        val layoutContext = getLayoutContextFromYogaNode(layoutResult.getYogaNode())
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
        yogaOutput._lastMeasuredSize = YogaMeasureOutput.make(newContentWidth, newContentHeight)
      }
    }

    // Reuse or recreate additional outputs. Outputs are recreated if the size has changed
    if (layoutResult.contentRenderUnit == null) {
      yogaOutput._contentRenderUnit =
          LithoNodeUtils.createContentRenderUnit(
              layoutResult.node, layoutResult.cachedMeasuresValid, layoutResult.diffNode)
      adjustRenderUnitBounds(layoutResult)
    }
    if (layoutResult.hostRenderUnit == null) {
      yogaOutput._hostRenderUnit = LithoNodeUtils.createHostRenderUnit(layoutResult.node)
    }
    if (layoutResult.backgroundRenderUnit == null || hasLayoutSizeChanged) {
      yogaOutput._backgroundRenderUnit =
          LithoNodeUtils.createBackgroundRenderUnit(
              layoutResult.node, layoutResult.width, layoutResult.height, layoutResult.diffNode)
    }
    if (layoutResult.foregroundRenderUnit == null || hasLayoutSizeChanged) {
      yogaOutput._foregroundRenderUnit =
          LithoNodeUtils.createForegroundRenderUnit(
              layoutResult.node, layoutResult.width, layoutResult.height, layoutResult.diffNode)
    }
    if (layoutResult.shouldDrawBorders() &&
        (layoutResult.borderRenderUnit == null || hasLayoutSizeChanged)) {
      yogaOutput._borderRenderUnit =
          LithoNodeUtils.createBorderRenderUnit(
              layoutResult.node,
              createBorderColorDrawable(layoutResult),
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
    val yogaOutput: YogaLayoutOutput = lithoLayoutResult.layoutOutput
    val diffNode: DiffNode? =
        if (lithoLayoutResult.cachedMeasuresValid) lithoLayoutResult.diffNode else null
    val width: Int
    val height: Int
    val delegate: LayoutResult?
    val layoutData: Any?

    // If diff node is set check if measurements from the previous pass can be reused
    if (diffNode?.lastWidthSpec == widthSpec &&
        diffNode.lastHeightSpec == heightSpec &&
        !shouldAlwaysRemeasure(component)) {
      width = diffNode.lastMeasuredWidth
      height = diffNode.lastMeasuredHeight
      layoutData = diffNode.layoutData
      delegate = diffNode.delegate
      yogaOutput._isDiffedLayout = true

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
              LithoLayoutContextExtraData(lithoLayoutResult.getYogaNode())
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
                  layoutOutput = lithoLayoutResult.layoutOutput,
                  paddingSet = lithoLayoutResult.node.isPaddingSet,
                  backgroundDrawable = lithoLayoutResult.node.background),
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
          yogaOutput._contentRenderUnit = null
        } else {
          layoutData = lithoLayoutResult.layoutData
        }
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection()
        }
      }
    }
    yogaOutput._delegate = delegate
    yogaOutput._layoutData = layoutData
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
          bounds.left += (lithoLayoutResult.paddingLeft + lithoLayoutResult.borderLeft)
          bounds.top += (lithoLayoutResult.paddingTop + lithoLayoutResult.borderTop)
          bounds.right -= (lithoLayoutResult.paddingRight + lithoLayoutResult.borderRight)
          bounds.bottom -= (lithoLayoutResult.paddingBottom + lithoLayoutResult.borderBottom)
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
    lithoLayoutResult.layoutOutput._adjustedBounds.set(bounds)
  }

  // This is used when we need to create a new output except YogaNode for layout caching.
  private fun copyLayoutResult(
      layoutResult: LithoLayoutResult,
      lithoNode: LithoNode,
      yogaNode: YogaNode
  ): LithoLayoutResult =
      lithoNode.createLayoutResult(
          layoutOutput =
              layoutResult.layoutOutput.copy(
                  yogaNode = yogaNode,
                  _isCachedLayout = true,
                  _isDiffedLayout = false,
                  _cachedMeasuresValid = true,
                  _wasMeasured = false,
                  _measureHadExceptions = false,
                  _nestedResult = null, // disable nested tree caching
                  _adjustedBounds = Rect(layoutResult.layoutOutput.adjustedBounds)))

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

  @Suppress("UNCHECKED_CAST")
  internal fun getLayoutContextFromYogaNode(yogaNode: YogaNode): LayoutContext<LithoLayoutContext> =
      (yogaNode.data as Pair<*, *>).first as LayoutContext<LithoLayoutContext>

  internal fun getLayoutResultFromYogaNode(yogaNode: YogaNode): LithoLayoutResult =
      (yogaNode.data as Pair<*, *>).second as LithoLayoutResult

  private fun setStyleWidthFromSpec(node: YogaNode, widthSpec: Int) {
    when (SizeSpec.getMode(widthSpec)) {
      SizeSpec.UNSPECIFIED -> node.setWidth(YogaConstants.UNDEFINED)
      SizeSpec.AT_MOST -> node.setMaxWidth(SizeSpec.getSize(widthSpec).toFloat())
      SizeSpec.EXACTLY -> node.setWidth(SizeSpec.getSize(widthSpec).toFloat())
      else -> {}
    }
  }

  private fun setStyleHeightFromSpec(node: YogaNode, heightSpec: Int) {
    when (SizeSpec.getMode(heightSpec)) {
      SizeSpec.UNSPECIFIED -> node.setHeight(YogaConstants.UNDEFINED)
      SizeSpec.AT_MOST -> node.setMaxHeight(SizeSpec.getSize(heightSpec).toFloat())
      SizeSpec.EXACTLY -> node.setHeight(SizeSpec.getSize(heightSpec).toFloat())
      else -> {}
    }
  }

  private fun LithoNode.createYogaNodeWriter(): YogaLayoutProps {
    return when (this) {
      is NestedTreeHolder -> {
        NestedTreeYogaLayoutProps(NodeConfig.createYogaNode())
      }
      is NullNode -> {
        NullWriter().apply { node.display = YogaDisplay.NONE }
      }
      else -> {
        YogaLayoutProps(NodeConfig.createYogaNode())
      }
    }
  }

  private fun LithoNode.writeNodePropsToYogaNode(writer: YogaLayoutProps) {
    val node: YogaNode = writer.node

    // Apply the extra layout props
    layoutDirection.let { node.setDirection(it.toYogaDirection()) }
    flexDirection?.let { node.flexDirection = it }
    justifyContent?.let { node.justifyContent = it }
    alignContent?.let { node.alignContent = it }
    alignItems?.let { node.alignItems = it }
    yogaWrap?.let { node.wrap = it }
    withValidGap { gap, yogaGutter -> node.setGap(yogaGutter, gap.toFloat()) }
    yogaMeasureFunction?.let { node.setMeasureFunction(it) }

    var nestedTreeHolderTransfered = false
    // Apply the layout props from the components to the YogaNode
    for (info in scopedComponentInfos) {
      val component: Component = info.component

      // If a NestedTreeHolder is set then transfer its resolved props into this LithoNode.
      if (nestedTreeHolder != null && Component.isLayoutSpecWithSizeSpec(component)) {
        if (nestedTreeHolderTransfered) {
          continue
        }
        nestedTreeHolderTransfered = true
        nestedTreeHolder?.transferInto(this)
        // TODO (T151239896): Revaluate copy into and freeze after common props are refactored
        _needsHostView = LithoNode.needsHostView(this)
        paddingFromBackground?.let { setPaddingFromDrawable(writer, it) }
      } else {
        info.commonProps?.let { props ->
          // Copy styled attributes into this LithoNode.
          props.writeStyledAttributesToLayoutProps(tailComponentContext.androidContext, writer)

          // Set the padding from the background
          props.paddingFromBackground?.let { padding -> setPaddingFromDrawable(writer, padding) }

          // Copy the layout props into this LithoNode.
          props.copyLayoutProps(writer)
        }
      }
    }

    // Apply the border widths
    applyBorderWidth { yogaEdge, width -> writer.setBorderWidth(yogaEdge, width) }

    // Maybe apply the padding if parent is a Nested Tree Holder
    applyNestedPadding(
        { yogaEdge, paddingPx -> writer.paddingPx(yogaEdge, paddingPx) },
        { yogaEdge, paddingPercent -> writer.paddingPercent(yogaEdge, paddingPercent) })

    debugLayoutProps?.copyInto(writer)
    isPaddingSet = writer.isPaddingSet
  }

  private fun NestedTreeHolder.writeNestedTreePropsToYogaNode(writer: NestedTreeYogaLayoutProps) {
    writeNodePropsToYogaNode(writer)
    nestedBorderEdges = writer.borderWidth
    nestedTreePadding = writer.padding
    nestedIsPaddingPercentage = writer.isPaddingPercentage
  }

  internal fun setPaddingFromDrawable(target: LayoutProps, padding: Rect) {
    if (padding.left > 0) {
      target.paddingPx(YogaEdge.LEFT, padding.left)
    }
    if (padding.top > 0) {
      target.paddingPx(YogaEdge.TOP, padding.top)
    }
    if (padding.right > 0) {
      target.paddingPx(YogaEdge.RIGHT, padding.right)
    }
    if (padding.bottom > 0) {
      target.paddingPx(YogaEdge.BOTTOM, padding.bottom)
    }
  }
}

/**
 * A wrapper around [YogaNode] that implements [LithoLayoutOutput], which will be used internally by
 * [LithoYogaLayoutFunction].
 */
@DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
data class YogaLayoutOutput(
    val yogaNode: YogaNode,
    val widthFromStyle: Float = YogaConstants.UNDEFINED,
    val heightFromStyle: Float = YogaConstants.UNDEFINED,
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
    internal var _nestedResult: LithoLayoutResult? = null,
    internal val _adjustedBounds: Rect = Rect(),
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

  override val borderLeft: Int
    get() = FastMath.round(yogaNode.getLayoutBorder(YogaEdge.LEFT))

  override val borderTop: Int
    get() = FastMath.round(yogaNode.getLayoutBorder(YogaEdge.TOP))

  override val borderRight: Int
    get() = FastMath.round(yogaNode.getLayoutBorder(YogaEdge.RIGHT))

  override val borderBottom: Int
    get() = FastMath.round(yogaNode.getLayoutBorder(YogaEdge.BOTTOM))

  override val layoutDirection: LayoutDirection
    get() = yogaNode.layoutDirection.toLayoutDirection()

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

  override val delegate: LayoutResult?
    get() = _delegate

  /**
   * In order to avoid redundant calculation that are happening in
   * [LithoYogaLayoutFunction.adjustRenderUnitBounds], we save the adjustments in a Rect that is
   * initialised during layout, which is specifically inside
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
  }
}
