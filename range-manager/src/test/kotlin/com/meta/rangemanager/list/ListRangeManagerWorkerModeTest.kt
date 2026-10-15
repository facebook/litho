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

package com.meta.rangemanager.list

import com.meta.rangemanager.RangeManager
import com.meta.rangemanager.WorkerMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListRangeManagerWorkerModeTest {

  @Test
  fun `asynchronous list range manager prepares on the executor it is given`() {
    val executor = Executor { runnable -> Thread(runnable, EXECUTOR_THREAD).start() }
    val preparedOn = AtomicReference<String>()
    val prepared = CountDownLatch(1)
    val item =
        TestPreparableItem(index = 0).apply {
          onPrepare = {
            preparedOn.set(Thread.currentThread().name)
            prepared.countDown()
          }
        }
    val itemProvider = TestCollectionItemProvider().apply { setItems(listOf(item)) }
    val rangeManager =
        RangeManager.ListBased(
            itemProvider = itemProvider,
            config = ListRangeConfig(behind = 0, ahead = 0),
            workerMode = WorkerMode.Async(executor),
        )

    try {
      rangeManager.recalculateRange(ListPosition(currentFirstVisible = 0, currentLastVisible = 0))

      assertThat(prepared.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
      assertThat(preparedOn.get()).isEqualTo(EXECUTOR_THREAD)
    } finally {
      rangeManager.destroy()
    }
  }

  private companion object {
    const val EXECUTOR_THREAD = "test-executor"
    const val AWAIT_SECONDS = 5L
  }
}
