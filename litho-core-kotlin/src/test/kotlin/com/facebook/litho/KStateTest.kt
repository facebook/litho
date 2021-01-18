/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import android.view.View.MeasureSpec.EXACTLY
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [useState]. */
@RunWith(LithoTestRunner::class)
class KStateTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  private fun <T> DslScope.useCustomState(value: T): State<T> {
    val state by useState { value }
    return state
  }

  @Test
  fun useState_updateState_stateIsUpdated() {
    lateinit var stateRef: AtomicReference<State<String>>
    lateinit var row: Row

    val root = KComponent {
      val state by useState { "hello" }
      stateRef = AtomicReference(state)

      row = Row(style = Style.onClick { updateState { state.value = "world" } })
      row
    }
    lithoViewRule.setRoot(root)
    lithoViewRule.attachToWindow().measure().layout()

    assertThat(stateRef.get().value).isEqualTo("hello")

    // Simulate clicking by dispatching a click event.
    EventDispatcherUtils.dispatchOnClick(row.commonProps!!.clickHandler, lithoViewRule.lithoView)
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    assertThat(stateRef.get().value).describedAs("String state is updated").isEqualTo("world")
  }

  @Test
  fun useStateOnHooks_updateState_stateIsUpdated() {
    ComponentsConfiguration.isHooksImplEnabled = true

    useState_updateState_stateIsUpdated()

    ComponentsConfiguration.isHooksImplEnabled = false
  }

  @Test
  fun useStateOnHooks_updateTwoStatesWithSamePropertyName_bothStatesAreUpdatedIndependently() {
    ComponentsConfiguration.isHooksImplEnabled = true

    lateinit var state1Ref: AtomicReference<State<String>>
    lateinit var state2Ref: AtomicReference<State<Int>>
    lateinit var row: Row

    val root = KComponent {
      val state1 = useCustomState("hello")
      val state2 = useCustomState(20)

      state1Ref = AtomicReference(state1)
      state2Ref = AtomicReference(state2)

      row =
          Row(
              style =
                  Style.onClick {
                    updateState {
                      state1.value = "world"
                      state2.value++
                    }
                  })
      row
    }
    lithoViewRule.setRoot(root)
    lithoViewRule.attachToWindow().measure().layout()

    assertThat(state1Ref.get().value).isEqualTo("hello")
    assertThat(state2Ref.get().value).isEqualTo(20)

    // Simulate clicking by dispatching a click event.
    EventDispatcherUtils.dispatchOnClick(row.commonProps!!.clickHandler, lithoViewRule.lithoView)
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    assertThat(state1Ref.get().value).describedAs("String state is updated").isEqualTo("world")
    assertThat(state2Ref.get().value).describedAs("Int state is updated").isEqualTo(21)

    ComponentsConfiguration.isHooksImplEnabled = false
  }

  @Test
  fun useState_calculateLayoutInTwoThreadsConcurrently_stateIsInitializedOnlyOnce() {
    val initCounter = AtomicInteger(0)
    val countDownLatch = CountDownLatch(2)
    val firstCountDownLatch = CountDownLatch(1)
    val secondCountDownLatch = CountDownLatch(1)

    val thread1 = Thread {
      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(firstCountDownLatch, secondCountDownLatch, initCounter),
          SizeSpec.makeSizeSpec(100, EXACTLY),
          SizeSpec.makeSizeSpec(100, EXACTLY))
      countDownLatch.countDown()
    }
    val thread2 = Thread {
      firstCountDownLatch.await()

      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(secondCountDownLatch, null, initCounter),
          SizeSpec.makeSizeSpec(200, EXACTLY),
          SizeSpec.makeSizeSpec(200, EXACTLY))
      countDownLatch.countDown()
    }
    thread1.start()
    thread2.start()
    countDownLatch.await()

    assertThat(initCounter.get()).describedAs("initCounter is initialized only once").isEqualTo(1)
    val componentTree = lithoViewRule.componentTree
    assertThat(componentTree.initialStateContainer.mInitialHookStates)
        .describedAs("Initial hook state container is empty")
        .isEmpty()
    assertThat(componentTree.initialStateContainer.mPendingStateHandlers)
        .describedAs("No pending StateHandlers")
        .isEmpty()
  }

  @Test
  fun useStateOnHooks_calculateComponentInTwoThreadsConcurrently_stateIsInitializedOnlyOnce() {
    ComponentsConfiguration.isHooksImplEnabled = true

    val initCounter = AtomicInteger(0)
    val countDownLatch = CountDownLatch(2)
    val firstCountDownLatch = CountDownLatch(1)
    val secondCountDownLatch = CountDownLatch(1)

    val thread1 = Thread {
      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(firstCountDownLatch, secondCountDownLatch, initCounter),
          SizeSpec.makeSizeSpec(100, EXACTLY),
          SizeSpec.makeSizeSpec(100, EXACTLY))
      countDownLatch.countDown()
    }
    val thread2 = Thread {
      firstCountDownLatch.await()

      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(secondCountDownLatch, null, initCounter),
          SizeSpec.makeSizeSpec(200, EXACTLY),
          SizeSpec.makeSizeSpec(200, EXACTLY))
      countDownLatch.countDown()
    }
    thread1.start()
    thread2.start()
    countDownLatch.await()

    assertThat(initCounter.get()).describedAs("initCounter is initialized only once").isEqualTo(1)
    val componentTree = lithoViewRule.componentTree
    assertThat(componentTree.initialStateContainer.mInitialHookStates)
        .describedAs("Initial hook state container is empty")
        .isEmpty()
    assertThat(componentTree.initialStateContainer.mPendingHooksHandlers)
        .describedAs("No pending HooksHandlers")
        .isEmpty()

    ComponentsConfiguration.isHooksImplEnabled = false
  }

  private class CountDownLatchComponent(
      countDownLatch: CountDownLatch,
      awaitable: CountDownLatch?,
      initCounter: AtomicInteger
  ) :
      KComponent({
        countDownLatch.countDown()
        awaitable?.await()

        val state by useState { initCounter.incrementAndGet() }
        Text("stateValue is ${state.value}")
      })
}
