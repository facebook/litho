/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.GraphBinding;

/**
 * Class used to get and set the runtime values of mount contents in the component hierarchy during
 * animations. All methods take a {@link ComponentProperty} which encapsulates a transitionKey used
 * to reference the mount content and the {@link AnimatedProperty} on that mount content.
 */
public interface Resolver {

  /**
   * @return the current value of this property before the next mount state is applied.
   */
  float getCurrentState(PropertyHandle propertyHandle);

  /**
   * @return the {@link AnimatedPropertyNode} for this {@link ComponentProperty}. This gives
   * animations the ability to hook this mount content property into the {@link GraphBinding} they
   * create to drive their animation.
   */
  AnimatedPropertyNode getAnimatedPropertyNode(PropertyHandle propertyHandle);
}
