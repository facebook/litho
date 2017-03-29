/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import com.facebook.R;
import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.reference.Reference;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.components.Component.isHostSpec;
import static com.facebook.components.Component.isMountViewSpec;
import static com.facebook.components.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.components.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.components.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.components.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.components.ComponentsLogger.EVENT_PREPARE_MOUNT;
import static com.facebook.components.ComponentsLogger.EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH;
import static com.facebook.components.ComponentsLogger.PARAM_IS_DIRTY;
import static com.facebook.components.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.components.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_MOVED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_NO_OP_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_UNCHANGED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_UPDATED_COUNT;
import static com.facebook.components.ThreadUtils.assertMainThread;

/**
 * Encapsulates the mounted state of a {@link Component}. Provides APIs to update state
 * by recycling existing UI elements e.g. {@link Drawable}s.
 *
 * @see #mount(LayoutState, Rect)
 * @see ComponentView
 * @see LayoutState
 */
class MountState {

  static final int ROOT_HOST_ID = 0;

  // Holds the current list of mounted items.
  // Should always be used within a draw lock.
  private final LongSparseArray<MountItem> mIndexToItemMap;

  // Holds a list with information about the components linked to the VisibilityOutputs that are
  // stored in LayoutState. An item is inserted in this map if its corresponding component is
  // visible. When the component exits the viewport, the item associated with it is removed from the
  // map.
  private final LongSparseArray<VisibilityItem> mVisibilityIdToItemMap;

  // Holds a list of MountItems that are currently mounted which can mount incrementally.
  private final LongSparseArray<MountItem> mCanMountIncrementallyMountItems;

  // A map from test key to a list of one or more `TestItem`s which is only allocated
  // and populated during test runs.
  private final Map<String, Deque<TestItem>> mTestItemMap;

  private long[] mLayoutOutputsIds;

  // True if we are receiving a new LayoutState and we need to completely
  // refresh the content of the HostComponent. Always set from the main thread.
  private boolean mIsDirty;

  // Holds the list of known component hosts during a mount pass.
  private final LongSparseArray<ComponentHost> mHostsByMarker = new LongSparseArray<>();

  private static final Rect sTempRect = new Rect();

  private final ComponentContext mContext;
  private final ComponentView mComponentView;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final PrepareMountStats mPrepareMountStats = new PrepareMountStats();
  private final MountStats mMountStats = new MountStats();
  private TransitionManager mTransitionManager;
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private int mLastMountedComponentTreeId;

  private final MountItem mRootHostMountItem;

  public MountState(ComponentView view) {
    mIndexToItemMap = new LongSparseArray<>();
    mVisibilityIdToItemMap = new LongSparseArray<>();
    mCanMountIncrementallyMountItems = new LongSparseArray<>();
    mContext = (ComponentContext) view.getContext();
    mComponentView = view;
    mIsDirty = true;

    mTestItemMap = ComponentsConfiguration.isEndToEndTestRun
        ? new HashMap<String, Deque<TestItem>>()
        : null;

    // The mount item representing the top-level ComponentView which
    // is always automatically mounted.
    mRootHostMountItem = ComponentsPools.acquireRootHostMountItem(
        HostComponent.create(),
        mComponentView,
        mComponentView);
  }

  /**
   * To be called whenever the components needs to start the mount process from scratch
   * e.g. when the component's props or layout change or when the components
   * gets attached to a host.
   */
  void setDirty() {
    assertMainThread();

    mIsDirty = true;
    mPreviousLocalVisibleRect.setEmpty();
  }

