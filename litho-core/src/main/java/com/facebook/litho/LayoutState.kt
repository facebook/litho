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
import android.util.Pair
import androidx.annotation.VisibleForTesting
import androidx.collection.LongSparseArray
import com.facebook.litho.EndToEndTestingExtension.EndToEndTestingExtensionInput
import com.facebook.litho.Transition.RootBoundsTransition
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.transition.TransitionData
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderTree
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.SizeConstraints.Helper.getHeightSpec
import com.facebook.rendercore.SizeConstraints.Helper.getWidthSpec
import com.facebook.rendercore.Systracer
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import com.facebook.rendercore.transitions.TransitionsExtensionInput
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer
import com.facebook.rendercore.visibility.VisibilityExtensionInput
import com.facebook.rendercore.visibility.VisibilityOutput
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.CheckReturnValue

/**
 * The main role of [LayoutState] is to hold the output of layout calculation. This includes
 * mountable outputs and visibility outputs. A centerpiece of the class is
 * [LithoReducer#setSizeAfterMeasureAndCollectResults(ComponentContext, LithoLayoutContext, LayoutState)]
 * which prepares the before-mentioned outputs based on the provided [LithoNode] for later use in
 * [MountState].
 *
 * @property componentTreeId the id of the [ComponentTree] that generated this [LayoutState]
 *
 * This needs to be accessible to statically mock the class in tests.
 */
