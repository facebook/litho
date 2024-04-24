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
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState

/**
 * This LithoVisibilityEventsController implementation dispatches to the registered observers the
 * lifecycle state changes triggered by the provided LifecycleOwner. For example, if a Fragment is
 * passed as param, the observers will be registered to listen to all of the fragment's lifecycle
 * state changes.
 */
open class AOSPLithoVisibilityEventsController(
    final override val lifecycleOwner: LifecycleOwner,
    private val delegate: LithoVisibilityEventsControllerDelegate =
        LithoVisibilityEventsControllerDelegate()
) :
    LithoVisibilityEventsController by delegate,
    LifecycleEventObserver,
    AOSPLifecycleOwnerProvider {

  init {
    lifecycleOwner.lifecycle.addObserver(this)
  }

  constructor(
      lifecycleOwner: LifecycleOwner
  ) : this(lifecycleOwner = lifecycleOwner, delegate = LithoVisibilityEventsControllerDelegate())

  override fun onStateChanged(source: LifecycleOwner, event: Event) {
    when (event) {
      Lifecycle.Event.ON_RESUME -> moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
      Lifecycle.Event.ON_PAUSE -> moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
      Lifecycle.Event.ON_DESTROY -> {
        moveToVisibilityState(LithoVisibilityState.DESTROYED)
        lifecycleOwner.lifecycle.removeObserver(this)
      }
      else -> {}
    }
  }
}
