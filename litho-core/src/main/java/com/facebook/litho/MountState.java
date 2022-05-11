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

package com.facebook.litho;

import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.ComponentHostUtils.maybeSetDrawableState;
import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_HAD_PREVIOUS_CT;
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
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LithoMountData.getMountData;
import static com.facebook.litho.LithoRenderUnit.getComponentContext;
import static com.facebook.litho.LithoRenderUnit.isMountableView;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.UnmountDelegateExtension;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput;
import com.facebook.rendercore.utils.BoundsUtils;
import com.facebook.rendercore.visibility.VisibilityItem;
import com.facebook.rendercore.visibility.VisibilityMountExtension;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
class MountState implements MountDelegateTarget {

  private static final String INVALID_REENTRANT_MOUNTS = "MountState:InvalidReentrantMounts";
  private static final double NS_IN_MS = 1000000.0;

  // Holds the current list of mounted items.
  // Should always be used within a draw lock.
  private final LongSparseArray<MountItem> mIndexToItemMap;

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

  // True if MountState is currently performing mount.
  private boolean mIsMounting;

  // See #needsRemount()
  private boolean mNeedsRemount;

  // Holds the list of known component hosts during a mount pass.
  private final LongSparseArray<ComponentHost> mHostsByMarker = new LongSparseArray<>();

  private final ComponentContext mContext;
  private final LithoView mLithoView;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final PrepareMountStats mPrepareMountStats = new PrepareMountStats();
  private final MountStats mMountStats = new MountStats();
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private int mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
  private @Nullable LayoutState mLayoutState;
  private @Nullable LayoutState mLastMountedLayoutState;

  private final MountItem mRootHostMountItem;

  private final @Nullable VisibilityMountExtension mVisibilityExtension;
  private final @Nullable ExtensionState mVisibilityExtensionState;
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();

  private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();
  private @Nullable MountDelegate mMountDelegate;
  private @Nullable UnmountDelegateExtension mUnmountDelegateExtension;
  private @Nullable TransitionsExtension mTransitionsExtension;
  private @Nullable ExtensionState mTransitionsExtensionState;
  private @Nullable HostMountContentPool mHostMountContentPool;

  public final boolean mShouldUsePositionInParent =
      ComponentsConfiguration.shouldUsePositionInParentForMounting;

  public MountState(LithoView view) {
    mIndexToItemMap = new LongSparseArray<>();
    mCanMountIncrementallyMountItems = new LongSparseArray<>();
    mContext = view.getComponentContext();
    mLithoView = view;
    mIsDirty = true;

    mTestItemMap =
        ComponentsConfiguration.isEndToEndTestRun ? new HashMap<String, Deque<TestItem>>() : null;

    // The mount item representing the top-level root host (LithoView) which
    // is always automatically mounted.
    mRootHostMountItem = LithoMountData.createRootHostMountItem(mLithoView);

    mMountDelegate = new MountDelegate(this);

    if (ComponentsConfiguration.enableVisibilityExtension) {
      mVisibilityExtension = VisibilityMountExtension.getInstance();
      mVisibilityExtensionState = mMountDelegate.registerMountExtension(mVisibilityExtension);

      VisibilityMountExtension.setRootHost(mVisibilityExtensionState, mLithoView);
    } else {
      mVisibilityExtension = null;
      mVisibilityExtensionState = null;
    }

    // Using Incremental Mount Extension and the Transition Extension here is not allowed.
    if (ComponentsConfiguration.enableTransitionsExtension) {
      mTransitionsExtension =
          TransitionsExtension.getInstance((AnimationsDebug.ENABLED ? AnimationsDebug.TAG : null));
      mTransitionsExtensionState = mMountDelegate.registerMountExtension(mTransitionsExtension);
    } else {
      mTransitionsExtension = null;
      mTransitionsExtensionState = null;
    }
  }

  @Deprecated
  @Override
  public ExtensionState registerMountExtension(MountExtension mountExtensions) {
    throw new UnsupportedOperationException("Must not be invoked when use this Litho's MountState");
  }

