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
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.SizeConstraints.Helper.getHeightSpec
import com.facebook.rendercore.SizeConstraints.Helper.getWidthSpec
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import com.facebook.rendercore.visibility.VisibilityOutput

/**
 * A data structure that holds all the information needed to perform a reduction pass, which is
 * going to be transferred to [LayoutState] in the end.
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
internal data class ReductionState(
    val componentContext: ComponentContext,
    val sizeConstraints: SizeConstraints,
    val currentLayoutState: LayoutState?,
    val offsetRootX: Int,
    val offsetRootY: Int,
    val root: LayoutResult? = null,
    var attachables: MutableList<Attachable>? = null,
    var transitions: MutableList<Transition>? = null,
    var scopedComponentInfosNeedingPreviousRenderData: MutableList<ScopedComponentInfo>? = null,
    var workingRangeContainer: WorkingRangeContainer? = null,
) {
  var layoutResult: LayoutResult? = root
  var width: Int = 0
  var height: Int = 0
  var rootNode: LithoNode? = null
  var diffTreeRoot: DiffNode? = null
  var currentTransitionId: TransitionId? = null
  var currentLayoutOutputAffinityGroup: OutputUnitsAffinityGroup<AnimatableItem>? = null
  var hasComponentsExcludedFromIncrementalMount: Boolean = false

  val mountableOutputs: MutableList<RenderTreeNode> = ArrayList(8)
  val componentRootName: String = componentContext.componentScope?.simpleName ?: ""
  val widthSpec: Int = getWidthSpec(sizeConstraints)
  val heightSpec: Int = getHeightSpec(sizeConstraints)
  val visibilityOutputs: MutableList<VisibilityOutput> = ArrayList(8)
  val testOutputs: MutableList<TestOutput> =
      if (ComponentsConfiguration.isEndToEndTestRun) ArrayList(8) else ArrayList(0)
  val scopedSpecComponentInfos: MutableList<ScopedComponentInfo> = ArrayList()
  val componentKeyToBounds: MutableMap<String, Rect> = HashMap()
  val componentHandleToBounds: MutableMap<Handle, Rect> = HashMap()
  val duplicatedTransitionIds: MutableSet<TransitionId> = HashSet()
  val transitionIdMapping: MutableMap<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> =
      LinkedHashMap()
  val mountableOutputTops: MutableList<IncrementalMountOutput> = ArrayList()
  val mountableOutputBottoms: MutableList<IncrementalMountOutput> = ArrayList()
  val incrementalMountOutputs: MutableMap<Long, IncrementalMountOutput> = LinkedHashMap(4)
  val renderUnitIdsWhichHostRenderTrees: MutableSet<Long> = HashSet(4)
  val renderUnitsWithViewAttributes: MutableMap<Long, ViewAttributes> = HashMap(8)
  val dynamicValueOutputs: MutableMap<Long, DynamicValueOutput> = LinkedHashMap()
  val animatableItems: LongSparseArray<AnimatableItem> = LongSparseArray<AnimatableItem>(8)
  val outputsIdToPositionMap: LongSparseArray<Int> = LongSparseArray<Int>(8)
  val id: Int = LayoutState.getIdGenerator().getAndIncrement()
  val previousLayoutStateId: Int = currentLayoutState?.mId ?: 0
  val rootX: Int = offsetRootX
  val rootY: Int = offsetRootY

  fun isLayoutRoot(result: LithoLayoutResult): Boolean {
    val lr = layoutResult
    return if (lr is NestedTreeHolderResult) {
      result == lr.nestedResult
    } else {
      result == lr
    }
  }

  fun createLayoutStateFromReductionState(
      lsc: LithoLayoutContext,
      resolveResult: ResolveResult,
      treeId: Int,
      layoutCache: LayoutCache
  ): LayoutState {
    val layoutState =
        LayoutState(
            id,
            resolveResult,
            sizeConstraints,
            lsc.rootOffset.x,
            lsc.rootOffset.y,
            treeId,
            lsc.isAccessibilityEnabled,
            currentLayoutState,
            layoutCache.writeCacheData,
            this)
    layoutState.setCreatedEventHandlers(mergeLists(resolveResult.eventHandlers, lsc.eventHandlers))
    mergeReductionStateIntoLayoutState(layoutState)
    return layoutState
  }

  fun mergeReductionStateIntoLayoutState(layoutState: LayoutState) {
    layoutState.mMountableOutputs.addAll(mountableOutputs)
    layoutState.mWidth = width
    layoutState.mHeight = height
    layoutState.mLayoutResult = layoutResult
    layoutState.mRoot = rootNode
    layoutState.mDiffTreeRoot = diffTreeRoot
    layoutState.mCurrentTransitionId = currentTransitionId
    layoutState.mCurrentLayoutOutputAffinityGroup = currentLayoutOutputAffinityGroup
    layoutState.mVisibilityOutputs = visibilityOutputs
    layoutState.mTestOutputs?.addAll(testOutputs)
    layoutState.mScopedSpecComponentInfos = scopedSpecComponentInfos
    layoutState.mComponentKeyToBounds.putAll(componentKeyToBounds)
    layoutState.mComponentHandleToBounds.putAll(componentHandleToBounds)
    layoutState.mDuplicatedTransitionIds.addAll(duplicatedTransitionIds)
    layoutState.mTransitionIdMapping.putAll(transitionIdMapping)
    layoutState.mMountableOutputTops.addAll(mountableOutputTops)
    layoutState.mMountableOutputBottoms.addAll(mountableOutputBottoms)
    layoutState.mIncrementalMountOutputs.putAll(incrementalMountOutputs)
    layoutState.mHasComponentsExcludedFromIncrementalMount =
        hasComponentsExcludedFromIncrementalMount
    layoutState.mRenderUnitIdsWhichHostRenderTrees.addAll(renderUnitIdsWhichHostRenderTrees)
    layoutState.mRenderUnitsWithViewAttributes.putAll(renderUnitsWithViewAttributes)
    layoutState.mDynamicValueOutputs.putAll(dynamicValueOutputs)
    layoutState.mAnimatableItems.putAll(animatableItems)
    layoutState.mOutputsIdToPositionMap.putAll(outputsIdToPositionMap)
  }
}
