/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.GraphBinding;

/**
 * Class used to get and set the runtime values of mount contents in the component hierarchy during
 * animations. All methods take a {@link PropertyHandle} which encapsulates a transitionKey used to
 * reference the mount content and the {@link AnimatedProperty} on that mount content.
 */
public interface Resolver {

  /** @return the current value of this property before the next mount state is applied. */
  float getCurrentState(PropertyHandle propertyHandle);

  /**
   * @return the {@link AnimatedPropertyNode} for this {@link PropertyHandle}. This gives animations
   *     the ability to hook this mount content property into the {@link GraphBinding} they create
   *     to drive their animation.
   */
  AnimatedPropertyNode getAnimatedPropertyNode(PropertyHandle propertyHandle);
}
