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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An {@link AnimationBinding} that's a sequence of other {@link AnimationBinding}s.
 */
public class SequenceBinding implements AnimationBinding {

  private final CopyOnWriteArrayList<AnimationBindingListener> mListeners =
      new CopyOnWriteArrayList<>();
  private final AnimationBinding[] mBindings;
  private final AnimationBindingListener mChildListener;
  private Resolver mResolver;
  private int mCurrentIndex = 0;
  private boolean mIsActive = false;

  public SequenceBinding(AnimationBinding... bindings) {
    mBindings = bindings;

    if (mBindings.length == 0) {
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
    };
  }

  private void onBindingFinished(AnimationBinding binding) {
    if (binding != mBindings[mCurrentIndex]) {
      throw new RuntimeException("Unexpected Binding completed");
    }
    binding.removeListener(mChildListener);
    mCurrentIndex++;

    if (mCurrentIndex >= mBindings.length) {
      finish();
    } else {
      AnimationBinding next = mBindings[mCurrentIndex];
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

  @Override
  public void start(Resolver resolver) {
    if (mIsActive) {
      throw new RuntimeException("Already started");
    }
    mIsActive = true;
    for (AnimationBindingListener listener : mListeners) {
      listener.onWillStart(this);
    }
    mResolver = resolver;
    mBindings[0].addListener(mChildListener);
    mBindings[0].start(mResolver);
  }

  @Override
  public void stop() {
    if (!mIsActive) {
      return;
    }
    mIsActive = false;
    mBindings[mCurrentIndex].stop();
  }

  @Override
  public boolean isActive() {
    return mIsActive;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    for (int i = 0; i < mBindings.length; i++) {
      mBindings[i].collectTransitioningProperties(outList);
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
