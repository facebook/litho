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
 * Creates animation that will run in a loop [repeatCount] times when [LoopAnimation.start] is
 * triggered
 */
class LoopAnimation(private var repeatCount: Int, private val animation: AnimatedAnimation) :
    AnimatedAnimation {
  private val loopAnimatorListeners = mutableListOf<AnimationFinishListener>()
  private var currentLoop = 0
  private var _isActive: Boolean = false

  init {
    animation.addListener(
        object : AnimationFinishListener {
          override fun onFinish(cancelled: Boolean) {
            if (cancelled && _isActive) {
              // child was cancelled, main animation is still active
              cancel()
              return
            }
            loopOrEndAnimation(cancelled)
          }
        })
  }

  @ThreadConfined(ThreadConfined.UI) override fun isActive(): Boolean = _isActive

  @ThreadConfined(ThreadConfined.UI)
  override fun start() {
    check(!_isActive) { "start() called more than once" }
    _isActive = true
    animation.start()
  }

  @ThreadConfined(ThreadConfined.UI)
  override fun cancel() {
    if (!_isActive) {
      // ignore multiple cancel calls
      return
    }
    _isActive = false
    if (animation.isActive()) {
      animation.cancel()
    }
    loopAnimatorListeners.forEach { it.onFinish(true) }
    finish()
  }

  override fun addListener(finishListener: AnimationFinishListener) {
    if (!loopAnimatorListeners.contains(finishListener)) {
      loopAnimatorListeners.add(finishListener)
    }
  }

  private fun loopOrEndAnimation(cancelled: Boolean) {
    if (!_isActive || cancelled) {
      return
    }
    currentLoop++
    if (currentLoop == repeatCount) {
      loopAnimatorListeners.forEach { it.onFinish(false) }
      finish()
      return
    }
    animation.start()
  }

  private fun finish() {
    _isActive = false
    currentLoop = 0
  }
}
