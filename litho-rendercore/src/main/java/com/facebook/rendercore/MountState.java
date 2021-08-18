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

package com.facebook.rendercore;

import static com.facebook.rendercore.extensions.RenderCoreExtension.shouldUpdate;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.BoundsUtils;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MountState implements MountDelegateTarget {

  public static final long ROOT_HOST_ID = 0L;

  private final LongSparseArray<MountItem> mIdToMountedItemMap;
  private final Context mContext;
  private final Host mRootHost;

  // Updated in prepareMount(), thus during mounting they hold the information
  // about the LayoutState that is being mounted, not mLastMountedLayoutState
  @Nullable private long[] mRenderUnitIds;

  private boolean mIsMounting;
  private boolean mNeedsRemount;
  private RenderTree mRenderTree;
  private @Nullable MountDelegate mMountDelegate;
  private @Nullable UnmountDelegateExtension mUnmountDelegateExtension;

  public MountState(Host rootHost) {
    mIdToMountedItemMap = new LongSparseArray<>();
    mContext = rootHost.getContext();
    mRootHost = rootHost;
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
    if (mRenderTree == null) {
      return;
    }

    final int position = mRenderTree.getRenderTreeNodeIndex(id);
    if (getItemAt(position) != null) {
      return;
    }

    final RenderTreeNode node = mRenderTree.getRenderTreeNodeAtIndex(position);
    mountRenderUnit(position, node, null);
  }

  @Override
  public void notifyUnmount(long id) {
    if (mIdToMountedItemMap == null) {
      return;
    }

    final MountItem mountItem = mIdToMountedItemMap.get(id);
    if (mountItem != null) {
      unmountItemRecursively(mountItem.getRenderTreeNode());
    }
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

    if (mIsMounting) {
      throw new IllegalStateException("Trying to mount while already mounting!");
    }

    if (!updateRenderTree(renderTree)) {
      return;
    }

    RenderCoreSystrace.beginSection("MountState.mount");

    mIsMounting = true;

    RenderCoreSystrace.beginSection("RenderCoreExtension.beforeMount");
    RenderCoreExtension.beforeMount(this, mRootHost, mRenderTree.getExtensionResults());
    RenderCoreSystrace.endSection();

    RenderCoreSystrace.beginSection("MountState.prepareMount");
    prepareMount();
    RenderCoreSystrace.endSection();

    // TODO: Remove this additional logging when root cause of crash in mountRenderUnit is found.
    final Locale lc = Locale.US;
    final StringBuilder mountLoopLogBuilder = new StringBuilder();
    mountLoopLogBuilder.append("Start of mount loop log:\n");

    // Starting from 1 as the RenderTreeNode in position 0 always represents the root which is
    // handled in prepareMount()
    for (int i = 1, size = renderTree.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = renderTree.getRenderTreeNodeAtIndex(i);

      final boolean isMountable = isMountable(renderTreeNode, i);
      final MountItem currentMountItem = getItemAt(i);
      final boolean isMounted = currentMountItem != null;

      mountLoopLogBuilder.append(
          String.format(
              lc,
              "Processing index %d: isMountable = %b, isMounted = %b\n",
              i,
              isMountable,
              isMounted));

      if (!isMountable) {
        if (isMounted) {
          unmountItemRecursively(renderTreeNode);
        }
      } else if (!isMounted) {
        RenderCoreSystrace.beginSection(
            "MountItem: ", renderTreeNode.getRenderUnit().getDescription());
        mountRenderUnit(i, renderTreeNode, mountLoopLogBuilder);
        RenderCoreSystrace.endSection();
      } else {
        updateMountItemIfNeeded(mMountDelegate, mContext, renderTreeNode, currentMountItem);
      }
    }

    mNeedsRemount = false;
    mIsMounting = false;
    RenderCoreSystrace.endSection();

    RenderCoreSystrace.beginSection("RenderCoreExtension.afterMount");
    RenderCoreExtension.afterMount(this, mRenderTree.getExtensionResults());
    RenderCoreSystrace.endSection();
  }

  @Override
  public void unmountAllItems() {
    unregisterAllExtensions();

    if (mRenderUnitIds == null) {
      return;
    }
    // Let's unmount all the content from the Root host. Everything else will be recursively
    // unmounted from there.
    final RenderTreeNode rootRenderTreeNode = mRenderTree.getRoot();

    for (int i = 0; i < rootRenderTreeNode.getChildrenCount(); i++) {
      unmountItemRecursively(rootRenderTreeNode.getChildAt(i));
    }

    // Let's unbind and unmount the root host.
    MountItem item = mIdToMountedItemMap.get(ROOT_HOST_ID);
    if (item != null) {
      if (item.isBound()) {
        unbindRenderUnitFromContent(mMountDelegate, mContext, item);
      }
      mIdToMountedItemMap.remove(ROOT_HOST_ID);
      unmountRenderUnitFromContent(
          mContext,
          rootRenderTreeNode,
          rootRenderTreeNode.getRenderUnit(),
          item.getContent(),
          mMountDelegate);
    }

    mRenderUnitIds = null;
    mNeedsRemount = true;
  }

  @Override
  public boolean isRootItem(int position) {
    final MountItem mountItem = getItemAt(position);
    if (mountItem == null) {
      return false;
    }

    return mountItem == mIdToMountedItemMap.get(ROOT_HOST_ID);
  }

  @Override
  public @Nullable MountItem getRootItem() {
    return mIdToMountedItemMap != null ? mIdToMountedItemMap.get(ROOT_HOST_ID) : null;
  }

  @Override
  public Object getContentAt(int position) {
    final MountItem mountItem = getItemAt(position);
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

  /** @deprecated Only used for Litho's integration. Marked for removal. */
  @Deprecated
  @Override
  public void registerMountDelegateExtension(MountExtension mountExtension) {
    if (mMountDelegate == null) {
      mMountDelegate = new MountDelegate(this);
    }
    mMountDelegate.addExtension(mountExtension);
  }

  /** @deprecated Only used for Litho's integration. Marked for removal. */
  @Deprecated
  @Override
  public void unregisterMountDelegateExtension(MountExtension mountExtension) {
    if (mMountDelegate == null) {
      return;
    }

    mMountDelegate.removeExtension(mountExtension);
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
    return getItemAt(position);
  }

  @Override
  public int getMountItemCount() {
    return mIdToMountedItemMap.size();
  }

  @Override
  public int getRenderUnitCount() {
    return mRenderUnitIds == null ? 0 : mRenderUnitIds.length;
  }

  @Override
  public void setUnmountDelegateExtension(UnmountDelegateExtension unmountDelegateExtension) {
    mUnmountDelegateExtension = unmountDelegateExtension;
  }

  @Override
  public void removeUnmountDelegateExtension() {
    mUnmountDelegateExtension = null;
  }

  @Override
  public @Nullable ExtensionState getExtensionState(MountExtension mountExtension) {
    return mMountDelegate != null ? mMountDelegate.getExtensionState(mountExtension) : null;
  }

  /**
   * This is called when the {@link MountItem}s mounted on this {@link MountState} need to be
   * re-bound with the same RenderUnit. This happens when a detach/attach happens on the root {@link
   * Host} that owns the MountState.
   */
  @Override
  public void attach() {
    if (mRenderUnitIds == null) {
      return;
    }

    for (int i = 0, size = mRenderUnitIds.length; i < size; i++) {
      final MountItem mountItem = getItemAt(i);
      if (mountItem == null || mountItem.isBound()) {
        continue;
      }

      final Object content = mountItem.getContent();
      bindRenderUnitToContent(mMountDelegate, mContext, mountItem);

      if (content instanceof View
          && !(content instanceof Host)
          && ((View) content).isLayoutRequested()) {
        final View view = (View) content;

        BoundsUtils.applyBoundsToMountContent(mountItem.getRenderTreeNode(), view, true);
      }
    }
  }

  /** Unbinds all the MountItems currently mounted on this MountState. */
  @Override
  public void detach() {
    if (mRenderUnitIds == null) {
      return;
    }

    for (int i = 0, size = mRenderUnitIds.length; i < size; i++) {
      final MountItem mountItem = getItemAt(i);

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindRenderUnitFromContent(mMountDelegate, mContext, mountItem);
    }
  }

  @Nullable
  RenderTree getRenderTree() {
    return mRenderTree;
  }

  private boolean isMountable(RenderTreeNode renderTreeNode, int index) {
    return mMountDelegate == null || mMountDelegate.maybeLockForMount(renderTreeNode, index);
  }

  private static void updateBoundsForMountedRenderTreeNode(
      RenderTreeNode renderTreeNode, MountItem item) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (renderTreeNode.getRenderUnit().getId() == ROOT_HOST_ID) {
      return;
    }

    final Object content = item.getContent();
    final boolean forceTraversal = content instanceof View && ((View) content).isLayoutRequested();

    BoundsUtils.applyBoundsToMountContent(
        item.getRenderTreeNode(), item.getContent(), forceTraversal /* force */);
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

  /** Prepare the {@link MountState} to mount a new {@link RenderTree}. */
  private void prepareMount() {
    unmountOrMoveOldItems();

    final MountItem rootItem = mIdToMountedItemMap.get(ROOT_HOST_ID);
    final RenderTreeNode rootNode = mRenderTree.getRenderTreeNodeAtIndex(0);
    final RenderUnit rootRenderUnit = rootNode.getRenderUnit();

    // If root mount item is null then mounting root node for the first time.
    if (rootItem == null) {
      // Run mount callbacks.
      mountRenderUnitToContent(mMountDelegate, mContext, rootNode, rootRenderUnit, mRootHost);

      // Create root mount item.
      final MountItem item = new MountItem(rootNode, mRootHost, mRootHost);

      // Adds root mount item to map.
      mIdToMountedItemMap.put(ROOT_HOST_ID, item);

      // Run binder callbacks
      bindRenderUnitToContent(mMountDelegate, mContext, item);

    } else {
      // If root mount item is present then update it.
      updateMountItemIfNeeded(mMountDelegate, mContext, rootNode, rootItem);
    }

    final int outputCount = mRenderTree.getMountableOutputCount();
    if (mRenderUnitIds == null || outputCount != mRenderUnitIds.length) {
      mRenderUnitIds = new long[outputCount];
    }

    for (int i = 0; i < outputCount; i++) {
      mRenderUnitIds[i] = mRenderTree.getRenderTreeNodeAtIndex(i).getRenderUnit().getId();
    }
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private void unmountOrMoveOldItems() {
    if (mRenderUnitIds == null) {
      return;
    }

    // Traversing from the beginning since mRenderUnitIds unmounting won't remove entries there
    // but only from mIndexToMountedItemMap. If an host changes we're going to unmount it and
    // recursively
    // all its mounted children.
    for (int i = 0; i < mRenderUnitIds.length; i++) {
      final int newPosition = mRenderTree.getRenderTreeNodeIndex(mRenderUnitIds[i]);
      final RenderTreeNode renderTreeNode =
          newPosition > -1 ? mRenderTree.getRenderTreeNodeAtIndex(newPosition) : null;
      final MountItem oldItem = getItemAt(i);

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
        unmountItemRecursively(oldItem.getRenderTreeNode());
      } else {
        final long newHostMarker =
            renderTreeNode.getParent() == null
                ? 0L
                : renderTreeNode.getParent().getRenderUnit().getId();
        final Host newHost =
            mIdToMountedItemMap.get(newHostMarker) == null
                ? null
                : (Host) mIdToMountedItemMap.get(newHostMarker).getContent();

        if (oldItem.getHost() != newHost) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItemRecursively(oldItem.getRenderTreeNode());
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
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first RenderTreeNode).
  private MountItem mountContentInHost(
      int index, Object content, Host host, RenderTreeNode renderTreeNode) {
    final MountItem item = new MountItem(renderTreeNode, host, content);

    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIdToMountedItemMap.put(mRenderUnitIds[index], item);
    host.mount(renderTreeNode.getPositionInParent(), item);

    return item;
  }

  private void mountRenderUnit(
      int index, RenderTreeNode renderTreeNode, @Nullable StringBuilder processLogBuilder) {
    // 1. Resolve the correct host to mount our content to.
    final RenderTreeNode hostTreeNode = renderTreeNode.getParent();

    final RenderUnit parentRenderUnit = hostTreeNode.getRenderUnit();
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();

    final MountItem mountItem = mIdToMountedItemMap.get(parentRenderUnit.getId());
    if (mountItem == null) {
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

    final Object parentContent = mountItem.getContent();
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

    final Host host = (Host) parentContent;

    // 2. call the RenderUnit's Mount bindings.
    final Object content = MountItemsPool.acquireMountContent(mContext, renderUnit);

    mountRenderUnitToContent(mMountDelegate, mContext, renderTreeNode, renderUnit, content);

    // 3. Mount the content into the selected host.
    final MountItem item = mountContentInHost(index, content, host, renderTreeNode);

    // 4. Call attach binding functions
    bindRenderUnitToContent(mMountDelegate, mContext, item);

    // 5. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    BoundsUtils.applyBoundsToMountContent(renderTreeNode, item.getContent(), true /* force */);
  }

  private void unmountItemRecursively(RenderTreeNode node) {
    final RenderUnit unit = node.getRenderUnit();
    final MountItem item = mIdToMountedItemMap.get(unit.getId());
    // Already has been unmounted.
    if (item == null) {
      return;
    }

    final Object content = item.getContent();

    // The root host item should never be unmounted as it's a reference
    // to the top-level LithoView.
    if (unit.getId() == ROOT_HOST_ID) {
      return;
    }

    mIdToMountedItemMap.remove(unit.getId());

    final boolean hasUnmountDelegate =
        mUnmountDelegateExtension != null
            && mUnmountDelegateExtension.shouldDelegateUnmount(
                mMountDelegate.getUnmountDelegateExtensionState(), item);

    // Recursively unmount mounted children items.
    // This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    // sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    // sub tree, that will still have mounted items. (Different sequence number on RenderTreeNode
    // id)
    if (node.getChildrenCount() > 0) {
      final Host host = (Host) content;

      // Concurrently remove items therefore traverse backwards.
      for (int i = 0; i < node.getChildrenCount(); i++) {
        unmountItemRecursively(node.getChildAt(i));
      }

      if (!hasUnmountDelegate && host.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    final Host host = item.getHost();

    if (hasUnmountDelegate) {
      mUnmountDelegateExtension.unmount(
          mMountDelegate.getUnmountDelegateExtensionState(), item, host);
    } else {
      if (item.isBound()) {
        unbindRenderUnitFromContent(mMountDelegate, mContext, item);
      }
      host.unmount(node.getPositionInParent(), item);

      if (content instanceof View) {
        ((View) content).setPadding(0, 0, 0, 0);
      }

      unmountRenderUnitFromContent(mContext, node, unit, content, mMountDelegate);

      item.releaseMountContent(mContext);
    }
  }

  @Override
  public void unbindMountItem(MountItem mountItem) {
    if (mountItem.isBound()) {
      unbindRenderUnitFromContent(mMountDelegate, mContext, mountItem);
    }
    final Object content = mountItem.getContent();
    if (content instanceof View) {
      ((View) content).setPadding(0, 0, 0, 0);
    }

    unmountRenderUnitFromContent(
        mContext,
        mountItem.getRenderTreeNode(),
        mountItem.getRenderTreeNode().getRenderUnit(),
        content,
        mMountDelegate);

    mountItem.releaseMountContent(mContext);
  }

  private @Nullable MountItem getItemAt(int i) {
    if (mIdToMountedItemMap == null || mRenderUnitIds == null) {
      return null;
    }

    if (i >= mRenderUnitIds.length) {
      return null;
    }

    return mIdToMountedItemMap.get(mRenderUnitIds[i]);
  }

  private void addExtensions(@Nullable Map<RenderCoreExtension<?, ?>, Object> extensions) {
    if (extensions != null) {
      if (mMountDelegate == null) {
        mMountDelegate = new MountDelegate(this);
      }
      for (Map.Entry<RenderCoreExtension<?, ?>, Object> e : extensions.entrySet()) {
        final MountExtension<?, ?> extension = e.getKey().getMountExtension();
        if (extension != null) {
          mMountDelegate.addExtension(extension);
        }
      }
    }
  }

  private void unregisterAllExtensions() {
    if (mMountDelegate != null) {
      mMountDelegate.unBind();
      mMountDelegate.unMount();

      if (mRenderTree != null && mRenderTree.getExtensionResults() != null) {
        mMountDelegate.unregisterAllExtensions();
      }

      mMountDelegate.releaseAllAcquiredReferences();
    }
  }

  private static void mountRenderUnitToContent(
      final @Nullable MountDelegate mountDelegate,
      final Context context,
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content) {
    unit.mountExtensions(context, content, node.getLayoutData());
    if (mountDelegate != null) {
      mountDelegate.onMountItem(unit, content, node.getLayoutData());
    }
  }

  private static void unmountRenderUnitFromContent(
      final Context context,
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content,
      final @Nullable MountDelegate mountDelegate) {
    unit.unmountExtensions(context, content, node.getLayoutData());
    if (mountDelegate != null) {
      mountDelegate.onUnmountItem(unit, content, node.getLayoutData());
    }
  }

  private static void bindRenderUnitToContent(
      final @Nullable MountDelegate mountDelegate, Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final Object content = item.getContent();
    final Object layoutData = item.getRenderTreeNode().getLayoutData();
    renderUnit.attachExtensions(context, content, layoutData);
    if (mountDelegate != null) {
      mountDelegate.onBindItem(renderUnit, content, layoutData);
    }
    item.setIsBound(true);
  }

  private static void unbindRenderUnitFromContent(
      final @Nullable MountDelegate mountDelegate, Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final Object content = item.getContent();
    final Object layoutData = item.getRenderTreeNode().getLayoutData();
    renderUnit.detachExtensions(context, content, layoutData);
    if (mountDelegate != null) {
      mountDelegate.onUnbindItem(renderUnit, content, layoutData);
    }
    item.setIsBound(false);
  }

  private static void updateMountItemIfNeeded(
      @Nullable MountDelegate mountDelegate,
      Context context,
      RenderTreeNode renderTreeNode,
      MountItem currentMountItem) {
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    final Object newLayoutData = renderTreeNode.getLayoutData();
    final RenderTreeNode currentNode = currentMountItem.getRenderTreeNode();
    final RenderUnit currentRenderUnit = currentNode.getRenderUnit();
    final Object currentLayoutData = currentNode.getLayoutData();
    final Object content = currentMountItem.getContent();

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    currentMountItem.update(renderTreeNode);

    if (currentRenderUnit != renderUnit) {
      RenderCoreSystrace.beginSection("Update Item: ", renderUnit.getDescription());

      renderUnit.updateExtensions(
          context, content, currentRenderUnit, currentLayoutData, newLayoutData);
    }

    if (mountDelegate != null) {
      mountDelegate.onUpdateItemsIfNeeded(
          currentRenderUnit, currentLayoutData, renderUnit, newLayoutData, content);
    }

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem);

    if (currentRenderUnit != renderUnit) {
      RenderCoreSystrace.endSection(); // UPDATE
    }
  }
}
