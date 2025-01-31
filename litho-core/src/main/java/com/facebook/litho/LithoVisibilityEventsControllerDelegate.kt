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

import androidx.annotation.IntDef
import androidx.annotation.UiThread
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import java.lang.IllegalStateException
import java.util.ArrayList

/**
 * Default [LithoVisibilityEventsController] implementation. Defines the standard state changes for
 * a Litho ComponentTree. A custom LithoVisibilityEventsController implementation can change the
 * lifecycle state but delegate to this to handle the effects of the state change. See an example of
 * how this facilitates a custom lifecycle implementation in [AOSPLithoVisibilityEventsController].
 */
class LithoVisibilityEventsControllerDelegate(
    private var _visibilityState: LithoVisibilityState = LithoVisibilityState.HINT_VISIBLE
) : LithoVisibilityEventsController {

  private val lithoVisibilityEventsListeners: MutableSet<LithoVisibilityEventsListener> = HashSet()

  override val visibilityState: LithoVisibilityState
    get() = _visibilityState

  @IntDef(
      LifecycleTransitionStatus.VALID,
      LifecycleTransitionStatus.NO_OP,
      LifecycleTransitionStatus.INVALID)
  annotation class LifecycleTransitionStatus {
    companion object {
      const val VALID = 0
      const val NO_OP = 1
      const val INVALID = 2
    }
  }

  @UiThread
  override fun moveToVisibilityState(newVisibilityState: LithoVisibilityState) {
    ThreadUtils.assertMainThread()
    if (newVisibilityState === LithoVisibilityState.DESTROYED &&
        _visibilityState === LithoVisibilityState.HINT_VISIBLE) {
      moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
    }
    @LifecycleTransitionStatus
    val transitionStatus = getTransitionState(_visibilityState, newVisibilityState)
    if (transitionStatus == LifecycleTransitionStatus.INVALID) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          "LithoVisibilityEventsController",
          "Cannot move from state $_visibilityState to state $newVisibilityState")
      return
    }
    if (transitionStatus == LifecycleTransitionStatus.VALID) {
      _visibilityState = newVisibilityState
      when (newVisibilityState) {
        LithoVisibilityState.HINT_VISIBLE -> {
          notifyOnResumeVisible()
          return
        }
        LithoVisibilityState.HINT_INVISIBLE -> {
          notifyOnPauseVisible()
          return
        }
        LithoVisibilityState.DESTROYED -> {
          notifyOnDestroy()
          return
        }
        else -> throw IllegalStateException("State not known")
      }
    }
  }

  @Synchronized
  override fun addListener(listener: LithoVisibilityEventsListener) {
    lithoVisibilityEventsListeners.add(listener)
  }

  @Synchronized
  override fun removeListener(listener: LithoVisibilityEventsListener) {
    lithoVisibilityEventsListeners.remove(listener)
  }

  private fun notifyOnResumeVisible() {
    val lithoVisibilityEventsListeners: List<LithoVisibilityEventsListener>
    synchronized(this) {
      lithoVisibilityEventsListeners = ArrayList(this.lithoVisibilityEventsListeners)
    }
    for (lithoLifecycleListener in lithoVisibilityEventsListeners) {
      lithoLifecycleListener.onMovedToState(LithoVisibilityState.HINT_VISIBLE)
    }
  }

  private fun notifyOnPauseVisible() {
    val lithoVisibilityEventsListeners: List<LithoVisibilityEventsListener>
    synchronized(this) {
      lithoVisibilityEventsListeners = ArrayList(this.lithoVisibilityEventsListeners)
    }
    for (lithoLifecycleListener in lithoVisibilityEventsListeners) {
      lithoLifecycleListener.onMovedToState(LithoVisibilityState.HINT_INVISIBLE)
    }
  }

  private fun notifyOnDestroy() {
    val lithoVisibilityEventsListeners: List<LithoVisibilityEventsListener>
    synchronized(this) {
      lithoVisibilityEventsListeners = ArrayList(this.lithoVisibilityEventsListeners)
    }
    for (lithoLifecycleListener in lithoVisibilityEventsListeners) {
      lithoLifecycleListener.onMovedToState(LithoVisibilityState.DESTROYED)
    }
  }

  companion object {
    @LifecycleTransitionStatus
    private fun getTransitionState(
        currentState: LithoVisibilityState,
        nextState: LithoVisibilityState
    ): Int {
      if (currentState === LithoVisibilityState.DESTROYED) {
        return LifecycleTransitionStatus.INVALID
      }
      if (nextState === LithoVisibilityState.DESTROYED) {
        // You have to move through HINT_INVISIBLE before moving to DESTROYED.
        return if (currentState === LithoVisibilityState.HINT_INVISIBLE)
            LifecycleTransitionStatus.VALID
        else LifecycleTransitionStatus.INVALID
      }
      if (nextState === LithoVisibilityState.HINT_VISIBLE) {
        if (currentState === LithoVisibilityState.HINT_VISIBLE) {
          return LifecycleTransitionStatus.NO_OP
        }
        return if (currentState === LithoVisibilityState.HINT_INVISIBLE) {
          LifecycleTransitionStatus.VALID
        } else {
          LifecycleTransitionStatus.INVALID
        }
      }
      if (nextState === LithoVisibilityState.HINT_INVISIBLE) {
        if (currentState === LithoVisibilityState.HINT_INVISIBLE) {
          return LifecycleTransitionStatus.NO_OP
        }
        return if (currentState === LithoVisibilityState.HINT_VISIBLE) {
          LifecycleTransitionStatus.VALID
        } else {
          LifecycleTransitionStatus.INVALID
        }
      }
      return LifecycleTransitionStatus.INVALID
    }
  }
}
