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

  private final MountDelegate mMountDelegate;
  private final State mState;
  private final Set<Long> mLayoutOutputMountRefs = new HashSet<>();

  ExtensionState(final MountDelegate mountDelegate, final State state) {
    mMountDelegate = mountDelegate;
    mState = state;
  }

  public @Nullable Host getRootHost() {
    MountItem root = mMountDelegate.getMountDelegateTarget().getRootItem();
    if (root != null) {
      return (Host) root.getContent();
    } else {
      return null;
    }
  }

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

  public void acquireMountReference(
      final RenderTreeNode node, final int position, final boolean isMounting) {
    acquireMountReference(node.getRenderUnit().getId(), position, isMounting);
  }

  public void acquireMountReference(final long id, final int position, final boolean isMounting) {
    if (ownsReference(id)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    mLayoutOutputMountRefs.add(id);
    mMountDelegate.acquireMountRef(id, isMounting);
  }

  public void releaseMountReference(
      final RenderTreeNode renderTreeNode, final int position, final boolean isMounting) {
    releaseMountReference(renderTreeNode.getRenderUnit().getId(), position, isMounting);
  }

  public void releaseMountReference(final long id, final int position, final boolean isMounting) {
    if (!ownsReference(id)) {
      throw new IllegalStateException("Trying to release a reference that wasn't acquired.");
    }

    mLayoutOutputMountRefs.remove(id);
    mMountDelegate.releaseMountRef(id, position, isMounting);
  }

  // TODO: T68620328 This method should be roll back to being protected once the transition
  // extension test ends.
  public boolean ownsReference(final RenderTreeNode renderTreeNode) {
    return ownsReference(renderTreeNode.getRenderUnit().getId());
  }

  public boolean ownsReference(final long id) {
    return mLayoutOutputMountRefs.contains(id);
  }
}
