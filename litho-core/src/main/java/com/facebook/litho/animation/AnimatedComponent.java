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
   * @return the key used to identify this component in the mounted hierarchy.
   */
  public String getKey() {
    return mKey;
  }
}
