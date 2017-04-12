// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A property (e.g. scale, opacity) on a component that's changing.
 */
public class FloatComponentProperty extends ComponentProperty {

  public FloatComponentProperty(
      AnimatedComponent animatedComponent,
      AnimatedProperty property) {
    super(animatedComponent, property);
  }

  public static class TransitionBuilder {

    private final Animated.AbstractBuilder mBuilderDelegate;

    public TransitionBuilder(Animated.AbstractBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForChange();
    }
  }
}
