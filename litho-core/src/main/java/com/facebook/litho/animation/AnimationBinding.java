/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import java.util.ArrayList;

/**
 * An animation or set of animations using {@link com.facebook.litho.dataflow.GraphBinding}s.
 * In {@link #start}, subclasses can use the provided Resolver instance to reference concrete mount
 * item properties when creating the GraphBinding.
 */
public interface AnimationBinding {

  /**
   * Starts this animation. The provided {@link Resolver} instance can be used to configure this
   * animation appropriately using mount content property current and end values.
   */
  void start(Resolver resolver);

  /**
   * Stops this animation.
   */
  void stop();

  /**
   * @return whether this animation is running
   */
  boolean isActive();

  /**
   * Collects the set of {@link PropertyAnimation}s that this animation will animate. This is used
   * to make sure before/after values are recorded and accessible for the animation. Implementations
   * should add their animating properties to this set.
   *
   * Note: This is a 'collect' call instead of a getter to allocating more sets then necessary for
   * animations with nested animation (e.g. a sequence of animations). Yay Java.
   */
   void collectTransitioningProperties(ArrayList<PropertyAnimation> outList);

  /**
   * Adds a {@link AnimationBindingListener}.
   */
  void addListener(AnimationBindingListener animationBindingListener);

  /**
   * Removes a previously added {@link AnimationBindingListener}.
   */
  void removeListener(AnimationBindingListener animationBindingListener);
}
