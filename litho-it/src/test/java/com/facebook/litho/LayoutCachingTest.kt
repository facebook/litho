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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.MountSpecInterStagePropsTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutCachingTest {

  @JvmField @Rule var legacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun `unchanged node should not be remeasured when state updates`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
            .child(
                MountSpecLifecycleTester.create(c)
                    .intrinsicSize(Size(100, 100))
                    .lifecycleTracker(lifecycleTracker)
                    .build())
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).contains(LifecycleStep.ON_MEASURE)

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps).doesNotContain(LifecycleStep.ON_MEASURE)
  }

  @Test
  fun `unchanged subtree should not be remeasured when state updates`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val caller: SimpleStateUpdateEmulatorSpec.Caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component: Column =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller))
            .child(
                Column.create(c)
                    .child(
                        MountSpecLifecycleTester.create(c)
                            .intrinsicSize(Size(100, 100))
                            .lifecycleTracker(lifecycleTracker1))
                    .child(
                        Column.create(c)
                            .child(
                                MountSpecLifecycleTester.create(c)
                                    .intrinsicSize(Size(200, 200))
                                    .lifecycleTracker(lifecycleTracker2))))
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker1.steps).contains(LifecycleStep.ON_MEASURE)
    Assertions.assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_MEASURE)

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker1.steps).doesNotContain(LifecycleStep.ON_MEASURE)
    Assertions.assertThat(lifecycleTracker2.steps).doesNotContain(LifecycleStep.ON_MEASURE)
  }

  @Test
  fun `changing size spec should trigger re-measurement`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(100, 100))
            .build()

    // Make the target component to be the root component and change the size spec
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(100), unspecified())
        .measure()
        .layout()
    Assertions.assertThat(lifecycleTracker.steps).contains(LifecycleStep.ON_MEASURE)

    lifecycleTracker.reset()
    legacyLithoViewRule.setSizeSpecs(exactly(200), unspecified()).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).contains(LifecycleStep.ON_MEASURE)
  }

  @Test
  fun `inter stage data should be copied for cached nodes`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller))
            .child(MountSpecInterStagePropsTester.create(c).lifecycleTracker(lifecycleTracker))
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).contains(LifecycleStep.ON_PREPARE)

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .doesNotContainSequence(LifecycleStep.ON_PREPARE, LifecycleStep.ON_MEASURE)
        .describedAs("prepare and measure should not be called for cached node")
        .contains(LifecycleStep.ON_BIND)
        .describedAs("prepared data should be present as the same as non-cached node")
  }

  @Test
  fun `unchanged node should not be remeasured when the size of root node changes`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        Column.create(c)
            .child(
                MountSpecLifecycleTester.create(c)
                    .lifecycleTracker(lifecycleTracker)
                    .maxWidthPx(200)
                    .maxHeightPx(200))
            .build()

    legacyLithoViewRule
        .setRoot(component)
        .setSizeSpecs(exactly(300), exactly(300))
        .attachToWindow()
        .measure()
        .layout()
    Assertions.assertThat(lifecycleTracker.steps).contains(LifecycleStep.ON_MEASURE)

    lifecycleTracker.reset()
    legacyLithoViewRule.setSizeSpecs(exactly(200), exactly(200)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).doesNotContain(LifecycleStep.ON_MEASURE)
  }
}
