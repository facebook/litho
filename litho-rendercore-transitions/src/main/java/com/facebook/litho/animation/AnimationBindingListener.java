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

import com.facebook.litho.dataflow.GraphBinding;

/** Listener that receives events when a {@link GraphBinding} is activated or is finished. */
public interface AnimationBindingListener {
  /**
   * Called when going to be started later. For example: when a set of {@link AnimationBinding}
   * starts, but this particular binding will come into play later. This may be called multiple
   * times in case of several nested sets
   */
  void onScheduledToStartLater(AnimationBinding binding);

  /** Called when {@link GraphBinding#activate} is called on the relevant binding. */
  void onWillStart(AnimationBinding binding);

  /** Called when a {@link GraphBinding} is finished, meaning all of its nodes are finished. */
  void onFinish(AnimationBinding binding);

  /** Called when a listener (including this one) returns false from {@link #shouldStart} */
  void onCanceledBeforeStart(AnimationBinding binding);

  /**
   * Return 'false' to cancel this animation and keep it from starting.
   *
   * @return shouldStart whether to start this animation (i.e. return false to cancel it and prevent
   *     it from running)
   */
  boolean shouldStart(AnimationBinding binding);
}
