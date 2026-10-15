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

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyBasedWorkerFactoryTest {

  @Test
  fun `key based worker defaults to synchronous`() {
    val worker = RangeManager.keyBasedWorker(TestStringKeyItemProvider())

    assertThat(worker).isInstanceOf(KeyBasedRangeWorker::class.java)

    worker.destroy()
  }

  @Test
  fun `key based worker selects asynchronous explicitly`() {
    val worker =
        RangeManager.keyBasedWorker(
            itemProvider = TestStringKeyItemProvider(),
            workerMode = WorkerMode.Async(),
        )

    assertThat(worker).isInstanceOf(AsynchronousKeyBasedRangeWorker::class.java)

    worker.destroy()
  }

  @Test
  fun `asynchronous worker prepares on the executor it is given`() {
    val executor = Executor { runnable -> Thread(runnable, EXECUTOR_THREAD).start() }
    val preparedOn = AtomicReference<String>()
    val prepared = CountDownLatch(1)
    val itemProvider = TestStringKeyItemProvider()
    itemProvider.setItems(
        mapOf(
            "A" to
                TestStringKeyItem(
                    "A",
                    onPrepare = {
                      preparedOn.set(Thread.currentThread().name)
                      prepared.countDown()
                    },
                ),
        ),
    )
    val worker =
        RangeManager.keyBasedWorker(
            itemProvider = itemProvider,
            workerMode = WorkerMode.Async(executor),
        )

    try {
      worker.schedule(Unit, TestStringKeyRange(listOf("A")))

      assertThat(prepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(preparedOn.get()).isEqualTo(EXECUTOR_THREAD)
    } finally {
      worker.destroy()
    }
  }

  @Test
  fun `asynchronous worker without an executor prepares on its own thread`() {
    val preparedOn = AtomicReference<String>()
    val prepared = CountDownLatch(1)
    val itemProvider = TestStringKeyItemProvider()
    itemProvider.setItems(
        mapOf(
            "A" to
                TestStringKeyItem(
                    "A",
                    onPrepare = {
                      preparedOn.set(Thread.currentThread().name)
                      prepared.countDown()
                    },
                ),
        ),
    )
    val worker =
        RangeManager.keyBasedWorker(itemProvider = itemProvider, workerMode = WorkerMode.Async())

    try {
      worker.schedule(Unit, TestStringKeyRange(listOf("A")))

      assertThat(prepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(preparedOn.get()).startsWith("AsynchronousKeyBasedRangeWorker:")
    } finally {
      worker.destroy()
    }
  }

  @Test
  fun `asynchronous worker sends a failing prepare to onFailure and prepares the next item`() {
    val executor = Executor { runnable -> Thread(runnable, EXECUTOR_THREAD).start() }
    val failures = CopyOnWriteArrayList<Exception>()
    val bPrepared = CountDownLatch(1)
    val itemProvider = TestStringKeyItemProvider()
    itemProvider.setItems(
        mapOf(
            "A" to TestStringKeyItem("A", onPrepare = { throw IllegalStateException("boom A") }),
            "B" to TestStringKeyItem("B", onPrepare = { bPrepared.countDown() }),
        ),
    )
    val worker =
        RangeManager.keyBasedWorker(
            itemProvider = itemProvider,
            workerMode = WorkerMode.Async(executor, onFailure = { failures.add(it) }),
        )

    try {
      worker.prepareKey("A")
      worker.prepareKey("B")

      assertThat(bPrepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(failures.map { it.message }).containsExactly("boom A")
    } finally {
      worker.destroy()
    }
  }

  private companion object {
    const val EXECUTOR_THREAD = "test-executor"
    const val AWAIT_SECONDS = 5L
  }
}
