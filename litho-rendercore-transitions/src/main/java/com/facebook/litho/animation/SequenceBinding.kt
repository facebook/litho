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

/** An [AnimationBinding] that's a sequence of other [AnimationBinding]s. */
class SequenceBinding(private val bindings: List<AnimationBinding>) : BaseAnimationBinding() {

  private var resolver: Resolver? = null
  private var currentIndex = 0
  private var isActiveFlag = false

  private val childListener =
      object : AnimationBindingListener {
        override fun onScheduledToStartLater(binding: AnimationBinding) = Unit

        override fun onWillStart(binding: AnimationBinding) = Unit

        override fun onFinish(binding: AnimationBinding) {
          this@SequenceBinding.onBindingFinished(binding)
        }

        override fun onCanceledBeforeStart(binding: AnimationBinding) {
          this@SequenceBinding.onBindingFinished(binding)
        }

        override fun shouldStart(binding: AnimationBinding): Boolean = true
      }

  init {
    if (bindings.isEmpty()) {
      throw IllegalArgumentException("Empty binding sequence")
    }
  }

  private fun onBindingFinished(binding: AnimationBinding) {
    if (binding != bindings[currentIndex]) {
      throw RuntimeException("Unexpected Binding completed")
    }
    binding.removeListener(childListener)
    currentIndex++

    if (currentIndex >= bindings.size) {
      finish()
    } else {
      val r = resolver ?: return
      val next = bindings[currentIndex]
      next.addListener(childListener)
      next.start(r)
    }
  }

  private fun finish() {
    notifyFinished()
    isActiveFlag = false
    resolver = null
  }

  override fun start(resolver: Resolver) {
    if (isActiveFlag) {
      throw RuntimeException("Already started")
    }

    if (!shouldStart()) {
      notifyCanceledBeforeStart()
      return
    }
    notifyWillStart()
    // Notify all children except the first one that will start later
    for (i in 1 until bindings.size) {
      bindings[i].prepareToStartLater()
    }
    // Now start the first one
    isActiveFlag = true
    this.resolver = resolver
    val first = bindings[0]
    first.addListener(childListener)
    first.start(resolver)
  }

  override fun stop() {
    if (!isActiveFlag) {
      return
    }
    isActiveFlag = false
    bindings[currentIndex].stop()
  }

  override fun isActive(): Boolean = isActiveFlag

  override fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>) {
    for (i in bindings.indices) {
      bindings[i].collectTransitioningProperties(outList)
    }
  }

  override fun prepareToStartLater() {
    notifyScheduledToStartLater()
    for (i in bindings.indices) {
      bindings[i].prepareToStartLater()
    }
  }
}
