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

package com.facebook.rendercore;

import static com.facebook.rendercore.debug.DebugEventAttribute.Bounds;
import static com.facebook.rendercore.debug.DebugEventAttribute.Description;
import static com.facebook.rendercore.debug.DebugEventAttribute.GlobalKey;
import static com.facebook.rendercore.debug.DebugEventAttribute.HashCode;
import static com.facebook.rendercore.debug.DebugEventAttribute.RenderUnitId;
import static com.facebook.rendercore.debug.DebugEventAttribute.RootHostHashCode;
import static com.facebook.rendercore.debug.DebugEventDispatcher.beginTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.endTrace;
import static com.facebook.rendercore.debug.DebugEventDispatcher.generateTraceIdentifier;
import static com.facebook.rendercore.extensions.RenderCoreExtension.shouldUpdate;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.util.Preconditions;
import com.facebook.rendercore.debug.DebugEvent;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.BoundsUtils;
import com.facebook.rendercore.utils.CommonUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MountState implements MountDelegateTarget {

  public static final long ROOT_HOST_ID = 0L;
  private static final String TAG = "MountState";

  private final LongSparseArray<MountItem> mIdToMountedItemMap;
  private final Context mContext;
  private final Host mRootHost;
  private final Systracer mTracer;

  private boolean mIsMounting;
  private boolean mNeedsRemount;
  private @Nullable RenderTree mRenderTree;
  private @Nullable MountDelegate mMountDelegate;
  private @Nullable UnmountDelegateExtension mUnmountDelegateExtension;

  private boolean mEnsureParentMounted = true;

  public MountState(Host rootHost) {
    this(rootHost, RenderCoreSystrace.getInstance());
  }

  /**
   * This constructor allows the outer framework using RenderCore (e.g. Litho) to provide its own
   * Systracer implementation so that Systrace blocks are logged consistently between the framework
   * and the framework's usage of RenderCore.
   */
  public MountState(Host rootHost, Systracer tracer) {
    mIdToMountedItemMap = new LongSparseArray<>();
    mContext = rootHost.getContext();
    mRootHost = rootHost;
    mTracer = tracer;
  }

  public void setEnsureParentMounted(boolean ensureParentMounted) {
    mEnsureParentMounted = ensureParentMounted;
  }

  /**
   * True if we have manually unmounted content (e.g. via unmountAllItems) which means that while we
   * may not have a new RenderTree, the mounted content does not match what the viewport for the
   * LithoView may be.
   */
  @Override
  public boolean needsRemount() {
    return mNeedsRemount;
  }

  @Override
  public void notifyMount(long id) {
    if (mIdToMountedItemMap.get(id) != null) {
      return;
    }

    final int position = mRenderTree.getRenderTreeNodeIndex(id);
    final RenderTreeNode node = mRenderTree.getRenderTreeNodeAtIndex(position);
    mountRenderUnit(node, null);
  }

  @Override
  public void notifyUnmount(long id) {
    if (mIdToMountedItemMap == null) {
      return;
    }

    unmountItemRecursively(id);
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param renderTree a new {@link RenderTree} to mount
   */
  @Override
  public void mount(RenderTree renderTree) {
    if (renderTree == null) {
      throw new IllegalStateException("Trying to mount a null RenderTreeNode");
    }

    Integer traceIdentifier = generateTraceIdentifier(DebugEvent.RenderTreeMounted);
    if (traceIdentifier != null) {
      HashMap<String, Object> attributes = new HashMap<>();
      attributes.put(RootHostHashCode, mRootHost.hashCode());

      beginTrace(
          traceIdentifier,
          DebugEvent.RenderTreeMounted,
          String.valueOf(renderTree.getRenderStateId()),
          attributes);
    }
    try {

      if (mIsMounting) {
        throw new IllegalStateException("Trying to mount while already mounting!");
      }

      mIsMounting = true;

      final RenderTree previousRenderTree = mRenderTree;

      if (!updateRenderTree(renderTree)) {
        return;
      }

      Preconditions.checkNotNull(mRenderTree);

      final boolean isTracing = mTracer.isTracing();
      if (isTracing) {
        mTracer.beginSection("MountState.mount");
        mTracer.beginSection("RenderCoreExtension.beforeMount");
      }

      RenderCoreExtension.beforeMount(mRootHost, mMountDelegate, mRenderTree.getExtensionResults());

      if (isTracing) {
        mTracer.endSection();
        mTracer.beginSection("MountState.prepareMount");
      }

      prepareMount(previousRenderTree);

      if (isTracing) {
        mTracer.endSection();
      }

      // TODO: Remove this additional logging when root cause of crash in mountRenderUnit is
      //  found. We only want to collect logs when we're not ensuring the parent is mounted.
      //  When false, we will throw an exception that contains these logs. The StringBuilder
      //  is not needed when mEnsureParentMount is true.
      @Nullable final StringBuilder mountLoopLogBuilder;
      if (!mEnsureParentMounted) {
        mountLoopLogBuilder = new StringBuilder();
        mountLoopLogBuilder.append("Start of mount loop log:\n");
      } else {
        mountLoopLogBuilder = null;
      }

      // Starting from 1 as the RenderTreeNode in position 0 always represents the root which
      // is handled in prepareMount()
      for (int i = 1, size = renderTree.getMountableOutputCount(); i < size; i++) {
        final RenderTreeNode renderTreeNode = renderTree.getRenderTreeNodeAtIndex(i);

        final boolean isMountable = isMountable(renderTreeNode, i);
        final MountItem currentMountItem =
            mIdToMountedItemMap.get(renderTreeNode.getRenderUnit().getId());
        boolean isMounted = currentMountItem != null;

        // There is a bug (T99579422) happening where we try to incorrectly update an already
        // mounted render unit.

        // TODO: T101249557
        if (isMounted) {
          final RenderUnit currentRenderUnit = currentMountItem.getRenderUnit();
          boolean needsRecovery = false;
          // The old render unit we try to update is the root host which should not be updated
          // That's why we start from index 1.
          if (currentRenderUnit.getId() != renderTreeNode.getRenderUnit().getId()) {
            needsRecovery = true;
            ErrorReporter.getInstance()
                .report(
                    LogLevel.ERROR,
                    TAG,
                    "The current render unit id does not match the new one. "
                        + " index: "
                        + i
                        + " mountableOutputCounts: "
                        + renderTree.getMountableOutputCount()
                        + " currentRenderUnitId: "
                        + currentRenderUnit.getId()
                        + " newRenderUnitId: "
                        + renderTreeNode.getRenderUnit().getId(),
                    null,
                    0,
                    null);
          }

          // The new render unit is not the same type as the old one.
          if (!currentRenderUnit
              .getRenderContentType()
              .equals(renderTreeNode.getRenderUnit().getRenderContentType())) {
            needsRecovery = true;
            ErrorReporter.getInstance()
                .report(
                    LogLevel.ERROR,
                    TAG,
                    "Trying to update a MountItem with different ContentType. "
                        + "index: "
                        + i
                        + " currentRenderUnitId: "
                        + currentRenderUnit.getId()
                        + " newRenderUnitId: "
                        + renderTreeNode.getRenderUnit().getId()
                        + " currentRenderUnitContentType: "
                        + currentRenderUnit.getRenderContentType()
                        + " newRenderUnitContentType: "
                        + renderTreeNode.getRenderUnit().getRenderContentType(),
                    null,
                    0,
                    null);
          }
          if (needsRecovery) {
            recreateMountedItemMap(previousRenderTree);
            // reset the loop to start over.
            i = 1;
            continue;
          }
        }

        if (!mEnsureParentMounted) {
          mountLoopLogBuilder.append(
              String.format(
                  Locale.US,
                  "Processing index %d: isMountable = %b, isMounted = %b\n",
                  i,
                  isMountable,
                  isMounted));
        }

        if (!isMountable) {
          if (isMounted) {
            unmountItemRecursively(currentMountItem.getRenderTreeNode().getRenderUnit().getId());
          }
        } else if (!isMounted) {
          mountRenderUnit(renderTreeNode, mountLoopLogBuilder);
        } else {
          updateMountItemIfNeeded(renderTreeNode, currentMountItem);
        }
      }

      mNeedsRemount = false;

      if (isTracing) {
        mTracer.endSection();
        mTracer.beginSection("RenderCoreExtension.afterMount");
      }

      RenderCoreExtension.afterMount(mMountDelegate);

      mIsMounting = false;

      if (mMountDelegate != null) {
        if (isTracing) {
          mTracer.beginSection("MountState.onRenderTreeUpdated");
        }
        mMountDelegate.renderTreeUpdated((RenderCoreExtensionHost) mRootHost);
        if (isTracing) {
          mTracer.endSection();
        }
      }

      if (isTracing) {
        mTracer.endSection();
      }

    } catch (Exception e) {
      ErrorReporter.report(
          LogLevel.ERROR,
          "MountState:Exception",
          "Exception while mounting: " + e.getMessage(),
          e,
          0,
          null);
      CommonUtils.rethrow(e);
    } finally {
      if (traceIdentifier != null) {
        endTrace(traceIdentifier);
      }
      mIsMounting = false;
    }
  }

  /**
   * This method will unmount everything and recreate the mIdToMountedItemMap.
   *
   * @param previousRenderTree
   */
  private void recreateMountedItemMap(RenderTree previousRenderTree) {
    // We keep a pointer to the rootHost.
    MountItem rootHost = null;
    final long[] keysToUnmount = new long[mIdToMountedItemMap.size()];
    // We unmount all everything but the root host.
    for (int j = 0, mountedItems = mIdToMountedItemMap.size(); j < mountedItems; j++) {
      keysToUnmount[j] = mIdToMountedItemMap.keyAt(j);
    }
    for (long keyAt : keysToUnmount) {
      final MountItem mountItem = mIdToMountedItemMap.get(keyAt);
      if (mountItem != null) {
        if (mountItem.getRenderUnit().getId() == ROOT_HOST_ID) {
          rootHost = mountItem;
          mIdToMountedItemMap.remove(keyAt);
        } else if (mountItem.getRenderUnit().getId() != keyAt) {
          // This checks if the item was in the wrong position in the map. If it was we need to
          // unmount that item.
          unmountItemRecursively(keyAt);
        } else {
          unmountItemRecursively(mountItem.getRenderTreeNode().getRenderUnit().getId());
        }
      }
    }
    mIdToMountedItemMap.put(ROOT_HOST_ID, rootHost);
  }

  @Override
  public void unmountAllItems() {
    try {
      if (RenderCoreConfig.shouldSetInLayoutDuringUnmountAll) {
        mRootHost.setInLayout();
      }

      if (mRenderTree == null) {
        unregisterAllExtensions();
        return;
      }

      final boolean isTracing = mTracer.isTracing();
      if (isTracing) {
        mTracer.beginSection("MountState.unmountAllItems");
      }

      // unmount all the content from the Root node
      unmountItemRecursively(ROOT_HOST_ID);

      unregisterAllExtensions();

      if (isTracing) {
        mTracer.endSection();
      }

      mNeedsRemount = true;

      if (RenderCoreConfig.shouldClearRenderTreeOnUnmountAll) {
        mRenderTree = null;
      }

    } finally {
      if (RenderCoreConfig.shouldSetInLayoutDuringUnmountAll) {
        mRootHost.unsetInLayout();
      }
    }
  }

  @Override
  public boolean isRootItem(int position) {
    if (mRenderTree == null || position >= mRenderTree.getMountableOutputCount()) {
      return false;
    }
    final RenderUnit renderUnit = mRenderTree.getRenderTreeNodeAtIndex(position).getRenderUnit();
    final MountItem mountItem = mIdToMountedItemMap.get(renderUnit.getId());
    if (mountItem == null) {
      return false;
    }

    return mountItem == mIdToMountedItemMap.get(ROOT_HOST_ID);
  }

  @Override
  public Host getRootHost() {
    return mRootHost;
  }

  @Override
  public @Nullable Object getContentAt(int position) {
    if (mRenderTree == null || position >= mRenderTree.getMountableOutputCount()) {
      return null;
    }

    final MountItem mountItem =
        mIdToMountedItemMap.get(
            mRenderTree.getRenderTreeNodeAtIndex(position).getRenderUnit().getId());
    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  @Override
  public @Nullable Object getContentById(long id) {
    if (mIdToMountedItemMap == null) {
      return null;
    }

    final MountItem mountItem = mIdToMountedItemMap.get(id);

    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  /**
   * @param mountExtension
   * @deprecated Only used for Litho's integration. Marked for removal.
   */
  @Deprecated
  @Override
  public ExtensionState registerMountExtension(MountExtension mountExtension) {
    if (mMountDelegate == null) {
      mMountDelegate = new MountDelegate(this, mTracer);
    }
    return mMountDelegate.registerMountExtension(mountExtension);
  }

  @Override
  public ArrayList<Host> getHosts() {
    final ArrayList<Host> hosts = new ArrayList<>();
    for (int i = 0, size = mIdToMountedItemMap.size(); i < size; i++) {
      final MountItem item = mIdToMountedItemMap.valueAt(i);
      final Object content = item.getContent();
      if (content instanceof Host) {
        hosts.add((Host) content);
      }
    }

    return hosts;
  }

  @Override
  public @Nullable MountItem getMountItemAt(int position) {
    if (mRenderTree == null) {
      return null;
    }

    return mIdToMountedItemMap.get(
        mRenderTree.getRenderTreeNodeAtIndex(position).getRenderUnit().getId());
  }

  @Override
  public int getMountItemCount() {
    return mIdToMountedItemMap.size();
  }

  @Override
  public int getRenderUnitCount() {
    return mRenderTree == null ? 0 : mRenderTree.getMountableOutputCount();
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

  @Override
  public int getRenderStateId() {
    return mRenderTree != null ? mRenderTree.getRenderStateId() : -1;
  }

  /**
   * This is called when the {@link MountItem}s mounted on this {@link MountState} need to be
   * re-bound with the same RenderUnit. This happens when a detach/attach happens on the root {@link
   * Host} that owns the MountState.
   */
  @Override
  public void attach() {
    if (mRenderTree == null) {
      return;
    }

    final boolean isTracing = mTracer.isTracing();

    if (isTracing) {
      mTracer.beginSection("MountState.bind");
    }

    for (int i = 0, size = mRenderTree.getMountableOutputCount(); i < size; i++) {
      final RenderUnit renderUnit = mRenderTree.getRenderTreeNodeAtIndex(i).getRenderUnit();
      final MountItem mountItem = mIdToMountedItemMap.get(renderUnit.getId());
      if (mountItem == null || mountItem.isBound()) {
        continue;
      }

      final Object content = mountItem.getContent();
      bindRenderUnitToContent(mountItem);

      if (content instanceof View
          && !(content instanceof Host)
          && ((View) content).isLayoutRequested()) {
        final View view = (View) content;

        BoundsUtils.applyBoundsToMountContent(mountItem.getRenderTreeNode(), view, true, mTracer);
      }
    }

    if (isTracing) {
      mTracer.endSection();
    }
  }

  /** Unbinds all the MountItems currently mounted on this MountState. */
  @Override
  public void detach() {
    if (mRenderTree == null) {
      return;
    }

    final boolean isTracing = mTracer.isTracing();
    if (isTracing) {
      mTracer.beginSection("MountState.unbind");
      mTracer.beginSection("MountState.unbindAllContent");
    }

    for (int i = 0, size = mRenderTree.getMountableOutputCount(); i < size; i++) {
      final RenderUnit renderUnit = mRenderTree.getRenderTreeNodeAtIndex(i).getRenderUnit();
      final MountItem mountItem = mIdToMountedItemMap.get(renderUnit.getId());

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindRenderUnitFromContent(mountItem);
    }

    if (isTracing) {
      mTracer.endSection();
      mTracer.beginSection("MountState.unbindExtensions");
    }

    if (mMountDelegate != null) {
      mMountDelegate.unBind();
    }

    if (isTracing) {
      mTracer.endSection();
      mTracer.endSection();
    }
  }

  @Nullable
  RenderTree getRenderTree() {
    return mRenderTree;
  }

  private boolean isMountable(RenderTreeNode renderTreeNode, int index) {
    return mMountDelegate == null || mMountDelegate.maybeLockForMount(renderTreeNode, index);
  }

  private void updateBoundsForMountedRenderTreeNode(
      RenderTreeNode renderTreeNode, MountItem item, @Nullable MountDelegate mountDelegate) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (renderTreeNode.getRenderUnit().getId() == ROOT_HOST_ID) {
      return;
    }

    final Object content = item.getContent();
    final boolean forceTraversal = content instanceof View && ((View) content).isLayoutRequested();

    BoundsUtils.applyBoundsToMountContent(
        item.getRenderTreeNode(), item.getContent(), forceTraversal /* force */, mTracer);

    if (mountDelegate != null) {
      mountDelegate.onBoundsAppliedToItem(renderTreeNode, item.getContent(), mTracer);
    }
  }

  /** Updates the extensions of this {@link MountState} from the new {@link RenderTree}. */
  private boolean updateRenderTree(RenderTree renderTree) {
    // If the trees are same or if no remount is required, then no update is required.
    if (renderTree == mRenderTree && !mNeedsRemount) {
      return false;
    }

    // If the extensions have changed, un-register the current and register the new extensions.
    if (mRenderTree == null || mNeedsRemount) {
      addExtensions(renderTree.getExtensionResults());
    } else if (shouldUpdate(mRenderTree.getExtensionResults(), renderTree.getExtensionResults())) {
      unregisterAllExtensions();
      addExtensions(renderTree.getExtensionResults());
    }

    // Update the current render tree.
    mRenderTree = renderTree;

    return true;
  }

  /**
   * Prepare the {@link MountState} to mount a new {@link RenderTree}.
   *
   * @param previousRenderTree
   */
  private void prepareMount(@Nullable RenderTree previousRenderTree) {
    unmountOrMoveOldItems(previousRenderTree);

    final MountItem rootItem = mIdToMountedItemMap.get(ROOT_HOST_ID);
    final RenderTreeNode rootNode = mRenderTree.getRenderTreeNodeAtIndex(0);

    // If root mount item is null then mounting root node for the first time.
    if (rootItem == null) {
      mountRootItem(rootNode);
    } else {
      // If root mount item is present then update it.
      updateMountItemIfNeeded(rootNode, rootItem);
    }
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private void unmountOrMoveOldItems(@Nullable RenderTree previousRenderTree) {
    if (mRenderTree == null || previousRenderTree == null) {
      return;
    }

    final boolean isTracing = mTracer.isTracing();

    if (isTracing) {
      mTracer.beginSection("unmountOrMoveOldItems");
    }

    // Traversing from the beginning since mRenderUnitIds unmounting won't remove entries there
    // but only from mIndexToMountedItemMap. If an host changes we're going to unmount it and
    // recursively
    // all its mounted children.
    for (int i = 1; i < previousRenderTree.getMountableOutputCount(); i++) {
      final RenderUnit previousRenderUnit =
          previousRenderTree.getRenderTreeNodeAtIndex(i).getRenderUnit();
      final int newPosition = mRenderTree.getRenderTreeNodeIndex(previousRenderUnit.getId());
      final RenderTreeNode renderTreeNode =
          newPosition > -1 ? mRenderTree.getRenderTreeNodeAtIndex(newPosition) : null;
      final MountItem oldItem = mIdToMountedItemMap.get(previousRenderUnit.getId());

      // if oldItem is null it was previously unmounted so there is nothing we need to do.
      if (oldItem == null) continue;

      final boolean hasUnmountDelegate =
          mUnmountDelegateExtension != null
              && mUnmountDelegateExtension.shouldDelegateUnmount(
                  mMountDelegate.getUnmountDelegateExtensionState(), oldItem);

      if (hasUnmountDelegate) {
        continue;
      }

      if (newPosition == -1) {
        unmountItemRecursively(oldItem.getRenderTreeNode().getRenderUnit().getId());
      } else {
        final long newHostMarker = renderTreeNode.getParent().getRenderUnit().getId();
        final @Nullable MountItem hostItem = mIdToMountedItemMap.get(newHostMarker);
        final @Nullable Host newHost = hostItem != null ? (Host) hostItem.getContent() : null;

        if (oldItem.getHost() == null || oldItem.getHost() != newHost) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItemRecursively(oldItem.getRenderTreeNode().getRenderUnit().getId());
        } else if (oldItem.getRenderTreeNode().getPositionInParent()
            != renderTreeNode.getPositionInParent()) {
          // If a MountItem for this id exists and its Host has not changed but its position
          // in the Host has changed we need to update the position in the Host to ensure
          // the z-ordering.
          oldItem
              .getHost()
              .moveItem(
                  oldItem,
                  oldItem.getRenderTreeNode().getPositionInParent(),
                  renderTreeNode.getPositionInParent());
        }
      }
    }

    if (isTracing) {
      mTracer.endSection();
    }
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first RenderTreeNode).
  private void mountContentInHost(final MountItem item, Host host, RenderTreeNode node) {
    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIdToMountedItemMap.put(node.getRenderUnit().getId(), item);
    host.mount(node.getPositionInParent(), item);
  }

  private boolean isMounted(final long id) {
    return mIdToMountedItemMap.get(id) != null;
  }

  private void mountRenderUnit(
      RenderTreeNode renderTreeNode, @Nullable StringBuilder processLogBuilder) {

    if (renderTreeNode.getRenderUnit().getId() == ROOT_HOST_ID) {
      mountRootItem(renderTreeNode);
      return;
    }

    Integer traceIdentifier = generateTraceIdentifier(DebugEvent.RenderUnitMounted);

    if (traceIdentifier != null) {
      HashMap<String, Object> attributes = new HashMap<>();
      attributes.put(RenderUnitId, renderTreeNode.getRenderUnit().getId());
      attributes.put(Description, renderTreeNode.getRenderUnit().getDescription());
      attributes.put(Bounds, renderTreeNode.getBounds());
      attributes.put(RootHostHashCode, mRootHost.hashCode());
      attributes.put(GlobalKey, renderTreeNode.getRenderUnit().getDebugKey());

      beginTrace(
          traceIdentifier,
          DebugEvent.RenderUnitMounted,
          String.valueOf(mRenderTree.getRenderStateId()),
          attributes);
    }

    final boolean isTracing = mTracer.isTracing();
    if (isTracing) {
      mTracer.beginSection("MountItem: " + renderTreeNode.getRenderUnit().getDescription());
    }

    // 1. Resolve the correct host to mount our content to.
    final RenderTreeNode hostTreeNode = renderTreeNode.getParent();

    final RenderUnit parentRenderUnit = hostTreeNode.getRenderUnit();
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();

    // 2. Ensure render tree node's parent is mounted or throw exception depending on the
    // ensure-parent-mounted flag.
    if (isTracing) {
      mTracer.beginSection("MountItem:mount-parent " + parentRenderUnit.getDescription());
    }
    maybeEnsureParentIsMounted(
        renderTreeNode, renderUnit, hostTreeNode, parentRenderUnit, processLogBuilder);
    if (isTracing) {
      mTracer.endSection();
    }

    final MountItem mountItem = mIdToMountedItemMap.get(parentRenderUnit.getId());
    final Object parentContent = mountItem.getContent();
    assertParentContentType(parentContent, renderUnit, parentRenderUnit);

    final Host host = (Host) parentContent;

    // 3. call the RenderUnit's Mount bindings.
    if (isTracing) {
      mTracer.beginSection("MountItem:acquire-content " + renderUnit.getDescription());
    }
    final Object content =
        MountItemsPool.acquireMountContent(mContext, renderUnit.getContentAllocator());
    if (isTracing) {
      mTracer.endSection();
    }

    if (mMountDelegate != null) {
      mMountDelegate.startNotifyVisibleBoundsChangedSection();
    }

    if (isTracing) {
      mTracer.beginSection("MountItem:mount " + renderTreeNode.getRenderUnit().getDescription());
    }
    final MountItem item = new MountItem(renderTreeNode, content);

    mountRenderUnitToContent(renderTreeNode, renderUnit, content, item.getBindData());

    // 4. Mount the content into the selected host.
    mountContentInHost(item, host, renderTreeNode);
    if (isTracing) {
      mTracer.endSection();
      mTracer.beginSection("MountItem:bind " + renderTreeNode.getRenderUnit().getDescription());
    }

    // 5. Call attach binding functions
    bindRenderUnitToContent(item);

    if (isTracing) {
      mTracer.endSection();
      mTracer.beginSection(
          "MountItem:applyBounds " + renderTreeNode.getRenderUnit().getDescription());
    }

    // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    BoundsUtils.applyBoundsToMountContent(
        renderTreeNode, item.getContent(), true /* force */, mTracer);

    if (isTracing) {
      mTracer.endSection();
      mTracer.beginSection("MountItem:after " + renderTreeNode.getRenderUnit().getDescription());
    }
    if (mMountDelegate != null) {
      mMountDelegate.onBoundsAppliedToItem(renderTreeNode, item.getContent(), mTracer);
      mMountDelegate.endNotifyVisibleBoundsChangedSection();
    }

    if (isTracing) {
      mTracer.endSection();
      mTracer.endSection();
    }

    if (traceIdentifier != null) {
      endTrace(traceIdentifier);
    }
  }

  private void unmountItemRecursively(final long id) {
    final MountItem item = mIdToMountedItemMap.get(id);
    // Already has been unmounted.
    if (item == null) {
      return;
    }

    // When unmounting use the render unit from the MountItem
    final boolean isTracing = mTracer.isTracing();
    final RenderTreeNode node = item.getRenderTreeNode();
    final RenderUnit unit = item.getRenderUnit();
    final Object content = item.getContent();
    final boolean hasUnmountDelegate =
        mUnmountDelegateExtension != null
            && mUnmountDelegateExtension.shouldDelegateUnmount(
                mMountDelegate.getUnmountDelegateExtensionState(), item);

    Integer traceIdentifier = generateTraceIdentifier(DebugEvent.RenderUnitUnmounted);
    if (traceIdentifier != null) {
      HashMap<String, Object> attributes = new HashMap<>();
      attributes.put(RenderUnitId, id);
      attributes.put(Description, unit.getDescription());
      attributes.put(Bounds, node.getBounds());
      attributes.put(RootHostHashCode, mRootHost.hashCode());
      attributes.put(GlobalKey, unit.getDebugKey());

      beginTrace(
          traceIdentifier,
          DebugEvent.RenderUnitUnmounted,
          String.valueOf(mRenderTree.getRenderStateId()),
          attributes);
    }

    if (isTracing) {
      mTracer.beginSection("UnmountItem: " + unit.getDescription());
    }

    /* Recursively unmount mounted children items.
    This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    sub tree, that will still have mounted items. (Different sequence number on RenderTreeNode id) */
    if (node.getChildrenCount() > 0) {

      // unmount all children
      for (int i = node.getChildrenCount() - 1; i >= 0; i--) {
        unmountItemRecursively(node.getChildAt(i).getRenderUnit().getId());
      }

      // check if all items are unmount from the host
      if (!hasUnmountDelegate && ((Host) content).getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    // The root host item cannot be unmounted as it's a reference
    // to the top-level Host, and it is not mounted in a host.
    if (unit.getId() == ROOT_HOST_ID) {
      unmountRootItem();
      if (isTracing) {
        mTracer.endSection();
      }

      if (traceIdentifier != null) {
        endTrace(traceIdentifier);
      }
      return;
    } else {
      mIdToMountedItemMap.remove(unit.getId());
    }

    final Host host = item.getHost();

    if (hasUnmountDelegate) {
      mUnmountDelegateExtension.unmount(
          mMountDelegate.getUnmountDelegateExtensionState(), item, host);
    } else {

      if (isTracing) {
        mTracer.beginSection("UnmountItem:remove: " + unit.getDescription());
      }
      // We don't expect Host to really be null but we observe cases where this
      // is actually happening
      if (host != null) {
        host.unmount(item);
      }
      if (isTracing) {
        mTracer.endSection();
      }

      if (item.isBound()) {
        if (isTracing) {
          mTracer.beginSection("UnmountItem:unbind: " + unit.getDescription());
        }
        unbindRenderUnitFromContent(item);
        if (isTracing) {
          mTracer.endSection();
        }
      }

      if (content instanceof View) {
        ((View) content).setPadding(0, 0, 0, 0);
      }

      if (isTracing) {
        mTracer.beginSection("UnmountItem:unmount: " + unit.getDescription());
      }
      unmountRenderUnitFromContent(node, unit, content, item.getBindData());
      if (isTracing) {
        mTracer.endSection();
      }

      item.releaseMountContent(mContext);
    }

    if (isTracing) {
      mTracer.endSection();
    }

    if (traceIdentifier != null) {
      endTrace(traceIdentifier);
    }
  }

  /**
   * Since the root item is not itself mounted on a host, its unmount method is encapsulated into a
   * different method.
   */
  private void unmountRootItem() {
    MountItem item = mIdToMountedItemMap.get(ROOT_HOST_ID);
    if (item != null) {

      if (item.isBound()) {
        unbindRenderUnitFromContent(item);
      }

      mIdToMountedItemMap.remove(ROOT_HOST_ID);

      final RenderTreeNode rootRenderTreeNode = mRenderTree.getRoot();

      unmountRenderUnitFromContent(
          rootRenderTreeNode,
          rootRenderTreeNode.getRenderUnit(),
          item.getContent(),
          item.getBindData());
    }
  }

  private void mountRootItem(RenderTreeNode rootNode) {
    // Create root mount item.
    final MountItem item = new MountItem(rootNode, mRootHost);

    // Run mount callbacks.
    mountRenderUnitToContent(rootNode, rootNode.getRenderUnit(), mRootHost, item.getBindData());

    // Adds root mount item to map.
    mIdToMountedItemMap.put(ROOT_HOST_ID, item);

    // Run binder callbacks
    bindRenderUnitToContent(item);
  }

  @Override
  public void unbindMountItem(MountItem mountItem) {
    if (mountItem.isBound()) {
      unbindRenderUnitFromContent(mountItem);
    }
    final Object content = mountItem.getContent();
    if (content instanceof View) {
      ((View) content).setPadding(0, 0, 0, 0);
    }

    unmountRenderUnitFromContent(
        mountItem.getRenderTreeNode(),
        mountItem.getRenderTreeNode().getRenderUnit(),
        content,
        mountItem.getBindData());

    mountItem.releaseMountContent(mContext);
  }

  public void setRenderTreeUpdateListener(RenderTreeUpdateListener listener) {
    if (mMountDelegate == null) {
      mMountDelegate = new MountDelegate(this, mTracer);
    }

    mMountDelegate.setMountStateListener(listener);
  }

  private void addExtensions(@Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> extensions) {
    if (extensions != null) {
      if (mMountDelegate == null) {
        mMountDelegate = new MountDelegate(this, mTracer);
      }
      mMountDelegate.registerExtensions(extensions);
    }
  }

  @Override
  public void unregisterAllExtensions() {
    if (mMountDelegate != null) {
      mMountDelegate.unBind();
      mMountDelegate.unMount();
      mMountDelegate.unregisterAllExtensions();
      mMountDelegate.releaseAllAcquiredReferences();
    }
  }

  private void mountRenderUnitToContent(
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content,
      final BindData bindData) {

    Integer traceIdentifier = generateTraceIdentifier(DebugEvent.MountItemMount);
    if (traceIdentifier != null) {
      HashMap<String, Object> attributes = new HashMap<>();
      attributes.put(RenderUnitId, unit.getId());
      attributes.put(Description, unit.getDescription());
      attributes.put(HashCode, content.hashCode());
      attributes.put(Bounds, node.getBounds());
      attributes.put(GlobalKey, unit.getDebugKey());

      beginTrace(
          traceIdentifier,
          DebugEvent.MountItemMount,
          String.valueOf(mRenderTree.getRenderStateId()),
          attributes);
    }

    final MountDelegate mountDelegate = mMountDelegate;
    unit.mountBinders(mContext, content, node.getLayoutData(), bindData, mTracer);
    if (mountDelegate != null) {
      mountDelegate.onMountItem(unit, content, node.getLayoutData(), mTracer);
    }

    if (traceIdentifier != null) {
      endTrace(traceIdentifier);
    }
  }

  private void unmountRenderUnitFromContent(
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content,
      final BindData bindData) {
    final MountDelegate mountDelegate = mMountDelegate;
    if (mountDelegate != null) {
      mountDelegate.onUnmountItem(unit, content, node.getLayoutData(), mTracer);
    }
    unit.unmountBinders(mContext, content, node.getLayoutData(), bindData, mTracer);
  }

  private void bindRenderUnitToContent(MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final Object content = item.getContent();
    final Object layoutData = item.getRenderTreeNode().getLayoutData();
    renderUnit.attachBinders(mContext, content, layoutData, item.getBindData(), mTracer);
    final MountDelegate mountDelegate = mMountDelegate;
    if (mountDelegate != null) {
      mountDelegate.onBindItem(renderUnit, content, layoutData, mTracer);
    }
    item.setIsBound(true);
  }

  private void unbindRenderUnitFromContent(MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final Object content = item.getContent();
    final Object layoutData = item.getRenderTreeNode().getLayoutData();
    final MountDelegate mountDelegate = mMountDelegate;
    if (mountDelegate != null) {
      mountDelegate.onUnbindItem(renderUnit, content, layoutData, mTracer);
    }
    renderUnit.detachBinders(mContext, content, layoutData, item.getBindData(), mTracer);
    item.setIsBound(false);
  }

  private void updateMountItemIfNeeded(RenderTreeNode renderTreeNode, MountItem currentMountItem) {
    final MountDelegate mountDelegate = mMountDelegate;
    final boolean isTracing = mTracer.isTracing();

    if (isTracing) {
      mTracer.beginSection("updateMountItemIfNeeded");
    }

    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    final Object newLayoutData = renderTreeNode.getLayoutData();
    final RenderTreeNode currentNode = currentMountItem.getRenderTreeNode();
    final RenderUnit currentRenderUnit = currentNode.getRenderUnit();
    final Object currentLayoutData = currentNode.getLayoutData();
    final Object content = currentMountItem.getContent();

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    currentMountItem.update(renderTreeNode);

    currentRenderUnit.onStartUpdateRenderUnit();

    if (mountDelegate != null) {
      mountDelegate.startNotifyVisibleBoundsChangedSection();
    }

    if (currentRenderUnit != renderUnit) {
      if (isTracing) {
        mTracer.beginSection("UpdateItem: " + renderUnit.getDescription());
      }

      Integer traceIdentifier = generateTraceIdentifier(DebugEvent.RenderUnitUpdated);
      if (traceIdentifier != null) {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(RenderUnitId, renderTreeNode.getRenderUnit().getId());
        attributes.put(Description, renderTreeNode.getRenderUnit().getDescription());
        attributes.put(Bounds, renderTreeNode.getBounds());
        attributes.put(RootHostHashCode, mRootHost.hashCode());
        attributes.put(GlobalKey, renderTreeNode.getRenderUnit().getDebugKey());

        beginTrace(
            traceIdentifier,
            DebugEvent.RenderUnitUpdated,
            String.valueOf(mRenderTree.getRenderStateId()),
            attributes);
      }

      renderUnit.updateBinders(
          mContext,
          content,
          currentRenderUnit,
          currentLayoutData,
          newLayoutData,
          mountDelegate,
          currentMountItem.getBindData(),
          currentMountItem.isBound(),
          mTracer);

      if (traceIdentifier != null) {
        endTrace(traceIdentifier);
      }
    }

    currentMountItem.setIsBound(true);

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem, mountDelegate);

    if (mountDelegate != null) {
      mountDelegate.endNotifyVisibleBoundsChangedSection();
    }

    currentRenderUnit.onEndUpdateRenderUnit();

    if (isTracing) {
      if (currentRenderUnit != renderUnit) {
        mTracer.endSection(); // UPDATE
      }

      mTracer.endSection();
    }
  }

  private static void assertParentContentType(
      final Object parentContent,
      final RenderUnit<?> renderUnit,
      final RenderUnit<?> parentRenderUnit) {
    if (!(parentContent instanceof Host)) {
      throw new RuntimeException(
          "Trying to mount a RenderTreeNode, its parent should be a Host, but was '"
              + parentContent.getClass().getSimpleName()
              + "'.\n"
              + "Parent RenderUnit: "
              + "id="
              + parentRenderUnit.getId()
              + "; contentType='"
              + parentRenderUnit.getRenderContentType()
              + "'.\n"
              + "Child RenderUnit: "
              + "id="
              + renderUnit.getId()
              + "; contentType='"
              + renderUnit.getRenderContentType()
              + "'.");
    }
  }

  private void maybeEnsureParentIsMounted(
      final RenderTreeNode renderTreeNode,
      final RenderUnit<?> renderUnit,
      final RenderTreeNode hostTreeNode,
      final RenderUnit<?> parentRenderUnit,
      final @Nullable StringBuilder processLogBuilder) {
    if (!isMounted(parentRenderUnit.getId())) {
      if (mEnsureParentMounted) {
        mountRenderUnit(hostTreeNode, processLogBuilder);
      } else {
        final String additionalProcessLog =
            processLogBuilder != null ? processLogBuilder.toString() : "NA";
        throw new HostNotMountedException(
            renderUnit,
            parentRenderUnit,
            "Trying to mount a RenderTreeNode, but its host is not mounted.\n"
                + "Parent RenderUnit: "
                + hostTreeNode.generateDebugString(mRenderTree)
                + "'.\n"
                + "Child RenderUnit: "
                + renderTreeNode.generateDebugString(mRenderTree)
                + "'.\n"
                + "Entire tree:\n"
                + mRenderTree.generateDebugString()
                + ".\n"
                + "Additional Process Log:\n"
                + additionalProcessLog
                + ".\n");
      }
    }
  }

  public void needsRemount(boolean needsRemount) {
    mNeedsRemount = needsRemount;
  }
}
