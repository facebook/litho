/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.rendercore.MountDelegate.MountDelegateInput;
import com.facebook.rendercore.MountDelegateExtension;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extension for performing transitions. */
public class TransitionsExtension extends MountDelegateExtension
    implements HostListenerExtension<TransitionsExtension.TransitionsExtensionInput>,
        TransitionManager.OnAnimationCompleteListener<EventHandler<TransitionEndEvent>> {

  private final Host mLithoView;
  private TransitionsExtensionInput mInput;
  private int mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
  private TransitionManager mTransitionManager;
  private final HashSet<TransitionId> mAnimatingTransitionIds = new HashSet<>();

  private boolean mTransitionsHasBeenCollected = false;
  private @Nullable Transition mRootTransition;
  private LayoutState mLastMountedLayoutState;

  public interface TransitionsExtensionInput extends MountDelegateInput {
    boolean needsToRerunTransitions();

    void setNeedsToRerunTransitions(boolean needsToRerunTransitions);
  }

  public TransitionsExtension(Host lithoView) {
    mLithoView = lithoView;
  }

  @Override
  public void beforeMount(TransitionsExtensionInput input, Rect localVisibleRect) {
    resetAcquiredReferences();
    mInput = input;

    LayoutState layoutState = (LayoutState) input;

    if (layoutState.getComponentTreeId() != mLastMountedComponentTreeId) {
      // If we're mounting a new ComponentTree, don't keep around and use the previous LayoutState
      // since things like transition animations aren't relevant.
      mLastMountedLayoutState = null;
    }

    updateTransitions((LayoutState) mInput, ((LithoView) mLithoView).getComponentTree());

    final int componentTreeId = layoutState.getComponentTreeId();
    mLastMountedComponentTreeId = componentTreeId;
  }

  @Override
  public void afterMount() {
    LayoutState layoutState = (LayoutState) mInput;

    maybeUpdateAnimatingMountContent();

    if (shouldAnimateTransitions(layoutState) && hasTransitionsToAnimate()) {
      mTransitionManager.runTransitions();
    }
    mInput.setNeedsToRerunTransitions(false);
    mLastMountedLayoutState = layoutState;
    mTransitionsHasBeenCollected = false;
  }

  public void onVisibleBoundsChanged(Rect localVisibleRect) {}

  @Override
  public void onUnmount() {
    resetAcquiredReferences();
  }

  @Override
  public void onUnbind() {
    resetAcquiredReferences();
  }

  /**
   * Creates and updates transitions for a new LayoutState. The steps are as follows:
   *
   * <p>1. Disappearing items: Update disappearing mount items that are no longer disappearing (e.g.
   * because they came back). This means canceling the animation and cleaning up the corresponding
   * ComponentHost.
   *
   * <p>2. New transitions: Use the transition manager to create new animations.
   *
   * <p>3. Update locked indices: Based on running/new animations, there are some mount items we
   * want to make sure are not unmounted due to incremental mount and being outside of visibility
   * bounds.
   */
  private void updateTransitions(LayoutState layoutState, ComponentTree componentTree) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      String logTag = componentTree.getContext().getLogTag();
      if (logTag == null) {
        ComponentsSystrace.beginSection("MountState.updateTransitions");
      } else {
        ComponentsSystrace.beginSection("MountState.updateTransitions:" + logTag);
      }
    }

    try {
      // If this is a new component tree but isn't the first time it's been mounted, then we
      // shouldn't
      // do any transition animations for changed mount content as it's just being remounted on a
      // new LithoView.
      final int componentTreeId = layoutState.getComponentTreeId();
      if (mLastMountedComponentTreeId != componentTreeId) {
        resetAnimationState();
        if (!mInput.needsToRerunTransitions()) {
          // Don't re-trigger appear animations were scrolled back onto the screen
          return;
        }
      }

      // TODO (T64352474): Handle disappearing items
      //      if (!mDisappearingMountItems.isEmpty()) {
      //        updateDisappearingMountItems(layoutState);
      //      }

      if (shouldAnimateTransitions(layoutState)) {
        collectAllTransitions(layoutState, componentTree);
        if (hasTransitionsToAnimate()) {
          createNewTransitions(layoutState, mRootTransition);
        }
      }

      if (mTransitionManager != null) {
        mTransitionManager.finishUndeclaredTransitions();
      }

      if (!mAnimatingTransitionIds.isEmpty()) {
        regenerateAnimationLockedIndices(layoutState);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void resetAnimationState() {
    if (mTransitionManager == null) {
      return;
    }
    // TODO (T64352474): Handle disappearing items
    //    for (OutputUnitsAffinityGroup<MountItem> group : mDisappearingMountItems.values()) {
    //      endUnmountDisappearingItem(group);
    //    }
    //    mDisappearingMountItems.clear();
    mAnimatingTransitionIds.clear();
    mTransitionManager.reset();
  }

  private void regenerateAnimationLockedIndices(LayoutState newLayoutState) {
    final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transitionMapping =
        newLayoutState.getTransitionIdMapping();
    if (transitionMapping != null) {
      for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transition :
          transitionMapping.entrySet()) {
        if (!mAnimatingTransitionIds.contains(transition.getKey())) {
          continue;
        }

        final OutputUnitsAffinityGroup<LayoutOutput> group = transition.getValue();
        for (int j = 0, sz = group.size(); j < sz; j++) {
          final LayoutOutput layoutOutput = group.getAt(j);
          final int position = newLayoutState.getLayoutOutputPositionForId(layoutOutput.getId());
          updateAnimationLockCount(newLayoutState, position, true, true);
        }
      }
    }
  }

  /**
   * @return whether we should animate transitions if we have any when mounting the new LayoutState.
   */
  private boolean shouldAnimateTransitions(LayoutState newLayoutState) {
    return (mLastMountedComponentTreeId == newLayoutState.getComponentTreeId()
        || mInput.needsToRerunTransitions());
  }

  /**
   * @return whether we have any transitions to animate for the current mount of the given
   *     LayoutState
   */
  private boolean hasTransitionsToAnimate() {
    return mRootTransition != null;
  }

  /**
   * Collect transitions from layout time, mount time and from state updates.
   *
   * @param layoutState that is going to be mounted.
   */
  void collectAllTransitions(LayoutState layoutState, ComponentTree componentTree) {
    assertMainThread();
    if (mTransitionsHasBeenCollected) {
      return;
    }

    final ArrayList<Transition> allTransitions = new ArrayList<>();

    if (layoutState.getTransitions() != null) {
      allTransitions.addAll(layoutState.getTransitions());
    }
    componentTree.applyPreviousRenderData(layoutState);
    collectMountTimeTransitions(layoutState, allTransitions);
    componentTree.consumeStateUpdateTransitions(allTransitions, layoutState.mRootComponentName);

    Transition.RootBoundsTransition rootWidthTransition = new Transition.RootBoundsTransition();
    Transition.RootBoundsTransition rootHeightTransition = new Transition.RootBoundsTransition();

    final TransitionId rootTransitionId = layoutState.getRootTransitionId();

    if (rootTransitionId != null) {
      for (int i = 0, size = allTransitions.size(); i < size; i++) {
        final Transition transition = allTransitions.get(i);
        if (transition == null) {
          throw new IllegalStateException(
              "NULL_TRANSITION when collecting root bounds anim. Root: "
                  + layoutState.mRootComponentName
                  + ", root TransitionId: "
                  + rootTransitionId);
        }
        TransitionUtils.collectRootBoundsTransitions(
            rootTransitionId, transition, AnimatedProperties.WIDTH, rootWidthTransition);

        TransitionUtils.collectRootBoundsTransitions(
            rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition);
      }
    }

    rootWidthTransition = rootWidthTransition.hasTransition ? rootWidthTransition : null;
    rootHeightTransition = rootHeightTransition.hasTransition ? rootHeightTransition : null;

    componentTree.setRootWidthAnimation(rootWidthTransition);
    componentTree.setRootHeightAnimation(rootHeightTransition);

    mRootTransition = TransitionManager.getRootTransition(allTransitions);
    mTransitionsHasBeenCollected = true;
  }

  private static @Nullable void collectMountTimeTransitions(
      LayoutState layoutState, List<Transition> outList) {
    final List<Component> componentsNeedingPreviousRenderData =
        layoutState.getComponentsNeedingPreviousRenderData();

    if (componentsNeedingPreviousRenderData == null) {
      return;
    }

    for (int i = 0, size = componentsNeedingPreviousRenderData.size(); i < size; i++) {
      final Component component = componentsNeedingPreviousRenderData.get(i);
      final Transition transition = component.createTransition(component.getScopedContext());
      if (transition != null) {
        TransitionUtils.addTransitions(transition, outList, layoutState.mRootComponentName);
      }
    }
  }

  private void prepareTransitionManager() {
    if (mTransitionManager == null) {
      mTransitionManager = new TransitionManager(this, null);
    }
  }

  private void createNewTransitions(LayoutState newLayoutState, Transition rootTransition) {
    prepareTransitionManager();

    mTransitionManager.setupTransitions(mLastMountedLayoutState, newLayoutState, rootTransition);

    final Map<TransitionId, ?> nextTransitionIds = newLayoutState.getTransitionIdMapping();
    for (TransitionId transitionId : nextTransitionIds.keySet()) {
      if (mTransitionManager.isAnimating(transitionId)) {
        mAnimatingTransitionIds.add(transitionId);
      }
    }
  }

  @Override
  public void onAnimationComplete(TransitionId transitionId) {
    // TODO (T64352474): Handle disappearing items
    //    final OutputUnitsAffinityGroup<MountItem> disappearingGroup =
    //        mDisappearingMountItems.remove(transitionId);
    //    if (disappearingGroup != null) {
    //      endUnmountDisappearingItem(disappearingGroup);
    //    } else {
    if (!mAnimatingTransitionIds.remove(transitionId)) {
      if (AnimationsDebug.ENABLED) {
        Log.e(
            AnimationsDebug.TAG,
            "Ending animation for id " + transitionId + " but it wasn't recorded as animating!");
      }
    }

    final OutputUnitsAffinityGroup<LayoutOutput> layoutOutputGroup =
        mLastMountedLayoutState.getLayoutOutputsForTransitionId(transitionId);
    if (layoutOutputGroup == null) {
      // This can happen if the component was unmounted without animation or the transitionId
      // was removed from the component.
      return;
    }

    for (int i = 0, size = layoutOutputGroup.size(); i < size; i++) {
      final LayoutOutput layoutOutput = layoutOutputGroup.getAt(i);
      final int position = layoutOutput.getIndex();
      updateAnimationLockCount(mLastMountedLayoutState, position, false, false);
    }
    // TODO (T64352474): Handle disappearing items
    //    }
  }

  @Override
  public void onAnimationUnitComplete(
      PropertyHandle propertyHandle, EventHandler transitionEndHandler) {
    if (transitionEndHandler != null) {
      transitionEndHandler.dispatchEvent(
          new TransitionEndEvent(
              propertyHandle.getTransitionId().mReference, propertyHandle.getProperty()));
    }
  }

  private void maybeUpdateAnimatingMountContent() {
    if (mTransitionManager == null) {
      return;
    }

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("updateAnimatingMountContent");
    }

    // Group mount content (represents current LayoutStates only) into groups and pass it to the
    // TransitionManager
    final Map<TransitionId, OutputUnitsAffinityGroup<Object>> animatingContent =
        new LinkedHashMap<>(mAnimatingTransitionIds.size());

    // TODO (T64352474): Remove dependency on MountState
    LongSparseArray<MountItem> mIndexToItemMap =
        ((MountState) getMountTarget()).getIndexToItemMap();

    for (int i = 0, size = mIndexToItemMap.size(); i < size; i++) {
      final MountItem mountItem = mIndexToItemMap.valueAt(i);
      final LayoutOutput output = getLayoutOutput(mountItem);
      if (output.getTransitionId() == null) {
        continue;
      }
      final long layoutOutputId = mIndexToItemMap.keyAt(i);
      final @OutputUnitType int type = LayoutStateOutputIdCalculator.getTypeFromId(layoutOutputId);
      OutputUnitsAffinityGroup<Object> group = animatingContent.get(output.getTransitionId());
      if (group == null) {
        group = new OutputUnitsAffinityGroup<>();
        animatingContent.put(output.getTransitionId(), group);
      }
      group.replace(type, mountItem.getContent());
    }
    for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<Object>> content :
        animatingContent.entrySet()) {
      mTransitionManager.setMountContent(content.getKey(), content.getValue());
    }

    // TODO (T64352474): Handle disappearing items
    //    // Retrieve mount content from disappearing mount items and pass it to the
    // TransitionManager
    //    for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<MountItem>> entry :
    //        mDisappearingMountItems.entrySet()) {
    //      final OutputUnitsAffinityGroup<MountItem> mountItemsGroup = entry.getValue();
    //      final OutputUnitsAffinityGroup<Object> mountContentGroup = new
    // OutputUnitsAffinityGroup<>();
    //      for (int j = 0, sz = mountItemsGroup.size(); j < sz; j++) {
    //        final @OutputUnitType int type = mountItemsGroup.typeAt(j);
    //        final MountItem mountItem = mountItemsGroup.getAt(j);
    //        mountContentGroup.add(type, mountItem.getContent());
    //      }
    //      mTransitionManager.setMountContent(entry.getKey(), mountContentGroup);
    //    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  @Override
  public void onUmountItem(Object item, long layoutOutputId) {
    MountItem mountItem = (MountItem) item;
    final LayoutOutput output = getLayoutOutput(mountItem);
    if (output.getTransitionId() != null) {
      final @OutputUnitType int type = LayoutStateOutputIdCalculator.getTypeFromId(layoutOutputId);
      maybeRemoveAnimatingMountContent(output.getTransitionId(), type);
    }
  }

  private void maybeRemoveAnimatingMountContent(
      TransitionId transitionId, @OutputUnitType int type) {
    if (mTransitionManager == null || transitionId == null) {
      return;
    }

    mTransitionManager.removeMountContent(transitionId, type);
  }

  /**
   * Update the animation locked count for all children and each parent of the animating item. Mount
   * items that have a lock count > 0 will not be unmounted during incremental mount.
   */
  private void updateAnimationLockCount(
      LayoutState layoutState, int index, boolean increment, boolean isMounting) {
    // Update children
    final int lastDescendantIndex = findLastDescendantIndex(layoutState, index);
    for (int i = index; i <= lastDescendantIndex; i++) {
      final RenderTreeNode renderTreeNode = layoutState.getMountableOutputAt(i);
      if (increment) {
        if (!ownsReference(renderTreeNode)) {
          acquireMountReference(renderTreeNode, i, mInput, false);
        }
      } else {
        if (ownsReference(renderTreeNode)) {
          releaseMountReference(renderTreeNode, i, false);
        }
      }
    }

    // Update parents
    long hostId = getLayoutOutput(layoutState.getMountableOutputAt(index)).getHostMarker();
    while (hostId != ROOT_HOST_ID) {
      final int hostIndex = layoutState.getLayoutOutputPositionForId(hostId);
      final RenderTreeNode renderTreeNode = layoutState.getMountableOutputAt(hostIndex);
      if (increment) {
        if (!ownsReference(renderTreeNode)) {
          acquireMountReference(renderTreeNode, hostIndex, mInput, false);
        }
      } else {
        if (ownsReference(renderTreeNode)) {
          releaseMountReference(renderTreeNode, hostIndex, false);
        }
      }
      hostId = getLayoutOutput(layoutState.getMountableOutputAt(hostIndex)).getHostMarker();
    }
  }

  private static int findLastDescendantIndex(LayoutState layoutState, int index) {
    final LayoutOutput host = getLayoutOutput(layoutState.getMountableOutputAt(index));
    final long hostId = host.getId();

    for (int i = index + 1, size = layoutState.getMountableOutputCount(); i < size; i++) {
      final LayoutOutput layoutOutput = getLayoutOutput(layoutState.getMountableOutputAt(i));

      // Walk up the parents looking for the host's id: if we find it, it's a descendant. If we
      // reach the root, then it's not a descendant and we can stop.
      long curentHostId = layoutOutput.getHostMarker();
      while (curentHostId != hostId) {
        if (curentHostId == ROOT_HOST_ID) {
          return i - 1;
        }

        final int parentIndex = layoutState.getLayoutOutputPositionForId(curentHostId);
        final LayoutOutput parent = getLayoutOutput(layoutState.getMountableOutputAt(parentIndex));
        curentHostId = parent.getHostMarker();
      }
    }

    return layoutState.getMountableOutputCount() - 1;
  }
}
