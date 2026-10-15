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

package com.facebook.litho.choreographercompat

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.annotation.VisibleForTesting
import com.facebook.litho.WorkContinuationInstrumenter
import com.facebook.rendercore.thread.utils.ThreadUtils

/**
 * Wrapper class for Choreographer functionality. If Choreographer is unavailable we fallback to
 * using a normal Handler.
 *
 * This code was taken from the facebook/rebound repository.
 */
class ChoreographerCompatImpl @VisibleForTesting internal constructor() : ChoreographerCompat {

  private val handler: Handler?
  private var choreographer: Choreographer? = null

  init {
    if (ThreadUtils.isMainThread) {
      choreographer = getChoreographer()
      handler = null
    } else {
      handler = Handler(Looper.getMainLooper())
      handler.post { choreographer = getChoreographer() }
    }
  }

  override fun postFrameCallback(callbackWrapper: ChoreographerCompat.FrameCallback) {
    callbackWrapper.tokenReference.set(
        WorkContinuationInstrumenter.onOfferWorkForContinuation(
            "ChoreographerCompat_postFrameCallback",
        ),
    )

    val localChoreographer = choreographer
    if (localChoreographer != null) {
      choreographerPostFrameCallback(localChoreographer, callbackWrapper.getFrameCallback())
    } else {
      handler?.postDelayed(callbackWrapper.getRunnable(), 0)
    }
  }

  override fun postFrameCallbackDelayed(
      callbackWrapper: ChoreographerCompat.FrameCallback,
      delayMillis: Long,
  ) {
    callbackWrapper.tokenReference.set(
        WorkContinuationInstrumenter.onOfferWorkForContinuation(
            "ChoreographerCompat_postFrameCallbackDelayed",
        ),
    )

    val localChoreographer = choreographer
    if (localChoreographer != null) {
      choreographerPostFrameCallbackDelayed(
          localChoreographer,
          callbackWrapper.getFrameCallback(),
          delayMillis,
      )
    } else {
      handler?.postDelayed(callbackWrapper.getRunnable(), delayMillis + ONE_FRAME_MILLIS)
    }
  }

  override fun removeFrameCallback(callbackWrapper: ChoreographerCompat.FrameCallback) {
    callbackWrapper.tokenReference.set(null)

    val localChoreographer = choreographer
    if (localChoreographer != null) {
      choreographerRemoveFrameCallback(localChoreographer, callbackWrapper.getFrameCallback())
    } else {
      handler?.removeCallbacks(callbackWrapper.getRunnable())
    }
  }

  private fun getChoreographer(): Choreographer = Choreographer.getInstance()

  private fun choreographerPostFrameCallback(
      choreographer: Choreographer,
      frameCallback: Choreographer.FrameCallback,
  ) {
    choreographer.postFrameCallback(frameCallback)
  }

  private fun choreographerPostFrameCallbackDelayed(
      choreographer: Choreographer,
      frameCallback: Choreographer.FrameCallback,
      delayMillis: Long,
  ) {
    choreographer.postFrameCallbackDelayed(frameCallback, delayMillis)
  }

  private fun choreographerRemoveFrameCallback(
      choreographer: Choreographer,
      frameCallback: Choreographer.FrameCallback,
  ) {
    choreographer.removeFrameCallback(frameCallback)
  }

  @VisibleForTesting fun isUsingChoreographer(): Boolean = choreographer != null

  companion object {
    private const val ONE_FRAME_MILLIS: Long = 17

    @JvmStatic private var instance: ChoreographerCompat = ChoreographerCompatImpl()

    /**
     * Returns a [ChoreographerCompat] instance for the main thread. This should really be on
     * [ChoreographerCompat] but interfaces don't support static methods until Java 8.
     *
     * NB: Unlike android.view.Choreographer#getInstance(), this method ALWAYS returns an instance
     * that posts to the MAIN THREAD android Choreographer.
     */
    @JvmStatic fun getInstance(): ChoreographerCompat = instance

    /** Set the ChoreographerCompat instance for tests. */
    @VisibleForTesting
    @JvmStatic
    fun setInstance(choreographerCompat: ChoreographerCompat?) {
      instance = choreographerCompat ?: ChoreographerCompatImpl()
    }
  }
}
