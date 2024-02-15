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

/**
 * Plugin style that can be used for instrument before & after the event is dispatched by
 * [EventDispatcher.dispatchOnEvent]
 */
object EventDispatcherInstrumenter {

  @Volatile private var instance: Instrumenter? = null

  @JvmStatic
  fun provide(instrumenter: Instrumenter?) {
    instance = instrumenter
  }

  @JvmStatic
  fun isTracing(): Boolean {
    return instance?.isTracing() ?: false
  }

  @JvmStatic
  fun onBeginWork(eventHandler: EventHandler<*>, eventState: Any): Any? {
    return instance?.onBeginWork(eventHandler, eventState)
  }

  @JvmStatic
  fun onEndWork(token: Any?) {
    token?.let { instance?.onEndWork(it) }
  }

  interface Instrumenter {

    fun isTracing(): Boolean

    fun onBeginWork(eventHandler: EventHandler<*>, eventState: Any): Any?

    fun onEndWork(token: Any?)
  }
}
