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

import com.facebook.rendercore.RunnableHandler
import java.util.HashMap
import java.util.concurrent.Executor
import javax.annotation.concurrent.GuardedBy

/**
 * A `LithoHandler` implementation that runs all layout computations against the provided
 * `Executor`. This `LithoHandler` can be used in apps that have a well established threading model
 * and need to run layout against one of their already available executors.
 */
class ExecutorLithoHandler
/**
 * Instantiates an `ExecutorLithoHandler` that uses the given `Executor` to run all layout tasks
 * against.
 */
(private val executor: Executor) : RunnableHandler {

  @GuardedBy("pendingTasks") private val pendingTasks: MutableMap<Runnable, Int> = HashMap()

  override fun post(runnable: Runnable, tag: String) {
    synchronized(pendingTasks) {
      val runCount = pendingTasks[runnable] ?: 0
      pendingTasks.put(runnable, runCount + 1)
    }

    executor.execute {
      var canRun: Boolean
      synchronized(pendingTasks) {
        var runCount: Int = pendingTasks[runnable] ?: 0
        canRun = runCount > 0
        runCount--
        if (runCount > 0) {
          pendingTasks[runnable] = runCount
        } else {
          pendingTasks.remove(runnable)
        }
      }

      if (canRun) {
        runnable.run()
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * In this implementation, postAtFront() is equivalent to post().
   */
  override fun postAtFront(runnable: Runnable, tag: String) {
    post(runnable, tag)
  }

  /**
   * {@inheritDoc}
   *
   * This implementation removes all instances of the provided `Runnable` and prevents them from
   * running if they have not started yet.
   */
  override fun remove(runnable: Runnable) {
    synchronized(pendingTasks) { pendingTasks.remove(runnable) }
  }

  override fun isTracing(): Boolean = false
}
