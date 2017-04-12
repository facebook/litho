// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Base class to reference a property on a component.
 */
public abstract class ComponentProperty {

  private final AnimatedComponent mAnimatedComponent;
  private final AnimatedProperty mProperty;

  ComponentProperty(
      AnimatedComponent animatedComponent,
      AnimatedProperty property) {
    mAnimatedComponent = animatedComponent;
    mProperty = property;
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

  /**
   * @return the {@link AnimatedProperty} on the monut item that's being animated.
   */
  public AnimatedProperty getProperty() {
    return mProperty;
  }
}
