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
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.facebook.litho.Transition.RootBoundsTransition
import com.facebook.litho.TransitionManager.Companion.getRootTransition
import com.facebook.litho.TransitionManager.OnAnimationCompleteListener
import com.facebook.litho.TransitionsExtension.TransitionsExtensionState
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.animation.PropertyHandle
import com.facebook.rendercore.ErrorReporter.report
import com.facebook.rendercore.Function
import com.facebook.rendercore.Host
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.MountDelegateTarget
import com.facebook.rendercore.MountItem
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.UnmountDelegateExtension
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.thread.utils.ThreadUtils.assertMainThread
import com.facebook.rendercore.transitions.AnimatedRootHost
import com.facebook.rendercore.transitions.DisappearingHost
import com.facebook.rendercore.transitions.TransitionRenderUnit
import com.facebook.rendercore.transitions.TransitionUtils
import com.facebook.rendercore.transitions.TransitionsExtensionInput
import com.facebook.rendercore.utils.BoundsUtils.applyBoundsToMountContent
import com.meta.systrace.Systracer
import java.util.ArrayDeque
import kotlin.math.max

/** Extension for performing transitions. */
class TransitionsExtension private constructor(private val debugTag: String?) :
    MountExtension<TransitionsExtensionInput, TransitionsExtensionState>(),
    UnmountDelegateExtension<TransitionsExtensionState>,
    OnItemCallbacks<TransitionsExtensionState> {
  class TransitionsExtensionState(internal val debugTag: String?) {
    internal val disappearingMountItems:
        MutableMap<TransitionId?, OutputUnitsAffinityGroup<MountItem?>> =
        LinkedHashMap<TransitionId?, OutputUnitsAffinityGroup<MountItem?>>()
    internal val lockedDisappearingMountItems: MutableMap<RenderUnit<*>?, AnimatableItem?> =
        HashMap<RenderUnit<*>?, AnimatableItem?>()
    internal val currentlyDisappearingItems: MutableMap<MountItem?, Host?> =
        HashMap<MountItem?, Host?>()
    internal var input: TransitionsExtensionInput? = null
    internal var lastMountedTreeId: Int = UNSET
    internal var transitionManager: TransitionManager? = null
    internal val animatingTransitionIds = HashSet<TransitionId?>()

    internal var transitionsHasBeenCollected = false
    internal var rootTransition: Transition? = null
    internal var lastTransitionsExtensionInput: TransitionsExtensionInput? = null

    companion object {
      internal val UNSET = -1
    }
  }

  private class AnimationCompleteListener(
      private val extensionState: ExtensionState<TransitionsExtensionState>,
  ) : OnAnimationCompleteListener<Function<TransitionEndEvent?>?> {
    private val state: TransitionsExtensionState = extensionState.state

    override fun onAnimationComplete(transitionId: TransitionId) {
      val disappearingGroup = state.disappearingMountItems.remove(transitionId)
      if (disappearingGroup != null) {
        endUnmountDisappearingItem(extensionState, disappearingGroup)
      } else {
        if (!state.animatingTransitionIds.remove(transitionId)) {
          if (extensionState.state.debugTag != null) {
            Log.e(
                extensionState.state.debugTag,
                "Ending animation for id $transitionId but it wasn't recorded as animating!",
            )
          }
        }

        val lastInput = state.lastTransitionsExtensionInput
        val animatableItemGroup = lastInput?.getAnimatableItemForTransitionId(transitionId)
        if (animatableItemGroup == null) {
          // This can happen if the component was unmounted without animation or the transitionId
          // was removed from the component.
          return
        }

        for (i in 0 until animatableItemGroup.size()) {
          val animatableItem = checkNotNull(animatableItemGroup.getAt(i))
          val position = checkNotNull(lastInput).getPositionForId(animatableItem.getId())
          updateAnimationLockCount(
              extensionState,
              lastInput,
              position,
              false,
          )
        }
      }
    }

    override fun onAnimationUnitComplete(
        propertyHandle: PropertyHandle,
        data: Function<TransitionEndEvent?>?,
    ) {
      if (data != null) {
        data.call(
            TransitionEndEvent(
                checkNotNull(propertyHandle.transitionId.reference),
                propertyHandle.property,
            ),
        )
      }
    }
  }

  override fun shouldDelegateUnmount(
      extensionState: ExtensionState<TransitionsExtensionState>,
      mountItem: MountItem,
  ): Boolean {
    val state = extensionState.state
    return state.lockedDisappearingMountItems.containsKey(mountItem.renderTreeNode.renderUnit)
  }

  override fun unmount(
      extensionState: ExtensionState<TransitionsExtensionState>,
      mountItem: MountItem,
      host: Host?,
  ) {
    val state = extensionState.state
    val renderUnit = mountItem.renderTreeNode.renderUnit
    val animatableItem = state.lockedDisappearingMountItems[renderUnit]
    val transitionId = checkNotNull(animatableItem).getTransitionId()
    val group = state.disappearingMountItems[transitionId]
    if (group != null) {
      val isRoot = group.get(animatableItem.getOutputType()) != null
      // We only start unmount disappearing item on the root of the disappearing animation. The rest
      // will be unmounted after the animation finishes.
      if (isRoot && host != null) {
        (host as DisappearingHost).startDisappearingMountItem(mountItem)
        state.currentlyDisappearingItems[mountItem] = host
      }
    } else {
      // Recovery path. The deferred-cleanup contract above ("the rest will be unmounted after the
      // animation finishes") only fires while the matching group is alive. If the group is gone
      // but the lockedDisappearingMountItems entry remains, no cleanup will run — the View stays
      // in the host while MountState already removed the item from `idToMountedItemMap`, tripping
      // the leftover-items assertion in MountState.unmountItemRecursively.
      // Mirror unmountDisappearingItem's post-animation cleanup: detach from host, then
      // unbindMountItem to run unbind/unmount-content/release (which MountState skips because
      // hasUnmountDelegate is true), and clear the stale entry.
      host?.unmount(mountItem)
      getMountTarget(extensionState).unbindMountItem(mountItem)
      state.lockedDisappearingMountItems.remove(renderUnit)
    }
  }

  override fun createState(): TransitionsExtensionState {
    return TransitionsExtensionState(debugTag)
  }

  override fun beforeMount(
      extensionState: ExtensionState<TransitionsExtensionState>,
      input: TransitionsExtensionInput?,
      localVisibleRect: Rect?,
  ) {
    val state = extensionState.state
    state.input = input

    val resolvedInput = checkNotNull(input)
    if (resolvedInput.treeId != state.lastMountedTreeId) {
      state.lastTransitionsExtensionInput = null
    }

    val tracer = checkNotNull(resolvedInput.tracer)
    updateTransitions(extensionState, resolvedInput, debugTag, tracer)
    extractDisappearingItems(extensionState, resolvedInput)
  }

  override fun afterMount(extensionState: ExtensionState<TransitionsExtensionState>) {
    val state = extensionState.state
    maybeUpdateAnimatingMountContent(extensionState)

    val input = state.input ?: return
    if (shouldAnimateTransitions(state, input) && hasTransitionsToAnimate(state)) {
      val transitionManager = checkNotNull(state.transitionManager)
      transitionManager.runTransitions()
    }

    input.setNeedsToRerunTransitions(false)
    state.lastTransitionsExtensionInput = input
    state.transitionsHasBeenCollected = false
    state.lastMountedTreeId = input.treeId
  }

  override fun onUnmount(extensionState: ExtensionState<TransitionsExtensionState>) {
    resetAnimationState(extensionState)
    extensionState.releaseAllAcquiredReferences()
    clearLastMountedTreeId(extensionState)
  }

  override fun onUnbind(extensionState: ExtensionState<TransitionsExtensionState>) {
    extensionState.releaseAllAcquiredReferences()
  }

  override fun onBoundsAppliedToItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
      changed: Boolean,
  ) {
    val input = extensionState.state.input
    if (
        input != null &&
            input.isIncrementalMountEnabled &&
            extensionState.ownsReference(renderUnit.id) &&
            input.renderUnitWithIdHostsRenderTrees(renderUnit.id)
    ) {
      val view = content as View
      recursivelyNotifyVisibleBoundsChanged(view)
    }
  }

  override fun beforeMountItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderTreeNode: RenderTreeNode,
      index: Int,
  ): Unit = Unit

  override fun onMountItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
  ): Unit = Unit

  override fun shouldUpdateItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      previousRenderUnit: RenderUnit<*>,
      previousLayoutData: Any?,
      nextRenderUnit: RenderUnit<*>,
      nextLayoutData: Any?,
  ): Boolean {
    return false
  }

  override fun onBindItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
  ): Unit = Unit

  override fun onUnbindItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
  ): Unit = Unit

  override fun onUnmountItem(
      extensionState: ExtensionState<TransitionsExtensionState>,
      renderUnit: RenderUnit<*>,
      content: Any,
      layoutData: Any?,
  ): Unit = Unit

  companion object {
    val instance: TransitionsExtension = TransitionsExtension(null)
    private var debugInstance: TransitionsExtension? = null

    fun getInstance(debugTag: String?): TransitionsExtension {
      if (debugTag != null) {
        var result = debugInstance
        if (result == null) {
          result = TransitionsExtension(debugTag)
          debugInstance = result
        }
        return result
      } else {
        return instance
      }
    }

    fun clearLastMountedTreeId(extensionState: ExtensionState<TransitionsExtensionState>) {
      val state = extensionState.state
      state.lastMountedTreeId = TransitionsExtensionState.UNSET
    }

    /**
     * Creates and updates transitions for a new TransitionsExtensionInput. The steps are as
     * follows:
     * 1. Disappearing items: Update disappearing mount items that are no longer disappearing (e.g.
     *    because they came back). This means canceling the animation and cleaning up the
     *    corresponding Host.
     * 2. New transitions: Use the transition manager to create new animations.
     * 3. Update locked indices: Based on running/new animations, there are some mount items we want
     *    to make sure are not unmounted due to incremental mount and being outside of visibility
     *    bounds.
     */
    private fun updateTransitions(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput,
        debugTag: String?,
        tracer: Systracer,
    ) {
      val state = extensionState.state
      tracer.beginSection("MountState.updateTransitions")

      try {
        // If this is a new component tree but isn't the first time it's been mounted, then we
        // shouldn't
        // do any transition animations for changed mount content as it's just being remounted on a
        // new RootHost.
        val treeId = input.treeId
        if (state.lastMountedTreeId != treeId) {
          resetAnimationState(extensionState)
          val currentInput = checkNotNull(state.input)
          if (!currentInput.needsToRerunTransitions()) {
            // Don't re-trigger appear animations were scrolled back onto the screen
            return
          }
        }

        if (state.disappearingMountItems.isNotEmpty()) {
          updateDisappearingMountItems(extensionState, input)
        }

        if (shouldAnimateTransitions(state, input)) {
          collectAllTransitions(extensionState, input)
          if (hasTransitionsToAnimate(state)) {
            createNewTransitions(extensionState, input, state.rootTransition, debugTag)
          }
        }

        val transitionManager = state.transitionManager
        if (transitionManager != null) {
          transitionManager.finishUndeclaredTransitions()
        }
        extensionState.releaseAllAcquiredReferences()
        if (state.animatingTransitionIds.isNotEmpty()) {
          regenerateAnimationLockedIndices(extensionState, input)
        }
      } finally {
        tracer.endSection()
      }
    }

    private fun updateDisappearingMountItems(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput,
    ) {
      val state = extensionState.state

      val nextMountedTransitionIds = input.transitionIdMapping
      if (nextMountedTransitionIds != null) {
        for (transitionId in nextMountedTransitionIds.keys) {
          val disappearingItem = state.disappearingMountItems.remove(transitionId)
          if (disappearingItem != null) {
            endUnmountDisappearingItem(extensionState, disappearingItem)
          }
        }
      }
    }

    private fun resetAnimationState(extensionState: ExtensionState<TransitionsExtensionState>) {
      val state = extensionState.state
      val transitionManager = state.transitionManager ?: return

      for (group in state.disappearingMountItems.values) {
        endUnmountDisappearingItem(extensionState, group)
      }

      extensionState.releaseAllAcquiredReferences()
      state.disappearingMountItems.clear()
      state.lockedDisappearingMountItems.clear()
      state.animatingTransitionIds.clear()
      transitionManager.reset()
    }

    private fun regenerateAnimationLockedIndices(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput,
    ) {
      val state = extensionState.state
      val transitionMapping = input.transitionIdMapping
      if (transitionMapping != null) {
        for (transition in transitionMapping.entries) {
          if (!state.animatingTransitionIds.contains(transition.key)) {
            continue
          }

          val group = transition.value
          for (j in 0 until group.size()) {
            val position = input.getPositionForId(checkNotNull(group.getAt(j)).getId())
            updateAnimationLockCount(extensionState, input, position, true)
          }
        }
      }
      if (extensionState.state.debugTag != null) {
        for (i in 0 until input.getMountableOutputCount()) {
          val renderTreeNode = checkNotNull(input.getMountableOutputAt(i))
          if (extensionState.ownsReference(renderTreeNode.renderUnit.id)) {
            val animatableItem = checkNotNull(input.getAnimatableItem(renderTreeNode.renderUnit.id))
            val parentId = renderTreeNode.parent?.renderUnit?.id ?: "root"
            Log.d(
                extensionState.state.debugTag,
                "$i [${animatableItem.getId()}] (${animatableItem.getTransitionId()}) host => ($parentId)",
            )
          }
        }
      }
    }

    /** @return whether we should animate transitions. */
    private fun shouldAnimateTransitions(
        state: TransitionsExtensionState,
        input: TransitionsExtensionInput?,
    ): Boolean {
      val currentInput = state.input ?: return false
      return input != null &&
          (state.lastMountedTreeId == input.treeId || currentInput.needsToRerunTransitions())
    }

    /** @return whether we have any transitions to animate for the current mount */
    private fun hasTransitionsToAnimate(state: TransitionsExtensionState): Boolean {
      return state.rootTransition != null
    }

    /**
     * Collect transitions from layout time, mount time and from state updates.
     *
     * @param input provides transitions information for the current mount.
     */
    internal fun collectAllTransitions(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput?,
    ) {
      val state = extensionState.state
      assertMainThread()
      if (state.transitionsHasBeenCollected) {
        return
      }

      val allTransitions = ArrayList<Transition>()

      val resolvedInput = checkNotNull(input)
      if (resolvedInput.transitions != null) {
        @Suppress("UNCHECKED_CAST")
        allTransitions.addAll(resolvedInput.transitions as List<Transition>)
      }

      val previousInput = extensionState.state.lastTransitionsExtensionInput
      collectMountTimeTransitions(previousInput, resolvedInput, allTransitions)

      val rootWidthResult = RootBoundsTransition()
      val rootHeightResult = RootBoundsTransition()

      val rootTransitionId = resolvedInput.rootTransitionId

      if (rootTransitionId != null) {
        for (i in 0 until allTransitions.size) {
          val transition = allTransitions[i]
          checkNotNull(transition) {
            "NULL_TRANSITION when collecting root bounds anim. Root: ${resolvedInput.rootName}, root TransitionId: $rootTransitionId"
          }
          TransitionUtils.collectRootBoundsTransitions(
              rootTransitionId,
              transition,
              AnimatedProperties.WIDTH,
              rootWidthResult,
          )

          TransitionUtils.collectRootBoundsTransitions(
              rootTransitionId,
              transition,
              AnimatedProperties.HEIGHT,
              rootHeightResult,
          )
        }
      }

      val rootWidthTransition = if (rootWidthResult.hasTransition) rootWidthResult else null
      val rootHeightTransition = if (rootHeightResult.hasTransition) rootHeightResult else null

      resolvedInput.setInitialRootBoundsForAnimation(rootWidthTransition, rootHeightTransition)

      state.rootTransition = getRootTransition(allTransitions)
      state.transitionsHasBeenCollected = true
    }

    private fun collectMountTimeTransitions(
        previousInput: TransitionsExtensionInput?,
        input: TransitionsExtensionInput,
        outList: MutableList<Transition>,
    ) {
      val transitions = input.getMountTimeTransitions(previousInput)
      if (transitions != null) {
        for (transition in transitions) {
          if (transition != null) {
            TransitionUtils.addTransitions(transition, outList)
          }
        }
      }
    }

    private fun prepareTransitionManager(
        extensionState: ExtensionState<TransitionsExtensionState>,
        debugTag: String?,
    ) {
      val state = extensionState.state
      if (state.transitionManager == null) {
        val input = checkNotNull(state.input)
        val tracer = checkNotNull(input.tracer)
        state.transitionManager =
            TransitionManager(
                AnimationCompleteListener(extensionState),
                debugTag,
                tracer,
            )
      }
    }

    private fun createNewTransitions(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput,
        rootTransition: Transition?,
        debugTag: String?,
    ) {
      val state = extensionState.state
      prepareTransitionManager(extensionState, debugTag)

      val transitionManager = checkNotNull(state.transitionManager)
      val lastInput = state.lastTransitionsExtensionInput
      val lastTransitions = lastInput?.transitionIdMapping
      transitionManager.setupTransitions(
          lastTransitions,
          input.transitionIdMapping,
          checkNotNull(rootTransition),
      )

      val nextTransitionIds = input.transitionIdMapping
      if (nextTransitionIds != null) {
        for (transitionId in nextTransitionIds.keys) {
          if (transitionManager.isAnimating(transitionId)) {
            state.animatingTransitionIds.add(transitionId)
          }
        }
      }
    }

    /** Determine whether to apply disappear animation to the given [MountItem] */
    private fun isItemDisappearing(
        state: TransitionsExtensionState,
        input: TransitionsExtensionInput?,
        index: Int,
    ): Boolean {
      if (!shouldAnimateTransitions(state, input) || !hasTransitionsToAnimate(state)) {
        return false
      }

      val transitionManager = state.transitionManager ?: return false
      val lastInput = state.lastTransitionsExtensionInput ?: return false

      val animatableItem = checkNotNull(
          lastInput.getAnimatableItem(
              checkNotNull(lastInput.getMountableOutputAt(index)).renderUnit.id,
          ),
      )

      val transitionId = animatableItem.getTransitionId() ?: return false

      return transitionManager.isDisappearing(transitionId)
    }

    private fun isMountedAtIndex(index: Int, mountTarget: MountDelegateTarget): Boolean {
      return mountTarget.getMountItemAt(index) != null
    }

    private fun getParentIndex(index: Int, input: TransitionsExtensionInput): Int {
      val node = input.getMountableOutputAt(index)
      val parent = node?.parent ?: return -1

      return input.getPositionForId(parent.renderUnit.id)
    }

    private fun mountParentIndicesIfNeeded(
        index: Int,
        extensionState: ExtensionState<TransitionsExtensionState>,
    ) {
      val input = extensionState.state.lastTransitionsExtensionInput

      // invalid input
      if (input == null || index < 0) {
        return
      }

      // extract the parent index form the given index.
      val parentIndex: Int = getParentIndex(index, input)

      // no parent, stop here.
      if (parentIndex < 0) {
        return
      }

      // if the parent index is mounted, stop here.
      if (isMountedAtIndex(parentIndex, getMountTarget(extensionState))) {
        return
      }

      // check the parents recursively before mounting this item
      mountParentIndicesIfNeeded(parentIndex, extensionState)

      // mount this item
      mountItemAtIndexIfNeeded(parentIndex, extensionState)
    }

    private fun mountItemAtIndexIfNeeded(
        index: Int,
        extensionState: ExtensionState<TransitionsExtensionState>,
    ) {
      if (isMountedAtIndex(index, getMountTarget(extensionState))) {
        return
      }

      // We need to release any mount reference to this because we need to force mount here.
      val lastInput = checkNotNull(extensionState.state.lastTransitionsExtensionInput)
      val id = checkNotNull(lastInput.getMountableOutputAt(index)).renderUnit.id
      if (extensionState.ownsReference(id)) {
        extensionState.releaseMountReference(id, false)
      }
      extensionState.acquireMountReference(id, true)
      // Here we have to release the ref count without mounting.
      extensionState.releaseMountReference(id, false)
    }

    /**
     * This is where we go through the new layout state and compare it to the previous one. If we
     * find we do a couple of things:
     * - Loop trough the disappearing tree making sure it is mounted (we mounted if it's not).
     * - Add all the items to a set to be able to hook the unmount delegate.
     * - Move the disappearing mount item to the root host.
     * - Finally map the disappearing mount item to the transition id
     */
    private fun extractDisappearingItems(
        extensionState: ExtensionState<TransitionsExtensionState>,
        newTransitionsExtensionInput: TransitionsExtensionInput?,
    ) {
      val mountTarget = getMountTarget(extensionState)
      val mountItemCount = mountTarget.getRenderUnitCount()
      val state = extensionState.state
      val lastInput = state.lastTransitionsExtensionInput
      if (lastInput == null || mountItemCount == 0) {
        return
      }

      var index = 1
      while (index < mountItemCount) {
        // If this is not a disappearing item, increment the index and continue to next item
        if (!isItemDisappearing(state, newTransitionsExtensionInput, index)) {
          index++
          continue
        }

        // if any of this disappearing item's parents are unmounted, mount them now
        mountParentIndicesIfNeeded(index, extensionState)

        val lastDescendantIndex: Int = findLastDescendantIndex(lastInput, index)

        // iterate over disappearing item and all its descendants, ensure they are mounted, and
        // add them to the locked disappearing mount items map.
        for (i in index..lastDescendantIndex) {
          mountItemAtIndexIfNeeded(i, extensionState)

          val node = checkNotNull(mountTarget.getMountItemAt(i)).renderTreeNode
          val renderUnit = node.renderUnit

          state.lockedDisappearingMountItems[renderUnit] =
              lastInput.getAnimatableItem(node.renderUnit.id)
        }

        // Reference to the root of the disappearing subtree
        val disappearingItem = mountTarget.getMountItemAt(index)

        checkNotNull(disappearingItem) {
          "The root of the disappearing subtree should not be null, acquireMountReference on this index should be called before this. Index: $index"
        }

        // Ensure index is at end of host for both the incoming and outgoing frame.
        // This means either the child count of the root in the outgoing frame,
        // or the child count of the root in the incoming frame. Whichever is greater.
        val resolvedNewInput = checkNotNull(newTransitionsExtensionInput)
        val remountIndex = max(
            getMountableOutputCountOfRoot(lastInput),
            getMountableOutputCountOfRoot(resolvedNewInput),
        )

        // Moving item to the root if needed.
        remountHostToRootIfNeeded(
            extensionState,
            remountIndex,
            disappearingItem,
            resolvedNewInput.tracer,
        )

        mapDisappearingItemWithTransitionId(state, disappearingItem)

        val id = disappearingItem.renderTreeNode.renderUnit.id
        mountTarget.notifyUnmount(id)

        // set the index to the last descendant index + 1
        index = lastDescendantIndex + 1
      }
    }

    private fun getMountableOutputCountOfRoot(input: TransitionsExtensionInput): Int {
      val root = input.getMountableOutputAt(0) ?: return 0
      return root.childrenCount
    }

    private fun unmountDisappearingItem(
        extensionState: ExtensionState<TransitionsExtensionState>,
        mountItem: MountItem,
        isRoot: Boolean,
    ) {
      val state = extensionState.state
      val content = mountItem.content

      if (mountItem.renderTreeNode.renderUnit is TransitionRenderUnit) {
        if ((content is DisappearingHost) && content !is AnimatedRootHost) {
          val contentHost = content as Host
          // Snapshot before unmounting because unmountDisappearingItem mutates the host's storage.
          val descendants = contentHost.snapshotMountItems()
          // Back-to-front: the snapshot lists displaced items last, and they have to be unmounted
          // before the active items that took their slots. See Host.snapshotMountItems.
          for (j in descendants.indices.reversed()) {
            try {
              unmountDisappearingItem(extensionState, descendants[j], false)
            } catch (e: RuntimeException) {
              val message =
                  "content: <cls>${mountItem.content.javaClass}</cls>\nrenderunit: <cls>${mountItem.renderTreeNode.renderUnit.javaClass}</cls>"
              throw RuntimeException(message, e)
            }
          }

          check(contentHost.isMountItemStorageEmpty) {
            "Recursively unmounting items from a Host, left some items behind, this should never happen."
          }
        }

        val host = if (isRoot) state.currentlyDisappearingItems[mountItem] else mountItem.host
        checkNotNull(host) { "Disappearing mountItem has no host, can not be unmounted." }
        if (isRoot) {
          val wasRemoved = (host as DisappearingHost).finaliseDisappearingItem(mountItem)
          if (!wasRemoved) {
            val item =
                extensionState.state.lockedDisappearingMountItems[
                        mountItem.renderTreeNode.renderUnit]
            val transitionId = checkNotNull(item).getTransitionId()
            throw RuntimeException(
                "Tried to remove non-existent disappearing item, transitionId: $transitionId",
            )
          }

          state.currentlyDisappearingItems.remove(mountItem)
        } else {
          host.unmount(mountItem)
        }

        getMountTarget(extensionState).unbindMountItem(mountItem)
        state.lockedDisappearingMountItems.remove(mountItem.renderTreeNode.renderUnit)
      }
    }

    private fun endUnmountDisappearingItem(
        extensionState: ExtensionState<TransitionsExtensionState>,
        group: OutputUnitsAffinityGroup<MountItem?>,
    ) {
      val mostSignificantUnit = checkNotNull(group.getMostSignificantUnit())
      val lockedItem = checkNotNull(
          extensionState.state.lockedDisappearingMountItems[
                  mostSignificantUnit.renderTreeNode.renderUnit],
      )
      maybeRemoveAnimatingMountContent(
          extensionState.state,
          lockedItem.getTransitionId(),
      )

      for (i in 0 until group.size()) {
        unmountDisappearingItem(extensionState, checkNotNull(group.getAt(i)), true)
      }
    }

    private fun mapDisappearingItemWithTransitionId(
        state: TransitionsExtensionState,
        item: MountItem,
    ) {
      val lastInput = checkNotNull(state.lastTransitionsExtensionInput)
      val animatableItem =
          checkNotNull(lastInput.getAnimatableItem(item.renderTreeNode.renderUnit.id))
      val transitionId = animatableItem.getTransitionId()
      var disappearingGroup = state.disappearingMountItems[transitionId]
      if (disappearingGroup == null) {
        disappearingGroup = OutputUnitsAffinityGroup<MountItem?>()
        state.disappearingMountItems[transitionId] = disappearingGroup
      }
      @OutputUnitType val type = animatableItem.getOutputType()
      if (disappearingGroup.get(type) != null) {
        report(
            LogLevel.ERROR,
            "OutputUnitsAffinityGroup:mapDissapearingItemsWithTransitionId",
            "Disappearing pair already exists for, component: ${lastInput.rootName}, transition_id: $transitionId, type: $type",
        )
        disappearingGroup.replace(type, item)
      } else {
        disappearingGroup.add(type, item)
      }
    }

    private fun remountHostToRootIfNeeded(
        extensionState: ExtensionState<TransitionsExtensionState>,
        index: Int,
        mountItem: MountItem,
        tracer: Systracer?,
    ) {
      val rootHost = getMountTarget(extensionState).getRootHost()
      val originalHost = mountItem.host
      checkNotNull(originalHost) { "Disappearing item host should never be null. Index: $index" }

      if (rootHost === originalHost) {
        // Already mounted to the root
        return
      }

      val content = mountItem.content
      checkNotNull(content) { "Disappearing item content should never be null. Index: $index" }

      // Before unmounting item get its position inside the root
      var left = 0
      var top = 0
      val right: Int
      val bottom: Int
      // Get left/top position of the item's host first
      var host: Host? = originalHost
      while (host !== rootHost) {
        // We use getX/Y here to also account for translation set on the parent.
        val currentHost = checkNotNull(host)
        left = (left + currentHost.x).toInt()
        top = (top + currentHost.y).toInt()
        host = currentHost.parent as Host
      }

      if (content is View) {
        val view = content
        left += view.left
        top += view.top
        right = left + view.width
        bottom = top + view.height
      } else {
        val bounds = (content as Drawable).bounds
        left += bounds.left
        right = left + bounds.width()
        top += bounds.top
        bottom = top + bounds.height()
      }

      // Unmount from the current host
      if (RenderCoreConfig.shouldEnableIMFix) {
        ViewCompat.dispatchStartTemporaryDetach(mountItem.content as View)
      }
      originalHost.unmount(mountItem)

      // Apply new bounds to the content as it will be mounted in the root now
      applyBoundsToMountContent(left, top, right, bottom, null, content, false, tracer)

      // Mount to the root
      rootHost.mount(index, mountItem)
      if (RenderCoreConfig.shouldEnableIMFix) {
        ViewCompat.dispatchFinishTemporaryDetach(mountItem.content as View)
      }
    }

    private fun maybeUpdateAnimatingMountContent(
        extensionState: ExtensionState<TransitionsExtensionState>,
    ) {
      val state = extensionState.state
      val transitionManager = state.transitionManager ?: return

      val input = checkNotNull(state.input)
      val tracer = checkNotNull(input.tracer)
      tracer.beginSection("updateAnimatingMountContent")

      // Group mount content (represents current LayoutStates only) into groups and pass it to the
      // TransitionManager
      val animatingContent = LinkedHashMap<TransitionId?, OutputUnitsAffinityGroup<Any>>(
          state.animatingTransitionIds.size,
      )

      for (i in 0 until getMountTarget(extensionState).getRenderUnitCount()) {
        val mountItem = getMountTarget(extensionState).getMountItemAt(i)
        if (mountItem == null) {
          continue
        }
        val animatableItem =
            checkNotNull(input.getAnimatableItem(mountItem.renderTreeNode.renderUnit.id))

        if (animatableItem.getTransitionId() == null) {
          continue
        }
        @OutputUnitType val type = animatableItem.getOutputType()
        var group = animatingContent[animatableItem.getTransitionId()]
        if (group == null) {
          group = OutputUnitsAffinityGroup<Any>()
          animatingContent[animatableItem.getTransitionId()] = group
        }
        group.replace(type, mountItem.content)
      }
      for (content in animatingContent.entries) {
        transitionManager.setMountContent(checkNotNull(content.key), content.value)
      }

      // Retrieve mount content from disappearing mount items and pass it to the TransitionManager
      for (entry in state.disappearingMountItems.entries) {
        val mountItemsGroup = entry.value
        val mountContentGroup = OutputUnitsAffinityGroup<Any>()
        for (j in 0 until mountItemsGroup.size()) {
          @OutputUnitType val type = mountItemsGroup.typeAt(j)
          val mountItem = checkNotNull(mountItemsGroup.getAt(j))
          mountContentGroup.add(type, mountItem.content)
        }
        transitionManager.setMountContent(checkNotNull(entry.key), mountContentGroup)
      }

      tracer.endSection()
    }

    private fun maybeRemoveAnimatingMountContent(
        state: TransitionsExtensionState,
        transitionId: TransitionId?,
    ) {
      val transitionManager = state.transitionManager
      if (transitionManager == null || transitionId == null) {
        return
      }

      transitionManager.setMountContent(transitionId, null)
    }

    /**
     * Update the animation locked count for all children and each parent of the animating item.
     * Mount items that have a lock count > 0 will not be unmounted during incremental mount.
     */
    private fun updateAnimationLockCount(
        extensionState: ExtensionState<TransitionsExtensionState>,
        input: TransitionsExtensionInput?,
        index: Int,
        increment: Boolean,
    ) {
      val resolvedInput = checkNotNull(input)
      // Update children
      val lastDescendantIndex: Int = findLastDescendantIndex(resolvedInput, index)
      for (i in index..lastDescendantIndex) {
        val renderTreeNode = checkNotNull(resolvedInput.getMountableOutputAt(i))
        val id = renderTreeNode.renderUnit.id
        if (increment) {
          if (!extensionState.ownsReference(id)) {
            extensionState.acquireMountReference(id, false)
          }
        } else {
          if (extensionState.ownsReference(id)) {
            extensionState.releaseMountReference(id, false)
          }
        }
      }

      // Update parents
      var parentRenderTreeNode = checkNotNull(resolvedInput.getMountableOutputAt(index)).parent
      while (parentRenderTreeNode != null && parentRenderTreeNode.parent != null) {
        val id = parentRenderTreeNode.renderUnit.id
        if (increment) {
          if (!extensionState.ownsReference(id)) {
            extensionState.acquireMountReference(id, false)
          }
        } else {
          if (extensionState.ownsReference(id)) {
            extensionState.releaseMountReference(id, false)
          }
        }
        parentRenderTreeNode = parentRenderTreeNode.parent
      }
    }

    private fun findLastDescendantIndex(input: TransitionsExtensionInput, index: Int): Int {
      val rootRenderTreeNode = input.getMountableOutputAt(index)
      for (i in index + 1 until input.getMountableOutputCount()) {
        var parentRenderTreeNode = checkNotNull(input.getMountableOutputAt(i)).parent
        // Walk up the parents looking for the host's id: if we find it, it's a descendant. If we
        // reach the root, then it's not a descendant and we can stop.
        while (parentRenderTreeNode != rootRenderTreeNode) {
          if (parentRenderTreeNode == null || parentRenderTreeNode.parent == null) {
            return i - 1
          }

          parentRenderTreeNode = parentRenderTreeNode.parent
        }
      }

      return input.getMountableOutputCount() - 1
    }

    private fun recursivelyNotifyVisibleBoundsChanged(view: View?) {
      assertMainThread()

      val viewStack = ArrayDeque<View?>()
      viewStack.push(view)

      while (!viewStack.isEmpty()) {
        val currView = viewStack.pop()
        if (currView is AnimatedRootHost) {
          currView.notifyVisibleBoundsChanged(
              Rect(0, 0, currView.width, currView.height),
              false,
              VisibilityChangeReason.MOUNT,
          )
        } else if (currView is ViewGroup) {
          val parent = currView
          for (i in parent.childCount - 1 downTo 0) {
            viewStack.push(parent.getChildAt(i))
          }
        }
      }
    }
  }
}
