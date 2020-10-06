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

import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.rendercore.extensions.MountExtension;
import java.util.ArrayList;
import java.util.List;

/**
 * Can be passed to a MountState to override default mounting behaviour and control which items get
 * mounted or unmounted.
 */
public class MountDelegate {

  private final LongSparseArray<Integer> mReferenceCountMap = new LongSparseArray<>();
  private final List<MountExtension> mMountExtensions = new ArrayList<>();
  private final MountDelegateTarget mMountDelegateTarget;
  private boolean mReferenceCountingEnabled = false;

  public MountDelegate(MountDelegateTarget mountDelegateTarget) {
    mMountDelegateTarget = mountDelegateTarget;
  }

  public void addExtension(MountExtension mountExtension) {
    mMountExtensions.add(mountExtension);
    mountExtension.registerToDelegate(this);
    mReferenceCountingEnabled = mReferenceCountingEnabled || mountExtension.canPreventMount();
  }

  void unregisterAllExtensions() {
    resetExtensionReferenceCount();
    mMountExtensions.clear();
    mReferenceCountingEnabled = false;
  }

  void unBind() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onUnbind();
    }
  }

  void unMount() {
    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).onUnmount();
    }
  }

  public Object getContentAt(int position) {
    return mMountDelegateTarget.getContentAt(position);
  }

  public boolean isRootItem(int position) {
    return mMountDelegateTarget.isRootItem(position);
  }

  /** @return true if this item needs to be mounted. */
  public boolean maybeLockForMount(RenderTreeNode renderTreeNode, int index) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    for (int i = 0, size = mMountExtensions.size(); i < size; i++) {
      mMountExtensions.get(i).beforeMountItem(renderTreeNode, index);
    }

    return hasAcquiredRef(renderTreeNode);
  }

  public boolean isLockedForMount(RenderTreeNode renderTreeNode) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    return hasAcquiredRef(renderTreeNode);
  }

  private boolean hasAcquiredRef(RenderTreeNode renderTreeNode) {
    final long renderUnitId = renderTreeNode.getRenderUnit().getId();
    final Integer refCount = mReferenceCountMap.get(renderUnitId);

    return refCount != null && refCount > 0;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void acquireMountRef(RenderTreeNode node, int i, boolean isMounting) {
    incrementExtensionRefCount(node);

    // Only mount if we're during a mounting phase, otherwise the mounting phase will take care of
    // that.
    if (isMounting) {
      mMountDelegateTarget.notifyMount(node, i);
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void releaseMountRef(RenderTreeNode renderTreeNode, int i, boolean isMounting) {
    final boolean wasLockedForMount = isLockedForMount(renderTreeNode);
    decrementExtensionRefCount(renderTreeNode);

    if (wasLockedForMount && !isLockedForMount(renderTreeNode) && isMounting) {
      mMountDelegateTarget.notifyUnmount(i);
    }
  }

  public void resetExtensionReferenceCount() {
    if (!mReferenceCountingEnabled) {
      return;
    }

    for (MountExtension<?> extension : mMountExtensions) {
      extension.resetAcquiredReferences();
    }

    mReferenceCountMap.clear();
  }

  private void incrementExtensionRefCount(RenderTreeNode renderTreeNode) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    final long renderUnitId = renderTreeNode.getRenderUnit().getId();
    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null) {
      refCount = 0;
    }

    mReferenceCountMap.put(renderUnitId, refCount + 1);
  }

  private void decrementExtensionRefCount(RenderTreeNode renderTreeNode) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    final long renderUnitId = renderTreeNode.getRenderUnit().getId();
    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null || refCount == 0) {
      throw new IllegalStateException(
          "Trying to decrement reference count for an item you don't own.");
    }

    mReferenceCountMap.put(renderUnitId, refCount - 1);
  }

  public MountDelegateTarget getMountDelegateTarget() {
    return mMountDelegateTarget;
  }
}
