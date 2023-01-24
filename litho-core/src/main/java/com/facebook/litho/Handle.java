/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Instances of this class are used to uniquely identify {@link Component}s for triggering external
 * events including showing a {@link LithoTooltip} or triggering an {@Link OnTrigger} event.
 *
 * @see {@link LithoTooltipController}
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class Handle {

  private @Nullable StateUpdater mStateUpdater;
  private @Nullable MountedViewReference mMountedViewReference;

  /**
   * @param stateUpdater Set {@link StateUpdater} when binding trigger handles so that this handle
   *     can be associated with only one ComponentTree and we can access the right event trigger
   *     later.
   */
  void setStateUpdaterAndRootViewReference(
      final StateUpdater stateUpdater, MountedViewReference mountedViewReference) {
    mStateUpdater = stateUpdater;
    mMountedViewReference = mountedViewReference;
  }

  @Nullable
  public MountedViewReference getMountedViewReference() {
    return mMountedViewReference;
  }

  /**
   * @return {@link StateUpdater} associated with this handle object which is used to find the
   *     correct event trigger as handle can be used across multiple component trees. For example if
   *     handle is set on a Component and we want to trigger event on it from one of the children in
   *     Section list, we should have access to Component's component tree.
   */
  @Nullable
  public StateUpdater getStateUpdater() {
    return mStateUpdater;
  }
}
