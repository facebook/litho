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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LayoutThreadPoolConfiguration
import com.facebook.rendercore.RunnableHandler
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import kotlin.jvm.JvmField

/** LithoHandler implementation that uses a thread pool to calculate the layout. */
class ThreadPoolLayoutHandler private constructor(configuration: LayoutThreadPoolConfiguration) :
    RunnableHandler {

  private object DefaultThreadPoolHolder {
    val INSTANCE = ThreadPoolLayoutHandler(DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION)
  }

  private val layoutThreadPoolExecutor: ThreadPoolExecutor =
      LayoutThreadPoolExecutor(
          configuration.corePoolSize,
          configuration.maxPoolSize,
          configuration.threadPriority,
          configuration.layoutThreadInitializer)

  override fun isTracing(): Boolean = false

  override fun post(runnable: Runnable, tag: String) {
    try {
      layoutThreadPoolExecutor.execute(runnable)
    } catch (e: RejectedExecutionException) {
      throw RuntimeException("Cannot execute layout calculation task; $e")
    }
  }

  override fun postAtFront(runnable: Runnable, tag: String) {
    throw IllegalStateException("postAtFront is not supported for ThreadPoolLayoutHandler")
  }

  override fun remove(runnable: Runnable) {
    layoutThreadPoolExecutor.remove(runnable)
  }

  companion object {
    @JvmField
    val DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION: LayoutThreadPoolConfiguration =
        LayoutThreadPoolConfigurationImpl(
            corePoolSize = 2,
            maxPoolSize = 2,
            threadPriority = ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY)

    @get:JvmStatic
    val defaultInstance: RunnableHandler
      /**
       * Gets the default static singleton reference to [ThreadPoolLayoutHandler]. It is preferred
       * to use the default thread pool in order prevent the app creating too many threads.
       *
       * @return default `ThreadPoolLayoutHandler`.
       */
      get() = DefaultThreadPoolHolder.INSTANCE

    /**
     * Creates a new [ThreadPoolLayoutHandler] with the provided configuration. This method will
     * create a new `ThreadPoolExecutor` which can negatively affect the performance of the app.
     *
     * @param configuration [com.facebook.litho.config.LayoutThreadPoolConfiguration] specifying
     *   core and max pool size, and thread priority
     * @return new instance with a separate `ThreadPoolExecutor` with specified configuration.
     */
    @JvmStatic
    fun getNewInstance(configuration: LayoutThreadPoolConfiguration): RunnableHandler =
        ThreadPoolLayoutHandler(configuration)
  }
}
