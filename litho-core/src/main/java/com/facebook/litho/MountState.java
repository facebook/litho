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

import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;
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
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LithoMountData.getMountData;
import static com.facebook.litho.LithoMountData.isViewClickable;
import static com.facebook.litho.LithoMountData.isViewEnabled;
import static com.facebook.litho.LithoMountData.isViewFocusable;
import static com.facebook.litho.LithoMountData.isViewLongClickable;
import static com.facebook.litho.LithoMountData.isViewSelected;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import androidx.core.view.ViewCompat;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.ErrorReporter;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.LogLevel;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
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
  private static final Rect sTempRect = new Rect();

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
  private final boolean mUseStatelessComponent = ComponentsConfiguration.useStatelessComponent;
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private int mLastMountedComponentTreeId = ComponentTree.INVALID_ID;
  private @Nullable LayoutState mLayoutState;
  private @Nullable LayoutState mLastMountedLayoutState;
  private int mLastDisappearRangeStart = -1;
  private int mLastDisappearRangeEnd = -1;

  private final MountItem mRootHostMountItem;

  private final VisibilityMountExtension mVisibilityExtension;
  private final ExtensionState mVisibilityExtensionState;
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();

  private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();
  private @Nullable MountDelegate mMountDelegate;
  private @Nullable UnmountDelegateExtension mUnmountDelegateExtension;
  private @Nullable TransitionsExtension mTransitionsExtension;
  private @Nullable ExtensionState mTransitionsExtensionState;

  private @ComponentTree.RecyclingMode int mRecyclingMode = ComponentTree.RecyclingMode.DEFAULT;

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

    mVisibilityExtension = VisibilityMountExtension.getInstance();
    mVisibilityExtensionState = mVisibilityExtension.createExtensionState(new MountDelegate(this));
    VisibilityMountExtension.setRootHost(mVisibilityExtensionState, mLithoView);

    // Using Incremental Mount Extension and the Transition Extension here is not allowed.
    if (!mLithoView.usingExtensionsWithMountDelegate()) {
      mTransitionsExtension = TransitionsExtension.getInstance();
      registerMountDelegateExtension(mTransitionsExtension);
      mTransitionsExtensionState = getExtensionState(mTransitionsExtension);
    }
  }

  @Override
  public void registerMountDelegateExtension(MountExtension mountExtension) {
    if (mMountDelegate == null) {
      mMountDelegate = new MountDelegate(this);
    }
    mMountDelegate.addExtension(mountExtension);
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

  void setRecyclingMode(@ComponentTree.RecyclingMode int recyclingMode) {
    this.mRecyclingMode = recyclingMode;
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
    final boolean isVisibilityProcessingEnabled = componentTree.isVisibilityProcessingEnabled();

    assertMainThread();

    if (layoutState == null) {
      throw new IllegalStateException("Trying to mount a null layoutState");
    }

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

    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      String sectionName = isIncrementalMountEnabled ? "incrementalMount" : "mount";
      ComponentsSystrace.beginSectionWithArgs(sectionName)
          .arg("treeId", layoutState.getComponentTreeId())
          .arg("component", componentTree.getSimpleName())
          .arg("logTag", componentTree.getContext().getLogTag())
          .flush();
      // We also would like to trace this section attributed with component name
      // for component share analysis.
      ComponentsSystrace.beginSection(sectionName + "_" + componentTree.getSimpleName());
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
        final RenderTreeNode node = layoutState.getMountableOutputAt(i);
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final Component component = layoutOutput.getComponent();
        if (isTracing) {
          ComponentsSystrace.beginSection(component.getSimpleName());
        }

        final MountItem currentMountItem = getItemAt(i);
        final boolean isMounted = currentMountItem != null;
        final boolean isRoot = currentMountItem != null && currentMountItem == rootMountItem;
        final boolean isMountable =
            !isIncrementalMountEnabled
                || isMountedHostWithChildContent(currentMountItem)
                || Rect.intersects(localVisibleRect, layoutOutput.getBounds())
                || isAnimationLocked(node)
                || isRoot;

        if (isMountable && !isMounted) {
          mountLayoutOutput(i, node, layoutOutput, layoutState);
          if (isIncrementalMountEnabled) {
            applyMountBinders(layoutOutput, getItemAt(i), i);
          }
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
                    node, currentMountItem, useUpdateValueFromLayoutOutput, componentTreeId, i);
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

    afterMountMaybeUpdateAnimations();

    if (isVisibilityProcessingEnabled) {
      if (isTracing) {
        ComponentsSystrace.beginSection("processVisibilityOutputs");
      }
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("EVENT_PROCESS_VISIBILITY_OUTPUTS_START");
      }
      processVisibilityOutputs(
          layoutState, localVisibleRect, mPreviousLocalVisibleRect, mIsDirty, mountPerfEvent);
      if (mountPerfEvent != null) {
        mountPerfEvent.markerPoint("EVENT_PROCESS_VISIBILITY_OUTPUTS_END");
      }
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    final boolean wasDirty = mIsDirty;
    mIsDirty = false;
    mNeedsRemount = false;
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }

    mLastMountedLayoutState = null;
    mLastMountedComponentTreeId = componentTreeId;
    mLastMountedLayoutState = layoutState;

    processTestOutputs(layoutState);

    if (mountPerfEvent != null) {
      logMountPerfEvent(logger, mountPerfEvent, wasDirty);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
      ComponentsSystrace.endSection();
    }
    LithoStats.incrementComponentMountCount();

    mIsMounting = false;
  }

  private void afterMountMaybeUpdateAnimations() {
    if (mTransitionsExtension != null && mIsDirty) {
      mTransitionsExtension.afterMount(mTransitionsExtensionState);
    }
  }

  @Override
  public void mount(RenderTree renderTree) {
    final LayoutState layoutState = (LayoutState) renderTree.getRenderTreeData();
    mount(layoutState, true);
  }

  /**
   * Mount only. Similar shape to RenderCore's mount. For extras such as incremental mount,
   * visibility outputs etc register an extension. To do: extract transitions logic from here.
   */
  void mount(LayoutState layoutState, boolean processVisibilityOutputs) {
    assertMainThread();

    if (layoutState == null) {
      throw new IllegalStateException("Trying to mount a null layoutState");
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

    final ComponentTree componentTree = mLithoView.getComponentTree();
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSectionWithArgs("mount")
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
      mLastMountedLayoutState = null;
    }

    final PerfEvent mountPerfEvent =
        logger == null
            ? null
            : LogTreePopulator.populatePerfEventFromLogger(
                componentTree.getContext(),
                logger,
                logger.newPerformanceEvent(componentTree.getContext(), EVENT_MOUNT));

    // Prepare the data structure for the new LayoutState and removes mountItems
    // that are not present anymore if isUpdateMountInPlace is enabled.
    if (mountPerfEvent != null) {
      mountPerfEvent.markerPoint("PREPARE_MOUNT_START");
    }
    prepareMount(layoutState, mountPerfEvent);
    if (mountPerfEvent != null) {
      mountPerfEvent.markerPoint("PREPARE_MOUNT_END");
    }

    mMountStats.reset();
    if (mountPerfEvent != null && logger.isTracing(mountPerfEvent)) {
      mMountStats.enableLogging();
    }

    for (int i = 0, size = layoutState.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = layoutState.getMountableOutputAt(i);
      final LayoutOutput layoutOutput = getLayoutOutput(renderTreeNode);
      final Component component = layoutOutput.getComponent();
      if (isTracing) {
        ComponentsSystrace.beginSection(component.getSimpleName());
      }

      final MountItem currentMountItem = getItemAt(i);
      final boolean isMounted = currentMountItem != null;
      final boolean isMountable = isMountable(renderTreeNode, i);

      if (!isMountable) {
        if (isMounted) {
          unmountItem(i, mHostsByMarker);
        }
      } else if (!isMounted) {
        mountLayoutOutput(i, renderTreeNode, layoutOutput, layoutState);
        applyMountBinders(layoutOutput, getItemAt(i), i);
      } else {
        final boolean useUpdateValueFromLayoutOutput =
            mLastMountedLayoutState != null
                && mLastMountedLayoutState.getId() == layoutState.getPreviousLayoutStateId();

        final long startTime = System.nanoTime();
        final boolean itemUpdated =
            updateMountItemIfNeeded(
                renderTreeNode,
                currentMountItem,
                useUpdateValueFromLayoutOutput,
                componentTreeId,
                i);

        if (mMountStats.isLoggingEnabled) {
          if (itemUpdated) {
            mMountStats.updatedNames.add(component.getSimpleName());
            mMountStats.updatedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
            mMountStats.updatedCount++;
          } else {
            mMountStats.noOpCount++;
          }
        }

        applyBindBinders(currentMountItem);
      }

      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    final boolean wasDirty = mIsDirty;
    mIsDirty = false;
    mNeedsRemount = false;

    mLastMountedLayoutState = null;
    mLastMountedComponentTreeId = componentTreeId;
    mLastMountedLayoutState = layoutState;

    if (mountPerfEvent != null) {
      logMountPerfEvent(logger, mountPerfEvent, wasDirty);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
    LithoStats.incrementComponentMountCount();

    mIsMounting = false;
  }

  private void applyMountBinders(LayoutOutput layoutOutput, MountItem mountItem, int position) {
    if (mTransitionsExtension != null) {
      mTransitionsExtension.onMountItem(
          mTransitionsExtensionState,
          mountItem.getRenderTreeNode().getRenderUnit(),
          mountItem.getContent(),
          mountItem.getRenderTreeNode().getLayoutData());
    } else if (mMountDelegate != null) {
      mMountDelegate.onMountItem(
          mountItem.getRenderTreeNode().getRenderUnit(),
          mountItem.getContent(),
          mountItem.getRenderTreeNode().getLayoutData());
    }
  }

  private void applyBindBinders(MountItem mountItem) {
    if (mMountDelegate == null) {
      return;
    }
  }

  private void applyUnbindBinders(LayoutOutput output, MountItem mountItem) {
    if (mTransitionsExtension != null) {
      mTransitionsExtension.onUnbindItem(
          mTransitionsExtensionState,
          mountItem.getRenderTreeNode().getRenderUnit(),
          output,
          mountItem.getContent());
    } else if (mMountDelegate != null) {
      mMountDelegate.onUnmountItem(
          mountItem.getRenderTreeNode().getRenderUnit(), output, mountItem.getContent());
    }
  }

  private boolean isMountable(RenderTreeNode renderTreeNode, int position) {
    if (mMountDelegate == null) {
      return true;
    }

    final boolean isLockedForMount = mMountDelegate.maybeLockForMount(renderTreeNode, position);

    return isLockedForMount;
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
    if (item == null) {
      return;
    }
    final int position = getLayoutOutput(item).getIndex();
    unmountItem(position, mHostsByMarker);
  }

  private void logMountPerfEvent(
      ComponentsLogger logger, PerfEvent mountPerfEvent, boolean isDirty) {
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

    logger.logPerfEvent(mountPerfEvent);
  }

  void processVisibilityOutputs(
      LayoutState layoutState,
      @Nullable Rect localVisibleRect,
      Rect previousLocalVisibleRect,
      boolean isDirty,
      @Nullable PerfEvent mountPerfEvent) {
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
  public ExtensionState getExtensionState(MountExtension mountExtension) {
    if (mountExtension == mVisibilityExtension) {
      return mVisibilityExtensionState;
    }

    return mMountDelegate.getExtensionState(mountExtension);
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
      if (localVisibleRect.bottom
          <= layoutState.getLayoutOutput(layoutOutputTops.get(i)).getBounds().top) {
        mPreviousTopsIndex = i;
        break;
      }
    }

    mPreviousBottomsIndex = layoutState.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top
          < layoutState.getLayoutOutput(layoutOutputBottoms.get(i)).getBounds().bottom) {
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
    VisibilityMountExtension.clearVisibilityItems(mVisibilityExtensionState);
  }

  private void registerHost(long id, ComponentHost host) {
    mHostsByMarker.put(id, host);
  }

  private static int computeRectArea(Rect rect) {
    return rect.isEmpty() ? 0 : (rect.width() * rect.height());
  }

  private boolean updateMountItemIfNeeded(
      RenderTreeNode node,
      MountItem currentMountItem,
      boolean useUpdateValueFromLayoutOutput,
      int componentTreeId,
      int index) {
    final LayoutOutput nextLayoutOutput = getLayoutOutput(node);
    final Component layoutOutputComponent = nextLayoutOutput.getComponent();
    final LayoutOutput currentLayoutOutput = getLayoutOutput(currentMountItem);
    final Component itemComponent = currentLayoutOutput.getComponent();
    final Object currentContent = currentMountItem.getContent();
    final ComponentHost host = (ComponentHost) currentMountItem.getHost();
    if (layoutOutputComponent == null) {
      throw new RuntimeException("Trying to update a MountItem with a null Component.");
    }

    // 1. Check if the mount item generated from the old component should be updated.
    final boolean shouldUpdate =
        shouldUpdateMountItem(
            nextLayoutOutput, currentLayoutOutput, useUpdateValueFromLayoutOutput);

    final boolean shouldUpdateViewInfo =
        shouldUpdate || shouldUpdateViewInfo(nextLayoutOutput, currentLayoutOutput);

    // 2. Reset all the properties like click handler, content description and tags related to
    // this item if it needs to be updated. the update mount item will re-set the new ones.

    if (shouldUpdateViewInfo) {
      maybeUnsetViewAttributes(currentMountItem);
    }

    // 3. We will re-bind this later in 7 regardless so let's make sure it's currently unbound.
    if (currentMountItem.isBound()) {
      unbindComponentFromContent(currentMountItem, itemComponent, currentMountItem.getContent());
    }

    // 4. Re initialize the MountItem internal state with the new attributes from LayoutOutput
    currentMountItem.update(node);

    // 5. If the mount item is not valid for this component update its content and view attributes.
    if (shouldUpdate) {
      updateMountedContent(currentMountItem, nextLayoutOutput, currentLayoutOutput);
    }

    if (shouldUpdateViewInfo) {
      setViewAttributes(currentMountItem);
    }

    // 6. Set the mounted content on the Component and call the bind callback.
    bindComponentToContent(currentMountItem, layoutOutputComponent, currentContent);

    // 7. Update the bounds of the mounted content. This needs to be done regardless of whether
    // the component has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedLayoutOutput(nextLayoutOutput, currentMountItem);

    if (currentMountItem.getContent() instanceof Drawable) {
      maybeSetDrawableState(
          host,
          (Drawable) currentContent,
          currentLayoutOutput.getFlags(),
          currentLayoutOutput.getNodeInfo());
    }

    return shouldUpdate;
  }

  static boolean shouldUpdateViewInfo(
      final LayoutOutput nextLayoutOutput, final LayoutOutput currentLayoutOutput) {

    final ViewNodeInfo nextViewNodeInfo = nextLayoutOutput.getViewNodeInfo();
    final ViewNodeInfo currentViewNodeInfo = currentLayoutOutput.getViewNodeInfo();
    if ((currentViewNodeInfo == null && nextViewNodeInfo != null)
        || (currentViewNodeInfo != null && !currentViewNodeInfo.isEquivalentTo(nextViewNodeInfo))) {

      return true;
    }

    final NodeInfo nextNodeInfo = nextLayoutOutput.getNodeInfo();
    final NodeInfo currentNodeInfo = currentLayoutOutput.getNodeInfo();
    return (currentNodeInfo == null && nextNodeInfo != null)
        || (currentNodeInfo != null && !currentNodeInfo.isEquivalentTo(nextNodeInfo));
  }

  static boolean shouldUpdateMountItem(
      final LayoutOutput nextLayoutOutput,
      final LayoutOutput currentLayoutOutput,
      final boolean useUpdateValueFromLayoutOutput) {
    @LayoutOutput.UpdateState final int updateState = nextLayoutOutput.getUpdateState();
    final Component currentComponent = currentLayoutOutput.getComponent();
    final Component nextComponent = nextLayoutOutput.getComponent();

    if (ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange
        && nextLayoutOutput.getOrientation() != currentLayoutOutput.getOrientation()) {
      return true;
    }

    // If the two components have different sizes and the mounted content depends on the size we
    // just return true immediately.
    if (nextComponent.isMountSizeDependent() && !sameSize(nextLayoutOutput, currentLayoutOutput)) {
      return true;
    }

    if (useUpdateValueFromLayoutOutput) {
      if (updateState == LayoutOutput.STATE_UPDATED) {

        // Check for incompatible ReferenceLifecycle.
        return currentComponent instanceof DrawableComponent
            && nextComponent instanceof DrawableComponent
            && currentComponent.shouldComponentUpdate(
                currentLayoutOutput.getScopedContext(),
                currentComponent,
                nextLayoutOutput.getScopedContext(),
                nextComponent);
      } else if (updateState == LayoutOutput.STATE_DIRTY) {
        return true;
      }
    }

    return currentComponent.shouldComponentUpdate(
        currentLayoutOutput.getScopedContext(),
        currentComponent,
        nextLayoutOutput.getScopedContext(),
        nextComponent);
  }

  static boolean sameSize(final LayoutOutput nextOutput, final LayoutOutput currentOutput) {
    final Rect nextBounds = nextOutput.getBounds();
    final Rect currentBounds = currentOutput.getBounds();

    return nextBounds.width() == currentBounds.width()
        && nextBounds.height() == currentBounds.height();
  }

  private static void updateBoundsForMountedLayoutOutput(
      LayoutOutput layoutOutput, MountItem item) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (layoutOutput.getId() == ROOT_HOST_ID) {
      return;
    }

    layoutOutput.getMountBounds(sTempRect);

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
  private void prepareMount(LayoutState layoutState, @Nullable PerfEvent perfEvent) {
    final boolean isTracing = ComponentsSystrace.isTracing();

    if (isTracing) {
      ComponentsSystrace.beginSection("prepareMount");
    }

    final PrepareMountStats stats = unmountOrMoveOldItems(layoutState);

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
      mLayoutOutputsIds[i] = getLayoutOutput(layoutState.getMountableOutputAt(i)).getId();
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
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

    // Traversing from the beginning since mLayoutOutputsIds unmounting won't remove entries there
    // but only from mIndexToItemMap. If an host changes we're going to unmount it and recursively
    // all its mounted children.
    for (int i = 0; i < mLayoutOutputsIds.length; i++) {
      final LayoutOutput newLayoutOutput = newLayoutState.getLayoutOutput(mLayoutOutputsIds[i]);
      final int newPosition = newLayoutOutput == null ? -1 : newLayoutOutput.getIndex();

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
      MountItem item, LayoutOutput newLayoutOutput, LayoutOutput previousLayoutOutput) {
    final Component newComponent = newLayoutOutput.getComponent();
    final Component previousComponent = previousLayoutOutput.getComponent();
    if (isHostSpec(newComponent)) {
      return;
    }

    final Object previousContent = item.getContent();

    // Call unmount and mount in sequence to make sure all the the resources are correctly
    // de-allocated. It's possible for previousContent to equal null - when the root is
    // interactive we create a LayoutOutput without content in order to set up click handling.
    previousComponent.unmount(
        getContextForComponent(previousComponent, previousLayoutOutput), previousContent);
    newComponent.mount(getContextForComponent(newComponent, newLayoutOutput), previousContent);
  }

  private void mountLayoutOutput(
      final int index,
      final RenderTreeNode node,
      final LayoutOutput layoutOutput,
      final LayoutState layoutState) {
    // 1. Resolve the correct host to mount our content to.
    final long startTime = System.nanoTime();
    final long hostMarker = layoutOutput.getHostMarker();
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
    if (component == null) {
      throw new RuntimeException("Trying to mount a LayoutOutput with a null Component.");
    }
    final Object content =
        ComponentsPools.acquireMountContent(
            mContext.getAndroidContext(), component, mRecyclingMode);

    final ComponentContext context = getContextForComponent(component, layoutOutput);
    component.mount(context, content);

    // 3. If it's a ComponentHost, add the mounted View to the list of Hosts.
    if (isHostSpec(component)) {
      ComponentHost componentHost = (ComponentHost) content;
      registerHost(layoutOutput.getId(), componentHost);
    }

    // 4. Mount the content into the selected host.
    final MountItem item = mountContent(index, component, content, host, node, layoutOutput);

    // 5. Notify the component that mounting has completed
    bindComponentToContent(item, component, content);

    // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    layoutOutput.getMountBounds(sTempRect);
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

      final ComponentContext scopedContext =
          mUseStatelessComponent
              ? layoutOutput.getScopedContext()
              : component.getScopedContext(null, null);

      mMountStats.extras.add(
          LogTreePopulator.getAnnotationBundleFromLogger(scopedContext, context.getLogger()));
    }
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first LayoutOutput).
  private MountItem mountContent(
      int index,
      Component component,
      Object content,
      ComponentHost host,
      RenderTreeNode node,
      LayoutOutput layoutOutput) {

    final MountItem item = new MountItem(node, host, content);
    item.setMountData(new LithoMountData(content));

    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIndexToItemMap.put(mLayoutOutputsIds[index], item);

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.put(mLayoutOutputsIds[index], item);
    }

    mount(host, index, content, item, layoutOutput);
    setViewAttributes(item);

    return item;
  }

  private static void mount(
      final ComponentHost host,
      final int index,
      final Object content,
      final MountItem item,
      final LayoutOutput output) {
    output.getMountBounds(sTempRect);
    host.mount(index, item, sTempRect);
  }

  private static void unmount(
      final ComponentHost host,
      final int index,
      final Object content,
      final MountItem item,
      final LayoutOutput output) {
    host.unmount(index, item);
  }

  private static void applyBoundsToMountContent(
      Object content, int left, int top, int right, int bottom, boolean force) {
    assertMainThread();

    BoundsUtils.applyBoundsToMountContent(left, top, right, bottom, null, content, force);
  }

  private static void setViewAttributes(MountItem item) {
    setViewAttributes(item.getContent(), getLayoutOutput(item));
  }

  static void setViewAttributes(Object content, LayoutOutput output) {
    final Component component = output.getComponent();
    if (!isMountViewSpec(component)) {
      return;
    }

    final View view = (View) content;
    final NodeInfo nodeInfo = output.getNodeInfo();

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
      setTransitionName(view, nodeInfo.getTransitionName());
    }

    setImportantForAccessibility(view, output.getImportantForAccessibility());

    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo != null) {
      final boolean isHostSpec = isHostSpec(component);
      setViewLayerType(view, viewNodeInfo);
      setViewStateListAnimator(view, viewNodeInfo);
      if (LayoutOutput.areDrawableOutputsDisabled(output.getFlags())) {
        setViewBackground(view, viewNodeInfo);
        setViewForeground(view, viewNodeInfo);

        // when background outputs are disabled, they are wrapped by a ComponentHost.
        // A background can set the padding of a view, but ComponentHost should not have
        // any padding because the layout calculation has already accounted for padding by
        // translating the bounds of its children.
        if (isHostSpec) {
          view.setPadding(0, 0, 0, 0);
        }
      }
      if (!isHostSpec) {
        // Set view background, if applicable.  Do this before padding
        // as it otherwise overrides the padding.
        setViewBackground(view, viewNodeInfo);

        setViewPadding(view, viewNodeInfo);

        setViewForeground(view, viewNodeInfo);

        setViewLayoutDirection(view, viewNodeInfo);
      }
    }
  }

  private static void setViewLayerType(final View view, final ViewNodeInfo info) {
    final int type = info.getLayerType();
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(info.getLayerType(), info.getLayoutPaint());
    }
  }

  private static void unsetViewLayerType(final View view, final int mountFlags) {
    int type = LithoMountData.getOriginalLayerType(mountFlags);
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      view.setLayerType(type, null);
    }
  }

  private static void maybeUnsetViewAttributes(MountItem item) {
    final LayoutOutput output = getLayoutOutput(item);
    final int flags = getMountData(item).getDefaultAttributeValuesFlags();
    unsetViewAttributes(item.getContent(), output, flags);
  }

  static void unsetViewAttributes(
      final Object content, final LayoutOutput output, final int mountFlags) {
    final Component component = output.getComponent();
    final boolean isHostView = isHostSpec(component);

    if (!isMountViewSpec(component)) {
      return;
    }

    final View view = (View) content;
    final NodeInfo nodeInfo = output.getNodeInfo();

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

    view.setClickable(isViewClickable(mountFlags));
    view.setLongClickable(isViewLongClickable(mountFlags));

    unsetFocusable(view, mountFlags);
    unsetEnabled(view, mountFlags);
    unsetSelected(view, mountFlags);

    if (output.getImportantForAccessibility() != IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      unsetImportantForAccessibility(view);
    }

    unsetAccessibilityDelegate(view);

    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo != null) {
      unsetViewStateListAnimator(view, viewNodeInfo);
      // Host view doesn't set its own padding, but gets absolute positions for inner content from
      // Yoga. Also bg/fg is used as separate drawables instead of using View's bg/fg attribute.
      if (LayoutOutput.areDrawableOutputsDisabled(output.getFlags())) {
        unsetViewBackground(view, viewNodeInfo);
        unsetViewForeground(view, viewNodeInfo);
      }
      if (!isHostView) {
        unsetViewPadding(view, output, viewNodeInfo);
        unsetViewBackground(view, viewNodeInfo);
        unsetViewForeground(view, viewNodeInfo);
        unsetViewLayoutDirection(view);
      }
    }

    unsetViewLayerType(view, mountFlags);
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
  private static void setClickHandler(@Nullable EventHandler<ClickEvent> clickHandler, View view) {
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

  @Nullable
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
      @Nullable EventHandler<LongClickEvent> longClickHandler, View view) {
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

  @Nullable
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
      @Nullable EventHandler<FocusChangedEvent> focusChangeHandler, View view) {
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
  private static void setTouchHandler(@Nullable EventHandler<TouchEvent> touchHandler, View view) {
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
      @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler, View view) {
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

  @Nullable
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

  private static void setViewTag(View view, @Nullable Object viewTag) {
    view.setTag(viewTag);
  }

  private static void setViewTags(View view, @Nullable SparseArray<Object> viewTags) {
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
    view.setTag(null);
  }

  private static void unsetViewTags(View view, @Nullable SparseArray<Object> viewTags) {
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

  private static void setOutlineProvider(View view, @Nullable ViewOutlineProvider outlineProvider) {
    if (outlineProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      view.setOutlineProvider(outlineProvider);
    }
  }

  private static void unsetOutlineProvider(
      View view, @Nullable ViewOutlineProvider outlineProvider) {
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

  private static void setContentDescription(View view, @Nullable CharSequence contentDescription) {
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

  private static void unsetFocusable(View view, int flags) {
    view.setFocusable(isViewFocusable(flags));
  }

  private static void setTransitionName(View view, @Nullable String transitionName) {
    ViewCompat.setTransitionName(view, transitionName);
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

  private static void unsetEnabled(View view, int flags) {
    view.setEnabled(isViewEnabled(flags));
  }

  private static void setSelected(View view, @NodeInfo.SelectedState int selectedState) {
    if (selectedState == NodeInfo.SELECTED_SET_TRUE) {
      view.setSelected(true);
    } else if (selectedState == NodeInfo.SELECTED_SET_FALSE) {
      view.setSelected(false);
    }
  }

  private static void unsetSelected(View view, int flags) {
    view.setSelected(isViewSelected(flags));
  }

  private static void setScale(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isScaleSet()) {
      final float scale = nodeInfo.getScale();
      view.setScaleX(scale);
      view.setScaleY(scale);
    }
  }

  private static void unsetScale(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isScaleSet()) {
      if (view.getScaleX() != 1) {
        view.setScaleX(1);
      }
      if (view.getScaleY() != 1) {
        view.setScaleY(1);
      }
    }
  }

  private static void setAlpha(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isAlphaSet()) {
      view.setAlpha(nodeInfo.getAlpha());
    }
  }

  private static void unsetAlpha(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isAlphaSet() && view.getAlpha() != 1) {
      view.setAlpha(1);
    }
  }

  private static void setRotation(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationSet()) {
      view.setRotation(nodeInfo.getRotation());
    }
  }

  private static void unsetRotation(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationSet() && view.getRotation() != 0) {
      view.setRotation(0);
    }
  }

  private static void setRotationX(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationXSet()) {
      view.setRotationX(nodeInfo.getRotationX());
    }
  }

  private static void unsetRotationX(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationXSet() && view.getRotationX() != 0) {
      view.setRotationX(0);
    }
  }

  private static void setRotationY(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationYSet()) {
      view.setRotationY(nodeInfo.getRotationY());
    }
  }

  private static void unsetRotationY(View view, NodeInfo nodeInfo) {
    if (nodeInfo.isRotationYSet() && view.getRotationY() != 0) {
      view.setRotationY(0);
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

  private static void unsetViewPadding(View view, LayoutOutput output, ViewNodeInfo viewNodeInfo) {
    if (!viewNodeInfo.hasPadding()) {
      return;
    }

    try {
      view.setPadding(0, 0, 0, 0);
    } catch (NullPointerException e) {
      // T53931759 Gathering extra info around this NPE
      ErrorReporter.getInstance()
          .report(
              LogLevel.ERROR,
              "LITHO:NPE:UNSET_PADDING",
              "From component: " + output.getComponent().getSimpleName(),
              e);
    }
  }

  private static void setViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable background = viewNodeInfo.getBackground();
    if (background != null) {
      setBackgroundCompat(view, background);
    }
  }

  private static void unsetViewBackground(View view, ViewNodeInfo viewNodeInfo) {
    final Drawable background = viewNodeInfo.getBackground();
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
    final Component component = getLayoutOutput(item).getComponent();

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
          lithoView.notifyVisibleBoundsChanged(
              new Rect(0, 0, view.getWidth(), view.getHeight()), false);
        } else {
          lithoView.notifyVisibleBoundsChanged();
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
        + ", disappearRange=["
        + mLastDisappearRangeStart
        + ","
        + mLastDisappearRangeEnd
        + "], contentType="
        + (item.getContent() != null ? item.getContent().getClass() : "<null_content>")
        + ", component="
        + (getLayoutOutput(item).getComponent() != null
            ? getLayoutOutput(item).getComponent().getSimpleName()
            : "<null_component>")
        + ", transitionId="
        + getLayoutOutput(item).getTransitionId()
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

    for (int i = mLayoutOutputsIds.length - 1; i >= 0; i--) {
      unmountItem(i, mHostsByMarker);
    }
    mPreviousLocalVisibleRect.setEmpty();
    mNeedsRemount = true;

    if (mMountDelegate != null) {
      mMountDelegate.releaseAllAcquiredReferences();
    }

    if (mVisibilityExtension != null) {
      mVisibilityExtension.onUnmount(mVisibilityExtensionState);
    }

    if (mTransitionsExtension != null) {
      mTransitionsExtension.onUnmount(mTransitionsExtensionState);
    }

    clearLastMountedTree();
    clearVisibilityItems();
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
      maybeUnsetViewAttributes(item);
      return;
    }

    final long layoutOutputId = mLayoutOutputsIds[index];
    mIndexToItemMap.remove(layoutOutputId);

    final Object content = item.getContent();

    final boolean hasUnmountDelegate =
        mUnmountDelegateExtension != null
            && mUnmountDelegateExtension.shouldDelegateUnmount(
                mMountDelegate.getUnmountDelegateExtensionState(), item);

    // Recursively unmount mounted children items.
    // This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    // sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    // sub tree, that will still have mounted items. (Different sequence number on LayoutOutput id)
    if ((content instanceof ComponentHost) && !(content instanceof LithoView)) {
      final ComponentHost host = (ComponentHost) content;

      // Concurrently remove items therefore traverse backwards.
      for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
        final MountItem mountItem = host.getMountItemAt(i);
        if (mIndexToItemMap.indexOfValue(mountItem) == -1) {
          final LayoutOutput output = getLayoutOutput(mountItem);
          final Component component = output.getComponent();
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.ERROR,
              "UnmountItem:ChildNotFound",
              "Child of mount item not found in MountSate mIndexToItemMap"
                  + ", child_component: "
                  + (component != null ? component.getSimpleName() : null)
                  + ", child_transitionId: "
                  + output.getTransitionId());
        }
        final long childLayoutOutputId =
            mIndexToItemMap.keyAt(mIndexToItemMap.indexOfValue(mountItem));

        for (int mountIndex = mLayoutOutputsIds.length - 1; mountIndex >= 0; mountIndex--) {
          if (mLayoutOutputsIds[mountIndex] == childLayoutOutputId) {
            unmountItem(mountIndex, hostsByMarker);
            break;
          }
        }
      }

      if (!hasUnmountDelegate && host.getMountItemCount() > 0) {
        final LayoutOutput output = getLayoutOutput(item);
        final Component component = output.getComponent();
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.ERROR,
            "UnmountItem:ChildsNotUnmounted",
            "Recursively unmounting items from a ComponentHost, left some items behind maybe because not tracked by its MountState"
                + ", component: "
                + (component != null ? component.getSimpleName() : null)
                + ", transitionId: "
                + output.getTransitionId());
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    final ComponentHost host = (ComponentHost) item.getHost();
    final LayoutOutput output = getLayoutOutput(item);
    final Component component = output.getComponent();

    if (component.hasChildLithoViews()) {
      mCanMountIncrementallyMountItems.delete(mLayoutOutputsIds[index]);
    }

    if (isHostSpec(component)) {
      final ComponentHost componentHost = (ComponentHost) content;
      hostsByMarker.removeAt(hostsByMarker.indexOfValue(componentHost));
    }

    if (hasUnmountDelegate) {
      mUnmountDelegateExtension.unmount(
          mMountDelegate.getUnmountDelegateExtensionState(), item, host);
    } else {
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

      unmount(host, index, content, item, output);
      unbindMountItem(item);
    }

    if (mMountStats.isLoggingEnabled) {
      mMountStats.unmountedTimes.add((System.nanoTime() - startTime) / NS_IN_MS);
      mMountStats.unmountedNames.add(component.getSimpleName());
      mMountStats.unmountedCount++;
    }
  }

  @Override
  public void unbindMountItem(MountItem mountItem) {
    final LayoutOutput output = getLayoutOutput(mountItem);
    final long layoutOutputId = output.getId();
    maybeUnsetViewAttributes(mountItem);

    unbindAndUnmountLifecycle(mountItem);

    applyUnbindBinders(output, mountItem);

    try {
      getMountData(mountItem)
          .releaseMountContent(
              mContext.getAndroidContext(), mountItem, "unmountItem", mRecyclingMode);
    } catch (LithoMountData.ReleasingReleasedMountContentException e) {
      throw new RuntimeException(e.getMessage() + " " + getMountItemDebugMessage(mountItem));
    }
  }

  private void unbindAndUnmountLifecycle(MountItem item) {
    final LayoutOutput layoutOutput = getLayoutOutput(item);
    final Component component = layoutOutput.getComponent();
    final Object content = item.getContent();
    final ComponentContext context = getContextForComponent(component, layoutOutput);

    // Call the component's unmount() method.
    if (item.isBound()) {
      unbindComponentFromContent(item, component, content);
    }
    if (mRecyclingMode != ComponentTree.RecyclingMode.NO_UNMOUNTING) {
      component.unmount(context, content);
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

  @Nullable
  @Override
  public Object getContentById(long id) {
    if (mIndexToItemMap == null) {
      return null;
    }

    final MountItem mountItem = mIndexToItemMap.get(id);

    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  public androidx.collection.LongSparseArray<MountItem> getIndexToItemMap() {
    return mIndexToItemMap;
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

        final int parentIndex = layoutState.getPositionForId(curentHostId);
        final LayoutOutput parent = getLayoutOutput(layoutState.getMountableOutputAt(parentIndex));
        curentHostId = parent.getHostMarker();
      }
    }

    return layoutState.getMountableOutputCount() - 1;
  }

  public void clearLastMountedTree() {
    if (mTransitionsExtension != null) {
      mTransitionsExtension.clearLastMountedTreeId(mTransitionsExtensionState);
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

      unbindComponentFromContent(
          mountItem, getLayoutOutput(mountItem).getComponent(), mountItem.getContent());
    }

    clearVisibilityItems();

    if (mVisibilityExtension != null) {
      mVisibilityExtension.onUnbind(mVisibilityExtensionState);
    }

    if (mTransitionsExtension != null) {
      mTransitionsExtension.onUnbind(mTransitionsExtensionState);
    }

    if (isTracing) {
      ComponentsSystrace.endSection();
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

    for (int i = 0, size = mLayoutOutputsIds.length; i < size; i++) {
      final MountItem mountItem = getItemAt(i);
      if (mountItem == null || mountItem.isBound()) {
        continue;
      }

      final Component component = getLayoutOutput(mountItem).getComponent();
      final Object content = mountItem.getContent();

      bindComponentToContent(mountItem, component, content);

      if (content instanceof View
          && !(content instanceof ComponentHost)
          && ((View) content).isLayoutRequested()) {
        final View view = (View) content;
        applyBoundsToMountContent(
            view, view.getLeft(), view.getTop(), view.getRight(), view.getBottom(), true);
      }
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
  private boolean performIncrementalMount(
      LayoutState layoutState, Rect localVisibleRect, boolean processVisibilityOutputs) {
    if (mPreviousLocalVisibleRect.isEmpty()) {
      return false;
    }

    if (localVisibleRect.left != mPreviousLocalVisibleRect.left
        || localVisibleRect.right != mPreviousLocalVisibleRect.right) {
      return false;
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
              >= layoutState
                  .getLayoutOutput(layoutOutputBottoms.get(mPreviousBottomsIndex))
                  .getBounds()
                  .bottom) {
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputBottoms.get(mPreviousBottomsIndex));
        final long id = getLayoutOutput(node).getId();
        final int layoutOutputIndex = layoutState.getPositionForId(id);
        if (!isAnimationLocked(node)) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              <= layoutState
                  .getLayoutOutput(layoutOutputBottoms.get(mPreviousBottomsIndex - 1))
                  .getBounds()
                  .bottom) {
        mPreviousBottomsIndex--;
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputBottoms.get(mPreviousBottomsIndex));
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final int layoutOutputIndex = layoutState.getPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getPositionForId(layoutOutput.getId()), node, layoutOutput, layoutState);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
      }
    }

    final int height = mLithoView.getHeight();
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom
              >= layoutState
                  .getLayoutOutput(layoutOutputTops.get(mPreviousTopsIndex))
                  .getBounds()
                  .top) {
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputTops.get(mPreviousTopsIndex));
        final LayoutOutput layoutOutput = getLayoutOutput(node);
        final int layoutOutputIndex = layoutState.getPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          mountLayoutOutput(
              layoutState.getPositionForId(layoutOutput.getId()), node, layoutOutput, layoutState);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              < layoutState
                  .getLayoutOutput(layoutOutputTops.get(mPreviousTopsIndex - 1))
                  .getBounds()
                  .top) {
        mPreviousTopsIndex--;
        final RenderTreeNode node =
            layoutState.getRenderTreeNode(layoutOutputTops.get(mPreviousTopsIndex));
        final long id = getLayoutOutput(node).getId();
        final int layoutOutputIndex = layoutState.getPositionForId(id);
        if (!isAnimationLocked(node)) {
          unmountItem(layoutOutputIndex, mHostsByMarker);
        }
      }
    }

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

    mComponentIdsMountedInThisFrame.clear();

    return true;
  }
  /**
   * Collect transitions from layout time, mount time and from state updates.
   *
   * @param layoutState that is going to be mounted.
   */
  void collectAllTransitions(LayoutState layoutState, ComponentTree componentTree) {
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
  private ComponentContext getContextForComponent(Component component, LayoutOutput layoutOutput) {
    ComponentContext c;
    if (mUseStatelessComponent) {
      c = layoutOutput.getScopedContext();
    } else {
      c = component.getScopedContext(null, null);
    }

    return c == null ? mContext : c;
  }

  private void bindComponentToContent(MountItem mountItem, Component component, Object content) {
    final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
    component.bind(getContextForComponent(component, layoutOutput), content);
    mDynamicPropsManager.onBindComponentToContent(component, content);
    mountItem.setIsBound(true);
  }

  private void unbindComponentFromContent(
      MountItem mountItem, Component component, Object content) {
    final LayoutOutput layoutOutput = getLayoutOutput(mountItem);
    mDynamicPropsManager.onUnbindComponent(component, content);
    component.unbind(getContextForComponent(component, layoutOutput), content);
    mountItem.setIsBound(false);
  }

  @VisibleForTesting
  DynamicPropsManager getDynamicPropsManager() {
    return mDynamicPropsManager;
  }
}
