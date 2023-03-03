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

import com.facebook.litho.LithoLifecycleProvider.LithoLifecycle

/**
 * LithoLifecycleProvider implementation that can be used to subscribe a nested ComponentTree to
 * listen to state changes of the lifecycle provider that the parent ComponentTree is also
 * subscribed to.
 */
class SimpleNestedTreeLifecycleProvider(parentLifecycleProvider: LithoLifecycleProvider?) :
    LithoLifecycleProvider, LithoLifecycleListener {

  private val lithoLifecycleDelegate = LithoLifecycleProviderDelegate()

  override val lifecycleStatus: LithoLifecycle
    get() = lithoLifecycleDelegate.lifecycleStatus

  init {
    parentLifecycleProvider?.addListener(this)
  }

  override fun moveToLifecycle(lithoLifecycle: LithoLifecycle) {
    lithoLifecycleDelegate.moveToLifecycle(lithoLifecycle)
  }

  override fun addListener(listener: LithoLifecycleListener) {
    lithoLifecycleDelegate.addListener(listener)
  }

  override fun removeListener(listener: LithoLifecycleListener) {
    lithoLifecycleDelegate.removeListener(listener)
  }

  override fun onMovedToState(state: LithoLifecycle) {
    when (state) {
      LithoLifecycle.HINT_VISIBLE -> moveToLifecycle(LithoLifecycle.HINT_VISIBLE)
      LithoLifecycle.HINT_INVISIBLE -> moveToLifecycle(LithoLifecycle.HINT_INVISIBLE)
      LithoLifecycle.DESTROYED -> {
        /* do nothing */
      }
    }
  }
}
