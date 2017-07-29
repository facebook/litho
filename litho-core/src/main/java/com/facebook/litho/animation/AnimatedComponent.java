// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Base class to reference a component that can be animated.
 */
public abstract class AnimatedComponent {

  private final String mKey;

  /**
   * @param key the transitionKey for this component. This key is used to look up this component
   * in the mounted hierarchy just before the animation.
   */
  public AnimatedComponent(String key) {
    mKey = key;
  }

  /**
   * @return a handle to the 'x' property of this component.
   */
  public abstract ComponentProperty x();

  /**
   * @return a handle to the 'y' property of this component.
   */
  public abstract ComponentProperty y();

  /**
   * @return a handle to the width property of this component.
   */
  public abstract ComponentProperty width();

  /**
   * @return a handle to the height property of this component.
   */
  public abstract ComponentProperty height();

  /**
   * @return a handle to the composite 'xy' position property of this component.
   */
  public abstract ComponentProperty xy();

  /**
   * @return a handle to the scale property of this component.
   */
  public abstract ComponentProperty scale();

  /**
   * @return a handle to the alpha property of this component.
   */
  public abstract ComponentProperty alpha();

  /**
   * @return the key used to identify this component in the mounted hierarchy.
   */
  public String getKey() {
    return mKey;
  }
}