class LayoutState
internal constructor(
    val resolveResult: ResolveResult,
    val sizeConstraints: SizeConstraints,
    val componentTreeId: Int,
    val isAccessibilityEnabled: Boolean,
    val layoutCacheData: Map<Any, Any?>?,
    // needed to be var as it's being reset via consumeCreatedEventHandlers
    private var createdEventHandlers: List<Pair<String, EventHandler<*>>>?,
    reductionState: ReductionState
) :
    IncrementalMountExtensionInput,
    VisibilityExtensionInput,
    TransitionsExtensionInput,
    EndToEndTestingExtensionInput,
    PotentiallyPartialResult,
    DynamicPropsExtensionInput {

  private val animatableItems: LongSparseArray<AnimatableItem> = reductionState.animatableItems
  private val outputsIdToPositionMap: LongSparseArray<Int> = reductionState.outputsIdToPositionMap
  private val incrementalMountOutputs: Map<Long, IncrementalMountOutput> =
      reductionState.incrementalMountOutputs
  private val mountableOutputTops: ArrayList<IncrementalMountOutput> =
      reductionState.mountableOutputTops
  private val mountableOutputBottoms: ArrayList<IncrementalMountOutput> =
      reductionState.mountableOutputBottoms

  private val testOutputs: List<TestOutput>? = reductionState.testOutputs
  private val workingRangeContainer: WorkingRangeContainer? = reductionState.workingRangeContainer

  internal val transitionData: TransitionData? = reductionState.transitionData

  val root: LithoNode? = reductionState.rootNode
  val diffTree: DiffNode? = reductionState.diffTreeRoot
  val mountableOutputs: List<RenderTreeNode> = reductionState.mountableOutputs

  val componentKeyToBounds: Map<String, Rect> = reductionState.componentKeyToBounds
  val componentHandleToBounds: Map<Handle, Rect> = reductionState.componentHandleToBounds

  val width: Int = reductionState.width
  val height: Int = reductionState.height
  /** Id of this [LayoutState]. */
  val id: Int = reductionState.id
  /** Id of the [LayoutState] that was compared to when calculating this [LayoutState]. */
  val previousLayoutStateId: Int = reductionState.previousLayoutStateId
  val currentTransitionId: TransitionId? = reductionState.currentTransitionId
  val attachables: List<Attachable>? = reductionState.attachables
  /** Whether or not there are components marked as 'ExcludeFromIncrementalMount'. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  val hasComponentsExcludedFromIncrementalMount: Boolean =
      reductionState.hasComponentsExcludedFromIncrementalMount
  val currentLayoutOutputAffinityGroup: OutputUnitsAffinityGroup<AnimatableItem>? =
      reductionState.currentLayoutOutputAffinityGroup

  override val tracer: Systracer = ComponentsSystrace.systrace
  override val rootTransitionId: TransitionId? =
      LithoNodeUtils.createTransitionId(resolveResult.node)
  /** Gets a mapping from transition ids to a group of LayoutOutput. */
  override val transitionIdMapping: Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> =
      reductionState.transitionIdMapping
  override val visibilityOutputs: List<VisibilityOutput> = reductionState.visibilityOutputs
  override val renderUnitIdsWhichHostRenderTrees: Set<Long> =
      reductionState.renderUnitIdsWhichHostRenderTrees
  val rootComponent: Component
    get() = resolveResult.component

  val componentHandles: Set<Handle>
    get() = componentHandleToBounds.keys

  val isEmpty: Boolean
    get() = resolveResult.component is EmptyComponent

  val isActivityValid: Boolean
    get() = ContextUtils.getValidActivityForContext(resolveResult.context.androidContext) != null

  val visibilityOutputCount: Int
    get() = visibilityOutputs.size

  val componentContext: ComponentContext
    get() = resolveResult.context

  /**
   * Returns the state handler instance currently held by LayoutState.
   *
   * @return the state handler
   */
  @get:CheckReturnValue
  val treeState: TreeState
    get() = resolveResult.treeState

  override val testOutputCount: Int
    get() = testOutputs?.size ?: 0

  override val dynamicValueOutputs: Map<Long, DynamicValueOutput> =
      reductionState.dynamicValueOutputs
  override val isPartialResult: Boolean = false

  override val animatableRootItem: AnimatableItem?
    get() = animatableItems[MountState.ROOT_HOST_ID]

  override val treeId: Int
    get() = componentTreeId

  override val transitions: List<Transition>?
    get() = transitionData?.transitions

  override val isIncrementalMountEnabled: Boolean
    get() = ComponentContext.isIncrementalMountEnabled(resolveResult.context)

  override val rootName: String
    get() = resolveResult.component.simpleName

  override val visibilityBoundsTransformer: VisibilityBoundsTransformer?
    get() = componentContext.lithoConfiguration.componentsConfig.visibilityBoundsTransformer

  override val isProcessingVisibilityOutputsEnabled: Boolean
    get() = shouldProcessVisibilityOutputs

  private var cachedRenderTree: RenderTree? = null // memoized RenderTree
  // TODO(t66287929): Remove isCommitted from LayoutState by matching RenderState logic around
  //  Futures.
  private var isCommitted = false
  // needed to be var as it's being updated in setShouldProcessVisibilityOutputs
  private var shouldProcessVisibilityOutputs = false
  // needed to be var as it's being reset via consumeScopedSpecComponentInfos
  private var scopedSpecComponentInfos: List<ScopedComponentInfo>? =
      reductionState.scopedSpecComponentInfos

  // needed to be var as a previously evaluated reference is set(restored) in LithoViewTestHelper
  var rootLayoutResult: LayoutResult? = reductionState.layoutResult
    internal set

  // needed to be var as it's being updated in setInitialRootBoundsForAnimation
  var rootWidthAnimation: RootBoundsTransition? = null
    private set

  // needed to be var as it's being updated in setInitialRootBoundsForAnimation
  var rootHeightAnimation: RootBoundsTransition? = null
    private set

  fun consumeScopedSpecComponentInfos(): List<ScopedComponentInfo>? {
    val scopedSpecComponentInfos = scopedSpecComponentInfos
    this.scopedSpecComponentInfos = null
    return scopedSpecComponentInfos
  }

  fun consumeCreatedEventHandlers(): List<Pair<String, EventHandler<*>>>? {
    val createdEventHandlers = createdEventHandlers
    this.createdEventHandlers = null
    return createdEventHandlers
  }

  fun toRenderTree(): RenderTree {
    cachedRenderTree?.let {
      return it
    }
    val root = mountableOutputs[0]
    check(root.renderUnit.id == MountState.ROOT_HOST_ID) {
      "Root render unit has invalid id ${root.renderUnit.id}"
    }
    val flatList = Array(mountableOutputs.size) { i -> mountableOutputs[i] }
    val renderTree =
        RenderTree.create(
            root,
            flatList,
            outputsIdToPositionMap,
            sizeConstraints.encodedValue,
            componentTreeId,
            null,
            null)
    cachedRenderTree = renderTree
    return renderTree
  }

  fun isCompatibleSpec(widthSpec: Int, heightSpec: Int): Boolean {
    val widthIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            getWidthSpec(sizeConstraints), widthSpec, width)
    val heightIsCompatible =
        MeasureComparisonUtils.isMeasureSpecCompatible(
            getHeightSpec(sizeConstraints), heightSpec, height)
    return widthIsCompatible && heightIsCompatible
  }

  fun isCompatibleComponentAndSpec(componentId: Int, widthSpec: Int, heightSpec: Int): Boolean =
      resolveResult.component.id == componentId && isCompatibleSpec(widthSpec, heightSpec)

  fun isCompatibleSize(width: Int, height: Int): Boolean =
      this.width == width && this.height == height

  fun isForComponentId(componentId: Int): Boolean = resolveResult.component.id == componentId

  override fun getMountableOutputCount(): Int = mountableOutputs.size

  override fun getIncrementalMountOutputCount(): Int = incrementalMountOutputs.size

  override fun getMountableOutputAt(position: Int): RenderTreeNode = mountableOutputs[position]

  override fun getIncrementalMountOutputForId(id: Long): IncrementalMountOutput? =
      incrementalMountOutputs[id]

  override fun getIncrementalMountOutputs(): Collection<IncrementalMountOutput> =
      incrementalMountOutputs.values

  override fun getAnimatableItem(id: Long): AnimatableItem? = animatableItems[id]

  override fun getOutputsOrderedByTopBounds(): List<IncrementalMountOutput> = mountableOutputTops

  override fun getOutputsOrderedByBottomBounds(): List<IncrementalMountOutput> =
      mountableOutputBottoms

  fun getVisibilityOutputAt(index: Int): VisibilityOutput = visibilityOutputs[index]

  override fun getTestOutputAt(position: Int): TestOutput? = testOutputs?.get(position)

  fun getWidthSpec(): Int = getWidthSpec(sizeConstraints)

  fun getHeightSpec(): Int = getHeightSpec(sizeConstraints)

  // If the layout root is a nested tree holder node, it gets skipped immediately while
  // collecting the LayoutOutputs. The nested tree itself effectively becomes the layout
  // root in this case.
  fun isLayoutRoot(result: LithoLayoutResult): Boolean =
      if (rootLayoutResult is NestedTreeHolderResult)
          result === (rootLayoutResult as NestedTreeHolderResult).nestedResult
      else result === rootLayoutResult

  /**
   * @return the position of the [LithoRenderUnit] with id layoutOutputId in the [LayoutState] list
   *   of outputs or -1 if no [LithoRenderUnit] with that id exists in the [LayoutState]
   */
  override fun getPositionForId(id: Long): Int = checkNotNull(outputsIdToPositionMap.get(id, -1))

  override fun renderUnitWithIdHostsRenderTrees(id: Long): Boolean =
      renderUnitIdsWhichHostRenderTrees.contains(id)

  /** Gets a group of LayoutOutput given transition key */
  override fun getAnimatableItemForTransitionId(
      transitionId: TransitionId
  ): OutputUnitsAffinityGroup<AnimatableItem>? = transitionIdMapping[transitionId]

  override fun setInitialRootBoundsForAnimation(
      rootWidth: RootBoundsTransition?,
      rootHeight: RootBoundsTransition?
  ) {
    rootWidthAnimation = rootWidth
    rootHeightAnimation = rootHeight
  }

  override fun getMountTimeTransitions(
      previousInput: TransitionsExtensionInput?
  ): List<Transition>? {
    previousInput as LayoutState?
    var mountTimeTransitions: MutableList<Transition>? = null
    val mountedLayoutStateId = previousInput?.id ?: NO_PREVIOUS_LAYOUT_STATE_ID
    if (transitionData != null && !transitionData.isEmpty()) {
      mountTimeTransitions = ArrayList()
      val canUseOptimisticTransitions = previousLayoutStateId == mountedLayoutStateId
      if (canUseOptimisticTransitions) {
        val optimisticTransitions = transitionData.optimisticTransitions
        if (optimisticTransitions != null) mountTimeTransitions.addAll(optimisticTransitions)
      }
      val previousTreeState = previousInput?.treeState
      val transitionCreators = transitionData.transitionCreators
      if (transitionCreators != null) {
        for (creator in transitionCreators) {
          if (creator.supportsOptimisticTransitions && canUseOptimisticTransitions) continue
          val previousRenderData = previousTreeState?.getPreviousRenderData(creator.identityKey)
          val transition = creator.createTransition(previousRenderData)
          if (transition != null) mountTimeTransitions.add(transition)
        }
      }
    }
    // Use the current resolved TreeState here rather than the previous one
    val updateStateTransitions = treeState.pendingStateUpdateTransitions
    if (updateStateTransitions.isNotEmpty()) {
      if (mountTimeTransitions == null) {
        mountTimeTransitions = ArrayList()
      }
      mountTimeTransitions.addAll(updateStateTransitions)
    }
    return mountTimeTransitions
  }

  @JvmName("recordRenderData")
  internal fun recordRenderData() {
    treeState.recordRenderData(this)
  }

  /** Debug-only: return a string representation of this LayoutState and its LayoutOutputs. */
  fun dumpAsString(): String {
    if (!LithoDebugConfigurations.isDebugModeEnabled &&
        !ComponentsConfiguration.isEndToEndTestRun) {
      throw RuntimeException(
          "LayoutState#dumpAsString() should only be called in debug mode or from e2e tests!")
    }
    var res =
        """LayoutState w/ ${getMountableOutputCount()} mountable outputs, root: ${resolveResult.component}
"""
    for (i in 0 until getMountableOutputCount()) {
      val node = getMountableOutputAt(i)
      val renderUnit = LithoRenderUnit.getRenderUnit(node)
      res +=
          """  [$i] id: ${node.renderUnit.id}, host: ${node.parent?.renderUnit?.id ?: -1}, component: ${renderUnit.component.simpleName}
"""
    }
    return res
  }

  fun checkWorkingRangeAndDispatch(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int,
      stateHandler: WorkingRangeStatusHandler
  ) {
    if (workingRangeContainer == null) {
      return
    }
    workingRangeContainer.checkWorkingRangeAndDispatch(
        position,
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex,
        stateHandler)
  }

  fun dispatchOnExitRangeIfNeeded(stateHandler: WorkingRangeStatusHandler) {
    if (workingRangeContainer == null) {
      return
    }
    workingRangeContainer.dispatchOnExitedRangeIfNeeded(stateHandler)
  }

  override fun needsToRerunTransitions(): Boolean {
    val stateUpdater = resolveResult.context.stateUpdater
    return stateUpdater?.isFirstMount == true
  }

  override fun setNeedsToRerunTransitions(needsToRerunTransitions: Boolean) {
    val stateUpdater = resolveResult.context.stateUpdater
    if (stateUpdater != null) {
      stateUpdater.isFirstMount = needsToRerunTransitions
    }
  }

  fun isCommitted(): Boolean = isCommitted

  fun markCommitted() {
    isCommitted = true
  }

  fun setShouldProcessVisibilityOutputs(value: Boolean) {
    shouldProcessVisibilityOutputs = value
  }

  fun getRenderTreeNode(output: IncrementalMountOutput): RenderTreeNode =
      getMountableOutputAt(output.index)

  companion object {
    @JvmStatic
    fun isFromSyncLayout(@RenderSource source: Int): Boolean =
        when (source) {
          RenderSource.MEASURE_SET_SIZE_SPEC,
          RenderSource.SET_ROOT_SYNC,
          RenderSource.UPDATE_STATE_SYNC,
          RenderSource.SET_SIZE_SPEC_SYNC -> true
          else -> false
        }

    @get:JvmStatic val idGenerator: AtomicInteger = AtomicInteger(1)

    const val NO_PREVIOUS_LAYOUT_STATE_ID: Int = -1

    @JvmStatic
    fun layoutSourceToString(@RenderSource source: Int): String =
        when (source) {
          RenderSource.SET_ROOT_SYNC -> "setRootSync"
          RenderSource.SET_SIZE_SPEC_SYNC -> "setSizeSpecSync"
          RenderSource.UPDATE_STATE_SYNC -> "updateStateSync"
          RenderSource.SET_ROOT_ASYNC -> "setRootAsync"
          RenderSource.SET_SIZE_SPEC_ASYNC -> "setSizeSpecAsync"
          RenderSource.UPDATE_STATE_ASYNC -> "updateStateAsync"
          RenderSource.MEASURE_SET_SIZE_SPEC -> "measure_setSizeSpecSync"
          RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC -> "measure_setSizeSpecAsync"
          RenderSource.TEST -> "test"
          RenderSource.NONE -> "none"
          else -> throw RuntimeException("Unknown calculate layout source: $source")
        }

    @JvmStatic
    @VisibleForTesting
    @OutputUnitType
    fun getTypeFromId(id: Long): Int {
      val masked = id and -0x100000000L
      return (masked shr 32).toInt()
    }

    @JvmStatic
    fun isNullOrEmpty(layoutState: LayoutState?): Boolean =
        layoutState == null || layoutState.isEmpty
  }
}
