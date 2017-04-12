// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Class that represents a float value that may need to be resolved at runtime (since it's relative
 * to a property on a mount item for example).
 */
public interface LazyValue {

  /**
   * Uses the given {@link Resolver} to determine the runtime value based on the given
   * {@link ComponentProperty}.
   */
  float resolve(Resolver resolver, ComponentProperty componentProperty);
}
