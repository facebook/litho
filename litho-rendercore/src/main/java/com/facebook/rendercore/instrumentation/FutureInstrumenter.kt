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

import java.util.concurrent.RunnableFuture

/**
 * Provides common instrumentation for [java.util.concurrent.Future](s) and related implementations.
 */
object FutureInstrumenter {

  interface Instrumenter {
    /**
     * Hook that allows to instrument a [RunnableFuture].
     *
     * @param future that has to be instrumented
     * @param tag used to mark the task for debugging purposes.
     * @return an instrumented [RunnableFuture] or returns the given input one.
     */
    fun <V> instrument(future: RunnableFuture<V>, tag: String): RunnableFuture<V>
  }

  @Volatile private var instance: Instrumenter? = null

  @JvmStatic
  fun provide(instrumenter: Instrumenter?) {
    instance = instrumenter
  }

  @JvmStatic
  fun <V> instrument(future: RunnableFuture<V>, tag: String): RunnableFuture<V> {
    val instrumenter = instance ?: return future
    return instrumenter.instrument(future, tag)
  }
}
