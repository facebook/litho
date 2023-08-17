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
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DynamicComponentGroupSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.MountSpecInterStagePropsTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
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

  fun `unchanged node with inter stage prop should not be remeasured when state updates`() {
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
                MountSpecInterStagePropsTester.create(c).lifecycleTracker(lifecycleTracker).build())
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs(
            "Node with inter stage props doesn't need remeasurement but rebinding if layout caching is turned on")
        .containsExactly(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun `unchanged node without inter stage prop should not get rebinding when state updates`() {
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
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs(
            "Node without inter stage props doesn't need rebinding if layout caching is turned on")
        .isEmpty()
  }

  @Test
  fun `unchanged subtree should not get rebinding when state updates`() {
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
                        MountSpecPureRenderLifecycleTester.create(c)
                            .intrinsicSize(Size(100, 100))
                            .lifecycleTracker(lifecycleTracker1))
                    .child(
                        Column.create(c)
                            .child(
                                MountSpecPureRenderLifecycleTester.create(c)
                                    .intrinsicSize(Size(200, 200))
                                    .lifecycleTracker(lifecycleTracker2))))
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker1.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker2.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker1.steps).isEmpty()
    Assertions.assertThat(lifecycleTracker2.steps).isEmpty()
  }

  @Test
  fun `changing size spec should trigger re-measurement`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
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
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker.reset()
    legacyLithoViewRule.setSizeSpecs(exactly(200), unspecified()).measure().layout()
    if (legacyLithoViewRule.componentTree.context.shouldCacheLayouts()) {
      Assertions.assertThat(lifecycleTracker.steps)
          .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
    } else {
      Assertions.assertThat(lifecycleTracker.steps)
          .containsExactly(
              LifecycleStep.ON_MEASURE,
              LifecycleStep.ON_BOUNDS_DEFINED,
              LifecycleStep.SHOULD_UPDATE,
              LifecycleStep.ON_UNBIND,
              LifecycleStep.ON_UNMOUNT,
              LifecycleStep.ON_MOUNT,
              LifecycleStep.ON_BIND)
    }
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
            .child(
                MountSpecInterStagePropsTester.create(c)
                    .lifecycleTracker(lifecycleTracker)
                    .viewTag("test"))
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker.reset()
    caller.increment()
    legacyLithoViewRule.lithoView.onDetachedFromWindowForTest()
    legacyLithoViewRule.lithoView.onAttachedToWindowForTest()

    // Will throw a NPE if the inter stage props are missing
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs("prepare and measure should not be called for cached node")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_BIND)
  }

  @Test
  fun `unchanged node should not get rebinding when the size of root node changes`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        Column.create(c)
            .child(
                MountSpecPureRenderLifecycleTester.create(c)
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
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker.reset()
    legacyLithoViewRule.setSizeSpecs(exactly(200), exactly(200)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).isEmpty()
  }

  /**
   * Context: With layout caching we need to reset the size spec of the root component if measured
   * size is different from specified one, to re-measure the whole tree. This case is to ensure that
   * the SizeSpec of AT_MOST is also being reset correctly.
   */
  @Test
  fun `RecyclerCollectionComponent with wrapContent should be re-measured with the latest size specs when it changes`() {
    if (!ComponentsConfiguration.enableLayoutCaching) {
      return
    }

    val context = legacyLithoViewRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()

    val component =
        buildRecyclerCollectionComponent(
            context, lifecycleTracker1, lifecycleTracker2, lifecycleTracker3)

    // Set LithoView with height so that it can fully show all the items
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(100), unspecified())
        .measure()
        .layout()

    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MEASURE))
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MEASURE))
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MEASURE))
        .isEqualTo(5)

    val height = legacyLithoViewRule.lithoView.height

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    val measuredSize = Size()
    legacyLithoViewRule.lithoView.componentTree?.setSizeSpec(
        exactly(200), unspecified(), measuredSize)
    Assertions.assertThat(measuredSize.height).isEqualTo(height)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MEASURE))
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MEASURE))
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MEASURE))
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
  }

  private fun buildRecyclerCollectionComponent(
      context: ComponentContext,
      lifecycleTracker1: LifecycleTracker,
      lifecycleTracker2: LifecycleTracker,
      lifecycleTracker3: LifecycleTracker,
  ): RecyclerCollectionComponent {

    val child1: Component =
        MountSpecLifecycleTester.create(context)
            .intrinsicSize(Size(10, 10))
            .lifecycleTracker(lifecycleTracker1)
            .build()
    val child2: Component =
        MountSpecLifecycleTester.create(context)
            .intrinsicSize(Size(10, 15))
            .lifecycleTracker(lifecycleTracker2)
            .build()
    val child3: Component =
        MountSpecLifecycleTester.create(context)
            .intrinsicSize(Size(10, 20))
            .lifecycleTracker(lifecycleTracker3)
            .build()

    val item: Component =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1))
            .child(Wrapper.create(context).delegate(child2))
            .child(Wrapper.create(context).delegate(child3))
            .build()
    val config: RecyclerConfiguration =
        ListRecyclerConfiguration.create()
            .recyclerBinderConfiguration(
                RecyclerBinderConfiguration.create().wrapContent(true).build())
            .build()
    val sectionContext = SectionContext(context)
    return RecyclerCollectionComponent.create(context)
        .recyclerConfiguration(config)
        .section(
            DynamicComponentGroupSection.create(sectionContext)
                .component(item)
                .totalItems(5)
                .build())
        .maxHeightDip(300f)
        .build()
  }
}
