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

import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.DESTROYED;

import com.facebook.infer.annotation.Nullsafe;

/**
 * LithoLifecycleProvider implementation that can be used to subscribe a nested ComponentTree to
 * listen to state changes of the lifecycle provider that the parent ComponentTree is also
 * subscribed to.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleNestedTreeLifecycleProvider
    implements LithoLifecycleProvider, LithoLifecycleListener {
  private final LithoLifecycleProviderDelegate mLithoLifecycleDelegate =
      new LithoLifecycleProviderDelegate();

  public SimpleNestedTreeLifecycleProvider(ComponentTree parentComponentTree) {
    final LithoLifecycleProvider parentLifecycle = parentComponentTree.mLifecycleProvider;
    if (parentLifecycle != null) {
      parentLifecycle.addListener(this);
    }
  }

  @Override
  public LithoLifecycle getLifecycleStatus() {
    return mLithoLifecycleDelegate.getLifecycleStatus();
  }

  @Override
  public void moveToLifecycle(LithoLifecycle lithoLifecycle) {
    mLithoLifecycleDelegate.moveToLifecycle(lithoLifecycle);
  }

  @Override
  public void addListener(LithoLifecycleListener listener) {
    mLithoLifecycleDelegate.addListener(listener);
  }

  @Override
  public void removeListener(LithoLifecycleListener listener) {
    mLithoLifecycleDelegate.removeListener(listener);
  }

  @Override
  public void onMovedToState(LithoLifecycle state) {
    switch (state) {
      case HINT_VISIBLE:
        moveToLifecycle(LithoLifecycle.HINT_VISIBLE);
        return;
      case HINT_INVISIBLE:
        moveToLifecycle(LithoLifecycle.HINT_INVISIBLE);
        return;
      case DESTROYED:
        moveToLifecycle(DESTROYED);
        return;
      default:
        throw new IllegalStateException("Illegal state: " + state);
    }
  }
}
