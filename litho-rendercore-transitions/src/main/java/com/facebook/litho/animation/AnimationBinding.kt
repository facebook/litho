/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.animation

/**
 * An animation or set of animations using [com.facebook.litho.dataflow.GraphBinding]s. In [start],
 * subclasses can use the provided Resolver instance to reference concrete mount item properties
 * when creating the GraphBinding.
 */
interface AnimationBinding {

  /**
   * Called when binding is to be started later. For example: when a set of bindings starts, but
   * this particular binding will come into play later. This may be called multiple times in case of
   * several nested sets
   */
  fun prepareToStartLater()

  /**
   * Starts this animation. The provided [Resolver] instance can be used to configure this animation
   * appropriately using mount content property current and end values.
   */
  fun start(resolver: Resolver)

  /** Stops this animation. */
  fun stop()

  /** Returns whether this animation is running */
  fun isActive(): Boolean

  /**
   * Collects the set of [PropertyAnimation]s that this animation will animate. This is used to make
   * sure before/after values are recorded and accessible for the animation. Implementations should
   * add their animating properties to this set.
   *
   * Note: This is a 'collect' call instead of a getter to allocating more sets then necessary for
   * animations with nested animation (e.g. a sequence of animations). Yay Java.
   */
  fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>)

  /** Adds a [AnimationBindingListener]. */
  fun addListener(animationBindingListener: AnimationBindingListener)

  /** Removes a previously added [AnimationBindingListener]. */
  fun removeListener(animationBindingListener: AnimationBindingListener)

  /**
   * Allows adding a tags that can also be used to store data without resorting to another data
   * structure.
   */
  fun getTag(): Any?

  fun setTag(tag: Any?)
}
