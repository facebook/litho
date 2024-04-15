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

package com.facebook.rendercore.instrumentation

import com.facebook.rendercore.RunnableHandler

object HandlerInstrumenter {

  @Volatile private var instrumenter: Instrumenter? = null

  @JvmStatic
  fun provide(instrumenter: Instrumenter?) {
    this.instrumenter = instrumenter
  }

  /** [Instrumenter.instrumentHandler] */
  @JvmStatic
  fun instrumentHandler(handler: RunnableHandler): RunnableHandler {
    val instrumenter = instrumenter ?: return handler
    return instrumenter.instrumentHandler(handler)
  }

  fun interface Instrumenter {
    /**
     * Instrument a [RunnableHandler] or return the same given [RunnableHandler]. If the
     * [RunnableHandler] given as input is null, the return value will be null too.
     */
    fun instrumentHandler(handler: RunnableHandler): RunnableHandler
  }
}
