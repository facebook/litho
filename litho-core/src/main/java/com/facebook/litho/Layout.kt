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

import android.content.Context
import androidx.collection.mutableScatterSetOf
import com.facebook.litho.transition.MutableTransitionData
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.utils.MeasureSpecUtils

internal object Layout {

  @JvmStatic
  fun measureTree(
      lithoLayoutContext: LithoLayoutContext,
      androidContext: Context,
      node: LithoNode? = null,
      widthSpec: Int,
      heightSpec: Int,
  ): LithoLayoutResult? {
    return measureTree(
        lithoLayoutContext = lithoLayoutContext,
        androidContext = androidContext,
        node = node,
        sizeConstraints =
            SizeConstraints.fromMeasureSpecs(
                widthSpec = widthSpec,
                heightSpec = heightSpec,
            ))
  }

  fun measureTree(
      lithoLayoutContext: LithoLayoutContext,
      androidContext: Context,
      node: LithoNode? = null,
      sizeConstraints: SizeConstraints,
  ): LithoLayoutResult? {
    if (node == null) {
      return null
    }

    val context: LayoutContext<LithoLayoutContext> =
        LayoutContext(androidContext, lithoLayoutContext, 0, lithoLayoutContext.layoutCache, null)

    val result: LithoLayoutResult = node.calculateLayout(context, sizeConstraints)

    return if (result !is NullLithoLayoutResult) {
      result
    } else {
      null
    }
  }

  @JvmStatic
  fun measure(
      lithoLayoutContext: LithoLayoutContext,
      parentContext: ComponentContext,
      holder: DeferredLithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int
  ): LithoLayoutResult? {
    val layout: LithoLayoutResult? =
        measureDeferredNode(lithoLayoutContext, parentContext, holder, widthSpec, heightSpec)
    val currentLayout: LithoLayoutResult? = holder.result
    // Set new created LayoutResult for future access
    if (layout != null && layout !== currentLayout) {
      holder.layoutOutput._actualDeferredNodeResult = layout
    }
    return layout
  }

  /**
   * In order to reuse render unit, we have to make sure layout data which render unit relies on is
   * determined before collecting layout results. So we're doing three things here:<br></br>
   * 1. Resolve DeferredNode.<br></br>
   * 2. Measure Primitive that were skipped due to fixed size.<br></br>
   * 3. Invoke OnBoundsDefined for all MountSpecs.<br></br>
   */
  @JvmStatic
  fun measurePendingSubtrees(
      parentContext: ComponentContext,
      lithoLayoutContext: LithoLayoutContext,
      reductionState: ReductionState,
      result: LithoLayoutResult,
  ) {
    if (lithoLayoutContext.isFutureReleased || result.measureHadExceptions) {
      // Exit early if the layout future as been released or if this result had exceptions.
      return
    }
    val lithoNode: LithoNode = result.node
    val component: Component = lithoNode.tailComponent
    val isTracing: Boolean = ComponentsSystrace.isTracing

    if (result is DeferredLithoLayoutResult) {
      // If the deferred node is defined, it has been resolved during a measure call during
      // layout calculation.
      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("resolveDeferredNode:${component.simpleName}")
            .arg("widthSpec", "EXACTLY ${result.width}")
            .arg("heightSpec", "EXACTLY ${result.height}")
            .arg("rootComponentId", lithoNode.tailComponent.instanceId)
            .flush()
      }
      val size: Int = lithoNode.componentCount
      val immediateParentContext: ComponentContext =
          if (size == 1) {
            parentContext
          } else {
            lithoNode.getComponentContextAt(1)
          }
      val actualLayoutResult: LithoLayoutResult? =
          measure(
              lithoLayoutContext = lithoLayoutContext,
              parentContext = immediateParentContext,
              holder = result,
              widthSpec = MeasureSpecUtils.exactly(result.width),
              heightSpec = MeasureSpecUtils.exactly(result.height))

      if (isTracing) {
        ComponentsSystrace.endSection()
      }

      if (actualLayoutResult == null) {
        return
      }

      Resolver.collectOutputs(actualLayoutResult.node)?.let { outputs ->
        reductionState.attachables
            .getOrCreate {
              ArrayList<Attachable>(outputs.attachables.size).also {
                reductionState.attachables = it
              }
            }
            .apply {
              addAll(outputs.attachables)
              actualLayoutResult.effects?.let { addAll(it) }
            }

        reductionState.transitionData
            .getOrCreate { MutableTransitionData().also { reductionState.transitionData = it } }
            .add(outputs.transitionData)
        for ((state, readers) in outputs.stateReads) reductionState.stateReads
            .getOrPut(state) { mutableScatterSetOf() }
            .addAll(readers)
      }

      try {
        measurePendingSubtrees(
            parentContext = parentContext,
            lithoLayoutContext = lithoLayoutContext,
            reductionState = reductionState,
            result = actualLayoutResult)
      } catch (e: Exception) {
        throw ComponentUtils.wrapWithMetadata(parentContext, e)
      }
      return
    } else if (result.childrenCount > 0) {
      val context: ComponentContext = result.node.tailComponentContext
      for (i in 0 until result.childCount) {
        val child: LithoLayoutResult = result.getChildAt(i)
        try {
          measurePendingSubtrees(
              parentContext = context,
              lithoLayoutContext = lithoLayoutContext,
              reductionState = reductionState,
              result = child)
        } catch (e: Exception) {
          throw ComponentUtils.wrapWithMetadata(context, e)
        }
      }
    }

