/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.dataflow.ChoreographerCompat;
import com.facebook.litho.internal.ArraySet;

/**
 * An {@link AnimationBinding} that's composed of other {@link AnimationBinding}s running in
 * parallel, possibly starting on a stagger.
 */
public class ParallelBinding implements AnimationBinding {

  private final CopyOnWriteArrayList<AnimationBindingListener> mListeners =
      new CopyOnWriteArrayList<>();
  private final AnimationBinding[] mBindings;
  private final AnimationBindingListener mChildListener;
  private final HashSet<AnimationBinding> mBindingsFinished = new HashSet<>();
  private final ChoreographerCompat.FrameCallback mStaggerCallback;
  private final int mStaggerMs;
  private int mNextIndexToStart = 0;
  private int mChildrenFinished = 0;
  private boolean mHasStarted = false;
  private boolean mIsActive = false;
  private Resolver mResolver;

  public ParallelBinding(int staggerMs, AnimationBinding... bindings) {
    mStaggerMs = staggerMs;
    mBindings = bindings;

    if (mBindings.length == 0) {
      throw new IllegalArgumentException("Empty binding parallel");
    }

    mChildListener = new AnimationBindingListener() {
      @Override
      public void onStart(AnimationBinding binding) {
      }

      @Override
      public void onFinish(AnimationBinding binding) {
        ParallelBinding.this.onBindingFinished(binding);
      }
    };

    if (mStaggerMs == 0) {
      mStaggerCallback = null;
    } else {
      mStaggerCallback = new ChoreographerCompat.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
          if (!mIsActive) {
            return;
          }
          startNextBindingForStagger();
        }
      };
    }
  }

  private void onBindingFinished(AnimationBinding binding) {
    if (mBindingsFinished.contains(binding)) {
      throw new RuntimeException("Binding unexpectedly completed twice");
    }
    mBindingsFinished.add(binding);
    mChildrenFinished++;
    binding.removeListener(mChildListener);

    if (mChildrenFinished >= mBindings.length) {
      finish();
    }
  }

  private void finish() {
    mIsActive = false;
    for (AnimationBindingListener listener : mListeners) {
      listener.onFinish(this);
    }
  }

  @Override
  public void start(Resolver resolver) {
    if (mHasStarted) {
      throw new RuntimeException("Starting binding multiple times");
    }
    mHasStarted = true;
    mIsActive = true;
    mResolver = resolver;

    for (AnimationBindingListener listener : mListeners) {
      listener.onStart(this);
    }

    for (AnimationBinding binding : mBindings) {
      binding.addListener(mChildListener);
    }
    if (mStaggerMs == 0) {
      for (int i = 0; i < mBindings.length; i++) {
        final AnimationBinding binding = mBindings[i];
        binding.start(mResolver);
      }
      mNextIndexToStart = mBindings.length;
    } else {
      startNextBindingForStagger();
    }
  }

  private void startNextBindingForStagger() {
    mBindings[mNextIndexToStart].start(mResolver);
    mNextIndexToStart++;

    if (mNextIndexToStart < mBindings.length) {
      ChoreographerCompat.getInstance().postFrameCallbackDelayed(mStaggerCallback, mStaggerMs);
    }
  }

  @Override
  public void stop() {
    if (!mIsActive) {
      return;
    }
    mIsActive = false;
    mResolver = null;
    for (int i = 0; i < mBindings.length; i++) {
      final AnimationBinding childBinding = mBindings[i];
      if (childBinding.isActive()) {
        childBinding.stop();
      }
    }
  }

  @Override
  public boolean isActive() {
    return mIsActive;
  }

  @Override
  public void collectTransitioningProperties(ArraySet<ComponentProperty> outSet) {
    for (int i = 0; i < mBindings.length; i++) {
      mBindings[i].collectTransitioningProperties(outSet);
    }
  }

  @Override
  public void collectAppearFromValues(SimpleArrayMap<ComponentProperty, RuntimeValue> outMap) {
    for (int i = 0; i < mBindings.length; i++) {
      mBindings[i].collectAppearFromValues(outMap);
    }
  }

  @Override
  public void collectDisappearToValues(SimpleArrayMap<ComponentProperty, RuntimeValue> outMap) {
    for (int i = 0; i < mBindings.length; i++) {
      mBindings[i].collectDisappearToValues(outMap);
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
