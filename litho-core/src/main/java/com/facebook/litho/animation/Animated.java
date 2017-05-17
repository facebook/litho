/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.animation;

/**
 * Utility class for building animated transitions.
 */
public final class Animated {

  private Animated() {
  }

  /**
   * Creates an AnimationBinding which is a sequence of other animation bindings (i.e. each executes
   * after the previous one has finished).
   */
  public static AnimationBinding sequence(AnimationForVarArgs... bindings) {
    return new SequenceBinding(createAnimationBindingsFromMixed(bindings));
  }

  /**
   * Creates an AnimationBinding which is a collection of other animation bindings starting at the
   * same time and executing in parallel.
   */
  public static AnimationBinding parallel(AnimationForVarArgs... bindings) {
    return new ParallelBinding(0, createAnimationBindingsFromMixed(bindings));
  }

  /**
   * Creates an AnimationBinding which is a collection of other animation bindings starting on a
   * stagger and executing in parallel.
   */
  public static AnimationBinding stagger(int staggerMs, AnimationForVarArgs... bindings) {
    return new ParallelBinding(staggerMs, createAnimationBindingsFromMixed(bindings));
  }

  /**
   * Build a spring animation.
   */
  public static SpringBuilder spring() {
    return new SpringBuilder();
  }

  /**
   * Build a spring animation.
   */
  public static TimingBuilder timing() {
    return new TimingBuilder();
  }

  /**
   * Build a Bezier curve transition.
   */
  public static BezierBuilder bezier() {
    return new BezierBuilder();
  }

  private static AnimationBinding[] createAnimationBindingsFromMixed(
      AnimationForVarArgs[] bindings) {
    AnimationBinding[] animationBindings = new AnimationBinding[bindings.length];
    for (int i = 0; i < bindings.length; i++) {
      if (bindings[i] instanceof AnimationBinding) {
        animationBindings[i] = (AnimationBinding) bindings[i];
      } else if (bindings[i] instanceof AnimationBuilder) {
        animationBindings[i] = ((AnimationBuilder) bindings[i]).build();
      } else {
        throw new RuntimeException("Got unexpected object in animation var args: " + bindings[i]);
      }
    }
    return animationBindings;
  }

  /**
   * Class that can create an AnimationBinding.
   */
  public interface AnimationBuilder extends AnimationForVarArgs {
    AnimationBinding build();
  }

  public static abstract class AbstractBuilder {

    private ComponentProperty mProperty;

    public AppearingDimensionComponentProperty.TransitionBuilder animate(
        AppearingDimensionComponentProperty property) {
      mProperty = property;
      return new AppearingDimensionComponentProperty.TransitionBuilder(this);
    }

    public AppearingFloatComponentProperty.TransitionBuilder animate(
        AppearingFloatComponentProperty property) {
      mProperty = property;
      return new AppearingFloatComponentProperty.TransitionBuilder(this);
    }

    public FloatComponentProperty.TransitionBuilder animate(FloatComponentProperty property) {
      mProperty = property;
      return new FloatComponentProperty.TransitionBuilder(this);
    }

    public DimensionComponentProperty.TransitionBuilder animate(
        DimensionComponentProperty property) {
      mProperty = property;
      return new DimensionComponentProperty.TransitionBuilder(this);
    }

    public DisappearingDimensionComponentProperty.TransitionBuilder animate(
        DisappearingDimensionComponentProperty property) {
      mProperty = property;
      return new DisappearingDimensionComponentProperty.TransitionBuilder(this);
    }

    public DisappearingFloatComponentProperty.TransitionBuilder animate(
        DisappearingFloatComponentProperty property) {
      mProperty = property;
      return new DisappearingFloatComponentProperty.TransitionBuilder(this);
    }

    final TransitionAnimationBinding buildForAppear(RuntimeValue fromValue) {
      final TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addAppearFromValue(mProperty, fromValue);
      return transition;
    }

    final TransitionAnimationBinding buildForChange() {
      return buildTransition(mProperty);
    }

    final TransitionAnimationBinding buildForDisappear(RuntimeValue toValue) {
      final TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addDisappearToValue(mProperty, toValue);
      return transition;
    }

    abstract TransitionAnimationBinding buildTransition(ComponentProperty property);
  }

  public static abstract class AbstractPointBuilder {

    private PositionComponentProperty mProperty;

    public AppearingPositionComponentProperty.TransitionBuilder animate(
        AppearingPositionComponentProperty property) {
      mProperty = property;
      return new AppearingPositionComponentProperty.TransitionBuilder(this);
    }

    public PositionComponentProperty.TransitionBuilder animate(PositionComponentProperty property) {
      mProperty = property;
      return new PositionComponentProperty.TransitionBuilder(this);
    }

    public DisappearingPositionComponentProperty.TransitionBuilder animate(
        DisappearingPositionComponentProperty property) {
      mProperty = property;
      return new DisappearingPositionComponentProperty.TransitionBuilder(this);
    }

    final TransitionAnimationBinding buildForAppear(RuntimeValue fromX, RuntimeValue fromY) {
      final TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addAppearFromValue(mProperty.getXProperty(), fromX);
      transition.addAppearFromValue(mProperty.getYProperty(), fromY);
      return transition;
    }

    final TransitionAnimationBinding buildForChange() {
      return buildTransition(mProperty);
    }

    final TransitionAnimationBinding buildForDisappear(RuntimeValue toX, RuntimeValue toY) {
      final TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addDisappearToValue(mProperty.getXProperty(), toX);
      transition.addDisappearToValue(mProperty.getYProperty(), toY);
      return transition;
    }

    abstract TransitionAnimationBinding buildTransition(PositionComponentProperty property);
  }

  public static class SpringBuilder extends AbstractBuilder {

    @Override
    TransitionAnimationBinding buildTransition(ComponentProperty property) {
      return new SpringTransition(property);
    }
  }

  public static class TimingBuilder extends AbstractBuilder {

    private int mDurationMs = 300;

    public TimingBuilder durationMs(int durationMs) {
      mDurationMs = durationMs;
      return this;
    }

    @Override
    TransitionAnimationBinding buildTransition(ComponentProperty property) {
      return new TimingTransition(mDurationMs, property);
    }
  }

  public static class BezierBuilder extends AbstractPointBuilder {

    private float mControlPointX = 0;
    private float mControlPointY = 1;

    /**
     * Configures the control point of this quadratic bezier curve, determining the shape of the
     * curve. Because we don't know the start/end positions beforehand, the control point is defined
     * in terms of the distance between the start and end points of this animation. It is NOT in
     * terms of pixels or dp.
     *
     * Specifically, controlPointX=0 will give the control point the x position of initial position
     * of the curve. controlPointX=1 will give the control point the x position of the end of the
     * curve. controlPointX=.5 will give it the x position midway between the start and end x
     * positions. Increasing the value beyond 1 or below 0 will move the control point beyond the
     * end x position or before the start x position, respectively, while values between 0 and 1
     * will place the point in between the start and end x positions.
     *
     * All of the above also applies to the controlPointY value as well.
     *
     * For good looking curves, you want to make sure controlPointX != controlPointY (or else the
     * curve won't curve since it lies on the straight line between the start and end points).
     */
    public BezierBuilder controlPoint(float controlPointX, float controlPointY) {
      mControlPointX = controlPointX;
      mControlPointY = controlPointY;
      return this;
    }

    @Override
    TransitionAnimationBinding buildTransition(PositionComponentProperty property) {
      return new BezierTransition(
          property.getXProperty(),
          property.getYProperty(),
          mControlPointX,
          mControlPointY);
    }
  }
}
