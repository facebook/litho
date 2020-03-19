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

package com.facebook.litho;

import java.util.HashSet;
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

  // TODO remove when we have a MountDelegateExtension for transitions.
  protected boolean isAnimationLocked(int position) {
    return mMountDelegate.isAnimationLocked(position);
  }

  protected MountItem getRootMountItem() {
    return mMountDelegate.getRootMountItem();
  }

  protected MountItem getItemAt(int position) {
    return mMountDelegate.getItemAt(position);
  }

  protected void acquireMountReference(
      LayoutOutput layoutOutput,
      int position,
      MountDelegate.MountDelegateInput input,
      boolean isMounting) {
    if (ownsReference(layoutOutput)) {
      throw new IllegalStateException("Cannot acquire the same reference more than once.");
    }

    mLayoutOutputMountRefs.add(layoutOutput.getId());
    mMountDelegate.acquireMountRef(layoutOutput, position, input, isMounting);
  }

  protected void releaseMountReference(LayoutOutput layoutOutput, int position) {
    if (!ownsReference(layoutOutput)) {
      throw new IllegalStateException("Trying to release a reference that wasn't acquired.");
    }

    mLayoutOutputMountRefs.remove(layoutOutput.getId());
    mMountDelegate.releaseMountRef(layoutOutput, position);
  }

  protected boolean isLockedForMount(LayoutOutput layoutOutput) {
    return mMountDelegate.isLockedForMount(layoutOutput);
  }

  protected boolean ownsReference(LayoutOutput layoutOutput) {
    return mLayoutOutputMountRefs.contains(layoutOutput.getId());
  }

  protected boolean canPreventMount() {
    return false;
  }
}
