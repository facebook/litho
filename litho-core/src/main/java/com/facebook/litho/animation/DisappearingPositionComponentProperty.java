// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A dimension-pair property (x, y) on a component that is disappearing.
 */
public class DisappearingPositionComponentProperty extends PositionComponentProperty {

  public DisappearingPositionComponentProperty(
      DisappearingComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent, xProperty, yProperty);
  }

  public static class TransitionBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;
    private LazyValue mToX;
    private LazyValue mToY;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    /**
     * Transition this property on a disappearing component to the given values.
     */
    public TransitionBuilder from(LazyValue toX, LazyValue toY) {
      mToX = toX;
      mToY = toY;
      return this;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForDisappear(mToX, mToY);
    }
  }
}
