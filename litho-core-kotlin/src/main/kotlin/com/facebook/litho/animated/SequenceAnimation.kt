/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
  private val sequenceAnimatorListeners = mutableListOf<AnimationListener>()
  private var currentIndex = 0
  private var isActive = false
  init {
    animators.forEach {
      it.addListener(
          object : AnimationListener {
            override fun onFinish() {
              animatorFinished()
            }
          })
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  override fun start() {
    check(!isActive) { "start() called more than once" }
    if (animators.isEmpty()) {
      throw IllegalArgumentException("Empty animators collection")
    }
    isActive = true
    startAnimator()
  }

  @ThreadConfined(ThreadConfined.UI)
  override fun stop() {
    animators[currentIndex].stop()
    finish()
  }

  override fun addListener(listener: AnimationListener) {
    if (!sequenceAnimatorListeners.contains(listener)) {
      sequenceAnimatorListeners.add(listener)
    }
  }

  private fun animatorFinished() {
    currentIndex++
    if (currentIndex == animators.size) {
      finish()
      sequenceAnimatorListeners.forEach { it.onFinish() }
      return
    }
    startAnimator()
  }

  private fun startAnimator() {
    animators[currentIndex].start()
  }

  private fun finish() {
    isActive = false
    currentIndex = 0
  }
}
