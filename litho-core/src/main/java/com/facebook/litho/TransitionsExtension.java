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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegate.MountDelegateInput;
import com.facebook.rendercore.MountDelegateExtension;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.UnmountDelegateExtension;
import com.facebook.rendercore.utils.BoundsUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Extension for performing transitions. */
public class TransitionsExtension extends MountDelegateExtension
    implements HostListenerExtension<TransitionsExtension.TransitionsExtensionInput>,
        TransitionManager.OnAnimationCompleteListener<EventHandler<TransitionEndEvent>>,
        UnmountDelegateExtension {

  private final Map<TransitionId, OutputUnitsAffinityGroup<MountItem>> mDisappearingMountItems =
      new LinkedHashMap<>();
  private final Set<MountItem> mLockedDisappearingMountitems = new HashSet<>();
  private final Host mLithoView;
  private TransitionsExtensionInput mInput;
  private int mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
  private TransitionManager mTransitionManager;
  private final HashSet<TransitionId> mAnimatingTransitionIds = new HashSet<>();

  private boolean mTransitionsHasBeenCollected = false;
  private @Nullable Transition mRootTransition;
  private @Nullable TransitionsExtensionInput mLastTransitionsExtensionInput;
  private AttachDetachBinder mAttachDetachBinder = new AttachDetachBinder();
  private MountUnmountBinder mMountUnmountBinder = new MountUnmountBinder();

  @Override
  public boolean shouldDelegateUnmount(MountItem mountItem) {
    return mLockedDisappearingMountitems.contains(mountItem);
  }

  @Override
  public void unmount(int index, MountItem mountItem, com.facebook.rendercore.Host host) {
    ((ComponentHost) host).startUnmountDisappearingItem(mountItem);
  }

  public interface TransitionsExtensionInput extends MountDelegateInput {
    int getMountableOutputCount();

    RenderTreeNode getMountableOutputAt(int index);

    boolean needsToRerunTransitions();

    void setNeedsToRerunTransitions(boolean needsToRerunTransitions);

    int getComponentTreeId();

    Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> getTransitionIdMapping();

    @Nullable
    OutputUnitsAffinityGroup<LayoutOutput> getLayoutOutputsForTransitionId(
        TransitionId transitionId);

    @Nullable
    List<Component> getComponentsNeedingPreviousRenderData();

    @Nullable
    String getRootComponentName();

    @Nullable
    List<Transition> getTransitions();

    @Nullable
    TransitionId getRootTransitionId();
  }

  public TransitionsExtension(Host lithoView) {
    mLithoView = lithoView;
  }

  @Override
  public void registerToDelegate(MountDelegate mountDelegate) {
    super.registerToDelegate(mountDelegate);
    getMountTarget().setUnmountDelegateExtension(this);
  }

  @Override
  public void beforeMount(TransitionsExtensionInput input, Rect localVisibleRect) {
    resetAcquiredReferences();
    mInput = input;

    if (input.getComponentTreeId() != mLastMountedComponentTreeId) {
      mLastTransitionsExtensionInput = null;
    }

    updateTransitions(input, ((LithoView) mLithoView).getComponentTree());
    extractDisappearingItems(input);

    final int componentTreeId = input.getComponentTreeId();
    mLastMountedComponentTreeId = componentTreeId;
  }

  @Override
  public void afterMount() {
    maybeUpdateAnimatingMountContent();

    if (shouldAnimateTransitions(mInput) && hasTransitionsToAnimate()) {
      mTransitionManager.runTransitions();
    }
    mInput.setNeedsToRerunTransitions(false);
    mLastTransitionsExtensionInput = mInput;
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
   * Creates and updates transitions for a new TransitionsExtensionInput. The steps are as follows:
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
  private void updateTransitions(TransitionsExtensionInput input, ComponentTree componentTree) {
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
      final int componentTreeId = componentTree.mId;
      if (mLastMountedComponentTreeId != componentTreeId) {
        resetAnimationState();
        if (!mInput.needsToRerunTransitions()) {
          // Don't re-trigger appear animations were scrolled back onto the screen
          return;
        }
      }

      if (!mDisappearingMountItems.isEmpty()) {
        updateDisappearingMountItems(input);
      }

      if (shouldAnimateTransitions(input)) {
        collectAllTransitions(input, componentTree);
        if (hasTransitionsToAnimate()) {
          createNewTransitions(input, mRootTransition);
        }
      }

      if (mTransitionManager != null) {
        mTransitionManager.finishUndeclaredTransitions();
      }

      if (!mAnimatingTransitionIds.isEmpty()) {
        regenerateAnimationLockedIndices(input);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void updateDisappearingMountItems(TransitionsExtensionInput input) {
    final Map<TransitionId, ?> nextMountedTransitionIds = input.getTransitionIdMapping();
    for (TransitionId transitionId : nextMountedTransitionIds.keySet()) {
      final OutputUnitsAffinityGroup<MountItem> disappearingItem =
          mDisappearingMountItems.remove(transitionId);
      if (disappearingItem != null) {
        endUnmountDisappearingItem(disappearingItem);
      }
    }
  }

  private void resetAnimationState() {
    if (mTransitionManager == null) {
      return;
    }

    for (OutputUnitsAffinityGroup<MountItem> group : mDisappearingMountItems.values()) {
      endUnmountDisappearingItem(group);
    }
    mDisappearingMountItems.clear();
    mLockedDisappearingMountitems.clear();
    mAnimatingTransitionIds.clear();
    mTransitionManager.reset();
  }

  private void regenerateAnimationLockedIndices(TransitionsExtensionInput input) {
    final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transitionMapping =
        input.getTransitionIdMapping();
    if (transitionMapping != null) {
      for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transition :
          transitionMapping.entrySet()) {
        if (!mAnimatingTransitionIds.contains(transition.getKey())) {
          continue;
        }

        final OutputUnitsAffinityGroup<LayoutOutput> group = transition.getValue();
        for (int j = 0, sz = group.size(); j < sz; j++) {
          final LayoutOutput layoutOutput = group.getAt(j);
          final int position = input.getLayoutOutputPositionForId(layoutOutput.getId());
          updateAnimationLockCount(input, position, true, true);
        }
      }
    }
  }

  /** @return whether we should animate transitions. */
  private boolean shouldAnimateTransitions(TransitionsExtensionInput input) {
    return (mLastMountedComponentTreeId == input.getComponentTreeId()
        || mInput.needsToRerunTransitions());
  }

  /** @return whether we have any transitions to animate for the current mount */
  private boolean hasTransitionsToAnimate() {
    return mRootTransition != null;
  }

  /**
   * Collect transitions from layout time, mount time and from state updates.
   *
   * @param input provides transitions information for the current mount.
   */
  void collectAllTransitions(TransitionsExtensionInput input, ComponentTree componentTree) {
    assertMainThread();
    if (mTransitionsHasBeenCollected) {
      return;
    }

    final ArrayList<Transition> allTransitions = new ArrayList<>();

    if (input.getTransitions() != null) {
      allTransitions.addAll(input.getTransitions());
    }
    componentTree.applyPreviousRenderData(input.getComponentsNeedingPreviousRenderData());
    collectMountTimeTransitions(input, allTransitions);
    componentTree.consumeStateUpdateTransitions(allTransitions, input.getRootComponentName());

    Transition.RootBoundsTransition rootWidthTransition = new Transition.RootBoundsTransition();
    Transition.RootBoundsTransition rootHeightTransition = new Transition.RootBoundsTransition();

    final TransitionId rootTransitionId = input.getRootTransitionId();

    if (rootTransitionId != null) {
      for (int i = 0, size = allTransitions.size(); i < size; i++) {
        final Transition transition = allTransitions.get(i);
        if (transition == null) {
          throw new IllegalStateException(
              "NULL_TRANSITION when collecting root bounds anim. Root: "
                  + input.getRootComponentName()
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
      TransitionsExtensionInput input, List<Transition> outList) {
    final List<Component> componentsNeedingPreviousRenderData =
        input.getComponentsNeedingPreviousRenderData();

    if (componentsNeedingPreviousRenderData == null) {
      return;
    }

    for (int i = 0, size = componentsNeedingPreviousRenderData.size(); i < size; i++) {
      final Component component = componentsNeedingPreviousRenderData.get(i);
      final Transition transition = component.createTransition(component.getScopedContext());
      if (transition != null) {
        TransitionUtils.addTransitions(transition, outList, input.getRootComponentName());
      }
    }
  }

  private void prepareTransitionManager() {
    if (mTransitionManager == null) {
      mTransitionManager = new TransitionManager(this, null);
    }
  }

  private void createNewTransitions(TransitionsExtensionInput input, Transition rootTransition) {
    prepareTransitionManager();

    Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> lastTransitions =
        mLastTransitionsExtensionInput == null
            ? null
            : mLastTransitionsExtensionInput.getTransitionIdMapping();
    mTransitionManager.setupTransitions(
        lastTransitions, input.getTransitionIdMapping(), rootTransition);

    final Map<TransitionId, ?> nextTransitionIds = input.getTransitionIdMapping();
    for (TransitionId transitionId : nextTransitionIds.keySet()) {
      if (mTransitionManager.isAnimating(transitionId)) {
        mAnimatingTransitionIds.add(transitionId);
      }
    }
  }

  @Override
  public void onAnimationComplete(TransitionId transitionId) {
    final OutputUnitsAffinityGroup<MountItem> disappearingGroup =
        mDisappearingMountItems.remove(transitionId);
    if (disappearingGroup != null) {
      endUnmountDisappearingItem(disappearingGroup);
    } else {
      if (!mAnimatingTransitionIds.remove(transitionId)) {
        if (AnimationsDebug.ENABLED) {
          Log.e(
              AnimationsDebug.TAG,
              "Ending animation for id " + transitionId + " but it wasn't recorded as animating!");
        }
      }

      final OutputUnitsAffinityGroup<LayoutOutput> layoutOutputGroup =
          mLastTransitionsExtensionInput.getLayoutOutputsForTransitionId(transitionId);
      if (layoutOutputGroup == null) {
        // This can happen if the component was unmounted without animation or the transitionId
        // was removed from the component.
        return;
      }

      for (int i = 0, size = layoutOutputGroup.size(); i < size; i++) {
        final LayoutOutput layoutOutput = layoutOutputGroup.getAt(i);
        final int position = layoutOutput.getIndex();
        updateAnimationLockCount(mLastTransitionsExtensionInput, position, false, false);
      }
    }
  }

  /** Determine whether to apply disappear animation to the given {@link MountItem} */
  private boolean isItemDisappearing(TransitionsExtensionInput input, int index) {
    if (!shouldAnimateTransitions(input) || !hasTransitionsToAnimate()) {
      return false;
    }

    if (mTransitionManager == null || mLastTransitionsExtensionInput == null) {
      return false;
    }

    final LayoutOutput layoutOutput =
        getLayoutOutput(mLastTransitionsExtensionInput.getMountableOutputAt(index));
    final TransitionId transitionId = layoutOutput.getTransitionId();
    if (transitionId == null) {
      return false;
    }

    return mTransitionManager.isDisappearing(transitionId);
  }

  /**
   * This is where we go through the new layout state and compare it to the previous one. If we find
   * we do a couple of things:
   *
   * <p>- Loop trough the disappearing tree making sure it is mounted (we mounted if it's not).
   *
   * <p>- Add all the items to a set to be able to hook the unmount delegate.
   *
   * <p>- Move the disappearing mount item to the root host.
   *
   * <p>- Finally map the disappearing mount item to the transition id
   */
  private void extractDisappearingItems(TransitionsExtensionInput newTransitionsExtensionInput) {
    int mountItemCount = getMountTarget().getMountItemCount();
    if (mLastTransitionsExtensionInput == null || mountItemCount == 0) {
      return;
    }

    for (int i = 1; i < mountItemCount; i++) {
      if (isItemDisappearing(newTransitionsExtensionInput, i)) {
        final int lastDescendantIndex = findLastDescendantIndex(mLastTransitionsExtensionInput, i);
        // Go though disappearing subtree. Acquire a reference for everything that is not mounted.
        for (int j = i; j <= lastDescendantIndex; j++) {
          MountItem mountedItem = getMountTarget().getMountItemAt(j);
          if (mountedItem == null) {
            acquireMountReference(
                mLastTransitionsExtensionInput.getMountableOutputAt(j), j, mInput, true);
            mountedItem = getMountTarget().getMountItemAt(j);
          }
          mLockedDisappearingMountitems.add(mountedItem);
        }

        // Reference to the root of the disappearing subtree
        final MountItem disappearingItem = getMountTarget().getMountItemAt(i);

        if (disappearingItem == null) {
          throw new IllegalStateException(
              "The root of the disappearing subtree should not be null,"
                  + " acquireMountReference on this index should be called before this. Index: "
                  + i);
        }

        // Moving item to the root if needed.
        remountHostToRootIfNeeded(i, disappearingItem);

        mapDisappearingItemWithTransitionId(disappearingItem);
        i = lastDescendantIndex;
      }
    }
  }

  private void unmountDisappearingItem(MountItem mountItem) {
    mLockedDisappearingMountitems.remove(mountItem);
    final Object content = mountItem.getContent();
    if ((content instanceof ComponentHost) && !(content instanceof LithoView)) {
      final com.facebook.rendercore.Host contentHost = (com.facebook.rendercore.Host) content;
      // Unmount descendant items in reverse order.
      for (int j = contentHost.getMountItemCount() - 1; j >= 0; j--) {
        unmountDisappearingItem(contentHost.getMountItemAt(j));
      }

      if (contentHost.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a Host, left"
                + " some items behind, this should never happen.");
      }
    }

    final ComponentHost host = (ComponentHost) mountItem.getHost();
    if (host == null) {
      throw new IllegalStateException("Disappearing mountItem has no host, can not be unmounted.");
    }
    host.unmountDisappearingItem(mountItem);

    getMountTarget().unbindMountItem(mountItem);
  }

  private void endUnmountDisappearingItem(OutputUnitsAffinityGroup<MountItem> group) {
    maybeRemoveAnimatingMountContent(
        getLayoutOutput(group.getMostSignificantUnit()).getTransitionId());

    for (int i = 0, size = group.size(); i < size; i++) {
      unmountDisappearingItem(group.getAt(i));
    }
  }

  private void mapDisappearingItemWithTransitionId(MountItem item) {
    final TransitionId transitionId = getLayoutOutput(item).getTransitionId();
    OutputUnitsAffinityGroup<MountItem> disappearingGroup =
        mDisappearingMountItems.get(transitionId);
    if (disappearingGroup == null) {
      disappearingGroup = new OutputUnitsAffinityGroup<>();
      mDisappearingMountItems.put(transitionId, disappearingGroup);
    }
    final @OutputUnitType int type =
        LayoutStateOutputIdCalculator.getTypeFromId(getLayoutOutput(item).getId());
    disappearingGroup.add(type, item);
  }

  private static void remountHostToRootIfNeeded(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    final com.facebook.rendercore.Host host = mountItem.getHost();

    if (host == null) {
      throw new IllegalStateException(
          "Disappearing item host should never be null. Index: " + index);
    }
    if (content == null) {
      throw new IllegalStateException(
          "Disappearing item content should never be null. Index: " + index);
    }

    if (!(host.getParent() instanceof com.facebook.rendercore.Host)) {
      // Already mounted to the root
      return;
    }

    // Before unmounting item get its position inside the root
    int left = 0;
    int top = 0;
    int right;
    int bottom;
    com.facebook.rendercore.Host itemHost = host;
    com.facebook.rendercore.Host rootHost = host;
    // Get left/top position of the item's host first
    while (itemHost != null) {
      left += itemHost.getLeft();
      top += itemHost.getTop();
      if (itemHost.getParent() instanceof com.facebook.rendercore.Host) {
        itemHost = (com.facebook.rendercore.Host) itemHost.getParent();
      } else {
        rootHost = itemHost;
        itemHost = null;
      }
    }

    if (content instanceof View) {
      final View view = (View) content;
      left += view.getLeft();
      top += view.getTop();
      right = left + view.getWidth();
      bottom = top + view.getHeight();
    } else {
      final Rect bounds = ((Drawable) content).getBounds();
      left += bounds.left;
      right = left + bounds.width();
      top += bounds.top;
      bottom = top + bounds.height();
    }

    // Unmount from the current host
    host.unmount(mountItem);

    // Apply new bounds to the content as it will be mounted in the root now
    BoundsUtils.applyBoundsToMountContent(new Rect(left, top, right, bottom), null, content, false);

    // Mount to the root
    rootHost.mount(index, mountItem);

    // Set new host to the MountItem
    mountItem.setHost(rootHost);
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

    for (int i = 0, size = getMountTarget().getMountItemCount(); i < size; i++) {
      final MountItem mountItem = getMountTarget().getMountItemAt(i);
      if (mountItem == null) {
        continue;
      }
      final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
      if (layoutOutput.getTransitionId() == null) {
        continue;
      }
      final @OutputUnitType int type =
          LayoutStateOutputIdCalculator.getTypeFromId(layoutOutput.getId());
      OutputUnitsAffinityGroup<Object> group = animatingContent.get(layoutOutput.getTransitionId());
      if (group == null) {
        group = new OutputUnitsAffinityGroup<>();
        animatingContent.put(layoutOutput.getTransitionId(), group);
      }
      group.replace(type, mountItem.getContent());
    }
    for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<Object>> content :
        animatingContent.entrySet()) {
      mTransitionManager.setMountContent(content.getKey(), content.getValue());
    }

    // Retrieve mount content from disappearing mount items and pass it to the TransitionManager
    for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<MountItem>> entry :
        mDisappearingMountItems.entrySet()) {
      final OutputUnitsAffinityGroup<MountItem> mountItemsGroup = entry.getValue();
      final OutputUnitsAffinityGroup<Object> mountContentGroup = new OutputUnitsAffinityGroup<>();
      for (int j = 0, sz = mountItemsGroup.size(); j < sz; j++) {
        final @OutputUnitType int type = mountItemsGroup.typeAt(j);
        final MountItem mountItem = mountItemsGroup.getAt(j);
        mountContentGroup.add(type, mountItem.getContent());
      }
      mTransitionManager.setMountContent(entry.getKey(), mountContentGroup);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private void maybeRemoveAnimatingMountContent(@Nullable TransitionId transitionId) {
    if (mTransitionManager == null || transitionId == null) {
      return;
    }

    mTransitionManager.setMountContent(transitionId, null);
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
      TransitionsExtensionInput input, int index, boolean increment, boolean isMounting) {
    // Update children
    final int lastDescendantIndex = findLastDescendantIndex(input, index);
    for (int i = index; i <= lastDescendantIndex; i++) {
      final RenderTreeNode renderTreeNode = input.getMountableOutputAt(i);
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
    long hostId = getLayoutOutput(input.getMountableOutputAt(index)).getHostMarker();

    while (hostId != ROOT_HOST_ID) {
      final int hostIndex = input.getLayoutOutputPositionForId(hostId);
      final RenderTreeNode renderTreeNode = input.getMountableOutputAt(hostIndex);
      if (increment) {
        if (!ownsReference(renderTreeNode)) {
          acquireMountReference(renderTreeNode, hostIndex, mInput, false);
        }
      } else {
        if (ownsReference(renderTreeNode)) {
          releaseMountReference(renderTreeNode, hostIndex, false);
        }
      }
      hostId = getLayoutOutput(input.getMountableOutputAt(hostIndex)).getHostMarker();
    }
  }

  private static int findLastDescendantIndex(TransitionsExtensionInput input, int index) {
    final long hostId = getLayoutOutput(input.getMountableOutputAt(index)).getId();

    for (int i = index + 1, size = input.getMountableOutputCount(); i < size; i++) {
      final LayoutOutput layoutOutput = getLayoutOutput(input.getMountableOutputAt(i));

      // Walk up the parents looking for the host's id: if we find it, it's a descendant. If we
      // reach the root, then it's not a descendant and we can stop.
      long currentHostId = layoutOutput.getHostMarker();
      while (currentHostId != hostId) {
        if (currentHostId == ROOT_HOST_ID) {
          return i - 1;
        }

        final int parentIndex = input.getLayoutOutputPositionForId(currentHostId);
        final LayoutOutput parent = getLayoutOutput(input.getMountableOutputAt(parentIndex));
        currentHostId = parent.getHostMarker();
      }
    }

    return input.getMountableOutputCount() - 1;
  }

  public void bind(
      Context context,
      com.facebook.rendercore.Host host,
      Object content,
      LithoRenderUnit lithoRenderUnit,
      @Nullable Object layoutData) {
    mAttachDetachBinder.bind(context, host, content, lithoRenderUnit, layoutData);
  }

  public RenderUnit.Binder getAttachDetachBinder() {
    return mAttachDetachBinder;
  }

  public RenderUnit.Binder getMountUnmountBinder() {
    return mMountUnmountBinder;
  }

  final class AttachDetachBinder implements RenderUnit.Binder<LithoRenderUnit, Object> {

    @Override
    public boolean shouldUpdate(
        LithoRenderUnit currentValue,
        LithoRenderUnit newValue,
        @Nullable Object currentLayoutData,
        @Nullable Object nextLayoutData) {
      return true;
    }

    @Override
    public void bind(
        Context context,
        com.facebook.rendercore.Host host,
        Object content,
        LithoRenderUnit lithoRenderUnit,
        @Nullable Object layoutData) {
      final LayoutOutput output = lithoRenderUnit.output;
      if (ownsReference(lithoRenderUnit.getId()) && output.getComponent().hasChildLithoViews()) {
        final View view = (View) content;
        MountUtils.ensureAllLithoViewChildrenAreMounted(view);
      }
    }

    @Override
    public void unbind(
        Context context,
        com.facebook.rendercore.Host host,
        Object o,
        LithoRenderUnit lithoRenderUnit,
        @Nullable Object layoutData) {}
  }

  final class MountUnmountBinder implements RenderUnit.Binder<LithoRenderUnit, Object> {

    @Override
    public boolean shouldUpdate(
        LithoRenderUnit currentValue,
        LithoRenderUnit newValue,
        @Nullable Object currentLayoutData,
        @Nullable Object nextLayoutData) {
      return true;
    }

    @Override
    public void bind(
        Context context,
        com.facebook.rendercore.Host host,
        Object content,
        LithoRenderUnit lithoRenderUnit,
        @Nullable Object layoutData) {}

    @Override
    public void unbind(
        Context context,
        com.facebook.rendercore.Host host,
        Object o,
        LithoRenderUnit lithoRenderUnit,
        @Nullable Object layoutData) {
      final LayoutOutput output = lithoRenderUnit.output;
      if (output.getTransitionId() != null) {
        final @OutputUnitType int type =
            LayoutStateOutputIdCalculator.getTypeFromId(output.getId());
        maybeRemoveAnimatingMountContent(output.getTransitionId(), type);
      }
    }
  }
}