  /** @deprecated Only used for Litho's integration. Marked for removal. */
  @Deprecated
  @Override
  public void unregisterAllExtensions() {
    throw new UnsupportedOperationException("Must not be invoked when use this Litho's MountState");
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

  /**
   * True if we have manually unmounted content (e.g. via unmountAllItems) which means that while we
   * may not have a new LayoutState, the mounted content does not match what the viewport for the
   * LithoView may be.
   */
  @Override
  public boolean needsRemount() {
    assertMainThread();

    return mNeedsRemount;
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param layoutState a new {@link LayoutState} to mount
   * @param localVisibleRect If this variable is null, then mount everything, since incremental
   *     mount is not enabled. Otherwise mount only what the rect (in local coordinates) contains
   * @param processVisibilityOutputs whether to process visibility outputs as part of the mount
   */
  void mount(
      LayoutState layoutState, @Nullable Rect localVisibleRect, boolean processVisibilityOutputs) {
    final ComponentTree componentTree = mLithoView.getComponentTree();
    final boolean isIncrementalMountEnabled = componentTree.isIncrementalMountEnabled();
    final boolean isVisibilityProcessingEnabled =
        componentTree.isVisibilityProcessingEnabled() && processVisibilityOutputs;

    assertMainThread();

    if (layoutState == null) {
      throw new IllegalStateException("Trying to mount a null layoutState");
    }

    final boolean shouldIncrementallyMount =
        isIncrementalMountEnabled
            && localVisibleRect != null
            && !mPreviousLocalVisibleRect.isEmpty()
            && localVisibleRect.left == mPreviousLocalVisibleRect.left
            && localVisibleRect.right == mPreviousLocalVisibleRect.right;

    if (mVisibilityExtension != null && mIsDirty) {
      mVisibilityExtension.beforeMount(mVisibilityExtensionState, layoutState, localVisibleRect);
    }

    if (mTransitionsExtension != null && mIsDirty) {
      mTransitionsExtension.beforeMount(mTransitionsExtensionState, layoutState, localVisibleRect);
    }

    mLayoutState = layoutState;

    if (mIsMounting) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.FATAL,
          INVALID_REENTRANT_MOUNTS,
          "Trying to mount while already mounting! "
              + getMountItemDebugMessage(mRootHostMountItem));
    }
    mIsMounting = true;

    final boolean isTracing = RenderCoreSystrace.isEnabled();
    if (isTracing) {

      // Equivalent to RCMS MountState.mount
      if (!shouldIncrementallyMount) {
        RenderCoreSystrace.beginSection("MountState.mount");
      }

      ComponentsSystrace.beginSectionWithArgs(
              "LMS."
                  + (shouldIncrementallyMount ? "incrementalMount" : "mount")
                  + (mIsDirty ? "Dirty" : "")
                  + ": "
                  + componentTree.getSimpleName())
          .arg("treeId", layoutState.getComponentTreeId())
          .arg("component", componentTree.getSimpleName())
          .arg("logTag", componentTree.getContext().getLogTag())
          .flush();
    }

    final ComponentsLogger logger = componentTree.getContext().getLogger();
    final int componentTreeId = layoutState.getComponentTreeId();
    if (componentTreeId != mLastMountedComponentTreeId) {
      // If we're mounting a new ComponentTree, don't keep around and use the previous LayoutState
      // since things like transition animations aren't relevant.
      clearLastMountedLayoutState();
    }

    final PerfEvent mountPerfEvent =
        logger == null
            ? null
            : LogTreePopulator.populatePerfEventFromLogger(
                componentTree.getContext(),
                logger,
                logger.newPerformanceEvent(componentTree.getContext(), EVENT_MOUNT));

    if (mIsDirty) {
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

    if (shouldIncrementallyMount) {
      performIncrementalMount(layoutState, localVisibleRect, processVisibilityOutputs);
    } else {

      final MountItem rootMountItem = mIndexToItemMap.get(ROOT_HOST_ID);
      final Rect absoluteBounds = new Rect();

      for (int i = 0, size = layoutState.getMountableOutputCount(); i < size; i++) {
        final RenderTreeNode node = layoutState.getMountableOutputAt(i);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final Component component = layoutOutput.getComponent();
        final MountItem currentMountItem = getItemAt(i);
        final IncrementalMountOutput incrementalMountOutput =
            layoutState.getIncrementalMountOutputForId(node.getRenderUnit().getId());
        final boolean isMounted = currentMountItem != null;
        final boolean isRoot = currentMountItem != null && currentMountItem == rootMountItem;
        final boolean isExcludingFromIncrementalMount =
            incrementalMountOutput != null && incrementalMountOutput.excludeFromIncrementalMount();
        final boolean isMountable =
            !isIncrementalMountEnabled
                || isMountedHostWithChildContent(currentMountItem)
                || Rect.intersects(localVisibleRect, node.getAbsoluteBounds(absoluteBounds))
                || isAnimationLocked(node)
                || isRoot
                || isExcludingFromIncrementalMount;
        if (isMountable && !isMounted) {
          mountLayoutOutput(i, node, layoutOutput, layoutState);
        } else if (!isMountable && isMounted) {
          unmountItem(i, mHostsByMarker);
        } else if (isMounted) {
          if (mIsDirty || (isRoot && mNeedsRemount)) {
            final boolean useUpdateValueFromLayoutOutput =
                mLastMountedLayoutState != null
                    && mLastMountedLayoutState.getId() == layoutState.getPreviousLayoutStateId();

            final long startTime = System.nanoTime();
            final boolean itemUpdated =
                updateMountItemIfNeeded(
                    node,
                    currentMountItem,
                    useUpdateValueFromLayoutOutput,
                    isIncrementalMountEnabled,
                    processVisibilityOutputs);
            if (mMountStats.isLoggingEnabled) {
              if (itemUpdated) {
                mMountStats.updatedNames.add(component.getSimpleName());
                mMountStats.updatedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
                mMountStats.updatedCount++;
              } else {
                mMountStats.noOpCount++;
              }
            }
          } else {
            if (isIncrementalMountEnabled
                && component.hasChildLithoViews()
                && !mLithoView.skipNotifyVisibleBoundsChangedCalls()) {
              mountItemIncrementally(currentMountItem, processVisibilityOutputs);
            }
          }
        }
      }

      if (isIncrementalMountEnabled) {
        setupPreviousMountableOutputData(layoutState, localVisibleRect);
      }
    }

    if (isTracing) {
      if (!shouldIncrementallyMount) {
        RenderCoreSystrace.endSection();
      }
      ComponentsSystrace.endSection(); // beginSectionWithArgs

      RenderCoreSystrace.beginSection("RenderCoreExtension.afterMount");
    }

    afterMountMaybeUpdateAnimations();

    if (isVisibilityProcessingEnabled) {
      if (isTracing) {
        RenderCoreSystrace.beginSection("LMS.processVisibilityOutputs");
      }
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("EVENT_PROCESS_VISIBILITY_OUTPUTS_START");
      }
      processVisibilityOutputs(localVisibleRect, mIsDirty);
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("EVENT_PROCESS_VISIBILITY_OUTPUTS_END");
      }
      if (isTracing) {
        RenderCoreSystrace.endSection();
      }
    }

