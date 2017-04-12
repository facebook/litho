// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A property (e.g. scale, opacity) on a component that's appearing.
 */
public class AppearingFloatComponentProperty extends FloatComponentProperty {

  public AppearingFloatComponentProperty(
      AppearingComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private LazyFloatValue mFromValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on an appearing component from the given absolute value.
     */
    public TransitionBuilder from(float value) {
      mFromValue = new LazyFloatValue(value);
      return this;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForAppear(mFromValue);
    }
  }
}