  boolean isDirty() {
    assertMainThread();

    return mIsDirty;
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   * @param layoutState
   * @param localVisibleRect If this variable is null, then mount everything, since incremental
   *                         mount is not enabled.
   *                         Otherwise mount only what the rect (in local coordinates) contains
   */
  void mount(LayoutState layoutState, Rect localVisibleRect) {
    assertMainThread();

    ComponentsSystrace.beginSection("mount");

    final ComponentTree componentTree = mComponentView.getComponent();
    final ComponentsLogger logger = componentTree.getContext().getLogger();

    if (logger != null) {
      logger.eventStart(EVENT_MOUNT, componentTree);
    }

    prepareTransitionManager(layoutState);
    if (mTransitionManager != null) {
      if (mIsDirty) {
        mTransitionManager.onNewTransitionContext(layoutState.getTransitionContext());
      }

      mTransitionManager.onMountStart();
      recordMountedItemsWithTransitionKeys(
          mTransitionManager,
          mIndexToItemMap,
          true /* isPreMount */);
    }

    if (mIsDirty) {
      suppressInvalidationsOnHosts(true);

      // Prepare the data structure for the new LayoutState and removes mountItems
      // that are not present anymore if isUpdateMountInPlace is enabled.
      prepareMount(layoutState);
    }

    mMountStats.reset();

    final int componentTreeId = layoutState.getComponentTreeId();
    final boolean isIncrementalMountEnabled = localVisibleRect != null;

    if (!isIncrementalMountEnabled ||
            !performIncrementalMount(layoutState, localVisibleRect)) {
      for (int i = 0, size = layoutState.getMountableOutputCount(); i < size; i++) {
        final LayoutOutput layoutOutput = layoutState.getMountableOutputAt(i);
        final Component component = layoutOutput.getComponent();
        ComponentsSystrace.beginSection(component.getSimpleName());
        final MountItem currentMountItem = getItemAt(i);

        final boolean isMounted = currentMountItem != null;
        final boolean isMountable =
            !isIncrementalMountEnabled ||
                isMountedHostWithChildContent(currentMountItem) ||
                Rect.intersects(localVisibleRect, layoutOutput.getBounds());

        if (isMountable && !isMounted) {
          mountLayoutOutput(i, layoutOutput, layoutState);
        } else if (!isMountable && isMounted) {
          unmountItem(mContext, i, mHostsByMarker);
        } else if (isMounted) {
          if (isIncrementalMountEnabled && canMountIncrementally(component)) {
            mountItemIncrementally(currentMountItem, layoutOutput.getBounds(), localVisibleRect);
          }

          if (mIsDirty) {
            final boolean useUpdateValueFromLayoutOutput =
                (componentTreeId >= 0) && (componentTreeId == mLastMountedComponentTreeId);

            final boolean itemUpdated = updateMountItemIfNeeded(
                layoutOutput,
                currentMountItem,
                useUpdateValueFromLayoutOutput,
                logger);

            if (itemUpdated) {
              mMountStats.updatedCount++;
            } else {
              mMountStats.noOpCount++;
            }
          }
        }

        ComponentsSystrace.endSection();
      }

      if (isIncrementalMountEnabled) {
        setupPreviousMountableOutputData(layoutState, localVisibleRect);
      }
    }

    mIsDirty = false;
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }

    processVisibilityOutputs(layoutState, localVisibleRect);

    if (mTransitionManager != null) {
      recordMountedItemsWithTransitionKeys(
          mTransitionManager,
          mIndexToItemMap,
          false /* isPreMount */);
      mTransitionManager.processTransitions();
    }

    processTestOutputs(layoutState);

    suppressInvalidationsOnHosts(false);

    mLastMountedComponentTreeId = componentTreeId;

    if (logger != null) {
      final String logTag = componentTree.getContext().getLogTag();
      logMountEnd(logger, logTag, componentTree, mMountStats);
    }

    ComponentsSystrace.endSection();
  }

