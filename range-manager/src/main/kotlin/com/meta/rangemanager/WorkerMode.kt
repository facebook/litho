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

import java.util.concurrent.Executor

/** How the range worker runs preparation work. */
sealed interface WorkerMode {

  /**
   * On the calling thread.
   *
   * Use when the caller manages threading itself, the work is light, or preparation has to be
   * finished before the call returns, as in tests.
   */
  data object Sync : WorkerMode

  /**
   * In the background, one job at a time.
   *
   * @param executor where the work runs, so an app can keep it on its own monitored pool. Each job
   *   is its own task, handed over after the previous one finishes, so a multi-threaded pool is
   *   fine. It must run work on another thread: a job it rejects, or runs on the posting thread
   *   before `execute` returns (a direct executor), is reported and not run. Null starts a
   *   dedicated background thread.
   * @param onFailure receives the exception when a prepare or release throws, so an app can send it
   *   to its own error reporting. The worker carries on with the next job either way. Null logs it.
   */
  class Async(
      val executor: Executor? = null,
      val onFailure: ((Exception) -> Unit)? = null,
  ) : WorkerMode
}
