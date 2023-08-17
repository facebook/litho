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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentWithSizeSpecWithMeasureCall
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
import com.facebook.litho.widget.NestedTreeComponentSpec.ExtraProps
import com.facebook.litho.widget.RootComponentWithTreeProps
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class NestedTreeResolutionTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun before() {
    ComponentsConfiguration.isEndToEndTestRun = true
  }

  @Test
  fun onRenderComponentWithSizeSpec_shouldContainNestTreeHolderAndNestedProps() {
    val c = legacyLithoViewRule.context
    val component = RootComponentWithTreeProps.create(c).build()
    legacyLithoViewRule.attachToWindow().setSizePx(100, 100).measure().setRoot(component).layout()

    // At the end of layout calculation, the render & layout context container should be null
    assertThat(c.calculationStateContext).isNull()
    val root = legacyLithoViewRule.currentRootNode
    assertThat(root).isNotNull
    assertThat(root?.getChildAt(1)).isInstanceOf(NestedTreeHolderResult::class.java)
    val holder = root?.getChildAt(1) as NestedTreeHolderResult
    assertThat((holder.node as NestedTreeHolder).mNestedTreePadding?.get(YogaEdge.ALL))
        .isEqualTo(5.0f)
    assertThat(holder.nestedResult?.paddingTop).isEqualTo(5)
  }

  @Test
  fun onRenderComponentWithSizeSpecWithoutReuse_shouldCallOnCreateLayoutTwice() {
    val c = legacyLithoViewRule.context
    val component = RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build()
    val props = ExtraProps()
    props.steps = ArrayList()
    legacyLithoViewRule
        .setTreeProp(ExtraProps::class.java, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout()
    val root = legacyLithoViewRule.currentRootNode
    assertThat(root).isNotNull
    assertThat(root?.getChildAt(1)).isInstanceOf(NestedTreeHolderResult::class.java)
    assertThat(props.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
  }

  @Test
  fun onRenderComponentWithSizeSpec_shouldNotTransferLayoutDirectionIfExplicitlySet() {
    val c = legacyLithoViewRule.context
    val component = RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build()
    val props = ExtraProps()
    props.steps = ArrayList()
    props.mDirection = YogaDirection.RTL
    legacyLithoViewRule
        .setTreeProp(ExtraProps::class.java, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout()
    val root = legacyLithoViewRule.currentRootNode
    assertThat(root).isNotNull
    assertThat(root?.getChildAt(1)).isInstanceOf(NestedTreeHolderResult::class.java)
    val holder = root?.getChildAt(1) as NestedTreeHolderResult
    assertThat(holder.yogaNode.layoutDirection).isEqualTo(YogaDirection.LTR)
    assertThat(holder.nestedResult?.yogaNode?.layoutDirection).isEqualTo(YogaDirection.RTL)
  }

  @Test
  fun onRenderComponentWithSizeSpec_shouldTransferLayoutDirectionIfNotExplicitlySet() {
    val c = legacyLithoViewRule.context
    val component =
        RootComponentWithTreeProps.create(c)
            .shouldNotUpdateState(true)
            .layoutDirection(YogaDirection.RTL)
            .build()
    val props = ExtraProps()
    props.steps = ArrayList()
    legacyLithoViewRule
        .setTreeProp(ExtraProps::class.java, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout()
    val root = legacyLithoViewRule.currentRootNode
    assertThat(root).isNotNull
    assertThat(root?.getChildAt(1)).isInstanceOf(NestedTreeHolderResult::class.java)
    val holder = root?.getChildAt(1) as NestedTreeHolderResult
    assertThat(holder.yogaNode.layoutDirection).isEqualTo(YogaDirection.RTL)
    assertThat(holder.nestedResult?.yogaNode?.layoutDirection).isEqualTo(YogaDirection.RTL)
  }

  @Test
  fun onReRenderComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    val c = legacyLithoViewRule.context
    val info0: MutableList<StepInfo> = ArrayList()
    val tracker0 = LifecycleTracker()
    val mountable0 = MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker0).build()
    val root0 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(LayoutWithSizeSpecLifecycleTester.create(c).steps(info0).body(mountable0))
            .child(Text.create(c).text("Hello World"))
            .build()
    legacyLithoViewRule.setRoot(root0).attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED, // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    info0.clear()
    tracker0.reset()
    val info1: List<StepInfo> = ArrayList()
    val tracker1 = LifecycleTracker()
    val mountable1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker1)
            .shouldUpdate(false)
            .build()
    val root1 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(LayoutWithSizeSpecLifecycleTester.create(c).steps(info1).body(mountable1))
            .child(Text.create(c).text("Hello World"))
            .build()
    legacyLithoViewRule.setRoot(root1)
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE, // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_BIND)
  }

  @Test
  fun onReRenderComponentWithSizeSpecInsideComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    val c = legacyLithoViewRule.context
    val info0: MutableList<StepInfo> = ArrayList()
    val tracker0 = LifecycleTracker()
    val info0_nested: MutableList<StepInfo> = ArrayList()
    val mountable0 = MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker0).build()
    val layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info0_nested).body(mountable0).build()
    val root0 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c).steps(info0).body(layoutWithSizeSpec))
            .child(Text.create(c).text("Hello World"))
            .build()
    legacyLithoViewRule.setRoot(root0).attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(LifecycleStep.getSteps(info0_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED, // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    info0.clear()
    info0_nested.clear()
    tracker0.reset()
    val info1: List<StepInfo> = ArrayList()
    val tracker1 = LifecycleTracker()
    val info1_nested: List<StepInfo> = ArrayList()
    val mountable1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker1)
            .shouldUpdate(false)
            .build()
    val layoutWithSizeSpec1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info1_nested).body(mountable1).build()
    val root1 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c).steps(info1).body(layoutWithSizeSpec1))
            .child(Text.create(c).text("Hello World"))
            .build()
    legacyLithoViewRule.setRoot(root1)
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(LifecycleStep.getSteps(info1_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE, // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_BIND)
  }

  @Test
  fun onReRenderSimpleComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    val c = legacyLithoViewRule.context
    val info0: MutableList<StepInfo> = ArrayList()
    val tracker0 = LifecycleTracker()
    val mountable0 = MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker0).build()
    val root0 = LayoutWithSizeSpecLifecycleTester.create(c).steps(info0).body(mountable0).build()
    legacyLithoViewRule.setRoot(root0).attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED, // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    info0.clear()
    tracker0.reset()
    val info1: MutableList<StepInfo> = ArrayList()
    val tracker1 = LifecycleTracker()
    val mountable1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker1)
            .shouldUpdate(false)
            .build()
    val root1 = LayoutWithSizeSpecLifecycleTester.create(c).steps(info1).body(mountable1).build()
    legacyLithoViewRule.setRoot(root1)
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_BIND)
    info1.clear()
    tracker1.reset()
    val info2: List<StepInfo> = ArrayList()
    val tracker2 = LifecycleTracker()
    val mountable2 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker2)
            .shouldUpdate(true)
            .build()
    val root2 = LayoutWithSizeSpecLifecycleTester.create(c).steps(info2).body(mountable2).build()
    legacyLithoViewRule.setRoot(root2)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT /* Because shouldUpdate returns true */)
    assertThat(LifecycleStep.getSteps(info2))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker2.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            LifecycleStep.ON_MEASURE, // Because shouldUpdate returns true
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_MOUNT, // Because shouldUpdate returns true
            LifecycleStep.ON_BIND)
  }

  @Test
  fun onReRenderSimpleComponentWithSizeSpecInsideComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    val c = legacyLithoViewRule.context
    val info0: MutableList<StepInfo> = ArrayList()
    val info0_nested: MutableList<StepInfo> = ArrayList()
    val tracker0 = LifecycleTracker()
    val mountable0 = MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker0).build()
    val layoutWithSizeSpec0 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info0_nested).body(mountable0).build()
    val root0 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info0).body(layoutWithSizeSpec0).build()
    legacyLithoViewRule.setRoot(root0).attachToWindow().measure().layout()
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(LifecycleStep.getSteps(info0_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE, // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED, // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    info0.clear()
    info0_nested.clear()
    tracker0.reset()
    val info1: MutableList<StepInfo> = ArrayList()
    val info1_nested: MutableList<StepInfo> = ArrayList()
    val tracker1 = LifecycleTracker()
    val mountable1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker1)
            .shouldUpdate(false)
            .build()
    val layoutWithSizeSpec1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info1_nested).body(mountable1).build()
    val root1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info1).body(layoutWithSizeSpec1).build()
    legacyLithoViewRule.setRoot(root1)
    assertThat(LifecycleStep.getSteps(info0))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(LifecycleStep.getSteps(info0_nested))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker0.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(LifecycleStep.getSteps(info1_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_BIND)
    info1.clear()
    info1_nested.clear()
    tracker1.reset()
    val info2: List<StepInfo> = ArrayList()
    val info2nested: List<StepInfo> = ArrayList()
    val tracker2 = LifecycleTracker()
    val mountable2 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker2)
            .shouldUpdate(true)
            .build()
    val layoutWithSizeSpec2 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info2nested).body(mountable2).build()
    val root2 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info2).body(layoutWithSizeSpec2).build()
    legacyLithoViewRule.setRoot(root2)
    assertThat(LifecycleStep.getSteps(info1))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(LifecycleStep.getSteps(info1_nested))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty()
    assertThat(tracker1.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT /* Because shouldUpdate returns true */)
    assertThat(LifecycleStep.getSteps(info2))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(LifecycleStep.getSteps(info2nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(tracker2.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            LifecycleStep.ON_MEASURE, // Because shouldUpdate returns true
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED, // Mount phase
            LifecycleStep.ON_MOUNT, // Because shouldUpdate returns true
            LifecycleStep.ON_BIND)
  }

  @Test
  fun LayoutWithSizeSpecUsingMeasureAPIShouldMountCorrectly() {
    val c = legacyLithoViewRule.context
    val text_component = Text.create(c).text("sample_text").build()
    val root =
        ComponentWithSizeSpecWithMeasureCall.create(c)
            .component(text_component)
            .shouldCacheResult(true)
            .build()
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.attachToWindow().measure().layout()
    ComponentTestHelper.mountComponent(c, root)
    assertThat(legacyLithoViewRule.findViewWithText("sample_text")).isNotNull
  }

  /*
          A
            \
              B

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  fun measureLayoutUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringOCLComponent(
            c, stepsInfo, mountableLifecycleTracker, widthSpec, heightSpec)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val steps = LifecycleStep.getSteps(stepsInfo)

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  /*
          A
            \
              B

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  fun measureLayoutUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringOCLComponent(
            c, stepsInfo, mountableLifecycleTracker, widthSpec, heightSpec)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val steps = LifecycleStep.getSteps(stepsInfo)
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutWithSizeSpecLifecycleTester) is OCLWSS and has a mountable as a child C
  */
  @Test
  fun measureLayoutWithSizeSpecAsRootUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringLayoutWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker, widthSpec, heightSpec)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            // OCLWSS gets resolved in measure phase
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    // we are resolving OCLWSS in measure phase it
    // gets resolved only once
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutWithSizeSpecLifecycleTester) is OCLWSS and has a mountable as a child C
  */
  @Test
  fun measureLayoutWithSizeSpecAsRootUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringLayoutWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker, widthSpec, heightSpec)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(widthSpec, heightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)

    // Width and height specs are same, cache is reused and we don't see second OCLWSS call
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            // OCLWSS gets resolved in measure phase
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL and has a child Component C which is OCLWSS
  */
  @Test
  fun measureLayoutWithSizeSpecAsChildUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringLayoutWithSizeSpecAsChild(
            c,
            stepsInfo,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    val steps = LifecycleStep.getSteps(stepsInfo)

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)

    // Since OCLWSS is child of OCL which is being measured, we do not see ON_CREATE_INITIAL_STATE
    // called twice. OCL will get another ON_CREATE_INITIAL_STATE call after global key changes and
    // creates NestedTreeHolder for OCLWSS
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL and has a child Component C which is OCLWSS
  */
  @Test
  fun measureLayoutWithSizeSpecAsChildUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponentMeasuringLayoutWithSizeSpecAsChild(
            c,
            stepsInfo,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    val steps = LifecycleStep.getSteps(stepsInfo)
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B

    A (ComponentWithSizeSpecMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  fun measureLayoutUsingMeasureApiInMeasure() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val root =
        createComponentWithSizeSpecMeasuringOCLComponent(c, stepsInfo, mountableLifecycleTracker)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val steps = LifecycleStep.getSteps(stepsInfo)

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (LayoutWithSizeSpecLifecycleTester) is OCLWSS and has a mountable as a child C
  */
  @Test
  fun measureLayoutWithSizeSpecAsRootUsingMeasureApiInMeasure() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val root =
        createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            // OCLWSS gets resolved in measure phase
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    // we are resolving OCLWSS in measure phase it
    // gets resolved only once
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL and has a child Component C which is OCLWSS
  */
  @Test
  fun measureLayoutWithSizeSpecAsChildUsingMeasureApiInMeasure() {
    val c = legacyLithoViewRule.context
    val stepsInfo: List<StepInfo> = ArrayList()
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val root =
        createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsChild(
            c, stepsInfo, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val layoutWithSizeSpecSteps = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    val steps = LifecycleStep.getSteps(stepsInfo)

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentUsingMeasureApiInsideAnotherMeasureCallWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            true,
            true)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentUsingMeasureApiInsideAnotherMeasureCallWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val lifecycleStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val parentWidthSpec = exactly(500)
    val parentHeightSpec = exactly(500)
    val childWidthSpec = exactly(600)
    val childHeightSpec = exactly(600)
    val root =
        createComponent(
            c,
            lifecycleStepsInfo,
            mountableLifecycleTracker,
            parentWidthSpec,
            parentHeightSpec,
            childWidthSpec,
            childHeightSpec,
            true,
            true,
            true)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(lifecycleStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecUsingMeasureApiInsideAnotherMeasureCallWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            true,
            false)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecUsingMeasureApiInsideAnotherMeasureCallWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            true,
            false)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecInsideLayoutWithSizeSpecMeasuringOCLWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            false,
            true)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecInsideLayoutWithSizeSpecMeasuringOCLWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            false,
            true)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentInsideLayoutWithSizeSpecWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            true,
            true)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun measureComponentInsideLayoutWithSizeSpecWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            true,
            true)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecWhichMeasuresAnotherComponentWithSizeSpecWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            false,
            false)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun measureComponentWithSizeSpecWhichMeasuresAnotherComponentWithSizeSpecWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            true,
            false,
            false)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun componentWithSizeSpecMeasuringOCLWhichMeasuresAnotherComponentWithSizeSpecWithCompatibleSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            true,
            false)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithMeasureCall) is OCL which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun componentWithSizeSpecMeasuringOCLWhichMeasuresAnotherComponentWithSizeSpecWithDifferentSizeSpecs() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            true,
            false)
    val lithoViewWidthSpec = exactly(300)
    val lithoViewHeightSpec = exactly(300)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutSpecLifecycleTester) is OCL which has a mountable as a child
  */
  @Test
  fun componentWithSizeSpecMeasuringComponentWithSizeSpecWhichMeasuresOCL() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            false,
            true)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_EVENT_VISIBLE,
            LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
            LifecycleStep.ON_VISIBILITY_CHANGED)
    assertThat(mountableLifecycleTracker.steps)
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

  /*
          A
            \
              B
                \
                  C

    A (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (ComponentWithSizeSpecWithMeasureCall) is OCLWSS which  calls Component.measure on Component C
    Component C (LayoutWithSizeSpecLifecycleTester) is OCLWSS which has a mountable as a child
  */
  @Test
  fun componentWithSizeSpecMeasuringOCLWSSWhichMeasuresAnotherOCLWSS() {
    val c = legacyLithoViewRule.context
    val layoutWithSizeSpecStepsInfo: List<StepInfo> = ArrayList()
    val mountableLifecycleTracker = LifecycleTracker()
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)
    val root =
        createComponent(
            c,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec,
            widthSpec,
            heightSpec,
            false,
            false,
            false)
    val lithoViewWidthSpec = exactly(500)
    val lithoViewHeightSpec = exactly(500)
    legacyLithoViewRule.setRoot(root)
    legacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()
    val stepsInfo = LifecycleStep.getSteps(layoutWithSizeSpecStepsInfo)
    assertThat(stepsInfo)
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    assertThat(mountableLifecycleTracker.steps)
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

  companion object {
    private fun createComponentMeasuringOCLComponent(
        c: ComponentContext,
        stepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker,
        widthSpec: Int,
        heightSpec: Int
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val component_OCL =
          LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build()
      return ComponentWithMeasureCall.create(c)
          .component(component_OCL)
          .widthSpec(widthSpec)
          .heightSpec(heightSpec)
          .shouldCacheResult(true)
          .build()
    }

    private fun createComponentMeasuringLayoutWithSizeSpecAsRoot(
        c: ComponentContext,
        layoutWithSizeSpecStepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker,
        widthSpec: Int,
        heightSpec: Int
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val layoutWithSizeSpec =
          LayoutWithSizeSpecLifecycleTester.create(c)
              .steps(layoutWithSizeSpecStepsInfo)
              .body(mountable)
              .build()
      return ComponentWithMeasureCall.create(c)
          .component(layoutWithSizeSpec)
          .widthSpec(widthSpec)
          .heightSpec(heightSpec)
          .shouldCacheResult(true)
          .build()
    }

    private fun createComponentMeasuringLayoutWithSizeSpecAsChild(
        c: ComponentContext,
        stepsInfo: List<StepInfo>,
        layoutWithSizeSpecStepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker,
        widthSpec: Int,
        heightSpec: Int
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val layoutWithSizeSpec =
          LayoutWithSizeSpecLifecycleTester.create(c)
              .steps(layoutWithSizeSpecStepsInfo)
              .body(mountable)
              .build()
      val component_OCL =
          LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(layoutWithSizeSpec).build()
      return ComponentWithMeasureCall.create(c)
          .component(component_OCL)
          .widthSpec(widthSpec)
          .heightSpec(heightSpec)
          .shouldCacheResult(true)
          .build()
    }

    private fun createComponentWithSizeSpecMeasuringOCLComponent(
        c: ComponentContext,
        stepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val component_OCL =
          LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build()
      return ComponentWithSizeSpecWithMeasureCall.create(c)
          .component(component_OCL)
          .shouldCacheResult(true)
          .build()
    }

    private fun createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsRoot(
        c: ComponentContext,
        layoutWithSizeSpecStepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val layoutWithSizeSpec =
          LayoutWithSizeSpecLifecycleTester.create(c)
              .steps(layoutWithSizeSpecStepsInfo)
              .body(mountable)
              .build()
      return ComponentWithSizeSpecWithMeasureCall.create(c)
          .component(layoutWithSizeSpec)
          .shouldCacheResult(true)
          .build()
    }

    private fun createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsChild(
        c: ComponentContext,
        stepsInfo: List<StepInfo>,
        layoutWithSizeSpecStepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val layoutWithSizeSpec =
          LayoutWithSizeSpecLifecycleTester.create(c)
              .steps(layoutWithSizeSpecStepsInfo)
              .body(mountable)
              .build()
      val component_OCL =
          LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(layoutWithSizeSpec).build()
      return ComponentWithSizeSpecWithMeasureCall.create(c)
          .component(component_OCL)
          .shouldCacheResult(true)
          .build()
    }

    /**
     * Returns component hierarchy Root -> Mid -> Bottom -> Mountable where Root / Mid / Bottom can
     * be OCL or OCLWSS based on provided params.
     *
     * Root and Mid component are using Component.measure API as well in OCL / OCLWSS
     *
     * @param c ComponentContext
     * @param stepsInfo to track lifecycle events on bottom component
     * @param mountableLifecycleTracker to track lifecycle events on Mountable component
     * @param rootMeasureWidthSpec width spec used by root component to measure mid component using
     *   Component.measure API
     * @param rootMeasureHeightSpec height spec used by root component to measure mid component
     *   using Component.measure API
     * @param midMeasureWidthSpec width spec used by mid component to measure bottom component using
     *   Component.measure API
     * @param midMeasureHeightSpec height spec used by mid component to measure bottom component
     *   using
     * * Component.measure API
     *
     * @param isRootOCL if true, use OCL for root component otherwise OCLWSS
     * @param isMidOCL if true, use OCL for mid component otherwise OCLWSS
     * @param isBottomOCL if true, use OCL for end component otherwise OCLWSS
     * @return
     */
    private fun createComponent(
        c: ComponentContext,
        stepsInfo: List<StepInfo>,
        mountableLifecycleTracker: LifecycleTracker,
        rootMeasureWidthSpec: Int,
        rootMeasureHeightSpec: Int,
        midMeasureWidthSpec: Int,
        midMeasureHeightSpec: Int,
        isRootOCL: Boolean,
        isMidOCL: Boolean,
        isBottomOCL: Boolean
    ): Component {
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val bottomComponent =
          if (isBottomOCL)
              LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build()
          else LayoutWithSizeSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build()
      val midComponent =
          if (isMidOCL)
              ComponentWithMeasureCall.create(c)
                  .component(bottomComponent)
                  .widthSpec(midMeasureWidthSpec)
                  .heightSpec(midMeasureHeightSpec)
                  .shouldCacheResult(true)
                  .build()
          else
              ComponentWithSizeSpecWithMeasureCall.create(c)
                  .component(bottomComponent)
                  .shouldCacheResult(true)
                  .build()
      return if (isRootOCL)
          ComponentWithMeasureCall.create(c)
              .component(midComponent)
              .widthSpec(rootMeasureWidthSpec)
              .heightSpec(rootMeasureHeightSpec)
              .shouldCacheResult(true)
              .build()
      else
          ComponentWithSizeSpecWithMeasureCall.create(c)
              .component(midComponent)
              .shouldCacheResult(true)
              .build()
    }
  }
}
