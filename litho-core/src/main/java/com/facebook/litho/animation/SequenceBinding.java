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

import java.util.ArrayList;
import java.util.List;

/** An {@link AnimationBinding} that's a sequence of other {@link AnimationBinding}s. */
public class SequenceBinding extends BaseAnimationBinding {

  private final List<AnimationBinding> mBindings;
  private final AnimationBindingListener mChildListener;
  private Resolver mResolver;
  private int mCurrentIndex = 0;
  private boolean mIsActive = false;

  public SequenceBinding(List<AnimationBinding> bindings) {
    mBindings = bindings;

    if (mBindings.isEmpty()) {
      throw new IllegalArgumentException("Empty binding sequence");
    }

    mChildListener =
        new AnimationBindingListener() {
          @Override
          public void onScheduledToStartLater(AnimationBinding binding) {}

          @Override
          public void onWillStart(AnimationBinding binding) {}

          @Override
          public void onFinish(AnimationBinding binding) {
            SequenceBinding.this.onBindingFinished(binding);
          }

          @Override
          public void onCanceledBeforeStart(AnimationBinding binding) {
            SequenceBinding.this.onBindingFinished(binding);
          }

          @Override
          public boolean shouldStart(AnimationBinding binding) {
            return true;
          }
        };
  }

  private void onBindingFinished(AnimationBinding binding) {
    if (binding != mBindings.get(mCurrentIndex)) {
      throw new RuntimeException("Unexpected Binding completed");
    }
    binding.removeListener(mChildListener);
    mCurrentIndex++;

    if (mCurrentIndex >= mBindings.size()) {
      finish();
    } else {
      AnimationBinding next = mBindings.get(mCurrentIndex);
      next.addListener(mChildListener);
      next.start(mResolver);
    }
  }

  private void finish() {
    notifyFinished();
    mIsActive = false;
    mResolver = null;
  }

  @Override
  public void start(Resolver resolver) {
    if (mIsActive) {
      throw new RuntimeException("Already started");
    }

    if (!shouldStart()) {
      notifyCanceledBeforeStart();
      return;
    }
    notifyWillStart();
    // Notify all children except the first one that will start later
    for (int i = 1, size = mBindings.size(); i < size; i++) {
      final AnimationBinding binding = mBindings.get(i);
      binding.prepareToStartLater();
    }
    // Now start the first one
    mIsActive = true;
    mResolver = resolver;
    final AnimationBinding first = mBindings.get(0);
    first.addListener(mChildListener);
    first.start(mResolver);
  }

  @Override
  public void stop() {
    if (!mIsActive) {
      return;
    }
    mIsActive = false;
    mBindings.get(mCurrentIndex).stop();
  }

  @Override
  public boolean isActive() {
    return mIsActive;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    for (int i = 0, size = mBindings.size(); i < size; i++) {
      mBindings.get(i).collectTransitioningProperties(outList);
    }
  }

  @Override
  public void prepareToStartLater() {
    notifyScheduledToStartLater();
    for (int i = 0, size = mBindings.size(); i < size; i++) {
      final AnimationBinding binding = mBindings.get(i);
      binding.prepareToStartLater();
    }
  }
}
