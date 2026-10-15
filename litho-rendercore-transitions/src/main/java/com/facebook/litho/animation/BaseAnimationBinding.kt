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

import java.util.concurrent.CopyOnWriteArrayList

/**
 * A base implementation of [AnimationBinding] interface, that takes care of adding/removing
 * listeners and provides helper methods for dealing with them
 */
abstract class BaseAnimationBinding : AnimationBinding {
  private val listeners: CopyOnWriteArrayList<AnimationBindingListener> = CopyOnWriteArrayList()
  private var tag: Any? = null

  final override fun addListener(animationBindingListener: AnimationBindingListener) {
    listeners.add(animationBindingListener)
  }

  final override fun removeListener(animationBindingListener: AnimationBindingListener) {
    listeners.remove(animationBindingListener)
  }

  override fun getTag(): Any? = tag

  override fun setTag(tag: Any?) {
    this.tag = tag
  }

  /**
   * Checks with all added [AnimationBindingListener] if should start
   *
   * @return true if *all* listeners return true from [AnimationBindingListener.shouldStart],
   *   false - otherwise
   */
  fun shouldStart(): Boolean {
    for (index in listeners.indices.reversed()) {
      val listener = listeners[index]
      if (!listener.shouldStart(this)) {
        return false
      }
    }
    return true
  }

  /** Notifies all the added [AnimationBindingListener]s that has been canceled before start. */
  fun notifyCanceledBeforeStart() {
    for (index in listeners.indices.reversed()) {
      listeners[index].onCanceledBeforeStart(this)
    }
  }

  /** Notifies all the added [AnimationBindingListener]s that will start. */
  fun notifyWillStart() {
    for (index in listeners.indices.reversed()) {
      listeners[index].onWillStart(this)
    }
  }

  /** Notifies all the added [AnimationBindingListener]s that has finished. */
  fun notifyFinished() {
    for (index in listeners.indices.reversed()) {
      listeners[index].onFinish(this)
    }
  }

  /** Notifies the added [AnimationBindingListener]s that has been scheduled to start later. */
  fun notifyScheduledToStartLater() {
    for (index in listeners.indices.reversed()) {
      listeners[index].onScheduledToStartLater(this)
    }
  }
}
