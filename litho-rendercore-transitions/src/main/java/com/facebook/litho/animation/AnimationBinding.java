/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation;

import androidx.annotation.Nullable;
import java.util.ArrayList;

/**
 * An animation or set of animations using {@link com.facebook.litho.dataflow.GraphBinding}s. In
 * {@link #start}, subclasses can use the provided Resolver instance to reference concrete mount
 * item properties when creating the GraphBinding.
 */
public interface AnimationBinding {

  /**
   * Called when binding is to be started later. For example: when a set of bindings starts, but
   * this particular binding will come into play later. This may be called multiple times in case of
   * several nested sets
   */
  void prepareToStartLater();

  /**
   * Starts this animation. The provided {@link Resolver} instance can be used to configure this
   * animation appropriately using mount content property current and end values.
   */
  void start(Resolver resolver);

  /** Stops this animation. */
  void stop();

  /** @return whether this animation is running */
  boolean isActive();

  /**
   * Collects the set of {@link PropertyAnimation}s that this animation will animate. This is used
   * to make sure before/after values are recorded and accessible for the animation. Implementations
   * should add their animating properties to this set.
   *
   * <p>Note: This is a 'collect' call instead of a getter to allocating more sets then necessary
   * for animations with nested animation (e.g. a sequence of animations). Yay Java.
   */
  void collectTransitioningProperties(ArrayList<PropertyAnimation> outList);

  /** Adds a {@link AnimationBindingListener}. */
  void addListener(AnimationBindingListener animationBindingListener);

  /** Removes a previously added {@link AnimationBindingListener}. */
  void removeListener(AnimationBindingListener animationBindingListener);

  /**
   * Allows adding a tags that can also be used to store data without resorting to another data
   * structure. *
   */
  @Nullable
  Object getTag();

  void setTag(@Nullable Object tag);
}
