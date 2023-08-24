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

import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [useCoroutine]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(LithoTestRunner::class)
class UseCoroutineTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `coroutine is launched and canceled`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        useCoroutine(Unit) {
          useCoroutineCalls += "launch"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "cancel"
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineComponent() }
    testDispatcher.scheduler.runCurrent()
    lithoView.detachFromWindow().release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("launch", "cancel")
  }

  @Test
  fun `coroutine restarted on each update`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent(val seq: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useCoroutine(Any()) {
          useCoroutineCalls += "attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "detach $seq"
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineComponent(seq = 0) }
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("attach 0")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(seq = 1))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 0", "attach 1")
    useCoroutineCalls.clear()

    lithoView.release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 1")
  }

  @Test
  fun `coroutine only restarted if dependency changes`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent(val dep: Int, val seq: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useCoroutine(dep) {
          useCoroutineCalls += "attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "detach $seq"
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineComponent(dep = 0, seq = 0) }
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("attach 0")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep = 0, seq = 1))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).isEmpty()
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep = 1, seq = 2))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 0", "attach 2")
    useCoroutineCalls.clear()

    lithoView.release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 2")
  }

  @Test
  fun `coroutine only restarts if nullable dependency changes`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent(val dep: Int?, val seq: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useCoroutine(dep) {
          useCoroutineCalls += "attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "detach $seq"
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineComponent(dep = null, seq = 0) }
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("attach 0")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep = null, seq = 1))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).isEmpty()
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep = 1, seq = 2))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 0", "attach 2")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep = null, seq = 3))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 2", "attach 3")
    useCoroutineCalls.clear()

    lithoView.release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 3")
  }

  @Test
  fun `coroutine only restarts if any dependency changes`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent(
        val dep1: Int,
        val dep2: Int,
        val seq: Int,
    ) : KComponent() {
      override fun ComponentScope.render(): Component {
        useCoroutine(dep1, dep2) {
          useCoroutineCalls += "attach $dep1:$dep2:$seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "detach $dep1:$dep2:$seq"
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCoroutineComponent(dep1 = 0, dep2 = 0, seq = 0) }
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("attach 0:0:0")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 0, dep2 = 0, seq = 1))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).isEmpty()
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 0, dep2 = 1, seq = 2))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 0:0:0", "attach 0:1:2")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 1, dep2 = 1, seq = 3))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 0:1:2", "attach 1:1:3")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 2, dep2 = 2, seq = 4))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 1:1:3", "attach 2:2:4")
    useCoroutineCalls.clear()

    lithoView.release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("detach 2:2:4")
  }

  @Test
  fun `multiple coroutines are launched independently`() {
    val useCoroutineCalls = mutableListOf<String>()

    class UseCoroutineComponent(val dep1: Int, val dep2: Int, val dep3: Int, val seq: Int) :
        KComponent() {
      override fun ComponentScope.render(): Component? {
        useCoroutine(dep1) {
          useCoroutineCalls += "dep1: attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "dep1: detach $seq"
          }
        }
        useCoroutine(dep2) {
          useCoroutineCalls += "dep2: attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "dep2: detach $seq"
          }
        }
        useCoroutine(dep3) {
          useCoroutineCalls += "dep3: attach $seq"
          try {
            awaitCancellation()
          } finally {
            useCoroutineCalls += "dep3: detach $seq"
          }
        }
        return Row()
      }
    }

    val lithoView =
        lithoViewRule.render { UseCoroutineComponent(dep1 = 0, dep2 = 0, dep3 = 0, seq = 0) }
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls)
        .containsExactly("dep1: attach 0", "dep2: attach 0", "dep3: attach 0")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 0, dep2 = 1, dep3 = 0, seq = 1))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).containsExactly("dep2: detach 0", "dep2: attach 1")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 1, dep2 = 1, dep3 = 1, seq = 2))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls)
        .containsExactly("dep1: detach 0", "dep1: attach 2", "dep3: detach 0", "dep3: attach 2")
    useCoroutineCalls.clear()

    lithoView.setRoot(UseCoroutineComponent(dep1 = 1, dep2 = 1, dep3 = 1, seq = 3))
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls).isEmpty()
    useCoroutineCalls.clear()

    lithoView.release()
    testDispatcher.scheduler.runCurrent()

    assertThat(useCoroutineCalls)
        .containsExactly("dep1: detach 2", "dep2: detach 1", "dep3: detach 2")
  }
}
