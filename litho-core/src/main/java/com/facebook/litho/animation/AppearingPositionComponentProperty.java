// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A dimension-pair property (x, y) on a component that is appearing.
 */
public class AppearingPositionComponentProperty extends PositionComponentProperty {

  public AppearingPositionComponentProperty(
      AnimatedComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent, xProperty, yProperty);
  }

  public static class TransitionBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;
    private LazyValue mFromX;
    private LazyValue mFromY;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on an appearing component from the given values.
     */
    public TransitionBuilder from(LazyValue fromX, LazyValue fromY) {
      mFromX = fromX;
      mFromY = fromY;
      return this;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForAppear(mFromX, mFromY);
    }
  }
}
