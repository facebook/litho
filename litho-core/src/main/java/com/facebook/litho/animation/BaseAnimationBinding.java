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

package com.facebook.litho.animation;

import androidx.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A base implementation of {@link AnimationBinding} interface, that takes care of adding/removing
 * listeners and provides helper methods for dealing with them
 */
abstract class BaseAnimationBinding implements AnimationBinding {
  private CopyOnWriteArrayList<AnimationBindingListener> mListeners = new CopyOnWriteArrayList<>();
  @Nullable private Object mTag;

  @Override
  public final void addListener(AnimationBindingListener animationBindingListener) {
    mListeners.add(animationBindingListener);
  }

  @Override
  public final void removeListener(AnimationBindingListener animationBindingListener) {
    mListeners.remove(animationBindingListener);
  }

  @Nullable
  @Override
  public Object getTag() {
    return mTag;
  }

  @Override
  public void setTag(@Nullable Object tag) {
    mTag = tag;
  }

  /**
   * Checks with all added {@link AnimationBindingListener} if should start
   *
   * @return true if *all* listeners return true from {@link
   *     AnimationBindingListener#shouldStart(AnimationBinding)}, false - otherwise
   */
  final boolean shouldStart() {
    for (int index = mListeners.size() - 1; index >= 0; index--) {
      final AnimationBindingListener listener = mListeners.get(index);
      if (!listener.shouldStart(this)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Notifies all the added {@link AnimationBindingListener}s that has been canceled before start.
   */
  final void notifyCanceledBeforeStart() {
    for (int index = mListeners.size() - 1; index >= 0; index--) {
      final AnimationBindingListener listener = mListeners.get(index);
      listener.onCanceledBeforeStart(this);
    }
  }

  /** Notifies all the added {@link AnimationBindingListener}s that will start. */
  final void notifyWillStart() {
    for (int index = mListeners.size() - 1; index >= 0; index--) {
      final AnimationBindingListener listener = mListeners.get(index);
      listener.onWillStart(this);
    }
  }

  /** Notifies all the added {@link AnimationBindingListener}s that has finished. */
  final void notifyFinished() {
    for (int index = mListeners.size() - 1; index >= 0; index--) {
      final AnimationBindingListener listener = mListeners.get(index);
      listener.onFinish(this);
    }
  }

  /**
   * Notifies the added {@link AnimationBindingListener}s that has been scheduled to start later.
   */
  final void notifyScheduledToStartLater() {
    for (int index = mListeners.size() - 1; index >= 0; index--) {
      final AnimationBindingListener listener = mListeners.get(index);
      listener.onScheduledToStartLater(this);
    }
  }
}
