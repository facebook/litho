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
import java.util.concurrent.RejectedExecutionException

/**
 * Runs range work on a caller-supplied [Executor], one job at a time and in order, whatever the
 * executor's own threading.
 *
 * Jobs wait here, and each is handed to the executor as its own task once the previous one
 * finishes, so an app's executor monitoring (task counts, long-running detection) sees every
 * preparation. If the executor refuses a follow-up hand-off, or runs it on the calling thread, the
 * current task keeps running the queue in a loop: those jobs were already accepted, and the loop
 * never recurses. [removePending] drops the jobs still waiting, and one already running still
 * finishes. [quit] stops accepting work but does not shut the executor down, since the caller owns
 * it.
 *
 * Hand-offs (`execute`) happen under the dispatcher's lock, so an executor that blocks in `execute`
 * delays other posts until it returns. A [post] that starts a hand-off can fail in two ways, and
 * both are reported: its job is dropped (jobs accepted earlier stay queued), [post] returns false,
 * and the next [post] hands off again.
 * - The executor rejects it (shut down or saturated).
 * - The executor runs it on the posting thread before `execute` returns (a direct executor, or a
 *   caller-runs policy). Running it would invoke range work inside [post], which the async worker
 *   calls while holding its own lock, so the job is not run. An executor that posts to the posting
 *   thread's own looper is not detected and runs the job there later.
 */
internal class ExecutorRangeWorkDispatcher(
    private val executor: Executor,
    private val failureReporter: (Exception) -> Unit = logRangeWorkFailure,
) : RangeWorkDispatcher {
  private val lock = Any()
  private val pending = ArrayDeque<() -> Unit>()
  private var isDraining = false
  private var isQuit = false

  override fun post(work: () -> Unit): Boolean {
    val failure: Exception
    synchronized(lock) {
      if (isQuit) {
        return false
      }
      pending.addLast(work)
      if (isDraining) {
        return true
      }
      // Handed off under the lock, so no other post sees "draining" before the hand-off succeeds
      // and is told its work will run when it won't. The task itself waits for the lock.
      val handOffFailure = handOffLocked()
      if (handOffFailure == null) {
        isDraining = true
        return true
      }
      // Only this post's job: jobs accepted before it (queued after an Error) wait for the next
      // one.
      pending.removeLast()
      failure = handOffFailure
    }
    failureReporter(failure)
    return false
  }

  override fun removePending() {
    synchronized(lock) { pending.clear() }
  }

  override fun quit() {
    synchronized(lock) {
      isQuit = true
      pending.clear()
    }
  }

  /** Hands a new [Task] to the executor, and returns why it didn't take it, or null if it did. */
  private fun handOffLocked(): Exception? {
    val task = Task(submitter = Thread.currentThread())
    return try {
      executor.execute(task)
      task.handedOff = true
      if (task.ranInline) {
        IllegalStateException(
            "The executor ran range work on the posting thread; RangeManager needs one that runs " +
                "it on another thread",
        )
      } else {
        null
      }
    } catch (rejected: RejectedExecutionException) {
      rejected
    }
  }

  /**
   * Passes the queue on to a new task, or ends draining if nothing is queued. False if the executor
   * did not take it, in which case the caller still owns the queue.
   */
  private fun handOffNext(): Boolean =
      synchronized(lock) {
        if (pending.isEmpty()) {
          isDraining = false
          return true
        }
        handOffLocked() == null
      }

  /** Runs one queued job, then passes the queue on; keeps running it here if it can't. */
  private inner class Task(private val submitter: Thread) : Runnable {
    @Volatile var handedOff = false
    @Volatile var ranInline = false

    override fun run() {
      if (!handedOff && Thread.currentThread() === submitter) {
        ranInline = true
        return
      }
      while (true) {
        val work =
            synchronized(lock) {
              pending.removeFirstOrNull()
                  ?: run {
                    isDraining = false
                    return
                  }
            }
        try {
          work()
        } catch (exception: Exception) {
          failureReporter(exception)
        } catch (error: Throwable) {
          // Only an Error gets here. Pass the queue on if the executor takes it, else free the
          // dispatcher so the next post hands off again.
          if (!handOffNext()) {
            synchronized(lock) { isDraining = false }
          }
          throw error
        }
        if (handOffNext()) {
          return
        }
      }
    }
  }
}

/**
 * The dispatcher for this mode: its [WorkerMode.Async.executor], or a thread named [threadName].
 */
internal fun WorkerMode.Async.rangeWorkDispatcher(threadName: String): RangeWorkDispatcher {
  val failureReporter = onFailure ?: logRangeWorkFailure
  val executor = executor ?: return HandlerThreadRangeWorkDispatcher(threadName, failureReporter)
  return ExecutorRangeWorkDispatcher(executor, failureReporter)
}