    final boolean wasDirty = mIsDirty;
    final boolean hadPreviousComponentTree =
        (mLastMountedComponentTreeId != ComponentTree.INVALID_ID);
    mIsDirty = false;
    mNeedsRemount = false;
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }

    clearLastMountedLayoutState();
    mLastMountedComponentTreeId = componentTreeId;
    mLastMountedLayoutState = layoutState;

    processTestOutputs(layoutState);

    if (mountPerfEvent != null) {
      logMountPerfEvent(logger, mountPerfEvent, wasDirty, hadPreviousComponentTree);
    }

    LithoStats.incrementComponentMountCount();

    mIsMounting = false;

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  private void clearLastMountedLayoutState() {
    mLastMountedLayoutState = null;
  }

  private void afterMountMaybeUpdateAnimations() {
    if (mTransitionsExtension != null && mIsDirty) {
      mTransitionsExtension.afterMount(mTransitionsExtensionState);
    }
  }

  @Override
  public void mount(RenderTree renderTree) {
    throw new UnsupportedOperationException("This method must not be invoked.");
  }

  @Override
  public void notifyMount(long id) {
    if (mLayoutState == null) {
      return;
    }

    final int position = mLayoutState.getPositionForId(id);

    if (position < 0 || getItemAt(position) != null) {
      return;
    }

    final RenderTreeNode node = mLayoutState.getMountableOutputAt(position);
    mountLayoutOutput(position, node, getLayoutOutput(node), mLayoutState);
  }

  @Override
  public void notifyUnmount(long id) {
    final MountItem item = mIndexToItemMap.get(id);
    if (item == null || mLayoutState == null) {
      return;
    }

    final int position = mLayoutState.getPositionForId(id);
    if (position >= 0) {
      unmountItem(position, mHostsByMarker);
    }
  }

  private void logMountPerfEvent(
      ComponentsLogger logger,
      PerfEvent mountPerfEvent,
      boolean isDirty,
      boolean hadPreviousComponentTree) {
    if (!mMountStats.isLoggingEnabled) {
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
    mountPerfEvent.markerAnnotate(PARAM_HAD_PREVIOUS_CT, hadPreviousComponentTree);

    logger.logPerfEvent(mountPerfEvent);
  }

  void processVisibilityOutputs(@Nullable Rect localVisibleRect, boolean isDirty) {
    if (mVisibilityExtension == null) {
      return;
    }

    if (isDirty) {
      mVisibilityExtension.afterMount(mVisibilityExtensionState);
    } else {
      mVisibilityExtension.onVisibleBoundsChanged(mVisibilityExtensionState, localVisibleRect);
    }
  }

  @VisibleForTesting
  Map<String, VisibilityItem> getVisibilityIdToItemMap() {
    return VisibilityMountExtension.getVisibilityIdToItemMap(mVisibilityExtensionState);
  }

  @VisibleForTesting
  @Override
  public ArrayList<Host> getHosts() {
    final ArrayList<Host> hosts = new ArrayList<>();
    for (int i = 0, size = mHostsByMarker.size(); i < size; i++) {
      hosts.add(mHostsByMarker.valueAt(i));
    }

    return hosts;
  }

  @Override
  public int getMountItemCount() {
    return mIndexToItemMap.size();
  }

  @Override
  public int getRenderUnitCount() {
    assertMainThread();
    return mLayoutOutputsIds == null ? 0 : mLayoutOutputsIds.length;
  }

  @Override
  public @Nullable MountItem getMountItemAt(int position) {
    return getItemAt(position);
  }

  @Override
  public void setUnmountDelegateExtension(UnmountDelegateExtension unmountDelegateExtension) {
    mUnmountDelegateExtension = unmountDelegateExtension;
  }

  @Override
  public void removeUnmountDelegateExtension() {
    mUnmountDelegateExtension = null;
  }

  @Nullable
  @Override
  public MountDelegate getMountDelegate() {
    return mMountDelegate;
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

  private static boolean isMountedHostWithChildContent(@Nullable MountItem mountItem) {
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

    final ArrayList<IncrementalMountOutput> layoutOutputTops =
        layoutState.getOutputsOrderedByTopBounds();
    final ArrayList<IncrementalMountOutput> layoutOutputBottoms =
        layoutState.getOutputsOrderedByBottomBounds();
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
    if (mVisibilityExtension != null) {
      VisibilityMountExtension.clearVisibilityItems(mVisibilityExtensionState);
    }
  }

  private void registerHost(long id, ComponentHost host) {
    mHostsByMarker.put(id, host);
  }

  private boolean updateMountItemIfNeeded(
      RenderTreeNode node,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput,
      boolean isIncrementalMountEnabled,
      boolean processVisibilityOutputs) {

    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("updateMountItemIfNeeded");
    }

    final LayoutOutput nextLayoutOutput = getLayoutOutput(node);
    final Component layoutOutputComponent = nextLayoutOutput.getComponent();
    final LayoutOutput currentLayoutOutput = getLayoutOutput(currentMountItem);
    final Component itemComponent = currentLayoutOutput.getComponent();
    final Object currentContent = currentMountItem.getContent();
    final ComponentHost host = (ComponentHost) currentMountItem.getHost();
    final ComponentContext currentContext = getComponentContext(currentMountItem);
    final ComponentContext nextContext = getComponentContext(node);
    final LithoLayoutData nextLayoutData = (LithoLayoutData) node.getLayoutData();
    final LithoLayoutData currentLayoutData =
        (LithoLayoutData) currentMountItem.getRenderTreeNode().getLayoutData();

    if (isTracing) {
      RenderCoreSystrace.beginSection("UpdateItem: " + node.getRenderUnit().getDescription());
    }

    // 1. Check if the mount item generated from the old component should be updated.
    final boolean shouldUpdate =
        MountSpecLithoRenderUnit.shouldUpdateMountItem(
            nextLayoutOutput,
            nextLayoutData,
            nextContext,
            currentLayoutOutput,
            currentLayoutData,
            currentContext,
            useUpdateValueFromLayoutOutput);

    final boolean shouldUpdateViewInfo =
        shouldUpdate
            || LithoViewAttributesExtension.shouldUpdateViewInfo(
                nextLayoutOutput, currentLayoutOutput);

    // 2. We will re-bind this later in 7 regardless so let's make sure it's currently unbound.
    if (currentMountItem.isBound()) {
      unbindComponentFromContent(currentMountItem, itemComponent, currentMountItem.getContent());
    }

    // 3. Reset all the properties like click handler, content description and tags related to
    // this item if it needs to be updated. the update mount item will re-set the new ones.

    if (shouldUpdateViewInfo) {
      maybeUnsetViewAttributes(currentMountItem);
    }

    // 4. Re initialize the MountItem internal state with the new attributes from LayoutOutput
    currentMountItem.update(node);

    // 5. If the mount item is not valid for this component update its content and view attributes.
    if (shouldUpdate) {
      updateMountedContent(
          currentMountItem,
          layoutOutputComponent,
          nextContext,
          nextLayoutData,
          itemComponent,
          currentContext,
          currentLayoutData);

      // For RCMS the delegates is invoked here
      // onUnbindItem
      // onUnmountItem
      // onMountItem
      // onBindItem
    }

    if (shouldUpdateViewInfo) {
      setViewAttributes(currentMountItem);
    }

    // 6. Set the mounted content on the Component and call the bind callback.
    bindComponentToContent(
        currentMountItem, layoutOutputComponent, nextContext, nextLayoutData, currentContent);

    // 7. Update the bounds of the mounted content. This needs to be done regardless of whether
    // the component has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedLayoutOutput(node, nextLayoutOutput, currentMountItem);

    if (isIncrementalMountEnabled
        && layoutOutputComponent.hasChildLithoViews()
        && !mLithoView.skipNotifyVisibleBoundsChangedCalls()) {
      mountItemIncrementally(currentMountItem, processVisibilityOutputs);
    }

    if (currentMountItem.getContent() instanceof Drawable) {
      maybeSetDrawableState(
          host,
          (Drawable) currentContent,
          currentLayoutOutput.getFlags(),
          currentLayoutOutput.getNodeInfo());
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.endSection();
    }

    return shouldUpdate;
  }

  private static void updateBoundsForMountedLayoutOutput(
      final RenderTreeNode node, final LayoutOutput layoutOutput, final MountItem item) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (node.getRenderUnit().getId() == ROOT_HOST_ID) {
      return;
    }

    final Rect bounds = node.getBounds();
    final boolean forceTraversal =
        isMountableView(node.getRenderUnit()) && ((View) item.getContent()).isLayoutRequested();

    applyBoundsToMountContent(
        item.getContent(),
        bounds.left,
        bounds.top,
        bounds.right,
        bounds.bottom,
        forceTraversal /* force */);
  }

  /** Prepare the {@link MountState} to mount a new {@link LayoutState}. */
  private void prepareMount(LayoutState layoutState, @Nullable PerfEvent perfEvent) {
    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("MountState.prepareMount");
    }

    final PrepareMountStats stats = unmountOrMoveOldItems(layoutState);

    if (perfEvent != null) {
      perfEvent.markerAnnotate(PARAM_UNMOUNTED_COUNT, stats.unmountedCount);
      perfEvent.markerAnnotate(PARAM_MOVED_COUNT, stats.movedCount);
      perfEvent.markerAnnotate(PARAM_UNCHANGED_COUNT, stats.unchangedCount);
    }

    if (mIndexToItemMap.get(ROOT_HOST_ID) == null || mHostsByMarker.get(ROOT_HOST_ID) == null) {
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
      mLayoutOutputsIds[i] = layoutState.getMountableOutputAt(i).getRenderUnit().getId();
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private PrepareMountStats unmountOrMoveOldItems(LayoutState newLayoutState) {
    mPrepareMountStats.reset();

    if (mLayoutOutputsIds == null) {
      return mPrepareMountStats;
    }

    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("unmountOrMoveOldItems");
    }

    // Traversing from the beginning since mLayoutOutputsIds unmounting won't remove entries there
    // but only from mIndexToItemMap. If an host changes we're going to unmount it and recursively
    // all its mounted children.
    for (int i = 1; i < mLayoutOutputsIds.length; i++) {
      final int newPosition = newLayoutState.getPositionForId(mLayoutOutputsIds[i]);
      final @Nullable RenderTreeNode newRenderTreeNode =
          newPosition != -1 ? newLayoutState.getMountableOutputAt(newPosition) : null;

      final MountItem oldItem = getItemAt(i);
      final boolean hasUnmountDelegate =
          mUnmountDelegateExtension != null && oldItem != null
              ? mUnmountDelegateExtension.shouldDelegateUnmount(
                  mMountDelegate.getUnmountDelegateExtensionState(), oldItem)
              : false;

      if (hasUnmountDelegate) {
        continue;
      }

      if (newPosition == -1) {
        unmountItem(i, mHostsByMarker);
        mPrepareMountStats.unmountedCount++;
      } else {
        final long newHostMarker = newRenderTreeNode.getParent().getRenderUnit().getId();

        if (oldItem == null) {
          // This was previously unmounted.
          mPrepareMountStats.unmountedCount++;
        } else if (oldItem.getHost() != mHostsByMarker.get(newHostMarker)) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItem(i, mHostsByMarker);
          mPrepareMountStats.unmountedCount++;

        } else if (mShouldUsePositionInParent
            && oldItem.getRenderTreeNode().getPositionInParent()
                != newRenderTreeNode.getPositionInParent()) {
          // If a MountItem for this id exists and the hostMarker has not changed but its position
          // in the outputs array has changed we need to update the position in the Host to ensure
          // the z-ordering.
          oldItem
              .getHost()
              .moveItem(
                  oldItem,
                  oldItem.getRenderTreeNode().getPositionInParent(),
                  newRenderTreeNode.getPositionInParent());
          mPrepareMountStats.movedCount++;
        } else if (!mShouldUsePositionInParent && newPosition != i) {
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

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }

    return mPrepareMountStats;
  }

  private void updateMountedContent(
      final MountItem item,
      final Component newComponent,
      final ComponentContext newContext,
      final LithoLayoutData nextLayoutData,
      final Component previousComponent,
      final ComponentContext previousContext,
      final LithoLayoutData currentLayoutData) {

    if (isHostSpec(newComponent)) {
      return;
    }

    final Object previousContent = item.getContent();

    // Call unmount and mount in sequence to make sure all the the resources are correctly
    // de-allocated. It's possible for previousContent to equal null - when the root is
    // interactive we create a LayoutOutput without content in order to set up click handling.
    previousComponent.unmount(
        previousContext, previousContent, (InterStagePropsContainer) currentLayoutData.mLayoutData);
    newComponent.mount(
        newContext, previousContent, (InterStagePropsContainer) nextLayoutData.mLayoutData);
  }

  private void mountLayoutOutput(
      final int index,
      final RenderTreeNode node,
      final LayoutOutput layoutOutput,
      final LayoutState layoutState) {
    // 1. Resolve the correct host to mount our content to.
    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("MountItem: " + node.getRenderUnit().getDescription());
      RenderCoreSystrace.beginSection("MountItem:before " + node.getRenderUnit().getDescription());
    }

    final long startTime = System.nanoTime();

    // parent should never be null
    final long hostMarker = node.getParent().getRenderUnit().getId();

    ComponentHost host = mHostsByMarker.get(hostMarker);

    if (host == null) {
      // Host has not yet been mounted - mount it now.
      final int hostMountIndex = layoutState.getPositionForId(hostMarker);
      final RenderTreeNode hostNode = layoutState.getMountableOutputAt(hostMountIndex);
      final LayoutOutput hostLayoutOutput = getLayoutOutput(hostNode);
      mountLayoutOutput(hostMountIndex, hostNode, hostLayoutOutput, layoutState);

      host = mHostsByMarker.get(hostMarker);
    }

    // 2. Generate the component's mount state (this might also be a ComponentHost View).
    final Component component = layoutOutput.getComponent();

    final Object content;
    if (component instanceof HostComponent) {
      content =
          acquireHostComponentContent(mContext.getAndroidContext(), (HostComponent) component);
    } else {
      content = MountItemsPool.acquireMountContent(mContext.getAndroidContext(), component);
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.beginSection("MountItem:mount " + node.getRenderUnit().getDescription());
    }

    final ComponentContext context = getContextForComponent(node);
    final LithoLayoutData layoutData = (LithoLayoutData) node.getLayoutData();
    component.mount(context, content, (InterStagePropsContainer) layoutData.mLayoutData);
    // For RCMS: onMountItem

    // 3. If it's a ComponentHost, add the mounted View to the list of Hosts.
    if (isHostSpec(component)) {
      ComponentHost componentHost = (ComponentHost) content;
      registerHost(node.getRenderUnit().getId(), componentHost);
    }

    // 4. Mount the content into the selected host.
    final MountItem item = mountContent(index, component, content, host, node);
    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.beginSection("MountItem:bind " + node.getRenderUnit().getDescription());
    }

    // 5. Notify the component that mounting has completed
    bindComponentToContent(item, component, context, layoutData, content);

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.beginSection(
          "MountItem:applyBounds " + node.getRenderUnit().getDescription());
    }

    // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    final Rect bounds = node.getBounds();
    applyBoundsToMountContent(
        item.getContent(), bounds.left, bounds.top, bounds.right, bounds.bottom, true /* force */);
    if (mMountDelegate != null) {
      mMountDelegate.onBoundsAppliedToItem(node, item.getContent());
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.beginSection("MountItem:after " + node.getRenderUnit().getDescription());
    }

    // 6. Update the mount stats
    if (mMountStats.isLoggingEnabled) {
      mMountStats.mountTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
      mMountStats.mountedNames.add(component.getSimpleName());
      mMountStats.mountedCount++;

      final ComponentContext scopedContext = getComponentContext(node);

      mMountStats.extras.add(
          LogTreePopulator.getAnnotationBundleFromLogger(scopedContext, context.getLogger()));
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.endSection();
    }
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first LayoutOutput).
  private MountItem mountContent(
      int index, Component component, Object content, ComponentHost host, RenderTreeNode node) {

    final MountItem item = new MountItem(node, host, content);
    item.setMountData(new LithoMountData(content));

    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIndexToItemMap.put(mLayoutOutputsIds[index], item);

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.put(mLayoutOutputsIds[index], item);
    }

    final int positionInParent = item.getRenderTreeNode().getPositionInParent();

    setViewAttributes(item);
    mount(host, mShouldUsePositionInParent ? positionInParent : index, item, node);

    return item;
  }

  private static void mount(
      final ComponentHost host, final int index, final MountItem item, final RenderTreeNode node) {
    host.mount(index, item, node.getBounds());
  }

  private static void unmount(final ComponentHost host, final int index, final MountItem item) {
    host.unmount(index, item);
  }

  private static void applyBoundsToMountContent(
      Object content, int left, int top, int right, int bottom, boolean force) {
    assertMainThread();

    BoundsUtils.applyBoundsToMountContent(left, top, right, bottom, null, content, force);
  }

  private static void setViewAttributes(MountItem item) {
    LithoViewAttributesExtension.setViewAttributes(item.getContent(), getLayoutOutput(item));
  }

  private static void maybeUnsetViewAttributes(MountItem item) {
    final LayoutOutput output = getLayoutOutput(item);
    final int flags = getMountData(item).getDefaultAttributeValuesFlags();
    LithoViewAttributesExtension.unsetViewAttributes(item.getContent(), output, flags);
  }

  private static void mountItemIncrementally(MountItem item, boolean processVisibilityOutputs) {

    if (!isMountableView(item.getRenderTreeNode().getRenderUnit())) {
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
          lithoView.notifyVisibleBoundsChanged(
              new Rect(0, 0, view.getWidth(), view.getHeight()), false);
        } else {
          lithoView.notifyVisibleBoundsChanged();
        }
      }
    } else if (view instanceof RenderCoreExtensionHost) {
      ((RenderCoreExtensionHost) view).notifyVisibleBoundsChanged();
    } else if (view instanceof ViewGroup) {
      final ViewGroup viewGroup = (ViewGroup) view;

      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        final View childView = viewGroup.getChildAt(i);
        mountViewIncrementally(childView, processVisibilityOutputs);
      }
    }
  }

  private String getMountItemDebugMessage(MountItem item) {
    final int index = mIndexToItemMap.indexOfValue(item);

    long id = -1;
    int layoutOutputIndex = -1;
    if (index > -1) {
      id = mIndexToItemMap.keyAt(index);
      for (int i = 0; i < mLayoutOutputsIds.length; i++) {
        if (id == mLayoutOutputsIds[i]) {
          layoutOutputIndex = i;
          break;
        }
      }
    }

    final ComponentTree componentTree = mLithoView.getComponentTree();
    final String rootComponent =
        componentTree == null ? "<null_component_tree>" : componentTree.getRoot().getSimpleName();

    return "rootComponent="
        + rootComponent
        + ", index="
        + layoutOutputIndex
        + ", mapIndex="
        + index
        + ", id="
        + id
        + ", contentType="
        + (item.getContent() != null ? item.getContent().getClass() : "<null_content>")
        + ", component="
        + getLayoutOutput(item).getComponent().getSimpleName()
        + ", host="
        + (item.getHost() != null ? item.getHost().getClass() : "<null_host>")
        + ", isRootHost="
        + (mHostsByMarker.get(ROOT_HOST_ID) == item.getHost());
  }

  @Override
  public void unmountAllItems() {
    assertMainThread();
    if (mLayoutOutputsIds == null) {
      return;
    }

    final boolean isTracing = RenderCoreSystrace.isEnabled();
    if (isTracing) {
      RenderCoreSystrace.beginSection("MountState.unmountAllItems");
    }
    unmountItem(0, mHostsByMarker);
    mPreviousLocalVisibleRect.setEmpty();
    mNeedsRemount = true;

    if (mVisibilityExtension != null) {
      mVisibilityExtension.onUnbind(mVisibilityExtensionState);
      mVisibilityExtension.onUnmount(mVisibilityExtensionState);
    }

    if (mTransitionsExtension != null) {
      mTransitionsExtension.onUnbind(mTransitionsExtensionState);
      mTransitionsExtension.onUnmount(mTransitionsExtensionState);
    }

    if (mMountDelegate != null) {
      mMountDelegate.releaseAllAcquiredReferences();
    }

    clearLastMountedTree();

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  private void unmountItem(int index, LongSparseArray<ComponentHost> hostsByMarker) {
    MountItem item = getItemAt(index);
    if (item != null) {
      unmountItem(item, hostsByMarker);
    }
  }

  private void unmountItem(@Nullable MountItem item, LongSparseArray<ComponentHost> hostsByMarker) {
    final long startTime = System.nanoTime();

    // Already has been unmounted.
    if (item == null) {
      return;
    }

    final boolean isTracing = RenderCoreSystrace.isEnabled();
    final RenderTreeNode node = item.getRenderTreeNode();
    final RenderUnit<?> unit = node.getRenderUnit();
    final long id = unit.getId();

    if (isTracing) {
      RenderCoreSystrace.beginSection("UnmountItem: " + unit.getDescription());
    }

    final Object content = item.getContent();

    final boolean hasUnmountDelegate =
        mUnmountDelegateExtension != null
            && mUnmountDelegateExtension.shouldDelegateUnmount(
                mMountDelegate.getUnmountDelegateExtensionState(), item);

    // Recursively unmount mounted children items.
    // This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    // sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    // sub tree, that will still have mounted items. (Different sequence number on LayoutOutput id)
    if (node.getChildrenCount() > 0) {

      final Host host = (Host) content;
      for (int i = node.getChildrenCount() - 1; i >= 0; i--) {
        unmountItem(mIndexToItemMap.get(node.getChildAt(i).getRenderUnit().getId()), hostsByMarker);
      }

      if (!hasUnmountDelegate && host.getMountItemCount() > 0) {
        final LayoutOutput output = getLayoutOutput(item);
        final Component component = output.getComponent();
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.ERROR,
            "UnmountItem:ChildsNotUnmounted",
            "Recursively unmounting items from a ComponentHost, left some items behind maybe because not tracked by its MountState"
                + ", component: "
                + component.getSimpleName());
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    // The root host item should never be unmounted as it's a reference
    // to the top-level LithoView.
    if (id == ROOT_HOST_ID) {
      unbindAndUnmountLifecycle(item);
      if (isTracing) {
        RenderCoreSystrace.endSection();
      }
      return;
    } else {
      mIndexToItemMap.remove(id);
    }

    final ComponentHost host = (ComponentHost) item.getHost();
    final LayoutOutput output = getLayoutOutput(item);
    final Component component = output.getComponent();

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.delete(id);
    }

    if (isHostSpec(component)) {
      final ComponentHost componentHost = (ComponentHost) content;
      hostsByMarker.removeAt(hostsByMarker.indexOfValue(componentHost));
    }

    if (hasUnmountDelegate) {
      mUnmountDelegateExtension.unmount(
          mMountDelegate.getUnmountDelegateExtensionState(), item, host);
    } else {

      if (isTracing) {
        RenderCoreSystrace.beginSection(
            "UnmountItem:remove: " + item.getRenderTreeNode().getRenderUnit().getDescription());
      }
      if (mShouldUsePositionInParent) {
        final int index = item.getRenderTreeNode().getPositionInParent();
        unmount(host, index, item);
      } else {

        // Find the index in the layout state to unmount
        for (int mountIndex = mLayoutOutputsIds.length - 1; mountIndex >= 0; mountIndex--) {
          if (mLayoutOutputsIds[mountIndex] == id) {
            unmount(host, mountIndex, item);
            break;
          }
        }
      }
      if (isTracing) {
        RenderCoreSystrace.endSection();
      }

      unbindMountItem(item);
    }

    if (mMountStats.isLoggingEnabled) {
      mMountStats.unmountedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
      mMountStats.unmountedNames.add(component.getSimpleName());
      mMountStats.unmountedCount++;
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public void unbindMountItem(MountItem mountItem) {
    unbindAndUnmountLifecycle(mountItem);
    try {
      getMountData(mountItem)
          .releaseMountContent(mContext.getAndroidContext(), mountItem, "unmountItem", this);
    } catch (LithoMountData.ReleasingReleasedMountContentException e) {
      throw new RuntimeException(e.getMessage() + " " + getMountItemDebugMessage(mountItem));
    }
  }

  private void unbindAndUnmountLifecycle(MountItem item) {
    final LayoutOutput layoutOutput = getLayoutOutput(item);
    final Component component = layoutOutput.getComponent();
    final Object content = item.getContent();
    final ComponentContext context = getContextForComponent(item.getRenderTreeNode());
    final boolean isTracing = RenderCoreSystrace.isEnabled();

    // Call the component's unmount() method.
    RenderUnit unit = item.getRenderTreeNode().getRenderUnit();
    if (isTracing) {
      RenderCoreSystrace.beginSection("UnmountItem:unbind: " + unit.getDescription());
    }
    if (item.isBound()) {
      unbindComponentFromContent(item, component, content);
    }
    if (isTracing) {
      RenderCoreSystrace.endSection();
    }

    if (isTracing) {
      RenderCoreSystrace.beginSection("UnmountItem:unmount: " + unit.getDescription());
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

    maybeUnsetViewAttributes(item);
    // For RCMS: onUnmountItem
    final LithoLayoutData layoutData = (LithoLayoutData) item.getRenderTreeNode().getLayoutData();
    component.unmount(context, content, (InterStagePropsContainer) layoutData.mLayoutData);
    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public boolean isRootItem(int position) {
    final MountItem mountItem = getItemAt(position);
    if (mountItem == null) {
      return false;
    }

    return mountItem == mIndexToItemMap.get(ROOT_HOST_ID);
  }

  @Override
  public @Nullable MountItem getRootItem() {
    return mIndexToItemMap != null ? mIndexToItemMap.get(ROOT_HOST_ID) : null;
  }

  @Nullable
  MountItem getItemAt(int i) {
    assertMainThread();

    // TODO simplify when replacing with getContent.
    if (mIndexToItemMap == null || mLayoutOutputsIds == null) {
      return null;
    }

    if (i >= mLayoutOutputsIds.length) {
      return null;
    }

    return mIndexToItemMap.get(mLayoutOutputsIds[i]);
  }

  @Override
  public Object getContentAt(int i) {
    final MountItem mountItem = getItemAt(i);
    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  @Override
  public @Nullable Object getContentById(long id) {
    if (mIndexToItemMap == null) {
      return null;
    }

    final MountItem mountItem = mIndexToItemMap.get(id);

    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  public void clearLastMountedTree() {
    if (mTransitionsExtension != null) {
      TransitionsExtension.clearLastMountedTreeId(mTransitionsExtensionState);
    }
    mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
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
   * Component}.
   */
  void unbind() {
    assertMainThread();
    if (mLayoutOutputsIds == null) {
      return;
    }

    boolean isTracing = RenderCoreSystrace.isEnabled();
    if (isTracing) {
      RenderCoreSystrace.beginSection("MountState.unbind");
      RenderCoreSystrace.beginSection("MountState.unbindAllContent");
    }

    for (int i = 0, size = mLayoutOutputsIds.length; i < size; i++) {
      MountItem mountItem = getItemAt(i);

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindComponentFromContent(
          mountItem, getLayoutOutput(mountItem).getComponent(), mountItem.getContent());
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.beginSection("MountState.unbindExtensions");
    }

    clearVisibilityItems();

    if (mVisibilityExtension != null) {
      mVisibilityExtension.onUnbind(mVisibilityExtensionState);
    }

    if (mTransitionsExtension != null) {
      mTransitionsExtension.onUnbind(mTransitionsExtensionState);
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
      RenderCoreSystrace.endSection();
    }
  }

  @Override
  public void detach() {
    assertMainThread();
    unbind();
  }

  @Override
  public void attach() {
    rebind();
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

    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("MountState.bind");
    }

    for (int i = 0, size = mLayoutOutputsIds.length; i < size; i++) {
      final MountItem mountItem = getItemAt(i);
      if (mountItem == null || mountItem.isBound()) {
        continue;
      }

      final Component component = getLayoutOutput(mountItem).getComponent();
      final Object content = mountItem.getContent();
      final LithoLayoutData layoutData =
          (LithoLayoutData) mountItem.getRenderTreeNode().getLayoutData();
      bindComponentToContent(
          mountItem, component, getComponentContext(mountItem), layoutData, content);

      if (content instanceof View
          && !(content instanceof ComponentHost)
          && ((View) content).isLayoutRequested()) {
        final View view = (View) content;
        applyBoundsToMountContent(
            view, view.getLeft(), view.getTop(), view.getRight(), view.getBottom(), true);
      }
    }

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }

  private boolean isAnimationLocked(RenderTreeNode renderTreeNode) {
    if (mTransitionsExtension != null) {
      if (mTransitionsExtensionState == null) {
        throw new IllegalStateException("Need a state when using the TransitionsExtension.");
      }
      return mTransitionsExtensionState.ownsReference(renderTreeNode.getRenderUnit().getId());
    }
    return false;
  }

  /**
   * @return true if this method did all the work that was necessary and there is no other content
   *     that needs mounting/unmounting in this mount step. If false then a full mount step should
   *     take place.
   */
  private void performIncrementalMount(
      LayoutState layoutState, Rect localVisibleRect, boolean processVisibilityOutputs) {

    final boolean isTracing = RenderCoreSystrace.isEnabled();

    if (isTracing) {
      RenderCoreSystrace.beginSection("performIncrementalMount");
    }

    final ArrayList<IncrementalMountOutput> layoutOutputTops =
        layoutState.getOutputsOrderedByTopBounds();
    final ArrayList<IncrementalMountOutput> layoutOutputBottoms =
        layoutState.getOutputsOrderedByBottomBounds();
    final int count = layoutState.getMountableOutputCount();

    if (localVisibleRect.top >= 0 || mPreviousLocalVisibleRect.top >= 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= layoutOutputBottoms.get(mPreviousBottomsIndex).getBounds().bottom) {

        IncrementalMountOutput output = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final RenderTreeNode node = layoutState.getRenderTreeNode(output);
        final long id = node.getRenderUnit().getId();
        final int layoutOutputIndex = layoutState.getPositionForId(id);
        if (!isAnimationLocked(node) && !output.excludeFromIncrementalMount()) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              <= layoutOutputBottoms.get(mPreviousBottomsIndex - 1).getBounds().bottom) {
        mPreviousBottomsIndex--;
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputBottoms.get(mPreviousBottomsIndex));
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final int layoutOutputIndex = layoutState.getPositionForId(node.getRenderUnit().getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getPositionForId(node.getRenderUnit().getId()),
              node,
              layoutOutput,
              layoutState);
          mComponentIdsMountedInThisFrame.add(node.getRenderUnit().getId());
        }
      }
    }

    final int height = mLithoView.getHeight();
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom >= layoutOutputTops.get(mPreviousTopsIndex).getBounds().top) {
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputTops.get(mPreviousTopsIndex));
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final int layoutOutputIndex = layoutState.getPositionForId(node.getRenderUnit().getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getPositionForId(node.getRenderUnit().getId()),
              node,
              layoutOutput,
              layoutState);
          mComponentIdsMountedInThisFrame.add(node.getRenderUnit().getId());
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              < layoutOutputTops.get(mPreviousTopsIndex - 1).getBounds().top) {
        mPreviousTopsIndex--;

        IncrementalMountOutput output = layoutOutputTops.get(mPreviousTopsIndex);
        final RenderTreeNode node = layoutState.getRenderTreeNode(output);
        final long id = node.getRenderUnit().getId();
        final int layoutOutputIndex = layoutState.getPositionForId(id);
        if (!isAnimationLocked(node) && !output.excludeFromIncrementalMount()) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
      }
    }

    if (!mLithoView.skipNotifyVisibleBoundsChangedCalls()) {
      for (int i = 0, size = mCanMountIncrementallyMountItems.size(); i < size; i++) {
        final MountItem mountItem = mCanMountIncrementallyMountItems.valueAt(i);
        final long layoutOutputId = mCanMountIncrementallyMountItems.keyAt(i);
        if (!mComponentIdsMountedInThisFrame.contains(layoutOutputId)) {
          final int layoutOutputPosition = layoutState.getPositionForId(layoutOutputId);
          if (layoutOutputPosition != -1) {
            mountItemIncrementally(mountItem, processVisibilityOutputs);
          }
        }
      }
    }

    mComponentIdsMountedInThisFrame.clear();

    if (isTracing) {
      RenderCoreSystrace.endSection();
    }
  }
  /**
   * Collect transitions from layout time, mount time and from state updates.
   *
   * @param layoutState that is going to be mounted.
   */
  void collectAllTransitions(LayoutState layoutState) {
    assertMainThread();

    if (mTransitionsExtension != null) {
      TransitionsExtension.collectAllTransitions(mTransitionsExtensionState, layoutState);
      return;
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
  private ComponentContext getContextForComponent(RenderTreeNode node) {
    ComponentContext c = getComponentContext(node);
    return c == null ? mContext : c;
  }

  private void bindComponentToContent(
      final MountItem mountItem,
      final Component component,
      final ComponentContext context,
      final LithoLayoutData layoutData,
      final Object content) {

    component.bind(
        getContextForComponent(mountItem.getRenderTreeNode()),
        content,
        (InterStagePropsContainer) layoutData.mLayoutData);
    mDynamicPropsManager.onBindComponentToContent(component, context, content);
    // For RCMS: onBindItem
    mountItem.setIsBound(true);
  }

  private void unbindComponentFromContent(
      MountItem mountItem, Component component, Object content) {
    mDynamicPropsManager.onUnbindComponent(component, content);
    RenderTreeNode node = mountItem.getRenderTreeNode();
    // For RCMS: onUnbindItem
    component.unbind(
        getContextForComponent(node),
        content,
        LithoLayoutData.getInterStageProps(node.getLayoutData()));
    mountItem.setIsBound(false);
  }

  @VisibleForTesting
  DynamicPropsManager getDynamicPropsManager() {
    return mDynamicPropsManager;
  }

  private Object acquireHostComponentContent(Context context, HostComponent component) {
    if (ComponentsConfiguration.hostComponentRecyclingByWindowIsEnabled) {
      return MountItemsPool.acquireHostMountContent(
          context, mLithoView.getWindowToken(), component);
    } else if (ComponentsConfiguration.hostComponentRecyclingByMountStateIsEnabled) {
      if (mHostMountContentPool != null) {
        return mHostMountContentPool.acquire(context, component);
      } else {
        return component.createMountContent(context);
      }
    } else if (ComponentsConfiguration.unsafeHostComponentRecyclingIsEnabled) {
      return MountItemsPool.acquireMountContent(context, component);
    } else {
      // Otherwise, recycling is disabled for hosts
      return component.createMountContent(context);
    }
  }

  void releaseHostComponentContent(Context context, HostComponent component, Object content) {
    if (ComponentsConfiguration.hostComponentRecyclingByWindowIsEnabled) {
      MountItemsPool.releaseHostMountContent(
          context, mLithoView.getWindowToken(), component, content);
    } else if (ComponentsConfiguration.hostComponentRecyclingByMountStateIsEnabled) {
      if (mHostMountContentPool == null) {
        mHostMountContentPool = (HostMountContentPool) component.createRecyclingPool();
      }
      mHostMountContentPool.release(content);
    } else if (ComponentsConfiguration.unsafeHostComponentRecyclingIsEnabled) {
      MountItemsPool.release(context, component, content);
    } else {
      // Otherwise, recycling is disabled for hosts
    }
  }
}