  private void processVisibilityOutputs(LayoutState layoutState, Rect localVisibleRect) {
    if (localVisibleRect == null) {
      return;
    }

    for (int j = 0, size = layoutState.getVisibilityOutputCount(); j < size; j++) {
      final VisibilityOutput visibilityOutput = layoutState.getVisibilityOutputAt(j);

      final EventHandler visibleHandler = visibilityOutput.getVisibleEventHandler();
      final EventHandler focusedHandler = visibilityOutput.getFocusedEventHandler();
      final EventHandler fullImpressionHandler = visibilityOutput.getFullImpressionEventHandler();
      final EventHandler invisibleHandler = visibilityOutput.getInvisibleEventHandler();
      final long visibilityOutputId = visibilityOutput.getId();
      final Rect visibilityOutputBounds = visibilityOutput.getBounds();

      sTempRect.set(visibilityOutputBounds);
      final boolean isCurrentlyVisible = sTempRect.intersect(localVisibleRect);

      VisibilityItem visibilityItem = mVisibilityIdToItemMap.get(visibilityOutputId);

      if (isCurrentlyVisible) {
        // The component is visible now, but used to be outside the viewport.
        if (visibilityItem == null) {
          visibilityItem = ComponentsPools.acquireVisibilityItem(invisibleHandler);
          mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

          if (visibleHandler != null) {
            EventDispatcherUtils.dispatchOnVisible(visibleHandler);
          }
        }

        // Check if the component has entered the focused range.
        if (focusedHandler != null && !visibilityItem.isInFocusedRange()) {
          final View parent = (View) mComponentView.getParent();

          if (hasEnteredFocusedRange(
              parent.getWidth(),
              parent.getHeight(),
              visibilityOutputBounds,
              sTempRect)) {
            visibilityItem.setIsInFocusedRange();
            EventDispatcherUtils.dispatchOnFocused(focusedHandler);
          }
        }

        // If the component has not entered the full impression range yet, make sure to update the
        // information about the visible edges.
        if (fullImpressionHandler != null && !visibilityItem.isInFullImpressionRange()) {
          visibilityItem.setVisibleEdges(visibilityOutputBounds, sTempRect);

          if (visibilityItem.isInFullImpressionRange()) {
            EventDispatcherUtils.dispatchOnFullImpression(fullImpressionHandler);
          }
        }
      } else if (visibilityItem != null) {
        // The component is invisible now, but used to be visible.
        if (invisibleHandler != null) {
          EventDispatcherUtils.dispatchOnInvisible(invisibleHandler);
        }

        mVisibilityIdToItemMap.remove(visibilityOutputId);
        ComponentsPools.release(visibilityItem);
      }
    }
  }

  /**
   * Clears and re-populates the test item map if we are in e2e test mode.
   */
  private void processTestOutputs(LayoutState layoutState) {
    if (mTestItemMap == null) {
      return;
    }

    for (Collection<TestItem> items : mTestItemMap.values()) {
      for (TestItem item : items) {
        ComponentsPools.release(item);
      }
    }
    mTestItemMap.clear();

    for (int i = 0, size = layoutState.getTestOutputCount(); i < size; i++) {
      final TestOutput testOutput = layoutState.getTestOutputAt(i);
      final long hostMarker = testOutput.getHostMarker();
      final long layoutOutputId = testOutput.getLayoutOutputId();
      final MountItem mountItem =
          layoutOutputId == -1 ? null : mIndexToItemMap.get(layoutOutputId);
      final TestItem testItem = ComponentsPools.acquireTestItem();
      testItem.setHost(hostMarker == -1 ? null : mHostsByMarker.get(hostMarker));
      testItem.setBounds(testOutput.getBounds());
      testItem.setTestKey(testOutput.getTestKey());
      testItem.setContent(mountItem == null ? null : mountItem.getContent());

      final Deque<TestItem> items = mTestItemMap.get(testOutput.getTestKey());
      final Deque<TestItem> updatedItems =
          items == null ? new LinkedList<TestItem>() : items;
      updatedItems.add(testItem);
      mTestItemMap.put(testOutput.getTestKey(), updatedItems);
    }
  }

  private boolean isMountedHostWithChildContent(MountItem mountItem) {
    if (mountItem == null) {
      return false;
    }

    final Object content = mountItem.getContent();
    if (!(content instanceof ComponentHost)) {
      return false;
    }

    final ComponentHost host = (ComponentHost) content;
    return host.getMountItemCount() > 0;
  }

