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

/**
 * An [AnimationBinding] that's composed of other [AnimationBinding]s running in parallel, possibly
 * starting on a stagger.
 */
class ParallelBinding(private val staggerMs: Int, private val bindings: List<AnimationBinding>) :
    BaseAnimationBinding() {

  private val bindingsFinished: HashSet<AnimationBinding> = HashSet()
  private var nextIndexToStart = 0
  private var childrenFinished = 0
  private var hasStarted = false
  private var _isActive = false
  private var resolver: Resolver? = null

  private val childListener =
      object : AnimationBindingListener {
        override fun onScheduledToStartLater(binding: AnimationBinding) = Unit

        override fun onWillStart(binding: AnimationBinding) = Unit

        override fun onFinish(binding: AnimationBinding) {
          this@ParallelBinding.onBindingFinished(binding)
        }

        override fun onCanceledBeforeStart(binding: AnimationBinding) {
          this@ParallelBinding.onBindingFinished(binding)
        }

        override fun shouldStart(binding: AnimationBinding): Boolean = true
      }

  private val staggerCallback: ChoreographerCompat.FrameCallback? =
      if (staggerMs == 0) {
        null
      } else {
        object : ChoreographerCompat.FrameCallback() {
          override fun doFrame(frameTimeNanos: Long) {
            if (!_isActive) {
              return
            }
            startNextBindingForStagger()
          }
        }
      }

  init {
    if (bindings.isEmpty()) {
      throw IllegalArgumentException("Empty binding parallel")
    }
  }

  private fun onBindingFinished(binding: AnimationBinding) {
    if (bindingsFinished.contains(binding)) {
      throw RuntimeException("Binding unexpectedly completed twice")
    }
    bindingsFinished.add(binding)
    childrenFinished++
    binding.removeListener(childListener)

    if (childrenFinished >= bindings.size) {
      finish()
    }
  }

  private fun finish() {
    _isActive = false
    notifyFinished()
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

    _isActive = true

    for (binding in bindings) {
      binding.addListener(childListener)
    }
    if (staggerMs == 0) {
      for (binding in bindings) {
        binding.start(resolver)
      }
      nextIndexToStart = bindings.size
    } else {
      // Notify all children except the first one that will start later
      for (i in 1 until bindings.size) {
        bindings[i].prepareToStartLater()
      }
      // Now start the first one
      startNextBindingForStagger()
    }
  }

  private fun startNextBindingForStagger() {
    val r = resolver ?: return
    bindings[nextIndexToStart].start(r)
    nextIndexToStart++

    if (nextIndexToStart < bindings.size) {
      val callback = staggerCallback ?: return
      ChoreographerCompatImpl.getInstance().postFrameCallbackDelayed(callback, staggerMs.toLong())
    }
  }

  override fun stop() {
    if (!_isActive) {
      return
    }
    _isActive = false
    resolver = null
    for (binding in bindings) {
      if (binding.isActive()) {
        binding.stop()
      }
    }
  }

  override fun isActive(): Boolean = _isActive

  override fun collectTransitioningProperties(outList: ArrayList<PropertyAnimation>) {
    for (binding in bindings) {
      binding.collectTransitioningProperties(outList)
    }
  }

  override fun prepareToStartLater() {
    notifyScheduledToStartLater()
    for (binding in bindings) {
      binding.prepareToStartLater()
    }
  }
}
