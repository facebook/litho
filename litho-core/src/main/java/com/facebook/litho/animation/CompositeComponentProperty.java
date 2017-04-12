// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Base class for a property that is actually backed by multiple float properties (e.g. xy-position
 * or color).
 */
public abstract class CompositeComponentProperty {

  private final AnimatedComponent mAnimatedComponent;

  CompositeComponentProperty(
      AnimatedComponent animatedComponent) {
    mAnimatedComponent = animatedComponent;
  }

  /**
   * @return the {@link AnimatedComponent} that this property belongs to.
   */
  public AnimatedComponent getAnimatedComponent() {
    return mAnimatedComponent;
  }

  /**
   * @return the transition key of the animating component.
   */
  public String getTransitionKey() {
    return mAnimatedComponent.getKey();
  }
}
