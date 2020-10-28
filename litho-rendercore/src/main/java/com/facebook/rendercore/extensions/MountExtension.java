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
public abstract class MountExtension<Input, State> {

  private Set<Long> mLayoutOutputMountRefs = new HashSet<>();

  public final ExtensionState<State> createExtensionState(@Nullable MountDelegate mountDelegate) {
    return new ExtensionState(mountDelegate, createState());
  }

  protected abstract State createState();

  public void resetAcquiredReferences() {
    mLayoutOutputMountRefs = new HashSet<>();
  }

  protected static @Nullable Host getRootHost(@Nullable ExtensionState extensionState) {
    if (extensionState == null) {
      return null;
    }

    final MountDelegate mountDelegate = extensionState.getMountDelegate();

    if (mountDelegate == null) {
      return null;
    }

    MountItem root = mountDelegate.getMountDelegateTarget().getRootItem();
    if (root != null) {
      return (Host) root.getContent();
    } else {
      return null;
    }
  }

  protected static boolean isRootItem(ExtensionState extensionState, int position) {
    return extensionState.getMountDelegate().isRootItem(position);
  }

  protected static Object getContentAt(ExtensionState extensionState, int position) {
    return extensionState.getMountDelegate().getContentAt(position);
  }

  protected void acquireMountReference(
      ExtensionState<State> extensionState, RenderTreeNode node, int position, boolean isMounting) {
    acquireMountReference(extensionState, node.getRenderUnit().getId(), position, isMounting);
  }

  protected void acquireMountReference(
      ExtensionState<State> extensionState, long id, int position, boolean isMounting) {
    if (ownsReference(id)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    mLayoutOutputMountRefs.add(id);
    extensionState.getMountDelegate().acquireMountRef(id, position, isMounting);
  }

  protected void releaseMountReference(
      ExtensionState<State> extensionState,
      RenderTreeNode renderTreeNode,
      int position,
      boolean isMounting) {
    releaseMountReference(
        extensionState, renderTreeNode.getRenderUnit().getId(), position, isMounting);
  }

  protected void releaseMountReference(
      ExtensionState<State> extensionState, long id, int position, boolean isMounting) {
    if (!ownsReference(id)) {
      throw new IllegalStateException("Trying to release a reference that wasn't acquired.");
    }

    mLayoutOutputMountRefs.remove(id);
    extensionState.getMountDelegate().releaseMountRef(id, position, isMounting);
  }

  protected static boolean isLockedForMount(
      ExtensionState extensionState, RenderTreeNode renderTreeNode) {
    return isLockedForMount(extensionState, renderTreeNode.getRenderUnit().getId());
  }

  protected static boolean isLockedForMount(ExtensionState extensionState, long id) {
    return extensionState.getMountDelegate().isLockedForMount(id);
  }

  // TODO: T68620328 This method should be roll back to being protected once the transition
  // extension test ends.
  public boolean ownsReference(RenderTreeNode renderTreeNode) {
    return ownsReference(renderTreeNode.getRenderUnit().getId());
  }

  public boolean ownsReference(long id) {
    return mLayoutOutputMountRefs.contains(id);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public boolean canPreventMount() {
    return false;
  }

  public static MountDelegateTarget getMountTarget(ExtensionState extensionState) {
    return extensionState.getMountDelegate().getMountDelegateTarget();
  }

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param extensionState The inner state of this extension when beforeMount is called.
   * @param input The new input the extension should use.
   */
  public void beforeMount(
      ExtensionState<State> extensionState, Input input, @Nullable Rect localVisibleRect) {}

  public void beforeMountItem(
      ExtensionState<State> extensionState, RenderTreeNode renderTreeNode, int index) {}

  /** Called immediately after mounting. */
  public void afterMount(ExtensionState<State> extensionState) {}

  /** Called when the visible bounds of the Host change. */
  public void onVisibleBoundsChanged(
      ExtensionState<State> extensionState, @Nullable Rect localVisibleRect) {}

  /** Called after all the Host's children have been unmounted. */
  public void onUnmount(ExtensionState<State> extensionState) {}

  /** Called after all the Host's children have been unbound. */
  public void onUnbind(ExtensionState<State> extensionState) {}
}
