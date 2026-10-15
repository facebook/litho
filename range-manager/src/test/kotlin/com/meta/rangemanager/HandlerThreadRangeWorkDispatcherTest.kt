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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HandlerThreadRangeWorkDispatcherTest {

  @Test
  fun `post lazily starts a uniquely named thread`() {
    val firstThreadName = AtomicReference<String>()
    val secondThreadName = AtomicReference<String>()
    val firstRan = CountDownLatch(1)
    val secondRan = CountDownLatch(1)
    val firstDispatcher = HandlerThreadRangeWorkDispatcher(THREAD_NAME)
    val secondDispatcher = HandlerThreadRangeWorkDispatcher(THREAD_NAME)

    try {
      firstDispatcher.post {
        firstThreadName.set(Thread.currentThread().name)
        firstRan.countDown()
      }
      secondDispatcher.post {
        secondThreadName.set(Thread.currentThread().name)
        secondRan.countDown()
      }

      assertThat(firstRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(secondRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(firstThreadName.get()).startsWith("$THREAD_NAME:")
      assertThat(secondThreadName.get()).startsWith("$THREAD_NAME:")
      assertThat(firstThreadName.get()).isNotEqualTo(secondThreadName.get())
    } finally {
      firstDispatcher.quit()
      secondDispatcher.quit()
    }
  }

  @Test
  fun `callbacks execute serially in FIFO order`() {
    val order = Collections.synchronizedList(mutableListOf<Int>())
    val runningCount = AtomicInteger(0)
    val callbacksRan = CountDownLatch(3)
    val dispatcher = HandlerThreadRangeWorkDispatcher(THREAD_NAME)

    try {
      repeat(3) { index ->
        dispatcher.post {
          assertThat(runningCount.incrementAndGet()).isEqualTo(1)
          order.add(index)
          runningCount.decrementAndGet()
          callbacksRan.countDown()
        }
      }

      assertThat(callbacksRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(order).containsExactly(0, 1, 2)
    } finally {
      dispatcher.quit()
    }
  }

  @Test
  fun `removePending does not revoke running work and removes queued work`() {
    val runningStarted = CountDownLatch(1)
    val releaseRunning = CountDownLatch(1)
    val runningFinished = CountDownLatch(1)
    val pendingRan = AtomicBoolean(false)
    val dispatcher = HandlerThreadRangeWorkDispatcher(THREAD_NAME)

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
      dispatcher.quit()
    }
  }

  @Test
  fun `quit clears pending work and rejects later work`() {
    val runningStarted = CountDownLatch(1)
    val releaseRunning = CountDownLatch(1)
    val runningFinished = CountDownLatch(1)
    val runningThread = AtomicReference<Thread>()
    val rejectedCallbacksRan = CountDownLatch(2)
    val dispatcher = HandlerThreadRangeWorkDispatcher(THREAD_NAME)

    try {
      dispatcher.post {
        runningThread.set(Thread.currentThread())
        runningStarted.countDown()
        check(releaseRunning.await(AWAIT_SECONDS, TimeUnit.SECONDS))
        runningFinished.countDown()
      }
      dispatcher.post { rejectedCallbacksRan.countDown() }
      assertThat(runningStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()

      dispatcher.quit()
      dispatcher.quit()
      dispatcher.post { rejectedCallbacksRan.countDown() }
      releaseRunning.countDown()

      assertThat(runningFinished.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      val thread = checkNotNull(runningThread.get())
      thread.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
      assertThat(thread.isAlive).isFalse()
      assertThat(rejectedCallbacksRan.count).isEqualTo(2)
    } finally {
      releaseRunning.countDown()
      dispatcher.quit()
    }
  }

  @Test
  fun `callback exception is reported and later callback still runs`() {
    val failure = IllegalStateException("expected failure")
    val reportedFailure = AtomicReference<Exception>()
    val failureReported = CountDownLatch(1)
    val laterCallbackRan = CountDownLatch(1)
    val dispatcher =
        HandlerThreadRangeWorkDispatcher(THREAD_NAME) { exception ->
          reportedFailure.set(exception)
          failureReported.countDown()
        }

    try {
      dispatcher.post { throw failure }
      dispatcher.post { laterCallbackRan.countDown() }

      assertThat(failureReported.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(reportedFailure.get()).isSameAs(failure)
      assertThat(laterCallbackRan.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    } finally {
      dispatcher.quit()
    }
  }

  private companion object {
    private const val THREAD_NAME = "RangeWorkDispatcherTest"
    private const val AWAIT_SECONDS = 5L
  }
}
