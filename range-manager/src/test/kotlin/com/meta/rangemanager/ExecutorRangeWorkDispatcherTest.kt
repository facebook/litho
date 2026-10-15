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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExecutorRangeWorkDispatcherTest {

  private val executor = ThreadPerJobExecutor(THREAD_NAME)

  @Test
  fun `callbacks run one at a time in FIFO order on an executor that runs everything in parallel`() {
    val order = Collections.synchronizedList(mutableListOf<Int>())
    val runningCount = AtomicInteger(0)
    val overlapped = AtomicBoolean(false)
    val callbacksRan = CountDownLatch(20)
    val dispatcher = ExecutorRangeWorkDispatcher(executor)

    repeat(20) { index ->
      dispatcher.post {
        if (runningCount.incrementAndGet() != 1) {
          overlapped.set(true)
        }
        Thread.sleep(2)
        order.add(index)
        runningCount.decrementAndGet()
        callbacksRan.countDown()
      }
    }

    assertThat(callbacksRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    assertThat(overlapped.get()).isFalse()
    assertThat(order).containsExactlyElementsOf(0 until 20)
  }

  @Test
  fun `removePending does not revoke running work and removes queued work`() {
    val runningStarted = CountDownLatch(1)
    val releaseRunning = CountDownLatch(1)
    val runningFinished = CountDownLatch(1)
    val pendingRan = AtomicBoolean(false)
    val dispatcher = ExecutorRangeWorkDispatcher(executor)

    try {
      dispatcher.post {
        runningStarted.countDown()
        check(releaseRunning.await(AWAIT_SECONDS, TimeUnit.SECONDS))
        runningFinished.countDown()
      }
      dispatcher.post { pendingRan.set(true) }

      assertThat(runningStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      dispatcher.removePending()
      releaseRunning.countDown()

      assertThat(runningFinished.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      val probeRan = CountDownLatch(1)
      dispatcher.post { probeRan.countDown() }
      assertThat(probeRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(pendingRan.get()).isFalse()
    } finally {
      releaseRunning.countDown()
    }
  }

  @Test
  fun `quit clears pending work and rejects later work`() {
    val runningStarted = CountDownLatch(1)
    val releaseRunning = CountDownLatch(1)
    val rejectedRan = AtomicInteger(0)
    val dispatcher = ExecutorRangeWorkDispatcher(executor)

    try {
      dispatcher.post {
        runningStarted.countDown()
        check(releaseRunning.await(AWAIT_SECONDS, TimeUnit.SECONDS))
      }
      dispatcher.post { rejectedRan.incrementAndGet() }
      assertThat(runningStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()

      dispatcher.quit()
      dispatcher.quit()
      dispatcher.post { rejectedRan.incrementAndGet() }
      releaseRunning.countDown()

      executor.joinAll(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      assertThat(rejectedRan.get()).isEqualTo(0)
    } finally {
      releaseRunning.countDown()
    }
  }

  @Test
  fun `callback exception is reported and later callback still runs`() {
    val reported = AtomicReference<Exception>()
    val laterRan = CountDownLatch(1)
    val dispatcher = ExecutorRangeWorkDispatcher(executor) { exception -> reported.set(exception) }

    dispatcher.post { throw IllegalStateException("boom") }
    dispatcher.post { laterRan.countDown() }

    assertThat(laterRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    assertThat(reported.get()).isInstanceOf(IllegalStateException::class.java)
  }

  /**
   * A shut-down or saturated pool refuses the hand-off; the dispatcher must not stay "draining".
   */
  @Test
  fun `a refused hand-off is reported, the work dropped, and the next post is handed off`() {
    val reported = AtomicReference<Exception>()
    var refuse = true
    val refusing = Executor { command ->
      if (refuse) throw RejectedExecutionException("saturated") else executor.execute(command)
    }
    val dispatcher = ExecutorRangeWorkDispatcher(refusing) { reported.set(it) }
    val refusedRan = AtomicBoolean(false)
    val laterRan = CountDownLatch(1)

    val accepted = dispatcher.post { refusedRan.set(true) }
    refuse = false
    val acceptedLater = dispatcher.post { laterRan.countDown() }

    assertThat(accepted).isFalse()
    assertThat(reported.get()).isInstanceOf(RejectedExecutionException::class.java)
    assertThat(acceptedLater).isTrue()
    assertThat(laterRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    assertThat(refusedRan.get()).isFalse()
  }

  /**
   * An executor that runs the job before `execute` returns would run range work inside `post`,
   * which the async worker calls while holding its own lock.
   */
  @Test
  fun `an executor that runs work inline is reported and runs nothing`() {
    val reported = AtomicReference<Exception>()
    val dispatcher = ExecutorRangeWorkDispatcher(Executor { it.run() }) { reported.set(it) }
    val ran = AtomicBoolean(false)

    val accepted = dispatcher.post { ran.set(true) }

    assertThat(accepted).isFalse()
    assertThat(ran.get()).isFalse()
    assertThat(reported.get()).isInstanceOf(IllegalStateException::class.java)
  }

  /** An `Error` from a job ends that drain; the dispatcher must still hand off later work. */
  @Test
  fun `a job that throws an Error does not stop later work`() {
    val dispatcher = ExecutorRangeWorkDispatcher(executor)
    val laterRan = CountDownLatch(1)

    dispatcher.post { throw AssertionError("fatal in a callback") }
    executor.joinAll(AWAIT_SECONDS * 1000)
    dispatcher.post { laterRan.countDown() }

    assertThat(laterRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
  }

  /**
   * A post that lands while another thread's hand-off is being refused must not be told its work
   * will run and then have it dropped.
   */
  @Test
  fun `a post during a refused hand-off is not told its work will run`() {
    val inExecute = CountDownLatch(1)
    val otherPosted = CountDownLatch(1)
    val refusing = Executor {
      inExecute.countDown()
      // Long enough for the other thread to post while this hand-off is still undecided.
      otherPosted.await(HAND_OFF_MS, TimeUnit.MILLISECONDS)
      throw RejectedExecutionException("saturated")
    }
    val dispatcher = ExecutorRangeWorkDispatcher(refusing) {}
    val otherAccepted = AtomicReference<Boolean>()
    val other = Thread {
      inExecute.await(AWAIT_SECONDS, TimeUnit.SECONDS)
      otherAccepted.set(dispatcher.post {})
      otherPosted.countDown()
    }
    other.start()

    dispatcher.post {}
    otherPosted.countDown()
    other.join(AWAIT_SECONDS * 1000)

    assertThat(otherAccepted.get()).isFalse()
  }

  /** One job per hand-off, so the executor's own monitoring sees each preparation as a task. */
  @Test
  fun `each job is its own hand-off, in order`() {
    val handOffs = AtomicInteger(0)
    val counting = Executor { command ->
      handOffs.incrementAndGet()
      executor.execute(command)
    }
    val dispatcher = ExecutorRangeWorkDispatcher(counting)
    val order = Collections.synchronizedList(mutableListOf<Int>())
    val done = CountDownLatch(1)

    dispatcher.post {
      repeat(JOBS) { index ->
        dispatcher.post {
          order.add(index)
          if (index == JOBS - 1) done.countDown()
        }
      }
    }

    assertThat(done.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    assertThat(order).isEqualTo((0 until JOBS).toList())
    assertThat(handOffs.get()).isEqualTo(JOBS + 1)
  }

  /** Jobs already accepted must still run when the executor refuses the next hand-off. */
  @Test
  fun `a refused follow-up hand-off keeps running the accepted jobs`() {
    val calls = AtomicInteger(0)
    val acceptsFirstOnly = Executor { command ->
      if (calls.incrementAndGet() > 1) throw RejectedExecutionException("saturated")
      executor.execute(command)
    }
    val dispatcher = ExecutorRangeWorkDispatcher(acceptsFirstOnly) {}
    val ran = AtomicInteger(0)
    val done = CountDownLatch(1)

    dispatcher.post {
      repeat(JOBS) { dispatcher.post { if (ran.incrementAndGet() == JOBS) done.countDown() } }
    }

    assertThat(done.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
  }

  /**
   * An executor that runs follow-ups on the calling thread (caller-runs when saturated) must not
   * make the drain recurse: a large backlog still runs without overflowing the stack.
   */
  @Test
  fun `an executor that runs follow-ups inline keeps a large backlog iterative`() {
    val calls = AtomicInteger(0)
    val inlineAfterFirst = Executor { command ->
      if (calls.incrementAndGet() == 1) executor.execute(command) else command.run()
    }
    val dispatcher = ExecutorRangeWorkDispatcher(inlineAfterFirst) {}
    val ran = AtomicInteger(0)
    val done = CountDownLatch(1)

    dispatcher.post {
      repeat(BACKLOG) {
        dispatcher.post { if (ran.incrementAndGet() == BACKLOG) done.countDown() }
      }
    }

    assertThat(done.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
  }

  /**
   * After an Error, the jobs still queued were accepted; a later refused post must drop only its
   * own job, so they run on the next hand-off that succeeds.
   */
  @Test
  fun `a refused post after an Error keeps the jobs earlier posts were told will run`() {
    val decisions = java.util.concurrent.ConcurrentLinkedQueue(listOf(true, false, false, true))
    val scripted = Executor { command ->
      if (decisions.poll() == false) throw RejectedExecutionException("saturated")
      executor.execute(command)
    }
    val dispatcher = ExecutorRangeWorkDispatcher(scripted) {}
    val queuedRan = CountDownLatch(1)
    val acceptedQueued = AtomicReference<Boolean>()

    dispatcher.post {
      acceptedQueued.set(dispatcher.post { queuedRan.countDown() })
      throw AssertionError("fatal in a callback")
    }
    executor.joinAll(AWAIT_SECONDS * 1000)
    val refused = dispatcher.post {}
    dispatcher.post {}

    assertThat(acceptedQueued.get()).isTrue()
    assertThat(refused).isFalse()
    assertThat(queuedRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
  }

  private companion object {
    const val BACKLOG = 100_000
    const val JOBS = 50
    const val HAND_OFF_MS = 200L
    const val THREAD_NAME = "test-executor"
    const val AWAIT_SECONDS = 5L
  }
}
