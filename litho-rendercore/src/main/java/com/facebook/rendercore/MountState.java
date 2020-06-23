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

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import com.facebook.rendercore.MountDelegate.MountDelegateTarget;
import com.facebook.rendercore.utils.BoundsUtils;
import java.util.ArrayList;
import java.util.List;

public class MountState implements MountDelegateTarget {

  public static final long ROOT_HOST_ID = 0L;

  private final LongSparseArray<MountItem> mIndexToMountedItemMap;
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

  /**
   * This boolean array is used to record the attach {@link RenderUnit.Binder} which were unbound in
   * {@link #updateMountItemIfNeeded(Context, RenderTreeNode, MountItem)}.
   */
  private boolean[] mTempUnboundAttachBinders = new boolean[4];

  /**
   * This boolean array is used to record the mount {@link RenderUnit.Binder} which were unbound in
   * {@link #updateMountItemIfNeeded(Context, RenderTreeNode, MountItem)}.
   */
  private boolean[] mTempUnboundMountBinders = new boolean[4];

  public MountState(Host rootHost) {
    mIndexToMountedItemMap = new LongSparseArray<>();
    mContext = rootHost.getContext();
    mRootHost = rootHost;
  }

  public @Nullable Object findMountContentById(long id) {
    if (mIndexToMountedItemMap == null) {
      return null;
    }

    final MountItem item = mIndexToMountedItemMap.get(id);
    if (item != null) {
      return item.getContent();
    }

    return null;
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
  public void notifyMount(
      MountDelegate.MountDelegateInput input, RenderTreeNode renderTreeNode, int position) {
    if (getItemAt(position) != null) {
      return;
    }

    mountRenderUnit(position, renderTreeNode);
  }

  @Override
  public void notifyUnmount(int position) {
    final MountItem mountItem = getItemAt(position);
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

    if (renderTree == mRenderTree && !mNeedsRemount) {
      return;
    }

    mRenderTree = renderTree;
    mIsMounting = true;

    prepareMount();

    // Let's start from 1 as the RenderTreeNode in position 0 always represents the root.
    for (int i = 1, size = renderTree.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = renderTree.getRenderTreeNodeAtIndex(i);
      final MountItem currentMountItem = getItemAt(i);
      final boolean isMounted = currentMountItem != null;

      if (!isMounted) {
        mountRenderUnit(i, renderTreeNode);
      } else {
        updateMountItemIfNeeded(mContext, renderTreeNode, currentMountItem);
      }
    }

    mNeedsRemount = false;
    mIsMounting = false;
  }

  @Override
  public void unmountAllItems() {
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
    MountItem item = mIndexToMountedItemMap.get(ROOT_HOST_ID);
    if (item != null) {
      if (item.isBound()) {
        unbindRenderUnitFromContent(mContext, item);
      }
      mIndexToMountedItemMap.remove(ROOT_HOST_ID);
      unmountRenderUnitFromContent(
          mContext,
          item.getHost(),
          rootRenderTreeNode,
          rootRenderTreeNode.getRenderUnit(),
          item.getContent());
    }

    mNeedsRemount = true;
  }

  @Override
  public boolean isRootItem(int position) {
    final MountItem mountItem = getItemAt(position);
    if (mountItem == null) {
      return false;
    }

    return mountItem == mIndexToMountedItemMap.get(ROOT_HOST_ID);
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
  public Object getContentById(long id) {
    if (mIndexToMountedItemMap == null) {
      return null;
    }

    final MountItem mountItem = mIndexToMountedItemMap.get(id);

    if (mountItem == null) {
      return null;
    }

    return mountItem.getContent();
  }

  @Override
  public int getContentCount() {
    return mRenderUnitIds == null ? 0 : mRenderUnitIds.length;
  }

  @Override
  public void registerMountDelegateExtension(MountDelegateExtension mountDelegateExtension) {
    if (mMountDelegate == null) {
      mMountDelegate = new MountDelegate(this);
    }
    mMountDelegate.addExtension(mountDelegateExtension);
  }

  @Override
  public ArrayList<Host> getHosts() {
    final ArrayList<Host> hosts = new ArrayList<>();
    for (int i = 0, size = mIndexToMountedItemMap.size(); i < size; i++) {
      final MountItem item = mIndexToMountedItemMap.valueAt(i);
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
    return mRenderUnitIds != null ? mRenderUnitIds.length : 0;
  }

  @Override
  public void setUnmountDelegateExtension(UnmountDelegateExtension unmountDelegateExtension) {
    mUnmountDelegateExtension = unmountDelegateExtension;
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
      bindRenderUnitToContent(mContext, mountItem);

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
      MountItem mountItem = getItemAt(i);

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindRenderUnitFromContent(mContext, mountItem);
    }
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

  /** Prepare the {@link MountState} to mount a new {@link RenderTree}. */
  private void prepareMount() {
    unmountOrMoveOldItems();

    final MountItem rootItem = mIndexToMountedItemMap.get(ROOT_HOST_ID);
    final RenderTreeNode rootNode = mRenderTree.getRenderTreeNodeAtIndex(0);
    final RenderUnit rootRenderUnit = rootNode.getRenderUnit();

    // If root mount item is null then mounting root node for the first time.
    if (rootItem == null) {

      // Run mount callbacks.
      mountRenderUnitToContent(mContext, mRootHost, rootNode, rootRenderUnit, mRootHost);

      // Create root mount item.
      final MountItem item = new MountItem(rootNode, mRootHost, mRootHost);

      // Adds root mount item to map.
      mIndexToMountedItemMap.put(ROOT_HOST_ID, item);

      // Run binder callbacks
      bindRenderUnitToContent(mContext, item);

    } else {

      // If root mount item is present then update it.
      updateMountItemIfNeeded(mContext, rootNode, rootItem);
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

      final boolean hasUnmountDelegate =
          mUnmountDelegateExtension != null && oldItem != null
              ? mUnmountDelegateExtension.shouldDelegateUnmount(oldItem)
              : false;

      if (newPosition == -1 || hasUnmountDelegate) {
        // if oldItem is null it was previously unmounted so there is nothing we need to do.
        if (oldItem != null) {
          unmountItemRecursively(oldItem.getRenderTreeNode());
        }
      } else {
        final long newHostMarker =
            renderTreeNode.getParent() == null
                ? 0L
                : renderTreeNode.getParent().getRenderUnit().getId();
        final Host newHost =
            mIndexToMountedItemMap.get(newHostMarker) == null
                ? null
                : (Host) mIndexToMountedItemMap.get(newHostMarker).getContent();
        if (oldItem == null) {
          // This was previously unmounted.
        } else if (oldItem.getHost() != newHost) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItemRecursively(oldItem.getRenderTreeNode());
        } else if (newPosition != i) {
          // If a MountItem for this id exists and the hostMarker has not changed but its position
          // in the outputs array has changed we need to update the position in the Host to ensure
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
    mIndexToMountedItemMap.put(mRenderUnitIds[index], item);
    host.mount(renderTreeNode.getPositionInParent(), item);

    return item;
  }

  private void mountRenderUnit(int index, RenderTreeNode renderTreeNode) {
    // 1. Resolve the correct host to mount our content to.
    final RenderTreeNode hostTreeNode = renderTreeNode.getParent();

    final Host host =
        (Host) mIndexToMountedItemMap.get(hostTreeNode.getRenderUnit().getId()).getContent();

    if (host == null) {
      throw new RuntimeException("Trying to mount a RenderTreeNode but its host is not mounted.");
    }

    // 2. call the RenderUnit's Mount bindings.
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    final Object content = MountItemsPool.acquireMountContent(mContext, renderUnit);

    mountRenderUnitToContent(mContext, host, renderTreeNode, renderUnit, content);

    // 3. Mount the content into the selected host.
    final MountItem item = mountContentInHost(index, content, host, renderTreeNode);

    // 4. Call attach binding functions
    bindRenderUnitToContent(mContext, item);

    // 5. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    BoundsUtils.applyBoundsToMountContent(renderTreeNode, item.getContent(), true /* force */);
  }

  private void unmountItemRecursively(RenderTreeNode node) {

    final RenderUnit unit = node.getRenderUnit();
    final MountItem item = mIndexToMountedItemMap.get(unit.getId());
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
    mIndexToMountedItemMap.remove(unit.getId());

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

      if (host.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    final Host host = item.getHost();

    if (mUnmountDelegateExtension != null
        && mUnmountDelegateExtension.shouldDelegateUnmount(item)) {
      mUnmountDelegateExtension.unmount(node.getPositionInParent(), item, host);
    } else {
      if (item.isBound()) {
        unbindRenderUnitFromContent(mContext, item);
      }
      host.unmount(node.getPositionInParent(), item);

      if (content instanceof View) {
        ((View) content).setPadding(0, 0, 0, 0);
      }

      unmountRenderUnitFromContent(mContext, host, node, unit, content);

      item.releaseMountContent(mContext);
    }
  }

  @Override
  public void unbindMountItem(MountItem mountItem) {
    if (mountItem.isBound()) {
      unbindRenderUnitFromContent(mContext, mountItem);
    }
    final Object content = mountItem.getContent();
    if (content instanceof View) {
      ((View) content).setPadding(0, 0, 0, 0);
    }

    unmountRenderUnitFromContent(
        mContext,
        mountItem.getHost(),
        mountItem.getRenderTreeNode(),
        mountItem.getRenderTreeNode().getRenderUnit(),
        content);

    mountItem.releaseMountContent(mContext);
  }

  private @Nullable MountItem getItemAt(int i) {
    if (mIndexToMountedItemMap == null || mRenderUnitIds == null) {
      return null;
    }

    if (i >= mRenderUnitIds.length) {
      return null;
    }

    return mIndexToMountedItemMap.get(mRenderUnitIds[i]);
  }

  private static void mountRenderUnitToContent(
      final Context context,
      final Host host,
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content) {
    final List<RenderUnit.Binder> mountUnmountFunctions = unit.mountUnmountFunctions();
    if (mountUnmountFunctions != null) {
      for (RenderUnit.Binder binder : mountUnmountFunctions) {
        binder.bind(context, content, unit, node.getLayoutData());
      }
    }
  }

  private static void unmountRenderUnitFromContent(
      final Context context,
      final Host host,
      final RenderTreeNode node,
      final RenderUnit unit,
      final Object content) {
    final List<RenderUnit.Binder> mountUnmountFunctions = unit.mountUnmountFunctions();
    if (mountUnmountFunctions != null) {
      int size = mountUnmountFunctions.size();
      for (int i = size - 1; i >= 0; i--) {
        RenderUnit.Binder binder = mountUnmountFunctions.get(i);
        binder.unbind(context, content, unit, node.getLayoutData());
      }
    }
  }

  private static void bindRenderUnitToContent(Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final List<RenderUnit.Binder> bindingFunctions = renderUnit.attachDetachFunctions();
    if (bindingFunctions != null) {
      for (RenderUnit.Binder binder : bindingFunctions) {
        binder.bind(
            context, item.getContent(), renderUnit, item.getRenderTreeNode().getLayoutData());
      }
    }
    item.setIsBound(true);
  }

  private static void unbindRenderUnitFromContent(Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final List<RenderUnit.Binder> bindingFunctions = renderUnit.attachDetachFunctions();
    if (bindingFunctions != null) {
      int size = bindingFunctions.size();
      for (int i = size - 1; i >= 0; i--) {
        RenderUnit.Binder binder = bindingFunctions.get(i);
        binder.unbind(
            context, item.getContent(), renderUnit, item.getRenderTreeNode().getLayoutData());
      }
    }
    item.setIsBound(false);
  }

  private void updateMountItemIfNeeded(
      Context context, RenderTreeNode renderTreeNode, MountItem currentMountItem) {
    final Host host = currentMountItem.getHost();
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    final RenderTreeNode currentNode = currentMountItem.getRenderTreeNode();
    final RenderUnit currentRenderUnit = currentNode.getRenderUnit();
    final Object content = currentMountItem.getContent();

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    currentMountItem.update(renderTreeNode);

    if (currentRenderUnit != renderUnit) {

      final List<RenderUnit.Binder> attachBinders = renderUnit.attachDetachFunctions();
      final List<RenderUnit.Binder> mountBinders = renderUnit.mountUnmountFunctions();
      final int numberOfAttachBinders;
      final int numberOfMountBinders;

      if (attachBinders != null) {
        numberOfAttachBinders = attachBinders.size();
        if (mTempUnboundAttachBinders.length < numberOfAttachBinders) {
          mTempUnboundAttachBinders = new boolean[numberOfAttachBinders];
        }
      } else {
        numberOfAttachBinders = 0;
      }

      if (mountBinders != null) {
        numberOfMountBinders = mountBinders.size();
        if (mTempUnboundMountBinders.length < numberOfMountBinders) {
          mTempUnboundMountBinders = new boolean[numberOfMountBinders];
        }
      } else {
        numberOfMountBinders = 0;
      }

      // 1. unbind all attach binders which should update.
      unbind(
          context,
          host,
          content,
          currentNode,
          renderTreeNode,
          attachBinders,
          mTempUnboundAttachBinders,
          numberOfAttachBinders);

      // 2. unbind all mount binders which should update.
      unbind(
          context,
          host,
          content,
          currentNode,
          renderTreeNode,
          mountBinders,
          mTempUnboundMountBinders,
          numberOfMountBinders);

      // 3. rebind all mount binder which did update.
      rebind(
          context,
          host,
          content,
          renderTreeNode,
          mountBinders,
          mTempUnboundMountBinders,
          numberOfMountBinders);

      // 4. rebind all attach binder which did update.
      rebind(
          context,
          host,
          content,
          renderTreeNode,
          attachBinders,
          mTempUnboundAttachBinders,
          numberOfAttachBinders);
    }

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem);
  }

  private static void unbind(
      final Context context,
      final Host host,
      final Object content,
      final RenderTreeNode currentNode,
      final RenderTreeNode newNode,
      final List<RenderUnit.Binder> binders,
      final boolean[] unbound,
      final int size) {

    if (size == 0) {
      return;
    }

    final RenderUnit currentRenderUnit = currentNode.getRenderUnit();
    final Object currentLayoutData = currentNode.getLayoutData();
    final RenderUnit newRenderUnit = newNode.getRenderUnit();
    final Object newLayoutData = newNode.getLayoutData();

    // unbind should be called in the reverse order.
    for (int i = size - 1; i >= 0; i--) {
      final RenderUnit.Binder binder = binders.get(i);
      final boolean shouldUpdate =
          binder.shouldUpdate(currentRenderUnit, newRenderUnit, currentLayoutData, newLayoutData);
      if (shouldUpdate) {
        binder.unbind(context, content, currentRenderUnit, currentLayoutData);
        unbound[i] = true;
      } else {
        unbound[i] = false;
      }
    }
  }

  private static void rebind(
      final Context context,
      final Host host,
      final Object content,
      final RenderTreeNode newNode,
      final List<RenderUnit.Binder> binders,
      final boolean[] toRebind,
      final int size) {

    if (size == 0) {
      return;
    }

    final RenderUnit newRenderUnit = newNode.getRenderUnit();
    final Object newLayoutData = newNode.getLayoutData();

    for (int i = 0; i < size; i++) {
      final boolean shouldUpdate = toRebind[i];
      if (shouldUpdate) {
        final RenderUnit.Binder binder = binders.get(i);
        binder.bind(context, content, newRenderUnit, newLayoutData);
      }
    }
  }
}
