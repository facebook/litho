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

import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.assertj.Conditions.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.Text
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class CachedLayoutTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

  val commonAssertion =
      fun(steps: List<StepInfo>) {
        assertThat(LifecycleStep.getSteps(steps))
            .describedAs("cached layout spec should be resolved only once")
            .containsExactly(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT)
      }

  @Test
  fun `when component has compatible cached layout it should not be recreated or remeasured`() {

    val tracker = LifecycleTracker()
    val steps = mutableListOf<StepInfo>()
    val sizeSpec = Size(exactly(1080), 0)
    val root = TrackingComponent(tracker, steps, sizeSpec, commonAssertion)

    lithoViewRule.render { root }

    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("cached layout spec should be resolved only once")
        .containsOnlyOnce(LifecycleStep.ON_CREATE_LAYOUT)

    assertThat(tracker.steps)
        .describedAs("cached mount spec should be measured only once")
        .containsOnlyOnce(LifecycleStep.ON_MEASURE)
  }

  @Test
  fun `when component has incompatible cached layout it should only be remeasured`() {
    val tracker = LifecycleTracker()
    val steps = mutableListOf<StepInfo>()
    val size = Size(exactly(200), 200)
    val root = TrackingComponent(tracker, steps, size, commonAssertion)

    lithoViewRule.render { root }

    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("cached layout spec should be resolved only once")
        .containsOnlyOnce(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun `when component with flex has incompatible cached layout it should only be remeasured`() {
    val c: ComponentContext = lithoViewRule.context
    val tracker = LifecycleTracker()
    val steps = mutableListOf<StepInfo>()
    val size = Size(exactly(200), 200)
    val root =
        Row.create(c)
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(mutableListOf<StepInfo>())
                    .causeYogaRemeasure(true)
                    .body(TrackingComponent(tracker, steps, size, commonAssertion)))
            .child(Text.create(c).text("Hello World"))
            .build()

    lithoViewRule.render { root }

    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("cached layout spec should be resolved only once")
        .containsOnlyOnce(LifecycleStep.ON_CREATE_LAYOUT)
  }

  class TrackingComponent(
      private val tracker: LifecycleTracker,
      private val steps: List<StepInfo>,
      private val sizeSpec: Size,
      private val assertion: ((List<StepInfo>) -> Unit)?
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val mountSpec =
          MountSpecLifecycleTester.create(context)
              .intrinsicSize(Size(100, 100))
              .lifecycleTracker(tracker)
              .build()

      val layoutSpec = LayoutSpecLifecycleTester.create(context).steps(steps).build()

      // measure the components
      mountSpec.measure(context, sizeSpec.width, sizeSpec.height, Size())

      // resolve and measure the layout spec
      layoutSpec.measure(context, sizeSpec.width, sizeSpec.height, Size())

      assertion?.let { it(steps) }

      return Column {
        child(layoutSpec)
        child(mountSpec)
      }
    }
  }
}
