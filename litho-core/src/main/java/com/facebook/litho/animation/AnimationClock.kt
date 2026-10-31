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

/** Owner-local play time shared by all values driven by this registration. */
internal abstract class AnimationClock : FrameDrivenAnimation {
  private var lastFrameTimeNanos = UNSET_FRAME_TIME

  var playTimeNanos: Long = 0L
    private set

  val isTicking: Boolean
    get() = lastFrameTimeNanos != UNSET_FRAME_TIME

  final override fun onFrame(frameTimeNanos: Long): Boolean {
    if (lastFrameTimeNanos != UNSET_FRAME_TIME) {
      playTimeNanos += frameTimeNanos - lastFrameTimeNanos
    }
    lastFrameTimeNanos = frameTimeNanos
    val pending = advance(playTimeNanos)
    if (!pending) freeze()
    return pending
  }

  protected abstract fun advance(playTimeNanos: Long): Boolean

  /**
   * Preserves play time and re-anchors on the next tick. The owner unregisters this clock to
   * suspend ticks; playback eligibility must also prevent registration while paused.
   */
  fun freeze() {
    lastFrameTimeNanos = UNSET_FRAME_TIME
  }

  private companion object {
    const val UNSET_FRAME_TIME = Long.MIN_VALUE
  }
}

internal inline fun AnimationClock(crossinline onAdvance: (Long) -> Boolean): AnimationClock {
  return object : AnimationClock() {
    override fun advance(playTimeNanos: Long) = onAdvance(playTimeNanos)
  }
}
