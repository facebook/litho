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
import com.facebook.litho.testing.LithoStatsRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertion
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertionSpec
import com.facebook.litho.widget.PreallocatedMountSpecLifecycleTester
import com.facebook.litho.widget.RecordsShouldUpdate
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.litho.widget.Text
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.RunnableHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountSpecLifecycleTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @JvmField @Rule val lithoStatsRule: LithoStatsRule = LithoStatsRule()

  @JvmField @Rule val expectedException = ExpectedException.none()

  @Test
  fun lifecycle_onSetComponentWithoutLayout_shouldNotCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component).idle()
    assertThat(lifecycleTracker.steps)
        .describedAs("Only render lifecycle methods should be called")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE)
  }

  @Test
  fun lifecycle_onLayout_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(800, 600))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
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
  }

  @Test
  fun lifecycle_onLayoutWithExactSize_shouldCallLifecycleMethodsExceptMeasure() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(800, 600))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.setSizePx(600, 800).attachToWindow().measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun lifecycle_onDetach_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.detachFromWindow()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND)
  }

  @Test
  fun lifecycle_onReAttach_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(800, 600))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout().detachFromWindow()
    lifecycleTracker.reset()
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND)
  }

  @Test
  fun lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.measure()
    assertThat(lifecycleTracker.steps)
        .describedAs("No lifecycle methods should be called")
        .isEmpty()
  }

  @Test
  fun lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.setSizeSpecs(exactly(800), unspecified()).measure()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
  }

  @Test
  fun lifecycle_onRemeasureWithExactSize_shouldNotCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(800, 600))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.setSizePx(800, 600).measure()
    assertThat(lifecycleTracker.steps)
        .describedAs(
            "No lifecycle methods should be called because EXACT measures should skip layout calculation.")
        .isEmpty()
  }

  @Test
  fun lifecycle_onReLayoutAfterMeasureWithExactSize_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(Size(800, 600))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.setSizePx(800, 600).measure().layout()

    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun lifecycle_onReLayoutAfterMeasureWithExactSizeAsNonRoot_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(Column.create(legacyLithoViewRule.context).child(component).build())
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.setSizePx(800, 600).measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun lifecycle_onSetShallowCopy_shouldNotCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    legacyLithoViewRule.setRoot(component.makeShallowCopy())
    assertThat(lifecycleTracker.steps)
        .describedAs("No lifecycle methods should be called")
        .isEmpty()
  }

  @Test
  fun lifecycle_onRemeasureWithCompatibleSpecs_shouldNotRemount() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component).measure().layout().attachToWindow()
    lifecycleTracker.reset()

    // Force measure call to propagate to ComponentTree
    legacyLithoViewRule.lithoView.requestLayout()
    legacyLithoViewRule.measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("No lifecycle methods should be called because measure was compatible")
        .isEmpty()
  }

  @Test
  fun lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    val lifecycleTracker = LifecycleTracker()
    val component =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    lifecycleTracker.reset()
    val newLifecycleTracker = LifecycleTracker()
    legacyLithoViewRule.setRoot(
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .intrinsicSize(Size(800, 600))
            .lifecycleTracker(newLifecycleTracker)
            .build())
    legacyLithoViewRule.measure().layout()
    assertThat(lifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods on old instance in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT)
    assertThat(newLifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethods() {
    val looper = ShadowLooper.getLooperForThread(Thread.currentThread())
    val tree =
        ComponentTree.create(legacyLithoViewRule.context)
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(RunnableHandler.DefaultHandler(looper))
            .build()
    legacyLithoViewRule.useComponentTree(tree)
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        PreallocatedMountSpecLifecycleTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.componentTree.setRootAndSizeSpecSync(
        component, legacyLithoViewRule.widthSpec, legacyLithoViewRule.heightSpec)
    legacyLithoViewRule.measure()
    ShadowLooper.runUiThreadTasks()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED)
  }

  @Test
  fun onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethodsInRenderCore() {
    val looper = ShadowLooper.getLooperForThread(Thread.currentThread())
    val tree =
        ComponentTree.create(legacyLithoViewRule.context)
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(RunnableHandler.DefaultHandler(looper))
            .build()
    legacyLithoViewRule.useComponentTree(tree)
    val info: List<StepInfo> = ArrayList<StepInfo>()
    val component =
        PreallocatedMountSpecLifecycleTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.componentTree.setRootAndSizeSpecSync(
        component, legacyLithoViewRule.widthSpec, legacyLithoViewRule.heightSpec)
    legacyLithoViewRule.measure()
    ShadowLooper.runUiThreadTasks()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED)
    assertThat(MountItemsPool.getMountItemPools().size)
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1)
  }

  @Test
  fun shouldUpdate_shouldUpdateIsCalled_prevAndNextAreInRightOrder() {
    val firstObject = Any()
    val shouldUpdateCalls: List<Diff<Any>> = ArrayList()
    legacyLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(legacyLithoViewRule.context)
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(firstObject)
                .build())
        .measure()
        .layout()
        .attachToWindow()
    val secondObject = Any()
    legacyLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(legacyLithoViewRule.context)
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(secondObject)
                .build())
        .measure()
        .layout()
    assertThat(shouldUpdateCalls).hasSize(1)
    assertThat(shouldUpdateCalls[0].previous).isEqualTo(firstObject)
    assertThat(shouldUpdateCalls[0].next).isEqualTo(secondObject)
  }

  /*
   * This case comes from a specific bug where when we shallow-copy components (which we do when we
   * update state) we were setting mHasManualKey to false even if there was a manual key which would
   * cause us to generate different keys between layouts.
   */
  @Test
  fun lifecycle_stateUpdateWithMultipleChildrenOfSameTypeAndManualKeys_doesNotRecreateInitialState() {
    val info_child1 = LifecycleTracker()
    val info_child2 = LifecycleTracker()
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val root =
        Column.create(legacyLithoViewRule.context)
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(800, 600))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(legacyLithoViewRule.context)
                    .intrinsicSize(Size(800, 600))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(legacyLithoViewRule.context).caller(stateUpdater))
            .build()
    legacyLithoViewRule.useComponentTree(
        ComponentTree.create(legacyLithoViewRule.context).isReconciliationEnabled(false).build())
    legacyLithoViewRule.setRoot(root).attachToWindow().measure().layout()
    val mountDelegateTarget = legacyLithoViewRule.lithoView.getMountDelegateTarget()
    assertThat(mountDelegateTarget.getMountItemCount()).isGreaterThan(1)
    info_child1.reset()
    info_child2.reset()
    stateUpdater.increment()
    assertThat(info_child1.steps)
        .describedAs("Should not recreate initial state.")
        .doesNotContain(LifecycleStep.ON_CREATE_INITIAL_STATE)
    assertThat(info_child2.steps)
        .describedAs("Should not recreate initial state.")
        .doesNotContain(LifecycleStep.ON_CREATE_INITIAL_STATE)
  }

  /**
   * This test case captures the scenario where unmount can get called on a component for which
   * mount was never invoked. 1. A layout is mounted. 2. The next layout update does not cause a new
   * mount pass. 3. When the next mount pass is triggered, an item is unmounted as it is out of the
   * view port. 4. The unmount must be called on the old (currently) mounted component.
   */
  @Test
  fun whenItemsAreUmounted_thenUnmountMustbeInvokedOnTheCurrentlyMountedComponent() {
    val c = legacyLithoViewRule.context
    val initialComponent =
        Column.create(c)
            .heightPx(200)
            .child(Text.create(c).text("1").heightPx(100))
            .child(
                MountSpecWithMountUnmountAssertion.create(c)
                    .container(MountSpecWithMountUnmountAssertionSpec.Container())
                    .heightPx(100))
            .build()
    val initialComponentTree = ComponentTree.create(c, initialComponent).build()
    val lithoView = LithoView(c.androidContext)

    // Mount a layout with the component.
    lithoView.componentTree = initialComponentTree
    lithoView.measure(exactly(100), exactly(200))
    lithoView.layout(0, 0, 100, 200)

    // Assert that the view is mounted
    assertThat(lithoView.childCount).isEqualTo(1)
    val newComponent =
        Column.create(c)
            .heightPx(200)
            .child(Text.create(c).text("1").heightPx(100))
            .child(
                MountSpecWithMountUnmountAssertion.create(c)
                    .container(MountSpecWithMountUnmountAssertionSpec.Container())
                    .heightPx(100)
                    .build())
            .build()

    // Create a new component tree so that state is recreated
    // but use the id maps, and component tree id from the initial
    // tree so that new ids match the current one
    val newComponentTree =
        ComponentTree.create(c, newComponent)
            .overrideRenderUnitIdMap(initialComponentTree)
            .overrideComponentTreeId(initialComponentTree.mId)
            .build()
    lithoView.componentTree = newComponentTree

    // Mount a new layout, but with a shorter height, to make the item unmount
    lithoView.measure(exactly(100), exactly(95))
    lithoView.layout(0, 0, 100, 95)

    // Assert that the items is unmounted.
    assertThat(lithoView.childCount).isEqualTo(0)
  }

  @Test
  fun mountTimeLifecycleMethodsShouldBeCalledInExpectedOrder() {
    val root =
        Column.create(legacyLithoViewRule.context)
            .child(
                MountSpecWithMountUnmountAssertion.create(legacyLithoViewRule.context)
                    .viewTag("tag")
                    .hasTagSet(true)
                    .container(MountSpecWithMountUnmountAssertionSpec.Container()))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(root).measure().layout()
    legacyLithoViewRule.lithoView.unmountAllItems()
  }
}
