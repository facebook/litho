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

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useFlow]. */
@OptIn(ExperimentalCoroutinesApi::class)
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class UseFlowTest {

  @Rule @JvmField val lithoViewRule = LithoTestRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `collect with a StateFlow`() {
    val stateRef = AtomicReference<Boolean>()
    val testFlow = MutableStateFlow(false)

    class UseCollectAsStateComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value = useFlow(testFlow)
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCollectAsStateComponent() }
    assertThat(stateRef.get()).isFalse

    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(stateRef.get()).isFalse

    lithoViewRule.act(lithoView) {
      testFlow.value = true
      testDispatcher.scheduler.runCurrent()
    }

    assertThat(stateRef.get()).isTrue
  }

  @Test
  fun `collect flow block`() {
    val stateRef = AtomicInteger()

    class UseCollectAsStateComponent(val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        val value = useFlow(initialValue = { 0 }, Any()) { MutableStateFlow(dep) }
        stateRef.set(value)
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCollectAsStateComponent(1) }

    assertThat(stateRef.get()).isEqualTo(0)

    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(stateRef.get()).isEqualTo(1)

    lithoView.setRoot(UseCollectAsStateComponent(2))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(stateRef.get()).isEqualTo(2)
  }

  @Test
  fun `collect flow block only rerun on dependency change`() {
    val flowEvents = mutableListOf<String>()

    class UseCollectAsStateComponent(private val dep: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useFlow(initialValue = { 0 }, dep) {
          flowEvents.add("launch $dep")
          try {
            awaitCancellation()
          } finally {
            flowEvents.add("cancel $dep")
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCollectAsStateComponent(1) }

    assertThat(flowEvents).isEmpty()

    testDispatcher.scheduler.runCurrent()
    lithoViewRule.idle()

    assertThat(flowEvents).containsExactly("launch 1")

    lithoView.setRoot(UseCollectAsStateComponent(1))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(flowEvents).containsExactly("launch 1")

    lithoView.setRoot(UseCollectAsStateComponent(2))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(flowEvents).containsExactly("launch 1", "cancel 1", "launch 2")
  }

  @Test
  fun `collect flow block only rerun on one of many dependencies change`() {
    val flowEvents = mutableListOf<String>()

    class UseCollectAsStateComponent(private val dep1: Int, private val dep2: Int) : KComponent() {
      override fun ComponentScope.render(): Component {
        useFlow(initialValue = { 0 }, dep1, dep2) {
          flowEvents.add("launch $dep1:$dep2")
          try {
            awaitCancellation()
          } finally {
            flowEvents.add("cancel $dep1:$dep2")
          }
        }
        return Row()
      }
    }

    val lithoView = lithoViewRule.render { UseCollectAsStateComponent(1, 1) }

    assertThat(flowEvents).isEmpty()

    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(flowEvents).containsExactly("launch 1:1")
    flowEvents.clear()

    lithoView.setRoot(UseCollectAsStateComponent(1, 1))
    lithoViewRule.act(lithoView) { lithoViewRule.idle() }

    assertThat(flowEvents).isEmpty()

    lithoView.setRoot(UseCollectAsStateComponent(2, 1))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(flowEvents).containsExactly("cancel 1:1", "launch 2:1")
    flowEvents.clear()

    lithoView.setRoot(UseCollectAsStateComponent(2, 2))
    lithoViewRule.act(lithoView) { testDispatcher.scheduler.runCurrent() }

    assertThat(flowEvents).containsExactly("cancel 2:1", "launch 2:2")
    flowEvents.clear()
  }

  @Test
  fun `collect with a StateFlow observes last value even if it has been emitted before subscription`() {
    val stateRef = AtomicReference<Boolean>()
    val testFlow = MutableStateFlow(false)

    val flowUpdatedLatch = CountDownLatch(1)

    class UseCollectAsStateComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val value = useFlow(testFlow)
        if (!flowUpdatedLatch.await(300, TimeUnit.MILLISECONDS)) {
          throw TimeoutException()
        }
        stateRef.set(value)
        return Row()
      }
    }

    val componentTree = ComponentTree.create(lithoViewRule.context).build()

    val lithoView =
        lithoViewRule
            .createTestLithoView(componentTree = componentTree)
            .attachToWindow()
            .measure()
            .layout()

    componentTree.setRootAndSizeSpecAsync(
        UseCollectAsStateComponent(), unspecified(), unspecified())

    assertThat(testFlow.subscriptionCount.value).isEqualTo(0)

    testFlow.value = true
    flowUpdatedLatch.countDown()

    lithoViewRule.idle()

    assertThat(stateRef.get()).isTrue
  }
}
