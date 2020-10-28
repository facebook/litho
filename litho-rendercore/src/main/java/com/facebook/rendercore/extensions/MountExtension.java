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

/**
 * Mount extension which can be registered by a MountState as an extension which can override
 * mounting behaviour. MountState will rely on the extensions registered on the MountDelegate to
 * decide what to mount or unmount. If no extensions are registered on the MountState's delegate, it
 * falls back to its default behaviour.
 */
@OkToExtend
public abstract class MountExtension<Input, State> {

  public final ExtensionState<State> createExtensionState(@Nullable MountDelegate mountDelegate) {
    return new ExtensionState(mountDelegate, createState());
  }

  protected abstract State createState();

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

  protected static boolean isLockedForMount(
      ExtensionState extensionState, RenderTreeNode renderTreeNode) {
    return isLockedForMount(extensionState, renderTreeNode.getRenderUnit().getId());
  }

  protected static boolean isLockedForMount(ExtensionState extensionState, long id) {
    return extensionState.getMountDelegate().isLockedForMount(id);
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
