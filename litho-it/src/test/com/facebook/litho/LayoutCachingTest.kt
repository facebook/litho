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

import android.graphics.Color
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DynamicComponentGroupSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.atMost
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.MountSpecInterStagePropsTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
import com.facebook.litho.widget.RecyclerBinderConfig
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutCachingTest {

  @JvmField @Rule var lithoTestRule = LithoTestRule()

  @Test
  fun `verify the background of container is properly reused or created with layout caching`() {
    val c = lithoTestRule.context
    val caller = SimpleStateUpdateEmulatorSpec.Caller()

    val component =
        Column.create(c)
            .backgroundColor(Color.LTGRAY)
            .child(
                SimpleStateUpdateEmulator.create(c)
                    .prefix("\n\n\n")
                    .initialCount(0)
                    .caller(caller)
                    .build(),
            )
            .build()

    val testLithoView = lithoTestRule.render { component }

    val background1 = testLithoView.committedLayoutState?.getMountableOutputAt(1)?.renderUnit

    // the height should be changed so we're not supposed to reuse the background outputs
    caller.increment()
    val background2 = testLithoView.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background1 != background2).isTrue

    // the height should not be changed so we could reuse the background outputs
    caller.increment()
    val background3 = testLithoView.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background2 === background3).isTrue
  }

  @Test
  fun `verify the layout behavior of container with background, padding is as expected`() {
    val c = lithoTestRule.context

    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(Column.create(c).backgroundColor(Color.LTGRAY).paddingPx(YogaEdge.VERTICAL, 10))
            .child(SimpleStateUpdateEmulator.create(c).prefix("\n\n\n").caller(caller).build())
            .build()

    val testLithoView = lithoTestRule.render { component }

    val background1 = testLithoView.committedLayoutState?.getMountableOutputAt(1)?.renderUnit

    // the background of column should be reused because it doesn't change at all
    caller.increment()
    val background2 = testLithoView.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background1 === background2).isTrue
  }

  @Test
  fun `verify the layout result of a fixed size component is not reused when its size has changed`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(c)
            .widthPercent(100f)
            .heightPercent(100f)
            .lifecycleTracker(lifecycleTracker)
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(400), heightSpec = exactly(200)) { component }
    lifecycleTracker.reset()

    testLithoView.setSizeSpecs(exactly(200), exactly(400)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
  }

  @Test
  fun `verify the layout result of a component with different size specs is re-measured`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(c)
            .widthPercent(100f)
            .heightPercent(100f)
            .lifecycleTracker(lifecycleTracker)
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(1080), heightSpec = unspecified()) { component }
    lifecycleTracker.reset()

    testLithoView.setSizeSpecs(atMost(200), atMost(200)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
  }

  @Test
  fun `unchanged node with inter stage prop should not be remeasured when state updates`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
            .child(
                MountSpecInterStagePropsTester.create(c).lifecycleTracker(lifecycleTracker).build(),
            )
            .build()

    lithoTestRule.render { component }
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs(
            "Node with inter stage props doesn't need re-measurement and rebinding if layout caching is turned on",
        )
        .isEmpty()
  }

  @Test
  fun `unchanged node without inter stage prop should not get rebinding when state updates`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
            .child(
                MountSpecLifecycleTester.create(c)
                    .intrinsicSize(Size(100, 100))
                    .lifecycleTracker(lifecycleTracker)
                    .paddingPx(YogaEdge.ALL, 5)
                    .border(Border.create(c).widthPx(YogaEdge.ALL, 5).build())
                    .build(),
            )
            .build()

    lithoTestRule.render { component }
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
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs(
            "Node without inter stage props doesn't need rebinding if layout caching is turned on",
        )
        .isEmpty()
  }

  @Test
  fun `unchanged subtree should not get rebinding when state updates`() {
    val c = lithoTestRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val caller: SimpleStateUpdateEmulatorSpec.Caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component: Component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller))
            .child(
                Column.create(c)
                    .child(
                        MountSpecPureRenderLifecycleTester.create(c)
                            .intrinsicSize(Size(100, 100))
                            .lifecycleTracker(lifecycleTracker1),
                    )
                    .child(
                        Column.create(c)
                            .child(
                                MountSpecPureRenderLifecycleTester.create(c)
                                    .intrinsicSize(Size(200, 200))
                                    .lifecycleTracker(lifecycleTracker2),
                            ),
                    ),
            )
            .build()

    lithoTestRule.render { component }
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
            LifecycleStep.ON_BIND,
        )
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
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker1.steps).isEmpty()
    Assertions.assertThat(lifecycleTracker2.steps).isEmpty()
  }

  @Test
  fun `changing size spec should trigger re-measurement`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(100, 100))
            .build()

    // Make the target component to be the root component and change the size spec
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(100), heightSpec = unspecified()) { component }
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
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(exactly(200), unspecified()).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
  }

  @Test
  fun `inter stage data should be copied for cached nodes`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller))
            .child(
                MountSpecInterStagePropsTester.create(c)
                    .lifecycleTracker(lifecycleTracker)
                    .viewTag("test"),
            )
            .build()

    lithoTestRule.render { component }
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker.reset()
    caller.increment()

    // Will throw a NPE if the inter stage props are missing
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs("prepare and measure should not be called for cached node")
        .isEmpty()
  }

  @Test
  fun `unchanged node should not get rebinding when the size of root node changes`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        Column.create(c)
            .child(
                MountSpecPureRenderLifecycleTester.create(c)
                    .lifecycleTracker(lifecycleTracker)
                    .maxWidthPx(200)
                    .maxHeightPx(200),
            )
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(300), heightSpec = exactly(300)) { component }
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
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(exactly(200), exactly(200)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps).isEmpty()
  }

  @Test
  fun `verify the behavior of nested container with flex settings`() {
    val c = lithoTestRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()
    val lifecycleTracker4 = LifecycleTracker()
    val mountSpec1 =
        MountSpecLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker1)
            .intrinsicSize(Size(70, 10))
            .build()
    val mountSpec2 =
        MountSpecLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker2)
            .intrinsicSize(Size(80, 10))
            .build()
    val mountSpec3 =
        MountSpecLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker3)
            .intrinsicSize(Size(25, 10))
            .build()
    val mountSpec4 =
        MountSpecLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker4)
            .intrinsicSize(Size(35, 10))
            .build()
    val caller: SimpleStateUpdateEmulatorSpec.Caller = SimpleStateUpdateEmulatorSpec.Caller()

    val child1 =
        Row.create(c)
            .viewTag("child1")
            .heightPx(20)
            .child(mountSpec1)
            .flex(1f)
            .flexBasisPx(0)
            .build()
    val child2 =
        Row.create(c)
            .viewTag("child2")
            .heightPx(20)
            .child(mountSpec2)
            .flex(1f)
            .flexBasisPx(0)
            .build()
    val child3 =
        Row.create(c) // Shrink this child with the default value(1f)
            .viewTag("child3")
            .heightPx(30)
            .child(mountSpec3)
            .build()
    val child4 =
        Row.create(c)
            .viewTag("child4")
            .heightPx(40)
            .flexShrink(0f) // Don't shrink this child
            .child(mountSpec4)
            .build()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).viewTag("StateUpdater").caller(caller))
            .child(Row.create(c).viewTag("Row1").child(child1).child(child2))
            .child(Row.create(c).viewTag("Row2").child(child3).child(child4))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) { component }

    Assertions.assertThat(lifecycleTracker1.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker2.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker3.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker4.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )

    val lithoView = testLithoView.lithoView
    val component_child1 = testLithoView.findViewWithTag("child1")
    val component_child2 = testLithoView.findViewWithTag("child2")
    val component_row1 = testLithoView.findViewWithTag("Row1")
    val component_child3 = testLithoView.findViewWithTag("child3")
    val component_child4 = testLithoView.findViewWithTag("child4")
    val component_row2 = testLithoView.findViewWithTag("Row2")
    val component_updater = testLithoView.findViewWithTag("StateUpdater")

    Assertions.assertThat(component_child1.width)
        .describedAs("Child1 should be measured with specified width")
        .isEqualTo(70)
    Assertions.assertThat(component_child2.width)
        .describedAs("Child2 should be measured with specified width")
        .isEqualTo(80)
    Assertions.assertThat(component_child3.width)
        .describedAs("Child3 should respect the value being set")
        .isEqualTo(25)
    Assertions.assertThat(component_child4.width)
        .describedAs("Child4 should respect the value being set")
        .isEqualTo(35)
    Assertions.assertThat(component_row1.height)
        .describedAs("Row should be filled with two children")
        .isEqualTo(component_child1.height)
        .isEqualTo(component_child2.height)
    Assertions.assertThat(lithoView.height)
        .describedAs("The height of the entire view should be equal to Row1 + Row2 + StateUpdater")
        .isEqualTo(component_row1.height + component_row2.height + component_updater.height)

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    lifecycleTracker4.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker1.steps)
        .describedAs("Child1 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs("Child2 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs("Child3 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs("Child4 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    lifecycleTracker4.reset()
    testLithoView.setSizePx(200, 200).measure().layout()
    Assertions.assertThat(component_child1.width)
        .describedAs("Child1 should take half of the width")
        .isEqualTo(100)
    Assertions.assertThat(component_child2.width)
        .describedAs("Child2 should take another half of the width")
        .isEqualTo(100)
    Assertions.assertThat(component_child3.width)
        .describedAs("Child3 should be equal to the intrinsic size")
        .isEqualTo(25)
    Assertions.assertThat(component_child4.width)
        .describedAs("Child4 should be equal to the intrinsic size")
        .isEqualTo(35)
    Assertions.assertThat(lifecycleTracker1.steps)
        .describedAs(
            "Child1 should NOT be re-measured because the root width is compatible with its size",
        )
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs(
            "Child2 should NOT be re-measured because the root width is compatible with its size",
        )
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs(
            "Child3 should NOT be re-measured because the root width is compatible with its size",
        )
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs(
            "Child4 should NOT be re-measured because the root width is compatible with its size",
        )
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    lifecycleTracker4.reset()
    testLithoView.setSizePx(40, 200).measure().layout()
    Assertions.assertThat(component_child1.width)
        .describedAs("Shrink width due to the size constraints")
        .isEqualTo(20)
    Assertions.assertThat(component_child2.width)
        .describedAs("The width should respect the value being set")
        .isEqualTo(20)
    Assertions.assertThat(component_child3.width)
        .describedAs("Shrink width due to the size constraints")
        .isEqualTo(5)
    Assertions.assertThat(component_child4.width)
        .describedAs("The width should respect the value being set")
        .isEqualTo(35)
    Assertions.assertThat(lifecycleTracker1.steps)
        .describedAs(
            "Child1 should be re-measured because the root width is NOT compatible with its size",
        )
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs(
            "Child2 should be re-measured because the root width is NOT compatible with its size",
        )
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs("Child3 should be re-measured because we need to shrink its width")
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs(
            "Child4 should NOT be re-measured because we need to respect its shrink settings",
        )
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND,
        )
  }

  @Test
  fun `verify the behavior of onBoundsDefined with padding and border`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
            .child(
                MountSpecPureRenderLifecycleTester.create(c)
                    .lifecycleTracker(lifecycleTracker)
                    .shouldUpdate(false)
                    .paddingPx(YogaEdge.ALL, 5)
                    .border(Border.create(c).widthPx(YogaEdge.ALL, 5).build())
                    .widthPercent(90f),
            ) // equivalent to exactly(972)
            .build()

    // initial mount
    val testLithoView = lithoTestRule.render { component }
    Assertions.assertThat(lifecycleTracker.steps)
        .contains(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
    Assertions.assertThat(lifecycleTracker.width).isEqualTo(972)
    Assertions.assertThat(lifecycleTracker.height).isEqualTo(20)

    // set root with the same component to verify that:
    // We're reusing the correct size that saved from diff node
    lifecycleTracker.reset()
    testLithoView
        .setRootAndSizeSpecSync(
            Column.create(c)
                .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
                .child(
                    MountSpecPureRenderLifecycleTester.create(c)
                        .lifecycleTracker(lifecycleTracker)
                        .shouldUpdate(false)
                        .paddingPx(YogaEdge.ALL, 5)
                        .border(Border.create(c).widthPx(YogaEdge.ALL, 5).build()),
                )
                .build(),
            exactly(972),
            unspecified(0),
        )
        .measure()
        .layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .doesNotContain(LifecycleStep.ON_MEASURE) // Hit layout diffing
        .contains(LifecycleStep.ON_BOUNDS_DEFINED, LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker.width).isEqualTo(972)
    Assertions.assertThat(lifecycleTracker.height).isEqualTo(20)

    // Trigger state update to go with layout caching and verify that:
    // 1. we should subtract padding and border from measured size when `onMeasure` being called
    // 2. we're reusing layout cache and render units
    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .doesNotContain(
            LifecycleStep.ON_MEASURE, // Hit cached component without size changing
            LifecycleStep.ON_BOUNDS_DEFINED, // Hit cached component without size changing
            LifecycleStep.ON_BIND, // We're re-using render units
        )

    // Set root with fixed size to verify that:
    // 1. we're skipping Yoga measurement due to the fixed size
    // 2. we should subtract padding and border from measured size when `onMeasure` not being called
    // 3. we're creating new render units
    lifecycleTracker.reset()
    testLithoView
        .setRoot(
            Column.create(c)
                .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
                .child(
                    MountSpecPureRenderLifecycleTester.create(c)
                        .lifecycleTracker(lifecycleTracker)
                        .shouldUpdate(false)
                        .paddingPx(YogaEdge.ALL, 5)
                        .border(Border.create(c).widthPx(YogaEdge.ALL, 5).build())
                        .widthPx(972)
                        .heightPx(20),
                )
                .build(),
        )
        .measure()
        .layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .doesNotContain(LifecycleStep.ON_MEASURE) // Yoga doesn't measure component with fixed size
        .contains(LifecycleStep.ON_BOUNDS_DEFINED, LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker.width).isEqualTo(972)
    Assertions.assertThat(lifecycleTracker.height).isEqualTo(20)

    // Trigger state update to go with layout caching and verify that:
    // We're able to reuse layout cache due to the state update without size changing, which is
    // comparing with the size that we saved during the previous setRoot.
    lifecycleTracker.reset()
    caller.increment()
    Assertions.assertThat(lifecycleTracker.steps)
        .doesNotContain(
            LifecycleStep.ON_MEASURE, // Yoga doesn't measure component with fixed size
            LifecycleStep.ON_BOUNDS_DEFINED, // Hit cached component without size changing
            LifecycleStep.ON_BIND, // We're re-using render units
        )
  }

  /**
   * Context: With layout caching we need to reset the size spec of the root component if measured
   * size is different from specified one, to re-measure the whole tree. This case is to ensure that
   * the SizeSpec of AT_MOST is also being reset correctly.
   */
  @Test
  fun `RecyclerCollectionComponent with wrapContent should be re-measured with the latest size specs when it changes`() {
    val context = lithoTestRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()

    val component = buildRecyclerCollectionComponent(
        context,
        lifecycleTracker1,
        lifecycleTracker2,
        lifecycleTracker3,
    )

    // Set LithoView with height so that it can fully show all the items
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(100), heightSpec = unspecified()) { component }

    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MEASURE),
        )
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MEASURE),
        )
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MEASURE),
        )
        .isEqualTo(5)

    val height = testLithoView.lithoView.height

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    val measuredSize = Size()
    testLithoView.lithoView.componentTree?.setSizeSpec(exactly(200), unspecified(), measuredSize)
    Assertions.assertThat(measuredSize.height).isEqualTo(height)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker1.steps, LifecycleStep.ON_MEASURE),
        )
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker2.steps, LifecycleStep.ON_MEASURE),
        )
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
    Assertions.assertThat(
            getCountOfLifecycleSteps(lifecycleTracker3.steps, LifecycleStep.ON_MEASURE),
        )
        .describedAs("Ensure that all children are re-measured")
        .isEqualTo(5)
  }

  @Test
  fun `verify root component with maxWidth constraint is properly reset when re-measuring`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .maxWidthPx(200)
            .intrinsicSize(Size(200, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(180), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 180px (capped by size spec, below maxWidth)")
        .isEqualTo(180)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(atMost(250), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should be capped at maxWidth constraint of 200px")
        .isEqualTo(200)
  }

  @Test
  fun `verify root component with maxHeight constraint is properly reset when re-measuring`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .maxHeightPx(200)
            .intrinsicSize(Size(50, 200))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = atMost(180)) { component }
    val initialHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(initialHeight)
        .describedAs("Height should be 180px (capped by size spec, below maxHeight)")
        .isEqualTo(180)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(unspecified(), atMost(250)).measure().layout()
    val newHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(newHeight)
        .describedAs("Height should be capped at maxHeight constraint of 200px")
        .isEqualTo(200)
  }

  @Test
  fun `verify size constraints are properly reset when re-measuring with different size specs`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(100)
            .maxWidthPx(300)
            .intrinsicSize(Size(300, 100))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(250), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Initial width should be 250px (capped by size spec, below maxWidth)")
        .isEqualTo(250)

    lifecycleTracker.reset()
    // Size spec is atMost(350) but should be capped at maxWidth of 300
    testLithoView.setSizeSpecs(atMost(350), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should be capped at maxWidth constraint of 300px")
        .isEqualTo(300)

    lifecycleTracker.reset()
    // Size spec is atMost(80), size spec takes precedence
    testLithoView.setSizeSpecs(atMost(80), unspecified()).measure().layout()
    val minWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(minWidth)
        .describedAs("Width should be capped at 80px by atMost(80) size spec")
        .isEqualTo(80)
  }

  @Test
  fun `verify root component with combined min and max constraints resets properly`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(100)
            .maxWidthPx(200)
            .minHeightPx(50)
            .maxHeightPx(100)
            .intrinsicSize(Size(200, 100))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(150), heightSpec = atMost(75)) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    val initialHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(initialWidth).isEqualTo(150)
    Assertions.assertThat(initialHeight).isEqualTo(75)

    lifecycleTracker.reset()
    // Test that maxWidth constraint is enforced (size spec is atMost(250) but should be capped at
    // 200)
    testLithoView.setSizeSpecs(atMost(250), atMost(120)).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    val newHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(newWidth)
        .describedAs("Width should be capped at maxWidth of 200px")
        .isEqualTo(200)
    Assertions.assertThat(newHeight)
        .describedAs("Height should be capped at maxHeight of 100px")
        .isEqualTo(100)

    lifecycleTracker.reset()
    // Test that minWidth constraint is enforced (size spec is atMost(80) but should be enforced to
    // 100)
    testLithoView.setSizeSpecs(atMost(80), atMost(40)).measure().layout()
    val minWidth = testLithoView.lithoView.measuredWidth
    val minHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(minWidth)
        .describedAs("Width should be enforced to minWidth of 80px")
        .isEqualTo(80)
    Assertions.assertThat(minHeight)
        .describedAs("Height should be enforced to minHeight of 40px")
        .isEqualTo(40)
  }

  @Test
  fun `verify root component with percentage-based constraints resets properly`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPercent(30f)
            .maxWidthPercent(70f)
            .intrinsicSize(Size(1000, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be capped at maxWidthPercent (70% of 500 = 350px)")
        .isEqualTo(350)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(atMost(600), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should be capped at maxWidthPercent (70% of 600 = 420px)")
        .isEqualTo(420)
  }

  @Test
  fun `verify root component with percentage-based height constraints resets properly`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minHeightPercent(30f)
            .maxHeightPercent(70f)
            .intrinsicSize(Size(50, 1000))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = atMost(500)) { component }
    val initialHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(initialHeight)
        .describedAs("Height should be capped at maxHeightPercent (70% of 500 = 350px)")
        .isEqualTo(350)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(unspecified(), atMost(600)).measure().layout()
    val newHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(newHeight)
        .describedAs("Height should be capped at maxHeightPercent (70% of 600 = 420px)")
        .isEqualTo(420)
  }

  @Test
  fun `verify width and maxWidth both applied when both are set on root`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .widthPercent(100f)
            .maxWidthPx(200)
            .intrinsicSize(Size(300, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    val initialHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 200px (widthPercent would be 500px but capped by maxWidth)")
        .isEqualTo(200)
    Assertions.assertThat(initialHeight)
        .describedAs("Height should be 50px (intrinsic size)")
        .isEqualTo(50)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(atMost(150), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    val newHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(newWidth)
        .describedAs("Width should be 150px (widthPercent 100% of 150, below maxWidth)")
        .isEqualTo(150)
    Assertions.assertThat(newHeight)
        .describedAs("Height should remain 50px (intrinsic size)")
        .isEqualTo(50)
  }

  @Test
  fun `verify height and maxHeight both applied when both are set on root`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .heightPercent(100f)
            .maxHeightPx(200)
            .intrinsicSize(Size(50, 300))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = atMost(500)) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    val initialHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 50px (intrinsic size)")
        .isEqualTo(50)
    Assertions.assertThat(initialHeight)
        .describedAs(
            "Height should be 200px (heightPercent would be 500px but capped by maxHeight)",
        )
        .isEqualTo(200)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(unspecified(), atMost(150)).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    val newHeight = testLithoView.lithoView.measuredHeight
    Assertions.assertThat(newWidth)
        .describedAs("Width should remain 50px (intrinsic size)")
        .isEqualTo(50)
    Assertions.assertThat(newHeight)
        .describedAs("Height should be 150px (heightPercent 100% of 150, below maxHeight)")
        .isEqualTo(150)
  }

  @Test
  fun `verify minWidth overrides maxWidth when minWidth is larger`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(300)
            .maxWidthPx(200)
            .intrinsicSize(Size(250, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 300px (minWidth overrides maxWidth)")
        .isEqualTo(300)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(atMost(400), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should still be 300px (minWidth still overrides maxWidth)")
        .isEqualTo(300)
  }

  @Test
  fun `verify exactly size spec overrides min and max constraints`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(200)
            .maxWidthPx(400)
            .intrinsicSize(Size(300, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(100), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 100px (exactly() overrides minWidth of 200)")
        .isEqualTo(100)

    lifecycleTracker.reset()
    testLithoView.setSizeSpecs(exactly(500), unspecified()).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should be 500px (exactly() overrides maxWidth of 400)")
        .isEqualTo(500)
  }

  @Test
  fun `verify minWidth and maxWidth work together for content sizing`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()

    // Test case 1: Small intrinsic size - minWidth should be enforced
    val componentSmall =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(100)
            .maxWidthPx(300)
            .intrinsicSize(Size(50, 50))
            .build()

    val testLithoView1 =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) { componentSmall }
    val width1 = testLithoView1.lithoView.measuredWidth
    Assertions.assertThat(width1)
        .describedAs("Width should be 100px (minWidth enforced for small content)")
        .isEqualTo(100)

    // Test case 2: Large intrinsic size - maxWidth should be enforced
    lifecycleTracker.reset()
    val componentLarge =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(100)
            .maxWidthPx(300)
            .intrinsicSize(Size(400, 50))
            .build()

    val testLithoView2 =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) { componentLarge }
    val width2 = testLithoView2.lithoView.measuredWidth
    Assertions.assertThat(width2)
        .describedAs("Width should be 300px (maxWidth enforced for large content)")
        .isEqualTo(300)

    // Test case 3: Content within range - intrinsic size should be used
    lifecycleTracker.reset()
    val componentMedium =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .minWidthPx(100)
            .maxWidthPx(300)
            .intrinsicSize(Size(200, 50))
            .build()

    val testLithoView3 =
        lithoTestRule.render(widthSpec = atMost(500), heightSpec = unspecified()) {
          componentMedium
        }
    val width3 = testLithoView3.lithoView.measuredWidth
    Assertions.assertThat(width3)
        .describedAs("Width should be 200px (intrinsic size within min/max range)")
        .isEqualTo(200)
  }

  @Test
  fun `verify unspecified size spec with maxWidth uses FitContent sizing`() {
    val c = lithoTestRule.context
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .maxWidthPx(200)
            .intrinsicSize(Size(300, 50))
            .build()

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) { component }
    val initialWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(initialWidth)
        .describedAs("Width should be 200px (capped by maxWidth in FitContent mode)")
        .isEqualTo(200)

    lifecycleTracker.reset()
    val component2 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(lifecycleTracker)
            .maxWidthPx(200)
            .intrinsicSize(Size(100, 50))
            .build()
    testLithoView.setRoot(component2).measure().layout()
    val newWidth = testLithoView.lithoView.measuredWidth
    Assertions.assertThat(newWidth)
        .describedAs("Width should be 100px (intrinsic size, below maxWidth)")
        .isEqualTo(100)
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
                RecyclerBinderConfiguration.create()
                    .recyclerBinderConfig(RecyclerBinderConfig(wrapContent = true))
                    .build(),
            )
            .build()
    val sectionContext = SectionContext(context)
    return RecyclerCollectionComponent.create(context)
        .recyclerConfiguration(config)
        .section(
            DynamicComponentGroupSection.create(sectionContext).component(item).build(),
        )
        .maxHeightDip(300f)
        .build()
  }
}
