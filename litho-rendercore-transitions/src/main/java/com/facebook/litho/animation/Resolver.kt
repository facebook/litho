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

import com.facebook.litho.dataflow.GraphBinding

/**
 * Class used to get and set the runtime values of mount contents in the component hierarchy during
 * animations. All methods take a [PropertyHandle] which encapsulates a transitionKey used to
 * reference the mount content and the [AnimatedProperty] on that mount content.
 */
interface Resolver {

  /** Returns the current value of this property before the next mount state is applied. */
  fun getCurrentState(propertyHandle: PropertyHandle): Float

  /**
   * Returns the [AnimatedPropertyNode] for this [PropertyHandle]. This gives animations the ability
   * to hook this mount content property into the [GraphBinding] they create to drive their
   * animation.
   */
  fun getAnimatedPropertyNode(propertyHandle: PropertyHandle): AnimatedPropertyNode
}
