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

import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import java.util.List;
import java.util.Map;

class MountState {

  static final long ROOT_HOST_ID = 0L;

  private final LongSparseArray<MountItem> mIndexToMountedItemMap;
  private final Context mContext;
  private final LongSparseArray<Host> mHostsById = new LongSparseArray<>();
  private final Host mRootHost;

  // Updated in prepareMount(), thus during mounting they hold the information
  // about the LayoutState that is being mounted, not mLastMountedLayoutState
  @Nullable private long[] mRenderUnitIds;

  private boolean mIsMounting;
  private boolean mNeedsRemount;
  private RenderTree mRenderTree;

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
  boolean needsRemount() {
    return mNeedsRemount;
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param renderTree a new {@link RenderTree} to mount
   */
  void mount(RenderTree renderTree) {
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

    suppressInvalidationsOnHosts(true);
    prepareMount(renderTree);

    // Let's start from 1 as the RenderTreeNode in position 0 always represents the root.
    for (int i = 1, size = renderTree.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = renderTree.getRenderTreeNodeAtIndex(i);
      final MountItem currentMountItem = getItemAt(i);
      final boolean isMounted = currentMountItem != null;

      if (!isMounted) {
        mountRenderUnit(i, renderTreeNode, renderTree.getLayoutContexts());
      } else {
        updateMountItemIfNeeded(
            mContext, renderTreeNode, renderTree.getLayoutContexts(), currentMountItem);
      }
    }

    mNeedsRemount = false;
    suppressInvalidationsOnHosts(false);
    mIsMounting = false;
  }

  void unmountAllItems() {
    if (mRenderUnitIds == null) {
      return;
    }
    // Let's unmount all the content from the Root host. Everything else will be recursively
    // unmounted from there.
    for (int i = 0; i < mRootHost.getMountItemCount(); i++) {
      unmountItem(mRootHost.getMountItemAt(i));
    }
    mNeedsRemount = true;
  }

  /**
   * This is called when the {@link MountItem}s mounted on this {@link MountState} need to be
   * re-bound with the same RenderUnit. This happens when a detach/attach happens on the root {@link
   * Host} that owns the MountState.
   */
  void attach() {
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
        applyBoundsToMountContent(mountItem.getRenderTreeNode(), view, true);
      }
    }
  }

  /** Unbinds all the MountItems currently mounted on this MountState. */
  void detach() {
    if (mRenderUnitIds == null) {
      return;
    }

    for (int i = 0, size = mRenderUnitIds.length; i < size; i++) {
      MountItem mountItem = getItemAt(i);

      if (mountItem == null || !mountItem.isBound()) {
        continue;
      }

      unbindRenderUnitFromContent(mContext, mountItem);
      mountItem.setIsBound(false);
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

    applyBoundsToMountContent(
        item.getRenderTreeNode(), item.getContent(), forceTraversal /* force */);
  }

  /** Prepare the {@link MountState} to mount a new {@link RenderTree}. */
  private void prepareMount(RenderTree newRenderTree) {
    unmountOrMoveOldItems(newRenderTree);

    if (mHostsById.get(ROOT_HOST_ID) == null) {
      // Mounting always starts with the root host.
      registerHost(ROOT_HOST_ID, mRootHost);
      // Root host is implicitly marked as mounted.
      mIndexToMountedItemMap.put(
          ROOT_HOST_ID,
          new MountItem(
              newRenderTree.getRenderTreeNodeAtIndex(0),
              null,
              mRootHost,
              newRenderTree.getLayoutContexts()));
    }

    final int outputCount = newRenderTree.getMountableOutputCount();
    if (mRenderUnitIds == null || outputCount != mRenderUnitIds.length) {
      mRenderUnitIds = new long[outputCount];
    }

    for (int i = 0; i < outputCount; i++) {
      mRenderUnitIds[i] = newRenderTree.getRenderTreeNodeAtIndex(i).getRenderUnit().getId();
    }
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private void unmountOrMoveOldItems(RenderTree newRenderTree) {
    if (mRenderUnitIds == null) {
      return;
    }

    // Traversing from the beginning since mRenderUnitIds unmounting won't remove entries there
    // but only from mIndexToMountedItemMap. If an host changes we're going to unmount it and
    // recursively
    // all its mounted children.
    for (int i = 0; i < mRenderUnitIds.length; i++) {
      final int newPosition = newRenderTree.getRenderTreeNodeIndex(mRenderUnitIds[i]);
      final RenderTreeNode renderTreeNode =
          newPosition > -1 ? newRenderTree.getRenderTreeNodeAtIndex(newPosition) : null;
      final MountItem oldItem = getItemAt(i);

      if (newPosition == -1) {
        unmountItem(oldItem);
      } else {
        final long newHostMarker =
            renderTreeNode.getParent() == null
                ? 0L
                : renderTreeNode.getParent().getRenderUnit().getId();
        if (oldItem == null) {
          // This was previously unmounted.
        } else if (oldItem.getHost() != mHostsById.get(newHostMarker)) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItem(oldItem);
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
      int index, Object content, Host host, RenderTreeNode renderTreeNode, Map layoutContexts) {
    final MountItem item = new MountItem(renderTreeNode, host, content, layoutContexts);

    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    mIndexToMountedItemMap.put(mRenderUnitIds[index], item);
    host.mount(renderTreeNode.getPositionInParent(), item);

    return item;
  }

  private void mountRenderUnit(int index, RenderTreeNode renderTreeNode, Map layoutContexts) {
    // 1. Resolve the correct host to mount our content to.
    final RenderTreeNode hostTreeNode = renderTreeNode.getParent();
    Host host = mHostsById.get(hostTreeNode.getRenderUnit().getId());

    // 2. call the RenderUnit's Mount bindings.
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    if (renderUnit == null) {
      throw new RuntimeException("Trying to mount a RenderTreeNode with a null Component.");
    }

    final Object content = MountItemsPool.acquireMountContent(mContext, renderUnit);
    final List<RenderUnit.Binder> mountUnmountFunctions = renderUnit.mountUnmountFunctions();
    if (mountUnmountFunctions != null) {
      for (RenderUnit.Binder binder : mountUnmountFunctions) {
        binder.bind(mContext, content, renderUnit, layoutContexts);
      }
    }

    // 3. If it's a ComponentHost, add the mounted View to the list of Hosts.
    if (content instanceof Host) {
      Host hostContent = (Host) content;
      registerHost(renderUnit.getId(), hostContent);
    }

    // 4. Mount the content into the selected host.
    final MountItem item = mountContentInHost(index, content, host, renderTreeNode, layoutContexts);

    // 5. Call attach binding functions
    bindRenderUnitToContent(mContext, item);

    // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
    // bind might have triggered a layout request within a View.
    applyBoundsToMountContent(renderTreeNode, item.getContent(), true /* force */);
  }

  private void unmountItem(MountItem item) {
    // Already has been unmounted.
    if (item == null) {
      return;
    }
    final Object content = item.getContent();

    // The root host item should never be unmounted as it's a reference
    // to the top-level LithoView.
    if (item.getRenderUnit().getId() == ROOT_HOST_ID) {
      return;
    }
    mIndexToMountedItemMap.remove(item.getRenderUnit().getId());

    // Recursively unmount mounted children items.
    // This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
    // sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
    // sub tree, that will still have mounted items. (Different sequence number on RenderTreeNode
    // id)
    if (content instanceof Host) {
      final Host host = (Host) content;

      // Concurrently remove items therefore traverse backwards.
      for (int i = host.getMountItemCount() - 1; i >= 0; i--) {
        final MountItem mountItem = host.getMountItemAt(i);
        unmountItem(mountItem);
      }

      if (host.getMountItemCount() > 0) {
        throw new IllegalStateException(
            "Recursively unmounting items from a ComponentHost, left"
                + " some items behind maybe because not tracked by its MountState");
      }
    }

    final Host host = item.getHost();
    host.unmount(item.getRenderTreeNode().getPositionInParent(), item);

    if (content instanceof Host) {
      mHostsById.remove(item.getRenderUnit().getId());
    } else if (content instanceof View) {
      ((View) content).setPadding(0, 0, 0, 0);
    }

    if (item.isBound()) {
      unbindRenderUnitFromContent(mContext, item);
    }

    final RenderUnit renderUnit = item.getRenderUnit();
    final List<RenderUnit.Binder> mountUnmountFunctions = renderUnit.mountUnmountFunctions();
    if (mountUnmountFunctions != null) {
      for (RenderUnit.Binder binder : mountUnmountFunctions) {
        binder.unbind(mContext, item.getContent(), renderUnit, item.getLayoutContexts());
      }
    }

    item.releaseMountContent(mContext);
  }

  private void registerHost(long id, Host host) {
    host.suppressInvalidations(true);
    mHostsById.put(id, host);
  }

  private void suppressInvalidationsOnHosts(boolean suppressInvalidations) {
    for (int i = mHostsById.size() - 1; i >= 0; i--) {
      mHostsById.valueAt(i).suppressInvalidations(suppressInvalidations);
    }
  }

  private MountItem getItemAt(int i) {
    return mIndexToMountedItemMap.get(mRenderUnitIds[i]);
  }

  private static void unbindRenderUnitFromContent(Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final List<RenderUnit.Binder> bindingFunctions = renderUnit.attachDetachFunctions();
    if (bindingFunctions != null) {
      for (RenderUnit.Binder binder : bindingFunctions) {
        binder.unbind(context, item.getContent(), renderUnit, item.getLayoutContexts());
      }
    }

    item.setIsBound(false);
  }

  private static void applyBoundsToMountContent(
      RenderTreeNode renderTreeNode, Object content, boolean force) {

    if (content instanceof View) {
      applyBoundsToView((View) content, renderTreeNode, force);
    } else if (content instanceof Drawable) {
      final Rect bounds = renderTreeNode.getBounds();
      final Rect padding = renderTreeNode.getResolvedPadding();
      int left = bounds.left;
      int top = bounds.top;
      int right = bounds.right;
      int bottom = bounds.bottom;
      if (padding != null) {
        left += padding.left;
        top += padding.top;
        right -= padding.right;
        bottom -= padding.bottom;
      }
      ((Drawable) content).setBounds(left, top, right, bottom);
    } else {
      throw new IllegalStateException("Unsupported mounted content " + content);
    }
  }

  /**
   * Sets the bounds on the given view if the view doesn't already have those bounds (or if 'force'
   * is supplied).
   */
  private static void applyBoundsToView(View view, RenderTreeNode renderTreeNode, boolean force) {
    final Rect bounds = renderTreeNode.getBounds();
    final int width = bounds.right - bounds.left;
    final int height = bounds.bottom - bounds.top;

    final Rect padding = renderTreeNode.getResolvedPadding();

    if (padding != null && !(view instanceof Host)) {
      view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
    }

    if (force || view.getMeasuredHeight() != height || view.getMeasuredWidth() != width) {
      view.measure(
          makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    if (force
        || view.getLeft() != bounds.left
        || view.getTop() != bounds.top
        || view.getRight() != bounds.right
        || view.getBottom() != bounds.bottom) {
      view.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }
  }

  private static void bindRenderUnitToContent(Context context, MountItem item) {
    final RenderUnit renderUnit = item.getRenderUnit();
    final List<RenderUnit.Binder> bindingFunctions = renderUnit.attachDetachFunctions();
    if (bindingFunctions != null) {
      for (RenderUnit.Binder binder : bindingFunctions) {
        binder.bind(context, item.getContent(), renderUnit, item.getLayoutContexts());
      }
    }
    item.setIsBound(true);
  }

  private static void updateMountItemIfNeeded(
      Context context,
      RenderTreeNode renderTreeNode,
      Map layoutContexts,
      MountItem currentMountItem) {
    final RenderUnit renderUnit = renderTreeNode.getRenderUnit();
    final RenderUnit currentRenderUnit = currentMountItem.getRenderUnit();
    final Object content = currentMountItem.getContent();

    if (currentRenderUnit != renderUnit) {
      // Let's check which attach/detach bindings need to happen with the new data.
      final List<RenderUnit.Binder> attachDetachFunctions = renderUnit.attachDetachFunctions();
      if (attachDetachFunctions != null) {
        for (RenderUnit.Binder binder : attachDetachFunctions) {
          if (binder.shouldUpdate(
              currentRenderUnit,
              renderUnit,
              currentMountItem.getLayoutContexts(),
              layoutContexts)) {
            binder.unbind(
                context, content, currentRenderUnit, currentMountItem.getLayoutContexts());
            binder.bind(context, content, renderUnit, layoutContexts);
          }
        }
      }

      // And then let's do the same thing with mount funtions.
      final List<RenderUnit.Binder> mountUnmountFunctions = renderUnit.mountUnmountFunctions();
      if (mountUnmountFunctions != null) {
        for (RenderUnit.Binder binder : mountUnmountFunctions) {
          if (binder.shouldUpdate(
              currentRenderUnit,
              renderUnit,
              currentMountItem.getLayoutContexts(),
              layoutContexts)) {
            binder.unbind(
                context, content, currentRenderUnit, currentMountItem.getLayoutContexts());
            binder.bind(context, content, renderUnit, layoutContexts);
          }
        }
      }
    }

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    currentMountItem.update(renderTreeNode, layoutContexts);

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem);
  }
}
