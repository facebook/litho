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

import android.os.Process
import com.facebook.rendercore.instrumentation.FutureInstrumenter
import com.facebook.rendercore.utils.ThreadUtils.getResultInheritingPriority
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * A future that lets the thread running the computation inherit the priority of any thread waiting
 * on it (if greater).
 *
 * @param <T> The type that is returned from the future
 */
open class ThreadInheritingPriorityFuture<T>(callable: Callable<T>, tag: String) {
  private var futureTask: RunnableFuture<T>? =
      FutureInstrumenter.instrument(FutureTask(callable), tag)
  private var resolvedResult: T? = null
  private val runningThreadId = AtomicInteger(-1)

  fun runAndGet(): T {
    val runnableFuture: RunnableFuture<T>?
    val existingResult: T?
    synchronized(this) {
      runnableFuture = futureTask
      existingResult = resolvedResult
    }
    if (existingResult != null) {
      return existingResult
    }
    requireNotNull(runnableFuture)
    if (runningThreadId.compareAndSet(-1, Process.myTid())) {
      runnableFuture.run()
    }
    val newResult = getResultInheritingPriority(runnableFuture, runningThreadId.get())
    synchronized(this) {
      resolvedResult = newResult
      futureTask = null
    }
    return newResult
  }

  val isRunning: Boolean
    get() = runningThreadId.get() != -1

  val isDone: Boolean
    get() {
      val futureTask: RunnableFuture<T>?
      synchronized(this) { futureTask = this.futureTask }
      return futureTask == null || futureTask.isDone
    }

  fun cancel() {
    val futureTask: RunnableFuture<T>?
    synchronized(this) { futureTask = this.futureTask }
    futureTask?.cancel(false)
  }

  val isCanceled: Boolean
    get() {
      val futureTask: RunnableFuture<T>?
      synchronized(this) { futureTask = this.futureTask }
      return futureTask != null && futureTask.isCancelled
    }
}
