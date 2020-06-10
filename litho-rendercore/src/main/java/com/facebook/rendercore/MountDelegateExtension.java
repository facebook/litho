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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mount extension which can be registered by a MountState as an extension which can override
 * mounting behaviour. MountState will rely on the extensions registered on the MountDelegate to
 * decide what to mount or unmount. If no extensions are registered on the MountState's delegate, it
 * falls back to its default behaviour.
 */
public class MountDelegateExtension {

  private Set<Long> mLayoutOutputMountRefs = new HashSet<>();
  private MountDelegate mMountDelegate;

  public void registerToDelegate(MountDelegate mountDelegate) {
    mMountDelegate = mountDelegate;
  }

  protected void resetAcquiredReferences() {
    mLayoutOutputMountRefs = new HashSet<>();
  }

  protected boolean isRootItem(int position) {
    return mMountDelegate.isRootItem(position);
  }

  protected Object getContentAt(int position) {
    return mMountDelegate.getContentAt(position);
  }

  protected void acquireMountReference(
      RenderTreeNode renderTreeNode,
      int position,
      MountDelegate.MountDelegateInput input,
      boolean isMounting) {
    if (ownsReference(renderTreeNode)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    mLayoutOutputMountRefs.add(renderTreeNode.getRenderUnit().getId());
    mMountDelegate.acquireMountRef(renderTreeNode, position, input, isMounting);
  }

  protected void releaseMountReference(
      RenderTreeNode renderTreeNode, int position, boolean isMounting) {
    if (!ownsReference(renderTreeNode)) {
      throw new IllegalStateException("Trying to release a reference that wasn't acquired.");
    }

    mLayoutOutputMountRefs.remove(renderTreeNode.getRenderUnit().getId());
    mMountDelegate.releaseMountRef(renderTreeNode, position, isMounting);
  }

  protected boolean isLockedForMount(RenderTreeNode renderTreeNode) {
    return mMountDelegate.isLockedForMount(renderTreeNode);
  }

  // TODO make protected when removing isAnimationLocked.
  public boolean ownsReference(RenderTreeNode renderTreeNode) {
    return mLayoutOutputMountRefs.contains(renderTreeNode.getRenderUnit().getId());
  }

  @VisibleForTesting(otherwise = 4)
  public boolean canPreventMount() {
    return false;
  }

  public void onUmountItem(Object item, long layoutOutputId) {}

  public MountDelegate.MountDelegateTarget getMountTarget() {
    return mMountDelegate.getMountDelegateTarget();
  }

  protected boolean isAnimationLocked(RenderTreeNode renderTreeNode, int position) {
    return mMountDelegate.isAnimationLocked(renderTreeNode, position);
  }

  protected List<MountDelegateExtension> getMountDelegateExtensions() {
    return mMountDelegate.getMountDelegateExtensions();
  }
}
