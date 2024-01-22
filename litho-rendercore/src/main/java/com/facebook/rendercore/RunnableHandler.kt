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

package com.facebook.rendercore

import android.os.Handler
import android.os.Looper

/**
 * Handler abstraction to allow instrumentation and that is responsible for scheduling [Runnable]
 * computations in RenderCore. The default implementation uses a [Handler] with a [Looper].
 */
interface RunnableHandler {

  fun isTracing(): Boolean

  fun post(runnable: Runnable, tag: String)

  fun postAtFront(runnable: Runnable, tag: String)

  fun remove(runnable: Runnable)

  /** Default implementation of the RunnableHandler which simply wraps a [Handler]. */
  class DefaultHandler(looper: Looper) : Handler(looper), RunnableHandler {

    override fun isTracing(): Boolean {
      return false
    }

    override fun post(runnable: Runnable, tag: String) {
      post(runnable)
    }

    override fun postAtFront(runnable: Runnable, tag: String) {
      postAtFrontOfQueue(runnable)
    }

    override fun remove(runnable: Runnable) {
      removeCallbacks(runnable)
    }
  }
}
