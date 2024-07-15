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

@file:JvmName("LithoLifecycleOwner")

package com.facebook.litho.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.TreeProp
import com.facebook.litho.treePropOf

@JvmField val LifecycleOwnerTreeProp: TreeProp<LifecycleOwner?> = treePropOf { null }

/**
 * The [LifecycleOwnerWrapper] is an abstraction that will allow [LiveData] to observe a [Lifecycle]
 * before the [LifecycleOwner] is determined. Once the [LifecycleOwner] is set on the
 * [LifecycleOwnerWrapper] all the [LifecycleObserver] in [observers] will be added to the delegate
 * [Lifecycle].
 */
class LifecycleOwnerWrapper
internal constructor(
    private var delegate: LifecycleOwner?,
) : LifecycleOwner, Lifecycle() {

  private val observers: MutableSet<LifecycleObserver> = HashSet()
  private var latestState = State.RESUMED

  override val currentState: State
    get() = synchronized(this) { delegate?.lifecycle?.currentState ?: latestState }

  @Synchronized
  override fun addObserver(observer: LifecycleObserver) {
    // add new observer
    observers.add(observer)

    val delegate = delegate
    delegate?.lifecycle?.addObserver(observer)

    // If the delegate is null, no events are dispatched to the observer at this moment. It will be
    // dispatched when the delegate is set
  }

  @Synchronized
  override fun removeObserver(observer: LifecycleObserver) {
    val delegate = delegate
    delegate?.lifecycle?.removeObserver(observer)

    observers.remove(observer)
  }

  override val lifecycle: Lifecycle = this

  @Synchronized
  fun setDelegate(value: LifecycleOwner?) {
    if (value == this) {
      throw IllegalArgumentException("Cannot set a LifecycleOwnerWrapper as its own delegate")
    }

    // if the value is already the delegate then do nothing
    if (value === delegate) {
      return
    }

    // if a delegate is present then all observers need
    // to be removed from it and added to the new value
    delegate?.let {
      for (observer in observers) {
        it.lifecycle.removeObserver(observer)
      }
    }

    // add all observers to value
    value?.lifecycle?.let { lifecycle ->
      for (observer in observers) {
        lifecycle.addObserver(observer)
      }
    }

    // store the latest state if value is null
    delegate?.let {
      if (value == null) {
        latestState = it.lifecycle.currentState
      }
    }
    // set value as the new delegate
    delegate = value
  }

  fun hasObservers(): Boolean = observers.isNotEmpty()

  @JvmName("getDelegate")
  @Synchronized
  internal fun getDelegate(): LifecycleOwner? {
    return delegate
  }
}
