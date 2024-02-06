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

import android.widget.TextView
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/** Unit tests for [useState]. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class KBatchedStateUpdatesTest {

  @get:Rule val lithoViewRule = LithoViewRule()

  @get:Rule val expectedException = ExpectedException.none()

  @get:Rule
  val backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun stateUpdates_numberOfRendersTriggeredCorrectly() {
    val renderCount = AtomicInteger(0)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  // have to block the main looper while we are queueing updates to make
                  // sure that if any scheduling is posted to the main thread, it only
                  // happens after the work inside the onClick finishes.
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                  }
                }) {
              child(Text(text = "Counter: ${counter.value}"))
            }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 2")

    /* initial render + render triggered by batched updates */
    val expectedRenderCount = 2
    assertThat(renderCount.get()).isEqualTo(expectedRenderCount)
  }

  /**
   * In this test we:
   * 1. Run two async updates (which are being batched when batching is enabled).
   * 2. Run on sync update. This sync update will end up consuming the currently batched updates and
   *    trigger one layout calculation.
   * 3. Then we start another batching with two async updates, which will trigger another layout
   *    calculation.
   *
   * The goal of this test is that the batching mechanism still works correctly, even when
   * interrupted by another sync update.
   */
  @Test
  fun stateUpdates_concurrent_sync_update_consumes_pending_state_updates_and_batching_can_be_resumed() {
    val renderCount = AtomicInteger(0)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }

                    /* this sync should consume all pending batched updates when batching is enabled */
                    executeAndRunPendingBgTasks { counter.updateSync { it + 1 } }
                  }

                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                  }
                }) {
              child(Text(text = "Counter: ${counter.value}"))
            }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 5")
    /* initial render + render triggered by sync update (which interrupts batching) + render triggered by async update */
    val expectedCount = 3

    assertThat(renderCount.get()).isEqualTo(expectedCount)
  }

  /**
   * In this test we:
   * 1. Execute two async updates (which are being batched when batching is enabled)
   * 2. Execute one sync update. This sync update actually ends up on consuming the already enqueued
   *    updates.
   * 3. In this scenario we only have one layout calculation (besides the initial one).
   *
   * This test guarantees that we don't do any extra layout calculation due to the batching.
   */
  @Test
  fun stateUpdates_concurrent_sync_update_consumes_all_updates_and_consumes_all_batched_updates() {
    val renderCount = AtomicInteger(0)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }

                    /* this sync should consume all pending batched updates when batching is enabled */
                    executeAndRunPendingBgTasks { counter.updateSync { it + 1 } }
                  }
                }) {
              child(Text(text = "Counter: ${counter.value}"))
            }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    executeAndRunPendingBgTasks { lithoViewRule.act(testLithoView) { clickOnTag("test_view") } }

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 3")

    /* initial render + render triggered by sync update */
    val expectedCount = 2
    assertThat(renderCount.get()).isEqualTo(expectedCount)
  }

  @Test
  fun stateUpdates_numberOfRendersTriggeredCorrectly_forPrimitive() {
    val renderCount = AtomicInteger(0)

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return LithoPrimitive(
            TestTextPrimitive(text = "Counter: ${counter.value}"),
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  // have to block the main looper while we are queueing updates to make
                  // sure that if any scheduling is posted to the main thread, it only
                  // happens after the work inside the onClick finishes.
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                  }
                })
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 2")

    /* initial render + render triggered by batched updates */
    val expectedRenderCount = 2
    assertThat(renderCount.get()).isEqualTo(expectedRenderCount)
  }

  /**
   * In this test we:
   * 1. Run two async updates (which are being batched when batching is enabled).
   * 2. Run on sync update. This sync update will end up consuming the currently batched updates and
   *    trigger one layout calculation.
   * 3. Then we start another batching with two async updates, which will trigger another layout
   *    calculation.
   *
   * The goal of this test is that the batching mechanism still works correctly, even when
   * interrupted by another sync update.
   */
  @Test
  fun stateUpdates_concurrent_sync_update_consumes_pending_state_updates_and_batching_can_be_resumed_forPrimitive() {
    val renderCount = AtomicInteger(0)

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return LithoPrimitive(
            TestTextPrimitive(text = "Counter: ${counter.value}"),
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }

                    /* this sync should consume all pending batched updates when batching is enabled */
                    executeAndRunPendingBgTasks { counter.updateSync { it + 1 } }
                  }

                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                  }
                })
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 5")
    /* initial render + render triggered by sync update (which interrupts batching) + render triggered by async update */
    val expectedCount = 3

    assertThat(renderCount.get()).isEqualTo(expectedCount)
  }

  /**
   * In this test we:
   * 1. Execute two async updates (which are being batched when batching is enabled)
   * 2. Execute one sync update. This sync update actually ends up on consuming the already enqueued
   *    updates.
   * 3. In this scenario we only have one layout calculation (besides the initial one).
   *
   * This test guarantees that we don't do any extra layout calculation due to the batching.
   */
  @Test
  fun stateUpdates_concurrent_sync_update_consumes_all_updates_and_consumes_all_batched_updates_forPrimitive() {
    val renderCount = AtomicInteger(0)

    class TestComponent : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        renderCount.incrementAndGet()

        val counter = useState { 0 }

        return LithoPrimitive(
            TestTextPrimitive(text = "Counter: ${counter.value}"),
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  withMainLooperPaused {
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }
                    executeAndRunPendingBgTasks { counter.update { it + 1 } }

                    /* this sync should consume all pending batched updates when batching is enabled */
                    executeAndRunPendingBgTasks { counter.updateSync { it + 1 } }
                  }
                })
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 0")
    assertThat(renderCount.get()).isEqualTo(1)

    executeAndRunPendingBgTasks { lithoViewRule.act(testLithoView) { clickOnTag("test_view") } }

    LithoAssertions.assertThat(testLithoView).hasVisibleText("Counter: 3")

    /* initial render + render triggered by sync update */
    val expectedCount = 2
    assertThat(renderCount.get()).isEqualTo(expectedCount)
  }

  private fun withMainLooperPaused(body: () -> Unit) {
    ShadowLooper.pauseMainLooper()
    body()
    ShadowLooper.unPauseMainLooper()
  }

  private fun executeAndRunPendingBgTasks(body: () -> Unit) {
    body()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
  }
}

@Suppress("TestFunctionName")
internal fun PrimitiveComponentScope.TestTextPrimitive(
    text: String,
    tag: String? = null
): Primitive {
  return Primitive(
      layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
      mountBehavior =
          MountBehavior(ViewAllocator { context -> TextView(context) }) {
            text.bindTo(TextView::setText, "")
            tag.bindTo(TextView::setTag)
            tag.bindTo(TextView::setContentDescription)
          })
}
