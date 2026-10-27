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

import androidx.annotation.MainThread
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentUtils

/**
 * Tracks committed target generations. Each mounted lifetime starts silently, including restored
 * tree state.
 */
internal class AnimationSettlement<T>(initialTarget: T) {
  private var target = initialTarget
  private var generation = 0L
  private var isMounted = false

  var isPending = false
    private set

  var context: ComponentContext? = null
    private set

  private var onSettled: ((T) -> Unit)? = null

  /** Returns whether this is the owner's first-ever commit, including after discarded resolves. */
  @MainThread
  fun commit(target: T, onSettled: ((T) -> Unit)?, context: ComponentContext?): Boolean {
    val initialCommit = generation == 0L
    if (!isMounted) {
      isMounted = true
      generation++
      isPending = false
    } else if (this.target != target) {
      generation++
      isPending = true
    }
    this.target = target
    this.onSettled = onSettled
    this.context = context
    return initialCommit
  }

  @MainThread
  fun complete() {
    if (!isPending) return
    isPending = false
    val completedGeneration = generation
    val completedTarget = target
    AnimationController.enqueueCompletion {
      if (!isMounted || generation != completedGeneration) return@enqueueCompletion
      dispatchAnimationCompletion(context) {
        onSettled?.invoke(completedTarget)
      }
    }
  }

  @MainThread
  fun unmount() {
    isMounted = false
    generation++
    isPending = false
    onSettled = null
    context = null
  }
}

/** Capture the reporting context before a callback can unmount its owner. */
@MainThread
internal inline fun dispatchAnimationCompletion(context: ComponentContext?, callback: () -> Unit) {
  try {
    callback()
  } catch (exception: Exception) {
    if (context == null) throw exception
    ComponentUtils.handle(context, exception)
  }
}
