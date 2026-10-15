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

import java.util.Collections
import java.util.concurrent.Executor

/**
 * Starts a new named thread for every job, so nothing it runs is serialized by the executor, and
 * remembers the threads so a test can wait for all of them.
 */
internal class ThreadPerJobExecutor(private val threadName: String) : Executor {
  val threads: MutableList<Thread> = Collections.synchronizedList(mutableListOf())

  override fun execute(command: Runnable) {
    Thread(command, threadName).also(threads::add).start()
  }

  fun joinAll(timeoutMillis: Long) {
    synchronized(threads) { threads.toList() }.forEach { it.join(timeoutMillis) }
  }
}
