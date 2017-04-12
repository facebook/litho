// Copyright 2004-present Facebook. All Rights Reserved.

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
  public static AnimationBinding sequence(AnimationBinding... bindings) {
    return new SequenceBinding(bindings);
  }

  /**
   * Creates an AnimationBinding which is a collection of other animation bindings starting at the
   * same time and executing in parallel.
   */
  public static AnimationBinding parallel(AnimationBinding... bindings) {
    return new ParallelBinding(0, bindings);
  }

  /**
   * Creates an AnimationBinding which is a collection of other animation bindings starting on a
   * stagger and executing in parallel.
   */
  public static AnimationBinding stagger(int staggerMs, AnimationBinding... bindings) {
    return new ParallelBinding(staggerMs, bindings);
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

    final TransitionAnimationBinding buildForAppear(LazyValue fromValue) {
      TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addAppearFromValue(mProperty, fromValue);
      return transition;
    }

    final TransitionAnimationBinding buildForChange() {
      return buildTransition(mProperty);
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

    final TransitionAnimationBinding buildForAppear(LazyValue fromX, LazyValue fromY) {
      TransitionAnimationBinding transition = buildTransition(mProperty);
      transition.addAppearFromValue(mProperty.getXProperty(), fromX);
      transition.addAppearFromValue(mProperty.getYProperty(), fromY);
      return transition;
    }

    final TransitionAnimationBinding buildForChange() {
      return buildTransition(mProperty);
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
