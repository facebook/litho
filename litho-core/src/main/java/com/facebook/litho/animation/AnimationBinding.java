/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.internal.ArraySet;

/**
 * An animation or set of animations using {@link com.facebook.litho.dataflow.GraphBinding}s.
 * In {@link #start}, subclasses can use the provided Resolver instance to reference concrete mount
 * item properties when creating the GraphBinding.
 */
public interface AnimationBinding {

  /**
   * Starts this animation. The provided {@link Resolver} instance can be used to configure this
   * animation appropriately using mount item property current and end values.
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
   * Collects the set of {@link ComponentProperty}s that this animation will animate. This is used
   * to make sure before/after values are recorded and accessible for the animation. Implementations
   * should add their animating properties to this set.
   *
   * Note: This is a 'collect' call instead of a getter to allocating more sets then necessary for
   * animations with nested animation (e.g. a sequence of animations). Yay Java.
   */
   void collectTransitioningProperties(ArraySet<ComponentProperty> outSet);

  /**
   * Collects a mapping from {@link ComponentProperty} to its initial value. This is used to set up
   * the initial property values for appear animations.
   *
   * Note: This is a 'collect' call instead of a getter to allocating more maps then necessary for
   * animations with nested animation (e.g. a sequence of animations). Yay Java.
   */
   void collectAppearFromValues(SimpleArrayMap<ComponentProperty, LazyValue> outMap);

  /**
   * Collects a mapping from {@link ComponentProperty} to its end value. This is used to set up
   * the end property values for disappear animations.
   *
   * Note: This is a 'collect' call instead of a getter to allocating more maps then necessary for
   * animations with nested animation (e.g. a sequence of animations).
   */
  void collectDisappearToValues(SimpleArrayMap<ComponentProperty, LazyValue> outMap);

  /**
   * Adds a {@link AnimationBindingListener}.
   */
  void addListener(AnimationBindingListener animationBindingListener);

  /**
   * Removes a previously added {@link AnimationBindingListener}.
   */
  void removeListener(AnimationBindingListener animationBindingListener);
}
