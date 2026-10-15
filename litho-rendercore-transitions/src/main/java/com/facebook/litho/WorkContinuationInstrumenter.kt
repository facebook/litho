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

import com.facebook.litho.WorkContinuationInstrumenter.Instrumenter
import kotlin.concurrent.Volatile

/**
 * Utility class that allows to capture work continuation updates. Client code can specify a custom
 * [Instrumenter] that will receive ad-hoc updates when work that has to be executed across threads
 * gets moved around.
 */
object WorkContinuationInstrumenter {

  @Volatile private var instance: Instrumenter? = null

  /**
   * Allows to provide an instrumenter that will receive work continuation updates.
   *
   * @param instrumenter that will receive the updates or null to reset.
   */
  @JvmStatic
  fun provide(instrumenter: Instrumenter?) {
    instance = instrumenter
  }

  @get:JvmStatic
  val isTracing: Boolean
    get() {
      val instrumenter = instance ?: return false
      return instrumenter.isTracing
    }

  @JvmStatic
  fun onAskForWorkToContinue(tag: String): Any? {
    val instrumenter = instance ?: return null
    return instrumenter.onAskForWorkToContinue(tag)
  }

  @JvmStatic
  fun onOfferWorkForContinuation(tag: String): Any? {
    val instrumenter = instance ?: return null
    return instrumenter.onOfferWorkForContinuation(tag)
  }

  @JvmStatic
  fun onOfferWorkForContinuation(tag: String, token: Any?): Any? {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return null
    }
    return instrumenter.onOfferWorkForContinuation(tag, token)
  }

  @JvmStatic
  fun onBeginWorkContinuation(tag: String, token: Any?): Any? {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return null
    }
    return instrumenter.onBeginWorkContinuation(tag, token)
  }

  @JvmStatic
  fun onEndWorkContinuation(token: Any?) {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return
    }
    instrumenter.onEndWorkContinuation(token)
  }

  @JvmStatic
  fun markFailure(token: Any?, th: Throwable) {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return
    }
    instrumenter.markFailure(token, th)
  }

  /** Allows to record work being continued across threads. */
  interface Instrumenter {
    /**
     * Allows to know in advance if the custom instrumenter desires to receive continuation updates.
     *
     * @return true to specify interest in handling the updates, false otherwise.
     */
    val isTracing: Boolean

    /**
     * Captures when asking for work to be stolen.
     *
     * @param tag name.
     * @return a token object that allows to track the continuation.
     */
    fun onAskForWorkToContinue(tag: String): Any?

    /**
     * Tracks when some work is ready to offered for continuation.
     *
     * @param tag name.
     * @return a token object that allows to track the continuation.
     */
    fun onOfferWorkForContinuation(tag: String): Any?

    /**
     * Tracks when some work is ready to be stolen.
     *
     * @param tag name (optional).
     * @param token returned by [Instrumenter#onAskForWorkToContinue].
     * @return a token object that allows to track the continuation.
     */
    fun onOfferWorkForContinuation(tag: String, token: Any): Any?

    /**
     * Captures the beginning of the continuation for stolen work.
     *
     * @param tag name (optional).
     * @param token returned by [Instrumenter#onOfferWorkForContinuation].
     * @return a token object that allows to track the continuation.
     */
    fun onBeginWorkContinuation(tag: String, token: Any): Any?

    /**
     * Captures the end of the continuation for stolen work.
     *
     * @param token returned by [Instrumenter#onBeginWorkContinuation].
     */
    fun onEndWorkContinuation(token: Any)

    /**
     * Reports a failure while executing work.
     *
     * <note>[Instrumenter#onEndWorkContinuation(Object)] (Object)} still needs to be invoked.
     *
     * @param token returned by [Instrumenter#onBeginWorkContinuation(String, Object)] (Object,
     *   String)}.
     * @param th containing the failure. </note>
     */
    fun markFailure(token: Any, th: Throwable)
  }
}
