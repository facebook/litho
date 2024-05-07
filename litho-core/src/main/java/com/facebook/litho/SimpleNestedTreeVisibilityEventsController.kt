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

import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState

/**
 * LithoVisibilityEventsController implementation that can be used to subscribe a nested
 * ComponentTree to listen to state changes of the lifecycle provider that the parent ComponentTree
 * is also subscribed to.
 */
class SimpleNestedTreeVisibilityEventsController(
    parentLifecycleProvider: LithoVisibilityEventsController?
) : LithoVisibilityEventsController, LithoVisibilityEventsListener {

  private val lithoLifecycleDelegate = LithoVisibilityEventsControllerDelegate()

  override val visibilityState: LithoVisibilityState
    get() = lithoLifecycleDelegate.visibilityState

  init {
    parentLifecycleProvider?.addListener(this)
  }

  override fun moveToVisibilityState(lithoLifecycle: LithoVisibilityState) {
    lithoLifecycleDelegate.moveToVisibilityState(lithoLifecycle)
  }

  override fun addListener(listener: LithoVisibilityEventsListener) {
    lithoLifecycleDelegate.addListener(listener)
  }

  override fun removeListener(listener: LithoVisibilityEventsListener) {
    lithoLifecycleDelegate.removeListener(listener)
  }

  override fun onMovedToState(state: LithoVisibilityState) {
    when (state) {
      LithoVisibilityState.HINT_VISIBLE -> moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
      LithoVisibilityState.HINT_INVISIBLE ->
          moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
      LithoVisibilityState.DESTROYED -> {}
    }
  }
}
