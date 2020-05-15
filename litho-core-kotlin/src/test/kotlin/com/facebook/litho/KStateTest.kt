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
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Unit tests for KState. */
@RunWith(LithoTestRunner::class)
class KStateTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun useState_updateState_stateIsUpdated() {
    lateinit var stateRef: AtomicReference<State<String>>
    lateinit var row: Row

    val root = KComponent {
      val state by useState { "hello" }
      stateRef = AtomicReference(state)

      Clickable(onClick = {
        updateState {
          state.value = "world"
        }
      }) {
        row = Row()
        row
      }
    }
    lithoViewRule.setRoot(root)
    lithoViewRule.attachToWindow().measure().layout()

    assertThat(stateRef.get().value).isEqualTo("hello")

    // Simulate clicking by dispatching a click event.
    EventDispatcherUtils.dispatchOnClick(row.commonProps!!.clickHandler, lithoViewRule.lithoView)
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    assertThat(stateRef.get().value)
        .describedAs("State is updated")
        .isEqualTo("world")
  }

  @Test
  fun useState_calculateLayoutInTwoThreadsConcurrently_stateIsInitializedOnlyOnce() {
    val initCounter = AtomicInteger(0)
    val countDownLatch = CountDownLatch(2)
    val firstCountDownLatch = CountDownLatch(1)
    val secondCountDownLatch = CountDownLatch(1)

    val thread1 = Thread {
      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(
              firstCountDownLatch,
              secondCountDownLatch,
              initCounter),
          SizeSpec.makeSizeSpec(100, EXACTLY),
          SizeSpec.makeSizeSpec(100, EXACTLY))
      countDownLatch.countDown()
    }
    val thread2 = Thread {
      firstCountDownLatch.await()

      lithoViewRule.setRootAndSizeSpec(
          CountDownLatchComponent(
              secondCountDownLatch,
              null,
              initCounter),
          SizeSpec.makeSizeSpec(200, EXACTLY),
          SizeSpec.makeSizeSpec(200, EXACTLY))
      countDownLatch.countDown()
    }
    thread1.start()
    thread2.start()
    countDownLatch.await()

    assertThat(initCounter.get())
        .describedAs("initCounter is initialized only once")
        .isEqualTo(1)
    val componentTree = lithoViewRule.componentTree
    assertThat(componentTree.initialStateContainer.mInitialHookStates)
        .describedAs("Initial hook state container is empty")
        .isEmpty()
    assertThat(componentTree.initialStateContainer.mPendingStateHandlers)
        .describedAs("No pending StateHandlers")
        .isEmpty()
  }

  private class CountDownLatchComponent(
      countDownLatch: CountDownLatch,
      awaitable: CountDownLatch?,
      initCounter: AtomicInteger
  ) : KComponent({
    countDownLatch.countDown()
    awaitable?.await()

    val state by useState {
      initCounter.incrementAndGet()
    }
    Text("stateValue is ${state.value}")
  })
}
