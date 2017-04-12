// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A property (e.g. scale, opacity) on a component that's disappearing.
 */
public class DisappearingFloatComponentProperty extends FloatComponentProperty {

  public DisappearingFloatComponentProperty(
      DisappearingComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;
    private LazyFloatValue mToValue;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given absolute value.
     */
    public TransitionBuilder to(float value) {
      mToValue = new LazyFloatValue(value);
      return this;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToValue);
    }
  }
}
