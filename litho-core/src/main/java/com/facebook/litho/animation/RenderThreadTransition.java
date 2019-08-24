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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.Log;
import android.util.Property;
import android.view.View;
import com.facebook.litho.AnimationsDebug;
import com.facebook.litho.Transition;
import com.facebook.litho.dataflow.ConstantNode;
import com.facebook.litho.dataflow.InterpolatorNode;
import com.facebook.litho.dataflow.MappingNode;
import com.facebook.litho.dataflow.TimingNode;
import java.util.ArrayList;
import javax.annotation.Nullable;

/**
 * Animation for the transition of a single {@link PropertyAnimation} to be run on the render
 * thread, while at the same time shadow the animation on the UI thread in order to keep {@link
 * AnimatedPropertyNode} value in sync. If the delay is passed it will be considered as a part of
 * the animation, and will be applied by creating an adjusted interpolator (you may consider using
 * {@link Transition#delay(int, Transition)} ()} instead, but this way the delay will be handled on
 * the UI thread)
 */
public class RenderThreadTransition extends TransitionAnimationBinding {
  private static final String TAG = "RenderThreadTransition";

  private final int mDurationMs;
  private final PropertyAnimation mPropertyAnimation;
  private final @Nullable TimeInterpolator mInterpolator;
  private AnimatedPropertyNode mAnimatedPropertyNode;
  private @Nullable Animator mRunningAnimator;

  public RenderThreadTransition(
      PropertyAnimation propertyAnimation,
      int delayMs,
      int durationMs,
      TimeInterpolator interpolator) {
    if (delayMs < 0) {
      throw new IllegalArgumentException("Delay value should be non-negative, provided=" + delayMs);
    }
    if (durationMs <= 0) {
      throw new IllegalArgumentException(
          "Duration value should be positive, provided=" + durationMs);
    }
    if (interpolator == null) {
      throw new IllegalArgumentException("Interpolator should not be null");
    }

    if (delayMs > 0) {
      // Here we sum up delay and actual duration, and substitute the interpolator with the one that
      // handles delay internally

      mDurationMs = delayMs + durationMs;
      mInterpolator = new DelayInterpolator(interpolator, durationMs, delayMs);
    } else {
      mDurationMs = durationMs;
      mInterpolator = interpolator;
    }
    mPropertyAnimation = propertyAnimation;
  }

  @Override
  public void collectTransitioningProperties(ArrayList<PropertyAnimation> outList) {
    outList.add(mPropertyAnimation);
  }

  @Override
  protected void setupBinding(Resolver resolver) {
    final PropertyHandle propertyHandle = mPropertyAnimation.getPropertyHandle();

    mAnimatedPropertyNode = resolver.getAnimatedPropertyNode(propertyHandle);
    mAnimatedPropertyNode.setUsingRenderThread(true);

    final TimingNode timingNode = new TimingNode(mDurationMs);
    final ConstantNode initial = new ConstantNode(resolver.getCurrentState(propertyHandle));
    final ConstantNode end = new ConstantNode(mPropertyAnimation.getTargetValue());
    final MappingNode mappingNode = new MappingNode();
    final InterpolatorNode interpolatorNode = new InterpolatorNode(mInterpolator);

    addBinding(timingNode, interpolatorNode);
    addBinding(interpolatorNode, mappingNode);
    addBinding(initial, mappingNode, MappingNode.INITIAL_INPUT);
    addBinding(end, mappingNode, MappingNode.END_INPUT);
    addBinding(mappingNode, mAnimatedPropertyNode);
  }

  @Override
  public void start(Resolver resolver) {
    super.start(resolver);

    final View target =
        resolver
            .getAnimatedPropertyNode(mPropertyAnimation.getPropertyHandle())
            .getSingleTargetView();
    final float finalValue = mPropertyAnimation.getTargetValue();

    if (AnimationsDebug.ENABLED) {
      Log.d(TAG, "Trying to start, target=" + target + ", finalValue=" + finalValue);
    }

    if (target == null) {
      Log.e(
          TAG,
          "Couldn't resolve target for RT animation. Most possible reasons:\n"
              + "\t1) the components is not wrapped in view, please consider calling .wrapInView()\n"
              + "\t2) incremental mount is enabled and the view is out of screen at this moment");
      return;
    }

    mRunningAnimator = createAnimator(target, mPropertyAnimation.getProperty(), finalValue);
    mRunningAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            mRunningAnimator = null;
          }
        });
    mRunningAnimator.setInterpolator(mInterpolator);
    mRunningAnimator.setDuration(mDurationMs);
    mRunningAnimator.start();
  }

  @Override
  public void stop() {
    super.stop();

    if (mRunningAnimator != null) {
      mRunningAnimator.cancel();
      mRunningAnimator = null;
    }

    mAnimatedPropertyNode.setUsingRenderThread(false);
  }

  private static Animator createAnimator(
      View target, AnimatedProperty animatedProperty, float finalValue) {
    final Property viewAnimatorProperty = getViewAnimatorProperty(animatedProperty);
    return ObjectAnimator.ofFloat(target, viewAnimatorProperty, finalValue);
  }

  private static Property getViewAnimatorProperty(AnimatedProperty animatedProperty) {
    if (animatedProperty == AnimatedProperties.ALPHA) {
      return View.ALPHA;
    }
    if (animatedProperty == AnimatedProperties.X) {
      return View.X;
    }
    if (animatedProperty == AnimatedProperties.Y) {
      return View.Y;
    }
    if (animatedProperty == AnimatedProperties.ROTATION) {
      return View.ROTATION;
    }
    throw new IllegalArgumentException(
        "Cannot animate " + animatedProperty.getName() + " on RenderThread");
  }

  /** A wrapper that adds a provided delay to a provided interpolator */
  private static class DelayInterpolator implements TimeInterpolator {
    final TimeInterpolator mOriginal;
    final float mDelayFraction;

    DelayInterpolator(TimeInterpolator original, int duration, int delay) {
      mOriginal = original;
      mDelayFraction = (float) delay / (delay + duration);
    }

    @Override
    public float getInterpolation(float input) {
      if (input <= mDelayFraction) {
        return 0f;
      }

      final float adjustedInput = (input - mDelayFraction) / (1 - mDelayFraction);
      return mOriginal.getInterpolation(adjustedInput);
    }
  }
}
