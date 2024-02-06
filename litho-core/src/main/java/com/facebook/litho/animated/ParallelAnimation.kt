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

package com.facebook.litho.animated

import com.facebook.infer.annotation.ThreadConfined

/**
 * Creates collection of animators that will run in parallel when [ParallelAnimation.start] is
 * triggered
 */
class ParallelAnimation(private val animators: Array<out AnimatedAnimation>) : AnimatedAnimation {
  private val parallelAnimationListeners = mutableListOf<AnimationFinishListener>()
  private var parallelFinishedAnimators = 0
  private var _isActive: Boolean = false

  init {
    animators.forEach {
      it.addListener(
          object : AnimationFinishListener {
            override fun onFinish(cancelled: Boolean) {
              if (cancelled && _isActive) {
                // child was cancelled, main animation is still active
                cancel()
                return
              }
              animatorFinished(cancelled)
            }
          })
    }
  }

  @ThreadConfined(ThreadConfined.UI) override fun isActive(): Boolean = _isActive

  @ThreadConfined(ThreadConfined.UI)
  override fun start() {
    check(!_isActive) { "start() called more than once" }
    if (animators.isEmpty()) {
      throw IllegalArgumentException("Empty animators collection")
    }
    _isActive = true
    startAnimators()
  }

  @ThreadConfined(ThreadConfined.UI)
  override fun cancel() {
    if (!_isActive) {
      // ignore multiple cancel calls
      return
    }
    _isActive = false
    animators.forEach {
      if (it.isActive()) {
        it.cancel()
      }
    }
    parallelAnimationListeners.forEach { it.onFinish(true) }
    finish()
  }

  override fun addListener(finishListener: AnimationFinishListener) {
    if (!parallelAnimationListeners.contains(finishListener)) {
      parallelAnimationListeners.add(finishListener)
    }
  }

  private fun animatorFinished(cancelled: Boolean) {
    if (!_isActive || cancelled) {
      return
    }
    parallelFinishedAnimators++
    if (animators.size == parallelFinishedAnimators) {
      finish()
      parallelAnimationListeners.forEach { it.onFinish(false) }
    }
  }

  private fun startAnimators() {
    animators.forEach { it.start() }
  }

  private fun finish() {
    _isActive = false
    parallelFinishedAnimators = 0
  }
}
