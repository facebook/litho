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
 * Creates collection of animators that will run one after another in sequence when
 * [SequenceAnimation.start] is triggered
 */
class SequenceAnimation(private val animators: Array<out AnimatedAnimation>) : AnimatedAnimation {
  private val sequenceAnimatorListeners = mutableListOf<AnimationFinishListener>()
  private var currentIndex = 0
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
    startAnimator()
  }

  @ThreadConfined(ThreadConfined.UI)
  override fun cancel() {
    if (!_isActive) {
      // ignore multiple cancel calls
      return
    }
    _isActive = false
    if (animators[currentIndex].isActive()) {
      animators[currentIndex].cancel()
    }
    sequenceAnimatorListeners.forEach { it.onFinish(true) }
    finish()
  }

  override fun addListener(finishListener: AnimationFinishListener) {
    if (!sequenceAnimatorListeners.contains(finishListener)) {
      sequenceAnimatorListeners.add(finishListener)
    }
  }

  private fun animatorFinished(cancelled: Boolean) {
    if (!_isActive || cancelled) {
      return
    }
    currentIndex++
    if (currentIndex == animators.size) {
      finish()
      sequenceAnimatorListeners.forEach { it.onFinish(false) }
      return
    }
    startAnimator()
  }

  private fun startAnimator() {
    animators[currentIndex].start()
  }

  private fun finish() {
    _isActive = false
    currentIndex = 0
  }
}
