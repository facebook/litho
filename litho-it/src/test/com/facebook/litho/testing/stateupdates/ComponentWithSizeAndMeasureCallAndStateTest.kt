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

package com.facebook.litho.stateupdates

import com.facebook.litho.Column.Companion.create
import com.facebook.litho.Component
import com.facebook.litho.LifecycleStep
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LithoViewAssert.Companion.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import java.util.ArrayList
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentWithSizeAndMeasureCallAndStateTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun render_withoutOptionalProps_usesDefaultPrefixAndRecordsLifecycle() {
    val steps: MutableList<StepInfo?> = ArrayList()
    val component = componentWithSteps(steps)

    val testLithoView = lithoTestRule.render { component }

    assertThat(testLithoView.lithoView).hasVisibleText("Count: 0")
    Assertions.assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED,
        )
  }

  @Test
  fun release_afterRender_recordsDetachLifecycle() {
    val steps: MutableList<StepInfo?> = ArrayList()
    val component = componentWithSteps(steps)
    val testLithoView = lithoTestRule.render { component }
    steps.clear()

    testLithoView.release()

    Assertions.assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.ON_DETACHED)
  }

  @Test
  fun render_withPrefixAndMountSpec_usesCustomPrefixAndMountSpecChild() {
    val steps: MutableList<StepInfo?> = ArrayList()
    val component =
        ComponentWithSizeAndMeasureCallAndState.create(lithoTestRule.context)
            .steps(steps)
            .shouldCacheResult(true)
            .prefix("Total:")
            .mountSpec(Text.create(lithoTestRule.context).text("Mount child").build())
            .build()

    val testLithoView = lithoTestRule.render { component }

    assertThat(testLithoView.lithoView).hasVisibleText("Total: 0")
    assertThat(testLithoView.lithoView).hasVisibleText("Mount child")
  }

  @Test
  fun stateUpdate_withCachedMeasuredChild_updatesVisibleCount() {
    val testLithoView = renderWithMeasuredChild(shouldCacheResult = true)
    val componentContext =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(0)

    ComponentWithSizeAndMeasureCallAndState.onIncrementCountSync(componentContext)

    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun stateUpdate_withUncachedMeasuredChild_updatesVisibleCount() {
    val testLithoView = renderWithMeasuredChild(shouldCacheResult = false)
    val componentContext =
        testLithoView.currentRootNode!!.node.getChildAt(0).getComponentContextAt(0)

    ComponentWithSizeAndMeasureCallAndState.onIncrementCountSync(componentContext)

    assertThat(testLithoView.lithoView).hasVisibleText("Count: 1")
  }

  private fun renderWithMeasuredChild(shouldCacheResult: Boolean) = lithoTestRule.render {
    create(context)
        .child(
            ComponentWithSizeAndMeasureCallAndState.create(context)
                .steps(ArrayList<StepInfo?>())
                .shouldCacheResult(shouldCacheResult)
                .component(Text.create(context).text("Measured child").build()),
        )
        .build()
  }

  private fun componentWithSteps(steps: MutableList<StepInfo?>): Component =
      ComponentWithSizeAndMeasureCallAndState.create(lithoTestRule.context)
          .steps(steps)
          .shouldCacheResult(true)
          .build()
}
