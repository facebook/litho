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

package com.facebook.litho;

import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.annotations.Event;

/**
 * Components should implement an event of this type in order to receive a callback when a {@link Transition} ends.
 * An example of the correct usage is:
 *
 * <pre>
 * {@code
 *
 * @OnEvent(TransitionEndEvent.class)
 * static void onTransitionEnd(
 *     ComponentContext c,
 *     @FromEvent String transitionKey,
 *     @FromEvent AnimatedProperty animatedProperty,
 *     @Param Param someParam,
 *     @Prop Prop someProp) {
 *   // Handle the event here.
 * }
 * </pre>
 */
@Event
public class TransitionEndEvent {
  public String transitionKey;
  public AnimatedProperty animatedProperty;

  public TransitionEndEvent(String transitionKey, AnimatedProperty animatedProperty) {
    this.transitionKey = transitionKey;
    this.animatedProperty = animatedProperty;
  }
}
