/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.animation;

import com.facebook.litho.choreographercompat.ChoreographerCompat;
import com.facebook.litho.choreographercompat.ChoreographerCompatImpl;
import java.util.ArrayList;

/** An {@link AnimationBinding} that adds a delay to the provided {@link AnimationBinding} */
public class DelayBinding extends BaseAnimationBinding {

  private final AnimationBinding mBinding;
  private final int mDelayMs;
  private boolean mHasStarted = false;
  private boolean mIsActive = false;
  private Resolver mResolver;

  public DelayBinding(int mDelayMs, AnimationBinding mBinding) {
    this.mDelayMs = mDelayMs;
    this.mBinding = mBinding;
  }

  @Override
  public void prepareToStartLater() {
    notifyScheduledToStartLater();
    mBinding.prepareToStartLater();
  }

  @Override
  public void start(Resolver resolver) {
    if (mHasStarted) {
      throw new RuntimeException("Starting binding multiple times");
    }
    mHasStarted = true;
    mResolver = resolver;

    if (!shouldStart()) {
      notifyCanceledBeforeStart();
      return;
    }
    notifyWillStart();

    mIsActive = true;

    mBinding.prepareToStartLater();

    final AnimationBindingListener bindingListener =
        new AnimationBindingListener() {
          @Override
          public void onScheduledToStartLater(AnimationBinding binding) {}

          @Override
          public void onWillStart(AnimationBinding binding) {}

          @Override
          public void onFinish(AnimationBinding binding) {
            binding.removeListener(this);
            DelayBinding.this.finish();
          }

          @Override
          public void onCanceledBeforeStart(AnimationBinding binding) {
            onFinish(binding);
          }

          @Override
          public boolean shouldStart(AnimationBinding binding) {
            return true;
          }
        };
    mBinding.addListener(bindingListener);

    final ChoreographerCompat.FrameCallback delayedStartCallback =
        new ChoreographerCompat.FrameCallback() {
          @Override
          public void doFrame(long frameTimeNanos) {
            if (!mIsActive) {
              return;
            }
            mBinding.start(mResolver);
          }
        };
    ChoreographerCompatImpl.getInstance().postFrameCallbackDelayed(delayedStartCallback, mDelayMs);
  }

  @Override
  public void stop() {
    if (!mIsActive) {
      return;
    }
    mIsActive = false;
    mResolver = null;
    if (mBinding.isActive()) {
      mBinding.stop();
    }
  }

  @Override
  public boolean isActive() {
    return mIsActive;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    mBinding.collectTransitioningProperties(outList);
  }

  private void finish() {
    mIsActive = false;
    notifyFinished();
  }
}
