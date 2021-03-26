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

import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Implementation of {@link Attachable} for layout specs. Handles dispatching {@link
 * com.facebook.litho.annotations.OnAttached} and {@link com.facebook.litho.annotations.OnDetached}
 * to the given Component when the Attachable is attached/detached.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
final class LayoutSpecAttachable implements Attachable {

  private final String mGlobalKey;
  private final Component mComponent;

  public LayoutSpecAttachable(String globalKey, Component component) {
    mGlobalKey = globalKey;
    mComponent = component;
  }

  @Override
  public String getUniqueId() {
    return mGlobalKey;
  }

  @SuppressWarnings("CatchGeneralException")
  @Override
  public void attach(LayoutStateContext layoutStateContext) {
    final ComponentContext scopedContext =
        Preconditions.checkNotNull(mComponent.getScopedContext(layoutStateContext, mGlobalKey));

    try {
      mComponent.onAttached(scopedContext);
    } catch (Exception e) {
      ComponentUtils.handle(scopedContext, e);
    }
  }

  @SuppressWarnings("CatchGeneralException")
  @Override
  public void detach(LayoutStateContext layoutStateContext) {
    final ComponentContext scopedContext =
        Preconditions.checkNotNull(mComponent.getScopedContext(layoutStateContext, mGlobalKey));

    try {
      mComponent.onDetached(scopedContext);
    } catch (Exception e) {
      ComponentUtils.handle(scopedContext, e);
    }
  }

  @Override
  public boolean shouldUpdate(Attachable nextAttachable) {
    return false;
  }

  @Override
  public boolean useLegacyUpdateBehavior() {
    return true;
  }
}