    LithoYogaLayoutFunction.onBoundsDefined(result)

    registerWorkingRange(reductionState, result)
  }

  /** Register working range for each node */
  @JvmStatic
  fun registerWorkingRange(reductionState: ReductionState, result: LithoLayoutResult) {
    val registrations: List<WorkingRangeContainer.Registration> =
        result.node.workingRangeRegistrations ?: return
    if (CollectionsUtils.isEmpty(registrations)) {
      return
    }

    val workingRange: WorkingRangeContainer =
        reductionState.workingRangeContainer.getOrCreate {
          WorkingRangeContainer().also { reductionState.workingRangeContainer = it }
        }
    val component: Component = result.node.tailComponent
    for (registration in registrations) {
      val interStagePropsContainer: InterStagePropsContainer? =
          if (component is SpecGeneratedComponent) {
            result.layoutData as InterStagePropsContainer?
          } else {
            null
          }
      workingRange.registerWorkingRange(
          registration.name,
          registration.workingRange,
          registration.scopedComponentInfo,
          interStagePropsContainer)
    }
  }

  @JvmStatic
  fun shouldComponentUpdate(layoutNode: LithoNode, diffNode: DiffNode? = null): Boolean {
    if (diffNode == null) {
      return true
    }
    val component: Component = layoutNode.tailComponent
    val scopedContext: ComponentContext = layoutNode.tailComponentContext

    // return true for primitives to exit early
    if (Component.isPrimitive(component)) {
      return true
    }

    try {
      return component.shouldComponentUpdate(
          getDiffNodeScopedContext(diffNode), diffNode.component, scopedContext, component)
    } catch (e: Exception) {
      ComponentUtils.handleWithHierarchy(scopedContext, component, e)
    }
    return true
  }

  private fun measureDeferredNode(
      lithoLayoutContext: LithoLayoutContext,
      parentContext: ComponentContext,
      holderResult: DeferredLithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int
  ): LithoLayoutResult? {

    // 1. Check if current layout result is compatible with size spec and can be reused or not
    val currentLayout: LithoLayoutResult? = holderResult.result
    if (currentLayout != null &&
        MeasureComparisonUtils.hasCompatibleSizeSpec(
            currentLayout.widthSpec,
            currentLayout.heightSpec,
            widthSpec,
            heightSpec,
            currentLayout.width,
            currentLayout.height)) {
      // Tell TreeState to keep state containers for the cache of DeferredNode, otherwise we'll end
      // up getting lost state containers when we commit layout state.
      Resolver.commitToLayoutStateRecursively(lithoLayoutContext.treeState, currentLayout.node)
      return currentLayout
    }

    // 2. Check if cached layout result is compatible and can be reused or not.
    val node: DeferredLithoNode = holderResult.node
    consumeAndGetCachedLayout(lithoLayoutContext, node, holderResult, widthSpec, heightSpec)?.let {
        cachedLayout ->
      // Tell TreeState to keep state containers for the cache of DeferredNode, otherwise we'll end
      // up getting lost state containers when we commit layout state.
      Resolver.commitToLayoutStateRecursively(lithoLayoutContext.treeState, cachedLayout.node)
      return cachedLayout
    }

    // 3. If component is not using OnCreateLayoutWithSizeSpec, we don't have to resolve it again
    // and we can simply re-measure the tree. This is for cases where component was measured with
    // Component.measure API but we could not find the cached layout result or cached layout result
    // was not compatible with given size spec.
    val component: Component = node.tailComponent
    if (currentLayout != null && !Component.isLayoutSpecWithSizeSpec(component)) {
      return measureTree(
          lithoLayoutContext = lithoLayoutContext,
          androidContext = currentLayout.context.androidContext,
          node = currentLayout.node,
          sizeConstraints = SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
    }

    // 4. If current layout result is not available or component uses OnCreateLayoutWithSizeSpec
    // then resolve the tree and measure it. At this point we know that current layout result and
    // cached layout result are not available or are not compatible with given size spec.

    // DeferredNode is used for two purposes i.e for components measured using Component.measure API
    // and for components which are OnCreateLayoutWithSizeSpec.
    // For components measured with measure API, we want to reuse the same global key calculated
    // during measure API call and for that we are using the cached node and accessing the global
    // key from it since DeferredNode will have incorrect global key for it.
    val globalKeyToReuse: String
    val treePropsToReuse: TreePropContainer?
    if (Component.isLayoutSpecWithSizeSpec(component)) {
      globalKeyToReuse = node.tailComponentKey
      treePropsToReuse = node.tailComponentContext.treePropContainer
    } else {
      globalKeyToReuse = checkNotNull(node.cachedNode).tailComponentKey
      treePropsToReuse = checkNotNull(node.cachedNode).tailComponentContext.treePropContainer
    }

    // 4.a Apply state updates early for layout phase
    lithoLayoutContext.treeState.applyStateUpdatesEarly(parentContext, component, null, true)
    val prevContext = parentContext.calculationStateContext

    return try {
      val nestedRsc =
          ResolveContext(
              treeId = lithoLayoutContext.treeId,
              cache = lithoLayoutContext.cache,
              treeState = lithoLayoutContext.treeState,
              layoutVersion = lithoLayoutContext.layoutVersion,
              rootComponentId = lithoLayoutContext.rootComponentId,
              isAccessibilityEnabled = lithoLayoutContext.isAccessibilityEnabled,
              treeFuture = null,
              currentRoot = null,
              isInLayout = true)
      parentContext.renderStateContext = nestedRsc

      // 4.b Create a new layout.
      val newNode: LithoNode? =
          Resolver.resolveImpl(
              nestedRsc,
              parentContext,
              widthSpec,
              heightSpec,
              component,
              true,
              globalKeyToReuse,
              treePropsToReuse)

      if (newNode == null) {
        // mark as error to prevent from resolving it again.
        holderResult.layoutOutput._measureHadExceptions = true
        return null
      }

      // TODO (T151239896): Revaluate copy into and freeze after common props are refactored
      holderResult.node.copyInto(newNode)
      newNode.applyParentDependentCommonProps(
          context = lithoLayoutContext,
          parentLayoutDirection = holderResult.node.layoutDirection,
      )

      val nestedLsc =
          LithoLayoutContext(
              treeId = nestedRsc.treeId,
              cache = nestedRsc.cache,
              rootContext = parentContext,
              treeState = nestedRsc.treeState,
              layoutVersion = nestedRsc.layoutVersion,
              rootComponentId = nestedRsc.rootComponentId,
              isAccessibilityEnabled = lithoLayoutContext.isAccessibilityEnabled,
              layoutCache = LayoutCache(), // disable caching deferred node results
              currentDiffTree = lithoLayoutContext.currentDiffTree,
              layoutStateFuture = null)

      // Set the DiffNode for the deferred node's result to consume during measurement.
      nestedLsc.setDiffNodeForDeferredNode(holderResult.diffNode)
      parentContext.setLithoLayoutContext(nestedLsc)

      // 4.b Measure the tree
      val result =
          measureTree(
              lithoLayoutContext = nestedLsc,
              androidContext = parentContext.androidContext,
              node = newNode,
              sizeConstraints = SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))

      CalculationContext.recordEventHandlers(nestedRsc, prevContext)
      CalculationContext.recordEventHandlers(nestedLsc, prevContext)

      result
    } finally {
      parentContext.calculationStateContext = prevContext
    }
  }

  private fun consumeAndGetCachedLayout(
      lithoLayoutContext: LithoLayoutContext,
      holder: DeferredLithoNode,
      holderResult: DeferredLithoLayoutResult,
      widthSpec: Int,
      heightSpec: Int
  ): LithoLayoutResult? {
    val cachedNode: LithoNode = holder.cachedNode ?: return null
    val resultCache: MeasuredResultCache = lithoLayoutContext.cache
    val component: Component = holder.tailComponent
    val cachedLayout: LithoLayoutResult = resultCache.getCachedResult(cachedNode) ?: return null

    // Consume the cached result
    resultCache.removeCachedResult(cachedNode)
    val hasValidDirection: Boolean =
        hasValidLayoutDirectionInActualLayoutResult(holderResult, cachedLayout)

    // Transfer the cached layout to the node it if it's compatible.
    if (hasValidDirection) {
      val hasCompatibleSizeSpec: Boolean =
          MeasureComparisonUtils.hasCompatibleSizeSpec(
              oldWidthSpec = cachedLayout.widthSpec,
              oldHeightSpec = cachedLayout.heightSpec,
              newWidthSpec = widthSpec,
              newHeightSpec = heightSpec,
              oldMeasuredWidth = cachedLayout.width,
              oldMeasuredHeight = cachedLayout.height)
      if (hasCompatibleSizeSpec) {
        return cachedLayout
      } else if (!Component.isLayoutSpecWithSizeSpec(component)) {
        return measureTree(
            lithoLayoutContext = lithoLayoutContext,
            androidContext = cachedLayout.context.androidContext,
            node = cachedLayout.node,
            sizeConstraints = SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec))
      }
    }
    return null
  }

  /**
   * Check that the root of the deferred node we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  private fun hasValidLayoutDirectionInActualLayoutResult(
      deferred: DeferredLithoLayoutResult,
      actual: LithoLayoutResult
  ): Boolean =
      actual.node.layoutDirection.isInherit ||
          actual.node.layoutDirection == deferred.node.layoutDirection

  /** DiffNode state should be retrieved from the committed LayoutState. */
  private fun getDiffNodeScopedContext(diffNode: DiffNode): ComponentContext =
      diffNode.scopedComponentInfo.context
}
