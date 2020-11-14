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
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;

/**
 * Mount extension which can be registered by a MountState as an extension which can override
 * mounting behaviour. MountState will rely on the extensions registered on the MountDelegate to
 * decide what to mount or unmount. If no extensions are registered on the MountState's delegate, it
 * falls back to its default behaviour.
 */
@OkToExtend
public abstract class MountExtension<Input, State> {

  public final ExtensionState<State> createExtensionState(final MountDelegate mountDelegate) {
    return new ExtensionState<>(mountDelegate, createState());
  }

  protected abstract State createState();

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public boolean canPreventMount() {
    return false;
  }

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param extensionState The inner state of this extension when beforeMount is called.
   * @param input The new input the extension should use.
   */
  public void beforeMount(
      final ExtensionState<State> extensionState,
      final Input input,
      final @Nullable Rect localVisibleRect) {}

  public void beforeMountItem(
      final ExtensionState<State> extensionState,
      final RenderTreeNode renderTreeNode,
      final int index) {}

  /** Called immediately after mounting. */
  public void afterMount(final ExtensionState<State> extensionState) {}

  /** Called when the visible bounds of the Host change. */
  public void onVisibleBoundsChanged(
      final ExtensionState<State> extensionState, final @Nullable Rect localVisibleRect) {}

  /** Called after all the Host's children have been unmounted. */
  public void onUnmount(ExtensionState<State> extensionState) {}

  /** Called after all the Host's children have been unbound. */
  public void onUnbind(ExtensionState<State> extensionState) {}

  /** Called after an item is bound, after it gets mounted or updated. */
  public void onBindItem(
      final ExtensionState<State> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {}

  /** Called after an item is unbound. */
  public void onUnbindItem(
      final ExtensionState<State> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {}

  /** Called after an item is unmounted. */
  public void onUnmountItem(
      final ExtensionState<State> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {}

  /** Called after an item is mounted. */
  public void onMountItem(
      final ExtensionState<State> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {}

  public static MountDelegateTarget getMountTarget(final ExtensionState<?> extensionState) {
    return extensionState.getMountDelegate().getMountDelegateTarget();
  }

  protected static boolean isRootItem(final long id) {
    return id == MountState.ROOT_HOST_ID;
  }

  protected static Object getContentAt(final ExtensionState<?> extensionState, final int position) {
    return extensionState.getMountDelegate().getContentAt(position);
  }

  protected static Object getContentById(final ExtensionState<?> extensionState, final long id) {
    return extensionState.getMountDelegate().getContentById(id);
  }

  protected static boolean isLockedForMount(
      final ExtensionState<?> extensionState, final RenderTreeNode renderTreeNode) {
    return isLockedForMount(extensionState, renderTreeNode.getRenderUnit().getId());
  }

  protected static boolean isLockedForMount(ExtensionState extensionState, long id) {
    return extensionState.getMountDelegate().isLockedForMount(id);
  }
}
