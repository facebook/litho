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

import com.facebook.litho.LifecycleStep.ON_MOUNT
import com.facebook.litho.LifecycleStep.ON_UNMOUNT
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DynamicComponentGroupSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SectionsRecyclerView
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class IncrementalMountTest {

  private lateinit var context: ComponentContext
  @JvmField @Rule var lithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    context = lithoViewRule.context
    lithoViewRule.useLithoView(LithoView(context))
  }

  private fun buildRecyclerCollectionComponent(
      lifecycleTracker1: LifecycleTracker,
      lifecycleTracker2: LifecycleTracker,
      lifecycleTracker3: LifecycleTracker,
      childWidth: Int,
      childHeight: Int
  ): RecyclerCollectionComponent {
    val child1: Component =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker1).build()
    val child2: Component =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker2).build()
    val child3: Component =
        MountSpecLifecycleTester.create(context).lifecycleTracker(lifecycleTracker3).build()

    // Item is composed of 3 children of equal size (10x10), making 1 item of height 30.
    val item: Component =
        Column.create(context)
            .child(
                Wrapper.create(context).delegate(child1).widthPx(childWidth).heightPx(childHeight))
            .child(
                Wrapper.create(context).delegate(child2).widthPx(childWidth).heightPx(childHeight))
            .child(
                Wrapper.create(context).delegate(child3).widthPx(childWidth).heightPx(childHeight))
            .build()
    val config: RecyclerConfiguration = ListRecyclerConfiguration.create().build()
    val sectionContext = SectionContext(context)
    return RecyclerCollectionComponent.create(context)
        .recyclerConfiguration(config)
        .section(
            DynamicComponentGroupSection.create(sectionContext)
                .component(item)
                .totalItems(5)
                .build())
        .build()
  }

  @Test
  fun `incrementalMount recycler view vertical orientation all items visible`() {
    val lifecycleTracker1: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker2: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker3: LifecycleTracker = LifecycleTracker()

    val CHILD_HEIGHT = 10

    val rcc: Component =
        buildRecyclerCollectionComponent(
            lifecycleTracker1, lifecycleTracker2, lifecycleTracker3, 10, CHILD_HEIGHT)

    // Set LithoView with height so that it can fully show all the items
    lithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(CHILD_HEIGHT * 15, SizeSpec.EXACTLY))
        .measure()
        .layout()

    // All 3 children are visible 5 times, so we should see ON_MOUNT being called 5 times
    // for each child
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(5)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(5)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(5)
  }

  @Test
  fun `incrementalMount recycler view vertical orientation nothing in visible bounds`() {
    val lifecycleTracker1: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker2: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker3: LifecycleTracker = LifecycleTracker()

    val CHILD_HEIGHT = 10

    val rcc: Component =
        buildRecyclerCollectionComponent(
            lifecycleTracker1, lifecycleTracker2, lifecycleTracker3, 10, CHILD_HEIGHT)

    // Set LithoView with height so that it can fully show all the items
    lithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(0, SizeSpec.EXACTLY))
        .measure()
        .layout()

    // SizeSpec height is 0, so nothing is visible and we should see that ON_MOUNT is not called.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(0)
  }

  @Test
  fun `incrementalMount recycler view vertical orientation some items completely in visible bounds`() {
    val lifecycleTracker1: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker2: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker3: LifecycleTracker = LifecycleTracker()

    val CHILD_HEIGHT = 10

    val rcc: Component =
        buildRecyclerCollectionComponent(
            lifecycleTracker1, lifecycleTracker2, lifecycleTracker3, 10, CHILD_HEIGHT)

    // Set LithoView with height so that it can fully show all the items
    lithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(CHILD_HEIGHT * 9, SizeSpec.EXACTLY))
        .measure()
        .layout()

    // All 3 children are visible 3 times, so we should see ON_MOUNT being called 3 times
    // for each child
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(3)
  }

  @Test
  fun `incrementalMount recycler view vertical orientation some items partially in visible bounds`() {
    val lifecycleTracker1: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker2: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker3: LifecycleTracker = LifecycleTracker()

    val CHILD_HEIGHT = 10

    val rcc: Component =
        buildRecyclerCollectionComponent(
            lifecycleTracker1, lifecycleTracker2, lifecycleTracker3, 10, CHILD_HEIGHT)

    // Set LithoView with height so that it can fully show all the items
    lithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(CHILD_HEIGHT * 8 + (CHILD_HEIGHT / 2), SizeSpec.EXACTLY))
        .measure()
        .layout()

    // All 3 children are visible 3 times, so we should see ON_MOUNT being called 3 times
    // for each child
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(3)
  }

  @Test
  fun `incrementalMount recycler view vertical orientation some items in visible bounds scroll up and down`() {
    val lifecycleTracker1: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker2: LifecycleTracker = LifecycleTracker()
    val lifecycleTracker3: LifecycleTracker = LifecycleTracker()
    val CHILD_HEIGHT = 10

    val rcc: Component =
        buildRecyclerCollectionComponent(
            lifecycleTracker1, lifecycleTracker2, lifecycleTracker3, 10, CHILD_HEIGHT)

    // Set LithoView with height so that it can fully show exactly 3 items (3 children per item).
    lithoViewRule
        .setRoot(rcc)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(CHILD_HEIGHT * 9, SizeSpec.EXACTLY))
        .measure()
        .layout()

    // Obtain the RV for scrolling later
    val recyclerView = (lithoViewRule.lithoView.getChildAt(0) as SectionsRecyclerView).recyclerView

    // All 3 children are visible 3 times, so we should see ON_MOUNT being called 3 times
    // for each child
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(3)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(3)

    // Clear the lifecycle steps
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    // Scroll down by the size of 1 child. We are expecting to top item's child1 to be
    // unmounted, and a new bottom item's child1 to be mounted.
    recyclerView.scrollBy(0, CHILD_HEIGHT)

    // Ensure unmount is called once
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_UNMOUNT)).isEqualTo(1)

    // Ensure mount is called once
    // When using Litho's inc-mount, the exiting item will be mounted twice due to an issue with
    // the calculation there. Inc-mount-ext does not have this issue.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(1)

    // child2 & 3 of all items should not change.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_UNMOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_UNMOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(0)

    // Clear the lifecycle steps
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()

    // Scroll up by the size of 1 component. We are expecting to top item's child1 to be mounted,
    // and the bottom item to be unmounted
    recyclerView.scrollBy(0, -CHILD_HEIGHT)

    // Ensure unmount is called once
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_UNMOUNT)).isEqualTo(1)

    // Ensure mount is called once
    // When using Litho's inc-mount, the item we previously expected to exit is still there, so
    // we don't expect a mount to occur.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker1.steps, ON_MOUNT)).isEqualTo(1)

    // child2 & 3 of all items should not change.
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_UNMOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker2.steps, ON_MOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_UNMOUNT)).isEqualTo(0)
    assertThat(getCountOfLifecycleSteps(lifecycleTracker3.steps, ON_MOUNT)).isEqualTo(0)
  }
}

/** Returns the amount of steps that match the given step in the given list of steps */
fun getCountOfLifecycleSteps(steps: List<LifecycleStep>, step: LifecycleStep): Int {
  var count = 0
  for (i in steps.indices) {
    if (steps[i] == step) {
      count++
    }
  }
  return count
}
