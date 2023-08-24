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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useProducer]. */
@OptIn(ExperimentalCoroutinesApi::class)
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class UseProducerTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `produces state`() {
    val stateRef = AtomicReference<Boolean>()
    val stateFlow = MutableStateFlow(false)

    class UseStateProducerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value = useProducer(initialValue = { false }, Unit) { stateFlow.collect { update(it) } }
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateProducerComponent() }
    lithoViewRule.act(lithoView) {
      // Make sure flow starts collecting, so that emissions aren't dropped.
      testDispatcher.scheduler.runCurrent()
      stateFlow.value = true
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isTrue
  }

  @Test
  fun `produces state synchronously`() {
    val stateRef = AtomicReference<Boolean>()
    val stateFlow = MutableStateFlow(false)

    class UseStateProducerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value =
            useProducer(initialValue = { false }, Unit) { stateFlow.collect { updateSync(it) } }
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateProducerComponent() }
    lithoViewRule.act(lithoView) {
      testDispatcher.scheduler.runCurrent()
      stateFlow.value = true
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isTrue
  }

  @Test
  fun `produces state updates based on current value`() {
    val stateRef = AtomicInteger()
    val sharedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    class UseStateProducerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value =
            useProducer(initialValue = { 0 }, Unit) { sharedFlow.collect { update { it + 1 } } }
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateProducerComponent() }
    lithoViewRule.act(lithoView) {
      testDispatcher.scheduler.runCurrent()
      sharedFlow.tryEmit(Unit)
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isEqualTo(1)

    lithoViewRule.act(lithoView) {
      sharedFlow.tryEmit(Unit)
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isEqualTo(2)
  }

  @Test
  fun `produces state updates synchronously based on current value`() {
    val stateRef = AtomicInteger()
    val sharedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    class UseStateProducerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value =
            useProducer(initialValue = { 0 }, Unit) { sharedFlow.collect { updateSync { it + 1 } } }
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateProducerComponent() }
    lithoViewRule.act(lithoView) {
      testDispatcher.scheduler.runCurrent()
      sharedFlow.tryEmit(Unit)
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isEqualTo(1)

    lithoView.setRoot(UseStateProducerComponent())
    lithoViewRule.act(lithoView) {
      sharedFlow.tryEmit(Unit)
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isEqualTo(2)
  }

  @Test
  fun `producer restarts on key change`() {
    val events = mutableListOf<String>()

    class UseStateCounterComponent(private val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useProducer(initialValue = { 0 }, dep) {
          events.add("produce $dep")
          try {
            awaitCancellation()
          } finally {
            events.add("cancel $dep")
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateCounterComponent(dep = 0) }
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).containsExactly("produce 0")
    events.clear()

    lithoView.setRoot(UseStateCounterComponent(dep = 0))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).isEmpty()

    lithoView.setRoot(UseStateCounterComponent(dep = 1))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).containsExactly("cancel 0", "produce 1")
  }

  @Test
  fun `restart producer when one of many keys change`() {
    val events = mutableListOf<String>()

    class UseStateCounterComponent(private val dep1: Int, private val dep2: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useProducer(initialValue = { 0 }, dep1, dep2) {
          events.add("produce $dep1:$dep2")
          try {
            awaitCancellation()
          } finally {
            events.add("cancel $dep1:$dep2")
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseStateCounterComponent(dep1 = 1, dep2 = 1) }
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).containsExactly("produce 1:1")
    events.clear()

    lithoView.setRoot(UseStateCounterComponent(dep1 = 1, dep2 = 1))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).isEmpty()
    events.clear()

    lithoView.setRoot(UseStateCounterComponent(dep1 = 100, dep2 = 1))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).containsExactly("cancel 1:1", "produce 100:1")
    events.clear()

    lithoView.setRoot(UseStateCounterComponent(dep1 = 100, dep2 = 100))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(events).containsExactly("cancel 100:1", "produce 100:100")
    events.clear()
  }
}
