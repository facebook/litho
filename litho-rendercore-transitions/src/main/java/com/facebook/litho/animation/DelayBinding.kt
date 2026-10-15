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

import com.facebook.litho.choreographercompat.ChoreographerCompat
import com.facebook.litho.choreographercompat.ChoreographerCompatImpl

/** An [AnimationBinding] that adds a delay to the provided [AnimationBinding] */
class DelayBinding(private val delayMs: Int, private val binding: AnimationBinding) :
    BaseAnimationBinding() {

  private var hasStarted = false
  private var isActiveFlag = false
  private var resolver: Resolver? = null

  override fun prepareToStartLater() {
    notifyScheduledToStartLater()
    binding.prepareToStartLater()
  }

  override fun start(resolver: Resolver) {
    if (hasStarted) {
      throw RuntimeException("Starting binding multiple times")
    }
    hasStarted = true
    this.resolver = resolver

    if (!shouldStart()) {
      notifyCanceledBeforeStart()
      return
    }
    notifyWillStart()

    isActiveFlag = true

    binding.prepareToStartLater()

    val bindingListener =
        object : AnimationBindingListener {
          override fun onScheduledToStartLater(binding: AnimationBinding) = Unit

          override fun onWillStart(binding: AnimationBinding) = Unit

          override fun onFinish(binding: AnimationBinding) {
            binding.removeListener(this)
            this@DelayBinding.finish()
          }

          override fun onCanceledBeforeStart(binding: AnimationBinding) {
            onFinish(binding)
          }

          override fun shouldStart(binding: AnimationBinding): Boolean = true
        }
    binding.addListener(bindingListener)

    val delayedStartCallback =
        object : ChoreographerCompat.FrameCallback() {
          override fun doFrame(frameTimeNanos: Long) {
            if (!isActiveFlag) {
              return
            }
            val r = this@DelayBinding.resolver ?: return
            binding.start(r)
          }
        }
    ChoreographerCompatImpl.getInstance()
        .postFrameCallbackDelayed(delayedStartCallback, delayMs.toLong())
  }

  override fun stop() {
    if (!isActiveFlag) {
      return
    }
    isActiveFlag = false
    resolver = null
    if (binding.isActive()) {
      binding.stop()
    }
  }

  override fun isActive(): Boolean = isActiveFlag

  override fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>) {
    binding.collectTransitioningProperties(outList)
  }

  private fun finish() {
    isActiveFlag = false
    notifyFinished()
  }
}
