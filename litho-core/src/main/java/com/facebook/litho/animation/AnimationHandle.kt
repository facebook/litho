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

import android.os.Handler
import android.os.Looper

/**
 * A handle that controls when an animation begins receiving frame callbacks.
 *
 * Created by the animation hooks ([useAnimatedValue], [useAnimationGroup]) and passed to
 * [AnimationTrigger.onReady]. The [AnimationTrigger] strategy decides when to call [start] — either
 * immediately ([AnimationTrigger.Immediate]) or at a later time ([AnimationTrigger.Lazy]).
 *
 * The [onStart] action is generic: it is not tied to a single [AnimatedValue], so it works for both
 * single-value hooks and multi-value transitions.
 */
class AnimationHandle
internal constructor(
    private val onStart: () -> Unit,
) {

  private var started = false

  /** Starts the animation. Safe to call from any thread. */
  fun start() {
    if (started) return
    started = true
    if (Looper.myLooper() == Looper.getMainLooper()) {
      onStart()
    } else {
      Handler(Looper.getMainLooper()).post(onStart)
    }
  }

  /** Makes [start] a no-op. Called by the framework on cleanup. */
  internal fun invalidate() {
    started = true
  }
}
