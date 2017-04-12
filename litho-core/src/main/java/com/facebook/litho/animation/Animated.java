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
}
