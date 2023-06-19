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

import com.facebook.litho.ExecutorLithoHandlerTest.QueuingExecutor
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.ArrayDeque
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ExecutorLithoHandlerTest {

  private val executor = QueuingExecutor()
  private val lithoHandler = ExecutorLithoHandler(executor)
  private var runCounter = 0
  private val runnable = Runnable { runCounter++ }

  @Test
  fun testIsTracing_returnsFalse() {
    assertThat(lithoHandler.isTracing).isFalse
  }

  @Test
  fun testPost_multipleInstancesOfSameRunnable_runsAll() {
    lithoHandler.post(runnable, TAG)
    lithoHandler.post(runnable, TAG)
    executor.runAllQueuedTasks()
    assertThat(runCounter).isEqualTo(2)
  }

  @Test
  fun testPost_multipleRunnables_runsAll() {
    val runnable2 = Runnable { runCounter += 2 }
    lithoHandler.post(runnable, TAG)
    lithoHandler.post(runnable2, TAG)
    executor.runAllQueuedTasks()
    assertThat(runCounter).isEqualTo(3)
  }

  @Test
  fun testRemove_removesAllInstances() {
    lithoHandler.post(runnable, TAG)
    lithoHandler.post(runnable, TAG)
    lithoHandler.post(runnable, TAG)
    lithoHandler.remove(runnable)
    executor.runAllQueuedTasks()
    assertThat(runCounter).isEqualTo(0)
  }

  @Test
  fun testRemoveThenPost_runsOnce() {
    lithoHandler.post(runnable, TAG)
    lithoHandler.post(runnable, TAG)
    lithoHandler.remove(runnable)
    lithoHandler.post(runnable, TAG)
    executor.runAllQueuedTasks()
    assertThat(runCounter).isEqualTo(1)
  }

  private class QueuingExecutor : Executor {
    private val queue = ArrayDeque<Runnable>()

    override fun execute(runnable: Runnable) {
      queue.add(runnable)
    }

    fun runAllQueuedTasks() {
      while (!queue.isEmpty()) {
        val runnable = queue.removeFirst()
        runnable.run()
      }
    }
  }

  companion object {
    private const val TAG = "testTag"
  }
}
