/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.Reference;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.litho.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.litho.ComponentsLogger.EVENT_PREPARE_MOUNT;
import static com.facebook.litho.ComponentsLogger.EVENT_SHOULD_UPDATE_REFERENCE_LAYOUT_MISMATCH;
import static com.facebook.litho.ComponentsLogger.PARAM_IS_DIRTY;
import static com.facebook.litho.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.litho.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_MOVED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_NO_OP_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UNCHANGED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UPDATED_COUNT;
import static com.facebook.litho.ThreadUtils.assertMainThread;

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

