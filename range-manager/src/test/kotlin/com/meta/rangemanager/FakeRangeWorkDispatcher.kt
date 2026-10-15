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

package com.meta.rangemanager

import java.util.ArrayDeque

internal class FakeRangeWorkDispatcher : RangeWorkDispatcher {
  private val lock = Any()
  private val queuedWork = ArrayDeque<() -> Unit>()
  private val dequeuedWork = ArrayDeque<() -> Unit>()

  @Volatile private var runningThread: Thread? = null

  val isRunningOnCurrentThread: Boolean
    get() = runningThread === Thread.currentThread()

  @Volatile
  var quitCalled: Boolean = false
    private set

  /** While false, [post] refuses work, like an executor that rejects it. */
  @Volatile var acceptsWork: Boolean = true

  val queuedCount: Int
    get() = synchronized(lock) { queuedWork.size }

  val dequeuedCount: Int
    get() = synchronized(lock) { dequeuedWork.size }

  override fun post(work: () -> Unit): Boolean {
    synchronized(lock) {
      if (quitCalled || !acceptsWork) {
        return false
      }
      queuedWork.addLast(work)
      return true
    }
  }

  override fun removePending() {
    synchronized(lock) { queuedWork.clear() }
  }

  override fun quit() {
    synchronized(lock) {
      quitCalled = true
      queuedWork.clear()
    }
  }

  fun dequeueOne(): Boolean =
      synchronized(lock) {
        val work = queuedWork.pollFirst() ?: return@synchronized false
        dequeuedWork.addLast(work)
        true
      }

  fun runDequeued(): Boolean {
    val work = synchronized(lock) { dequeuedWork.pollFirst() } ?: return false
    run(work)
    return true
  }

  fun runOne(): Boolean {
    val work =
        synchronized(lock) {
          if (dequeuedWork.isEmpty()) {
            queuedWork.pollFirst()?.let { dequeuedWork.addLast(it) }
          }
          dequeuedWork.pollFirst()
        } ?: return false
    run(work)
    return true
  }

  fun drain() {
    while (runOne()) {
      // A callback can enqueue the next unit of work.
    }
  }

  private fun run(work: () -> Unit) {
    runningThread = Thread.currentThread()
    try {
      work()
    } finally {
      runningThread = null
    }
  }
}
