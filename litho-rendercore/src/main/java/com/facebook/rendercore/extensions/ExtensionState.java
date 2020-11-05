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

import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import java.util.HashSet;
import java.util.Set;

public class ExtensionState<State> {
  private final @Nullable MountDelegate mMountDelegate;
  private final State mState;
  private final Set<Long> mLayoutOutputMountRefs = new HashSet<>();

  ExtensionState(final @Nullable MountDelegate mountDelegate, final State state) {
    mMountDelegate = mountDelegate;
    mState = state;
  }

  public @Nullable Host getRootHost() {
    if (mMountDelegate == null) {
      return null;
    }

    MountItem root = mMountDelegate.getMountDelegateTarget().getRootItem();
    if (root != null) {
      return (Host) root.getContent();
    } else {
      return null;
    }
  }

  @Nullable
  public MountDelegate getMountDelegate() {
    return mMountDelegate;
  }

  public State getState() {
    return mState;
  }

  public void releaseAllAcquiredReferences() {
    for (Long id : mLayoutOutputMountRefs) {
      mMountDelegate.releaseMountRef(id, 0, false);
    }
    mLayoutOutputMountRefs.clear();
  }

  public void acquireMountReference(RenderTreeNode node, int position, boolean isMounting) {
    acquireMountReference(node.getRenderUnit().getId(), position, isMounting);
  }

  public void acquireMountReference(long id, int position, boolean isMounting) {
    if (ownsReference(id)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    assertMountDelegate();

    mLayoutOutputMountRefs.add(id);
    mMountDelegate.acquireMountRef(id, position, isMounting);
  }

  public void releaseMountReference(
      RenderTreeNode renderTreeNode, int position, boolean isMounting) {
    releaseMountReference(renderTreeNode.getRenderUnit().getId(), position, isMounting);
  }

  public void releaseMountReference(long id, int position, boolean isMounting) {
    if (!ownsReference(id)) {
      throw new IllegalStateException("Trying to release a reference that wasn't acquired.");
    }

    assertMountDelegate();

    mLayoutOutputMountRefs.remove(id);
    mMountDelegate.releaseMountRef(id, position, isMounting);
  }

  // TODO: T68620328 This method should be roll back to being protected once the transition
  // extension test ends.
  public boolean ownsReference(RenderTreeNode renderTreeNode) {
    return ownsReference(renderTreeNode.getRenderUnit().getId());
  }

  public boolean ownsReference(long id) {
    return mLayoutOutputMountRefs.contains(id);
  }

  private void assertMountDelegate() {
    if (mMountDelegate == null) {
      throw new IllegalStateException(
          "Cannot acquire or release mount references without a MountDelegate.");
    }
  }
}
