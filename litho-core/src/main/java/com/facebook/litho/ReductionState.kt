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
import androidx.collection.MutableScatterSet
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.state.StateId
import com.facebook.litho.transition.MutableTransitionData
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
    val rootX: Int,
    val rootY: Int,
    val root: LayoutResult? = null,
    val id: Int = LayoutState.idGenerator.getAndIncrement(),
    val previousLayoutStateId: Int =
        currentLayoutState?.id ?: LayoutState.NO_PREVIOUS_LAYOUT_STATE_ID,
    val mountableOutputs: MutableList<RenderTreeNode> = ArrayList(8),
    val componentRootName: String = componentContext.componentScope?.simpleName ?: "",
    val widthSpec: Int = getWidthSpec(sizeConstraints),
    val heightSpec: Int = getHeightSpec(sizeConstraints),
    val visibilityOutputs: MutableList<VisibilityOutput> = ArrayList(8),
    val testOutputs: MutableList<TestOutput>? =
        if (ComponentsConfiguration.isEndToEndTestRun) ArrayList(8) else null,
    val componentScopes: MutableList<ScopedComponentInfo> = ArrayList(),
    val componentKeyToBounds: MutableMap<String, Rect> = HashMap(),
    val componentHandleToBounds: MutableMap<Handle, Rect> = HashMap(),
    val duplicatedTransitionIds: MutableSet<TransitionId> = HashSet(),
    val transitionIdMapping: MutableMap<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> =
        LinkedHashMap(),
    val mountableOutputTops: ArrayList<IncrementalMountOutput> = ArrayList(),
    val mountableOutputBottoms: ArrayList<IncrementalMountOutput> = ArrayList(),
    val incrementalMountOutputs: MutableMap<Long, IncrementalMountOutput> = LinkedHashMap(8),
    val renderUnitIdsWhichHostRenderTrees: MutableSet<Long> = HashSet(4),
    val dynamicValueOutputs: MutableMap<Long, DynamicValueOutput> = LinkedHashMap(8),
    val animatableItems: LongSparseArray<AnimatableItem> = LongSparseArray(8),
    val outputsIdToPositionMap: LongSparseArray<Int> = LongSparseArray(8),
    var layoutResult: LayoutResult? = root,
    var width: Int = 0,
    var height: Int = 0,
    var rootNode: LithoNode? = null,
    var diffTreeRoot: DiffNode? = null,
    var currentTransitionId: TransitionId? = currentLayoutState?.currentTransitionId,
    var currentLayoutOutputAffinityGroup: OutputUnitsAffinityGroup<AnimatableItem>? =
        currentLayoutState?.currentLayoutOutputAffinityGroup,
    var hasComponentsExcludedFromIncrementalMount: Boolean = false,
    var attachables: MutableList<Attachable>? = null,
    var transitionData: MutableTransitionData? = null,
    var workingRangeContainer: WorkingRangeContainer? = null,
    val stateReads: MutableMap<StateId, MutableScatterSet<String>> = mutableMapOf(),
)
