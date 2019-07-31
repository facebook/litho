/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_IS_DIRTY;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_EXTRAS;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_TIME;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOVED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_NO_OP_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNCHANGED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_TIME;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UPDATED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UPDATED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UPDATED_TIME;
import static com.facebook.litho.FrameworkLogEvents.PARAM_VISIBILITY_HANDLER;
import static com.facebook.litho.FrameworkLogEvents.PARAM_VISIBILITY_HANDLERS_TOTAL_TIME;
import static com.facebook.litho.FrameworkLogEvents.PARAM_VISIBILITY_HANDLER_TIME;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableDrawable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the mounted state of a {@link Component}. Provides APIs to update state by recycling
 * existing UI elements e.g. {@link Drawable}s.
 *
 * @see #mount(LayoutState, Rect, boolean)
 * @see LithoView
 * @see LayoutState
 */
@ThreadConfined(ThreadConfined.UI)
class MountState implements TransitionManager.OnAnimationCompleteListener {

  static final long ROOT_HOST_ID = 0L;
  private static final double NS_IN_MS = 1000000.0;

  private static final BitSet sEmptyBitSet = new BitSet(0);

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

  // Both these arrays are updated in prepareMount(), thus during mounting they hold the information
  // about the LayoutState that is being mounted, not mLastMountedLayoutState
  @Nullable private long[] mLayoutOutputsIds;

  // True if we are receiving a new LayoutState and we need to completely
  // refresh the content of the HostComponent. Always set from the main thread.
  private boolean mIsDirty;

  // True if MountState is currently performing mount
  private boolean mIsMounting;

  // See #needsRemount()
  private boolean mNeedsRemount;

  // Holds the list of known component hosts during a mount pass.
  private final LongSparseArray<ComponentHost> mHostsByMarker = new LongSparseArray<>();

  private static final Rect sTempRect = new Rect();
  private static final Rect sTempRect2 = new Rect();

  private final ComponentContext mContext;
  private final LithoView mLithoView;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final PrepareMountStats mPrepareMountStats = new PrepareMountStats();
  private final MountStats mMountStats = new MountStats();
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private int mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
  private LayoutState mLastMountedLayoutState;
  private boolean mIsFirstMountOfComponentTree = false;
  private int mLastDisappearRangeStart = -1;
  private int mLastDisappearRangeEnd = -1;

  private final MountItem mRootHostMountItem;

  private TransitionManager mTransitionManager;
  private final HashSet<TransitionId> mAnimatingTransitionIds = new HashSet<>();
  private int[] mAnimationLockedIndices;
  private final Map<TransitionId, OutputUnitsAffinityGroup<MountItem>> mDisappearingMountItems =
      new LinkedHashMap<>();
  private @Nullable Transition mRootTransition;
  private boolean mTransitionsHasBeenCollected = false;
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();

  private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();

  public MountState(LithoView view) {
    mIndexToItemMap = new LongSparseArray<>();
    mVisibilityIdToItemMap = new LongSparseArray<>();
    mCanMountIncrementallyMountItems = new LongSparseArray<>();
    mContext = view.getComponentContext();
    mLithoView = view;
    mIsDirty = true;

    mTestItemMap =
        ComponentsConfiguration.isEndToEndTestRun ? new HashMap<String, Deque<TestItem>>() : null;

    // The mount item representing the top-level root host (LithoView) which
    // is always automatically mounted.
    mRootHostMountItem = MountItem.createRootHostMountItem(mLithoView);
  }

  /**
   * To be called whenever the components needs to start the mount process from scratch e.g. when
   * the component's props or layout change or when the components gets attached to a host.
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

  boolean isMounting() {
    assertMainThread();

    return mIsMounting;
  }

  /**
   * True if we have manually unmounted content (e.g. via unmountAllItems) which means that while we
   * may not have a new LayoutState, the mounted content does not match what the viewport for the
   * LithoView may be.
   */
  boolean needsRemount() {
    assertMainThread();

    return mNeedsRemount;
  }

