// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.GraphBinding;

/**
 * A class used to get and set the runtime values of mount items in the component hierarchy during
 * animations. All methods take a {@link ComponentProperty} which encapsulates a transitionKey used
 * to reference the mount item and the {@link AnimatedProperty} on that mount item.
 */
public interface Resolver {

  /**
   * @return the current value of this property before the next mount state is applied.
   */
  float getCurrentState(ComponentProperty property);

  /**
   * @return the value of this property in it's final state after the next mount state is applied.
   */
  float getEndState(ComponentProperty property);

  /**
   * @return the {@link AnimatedPropertyNode} for this {@link ComponentProperty}. This gives
   * animations the ability to hook this mount item property into the {@link GraphBinding} they
   * create to drive their animation.
   */
  AnimatedPropertyNode getAnimatedPropertyNode(ComponentProperty property);
}
