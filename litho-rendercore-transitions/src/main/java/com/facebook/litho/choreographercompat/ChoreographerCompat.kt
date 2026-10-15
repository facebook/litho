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

import android.view.Choreographer
import com.facebook.litho.WorkContinuationInstrumenter
import java.util.concurrent.atomic.AtomicReference

/** See [ChoreographerCompatImpl]. Interface exists for mocking out in tests. */
interface ChoreographerCompat {

  fun postFrameCallback(callbackWrapper: FrameCallback)

  fun postFrameCallbackDelayed(callbackWrapper: FrameCallback, delayMillis: Long)

  fun removeFrameCallback(callbackWrapper: FrameCallback)

  /**
   * This class provides a compatibility wrapper around the JellyBean FrameCallback with methods to
   * access cached wrappers for submitting a real FrameCallback to a Choreographer or a Runnable to
   * a Handler.
   */
  abstract class FrameCallback {

    private var runnable: Runnable? = null
    private var frameCallback: Choreographer.FrameCallback? = null

    @JvmField val tokenReference: AtomicReference<Any?> = AtomicReference()

    fun getFrameCallback(): Choreographer.FrameCallback {
      var callback = frameCallback
      if (callback == null) {
        callback = Choreographer.FrameCallback { frameTimeNanos ->
          this@FrameCallback.doFrameInternal(frameTimeNanos)
        }
        frameCallback = callback
      }
      return callback
    }

    fun getRunnable(): Runnable {
      var r = runnable
      if (r == null) {
        r = Runnable { doFrameInternal(System.nanoTime()) }
        runnable = r
      }
      return r
    }

    private fun doFrameInternal(frameTimeNanos: Long) {
      val continuationToken =
          WorkContinuationInstrumenter.onBeginWorkContinuation(
              "ChoreographerCompat_doFrame",
              tokenReference.getAndSet(null),
          )
      try {
        doFrame(frameTimeNanos)
      } catch (th: Throwable) {
        WorkContinuationInstrumenter.markFailure(continuationToken, th)
        throw th
      } finally {
        WorkContinuationInstrumenter.onEndWorkContinuation(continuationToken)
      }
    }

    abstract fun doFrame(frameTimeNanos: Long)
  }
}
