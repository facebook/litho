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

package com.facebook.litho

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.facebook.litho.LithoLifecycleProvider.LithoLifecycle

/**
 * This LithoLifecycleProvider implementation dispatches to the registered observers the lifecycle
 * state changes triggered by the provided LifecycleOwner. For example, if a Fragment is passed as
 * param, the observers will be registered to listen to all of the fragment's lifecycle state
 * changes.
 */
open class AOSPLithoLifecycleProvider(lifecycleOwner: LifecycleOwner) :
    LithoLifecycleProvider, LifecycleObserver, AOSPLifecycleOwnerProvider {

  init {
    lifecycleOwner.lifecycle.addObserver(this)
  }

  private val lithoLifecycleProviderDelegate: LithoLifecycleProviderDelegate =
      LithoLifecycleProviderDelegate()

  private val _lifecycleOwner: LifecycleOwner = lifecycleOwner

  override val lifecycleStatus: LithoLifecycle
    get() = lithoLifecycleProviderDelegate.lifecycleStatus

  override fun moveToLifecycle(lithoLifecycle: LithoLifecycle) {
    lithoLifecycleProviderDelegate.moveToLifecycle(lithoLifecycle)
  }

  override fun addListener(listener: LithoLifecycleListener) {
    lithoLifecycleProviderDelegate.addListener(listener)
  }

  override fun removeListener(listener: LithoLifecycleListener) {
    lithoLifecycleProviderDelegate.removeListener(listener)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  private fun onVisible() {
    moveToLifecycle(LithoLifecycle.HINT_VISIBLE)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  private fun onInvisible() {
    moveToLifecycle(LithoLifecycle.HINT_INVISIBLE)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  private fun onDestroy() {
    moveToLifecycle(LithoLifecycle.DESTROYED)
  }

  override val lifecycleOwner: LifecycleOwner?
    get() = _lifecycleOwner
}
