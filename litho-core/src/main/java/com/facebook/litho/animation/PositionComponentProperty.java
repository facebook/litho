// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * A dimension-pair property (x, y) on a component that is changing.
 */
public class PositionComponentProperty extends CompositeComponentProperty {

  private final ComponentProperty mXProperty;
  private final ComponentProperty mYProperty;

  public PositionComponentProperty(
      AnimatedComponent animatedComponent,
      ComponentProperty xProperty,
      ComponentProperty yProperty) {
    super(animatedComponent);
    mXProperty = xProperty;
    mYProperty = yProperty;
  }

  /**
   * @return the {@link ComponentProperty} corresponding to the x property of this point.
   */
  public ComponentProperty getXProperty() {
    return mXProperty;
  }

  /**
   * @return the {@link ComponentProperty} corresponding to the y property of this point.
   */
  public ComponentProperty getYProperty() {
    return mYProperty;
  }

  public static class TransitionBuilder {

    private final Animated.AbstractPointBuilder mBuilderDelegate;

    public TransitionBuilder(Animated.AbstractPointBuilder builderDelegate) {
      mBuilderDelegate = builderDelegate;
    }

    public AnimationBinding build() {
      return mBuilderDelegate.buildForChange();
    }
  }
}
