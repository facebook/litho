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

import com.facebook.litho.CleanupFunc
import com.facebook.litho.ComponentScope
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.onCleanup
import com.facebook.litho.useCached

/**
 * Strategy interface that controls *when* an animation begins receiving frame callbacks.
 *
 * Inspired by `CoroutineStart` / `SharingStarted`, this lets callers decouple animation setup from
 * animation start. In collections with hundreds of items, animations can run to completion while
 * items are still off-screen. [AnimationTrigger] solves this without per-item state updates.
 *
 * @see AnimationTrigger.Immediate
 * @see AnimationTrigger.Lazy
 */
sealed interface AnimationTrigger {

  /**
   * Called when the animation is set up and ready to begin. Call [handle.start] to begin frame
   * callbacks. Return a [CleanupFunc] to clean up resources when the animation is cancelled or the
   * component unmounts, or null if not needed.
   */
  fun onReady(handle: AnimationHandle): CleanupFunc?

  /** Starts the animation immediately. This is the default. */
  object Immediate : AnimationTrigger {
    override fun onReady(handle: AnimationHandle): CleanupFunc? {
      handle.start()
      return null
    }
  }

  /**
   * An [AnimationTrigger] that defers animation start until [start] is called explicitly.
   *
   * Useful for items in a scrollable collection where animations should only play once the item
   * becomes visible on screen. Prefer [useLazyAnimationTrigger] to create a stable instance across
   * renders.
   *
   * [start] latches. Each retarget hands [onReady] a *new* [AnimationHandle] — the handle is a
   * one-shot command, so all durable state lives here on the trigger — and once started, this
   * trigger starts each new handle on arrival. Without that latch a group whose children settle at
   * different times would tear: a retarget re-gates only the settled children, while the siblings
   * already receiving frames keep animating toward the new target.
   */
  class Lazy : AnimationTrigger {

    private var handle: AnimationHandle? = null
    private var started: Boolean = false

    override fun onReady(handle: AnimationHandle): CleanupFunc? {
      this.handle = handle
      if (started) {
        handle.start()
      }
      return onCleanup { this.handle = null }
    }

    /**
     * Starts the deferred animation and keeps later retargets running without further calls. Safe
     * to call multiple times — only the first call has effect.
     */
    fun start() {
      started = true
      handle?.start()
    }
  }
}

/** Returns a stable [AnimationTrigger.Lazy] instance for explicit manual triggering. */
@ExperimentalLithoApi
@Hook
fun ComponentScope.useLazyAnimationTrigger(): AnimationTrigger.Lazy = useCached {
  AnimationTrigger.Lazy()
}