  /**
   * Sets whether the next mount will be the first mount of this ComponentTree. This is used to
   * determine whether to run animations or not (we want animations to run on the first mount of
   * this ComponentTree, but not other times the mounted ComponentTree id changes). Ideally, we want
   * animations to only occur when the ComponentTree is updated on screen or is first inserted into
   * a list onscreen, but that requires more integration with the list controller, e.g. sections,
   * than we currently have.
   */
  void setIsFirstMountOfComponentTree() {
    assertMainThread();

    mIsFirstMountOfComponentTree = true;
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param layoutState
   * @param localVisibleRect If this variable is null, then mount everything, since incremental
   *     mount is not enabled. Otherwise mount only what the rect (in local coordinates) contains
   * @param processVisibilityOutputs whether to process visibility outputs as part of the mount
   */
  void mount(LayoutState layoutState, Rect localVisibleRect, boolean processVisibilityOutputs) {
    assertMainThread();

    if (layoutState == null) {
      throw new IllegalStateException("Trying to mount a null layoutState");
    }

    if (mIsMounting) {
      throw new IllegalStateException("Trying to mount while already mounting");
    }
    mIsMounting = true;

    final ComponentTree componentTree = mLithoView.getComponentTree();
    final boolean isIncrementalMountEnabled = localVisibleRect != null;
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      final StringBuilder sectionName =
          new StringBuilder(isIncrementalMountEnabled ? "incrementalMount" : "mount")
              .append("_")
              .append(componentTree.getSimpleName());
      final String logTag = componentTree.getContext().getLogTag();
      if (logTag != null) {
        sectionName.append("_").append(logTag);
      }
      ComponentsSystrace.beginSectionWithArgs(sectionName.toString())
          .arg("treeId", layoutState.getComponentTreeId())
          .flush();
    }

    final ComponentsLogger logger = componentTree.getContext().getLogger();
    final int componentTreeId = layoutState.getComponentTreeId();
    if (componentTreeId != mLastMountedComponentTreeId) {
      // If we're mounting a new ComponentTree, don't keep around and use the previous LayoutState
      // since things like transition animations aren't relevant.
      mLastMountedLayoutState = null;
    }

    final PerfEvent mountPerfEvent =
        logger == null
            ? null
            : LogTreePopulator.populatePerfEventFromLogger(
                componentTree.getContext(),
                logger,
                logger.newPerformanceEvent(componentTree.getContext(), EVENT_MOUNT));

    if (mIsDirty) {
      updateTransitions(layoutState, componentTree);

      suppressInvalidationsOnHosts(true);

      // Prepare the data structure for the new LayoutState and removes mountItems
      // that are not present anymore if isUpdateMountInPlace is enabled.
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("PREPARE_MOUNT_START");
      }
      prepareMount(layoutState, mountPerfEvent);
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("PREPARE_MOUNT_END");
      }
    }

    mMountStats.reset();
    if (mountPerfEvent != null && logger.isTracing(mountPerfEvent)) {
      mMountStats.enableLogging();
    }

    if (!isIncrementalMountEnabled
        || !performIncrementalMount(layoutState, localVisibleRect, processVisibilityOutputs)) {
      final MountItem rootMountItem = mIndexToItemMap.get(ROOT_HOST_ID);

      for (int i = 0, size = layoutState.getMountableOutputCount(); i < size; i++) {
        final LayoutOutput layoutOutput = layoutState.getMountableOutputAt(i);
        final Component component = layoutOutput.getComponent();
        if (isTracing) {
          ComponentsSystrace.beginSection(component.getSimpleName());
        }

        final MountItem currentMountItem = getItemAt(i);
        final boolean isMounted = currentMountItem != null;
        final boolean isMountable =
            !isIncrementalMountEnabled
                || isMountedHostWithChildContent(currentMountItem)
                || Rect.intersects(localVisibleRect, layoutOutput.getBounds())
                || isAnimationLocked(i)
                || (currentMountItem != null && currentMountItem == rootMountItem);

        if (isMountable && !isMounted) {
          mountLayoutOutput(i, layoutOutput, layoutState);

          if (isAnimationLocked(i) && isIncrementalMountEnabled && component.hasChildLithoViews()) {
            // If the component is locked for animation then we need to make sure that all the
            // children are also mounted.
            final View view = (View) getItemAt(i).getContent();
            // We're mounting everything, don't process visibility outputs as they will not be
            // accurate.
            mountViewIncrementally(view, false);
          }
        } else if (!isMountable && isMounted) {
          unmountItem(i, mHostsByMarker);
        } else if (isMounted) {
          if (mIsDirty) {
            final boolean useUpdateValueFromLayoutOutput =
                mLastMountedLayoutState != null
                    && mLastMountedLayoutState.getId() == layoutState.getPreviousLayoutStateId();

            final long startTime = System.nanoTime();
            final TransitionId transitionId = currentMountItem.getTransitionId();
            final boolean itemUpdated =
                updateMountItemIfNeeded(
                    layoutOutput,
                    layoutState,
                    currentMountItem,
                    useUpdateValueFromLayoutOutput,
                    componentTreeId,
                    i);

            if (itemUpdated) {
              // This mount content might be animating and we may be remounting it as a different
              // component in the same tree, or as a component in a totally different tree so we
              // will reset animating content for its key
              maybeRemoveAnimatingMountContent(transitionId);
            }

            if (mMountStats.isLoggingEnabled) {
              if (itemUpdated) {
                mMountStats.updatedNames.add(component.getSimpleName());
                mMountStats.updatedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
                mMountStats.updatedCount++;
              } else {
                mMountStats.noOpCount++;
              }
            }
          }

          if (isIncrementalMountEnabled && component.hasChildLithoViews()) {
            mountItemIncrementally(currentMountItem, processVisibilityOutputs);
          }
        }

        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }

      if (isIncrementalMountEnabled) {
        setupPreviousMountableOutputData(layoutState, localVisibleRect);
      }
    }

    maybeUpdateAnimatingMountContent();
    if (shouldAnimateTransitions(layoutState) && hasTransitionsToAnimate()) {
      mTransitionManager.runTransitions();
    }

    if (processVisibilityOutputs) {
      ComponentsSystrace.beginSection("processVisibilityOutputs");
      processVisibilityOutputs(layoutState, localVisibleRect, mountPerfEvent);
      ComponentsSystrace.endSection();
    }

    mRootTransition = null;
    mTransitionsHasBeenCollected = false;
    final boolean wasDirty = mIsDirty;
    mIsDirty = false;
    mNeedsRemount = false;
    mIsFirstMountOfComponentTree = false;
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }

    mLastMountedLayoutState = null;
    mLastMountedComponentTreeId = componentTreeId;
    mLastMountedLayoutState = layoutState;

    processTestOutputs(layoutState);

    suppressInvalidationsOnHosts(false);

    if (logger != null) {
      logMountPerfEvent(logger, mountPerfEvent, wasDirty);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
    mIsMounting = false;
  }

  private void logMountPerfEvent(
      ComponentsLogger logger, @Nullable PerfEvent mountPerfEvent, boolean isDirty) {
    if (!mMountStats.isLoggingEnabled || mountPerfEvent == null) {
      logger.cancelPerfEvent(mountPerfEvent);
      return;
    }

    // MOUNT events that don't mount any content are not valuable enough to log at the moment.
    // We will likely enable them again in the future. T31729233
    if (mMountStats.mountedCount == 0 || mMountStats.mountedNames.isEmpty()) {
      logger.cancelPerfEvent(mountPerfEvent);
      return;
    }

    mountPerfEvent.markerAnnotate(PARAM_MOUNTED_COUNT, mMountStats.mountedCount);
    mountPerfEvent.markerAnnotate(
        PARAM_MOUNTED_CONTENT, mMountStats.mountedNames.toArray(new String[0]));
    mountPerfEvent.markerAnnotate(
        PARAM_MOUNTED_TIME, mMountStats.mountTimes.toArray(new Double[0]));

    mountPerfEvent.markerAnnotate(PARAM_UNMOUNTED_COUNT, mMountStats.unmountedCount);
    mountPerfEvent.markerAnnotate(
        PARAM_UNMOUNTED_CONTENT, mMountStats.unmountedNames.toArray(new String[0]));
    mountPerfEvent.markerAnnotate(
        PARAM_UNMOUNTED_TIME, mMountStats.unmountedTimes.toArray(new Double[0]));
    mountPerfEvent.markerAnnotate(PARAM_MOUNTED_EXTRAS, mMountStats.extras.toArray(new String[0]));

    mountPerfEvent.markerAnnotate(PARAM_UPDATED_COUNT, mMountStats.updatedCount);
    mountPerfEvent.markerAnnotate(
        PARAM_UPDATED_CONTENT, mMountStats.updatedNames.toArray(new String[0]));
    mountPerfEvent.markerAnnotate(
        PARAM_UPDATED_TIME, mMountStats.updatedTimes.toArray(new Double[0]));

    mountPerfEvent.markerAnnotate(
        PARAM_VISIBILITY_HANDLERS_TOTAL_TIME, mMountStats.visibilityHandlersTotalTime);
    mountPerfEvent.markerAnnotate(
        PARAM_VISIBILITY_HANDLER, mMountStats.visibilityHandlerNames.toArray(new String[0]));
    mountPerfEvent.markerAnnotate(
        PARAM_VISIBILITY_HANDLER_TIME, mMountStats.visibilityHandlerTimes.toArray(new Double[0]));

    mountPerfEvent.markerAnnotate(PARAM_NO_OP_COUNT, mMountStats.noOpCount);
    mountPerfEvent.markerAnnotate(PARAM_IS_DIRTY, isDirty);

    logger.logPerfEvent(mountPerfEvent);
  }

  private void maybeRemoveAnimatingMountContent(TransitionId transitionId) {
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

  private void maybeUpdateAnimatingMountContent() {
    if (mTransitionManager == null) {
      return;
    }

    // Group mount content (represents current LayoutStates only) into groups and pass it to the
    // TransitionManager
    final Map<TransitionId, OutputUnitsAffinityGroup<Object>> animatingContent =
        new LinkedHashMap<>(mAnimatingTransitionIds.size());
    for (int i = 0, size = mIndexToItemMap.size(); i < size; i++) {
      final MountItem mountItem = mIndexToItemMap.valueAt(i);
      if (!mountItem.hasTransitionId()) {
        continue;
      }
      final long layoutOutputId = mIndexToItemMap.keyAt(i);
      final @OutputUnitType int type = LayoutStateOutputIdCalculator.getTypeFromId(layoutOutputId);
      OutputUnitsAffinityGroup<Object> group = animatingContent.get(mountItem.getTransitionId());
      if (group == null) {
        group = new OutputUnitsAffinityGroup<>();
        animatingContent.put(mountItem.getTransitionId(), group);
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
  }

  void processVisibilityOutputs(
      LayoutState layoutState, Rect localVisibleRect, @Nullable PerfEvent mountPerfEvent) {
    assertMainThread();

    if (localVisibleRect == null) {
      return;
    }

    if (mountPerfEvent != null) {
      mountPerfEvent.markerPoint("VISIBILITY_HANDLERS_START");
    }

    final boolean isDoingPerfLog = mMountStats.isLoggingEnabled;
    final boolean isTracing = ComponentsSystrace.isTracing();
    final long totalStartTime = isDoingPerfLog ? System.nanoTime() : 0L;
    for (int j = 0, size = layoutState.getVisibilityOutputCount(); j < size; j++) {
      final VisibilityOutput visibilityOutput = layoutState.getVisibilityOutputAt(j);
      if (isTracing) {
        final String componentName =
            visibilityOutput.getComponent() != null
                ? visibilityOutput.getComponent().getSimpleName()
                : "Unknown";
        ComponentsSystrace.beginSection("visibilityHandlers:" + componentName);
      }
      final long handlerStartTime = isDoingPerfLog ? System.nanoTime() : 0;
      final EventHandler<VisibleEvent> visibleHandler = visibilityOutput.getVisibleEventHandler();
      final EventHandler<FocusedVisibleEvent> focusedHandler =
          visibilityOutput.getFocusedEventHandler();
      final EventHandler<UnfocusedVisibleEvent> unfocusedHandler =
          visibilityOutput.getUnfocusedEventHandler();
      final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
          visibilityOutput.getFullImpressionEventHandler();
      final EventHandler<InvisibleEvent> invisibleHandler =
          visibilityOutput.getInvisibleEventHandler();
      final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
          visibilityOutput.getVisibilityChangedEventHandler();
      final long visibilityOutputId = visibilityOutput.getId();
      final Rect visibilityOutputBounds = visibilityOutput.getBounds();

      boolean boundsIntersect = sTempRect.setIntersect(visibilityOutputBounds, localVisibleRect);
      final boolean isCurrentlyVisible =
          boundsIntersect && isInVisibleRange(visibilityOutput, visibilityOutputBounds, sTempRect);

      VisibilityItem visibilityItem = mVisibilityIdToItemMap.get(visibilityOutputId);
      if (visibilityItem != null) {
        final String previousGlobalKey = visibilityItem.getGlobalKey();
        final String currentGlobalKey =
            visibilityOutput.getComponent() != null
                ? visibilityOutput.getComponent().getGlobalKey()
                : null;
        final boolean hasGlobalKeyChanged =
            previousGlobalKey != null && !previousGlobalKey.equals(currentGlobalKey);

        if (!hasGlobalKeyChanged) {
          // If we did a relayout due to e.g. a state update then the handlers will have changed,
          // so we should keep them up to date.
          visibilityItem.setUnfocusedHandler(unfocusedHandler);
          visibilityItem.setInvisibleHandler(invisibleHandler);
        }

        if (!isCurrentlyVisible || hasGlobalKeyChanged) {
          // Either the component is invisible now, but used to be visible, or the key on the
          // component has changed so we should generate new visibility events for the new
          // component.
          if (visibilityItem.getInvisibleHandler() != null) {
            EventDispatcherUtils.dispatchOnInvisible(visibilityItem.getInvisibleHandler());
          }

          if (visibilityChangedHandler != null) {
            EventDispatcherUtils.dispatchOnVisibilityChanged(
                visibilityChangedHandler, 0, 0, 0f, 0f);
          }

          if (visibilityItem.isInFocusedRange()) {
            visibilityItem.setFocusedRange(false);
            if (visibilityItem.getUnfocusedHandler() != null) {
              EventDispatcherUtils.dispatchOnUnfocused(visibilityItem.getUnfocusedHandler());
            }
          }

          mVisibilityIdToItemMap.remove(visibilityOutputId);
          visibilityItem = null;
        } else {
          // Processed, do not clear.
          visibilityItem.setDoNotClearInThisPass(mIsDirty);
        }
      }

      if (isCurrentlyVisible) {
        // The component is visible now, but used to be outside the viewport.
        if (visibilityItem == null) {
          final String globalKey =
              visibilityOutput.getComponent() != null
                  ? visibilityOutput.getComponent().getGlobalKey()
                  : null;
          visibilityItem =
              new VisibilityItem(
                  globalKey, invisibleHandler, unfocusedHandler, visibilityChangedHandler);
          visibilityItem.setDoNotClearInThisPass(mIsDirty);
          mVisibilityIdToItemMap.put(visibilityOutputId, visibilityItem);

          if (visibleHandler != null) {
            EventDispatcherUtils.dispatchOnVisible(visibleHandler);
          }
        }

        // Check if the component has entered or exited the focused range.
        if (focusedHandler != null || unfocusedHandler != null) {
          if (isInFocusedRange(visibilityOutputBounds, sTempRect)) {
            if (!visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(true);
              if (focusedHandler != null) {
                EventDispatcherUtils.dispatchOnFocused(focusedHandler);
              }
            }
          } else {
            if (visibilityItem.isInFocusedRange()) {
              visibilityItem.setFocusedRange(false);
              if (unfocusedHandler != null) {
                EventDispatcherUtils.dispatchOnUnfocused(unfocusedHandler);
              }
            }
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

        if (visibilityChangedHandler != null) {
          final int visibleWidth = sTempRect.right - sTempRect.left;
          final int visibleHeight = sTempRect.bottom - sTempRect.top;
          EventDispatcherUtils.dispatchOnVisibilityChanged(
              visibilityChangedHandler,
              visibleWidth,
              visibleHeight,
              100f * visibleWidth / visibilityOutputBounds.width(),
              100f * visibleHeight / visibilityOutputBounds.height());
        }
      }
      if (isDoingPerfLog) {
        final String componentName =
            visibilityOutput.getComponent() != null
                ? visibilityOutput.getComponent().getSimpleName()
                : "Unknown";
        mMountStats.visibilityHandlerTimes.add((System.nanoTime() - handlerStartTime) / NS_IN_MS);
        mMountStats.visibilityHandlerNames.add(componentName);
      }
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    if (mIsDirty) {
      clearVisibilityItems();
    }

    if (isDoingPerfLog) {
      mMountStats.visibilityHandlersTotalTime = (System.nanoTime() - totalStartTime) / NS_IN_MS;
    }

    if (mountPerfEvent != null) {
      mountPerfEvent.markerPoint("VISIBILITY_HANDLERS_END");
    }
  }

  /** Clears and re-populates the test item map if we are in e2e test mode. */
  private void processTestOutputs(LayoutState layoutState) {
    if (mTestItemMap == null) {
      return;
    }

    mTestItemMap.clear();

    for (int i = 0, size = layoutState.getTestOutputCount(); i < size; i++) {
      final TestOutput testOutput = layoutState.getTestOutputAt(i);
      final long hostMarker = testOutput.getHostMarker();
      final long layoutOutputId = testOutput.getLayoutOutputId();
      final MountItem mountItem = layoutOutputId == -1 ? null : mIndexToItemMap.get(layoutOutputId);
      final TestItem testItem = new TestItem();
      testItem.setHost(hostMarker == -1 ? null : mHostsByMarker.get(hostMarker));
      testItem.setBounds(testOutput.getBounds());
      testItem.setTestKey(testOutput.getTestKey());
      testItem.setContent(mountItem == null ? null : mountItem.getContent());

      final Deque<TestItem> items = mTestItemMap.get(testOutput.getTestKey());
      final Deque<TestItem> updatedItems = items == null ? new LinkedList<TestItem>() : items;
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

  List<LithoView> getChildLithoViewsFromCurrentlyMountedItems() {
    final ArrayList<LithoView> childLithoViews = new ArrayList<>();
    for (int i = 0; i < mIndexToItemMap.size(); i++) {
      final long layoutOutputId = mIndexToItemMap.keyAt(i);
      final MountItem mountItem = mIndexToItemMap.get(layoutOutputId);
      if (mountItem != null && mountItem.getContent() instanceof HasLithoViewChildren) {
        ((HasLithoViewChildren) mountItem.getContent()).obtainLithoViewChildren(childLithoViews);
      }
    }
    return childLithoViews;
  }

  void clearVisibilityItems() {
    assertMainThread();
    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("MountState.clearVisibilityItems");
    }

    for (int i = mVisibilityIdToItemMap.size() - 1; i >= 0; i--) {
      final VisibilityItem visibilityItem = mVisibilityIdToItemMap.valueAt(i);
      if (visibilityItem.doNotClearInThisPass()) {
        // This visibility item has already been accounted for in this pass, so ignore it.
        visibilityItem.setDoNotClearInThisPass(false);
      } else {
        final EventHandler<InvisibleEvent> invisibleHandler = visibilityItem.getInvisibleHandler();
        final EventHandler<UnfocusedVisibleEvent> unfocusedHandler =
            visibilityItem.getUnfocusedHandler();
        final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
            visibilityItem.getVisibilityChangedHandler();

        if (invisibleHandler != null) {
          EventDispatcherUtils.dispatchOnInvisible(invisibleHandler);
        }

        if (visibilityItem.isInFocusedRange()) {
          visibilityItem.setFocusedRange(false);
          if (unfocusedHandler != null) {
            EventDispatcherUtils.dispatchOnUnfocused(unfocusedHandler);
          }
        }

        if (visibilityChangedHandler != null) {
          EventDispatcherUtils.dispatchOnVisibilityChanged(visibilityChangedHandler, 0, 0, 0f, 0f);
        }

        mVisibilityIdToItemMap.removeAt(i);
      }
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  private void registerHost(long id, ComponentHost host) {
    host.suppressInvalidations(true);
    mHostsByMarker.put(id, host);
  }

  private boolean isInVisibleRange(
      VisibilityOutput visibilityOutput, Rect bounds, Rect visibleBounds) {
    float heightRatio = visibilityOutput.getVisibleHeightRatio();
    float widthRatio = visibilityOutput.getVisibleWidthRatio();

    if (heightRatio == 0 && widthRatio == 0) {
      return true;
    }

    return isInRatioRange(heightRatio, bounds.height(), visibleBounds.height())
        && isInRatioRange(widthRatio, bounds.width(), visibleBounds.width());
  }

  private static boolean isInRatioRange(float ratio, int length, int visiblelength) {
    return visiblelength >= ratio * length;
  }

  /** Returns true if the component is in the focused visible range. */
  private boolean isInFocusedRange(Rect componentBounds, Rect componentVisibleBounds) {
    final View parent = (View) mLithoView.getParent();
    if (parent == null) {
      return false;
    }

    final int halfViewportArea = parent.getWidth() * parent.getHeight() / 2;
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
      LayoutState layoutState,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput,
      int componentTreeId,
      int index) {
    final Component layoutOutputComponent = layoutOutput.getComponent();
    final Component itemComponent = currentMountItem.getComponent();
    if (layoutOutputComponent == null) {
      throw new RuntimeException("Trying to update a MountItem with a null Component.");
    }

    // 1. Check if the mount item generated from the old component should be updated.
    final boolean shouldUpdate =
        shouldUpdateMountItem(layoutOutput, currentMountItem, useUpdateValueFromLayoutOutput);

    final boolean shouldUpdateViewInfo =
        shouldUpdate || shouldUpdateViewInfo(layoutOutput, currentMountItem);

    // 2. Reset all the properties like click handler, content description and tags related to
    // this item if it needs to be updated. the update mount item will re-set the new ones.
    if (shouldUpdate) {
      // If we're remounting this ComponentHost for a new ComponentTree, remove all disappearing
      // mount content that was animating since those disappearing animations belong to the old
      // ComponentTree
      if (mLastMountedComponentTreeId != componentTreeId) {
        final Component component = currentMountItem.getComponent();

        if (isHostSpec(component)) {
          final ComponentHost componentHost = (ComponentHost) currentMountItem.getContent();
          removeDisappearingMountContentFromComponentHost(componentHost);
        }
      }

      maybeUnsetViewAttributes(currentMountItem);

      final ComponentHost host = currentMountItem.getHost();
      host.maybeUnregisterTouchExpansion(index, currentMountItem);
    } else if (shouldUpdateViewInfo) {
      maybeUnsetViewAttributes(currentMountItem);

      final ComponentHost host = currentMountItem.getHost();
      host.maybeUnregisterTouchExpansion(index, currentMountItem);
    }

    // 3. We will re-bind this later in 7 regardless so let's make sure it's currently unbound.
    if (currentMountItem.isBound()) {
      itemComponent.onUnbind(getContextForComponent(itemComponent), currentMountItem.getContent());
      currentMountItem.setIsBound(false);
    }

    // 4. Re initialize the MountItem internal state with the new attributes from LayoutOutput
    currentMountItem.update(layoutOutput);

    // 5. If the mount item is not valid for this component update its content and view attributes.
    if (shouldUpdate) {
      final ComponentHost host = currentMountItem.getHost();
      host.maybeRegisterTouchExpansion(index, currentMountItem);

      updateMountedContent(currentMountItem, layoutOutput, itemComponent);
      setViewAttributes(currentMountItem);
    } else if (shouldUpdateViewInfo) {
      final ComponentHost host = currentMountItem.getHost();
      host.maybeRegisterTouchExpansion(index, currentMountItem);

      setViewAttributes(currentMountItem);
    }

    final Object currentContent = currentMountItem.getContent();

    // 6. Set the mounted content on the Component and call the bind callback.
    bindComponentToContent(layoutOutputComponent, currentContent);
    currentMountItem.setIsBound(true);

    // 7. Update the bounds of the mounted content. This needs to be done regardless of whether
    // the component has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedLayoutOutput(layoutOutput, layoutState, currentMountItem);

    maybeInvalidateAccessibilityState(currentMountItem);
    if (currentMountItem.getContent() instanceof Drawable) {
      maybeSetDrawableState(
          currentMountItem.getHost(),
          (Drawable) currentMountItem.getContent(),
          currentMountItem.getLayoutFlags(),
          currentMountItem.getNodeInfo());
    }

    return shouldUpdate;
  }

  private static boolean shouldUpdateViewInfo(
      LayoutOutput layoutOutput, MountItem currentMountItem) {

    final ViewNodeInfo nextViewNodeInfo = layoutOutput.getViewNodeInfo();
    final ViewNodeInfo currentViewNodeInfo = currentMountItem.getViewNodeInfo();
    if ((currentViewNodeInfo == null && nextViewNodeInfo != null)
        || (currentViewNodeInfo != null && !currentViewNodeInfo.isEquivalentTo(nextViewNodeInfo))) {

      return true;
    }

    final NodeInfo nextNodeInfo = layoutOutput.getNodeInfo();
    final NodeInfo currentNodeInfo = currentMountItem.getNodeInfo();
    return (currentNodeInfo == null && nextNodeInfo != null)
        || (currentNodeInfo != null && !currentNodeInfo.isEquivalentTo(nextNodeInfo));
  }

  private static boolean shouldUpdateMountItem(
      LayoutOutput layoutOutput,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput) {
    @LayoutOutput.UpdateState final int updateState = layoutOutput.getUpdateState();
    final Component currentComponent = currentMountItem.getComponent();
    final Component nextComponent = layoutOutput.getComponent();

    // If the orientation has changed, we should definitely update.
    if (layoutOutput.getOrientation() != currentMountItem.getOrientation()) {
      return true;
    }

    // If the two components have different sizes and the mounted content depends on the size we
    // just return true immediately.
    if (!sameSize(layoutOutput, currentMountItem) && nextComponent.isMountSizeDependent()) {
      return true;
    }

    if (useUpdateValueFromLayoutOutput) {
      if (updateState == LayoutOutput.STATE_UPDATED) {

        // Check for incompatible ReferenceLifecycle.
        return currentComponent instanceof DrawableComponent
            && nextComponent instanceof DrawableComponent
            && currentComponent.shouldComponentUpdate(currentComponent, nextComponent);
      } else if (updateState == LayoutOutput.STATE_DIRTY) {
        return true;
      }
    }

    if (!currentComponent.callsShouldUpdateOnMount()) {
      return true;
    }

    return currentComponent.shouldComponentUpdate(currentComponent, nextComponent);
  }

  private static boolean sameSize(LayoutOutput layoutOutput, MountItem item) {
    final Rect layoutOutputBounds = layoutOutput.getBounds();
    final Object mountedContent = item.getContent();

    return layoutOutputBounds.width() == getWidthForMountedContent(mountedContent)
        && layoutOutputBounds.height() == getHeightForMountedContent(mountedContent);
  }

  private static int getWidthForMountedContent(Object content) {
    return content instanceof Drawable
        ? ((Drawable) content).getBounds().width()
        : ((View) content).getWidth();
  }

  private static int getHeightForMountedContent(Object content) {
    return content instanceof Drawable
        ? ((Drawable) content).getBounds().height()
        : ((View) content).getHeight();
  }

  private static void updateBoundsForMountedLayoutOutput(
      LayoutOutput layoutOutput, LayoutState layoutState, MountItem item) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (layoutOutput.getId() == ROOT_HOST_ID) {
      return;
    }

    getActualBounds(layoutOutput, layoutState, sTempRect);

    final boolean forceTraversal =
        Component.isMountViewSpec(layoutOutput.getComponent())
            && ((View) item.getContent()).isLayoutRequested();

    applyBoundsToMountContent(
        item.getContent(),
        sTempRect.left,
        sTempRect.top,
        sTempRect.right,
        sTempRect.bottom,
        forceTraversal /* force */);
  }

  /** Prepare the {@link MountState} to mount a new {@link LayoutState}. */
  @SuppressWarnings("unchecked")
  private void prepareMount(LayoutState layoutState, @Nullable PerfEvent perfEvent) {
    final List<Integer> disappearingItems = extractDisappearingItems(layoutState);
    final PrepareMountStats stats = unmountOrMoveOldItems(layoutState, disappearingItems);

    if (perfEvent != null) {
      perfEvent.markerAnnotate(PARAM_UNMOUNTED_COUNT, stats.unmountedCount);
      perfEvent.markerAnnotate(PARAM_MOVED_COUNT, stats.movedCount);
      perfEvent.markerAnnotate(PARAM_UNCHANGED_COUNT, stats.unchangedCount);
    }

    if (mHostsByMarker.get(ROOT_HOST_ID) == null) {
      // Mounting always starts with the root host.
      registerHost(ROOT_HOST_ID, mLithoView);

      // Root host is implicitly marked as mounted.
      mIndexToItemMap.put(ROOT_HOST_ID, mRootHostMountItem);
    }

    final int outputCount = layoutState.getMountableOutputCount();
    if (mLayoutOutputsIds == null || outputCount != mLayoutOutputsIds.length) {
      mLayoutOutputsIds = new long[outputCount];
    }

    for (int i = 0; i < outputCount; i++) {
      mLayoutOutputsIds[i] = layoutState.getMountableOutputAt(i).getId();
    }
  }

  /** Determine whether to apply disappear animation to the given {@link MountItem} */
  private boolean isItemDisappearing(LayoutState layoutState, int index) {
    if (!shouldAnimateTransitions(layoutState) || !hasTransitionsToAnimate()) {
      return false;
    }

    if (mTransitionManager == null || mLastMountedLayoutState == null) {
      return false;
    }

    final LayoutOutput layoutOutput = mLastMountedLayoutState.getMountableOutputAt(index);
    final TransitionId transitionId = layoutOutput.getTransitionId();
    if (transitionId == null) {
      return false;
    }

    return mTransitionManager.isDisappearing(transitionId);
  }

  /**
   * Takes care of disappearing items from the last mounted layout (re-mounts them to the root if
   * needed, starts disappearing, removes them from mapping). Returns the list of ids, which for
   * every disappearing subtree contains a pair [index of the root of the subtree, index of the last
   * descendant of that subtree]
   */
  private List<Integer> extractDisappearingItems(LayoutState newLayoutState) {
    if (mLayoutOutputsIds == null) {
      return Collections.emptyList();
    }

    List<Integer> indices = null;
    int index = 0;
    while (index < mLayoutOutputsIds.length) {
      if (isItemDisappearing(newLayoutState, index)) {
        final int lastDescendantIndex = findLastDescendantIndex(mLastMountedLayoutState, index);

        // Go though disappearing subtree, mount everything that is not mounted yet
        // That's okay to mount here *before* we call unmountOrMoveOldItems() and only passing
        // last mount LayoutState
        // This item representing the root of the disappearing subtree will be unmounted immediately
        // after this cycle is over and will be moved to the root. If any if its parents have been
        // mounted as well, they will get picked up in unmountOrMoveOldItems()
        for (int j = index; j <= lastDescendantIndex; j++) {
          final MountItem mountedItem = getItemAt(j);
          if (mountedItem != null) {
            // Item is already mounted - skip
            continue;
          }
          final LayoutOutput layoutOutput = mLastMountedLayoutState.getMountableOutputAt(j);
          mountLayoutOutput(j, layoutOutput, mLastMountedLayoutState);
        }

        // Reference to the root of the disappearing subtree
        final MountItem disappearingItem = getItemAt(index);

        // Moving item to the root
        remountComponentHostToRootIfNeeded(index);

        // Removing references of all the items of the disappearing subtree from mIndexToItemMap and
        // mHostsByMaker
        removeDisappearingItemMappings(index, lastDescendantIndex);

        // Start animating disappearing
        startUnmountDisappearingItem(disappearingItem, index);

        if (indices == null) {
          indices = new ArrayList<>(2);
        }
        indices.add(index);
        indices.add(lastDescendantIndex);

        index = lastDescendantIndex + 1;
      } else {
        index++;
      }
    }
    return indices != null ? indices : Collections.<Integer>emptyList();
  }

  private void remountComponentHostToRootIfNeeded(int index) {
    final ComponentHost rootHost = mHostsByMarker.get(ROOT_HOST_ID);
    final MountItem item = getItemAt(index);
    if (item.getHost() == rootHost) {
      // Already mounted to the root
      return;
    }

    final Object content = item.getContent();

    // Before unmounting item get its position inside the root
    int left = 0;
    int top = 0;
    int right;
    int bottom;
    // Get left/top position of the item's host first
    ComponentHost componentHost = item.getHost();
    while (componentHost != rootHost) {
      left += componentHost.getLeft();
      top += componentHost.getTop();
      componentHost = (ComponentHost) componentHost.getParent();
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
    item.getHost().unmount(index, item);

    // Apply new bounds to the content as it will be mounted in the root now
    applyBoundsToMountContent(content, left, top, right, bottom, false);

    // Mount to the root
    rootHost.mount(index, item, sTempRect);

    // Set new host to the MountItem
    item.setHost(rootHost);
  }

  private void removeDisappearingItemMappings(int fromIndex, int toIndex) {
    mLastDisappearRangeStart = fromIndex;
    mLastDisappearRangeEnd = toIndex;

    for (int i = fromIndex; i <= toIndex; i++) {
      final MountItem item = getItemAt(i);

      // We do not need this mapping for disappearing items.
      mIndexToItemMap.remove(mLayoutOutputsIds[i]);

      if (item.getComponent() != null && item.getComponent().hasChildLithoViews()) {
        mCanMountIncrementallyMountItems.remove(mLayoutOutputsIds[i]);
      }

      // Likewise we no longer need host mapping for disappearing items.
      if (isHostSpec(item.getComponent())) {
        mHostsByMarker.removeAt(mHostsByMarker.indexOfValue((ComponentHost) item.getContent()));
      }
    }
  }

  private void startUnmountDisappearingItem(MountItem item, int index) {
    final TransitionId transitionId = item.getTransitionId();
    OutputUnitsAffinityGroup<MountItem> disappearingGroup =
        mDisappearingMountItems.get(transitionId);
    if (disappearingGroup == null) {
      disappearingGroup = new OutputUnitsAffinityGroup<>();
      mDisappearingMountItems.put(transitionId, disappearingGroup);
    }
    final @OutputUnitType int type =
        LayoutStateOutputIdCalculator.getTypeFromId(mLayoutOutputsIds[index]);
    disappearingGroup.add(type, item);

    final ComponentHost host = item.getHost();
    host.startUnmountDisappearingItem(index, item);
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private PrepareMountStats unmountOrMoveOldItems(
      LayoutState newLayoutState, List<Integer> disappearingItems) {
    mPrepareMountStats.reset();

    if (mLayoutOutputsIds == null) {
      return mPrepareMountStats;
    }

    int disappearingItemsPointer = 0;

    // Traversing from the beginning since mLayoutOutputsIds unmounting won't remove entries there
    // but only from mIndexToItemMap. If an host changes we're going to unmount it and recursively
    // all its mounted children.
    for (int i = 0; i < mLayoutOutputsIds.length; i++) {
      final LayoutOutput newLayoutOutput = newLayoutState.getLayoutOutput(mLayoutOutputsIds[i]);
      final int newPosition = newLayoutOutput == null ? -1 : newLayoutOutput.getIndex();

      final MountItem oldItem = getItemAt(i);

      // Just skip disappearing items here
      if (disappearingItems.size() > disappearingItemsPointer
          && disappearingItems.get(disappearingItemsPointer) == i) {
        // Updating i to the index of the last member of the disappearing subtree, so the whole
        // subtree will be skipped, as it's been dealt with at extractDisappearingItems()
        i = disappearingItems.get(disappearingItemsPointer + 1);
        disappearingItemsPointer += 2;
        continue;
      }

      if (newPosition == -1) {
        unmountItem(i, mHostsByMarker);
        mPrepareMountStats.unmountedCount++;
      } else {
        final long newHostMarker = newLayoutOutput.getHostMarker();

        if (oldItem == null) {
          // This was previously unmounted.
          mPrepareMountStats.unmountedCount++;
        } else if (oldItem.getHost() != mHostsByMarker.get(newHostMarker)) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItem(i, mHostsByMarker);
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

    return mPrepareMountStats;
  }

  private void updateMountedContent(
      MountItem item, LayoutOutput layoutOutput, Component previousComponent) {
    final Component newComponent = layoutOutput.getComponent();
    if (isHostSpec(newComponent)) {
      return;
    }

    final Object previousContent = item.getContent();

    // Call unmount and mount in sequence to make sure all the the resources are correctly
    // de-allocated. It's possible for previousContent to equal null - when the root is
    // interactive we create a LayoutOutput without content in order to set up click handling.
    previousComponent.unmount(getContextForComponent(previousComponent), previousContent);
    newComponent.mount(getContextForComponent(newComponent), previousContent);
  }

  private void mountLayoutOutput(int index, LayoutOutput layoutOutput, LayoutState layoutState) {
    // 1. Resolve the correct host to mount our content to.
    final long startTime = System.nanoTime();
    final long hostMarker = layoutOutput.getHostMarker();
    ComponentHost host = mHostsByMarker.get(hostMarker);

    if (host == null) {
      // Host has not yet been mounted - mount it now.
      final int hostMountIndex = layoutState.getLayoutOutputPositionForId(hostMarker);
      final LayoutOutput hostLayoutOutput = layoutState.getMountableOutputAt(hostMountIndex);
      mountLayoutOutput(hostMountIndex, hostLayoutOutput, layoutState);

      host = mHostsByMarker.get(hostMarker);
    }

    // 2. Generate the component's mount state (this might also be a ComponentHost View).
    final Component component = layoutOutput.getComponent();
    if (component == null) {
      throw new RuntimeException("Trying to mount a LayoutOutput with a null Component.");
    }
    final Object content =
        ComponentsPools.acquireMountContent(mContext.getAndroidContext(), component);

    final ComponentContext context = getContextForComponent(component);
    component.mount(context, content);

    // 3. If it's a ComponentHost, add the mounted View to the list of Hosts.
    if (isHostSpec(component)) {
      ComponentHost componentHost = (ComponentHost) content;
      componentHost.setParentHostMarker(hostMarker);
      registerHost(layoutOutput.getId(), componentHost);
    }

    // 4. Mount the content into the selected host.
    final MountItem item = mountContent(index, component, content, host, layoutOutput);

    // 5. Notify the component that mounting has completed
    bindComponentToContent(component, content);
    item.setIsBound(true);

    // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    getActualBounds(layoutOutput, layoutState, sTempRect);
    applyBoundsToMountContent(
        item.getContent(),
        sTempRect.left,
        sTempRect.top,
        sTempRect.right,
        sTempRect.bottom,
        true /* force */);

    // 6. Update the mount stats
    if (mMountStats.isLoggingEnabled) {
      mMountStats.mountTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
      mMountStats.mountedNames.add(component.getSimpleName());
      mMountStats.mountedCount++;
      mMountStats.extras.add(
          LogTreePopulator.getAnnotationBundleFromLogger(component, context.getLogger()));
    }
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first LayoutOutput).
  private MountItem mountContent(
      int index,
      Component component,
      Object content,
      ComponentHost host,
      LayoutOutput layoutOutput) {

    final MountItem item = new MountItem(component, host, content, layoutOutput);

    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIndexToItemMap.put(mLayoutOutputsIds[index], item);

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.put(mLayoutOutputsIds[index], item);
    }

    layoutOutput.getMountBounds(sTempRect);

    host.mount(index, item, sTempRect);

    setViewAttributes(item);

    return item;
  }

  private static void applyBoundsToMountContent(
      Object content, int left, int top, int right, int bottom, boolean force) {
    assertMainThread();

    if (content instanceof View) {
      BoundsHelper.applyBoundsToView((View) content, left, top, right, bottom, force);
    } else if (content instanceof Drawable) {
      ((Drawable) content).setBounds(left, top, right, bottom);
    } else {
      throw new IllegalStateException("Unsupported mounted content " + content);
    }
  }

  /** @return bounds for a given LayoutOutput within its actual host, {@see getActualHostMarker} */
  private static void getActualBounds(
      LayoutOutput layoutOutput, LayoutState layoutState, Rect outRect) {
    final long actualHostMarker = layoutOutput.getHostMarker();
    layoutOutput.getMountBounds(outRect);

    long hostMarker = layoutOutput.getHostMarker();
    while (hostMarker != actualHostMarker) {
      final LayoutOutput ancestor = layoutState.getLayoutOutput(hostMarker);
      ancestor.getMountBounds(sTempRect2);
      outRect.offset(sTempRect2.left, sTempRect2.top);
      hostMarker = ancestor.getHostMarker();
    }
  }

  private static void setViewAttributes(MountItem item) {
    final Component component = item.getComponent();
    if (!isMountViewSpec(component)) {
      return;
    }

    final View view = (View) item.getContent();
    final NodeInfo nodeInfo = item.getNodeInfo();

    if (nodeInfo != null) {
      setClickHandler(nodeInfo.getClickHandler(), view);
      setLongClickHandler(nodeInfo.getLongClickHandler(), view);
      setFocusChangeHandler(nodeInfo.getFocusChangeHandler(), view);
      setTouchHandler(nodeInfo.getTouchHandler(), view);
      setInterceptTouchHandler(nodeInfo.getInterceptTouchHandler(), view);

      setAccessibilityDelegate(view, nodeInfo);

      setViewTag(view, nodeInfo.getViewTag());
      setViewTags(view, nodeInfo.getViewTags());

      setShadowElevation(view, nodeInfo.getShadowElevation());
      setOutlineProvider(view, nodeInfo.getOutlineProvider());
      setClipToOutline(view, nodeInfo.getClipToOutline());
      setClipChildren(view, nodeInfo);

      setContentDescription(view, nodeInfo.getContentDescription());

      setFocusable(view, nodeInfo.getFocusState());
      setClickable(view, nodeInfo.getClickableState());
      setEnabled(view, nodeInfo.getEnabledState());
      setSelected(view, nodeInfo.getSelectedState());
      setScale(view, nodeInfo);
      setAlpha(view, nodeInfo);
      setRotation(view, nodeInfo);
      setRotationX(view, nodeInfo);
      setRotationY(view, nodeInfo);
    }

    setImportantForAccessibility(view, item.getImportantForAccessibility());

    final ViewNodeInfo viewNodeInfo = item.getViewNodeInfo();
    if (viewNodeInfo != null) {
      setViewStateListAnimator(view, viewNodeInfo);
      if (!isHostSpec(component)) {
        // Set view background, if applicable.  Do this before padding
        // as it otherwise overrides the padding.
        setViewBackground(view, viewNodeInfo);

        setViewPadding(view, viewNodeInfo);

        setViewForeground(view, viewNodeInfo);

        setViewLayoutDirection(view, viewNodeInfo);
      }
    }
  }

  private static void maybeUnsetViewAttributes(MountItem item) {
    final Component component = item.getComponent();
    if (!isMountViewSpec(component)) {
      return;
    }

    unsetViewAttributes(item, isHostSpec(component));
  }

  private static void unsetViewAttributes(MountItem item, boolean isHostView) {
    final View view = (View) item.getContent();
    final NodeInfo nodeInfo = item.getNodeInfo();

    if (nodeInfo != null) {
      if (nodeInfo.getClickHandler() != null) {
        unsetClickHandler(view);
      }

      if (nodeInfo.getLongClickHandler() != null) {
        unsetLongClickHandler(view);
      }

      if (nodeInfo.getFocusChangeHandler() != null) {
        unsetFocusChangeHandler(view);
      }

      if (nodeInfo.getTouchHandler() != null) {
        unsetTouchHandler(view);
      }

      if (nodeInfo.getInterceptTouchHandler() != null) {
        unsetInterceptTouchEventHandler(view);
      }

      unsetViewTag(view);
      unsetViewTags(view, nodeInfo.getViewTags());

      unsetShadowElevation(view, nodeInfo.getShadowElevation());
      unsetOutlineProvider(view, nodeInfo.getOutlineProvider());
      unsetClipToOutline(view, nodeInfo.getClipToOutline());
      unsetClipChildren(view, nodeInfo.getClipChildren());

      if (!TextUtils.isEmpty(nodeInfo.getContentDescription())) {
        unsetContentDescription(view);
      }

      unsetScale(view, nodeInfo);
      unsetAlpha(view, nodeInfo);
      unsetRotation(view, nodeInfo);
      unsetRotationX(view, nodeInfo);
      unsetRotationY(view, nodeInfo);
    }

    view.setClickable(item.isViewClickable());
    view.setLongClickable(item.isViewLongClickable());

    unsetFocusable(view, item);
    unsetEnabled(view, item);
    unsetSelected(view, item);

    if (item.getImportantForAccessibility() != IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      unsetImportantForAccessibility(view);
    }

    unsetAccessibilityDelegate(view);

    final ViewNodeInfo viewNodeInfo = item.getViewNodeInfo();
    if (viewNodeInfo != null) {
      unsetViewStateListAnimator(view, viewNodeInfo);
      // Host view doesn't set its own padding, but gets absolute positions for inner content from
      // Yoga. Also bg/fg is used as separate drawables instead of using View's bg/fg attribute.
      if (!isHostView) {
        unsetViewPadding(view, viewNodeInfo);
        unsetViewBackground(view, viewNodeInfo);
        unsetViewForeground(view, viewNodeInfo);
        unsetViewLayoutDirection(view);
      }
    }
  }

  /**
   * Store a {@link NodeInfo} as a tag in {@code view}. {@link LithoView} contains the logic for
   * setting/unsetting it whenever accessibility is enabled/disabled
   *
   * <p>For non {@link ComponentHost}s this is only done if any {@link EventHandler}s for
   * accessibility events have been implemented, we want to preserve the original behaviour since
   * {@code view} might have had a default delegate.
   */
  private static void setAccessibilityDelegate(View view, NodeInfo nodeInfo) {
    if (!(view instanceof ComponentHost) && !nodeInfo.needsAccessibilityDelegate()) {
      return;
    }

    view.setTag(R.id.component_node_info, nodeInfo);
  }

  private static void unsetAccessibilityDelegate(View view) {
    if (!(view instanceof ComponentHost) && view.getTag(R.id.component_node_info) == null) {
      return;
    }
    view.setTag(R.id.component_node_info, null);
    if (!(view instanceof ComponentHost)) {
      ViewCompat.setAccessibilityDelegate(view, null);
    }
  }

  /**
   * Installs the click listeners that will dispatch the click handler defined in the component's
   * props. Unconditionally set the clickable flag on the view.
   */
  private static void setClickHandler(EventHandler<ClickEvent> clickHandler, View view) {
    if (clickHandler == null) {
      return;
    }

    ComponentClickListener listener = getComponentClickListener(view);

    if (listener == null) {
      listener = new ComponentClickListener();
      setComponentClickListener(view, listener);
    }

    listener.setEventHandler(clickHandler);
    view.setClickable(true);
  }

  private static void unsetClickHandler(View view) {
    final ComponentClickListener listener = getComponentClickListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  static ComponentClickListener getComponentClickListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentClickListener();
    } else {
      return (ComponentClickListener) v.getTag(R.id.component_click_listener);
    }
  }

  static void setComponentClickListener(View v, ComponentClickListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentClickListener(listener);
    } else {
      v.setOnClickListener(listener);
      v.setTag(R.id.component_click_listener, listener);
    }
  }

  /**
   * Installs the long click listeners that will dispatch the click handler defined in the
   * component's props. Unconditionally set the clickable flag on the view.
   */
  private static void setLongClickHandler(
      EventHandler<LongClickEvent> longClickHandler, View view) {
    if (longClickHandler != null) {
      ComponentLongClickListener listener = getComponentLongClickListener(view);

      if (listener == null) {
        listener = new ComponentLongClickListener();
        setComponentLongClickListener(view, listener);
      }

      listener.setEventHandler(longClickHandler);

      view.setLongClickable(true);
    }
  }

  private static void unsetLongClickHandler(View view) {
    final ComponentLongClickListener listener = getComponentLongClickListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  static ComponentLongClickListener getComponentLongClickListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentLongClickListener();
    } else {
      return (ComponentLongClickListener) v.getTag(R.id.component_long_click_listener);
    }
  }

  static void setComponentLongClickListener(View v, ComponentLongClickListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentLongClickListener(listener);
    } else {
      v.setOnLongClickListener(listener);
      v.setTag(R.id.component_long_click_listener, listener);
    }
  }

  /**
   * Installs the on focus change listeners that will dispatch the click handler defined in the
   * component's props. Unconditionally set the clickable flag on the view.
   */
  private static void setFocusChangeHandler(
      EventHandler<FocusChangedEvent> focusChangeHandler, View view) {
    if (focusChangeHandler == null) {
      return;
    }

    ComponentFocusChangeListener listener = getComponentFocusChangeListener(view);

    if (listener == null) {
      listener = new ComponentFocusChangeListener();
      setComponentFocusChangeListener(view, listener);
    }

    listener.setEventHandler(focusChangeHandler);
  }

  private static void unsetFocusChangeHandler(View view) {
    final ComponentFocusChangeListener listener = getComponentFocusChangeListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  static ComponentFocusChangeListener getComponentFocusChangeListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentFocusChangeListener();
    } else {
      return (ComponentFocusChangeListener) v.getTag(R.id.component_focus_change_listener);
    }
  }

  static void setComponentFocusChangeListener(View v, ComponentFocusChangeListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentFocusChangeListener(listener);
    } else {
      v.setOnFocusChangeListener(listener);
      v.setTag(R.id.component_focus_change_listener, listener);
    }
  }

  /**
   * Installs the touch listeners that will dispatch the touch handler defined in the component's
   * props.
   */
  private static void setTouchHandler(EventHandler<TouchEvent> touchHandler, View view) {
    if (touchHandler != null) {
      ComponentTouchListener listener = getComponentTouchListener(view);

      if (listener == null) {
        listener = new ComponentTouchListener();
        setComponentTouchListener(view, listener);
      }

      listener.setEventHandler(touchHandler);
    }
  }

  private static void unsetTouchHandler(View view) {
    final ComponentTouchListener listener = getComponentTouchListener(view);

    if (listener != null) {
      listener.setEventHandler(null);
    }
  }

  /** Sets the intercept touch handler defined in the component's props. */
  private static void setInterceptTouchHandler(
      EventHandler<InterceptTouchEvent> interceptTouchHandler, View view) {
    if (interceptTouchHandler == null) {
      return;
    }

    if (view instanceof ComponentHost) {
      ((ComponentHost) view).setInterceptTouchEventHandler(interceptTouchHandler);
    }
  }

  private static void unsetInterceptTouchEventHandler(View view) {
    if (view instanceof ComponentHost) {
      ((ComponentHost) view).setInterceptTouchEventHandler(null);
    }
  }

  static ComponentTouchListener getComponentTouchListener(View v) {
    if (v instanceof ComponentHost) {
      return ((ComponentHost) v).getComponentTouchListener();
    } else {
      return (ComponentTouchListener) v.getTag(R.id.component_touch_listener);
    }
  }

  static void setComponentTouchListener(View v, ComponentTouchListener listener) {
    if (v instanceof ComponentHost) {
      ((ComponentHost) v).setComponentTouchListener(listener);
    } else {
      v.setOnTouchListener(listener);
      v.setTag(R.id.component_touch_listener, listener);
    }
  }

  private static void setViewTag(View view, Object viewTag) {
    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTag(viewTag);
    } else {
      view.setTag(viewTag);
    }
  }

  private static void setViewTags(View view, SparseArray<Object> viewTags) {
    if (viewTags == null) {
      return;
    }

    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTags(viewTags);
    } else {
      for (int i = 0, size = viewTags.size(); i < size; i++) {
        view.setTag(viewTags.keyAt(i), viewTags.valueAt(i));
      }
    }
  }

  private static void unsetViewTag(View view) {
    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTag(null);
    } else {
      view.setTag(null);
    }
  }

  private static void unsetViewTags(View view, SparseArray<Object> viewTags) {
    if (view instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) view;
      host.setViewTags(null);
    } else {
      if (viewTags != null) {
        for (int i = 0, size = viewTags.size(); i < size; i++) {
          view.setTag(viewTags.keyAt(i), null);
        }
      }
    }
  }

  private static void setShadowElevation(View view, float shadowElevation) {
    if (shadowElevation != 0) {
      ViewCompat.setElevation(view, shadowElevation);
    }
  }

  private static void unsetShadowElevation(View view, float shadowElevation) {
    if (shadowElevation != 0) {
      ViewCompat.setElevation(view, 0);
    }
  }

  private static void setOutlineProvider(View view, ViewOutlineProvider outlineProvider) {
    if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setOutlineProvider(outlineProvider);
    }
  }

  private static void unsetOutlineProvider(View view, ViewOutlineProvider outlineProvider) {
    if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
    }
  }

  private static void setClipToOutline(View view, boolean clipToOutline) {
    if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setClipToOutline(clipToOutline);
    }
  }

  private static void unsetClipToOutline(View view, boolean clipToOutline) {
    if (clipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setClipToOutline(false);
    }
  }

  private static void setClipChildren(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isClipChildrenSet() && view instanceof ViewGroup) {
      ((ViewGroup) view).setClipChildren(nodeInfo.getClipChildren());
    }
  }

  private static void unsetClipChildren(View view, boolean clipChildren) {
    if (!clipChildren && view instanceof ViewGroup) {
      // Default value for clipChildren is 'true'.
      // If this ViewGroup had clipChildren set to 'false' before mounting we would reset this
      // property here on recycling.
      ((ViewGroup) view).setClipChildren(true);
    }
  }

  private static void setContentDescription(View view, CharSequence contentDescription) {
    if (TextUtils.isEmpty(contentDescription)) {
      return;
    }

    view.setContentDescription(contentDescription);
  }

  private static void unsetContentDescription(View view) {
    view.setContentDescription(null);
  }

  private static void setImportantForAccessibility(View view, int importantForAccessibility) {
    if (importantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      return;
    }

    ViewCompat.setImportantForAccessibility(view, importantForAccessibility);
  }

  private static void unsetImportantForAccessibility(View view) {
    ViewCompat.setImportantForAccessibility(view, IMPORTANT_FOR_ACCESSIBILITY_AUTO);
  }

  private static void setFocusable(View view, @NodeInfo.FocusState int focusState) {
    if (focusState == NodeInfo.FOCUS_SET_TRUE) {
      view.setFocusable(true);
    } else if (focusState == NodeInfo.FOCUS_SET_FALSE) {
      view.setFocusable(false);
    }
  }

  private static void unsetFocusable(View view, MountItem mountItem) {
    view.setFocusable(mountItem.isViewFocusable());
  }

  private static void setClickable(View view, @NodeInfo.FocusState int clickableState) {
    if (clickableState == NodeInfo.CLICKABLE_SET_TRUE) {
      view.setClickable(true);
    } else if (clickableState == NodeInfo.CLICKABLE_SET_FALSE) {
      view.setClickable(false);
    }
  }

  private static void setEnabled(View view, @NodeInfo.EnabledState int enabledState) {
    if (enabledState == NodeInfo.ENABLED_SET_TRUE) {
      view.setEnabled(true);
    } else if (enabledState == NodeInfo.ENABLED_SET_FALSE) {
      view.setEnabled(false);
    }
  }

  private static void unsetEnabled(View view, MountItem mountItem) {
    view.setEnabled(mountItem.isViewEnabled());
  }

  private static void setSelected(View view, @NodeInfo.SelectedState int selectedState) {
    if (selectedState == NodeInfo.SELECTED_SET_TRUE) {
      view.setSelected(true);
    } else if (selectedState == NodeInfo.SELECTED_SET_FALSE) {
      view.setSelected(false);
    }
  }

  private static void unsetSelected(View view, MountItem mountItem) {
    view.setSelected(mountItem.isViewSelected());
  }

  private static void setScale(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isScaleSet()) {
        final float scale = nodeInfo.getScale();
        view.setScaleX(scale);
        view.setScaleY(scale);
      }
    }
  }

  private static void unsetScale(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isScaleSet()) {
        if (view.getScaleX() != 1) {
          view.setScaleX(1);
        }
        if (view.getScaleY() != 1) {
          view.setScaleY(1);
        }
      }
    }
  }

  private static void setAlpha(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isAlphaSet()) {
        view.setAlpha(nodeInfo.getAlpha());
      }
    }
  }

  private static void unsetAlpha(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isAlphaSet() && view.getAlpha() != 1) {
        view.setAlpha(1);
      }
    }
  }

  private static void setRotation(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationSet()) {
        view.setRotation(nodeInfo.getRotation());
      }
    }
  }

  private static void unsetRotation(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationSet() && view.getRotation() != 0) {
        view.setRotation(0);
      }
    }
  }

  private static void setRotationX(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationXSet()) {
        view.setRotationX(nodeInfo.getRotationX());
      }
    }
  }

  private static void unsetRotationX(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationXSet() && view.getRotationX() != 0) {
        view.setRotationX(0);
      }
    }
  }

  private static void setRotationY(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationYSet()) {
        view.setRotationY(nodeInfo.getRotationY());
      }
    }
  }

  private static void unsetRotationY(View view, NodeInfo nodeInfo) {
    if (Build.VERSION.SDK_INT >= 11) {
      if (nodeInfo.isRotationYSet() && view.getRotationY() != 0) {
        view.setRotationY(0);
      }
    }
  }

  private static void setViewPadding(View view, ViewNodeInfo viewNodeInfo) {
    if (!viewNodeInfo.hasPadding()) {
      return;
    }

    view.setPadding(
        viewNodeInfo.getPaddingLeft(),
        viewNodeInfo.getPaddingTop(),
        viewNodeInfo.getPaddingRight(),
        viewNodeInfo.getPaddingBottom());
  }

  private static void unsetViewPadding(View view, ViewNodeInfo viewNodeInfo) {
    if (!viewNodeInfo.hasPadding()) {
      return;
    }

    view.setPadding(0, 0, 0, 0);
  }

  private static void setViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final ComparableDrawable background = viewNodeInfo.getBackground();
    if (background != null) {
      setBackgroundCompat(view, background);
    }
  }

  private static void unsetViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final ComparableDrawable background = viewNodeInfo.getBackground();
    if (background != null) {
      setBackgroundCompat(view, null);
    }
  }

  @SuppressWarnings("deprecation")
  private static void setBackgroundCompat(View view, Drawable drawable) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      view.setBackgroundDrawable(drawable);
    } else {
      view.setBackground(drawable);
    }
  }

  private static void setViewForeground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable foreground = viewNodeInfo.getForeground();
    if (foreground != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw new IllegalStateException(
            "MountState has a ViewNodeInfo with foreground however "
                + "the current Android version doesn't support foreground on Views");
      }

      view.setForeground(foreground);
    }
  }

  private static void unsetViewForeground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable foreground = viewNodeInfo.getForeground();
    if (foreground != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw new IllegalStateException(
            "MountState has a ViewNodeInfo with foreground however "
                + "the current Android version doesn't support foreground on Views");
      }

      view.setForeground(null);
    }
  }

  private static void setViewLayoutDirection(View view, ViewNodeInfo viewNodeInfo) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return;
    }

    final int viewLayoutDirection;
    switch (viewNodeInfo.getLayoutDirection()) {
      case LTR:
        viewLayoutDirection = View.LAYOUT_DIRECTION_LTR;
        break;
      case RTL:
        viewLayoutDirection = View.LAYOUT_DIRECTION_RTL;
        break;
      default:
        viewLayoutDirection = View.LAYOUT_DIRECTION_INHERIT;
    }

    view.setLayoutDirection(viewLayoutDirection);
  }

  private static void unsetViewLayoutDirection(View view) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return;
    }

    view.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
  }

  private static void setViewStateListAnimator(View view, ViewNodeInfo viewNodeInfo) {
    StateListAnimator stateListAnimator = viewNodeInfo.getStateListAnimator();
    final int stateListAnimatorRes = viewNodeInfo.getStateListAnimatorRes();
    if (stateListAnimator == null && stateListAnimatorRes == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewNodeInfo with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    if (stateListAnimator == null) {
      stateListAnimator =
          AnimatorInflater.loadStateListAnimator(view.getContext(), stateListAnimatorRes);
    }
    view.setStateListAnimator(stateListAnimator);
  }

  private static void unsetViewStateListAnimator(View view, ViewNodeInfo viewNodeInfo) {
    if (viewNodeInfo.getStateListAnimator() == null
        && viewNodeInfo.getStateListAnimatorRes() == 0) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException(
          "MountState has a ViewNodeInfo with stateListAnimator, "
              + "however the current Android version doesn't support stateListAnimator on Views");
    }
    view.setStateListAnimator(null);
  }

  private static void mountItemIncrementally(MountItem item, boolean processVisibilityOutputs) {
    final Component component = item.getComponent();

    if (!isMountViewSpec(component)) {
      return;
    }

    // We can't just use the bounds of the View since we need the bounds relative to the
    // hosting LithoView (which is what the localVisibleRect is measured relative to).
    final View view = (View) item.getContent();

    mountViewIncrementally(view, processVisibilityOutputs);
  }

  private static void mountViewIncrementally(View view, boolean processVisibilityOutputs) {
    assertMainThread();

    if (view instanceof LithoView) {
      final LithoView lithoView = (LithoView) view;
      if (lithoView.isIncrementalMountEnabled()) {
        if (!processVisibilityOutputs) {
          lithoView.performIncrementalMount(
              new Rect(0, 0, view.getWidth(), view.getHeight()), false);
        } else {
          lithoView.performIncrementalMount();
        }
      }
    } else if (view instanceof ViewGroup) {
      final ViewGroup viewGroup = (ViewGroup) view;

      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        final View childView = viewGroup.getChildAt(i);
        mountViewIncrementally(childView, processVisibilityOutputs);
      }
    }
  }

  private void unmountDisappearingItemChild(ComponentContext context, MountItem item) {
    maybeRemoveAnimatingMountContent(item.getTransitionId());

    final Object content = item.getContent();

    // Recursively unmount mounted children items.
    if (content instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) content;

      for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
        final MountItem mountItem = host.getMountItemAt(i);
        unmountDisappearingItemChild(context, mountItem);
      }

      if (host.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    final ComponentHost host = item.getHost();
    host.unmount(item);

    maybeUnsetViewAttributes(item);

    unbindAndUnmountLifecycle(item);

    if (item.getComponent().hasChildLithoViews()) {
      final int index = mCanMountIncrementallyMountItems.indexOfValue(item);
      if (index > 0) {
        mCanMountIncrementallyMountItems.removeAt(index);
      }
    }
    assertNoDanglingMountContent(item);
    item.releaseMountContent(context.getAndroidContext());
  }

  private void assertNoDanglingMountContent(MountItem item) {
    final int index = mIndexToItemMap.indexOfValue(item);
    if (index > -1) {
      final long id = mIndexToItemMap.keyAt(index);
      int layoutOutputIndex = -1;
      for (int i = 0; i < mLayoutOutputsIds.length; i++) {
        if (id == mLayoutOutputsIds[i]) {
          layoutOutputIndex = i;
          break;
        }
      }
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.ERROR,
          "Got dangling mount content during animation: index="
              + layoutOutputIndex
              + ", mapIndex="
              + index
              + ", id="
              + id
              + ", disappearRange=["
              + mLastDisappearRangeStart
              + ","
              + mLastDisappearRangeEnd
              + "], contentType="
              + item.getContent().getClass()
              + ", component="
              + (item.getComponent() != null ? item.getComponent().getSimpleName() : null)
              + ", transitionId="
              + item.getTransitionId()
              + ", host="
              + item.getHost()
              + ", isRootHost="
              + (mHostsByMarker.get(ROOT_HOST_ID) == item.getHost()));
    }
  }

  void unmountAllItems() {
    assertMainThread();
    if (mLayoutOutputsIds == null) {
      return;
    }
    for (int i = mLayoutOutputsIds.length - 1; i >= 0; i--) {
      unmountItem(i, mHostsByMarker);
    }
    mPreviousLocalVisibleRect.setEmpty();
    mNeedsRemount = true;
  }

  private void unmountItem(int index, LongSparseArray<ComponentHost> hostsByMarker) {
    final MountItem item = getItemAt(index);
    final long startTime = System.nanoTime();

    // Already has been unmounted.
    if (item == null) {
      return;
    }

    // The root host item should never be unmounted as it's a reference
    // to the top-level LithoView.
    if (mLayoutOutputsIds[index] == ROOT_HOST_ID) {
      unsetViewAttributes(item, true);
      return;
    }

    final Object content = item.getContent();

    // Recursively unmount mounted children items.
    // This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    // sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    // sub tree, that will still have mounted items. (Different sequence number on LayoutOutput id)
    if ((content instanceof ComponentHost) && !(content instanceof LithoView)) {
      final ComponentHost host = (ComponentHost) content;

      // Concurrently remove items therefore traverse backwards.
      for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
        final MountItem mountItem = host.getMountItemAt(i);
        final long layoutOutputId = mIndexToItemMap.keyAt(mIndexToItemMap.indexOfValue(mountItem));

        for (int mountIndex = mLayoutOutputsIds.length - 1; mountIndex >= 0; mountIndex--) {
          if (mLayoutOutputsIds[mountIndex] == layoutOutputId) {
            unmountItem(mountIndex, hostsByMarker);
            break;
          }
        }
      }

      if (host.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    /*
     * The mounted content might contain other LithoViews which are not reachable from
     * this MountState. If that content contains other LithoViews, we need to unmount them as well,
     * so that their contents are recycled and reused next time.
     */
    if (content instanceof HasLithoViewChildren) {
      final ArrayList<LithoView> lithoViews = new ArrayList<>();
      ((HasLithoViewChildren) content).obtainLithoViewChildren(lithoViews);

      for (int i = lithoViews.size() - 1; i >= 0; i--) {
        final LithoView lithoView = lithoViews.get(i);
        lithoView.unmountAllItems();
      }
    }

    final ComponentHost host = item.getHost();
    host.unmount(index, item);

    maybeUnsetViewAttributes(item);

    final Component component = item.getComponent();

    if (isHostSpec(component)) {
      final ComponentHost componentHost = (ComponentHost) content;
      hostsByMarker.removeAt(hostsByMarker.indexOfValue(componentHost));
      removeDisappearingMountContentFromComponentHost(componentHost);
    }

    unbindAndUnmountLifecycle(item);

    final long layoutOutputId = mLayoutOutputsIds[index];
    mIndexToItemMap.remove(layoutOutputId);
    if (item.hasTransitionId()) {
      final @OutputUnitType int type = LayoutStateOutputIdCalculator.getTypeFromId(layoutOutputId);
      maybeRemoveAnimatingMountContent(item.getTransitionId(), type);
    }

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.delete(mLayoutOutputsIds[index]);
    }

    item.releaseMountContent(mContext.getAndroidContext());

    if (mMountStats.isLoggingEnabled) {
      mMountStats.unmountedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
      mMountStats.unmountedNames.add(component.getSimpleName());
      mMountStats.unmountedCount++;
    }
  }

  private void unbindAndUnmountLifecycle(MountItem item) {
    final Component component = item.getComponent();
    final Object content = item.getContent();
    final ComponentContext context = getContextForComponent(component);

    // Call the component's unmount() method.
    if (item.isBound()) {
      component.onUnbind(context, content);
      item.setIsBound(false);
    }
    component.unmount(context, content);
  }

  private void endUnmountDisappearingItem(OutputUnitsAffinityGroup<MountItem> group) {
    maybeRemoveAnimatingMountContent(group.getMostSignificantUnit().getTransitionId());

    for (int i = 0, size = group.size(); i < size; i++) {
      final MountItem item = group.getAt(i);
      // We used to do (item.getContent() instanceof ComponentHost) check here, which didn't
      // take
      // into consideration MountSpecs that mount a LithoView which would pass the check while
      // shouldn't
      if (group.typeAt(i) == OutputUnitType.HOST) {
        final ComponentHost content = (ComponentHost) item.getContent();

        // Unmount descendant items in reverse order.
        for (int j = content.getMountItemCount() - 1; j >= 0; j--) {
          final MountItem mountItem = content.getMountItemAt(j);
          unmountDisappearingItemChild(mContext, mountItem);
        }

        if (content.getMountItemCount() > 0) {
          throw new IllegalStateException(
              "Recursively unmounting items from a ComponentHost, left"
                  + " some items behind maybe because not tracked by its MountState");
        }
      }

      final ComponentHost host = item.getHost();
      host.unmountDisappearingItem(item);
      maybeUnsetViewAttributes(item);

      unbindAndUnmountLifecycle(item);

      if (item.getComponent().hasChildLithoViews()) {
        final int index = mCanMountIncrementallyMountItems.indexOfValue(item);
        if (index > 0) {
          mCanMountIncrementallyMountItems.removeAt(index);
        }
      }
      assertNoDanglingMountContent(item);
      item.releaseMountContent(mContext.getAndroidContext());
    }
  }

  int getItemCount() {
    assertMainThread();
    return mLayoutOutputsIds == null ? 0 : mLayoutOutputsIds.length;
  }

  MountItem getItemAt(int i) {
    assertMainThread();
    return mIndexToItemMap.get(mLayoutOutputsIds[i]);
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
    if (!mIsDirty) {
      throw new RuntimeException("Should only process transitions on dirty mounts");
    }

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
        if (!mIsFirstMountOfComponentTree) {
          return;
        }
      }

      if (!mDisappearingMountItems.isEmpty()) {
        updateDisappearingMountItems(layoutState);
      }

      if (shouldAnimateTransitions(layoutState)) {
        collectAllTransitions(layoutState, componentTree);
        if (hasTransitionsToAnimate()) {
          createNewTransitions(layoutState, mRootTransition);
        }
      }

      mAnimationLockedIndices = null;
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
    for (OutputUnitsAffinityGroup<MountItem> group : mDisappearingMountItems.values()) {
      endUnmountDisappearingItem(group);
    }
    mDisappearingMountItems.clear();
    mAnimatingTransitionIds.clear();
    mTransitionManager.reset();
    mAnimationLockedIndices = null;
  }

  private void updateDisappearingMountItems(LayoutState newLayoutState) {
    final Map<TransitionId, ?> nextMountedTransitionIds = newLayoutState.getTransitionIdMapping();
    for (TransitionId transitionId : nextMountedTransitionIds.keySet()) {
      final OutputUnitsAffinityGroup<MountItem> disappearingItem =
          mDisappearingMountItems.remove(transitionId);
      if (disappearingItem != null) {
        endUnmountDisappearingItem(disappearingItem);
      }
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

  private void regenerateAnimationLockedIndices(LayoutState newLayoutState) {
    final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transitionMapping =
        newLayoutState.getTransitionIdMapping();
    if (transitionMapping != null) {
      for (Map.Entry<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transition :
          transitionMapping.entrySet()) {
        if (!mAnimatingTransitionIds.contains(transition.getKey())) {
          continue;
        }

        if (mAnimationLockedIndices == null) {
          mAnimationLockedIndices = new int[newLayoutState.getMountableOutputCount()];
        }

        final OutputUnitsAffinityGroup<LayoutOutput> group = transition.getValue();
        for (int j = 0, sz = group.size(); j < sz; j++) {
          final LayoutOutput layoutOutput = group.getAt(j);
          final int position = newLayoutState.getLayoutOutputPositionForId(layoutOutput.getId());
          updateAnimationLockCount(newLayoutState, position, true);
        }
      }
    } else {
      mAnimationLockedIndices = null;
    }

    if (AnimationsDebug.ENABLED) {
      AnimationsDebug.debugPrintAnimationLockedIndices(newLayoutState, mAnimationLockedIndices);
    }
  }

  private int findLastDescendantIndex(LayoutState layoutState, int index) {
    final LayoutOutput host = layoutState.getMountableOutputAt(index);
    final long hostId = host.getId();

    for (int i = index + 1, size = layoutState.getMountableOutputCount(); i < size; i++) {
      final LayoutOutput layoutOutput = layoutState.getMountableOutputAt(i);

      // Walk up the parents looking for the host's id: if we find it, it's a descendant. If we
      // reach the root, then it's not a descendant and we can stop.
      long curentHostId = layoutOutput.getHostMarker();
      while (curentHostId != hostId) {
        if (curentHostId == ROOT_HOST_ID) {
          return i - 1;
        }

        final int parentIndex = layoutState.getLayoutOutputPositionForId(curentHostId);
        final LayoutOutput parent = layoutState.getMountableOutputAt(parentIndex);
        curentHostId = parent.getHostMarker();
      }
    }

    return layoutState.getMountableOutputCount() - 1;
  }

  /**
   * Update the animation locked count for all children and each parent of the animating item. Mount
   * items that have a lock count > 0 will not be unmounted during incremental mount.
   */
  private void updateAnimationLockCount(LayoutState layoutState, int index, boolean increment) {
    // Update children
    final int lastDescendantIndex = findLastDescendantIndex(layoutState, index);
    for (int i = index; i <= lastDescendantIndex; i++) {
      if (increment) {
        mAnimationLockedIndices[i]++;
      } else {
        if (--mAnimationLockedIndices[i] < 0) {
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.FATAL, "Decremented animation lock count below 0!");
          mAnimationLockedIndices[i] = 0;
        }
      }
    }

    // Update parents
    long hostId = layoutState.getMountableOutputAt(index).getHostMarker();
    while (hostId != ROOT_HOST_ID) {
      final int hostIndex = layoutState.getLayoutOutputPositionForId(hostId);
      if (increment) {
        mAnimationLockedIndices[hostIndex]++;
      } else {
        if (--mAnimationLockedIndices[hostIndex] < 0) {
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.FATAL, "Decremented animation lock count below 0!");
          mAnimationLockedIndices[hostIndex] = 0;
        }
      }
      hostId = layoutState.getMountableOutputAt(hostIndex).getHostMarker();
    }
  }

  /**
   * @return whether we should animate transitions if we have any when mounting the new LayoutState.
   */
  private boolean shouldAnimateTransitions(LayoutState newLayoutState) {
    return mIsDirty
        && (mLastMountedComponentTreeId == newLayoutState.getComponentTreeId()
            || mIsFirstMountOfComponentTree);
  }

  /**
   * @return whether we have any transitions to animate for the current mount of the given
   *     LayoutState
   */
  private boolean hasTransitionsToAnimate() {
    return mRootTransition != null;
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
          mLastMountedLayoutState.getLayoutOutputsForTransitionId(transitionId);
      if (layoutOutputGroup == null) {
        // This can happen if the component was unmounted without animation or the transitionId
        // was removed from the component.
        return;
      }

      for (int i = 0, size = layoutOutputGroup.size(); i < size; i++) {
        final LayoutOutput layoutOutput = layoutOutputGroup.getAt(i);
        final int position = layoutOutput.getIndex();
        updateAnimationLockCount(mLastMountedLayoutState, position, false);
      }

      if (ComponentsConfiguration.isDebugModeEnabled && mAnimatingTransitionIds.isEmpty()) {
        for (int i = 0, size = mAnimationLockedIndices.length; i < size; i++) {
          if (mAnimationLockedIndices[i] != 0) {
            throw new RuntimeException(
                "No running animations but index " + i + " is still animation locked!");
          }
        }
      }
    }
  }

  private static class PrepareMountStats {
    private int unmountedCount = 0;
    private int movedCount = 0;
    private int unchangedCount = 0;

    private PrepareMountStats() {}

    private void reset() {
      unchangedCount = 0;
      movedCount = 0;
      unmountedCount = 0;
    }
  }

  private static class MountStats {
    private List<String> mountedNames;
    private List<String> unmountedNames;
    private List<String> updatedNames;
    private List<String> visibilityHandlerNames;
    private List<String> extras;

    private List<Double> mountTimes;
    private List<Double> unmountedTimes;
    private List<Double> updatedTimes;
    private List<Double> visibilityHandlerTimes;

    private int mountedCount;
    private int unmountedCount;
    private int updatedCount;
    private int noOpCount;

    private double visibilityHandlersTotalTime;

    private boolean isLoggingEnabled;
    private boolean isInitialized;

    private void enableLogging() {
      isLoggingEnabled = true;

      if (!isInitialized) {
        isInitialized = true;
        mountedNames = new ArrayList<>();
        unmountedNames = new ArrayList<>();
        updatedNames = new ArrayList<>();
        visibilityHandlerNames = new ArrayList<>();
        extras = new ArrayList<>();

        mountTimes = new ArrayList<>();
        unmountedTimes = new ArrayList<>();
        updatedTimes = new ArrayList<>();
        visibilityHandlerTimes = new ArrayList<>();
      }
    }

    private void reset() {
      mountedCount = 0;
      unmountedCount = 0;
      updatedCount = 0;
      noOpCount = 0;
      visibilityHandlersTotalTime = 0;

      if (isInitialized) {
        mountedNames.clear();
        unmountedNames.clear();
        updatedNames.clear();
        visibilityHandlerNames.clear();
        extras.clear();

        mountTimes.clear();
        unmountedTimes.clear();
        updatedTimes.clear();
        visibilityHandlerTimes.clear();
      }

      isLoggingEnabled = false;
    }
  }

  /**
   * Unbinds all the MountItems currently mounted on this MountState. Unbinding a MountItem means
   * calling unbind on its {@link Component}. The MountItem is not yet unmounted after unbind is
   * called and can be re-used in place to re-mount another {@link Component} with the same {@link
   * ComponentLifecycle}.
   */
  void unbind() {
    assertMainThread();
    if (mLayoutOutputsIds == null) {
      return;
    }

    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("MountState.unbind");
    }

    for (int i = 0, size = mLayoutOutputsIds.length; i < size; i++) {
      MountItem mountItem = getItemAt(i);

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindComponentFromContent(mountItem.getComponent(), mountItem.getContent());
      mountItem.setIsBound(false);
    }

    clearVisibilityItems();

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  void detach() {
    assertMainThread();
    unbind();
  }

  /**
   * This is called when the {@link MountItem}s mounted on this {@link MountState} need to be
   * re-bound with the same component. The common case here is a detach/attach happens on the {@link
   * LithoView} that owns the MountState.
   */
  void rebind() {
    assertMainThread();

    if (mLayoutOutputsIds == null) {
      return;
    }

    for (int i = 0, size = mLayoutOutputsIds.length; i < size; i++) {
      final MountItem mountItem = getItemAt(i);
      if (mountItem == null || mountItem.isBound()) {
        continue;
      }

      final Component component = mountItem.getComponent();
      final Object content = mountItem.getContent();

      bindComponentToContent(component, content);
      mountItem.setIsBound(true);

      if (content instanceof View
          && !(content instanceof ComponentHost)
          && ((View) content).isLayoutRequested()) {
        final View view = (View) content;
        applyBoundsToMountContent(
            view, view.getLeft(), view.getTop(), view.getRight(), view.getBottom(), true);
      }
    }
  }

  /**
   * Whether the item at this index (or one of its parents) are animating. In that case, we don't
   * want to unmount this index for visibility reasons (e.g. incremental mount). The reason for this
   * is that this item (or it's parent) may have a translation X/Y that actually shows it on the
   * screen, even though the non-translated bounds are off the screen.
   */
  private boolean isAnimationLocked(int index) {
    if (mAnimationLockedIndices == null) {
      return false;
    }
    return mAnimationLockedIndices[index] > 0;
  }

  /**
   * @return true if this method did all the work that was necessary and there is no other content
   *     that needs mounting/unmounting in this mount step. If false then a full mount step should
   *     take place.
   */
  private boolean performIncrementalMount(
      LayoutState layoutState, Rect localVisibleRect, boolean processVisibilityOutputs) {
    if (mPreviousLocalVisibleRect.isEmpty()) {
      return false;
    }

    if (localVisibleRect.left != mPreviousLocalVisibleRect.left
        || localVisibleRect.right != mPreviousLocalVisibleRect.right) {
      return false;
    }

    final ArrayList<LayoutOutput> layoutOutputTops = layoutState.getMountableOutputTops();
    final ArrayList<LayoutOutput> layoutOutputBottoms = layoutState.getMountableOutputBottoms();
    final int count = layoutState.getMountableOutputCount();

    if (localVisibleRect.top > 0 || mPreviousLocalVisibleRect.top > 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= layoutOutputBottoms.get(mPreviousBottomsIndex).getBounds().bottom) {
        final long id = layoutOutputBottoms.get(mPreviousBottomsIndex).getId();
        final int layoutOutputIndex = layoutState.getLayoutOutputPositionForId(id);
        if (!isAnimationLocked(layoutOutputIndex)) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              < layoutOutputBottoms.get(mPreviousBottomsIndex - 1).getBounds().bottom) {
        mPreviousBottomsIndex--;
        final LayoutOutput layoutOutput = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final int layoutOutputIndex =
            layoutState.getLayoutOutputPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getLayoutOutputPositionForId(layoutOutput.getId()),
              layoutOutput,
              layoutState);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
      }
    }

    final int height = mLithoView.getHeight();
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom > layoutOutputTops.get(mPreviousTopsIndex).getBounds().top) {
        final LayoutOutput layoutOutput = layoutOutputTops.get(mPreviousTopsIndex);
        final int layoutOutputIndex =
            layoutState.getLayoutOutputPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getLayoutOutputPositionForId(layoutOutput.getId()),
              layoutOutput,
              layoutState);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              <= layoutOutputTops.get(mPreviousTopsIndex - 1).getBounds().top) {
        mPreviousTopsIndex--;
        final long id = layoutOutputTops.get(mPreviousTopsIndex).getId();
        final int layoutOutputIndex = layoutState.getLayoutOutputPositionForId(id);
        if (!isAnimationLocked(layoutOutputIndex)) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
      }
    }

    for (int i = 0, size = mCanMountIncrementallyMountItems.size(); i < size; i++) {
      final MountItem mountItem = mCanMountIncrementallyMountItems.valueAt(i);
      final long layoutOutputId = mCanMountIncrementallyMountItems.keyAt(i);
      if (!mComponentIdsMountedInThisFrame.contains(layoutOutputId)) {
        final int layoutOutputPosition = layoutState.getLayoutOutputPositionForId(layoutOutputId);
        if (layoutOutputPosition != -1) {
          mountItemIncrementally(mountItem, processVisibilityOutputs);
        }
      }
    }

    mComponentIdsMountedInThisFrame.clear();

    return true;
  }

  private void prepareTransitionManager() {
    if (mTransitionManager == null) {
      mTransitionManager = new TransitionManager(this, this);
    }
  }

  private void removeDisappearingMountContentFromComponentHost(ComponentHost componentHost) {
    if (componentHost.hasDisappearingItems()) {
      List<TransitionId> ids = componentHost.getDisappearingItemTransitionIds();
      for (int i = 0, size = ids.size(); i < size; i++) {
        mTransitionManager.setMountContent(ids.get(i), null);
      }
    }
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

  /** @see LithoViewTestHelper#findTestItems(LithoView, String) */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Deque<TestItem> findTestItems(String testKey) {
    if (mTestItemMap == null) {
      throw new UnsupportedOperationException(
          "Trying to access TestItems while "
              + "ComponentsConfiguration.isEndToEndTestRun is false.");
    }

    final Deque<TestItem> items = mTestItemMap.get(testKey);
    return items == null ? new LinkedList<TestItem>() : items;
  }

  /**
   * For HostComponents, we don't set a scoped context during layout calculation because we don't
   * need one, as we could never call a state update on it. Instead it's okay to use the context
   * that is passed to MountState from the LithoView, which is not scoped.
   */
  private ComponentContext getContextForComponent(Component component) {
    final ComponentContext c = component.getScopedContext();
    return c == null ? mContext : c;
  }

  private void bindComponentToContent(Component component, Object content) {
    component.bind(getContextForComponent(component), content);
    mDynamicPropsManager.onBindComponentToContent(component, content);
  }

  private void unbindComponentFromContent(Component component, Object content) {
    mDynamicPropsManager.onUnbindComponent(component);
    component.unbind(getContextForComponent(component), content);
  }
}
