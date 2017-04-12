// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A dimension property (x, y, width, height) on a component that is changing.
 */
public class DimensionComponentProperty extends ComponentProperty {

  public DimensionComponentProperty(
      AnimatedComponent AnimatedComponent,
      AnimatedProperty property) {
    super(AnimatedComponent, property);
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
