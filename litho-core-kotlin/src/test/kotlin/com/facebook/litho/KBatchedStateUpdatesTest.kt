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

import com.facebook.litho.config.BatchedUpdatesConfiguration
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.viewtree.ViewPredicates.hasVisibleText
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/** Unit tests for [useState]. */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(ParameterizedRobolectricTestRunner::class)
class KBatchedStateUpdatesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val expectedException = ExpectedException.none()

  @Rule
  @JvmField
  val backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  companion object {

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters
    fun configurations(): Collection<Array<Any?>> =
        listOf(
            /*
            This is the case of when we have no batched update strategies. The test is setup so that
            it will calculate layout once per state update. The expected render count is 3, because
            there is 1 initial render + 2 update-triggered renders
             */
            arrayOf(TestConfig(null, 3)),

            /*
            From this section below, is the case when we have batched update strategies. In this case it will
            batch the updates and do only one extra render (besides the initial one) - therefore the
            expected render count is 2.
            */
            arrayOf(TestConfig(BatchedUpdatesConfiguration.POST_TO_FRONT_OF_MAIN_THREAD, 2)),
            arrayOf(TestConfig(BatchedUpdatesConfiguration.POST_TO_CHOREOGRAPHER_CALLBACK, 2)),
            arrayOf(TestConfig(BatchedUpdatesConfiguration.BEST_EFFORT_FRONT_AND_CHOREOGRAPHER, 2)),
        )
  }

  @ParameterizedRobolectricTestRunner.Parameter lateinit var config: TestConfig

  @After
  fun tearDown() {
    ComponentsConfiguration.sBatchedUpdatesConfiguration = null
  }

  @Test
  fun stateUpdates_numberOfRendersTriggeredCorrectly() {
    ComponentsConfiguration.sBatchedUpdatesConfiguration = config.batchedUpdatesConfiguration

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
                  ShadowLooper.pauseMainLooper()
                  counter.update { it + 1 }
                  backgroundLayoutLooperRule.runToEndOfTasksSync()

                  counter.update { it + 1 }
                  backgroundLayoutLooperRule.runToEndOfTasksSync()
                  ShadowLooper.unPauseMainLooper()
                }) { child(Text(text = "Counter: ${counter.value}")) }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    lithoViewRule.act(testLithoView) { hasVisibleText("Counter: 0") }
    assertThat(renderCount.get()).isEqualTo(1)

    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    lithoViewRule.act(testLithoView) { hasVisibleText("Counter: 2") }
    assertThat(renderCount.get()).isEqualTo(config.expectedRenderCount)
  }

  data class TestConfig(
      val batchedUpdatesConfiguration: BatchedUpdatesConfiguration?,
      val expectedRenderCount: Int,
  )
}
