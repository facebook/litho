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

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.DESTROYED;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_INVISIBLE;
import static com.facebook.litho.LithoLifecycleProvider.LithoLifecycle.HINT_VISIBLE;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import com.facebook.infer.annotation.Nullsafe;

/**
 * This LithoLifecycleProvider implementation dispatches to the registered observers the lifecycle
 * state changes triggered by the provided LifecycleOwner. For example, if a Fragment is passed as
 * param, the observers will be registered to listen to all of the fragment's lifecycle state
 * changes.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class AOSPLithoLifecycleProvider implements LithoLifecycleProvider, LifecycleObserver {
  private LithoLifecycleProviderDelegate mLithoLifecycleProviderDelegate;

  public AOSPLithoLifecycleProvider(LifecycleOwner lifecycleOwner) {
    lifecycleOwner.getLifecycle().addObserver(this);
    mLithoLifecycleProviderDelegate = new LithoLifecycleProviderDelegate();
  }

  @Override
  public LithoLifecycle getLifecycleStatus() {
    return mLithoLifecycleProviderDelegate.getLifecycleStatus();
  }

  @Override
  public void moveToLifecycle(LithoLifecycle lithoLifecycle) {
    mLithoLifecycleProviderDelegate.moveToLifecycle(lithoLifecycle);
  }

  @Override
  public void addListener(LithoLifecycleListener listener) {
    mLithoLifecycleProviderDelegate.addListener(listener);
  }

  @Override
  public void removeListener(LithoLifecycleListener listener) {
    mLithoLifecycleProviderDelegate.removeListener(listener);
  }

  @OnLifecycleEvent(ON_RESUME)
  private void onVisible() {
    moveToLifecycle(HINT_VISIBLE);
  }

  @OnLifecycleEvent(ON_PAUSE)
  private void onInvisible() {
    moveToLifecycle(HINT_INVISIBLE);
  }

  @OnLifecycleEvent(ON_DESTROY)
  private void onDestroy() {
    moveToLifecycle(DESTROYED);
  }
}
