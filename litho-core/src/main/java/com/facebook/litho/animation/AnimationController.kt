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

import android.view.Choreographer
import androidx.annotation.MainThread
import com.facebook.rendercore.thread.utils.ThreadUtils
import java.util.ArrayDeque

/**
 * Coalesces active [FrameDrivenAnimation]s onto a shared [AnimationFrameDriver].
 *
 * On each frame, all registered animations are ticked via [FrameDrivenAnimation.onFrame]. Terminal
 * notifications are delivered after value invalidations flush. A frame remains scheduled while
 * either animations or notifications need work.
 *
 * ## Frame Coalescing
 *
 * A single driver callback ticks all active animations per frame, avoiding per-animation callback
 * overhead.
 *
 * ## Testability
 *
 * Tests can temporarily swap the underlying [frameDriver] with [withFrameDriverForTest] to advance
 * frame delivery and frame time deterministically.
 *
 * @see AnimatedValue
 */
object AnimationController {

  private val activeAnimations = mutableSetOf<FrameDrivenAnimation>()
  private val completions = ArrayDeque<PostFrameCallback>()
  private val frameCallback = Choreographer.FrameCallback(::onFrame)
  private var frameScheduled = false
  private var isDispatchingFrame = false
  private var frameDriver: AnimationFrameDriver = ChoreographerAnimationFrameDriver

  /** Returns true if any animations are currently running. */
  val hasRunningAnimations: Boolean
    get() = activeAnimations.isNotEmpty()

  /**
   * Registers an animation for frame callbacks. Called when an animation starts. Must be called on
   * the main thread.
   */
  @MainThread
  internal fun register(animation: FrameDrivenAnimation) {
    ThreadUtils.assertMainThread()
    if (activeAnimations.add(animation)) {
      scheduleFrame()
    }
  }

  /**
   * Unregisters an animation from frame callbacks. Called when an animation is stopped or snapped
   * to a value.
   */
  @MainThread
  internal fun unregister(animation: FrameDrivenAnimation) {
    ThreadUtils.assertMainThread()
    if (activeAnimations.remove(animation) && activeAnimations.isEmpty() && completions.isEmpty()) {
      cancelScheduledFrame()
    }
  }

  @MainThread
  internal fun enqueueCompletion(completion: PostFrameCallback) {
    ThreadUtils.assertMainThread()
    completions.addLast(completion)
    scheduleFrame()
  }

  /**
   * Temporarily replaces the frame driver for deterministic tests.
   *
   * Any previously scheduled frame is cleared before the driver swap and after [block] completes so
   * tests start from and restore a clean controller state.
   */
  @MainThread
  internal inline fun <T> withFrameDriverForTest(
      driver: AnimationFrameDriver,
      block: () -> T,
  ): T {
    ThreadUtils.assertMainThread()
    val previousDriver = frameDriver
    resetControllerState()
    frameDriver = driver
    return try {
      block()
    } finally {
      resetControllerState()
      frameDriver = previousDriver
    }
  }

  private fun scheduleFrame() {
    if (frameScheduled || isDispatchingFrame) return
    frameScheduled = true
    frameDriver.postFrameCallback(frameCallback)
  }

  private fun cancelScheduledFrame() {
    if (!frameScheduled) return
    frameDriver.removeFrameCallback(frameCallback)
    frameScheduled = false
  }

  private fun resetControllerState() {
    check(!isDispatchingFrame)
    cancelScheduledFrame()
    activeAnimations.clear()
    completions.clear()
    AnimationInvalidationDispatcher.clear()
  }

  @MainThread
  private fun onFrame(frameTimeNanos: Long) {
    ThreadUtils.assertMainThread()
    check(!isDispatchingFrame) { "AnimationController frame dispatch is not re-entrant" }
    frameScheduled = false
    isDispatchingFrame = true
    try {
      if (activeAnimations.isNotEmpty()) {
        // Ticking may change registrations; completion delivery happens after the batch flush.
        val snapshot = activeAnimations.toTypedArray()
        AnimationInvalidationDispatcher.batch {
          for (animation in snapshot) {
            if (!activeAnimations.contains(animation)) continue
            if (!animation.onFrame(frameTimeNanos)) {
              activeAnimations.remove(animation)
            }
          }
        }
      }
      dispatchCompletions()
    } finally {
      isDispatchingFrame = false
      if (activeAnimations.isNotEmpty() || completions.isNotEmpty()) scheduleFrame()
    }
  }

  private fun dispatchCompletions() {
    var failure: Exception? = null
    // Notifications produced by callbacks or error handlers belong to a later frame.
    repeat(completions.size) {
      val completion = completions.removeFirst()
      try {
        completion.onPostFrame()
      } catch (exception: Exception) {
        val previous = failure
        if (previous == null) failure = exception
        else if (previous !== exception) previous.addSuppressed(exception)
      }
    }
    failure?.let { throw it }
  }
}
