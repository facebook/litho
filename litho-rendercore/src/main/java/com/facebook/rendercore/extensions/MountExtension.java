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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.OkToExtend;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import java.util.HashSet;
import java.util.Set;

/**
 * Mount extension which can be registered by a MountState as an extension which can override
 * mounting behaviour. MountState will rely on the extensions registered on the MountDelegate to
 * decide what to mount or unmount. If no extensions are registered on the MountState's delegate, it
 * falls back to its default behaviour.
 */
@OkToExtend
public class MountExtension<Input> {

  private Set<Long> mLayoutOutputMountRefs = new HashSet<>();
  private MountDelegate mMountDelegate;

  public void registerToDelegate(MountDelegate mountDelegate) {
    mMountDelegate = mountDelegate;
  }

  protected void resetAcquiredReferences() {
    mLayoutOutputMountRefs = new HashSet<>();
  }

  protected final @Nullable Host getRootHost() {
    MountItem root = mMountDelegate.getMountDelegateTarget().getRootItem();
    if (root != null) {
      return (Host) root.getContent();
    } else {
      return null;
    }
  }

  protected boolean isRootItem(int position) {
    return mMountDelegate.isRootItem(position);
  }

  protected Object getContentAt(int position) {
    return mMountDelegate.getContentAt(position);
  }

  protected void acquireMountReference(RenderTreeNode node, int position, boolean isMounting) {
    if (ownsReference(node)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    mLayoutOutputMountRefs.add(node.getRenderUnit().getId());
    mMountDelegate.acquireMountRef(node, position, isMounting);
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

  // TODO: T68620328 This method should be roll back to being protected once the transition
  // extension test ends.
  public boolean ownsReference(RenderTreeNode renderTreeNode) {
    return ownsReference(renderTreeNode.getRenderUnit().getId());
  }

  protected boolean ownsReference(long id) {
    return mLayoutOutputMountRefs.contains(id);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public boolean canPreventMount() {
    return false;
  }

  public MountDelegateTarget getMountTarget() {
    return mMountDelegate.getMountDelegateTarget();
  }

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param input The new input the extension should use.
   */
  public void beforeMount(Input input, @Nullable Rect localVisibleRect) {}

  public void beforeMountItem(RenderTreeNode renderTreeNode, int index) {}

  /** Called immediately after mounting. */
  public void afterMount() {}

  /** Called when the visible bounds of the Host change. */
  public void onVisibleBoundsChanged(@Nullable Rect localVisibleRect) {}

  /** Called after all the Host's children have been unmounted. */
  public void onUnmount() {}

  /** Called after all the Host's children have been unbound. */
  public void onUnbind() {}
}
