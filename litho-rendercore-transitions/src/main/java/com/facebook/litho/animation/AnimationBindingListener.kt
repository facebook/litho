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

/** Listener that receives events when a [GraphBinding] is activated or is finished. */
interface AnimationBindingListener {
  /**
   * Called when going to be started later. For example: when a set of [AnimationBinding] starts,
   * but this particular binding will come into play later. This may be called multiple times in
   * case of several nested sets
   */
  fun onScheduledToStartLater(binding: AnimationBinding)

  /** Called when [GraphBinding.activate] is called on the relevant binding. */
  fun onWillStart(binding: AnimationBinding)

  /** Called when a [GraphBinding] is finished, meaning all of its nodes are finished. */
  fun onFinish(binding: AnimationBinding)

  /** Called when a listener (including this one) returns false from [shouldStart] */
  fun onCanceledBeforeStart(binding: AnimationBinding)

  /**
   * Return 'false' to cancel this animation and keep it from starting.
   *
   * @return shouldStart whether to start this animation (i.e. return false to cancel it and prevent
   *   it from running)
   */
  fun shouldStart(binding: AnimationBinding): Boolean
}
