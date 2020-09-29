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

package com.facebook.rendercore.testing;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.extensions.MountExtension;

public class TestMountExtension extends MountExtension {

  private Object state;

  @Override
  public void registerToDelegate(MountDelegate mountDelegate) {
    super.registerToDelegate(mountDelegate);
  }

  @Override
  protected void resetAcquiredReferences() {
    super.resetAcquiredReferences();
  }

  @Override
  protected boolean isRootItem(int position) {
    return super.isRootItem(position);
  }

  @Override
  protected Object getContentAt(int position) {
    return super.getContentAt(position);
  }

  @Override
  protected void acquireMountReference(RenderTreeNode node, int position, boolean isMounting) {
    super.acquireMountReference(node, position, isMounting);
  }

  @Override
  protected void releaseMountReference(
      RenderTreeNode renderTreeNode, int position, boolean isMounting) {
    super.releaseMountReference(renderTreeNode, position, isMounting);
  }

  @Override
  protected boolean isLockedForMount(RenderTreeNode renderTreeNode) {
    return super.isLockedForMount(renderTreeNode);
  }

  @Override
  public boolean ownsReference(RenderTreeNode renderTreeNode) {
    return super.ownsReference(renderTreeNode);
  }

  @Override
  protected boolean ownsReference(long id) {
    return super.ownsReference(id);
  }

  @Override
  public boolean canPreventMount() {
    return super.canPreventMount();
  }

  @Override
  public MountDelegateTarget getMountTarget() {
    return super.getMountTarget();
  }

  @Override
  public void beforeMount(Object o, @Nullable Rect localVisibleRect) {
    super.beforeMount(o, localVisibleRect);
    this.state = o;
  }

  @Override
  public void afterMount() {
    super.afterMount();
  }

  @Override
  public void onVisibleBoundsChanged(@Nullable Rect localVisibleRect) {
    super.onVisibleBoundsChanged(localVisibleRect);
  }

  @Override
  public void onUnmount() {
    super.onUnmount();
  }

  @Override
  public void onUnbind() {
    super.onUnbind();
  }

  public Object getState() {
    return state;
  }
}
