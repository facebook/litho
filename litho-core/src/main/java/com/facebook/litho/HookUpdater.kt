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

package com.facebook.litho

/**
 * EXPERIMENTAL - fun interface for a block of code that will update state created with useState.
 * See KState.kt for more details.
 */
fun interface HookUpdater : StateUpdateApplier {

  /** Get a [KStateContainer] instance with updates applied. */
  fun getUpdatedStateContainer(currentState: KStateContainer): KStateContainer

  companion object {
    /**
     * A [HookUpdater] that does not modify the state container. Triggers a re-render pass without
     * changing any stored state values.
     *
     * Used by [AnimationController][animation.AnimationController] to signal that animated values
     * have changed and components reading them should re-render. The actual values live in
     * [AnimatedValue][animation.AnimatedValue] instances, not in [KStateContainer].
     */
    val NoOp: HookUpdater = HookUpdater { it }
  }
}