  private void setupPreviousMountableOutputData(LayoutState layoutState, Rect localVisibleRect) {
    if (localVisibleRect.isEmpty()) {
      return;
    }

    final ArrayList<LayoutOutput> layoutOutputTops = layoutState.getMountableOutputTops();
    final ArrayList<LayoutOutput> layoutOutputBottoms = layoutState.getMountableOutputBottoms();
    final int mountableOutputCount = layoutState.getMountableOutputCount();

    mPreviousTopsIndex = layoutState.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.bottom <= layoutOutputTops.get(i).getBounds().top) {
        mPreviousTopsIndex = i;
        break;
      }
    }

    mPreviousBottomsIndex = layoutState.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top < layoutOutputBottoms.get(i).getBounds().bottom) {
        mPreviousBottomsIndex = i;
        break;
      }
    }
  }

  private void clearVisibilityItems() {
    for (int i = mVisibilityIdToItemMap.size() - 1; i >= 0; i--) {
      final VisibilityItem visibilityItem = mVisibilityIdToItemMap.valueAt(i);
      final EventHandler invisibleHandler = visibilityItem.getInvisibleHandler();

      if (invisibleHandler != null) {
        EventDispatcherUtils.dispatchOnInvisible(invisibleHandler);
      }

      mVisibilityIdToItemMap.removeAt(i);
      ComponentsPools.release(visibilityItem);
    }
  }

  private void registerHost(long id, ComponentHost host) {
    host.suppressInvalidations(true);
    mHostsByMarker.put(id, host);
  }

  /**
   * Returns true if the component has entered the focused visible range.
   */
  static boolean hasEnteredFocusedRange(
      int viewportWidth,
      int viewportHeight,
      Rect componentBounds,
      Rect componentVisibleBounds) {
    final int halfViewportArea = viewportWidth * viewportHeight / 2;
    final int totalComponentArea = computeRectArea(componentBounds);
    final int visibleComponentArea = computeRectArea(componentVisibleBounds);

    // The component has entered the focused range either if it is larger than half of the viewport
    // and it occupies at least half of the viewport or if it is smaller than half of the viewport
    // and it is fully visible.
    return (totalComponentArea >= halfViewportArea)
        ? (visibleComponentArea >= halfViewportArea)
        : componentBounds.equals(componentVisibleBounds);
  }

  private static int computeRectArea(Rect rect) {
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  private void suppressInvalidationsOnHosts(boolean suppressInvalidations) {
    for (int i = mHostsByMarker.size() - 1; i >= 0; i--) {
      mHostsByMarker.valueAt(i).suppressInvalidations(suppressInvalidations);
    }
  }

  private boolean updateMountItemIfNeeded(
      LayoutOutput layoutOutput,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput,
      ComponentsLogger logger) {
    final Component layoutOutputComponent = layoutOutput.getComponent();
    final Component itemComponent = currentMountItem.getComponent();

    // 1. Check if the mount item generated from the old component should be updated.
    final boolean shouldUpdate = shouldUpdateMountItem(
        layoutOutput,
        currentMountItem,
        useUpdateValueFromLayoutOutput,
        mIndexToItemMap,
        mLayoutOutputsIds,
        logger);

    // 2. Reset all the properties like click handler, content description and tags related to
    // this item if it needs to be updated. the update mount item will re-set the new ones.
    if (shouldUpdate) {
      unsetViewAttributes(currentMountItem);
    }

    // 3. We will re-bind this later in 7 regardless so let's make sure it's currently unbound.
    if (currentMountItem.isBound()) {
      itemComponent.getLifecycle().onUnbind(
          itemComponent.getScopedContext(),
          currentMountItem.getContent(),
          itemComponent);
      currentMountItem.setIsBound(false);
    }

    // 4. Re initialize the MountItem internal state with the new attributes from LayoutOutput
    currentMountItem.init(layoutOutput.getComponent(), currentMountItem, layoutOutput);

    // 5. If the mount item is not valid for this component update its content and view attributes.
    if (shouldUpdate) {
      updateMountedContent(currentMountItem, layoutOutput, itemComponent);
      setViewAttributes(currentMountItem);
    }

    final Object currentContent = currentMountItem.getContent();

    // 6. Set the mounted content on the Component and call the bind callback.
    layoutOutputComponent.getLifecycle().bind(
        layoutOutputComponent.getScopedContext(),
        currentContent,
        layoutOutputComponent);
    currentMountItem.setIsBound(true);

    // 7. Update the bounds of the mounted content. This needs to be done regardless of whether
    // the component has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedLayoutOutput(layoutOutput, currentMountItem);

    maybeInvalidateAccessibilityState(currentMountItem);
    if (currentMountItem.getContent() instanceof Drawable) {
      maybeSetDrawableState(
          currentMountItem.getHost(),
          (Drawable) currentMountItem.getContent(),
          currentMountItem.getFlags(),
          currentMountItem.getNodeInfo());
    }

    if (currentMountItem.getDisplayListDrawable() != null) {
      currentMountItem.getDisplayListDrawable().suppressInvalidations(false);
    }

    return shouldUpdate;
  }

  private static boolean shouldUpdateMountItem(
      LayoutOutput layoutOutput,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput,
      LongSparseArray<MountItem> indexToItemMap,
      long[] layoutOutputsIds,
      ComponentsLogger logger) {
    final @LayoutOutput.UpdateState int updateState = layoutOutput.getUpdateState();
    final Component currentComponent = currentMountItem.getComponent();
    final ComponentLifecycle currentLifecycle = currentComponent.getLifecycle();
    final Component nextComponent = layoutOutput.getComponent();
    final ComponentLifecycle nextLifecycle = nextComponent.getLifecycle();

    // If the two components have different sizes and the mounted content depends on the size we
    // just return true immediately.
    if (!sameSize(layoutOutput, currentMountItem) && nextLifecycle.isMountSizeDependent()) {
      return true;
    }

    if (useUpdateValueFromLayoutOutput) {
      if (updateState == LayoutOutput.STATE_UPDATED) {

        // Check for incompatible ReferenceLifecycle.
        if (currentLifecycle instanceof DrawableComponent
            && nextLifecycle instanceof DrawableComponent
            && currentLifecycle.shouldComponentUpdate(currentComponent, nextComponent)) {

          if (logger != null) {
            ComponentsLogger.LayoutOutputLog logObj = new ComponentsLogger.LayoutOutputLog();

            logObj.currentId = indexToItemMap.keyAt(
                indexToItemMap.indexOfValue(currentMountItem));
            logObj.currentLifecycle = currentLifecycle.toString();

            logObj.nextId = layoutOutput.getId();
            logObj.nextLifecycle = nextLifecycle.toString();

            for (int i = 0; i < layoutOutputsIds.length; i++) {
              if (layoutOutputsIds[i] == logObj.currentId) {
                if (logObj.currentIndex == -1) {
                  logObj.currentIndex = i;
                }

                logObj.currentLastDuplicatedIdIndex = i;
              }
            }

            if (logObj.nextId == logObj.currentId) {
              logObj.nextIndex = logObj.currentIndex;
              logObj.nextLastDuplicatedIdIndex = logObj.currentLastDuplicatedIdIndex;
            } else {
              for (int i = 0; i < layoutOutputsIds.length; i++) {
                if (layoutOutputsIds[i] == logObj.nextId) {
                  if (logObj.nextIndex == -1) {
                    logObj.nextIndex = i;
                  }

                  logObj.nextLastDuplicatedIdIndex = i;
                }
              }
            }

            logger.eventStart(EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH, logObj);
            logger
                .eventEnd(EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH, logObj, ACTION_SUCCESS);
          }

          return true;
        }

        return false;
      } else if (updateState == LayoutOutput.STATE_DIRTY) {
        return true;
      }
    }

    if (!currentLifecycle.callsShouldUpdateOnMount()) {
      return true;
    }

    return currentLifecycle.shouldComponentUpdate(
        currentComponent,
        nextComponent);
  }

  private static boolean sameSize(LayoutOutput layoutOutput, MountItem item) {
    final Rect layoutOutputBounds = layoutOutput.getBounds();
    final Object mountedContent = item.getContent();

    return layoutOutputBounds.width() == getWidthForMountedContent(mountedContent) &&
        layoutOutputBounds.height() == getHeightForMountedContent(mountedContent);
  }

  private static int getWidthForMountedContent(Object content) {
    return content instanceof Drawable ?
        ((Drawable) content).getBounds().width() :
        ((View) content).getWidth();
  }

  private static int getHeightForMountedContent(Object content) {
    return content instanceof Drawable ?
        ((Drawable) content).getBounds().height() :
        ((View) content).getHeight();
  }

  private void updateBoundsForMountedLayoutOutput(LayoutOutput layoutOutput, MountItem item) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the ComponentView.
    if (layoutOutput.getId() == ROOT_HOST_ID) {
      return;
    }

    layoutOutput.getMountBounds(sTempRect);

    final boolean forceTraversal = Component.isMountViewSpec(layoutOutput.getComponent())
        && ((View) item.getContent()).isLayoutRequested();

    applyBoundsToMountContent(
        item.getContent(),
        sTempRect.left,
        sTempRect.top,
        sTempRect.right,
        sTempRect.bottom,
        forceTraversal /* force */);
  }

  /**
   * Prepare the {@link MountState} to mount a new {@link LayoutState}.
   */
  @SuppressWarnings("unchecked")
  private void prepareMount(LayoutState layoutState) {
    final ComponentTree component = mComponentView.getComponent();
    final ComponentsLogger logger = component.getContext().getLogger();
    final String logTag = component.getContext().getLogTag();

    if (logger != null) {
      logger.eventStart(EVENT_PREPARE_MOUNT, component);
    }

    PrepareMountStats stats = unmountOrMoveOldItems(layoutState);

    if (logger != null) {
      logPrepareMountParams(logger, logTag, component, stats);
    }

    if (mHostsByMarker.get(ROOT_HOST_ID) == null) {
      // Mounting always starts with the root host.
      registerHost(ROOT_HOST_ID, mComponentView);

      // Root host is implicitly marked as mounted.
      mIndexToItemMap.put(ROOT_HOST_ID, mRootHostMountItem);
    }

    int outputCount = layoutState.getMountableOutputCount();
    if (mLayoutOutputsIds == null || outputCount != mLayoutOutputsIds.length) {
      mLayoutOutputsIds = new long[layoutState.getMountableOutputCount()];
    }

    for (int i = 0; i < outputCount; i++) {
      mLayoutOutputsIds[i] = layoutState.getMountableOutputAt(i).getId();
    }

    if (logger != null) {
      logger.eventEnd(EVENT_PREPARE_MOUNT, component, ACTION_SUCCESS);
    }
  }

  /**
   * Determine whether to apply disappear animation to the given {@link MountItem}
   */
  private static boolean isItemDisappearing(
      MountItem mountItem,
      TransitionContext transitionContext) {
    if (mountItem == null
        || mountItem.getViewNodeInfo() == null
        || transitionContext == null) {
      return false;
    }

    return transitionContext.isDisappearingKey(mountItem.getViewNodeInfo().getTransitionKey());
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs.
   * If an item is still present but in a new position move the item inside its host.
   * The condition where an item changed host doesn't need any special treatment here since we
   * mark them as removed and re-added when calculating the new LayoutOutputs
   */
  private PrepareMountStats unmountOrMoveOldItems(LayoutState newLayoutState) {
    mPrepareMountStats.reset();

    if (mLayoutOutputsIds == null) {
      return mPrepareMountStats;
    }

    // Traversing from the beginning since mLayoutOutputsIds unmounting won't remove entries there
    // but only from mIndexToItemMap. If an host changes we're going to unmount it and recursively
    // all its mounted children.
    for (int i = 0; i < mLayoutOutputsIds.length; i++) {
      final int newPosition = newLayoutState.getLayoutOutputPositionForId(mLayoutOutputsIds[i]);
      final MountItem oldItem = getItemAt(i);

      if (isItemDisappearing(oldItem, newLayoutState.getTransitionContext())) {

        startUnmountDisappearingItem(i, oldItem.getViewNodeInfo().getTransitionKey());

        final int lastDescendantOfItem = findLastDescendantOfItem(i, oldItem);
        // Disassociate disappearing items from current mounted items. The layout tree will not
        // contain disappearing items anymore, however they are kept separately in their hosts.
        removeDisappearingItemMappings(i, lastDescendantOfItem);

        // Skip this disappearing item and all its descendants. Do not unmount or move them yet.
        // We will unmount them after animation is completed.
        i = lastDescendantOfItem;
        continue;
      }

      if (newPosition == -1) {
        unmountItem(mContext, i, mHostsByMarker);
        mPrepareMountStats.unmountedCount++;
      } else {
        final long newHostMarker = newLayoutState.getMountableOutputAt(newPosition).getHostMarker();

        if (oldItem == null) {
          // This was previously unmounted.
          mPrepareMountStats.unmountedCount++;
        } else if (oldItem.getHost() != mHostsByMarker.get(newHostMarker)) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItem(mContext, i, mHostsByMarker);
          mPrepareMountStats.unmountedCount++;
        } else if (newPosition != i) {
          // If a MountItem for this id exists and the hostMarker has not changed but its position
          // in the outputs array has changed we need to update the position in the Host to ensure
          // the z-ordering.
          oldItem.getHost().moveItem(oldItem, i, newPosition);
          mPrepareMountStats.movedCount++;
        } else {
          mPrepareMountStats.unchangedCount++;
        }
      }
    }

