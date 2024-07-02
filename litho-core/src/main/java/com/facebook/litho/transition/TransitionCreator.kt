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

package com.facebook.litho.transition

import com.facebook.litho.Component
import com.facebook.litho.Diff
import com.facebook.litho.Transition
import com.facebook.litho.internal.HookKey

/**
 * Encapsulates the logic needed for creating [Transition]s whose definition depends on the [Diff]
 * of data from a previous render.
 */
internal interface TransitionCreator {

  /**
   * A [HookKey] that uniquely identifies this definition and may be used to efficiently store and
   * retrieve the [Component.RenderData] of this definition.
   *
   * @see [recordRenderData]
   */
  val identityKey: HookKey

  /**
   * Whether this definition supports optimistic transitions.
   *
   * Definitions that support optimistic transitions may completely avoid doing work at mount time,
   * provided the evaluation conditions remain the same. This is possible since their transitions
   * can be optimistically created on a background thread during the resolve phase
   */
  val supportsOptimisticTransitions: Boolean

  /**
   * Creates a [Transition] based on the [Diff] of data from a previous render.
   *
   * @param previous the [Component.RenderData] from the previous render.
   */
  fun createTransition(previous: Component.RenderData?): Transition?

  /**
   * Captures the [Component.RenderData] for the current render which will be passed to
   * [createTransition] at the next render.
   *
   * @return the [Component.RenderData] for the current render.
   */
  fun recordRenderData(): Component.RenderData
}
