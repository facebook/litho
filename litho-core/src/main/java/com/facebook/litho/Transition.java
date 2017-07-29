/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.ArrayList;

import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.DimensionValue;
import com.facebook.litho.animation.FloatValue;
import com.facebook.litho.animation.PropertyAnimation;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.litho.animation.RuntimeValue;
import com.facebook.litho.animation.SpringTransition;
import com.facebook.litho.animation.TransitionAnimationBinding;

/**
 * Defines how a property on a component should animate as it changes, allowing you to optionally
 * define appear-from values for appear animations and disappear-to values for disappear animations.
 */
public class Transition {

  private static final TransitionAnimator DEFAULT_ANIMATOR = new SpringTransitionAnimator();

  /**
   * Class that knows how to create a {@link TransitionAnimationBinding} given a
   * {@link PropertyAnimation}. This can be used to customize the type of animation using
   * {@link Builder#animator}.
   */
  public interface TransitionAnimator {

    /**
     * @return a {@link TransitionAnimationBinding} for the given {@link PropertyAnimation} that
     * will animate the change in value on this property.
     */
    TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation);
  }

  /**
   * Creates a Transition for the given property on the component with the given key.
   */
  public static Transition.Builder create(String key) {
    return new Transition.Builder(key);
  }

  /**
   * Creates a set of {@link Transition}s.
   */
  public static TransitionSet createSet(Transition.Builder... transitions) {
    return new TransitionSet(transitions);
  }

  private final String mTransitionKey;
  private final AnimatedProperty mAnimatedProperty;
  private final TransitionAnimator mTransitionAnimator;
  private final RuntimeValue mAppearFrom;
  private final RuntimeValue mDisappearTo;

  public Transition(
      String transitionKey,
      AnimatedProperty animatedProperty,
      TransitionAnimator transitionAnimator,
      RuntimeValue appearFrom,
      RuntimeValue disappearTo) {
    mTransitionKey = transitionKey;
    mAnimatedProperty = animatedProperty;
    mTransitionAnimator = transitionAnimator;
    mAppearFrom = appearFrom;
    mDisappearTo = disappearTo;
  }

  boolean hasAppearAnimation() {
    return mAppearFrom != null;
  }

  boolean hasDisappearAnimation() {
    return mDisappearTo != null;
  }

  String getTransitionKey() {
    return mTransitionKey;
  }

  AnimatedProperty getAnimatedProperty() {
    return mAnimatedProperty;
  }

  RuntimeValue getAppearFrom() {
    return mAppearFrom;
  }

  RuntimeValue getDisappearTo() {
    return mDisappearTo;
  }

  AnimationBinding createAnimation(float targetValue) {
    final PropertyHandle propertyHandle = new PropertyHandle(mTransitionKey, mAnimatedProperty);
    final PropertyAnimation propertyAnimation = new PropertyAnimation(propertyHandle, targetValue);
    return mTransitionAnimator.createAnimation(propertyAnimation);
  }

  public static class Builder {

    private final String mKey;
    private final ArrayList<Transition> mBuiltTransitions = new ArrayList<>();

    private AnimatedProperty mAnimatedProperty;
    private TransitionAnimator mTransitionAnimator = DEFAULT_ANIMATOR;
    private RuntimeValue mAppearFrom;
    private RuntimeValue mDisappearTo;

    Builder(String key) {
      mKey = key;
    }

    public Builder animate(AnimatedProperty property) {
      maybeCommitCurrentBuilder();
      mAnimatedProperty = property;
      return this;
    }

    /**
     * Use to define the {@link TransitionAnimator} that drives the animation. The default is a
     * spring.
     */
    public Builder animator(TransitionAnimator animator) {
      mTransitionAnimator = animator;
      return this;
    }

    /**
     * Define where appear animations should start from.
     *
     * @see FloatValue
     * @see DimensionValue
     */
    public Builder appearFrom(RuntimeValue value) {
      mAppearFrom = value;
      return this;
    }

    /**
     * Define where disappear animations should end at.
     *
     * @see FloatValue
     * @see DimensionValue
     */
    public Builder disappearTo(RuntimeValue value) {
      mDisappearTo = value;
      return this;
    }

    /**
     * Define a constant value where appear animations should start from.
     */
    public Builder appearFrom(float value) {
      mAppearFrom = new FloatValue(value);
      return this;
    }

    /**
     * Define a constant value where disappear animations should end at.
     */
    public Builder disappearTo(float value) {
      mDisappearTo = new FloatValue(value);
      return this;
    }

    ArrayList<Transition> getTransitions() {
      maybeCommitCurrentBuilder();
      return mBuiltTransitions;
    }

    private void maybeCommitCurrentBuilder() {
      if (mAnimatedProperty == null) {
        return;
      }
      mBuiltTransitions.add(
          new Transition(
              mKey,
              mAnimatedProperty,
              mTransitionAnimator,
              mAppearFrom,
              mDisappearTo));
      mAnimatedProperty = null;
      mTransitionAnimator = DEFAULT_ANIMATOR;
      mAppearFrom = null;
      mDisappearTo = null;
    }
  }

  /**
   * Creates spring-driven animations.
   */
  public static class SpringTransitionAnimator implements TransitionAnimator {

    @Override
    public TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation) {
      // todo: re-enable. Commented out to split diffs more easily
//      return new SpringTransition(propertyAnimation);
      return null;
    }
  }
}
