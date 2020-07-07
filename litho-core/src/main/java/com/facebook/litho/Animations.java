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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.Interpolator;
import androidx.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public class Animations {

  /** Create a new Flow to define how other DynamicProps may be derived from the input. */
  public static DynamicValueBindingBuilder bind(DynamicValue<Float> in) {
    return new DynamicValueBindingBuilder(in);
  }

  public static DynamicValueBindingBuilder bind(StateValue<DynamicValue<Float>> in) {
    DynamicValue<Float> dynamicValue = in.get();
    if (dynamicValue == null) {
      throw new IllegalArgumentException("The input must not be null.");
    }
    return bind(dynamicValue);
  }

  /** Create a new AnimationBuilder to manage the creation of a DynamicProps animation. */
  public static AnimationBuilder animate(DynamicValue<Float> in) {
    return new AnimationBuilder(in);
  }

  public static class DynamicValueBindingBuilder {

    private final DynamicValue<Float> mSource;
    private boolean hasInputRange = false;
    private float inputRangeStart = 0;
    private float inputRangeEnd = 1;
    private boolean hasOutputRange = false;
    private float outputRangeStart = 0;
    private float outputRangeEnd = 1;
    private @Nullable Interpolator mInterpolator;

    private DynamicValueBindingBuilder(DynamicValue<Float> source) {
      mSource = source;
    }

    public DynamicValueBindingBuilder inputRange(float start, float end) {
      inputRangeStart = start;
      inputRangeEnd = end;
      hasInputRange = true;
      return this;
    }

    public DynamicValueBindingBuilder outputRange(float start, float end) {
      outputRangeStart = start;
      outputRangeEnd = end;
      hasOutputRange = true;
      return this;
    }

    public DynamicValueBindingBuilder with(Interpolator interpolator) {
      mInterpolator = interpolator;
      return this;
    }

    /** Bind modified flow source to a dynamic value */
    public void to(StateValue<DynamicValue<Float>> dynamicValueState) {
      DynamicValue<Float> dynamicValue = create();
      dynamicValueState.set(dynamicValue);
    }

    /** Special case method to bind to a DynamicValue<Integer> to animate color */
    public void toInteger(StateValue<DynamicValue<Integer>> dynamicValueState) {
      DynamicValue<Integer> dynamicValue = createInteger();
      dynamicValueState.set(dynamicValue);
    }

    private float modify(float in) {
      float result = in;

      if (hasInputRange) {
        result = (result - inputRangeStart) / (inputRangeEnd - inputRangeStart);
        result = Math.min(result, 1);
        result = Math.max(result, 0);
      }

      if (mInterpolator != null) {
        result = mInterpolator.getInterpolation(result);
      }

      if (hasOutputRange) {
        final float range = outputRangeEnd - outputRangeStart;
        result = outputRangeStart + result * range;
      }

      return result;
    }

    public DynamicValue<Float> create() {
      DerivedDynamicValue.Modifier<Float, Float> modifier =
          new DerivedDynamicValue.Modifier<Float, Float>() {
            @Override
            public Float modify(Float in) {
              return DynamicValueBindingBuilder.this.modify(in);
            }
          };
      return new DerivedDynamicValue<>(mSource, modifier);
    }

    public DynamicValue<Integer> createInteger() {
      DerivedDynamicValue.Modifier<Float, Integer> modifier =
          new DerivedDynamicValue.Modifier<Float, Integer>() {
            @Override
            public Integer modify(Float in) {
              return (int) DynamicValueBindingBuilder.this.modify(in);
            }
          };

      return new DerivedDynamicValue<>(mSource, modifier);
    }
  }

  public static class AnimationBuilder {

    private final DynamicValue<Float> mValueToAnimate;
    private long mDuration = -1;
    private float mFrom;
    private float mTo = 0;
    private @Nullable Interpolator mInterpolator;

    private AnimationBuilder(DynamicValue<Float> valueToAnimate) {
      mValueToAnimate = valueToAnimate;
      mFrom = valueToAnimate.get();
    }

    /** Specify that value that the DynamicValue will be animated to. */
    public AnimationBuilder to(float to) {
      mTo = to;
      return this;
    }

    /** Specify that value that the DynamicValue will be animated from. */
    public AnimationBuilder from(float from) {
      mFrom = from;
      return this;
    }

    /** Specify that duration of the animation. */
    public AnimationBuilder duration(long duration) {
      mDuration = duration;
      return this;
    }

    /** Specify that duration of the animation. */
    public AnimationBuilder interpolator(Interpolator interpolator) {
      mInterpolator = interpolator;
      return this;
    }

    /** Start the animation and return a reference to the Animator */
    public Animator start() {
      final ValueAnimator animator = ValueAnimator.ofFloat(mFrom, mTo);
      if (mDuration > -1) {
        animator.setDuration(mDuration);
      }
      if (mInterpolator != null) {
        animator.setInterpolator(mInterpolator);
      }
      animator.addUpdateListener(
          new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
              float animatedValue = (Float) animation.getAnimatedValue();
              mValueToAnimate.set(animatedValue);
            }
          });

      animator.start();
      return animator;
    }

    /**
     * Stop the previous animation and start a new one.
     *
     * @param animatorRef A reference to the previous animation. This will be changed to the new
     *     animation.
     */
    public void startAndCancelPrevious(AtomicReference<Animator> animatorRef) {
      Animator oldAnimator = animatorRef.get();
      if (oldAnimator != null) {
        oldAnimator.cancel();
      }

      Animator newAnimator = start();
      animatorRef.set(newAnimator);
    }
  }
}
