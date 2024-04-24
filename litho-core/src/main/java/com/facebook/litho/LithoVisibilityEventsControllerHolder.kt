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

import androidx.lifecycle.findViewTreeLifecycleOwner
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState

/**
 * This is a holder to form listening chains for LithoVisibilityEventsController in LithoView
 * hierarchy. Consider a simple following case:
 *
 * LithoView(holder1) <- nested LithoView(holder2) <- nested LithoView(holder3)
 *
 * In this case, holder3 will listen to holder2 and holder2 will listen to holder1. And when holder1
 * is added to hold an AOSPLithoVisibilityEventsController, holder2 and holder3 will also listen to
 * it automatically.
 */
internal class LithoVisibilityEventsControllerHolder :
    LithoVisibilityEventsController, LithoVisibilityEventsListener {

  private val lithoVisibilityEventsListeners: MutableSet<LithoVisibilityEventsListener> = HashSet()

  private var internalLifecycleProvider: LithoVisibilityEventsController =
      LithoVisibilityEventsControllerDelegate()

  private var hasHeldLifecycleProvider: Boolean = false

  private var isDefaultLifecycleProvider: Boolean = false

  fun getHeldLifecycleProvider(): LithoVisibilityEventsController? {
    return if (hasHeldLifecycleProvider) {
      internalLifecycleProvider
    } else {
      null
    }
  }

  @Synchronized
  @JvmOverloads
  fun setHeldLifecycleProvider(
      lifecycleProvider: LithoVisibilityEventsController?,
      isDefault: Boolean = false
  ) {
    if (internalLifecycleProvider == lifecycleProvider) {
      return
    }
    lithoVisibilityEventsListeners.forEach { listener ->
      internalLifecycleProvider.removeListener(listener)
    }
    // If lifecycleProvider is null, we re-create LithoVisibilityEventsControllerDelegate with
    // default state
    internalLifecycleProvider = lifecycleProvider ?: LithoVisibilityEventsControllerDelegate()
    hasHeldLifecycleProvider = lifecycleProvider != null

    lithoVisibilityEventsListeners.forEach { listener ->
      internalLifecycleProvider.addListener(listener)
    }
    isDefaultLifecycleProvider = isDefault
  }

  @Synchronized
  fun attachDefaultAOSPLithoVisibilityEventsController(lithoView: LithoView) {
    if (lithoView.isAttached && !hasHeldLifecycleProvider) {
      val lifecycleOwner = lithoView.findViewTreeLifecycleOwner()
      if (lifecycleOwner != null) {
        setHeldLifecycleProvider(AOSPLithoVisibilityEventsController(lifecycleOwner), true)
      }
    }
  }

  @Synchronized
  fun detachDefaultAOSPLithoVisibilityEventsController() {
    if (isDefaultLifecycleProvider) {
      setHeldLifecycleProvider(null)
    }
  }

  override fun onMovedToState(state: LithoVisibilityState) {
    if (hasHeldLifecycleProvider) {
      // If this holder has a held LifecycleProvider, we don't need to move state here because it
      // should move state based on its LifecycleOwner
      return
    }

    when (state) {
      LithoVisibilityState.HINT_VISIBLE -> {
        internalLifecycleProvider.moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
        return
      }
      LithoVisibilityState.HINT_INVISIBLE -> {
        internalLifecycleProvider.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
        return
      }
      LithoVisibilityState.DESTROYED -> {
        internalLifecycleProvider.moveToVisibilityState(LithoVisibilityState.DESTROYED)
        return
      }
      else -> throw IllegalStateException("Illegal state: $state")
    }
  }

  override fun moveToVisibilityState(lithoLifecycle: LithoVisibilityState) {
    internalLifecycleProvider.moveToVisibilityState(lithoLifecycle)
  }

  override val visibilityState: LithoVisibilityState
    get() = internalLifecycleProvider.visibilityState

  @Synchronized
  override fun addListener(listener: LithoVisibilityEventsListener) {
    internalLifecycleProvider.addListener(listener)
    lithoVisibilityEventsListeners.add(listener)
  }

  @Synchronized
  override fun removeListener(listener: LithoVisibilityEventsListener) {
    internalLifecycleProvider.removeListener(listener)
    lithoVisibilityEventsListeners.remove(listener)
  }
}
