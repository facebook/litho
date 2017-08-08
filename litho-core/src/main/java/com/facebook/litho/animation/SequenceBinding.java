/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An {@link AnimationBinding} that's a sequence of other {@link AnimationBinding}s.
 */
public class SequenceBinding implements AnimationBinding {

  private final CopyOnWriteArrayList<AnimationBindingListener> mListeners =
      new CopyOnWriteArrayList<>();
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

    mChildListener = new AnimationBindingListener() {
      @Override
      public void onWillStart(AnimationBinding binding) {
      }

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
    for (AnimationBindingListener listener : mListeners) {
      listener.onFinish(this);
    }
    mIsActive = false;
    mResolver = null;
  }

  private void notifyCanceledBeforeStart() {
    for (AnimationBindingListener listener : mListeners) {
      listener.onCanceledBeforeStart(this);
    }
  }

  @Override
  public void start(Resolver resolver) {
    if (mIsActive) {
      throw new RuntimeException("Already started");
    }
    for (AnimationBindingListener listener : mListeners) {
      if (!listener.shouldStart(this)) {
        notifyCanceledBeforeStart();
        return;
      }
    }
    for (AnimationBindingListener listener : mListeners) {
      listener.onWillStart(this);
    }
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
  public void addListener(AnimationBindingListener animationBindingListener) {
    mListeners.add(animationBindingListener);
  }

  @Override
  public void removeListener(AnimationBindingListener animationBindingListener) {
    mListeners.remove(animationBindingListener);
  }
}
