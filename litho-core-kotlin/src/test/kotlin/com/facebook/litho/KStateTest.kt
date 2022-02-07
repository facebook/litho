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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.litho.widget.Text
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Unit tests for [useState]. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class KStateTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private fun <T> ComponentScope.useCustomState(value: T): State<T> {
    val state = useState { value }
    return state
  }

  @Test
  fun useState_updateState_stateIsUpdated() {
    lateinit var stateRef: AtomicReference<String>

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val state = useState { "hello" }
        stateRef = AtomicReference(state.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  state.update("world")
                })
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    assertThat(stateRef.get()).isEqualTo("hello")

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }

    assertThat(stateRef.get()).describedAs("String state is updated").isEqualTo("world")
  }

  @Test
  fun useStateOnHooks_updateTwoStatesWithSamePropertyName_bothStatesAreUpdatedIndependently() {
    lateinit var state1Ref: AtomicReference<State<String>>
    lateinit var state2Ref: AtomicReference<State<Int>>

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val state1 = useCustomState("hello")
        val state2 = useCustomState(20)

        state1Ref = AtomicReference(state1)
        state2Ref = AtomicReference(state2)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  // The correct way to do this (at least until we have automatic batching)
                  // would be to store these states in the same obj to trigger only one state
                  // update
                  state1.update("world")
                  state2.update { value -> value + 1 }
                })
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }

    assertThat(state1Ref.get().value).isEqualTo("hello")
    assertThat(state2Ref.get().value).isEqualTo(20)
    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }

    assertThat(state1Ref.get().value).describedAs("String state is updated").isEqualTo("world")
    assertThat(state2Ref.get().value).describedAs("Int state is updated").isEqualTo(21)
  }

  @Test
  fun useState_calculateLayoutInTwoThreadsConcurrently_stateIsInitializedOnlyOnce() {
    val initCounter = AtomicInteger(0)
    val countDownLatch = CountDownLatch(2)
    val firstCountDownLatch = CountDownLatch(1)
    val secondCountDownLatch = CountDownLatch(1)

    val view = lithoViewRule.createTestLithoView()

    val thread1 = Thread {
      view.setRootAndSizeSpecSync(
          CountDownLatchComponent(firstCountDownLatch, secondCountDownLatch, initCounter),
          SizeSpec.makeSizeSpec(100, EXACTLY),
          SizeSpec.makeSizeSpec(100, EXACTLY))
      countDownLatch.countDown()
    }
    val thread2 = Thread {
      firstCountDownLatch.await()
      view.setRootAndSizeSpecSync(
          CountDownLatchComponent(secondCountDownLatch, null, initCounter),
          SizeSpec.makeSizeSpec(200, EXACTLY),
          SizeSpec.makeSizeSpec(200, EXACTLY))
      countDownLatch.countDown()
    }

    thread1.start()
    thread2.start()

    countDownLatch.await()
    lithoViewRule.idle()

    assertThat(initCounter.get()).describedAs("initCounter is initialized only once").isEqualTo(1)
    val componentTree = view.componentTree
    assertThat(componentTree.initialStateContainer?.mInitialStates)
        .describedAs("Initial hook state container is empty")
        .isEmpty()
    assertThat(componentTree.initialStateContainer?.mPendingStateHandlers)
        .describedAs("No pending StateHandlers")
        .isEmpty()
  }

  @Test
  fun useState_counterIncrementedTwiceBeforeStateCommit_bothIncrementsAreApplied() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val counter = useState { 0 }

        return Row(
            style = Style.viewTag("test_view").onClick { counter.update { value -> value + 1 } }) {
          child(
              Text(
                  style = Style.viewTag("Counter: ${counter.value}"),
                  text = "Counter: ${counter.value}"))
        }
      }
    }

    val view =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { TestComponent() }

    lithoViewRule.act(view) {
      clickOnTag("test_view")
      clickOnTag("test_view")
    }

    assertThat(view.findViewWithTagOrNull("Counter: 2")).isNotNull()
  }

  @Test
  fun useState_synchronousUpdate_stateIsUpdatedSynchronously() {
    lateinit var stateRef: AtomicReference<String>

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val state = useState { "hello" }
        stateRef = AtomicReference(state.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  state.updateSync("world")
                })
      }
    }

    val view =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { TestComponent() }

    assertThat(stateRef.get()).isEqualTo("hello")
    lithoViewRule.act(view) { clickOnTag("test_view") }

    assertThat(stateRef.get()).describedAs("String state is updated").isEqualTo("world")
  }

  @Test
  fun useState_reconciliation_stateIsUpdatedWithoutCallingRenderOnSibling() {
    val siblingRenderCount = AtomicInteger()

    class RootComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.wrapInView()) {
          child(ClickableComponentWithState(tag = "test_view"))
          child(CountRendersComponent(renderCount = siblingRenderCount))
        }
      }
    }

    val view =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { RootComponent() }

    assertThat(siblingRenderCount.get()).isEqualTo(1)

    lithoViewRule.act(view) { clickOnTag("test_view") }

    // Using viewTag because Text is currently a drawable and harder to access directly
    assertThat(view.findViewWithTagOrNull("Counter: 1")).isNotNull()

    // Assert that the state update didn't cause the sibling to re-render
    assertThat(siblingRenderCount.get()).isEqualTo(1)
  }

  /**
   * While it's not exactly desired that the parent needs render() called on it again, this test is
   * to detect unexpected changes in that behavior.
   */
  @Test
  fun useState_reconciliation_renderCalledOnParentOfUpdatedComponent() {
    val siblingRenderCount = AtomicInteger()
    val parentRenderCount = AtomicInteger()

    class ParentOfComponentWithStateUpdate(private val renderCount: AtomicInteger) : KComponent() {
      override fun ComponentScope.render(): Component {
        renderCount.incrementAndGet()
        return ClickableComponentWithState(tag = "test_view")
      }
    }

    class RootComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.wrapInView()) {
          child(ParentOfComponentWithStateUpdate(renderCount = parentRenderCount))
          child(CountRendersComponent(renderCount = siblingRenderCount))
        }
      }
    }

    val view =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { RootComponent() }

    assertThat(parentRenderCount.get()).isEqualTo(1)
    assertThat(siblingRenderCount.get()).isEqualTo(1)

    lithoViewRule.act(view) { clickOnTag("test_view") }

    // Assert that the state update still causes parent to re-render but not sibling
    assertThat(view.findViewWithTagOrNull("Counter: 1")).isNotNull()
    assertThat(parentRenderCount.get()).isEqualTo(2)
    assertThat(siblingRenderCount.get()).isEqualTo(1)
  }

  private class CountDownLatchComponent(
      val countDownLatch: CountDownLatch,
      val awaitable: CountDownLatch?,
      val initCounter: AtomicInteger
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      countDownLatch.countDown()
      awaitable?.await()

      val state = useState { initCounter.incrementAndGet() }
      return Text("stateValue is ${state.value}")
    }
  }

  class ClickableComponentWithState(private val tag: String) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val counter = useState { 0 }

      return Row(style = Style.viewTag(tag).onClick { counter.updateSync { value -> value + 1 } }) {
        child(
            Text(
                style = Style.viewTag("Counter: ${counter.value}"),
                text = "Counter: ${counter.value}"))
      }
    }
  }

  private class CountRendersComponent(private val renderCount: AtomicInteger) : KComponent() {
    override fun ComponentScope.render(): Component? {
      renderCount.incrementAndGet()
      return Row(style = Style.width(100.px).height(100.px))
    }
  }
}
