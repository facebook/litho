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
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.atMost
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.MountSpecInterStagePropsTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
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

  @JvmField @Rule var legacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun `verify the background of container is properly reused or created with layout caching`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

    val caller = SimpleStateUpdateEmulatorSpec.Caller()

    val component =
        Column.create(c)
            .backgroundColor(Color.LTGRAY)
            .child(
                SimpleStateUpdateEmulator.create(c)
                    .prefix("\n\n\n")
                    .initialCount(0)
                    .caller(caller)
                    .build())
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()

    val background1 = legacyLithoViewRule.committedLayoutState?.getMountableOutputAt(1)?.renderUnit

    // the height should be changed so we're not supposed to reuse the background outputs
    caller.increment()
    val background2 = legacyLithoViewRule.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background1 != background2).isTrue

    // the height should not be changed so we could reuse the background outputs
    caller.increment()
    val background3 = legacyLithoViewRule.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background2 === background3).isTrue
  }

  @Test
  fun `verify the layout behavior of container with background, padding is as expected`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val component =
        Column.create(c)
            .child(Column.create(c).backgroundColor(Color.LTGRAY).paddingPx(YogaEdge.VERTICAL, 10))
            .child(SimpleStateUpdateEmulator.create(c).prefix("\n\n\n").caller(caller).build())
            .build()

    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()

    val background1 = legacyLithoViewRule.committedLayoutState?.getMountableOutputAt(1)?.renderUnit

    // the background of column should be reused because it doesn't change at all
    caller.increment()
    val background2 = legacyLithoViewRule.committedLayoutState?.getMountableOutputAt(1)?.renderUnit
    Assertions.assertThat(background1 === background2).isTrue
  }

  @Test
  fun `verify the layout result of a fixed size component is not reused when its size has changed`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(c)
            .widthPercent(100f)
            .heightPercent(100f)
            .lifecycleTracker(lifecycleTracker)
            .build()

    legacyLithoViewRule
        .setRoot(component)
        .setSizeSpecs(exactly(400), exactly(200))
        .attachToWindow()
        .measure()
        .layout()
    lifecycleTracker.reset()

    legacyLithoViewRule.setSizeSpecs(exactly(200), exactly(400)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun `verify the layout result of a component with different size specs is re-measured`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(c)
            .widthPercent(100f)
            .heightPercent(100f)
            .lifecycleTracker(lifecycleTracker)
            .build()

    legacyLithoViewRule
        .setRoot(component)
        .setSizeSpecs(exactly(1080), unspecified())
        .attachToWindow()
        .measure()
        .layout()
    lifecycleTracker.reset()

    legacyLithoViewRule.setSizeSpecs(atMost(200), atMost(200)).measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun `unchanged node with inter stage prop should not be remeasured when state updates`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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
            "Node with inter stage props doesn't need re-measurement and rebinding if layout caching is turned on")
        .isEmpty()
  }

  @Test
  fun `unchanged node without inter stage prop should not get rebinding when state updates`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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
    Assertions.assertThat(lifecycleTracker.steps)
        .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
  }

  @Test
  fun `inter stage data should be copied for cached nodes`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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

    // Will throw a NPE if the inter stage props are missing
    Assertions.assertThat(lifecycleTracker.steps)
        .describedAs("prepare and measure should not be called for cached node")
        .isEmpty()
  }

  @Test
  fun `unchanged node should not get rebinding when the size of root node changes`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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

  @Test
  fun `verify the behavior of nested container with flex settings`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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

    legacyLithoViewRule
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(SizeSpec.UNSPECIFIED, 0),
            SizeSpec.makeSizeSpec(SizeSpec.UNSPECIFIED, 0))
        .setRoot(component)
        .attachToWindow()
        .measure()
        .layout()

    Assertions.assertThat(lifecycleTracker1.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker2.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker3.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker4.steps)
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    val lithoView = legacyLithoViewRule.lithoView
    val component_child1 = legacyLithoViewRule.findViewWithTag("child1")
    val component_child2 = legacyLithoViewRule.findViewWithTag("child2")
    val component_row1 = legacyLithoViewRule.findViewWithTag("Row1")
    val component_child3 = legacyLithoViewRule.findViewWithTag("child3")
    val component_child4 = legacyLithoViewRule.findViewWithTag("child4")
    val component_row2 = legacyLithoViewRule.findViewWithTag("Row2")
    val component_updater = legacyLithoViewRule.findViewWithTag("StateUpdater")

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
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs("Child2 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs("Child3 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs("Child4 should NOT be re-measured because nothing changed")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    lifecycleTracker4.reset()
    legacyLithoViewRule.setSizePx(200, 200).measure().layout()
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
            "Child1 should NOT be re-measured because the root width is compatible with its size")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs(
            "Child2 should NOT be re-measured because the root width is compatible with its size")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs(
            "Child3 should NOT be re-measured because the root width is compatible with its size")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs(
            "Child4 should NOT be re-measured because the root width is compatible with its size")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    lifecycleTracker4.reset()
    legacyLithoViewRule.setSizePx(40, 200).measure().layout()
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
            "Child1 should be re-measured because the root width is NOT compatible with its size")
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker2.steps)
        .describedAs(
            "Child2 should be re-measured because the root width is NOT compatible with its size")
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker3.steps)
        .describedAs("Child3 should be re-measured because we need to shrink its width")
        .contains(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    Assertions.assertThat(lifecycleTracker4.steps)
        .describedAs(
            "Child4 should NOT be re-measured because we need to respect its shrink settings")
        .doesNotContain(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun `verify the behavior of onBoundsDefined with padding and border`() {
    val c = legacyLithoViewRule.context
    if (!c.shouldCacheLayouts()) {
      return
    }

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
                    .widthPercent(90f)) // equivalent to exactly(972)
            .build()

    // initial mount
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    Assertions.assertThat(lifecycleTracker.steps)
        .contains(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
    Assertions.assertThat(lifecycleTracker.width).isEqualTo(972)
    Assertions.assertThat(lifecycleTracker.height).isEqualTo(20)

    // set root with the same component to verify that:
    // We're reusing the correct size that saved from diff node
    lifecycleTracker.reset()
    legacyLithoViewRule
        .setRootAndSizeSpecSync(
            Column.create(c)
                .child(SimpleStateUpdateEmulator.create(c).caller(caller).build())
                .child(
                    MountSpecPureRenderLifecycleTester.create(c)
                        .lifecycleTracker(lifecycleTracker)
                        .shouldUpdate(false)
                        .paddingPx(YogaEdge.ALL, 5)
                        .border(Border.create(c).widthPx(YogaEdge.ALL, 5).build()))
                .build(),
            exactly(972),
            unspecified(0))
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
    legacyLithoViewRule
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
                        .heightPx(20))
                .build())
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
    val context = legacyLithoViewRule.context
    if (!context.shouldCacheLayouts()) {
      return
    }

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
