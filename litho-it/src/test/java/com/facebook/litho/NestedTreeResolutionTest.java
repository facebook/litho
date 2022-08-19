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

package com.facebook.litho;

import static com.facebook.litho.LifecycleStep.ON_ATTACHED;
import static com.facebook.litho.LifecycleStep.ON_BIND;
import static com.facebook.litho.LifecycleStep.ON_BOUNDS_DEFINED;
import static com.facebook.litho.LifecycleStep.ON_CALCULATE_CACHED_VALUE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_INITIAL_STATE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_LAYOUT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC;
import static com.facebook.litho.LifecycleStep.ON_CREATE_MOUNT_CONTENT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_TREE_PROP;
import static com.facebook.litho.LifecycleStep.ON_EVENT_VISIBLE;
import static com.facebook.litho.LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT;
import static com.facebook.litho.LifecycleStep.ON_MEASURE;
import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_PREPARE;
import static com.facebook.litho.LifecycleStep.ON_VISIBILITY_CHANGED;
import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stateupdates.ComponentWithMeasureCall;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentWithSizeSpecWithMeasureCall;
import com.facebook.litho.widget.LayoutSpecLifecycleTester;
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import com.facebook.litho.widget.NestedTreeComponentSpec.ExtraProps;
import com.facebook.litho.widget.RootComponentWithTreeProps;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void before() {
    ComponentsConfiguration.isEndToEndTestRun = true;
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldContainNestTreeHolderAndNestedProps() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final RootComponentWithTreeProps component = RootComponentWithTreeProps.create(c).build();

    mLegacyLithoViewRule.attachToWindow().setSizePx(100, 100).measure().setRoot(component).layout();

    final LithoLayoutResult root = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);

    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);
    assertThat(((NestedTreeHolder) holder.getNode()).mNestedTreePadding.get(YogaEdge.ALL))
        .isEqualTo(5.0f);
    assertThat(holder.getNestedResult().getPaddingTop()).isEqualTo(5);
  }

  @Test
  public void onRenderComponentWithSizeSpecWithoutReuse_shouldCallOnCreateLayoutTwice() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.steps = new ArrayList<>();

    mLegacyLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    assertThat(props.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldNotTransferLayoutDirectionIfExplicitlySet() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.steps = new ArrayList<>();
    props.mDirection = YogaDirection.RTL;

    mLegacyLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.LTR);
    assertThat(holder.getNestedResult().getYogaNode().getLayoutDirection())
        .isEqualTo(YogaDirection.RTL);
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldTransferLayoutDirectionIfNotExplicitlySet() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c)
            .shouldNotUpdateState(true)
            .layoutDirection(YogaDirection.RTL)
            .build();

    final ExtraProps props = new ExtraProps();
    props.steps = new ArrayList<>();

    mLegacyLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.RTL);
    assertThat(holder.getNestedResult().getYogaNode().getLayoutDirection())
        .isEqualTo(YogaDirection.RTL);
  }

  @Test
  public void onReRenderComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();

    final Component root_0 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(LayoutWithSizeSpecLifecycleTester.create(c).steps(info_0).body(mountable_0))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLegacyLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

    assertThat(getSteps(info_0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    info_0.clear();
    tracker_0.reset();

    final List<LifecycleStep.StepInfo> info_1 = new ArrayList<>();
    final LifecycleTracker tracker_1 = new LifecycleTracker();

    final Component mountable_1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_1)
            .shouldUpdate(false)
            .build();

    final Component root_1 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(LayoutWithSizeSpecLifecycleTester.create(c).steps(info_1).body(mountable_1))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLegacyLithoViewRule.setRoot(root_1);

    assertThat(getSteps(info_0)).describedAs("Should not call any lifecycle methods.").isEmpty();

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND);

    assertThat(getSteps(info_1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE,
            // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_BIND);
  }

  @Test
  public void
      onReRenderComponentWithSizeSpecInsideComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final List<LifecycleStep.StepInfo> info_0_nested = new ArrayList<>();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();
    final Component layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_0_nested).body(mountable_0).build();

    final Component root_0 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c).steps(info_0).body(layoutWithSizeSpec))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLegacyLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

    assertThat(getSteps(info_0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(getSteps(info_0_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    info_0.clear();
    info_0_nested.clear();
    tracker_0.reset();

    final List<LifecycleStep.StepInfo> info_1 = new ArrayList<>();
    final LifecycleTracker tracker_1 = new LifecycleTracker();

    final List<LifecycleStep.StepInfo> info_1_nested = new ArrayList<>();

    final Component mountable_1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_1)
            .shouldUpdate(false)
            .build();

    final Component layoutWithSizeSpec_1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_1_nested).body(mountable_1).build();

    final Component root_1 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info_1)
                    .body(layoutWithSizeSpec_1))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLegacyLithoViewRule.setRoot(root_1);

    assertThat(getSteps(info_0)).describedAs("Should not call any lifecycle methods.").isEmpty();

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND);

    assertThat(getSteps(info_1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(getSteps(info_1_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE,
            // Nested tree resolution from collect results
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_BIND);
  }

  @Test
  public void onReRenderSimpleComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();

    final Component root_0 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_0).body(mountable_0).build();

    mLegacyLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

    assertThat(getSteps(info_0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    info_0.clear();
    tracker_0.reset();

    final List<LifecycleStep.StepInfo> info_1 = new ArrayList<>();
    final LifecycleTracker tracker_1 = new LifecycleTracker();

    final Component mountable_1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_1)
            .shouldUpdate(false)
            .build();

    final Component root_1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_1).body(mountable_1).build();

    mLegacyLithoViewRule.setRoot(root_1);

    assertThat(getSteps(info_0)).describedAs("Should not call any lifecycle methods.").isEmpty();

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND);

    assertThat(getSteps(info_1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_BIND);

    info_1.clear();
    tracker_1.reset();

    final List<LifecycleStep.StepInfo> info_2 = new ArrayList<>();
    final LifecycleTracker tracker_2 = new LifecycleTracker();

    final Component mountable_2 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_2)
            .shouldUpdate(true)
            .build();

    final Component root_2 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_2).body(mountable_2).build();

    mLegacyLithoViewRule.setRoot(root_2);

    assertThat(getSteps(info_1)).describedAs("Should not call any lifecycle methods.").isEmpty();

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT /* Because shouldUpdate returns true */);

    assertThat(getSteps(info_2))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_2.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            LifecycleStep.ON_MEASURE, // Because shouldUpdate returns true
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_MOUNT, // Because shouldUpdate returns true
            LifecycleStep.ON_BIND);
  }

  @Test
  public void
      onReRenderSimpleComponentWithSizeSpecInsideComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final List<LifecycleStep.StepInfo> info_0_nested = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();

    final Component layoutWithSizeSpec_0 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_0_nested).body(mountable_0).build();

    final Component root_0 =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_0)
            .body(layoutWithSizeSpec_0)
            .build();

    mLegacyLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

    assertThat(getSteps(info_0))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(getSteps(info_0_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            // Mount phase
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    info_0.clear();
    info_0_nested.clear();
    tracker_0.reset();

    final List<LifecycleStep.StepInfo> info_1 = new ArrayList<>();
    final List<LifecycleStep.StepInfo> info_1_nested = new ArrayList<>();
    final LifecycleTracker tracker_1 = new LifecycleTracker();

    final Component mountable_1 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_1)
            .shouldUpdate(false)
            .build();

    final Component layoutWithSizeSpec_1 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_1_nested).body(mountable_1).build();

    final Component root_1 =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_1)
            .body(layoutWithSizeSpec_1)
            .build();

    mLegacyLithoViewRule.setRoot(root_1);

    assertThat(getSteps(info_0)).describedAs("Should not call any lifecycle methods.").isEmpty();
    assertThat(getSteps(info_0_nested))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty();

    assertThat(tracker_0.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND);

    assertThat(getSteps(info_1))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(getSteps(info_1_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_BIND);

    info_1.clear();
    info_1_nested.clear();
    tracker_1.reset();

    final List<LifecycleStep.StepInfo> info_2 = new ArrayList<>();
    final List<LifecycleStep.StepInfo> info_2_nested = new ArrayList<>();
    final LifecycleTracker tracker_2 = new LifecycleTracker();

    final Component mountable_2 =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(tracker_2)
            .shouldUpdate(true)
            .build();

    final Component layoutWithSizeSpec_2 =
        LayoutWithSizeSpecLifecycleTester.create(c).steps(info_2_nested).body(mountable_2).build();

    final Component root_2 =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_2)
            .body(layoutWithSizeSpec_2)
            .build();

    mLegacyLithoViewRule.setRoot(root_2);

    assertThat(getSteps(info_1)).describedAs("Should not call any lifecycle methods.").isEmpty();
    assertThat(getSteps(info_1_nested))
        .describedAs("Should not call any lifecycle methods.")
        .isEmpty();

    assertThat(tracker_1.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT /* Because shouldUpdate returns true */);

    assertThat(getSteps(info_2))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(getSteps(info_2_nested))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(tracker_2.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE, // Called during layout diffing,
            LifecycleStep.ON_MEASURE, // Because shouldUpdate returns true
            // Collect results phase
            LifecycleStep.ON_BOUNDS_DEFINED,
            // Mount phase
            LifecycleStep.ON_MOUNT, // Because shouldUpdate returns true
            LifecycleStep.ON_BIND);
  }

  @Test
  public void LayoutWithSizeSpecUsingMeasureAPIShouldMountCorrectly() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component text_component = Text.create(c).text("sample_text").build();
    final Component root =
        ComponentWithSizeSpecWithMeasureCall.create(c)
            .component(text_component)
            .shouldCacheResult(true)
            .build();

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.attachToWindow().measure().layout();
    ComponentTestHelper.mountComponent(c, root);

    assertThat(mLegacyLithoViewRule.findViewWithText("sample_text")).isNotNull();
  }

  /*
          A
            \
              B

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  public void measureLayoutUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringOCLComponent(
            c, stepsInfo, mountableLifecycleTracker, widthSpec, heightSpec);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> steps = getSteps(stepsInfo);

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            // Keys are different when we use Component.measure on component and return the same
            // component which causes ON_CREATE_INITIAL_STATE to be called twice
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Size Specs are different so we can remeasure the same LithoNode from cached layout
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  /*
          A
            \
              B

    A (ComponentWithMeasureCall) is OCL which calls Component.measure on Component B passed as prop and simply returns the same component

    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  public void measureLayoutUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringOCLComponent(
            c, stepsInfo, mountableLifecycleTracker, widthSpec, heightSpec);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> steps = getSteps(stepsInfo);

    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  private static Component createComponentMeasuringOCLComponent(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> stepsInfo,
      final LifecycleTracker mountableLifecycleTracker,
      final int widthSpec,
      final int heightSpec) {
    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component component_OCL =
        LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build();

    return ComponentWithMeasureCall.create(c)
        .component(component_OCL)
        .widthSpec(widthSpec)
        .heightSpec(heightSpec)
        .shouldCacheResult(true)
        .build();
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
  public void measureLayoutWithSizeSpecAsRootUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringLayoutWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker, widthSpec, heightSpec);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);

    if (ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure) {
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC); // OCLWSS gets resolved in measure phase, since
      // we are resolving OCLWSS in measure phase it
      // gets resolved only once

      assertThat(mountableLifecycleTracker.getSteps())
          .describedAs("Should call the lifecycle methods in expected order")
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              // Collect results phase
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              // Mount phase
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND);
    } else {
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC); // Width and height specs are different, cache
      // is not reused

      assertThat(mountableLifecycleTracker.getSteps())
          .describedAs("Should call the lifecycle methods in expected order")
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              // Nested tree resolution
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              // Collect results phase
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              // Mount phase
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND);
    }
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
  public void measureLayoutWithSizeSpecAsRootUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringLayoutWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker, widthSpec, heightSpec);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(widthSpec, heightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);

    if (ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure) {
      // Width and height specs are same, cache is reused and we don't see second OCLWSS call
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC); // OCLWSS gets resolved in measure phase,since
      // we are resolving OCLWSS in measure phase it
      // gets resolved only once
    } else {
      // Width and height specs are same, cache is reused and we don't see second OCLWSS call
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE, ON_CREATE_LAYOUT_WITH_SIZE_SPEC, ON_CREATE_INITIAL_STATE);
    }

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  private static Component createComponentMeasuringLayoutWithSizeSpecAsRoot(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo,
      final LifecycleTracker mountableLifecycleTracker,
      final int widthSpec,
      final int heightSpec) {

    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(layoutWithSizeSpecStepsInfo)
            .body(mountable)
            .build();

    return ComponentWithMeasureCall.create(c)
        .component(layoutWithSizeSpec)
        .widthSpec(widthSpec)
        .heightSpec(heightSpec)
        .shouldCacheResult(true)
        .build();
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
  public void measureLayoutWithSizeSpecAsChildUsingMeasureApiInRenderWithDifferentSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringLayoutWithSizeSpecAsChild(
            c,
            stepsInfo,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);

    List<LifecycleStep> steps = getSteps(stepsInfo);

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    // Since OCLWSS is child of OCL which is being measured, we do not see ON_CREATE_INITIAL_STATE
    // called twice. OCL will get another ON_CREATE_INITIAL_STATE call after global key changes and
    // creates NestedTreeHolder for OCLWSS
    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Nested tree resolution
            ON_CREATE_TREE_PROP,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
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
  public void measureLayoutWithSizeSpecAsChildUsingMeasureApiInRenderWithCompatibleSizeSpecs() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    final Component root =
        createComponentMeasuringLayoutWithSizeSpecAsChild(
            c,
            stepsInfo,
            layoutWithSizeSpecStepsInfo,
            mountableLifecycleTracker,
            widthSpec,
            heightSpec);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);

    List<LifecycleStep> steps = getSteps(stepsInfo);

    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(ON_CREATE_INITIAL_STATE, ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  private static Component createComponentMeasuringLayoutWithSizeSpecAsChild(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> stepsInfo,
      final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo,
      final LifecycleTracker mountableLifecycleTracker,
      final int widthSpec,
      final int heightSpec) {

    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(layoutWithSizeSpecStepsInfo)
            .body(mountable)
            .build();

    final Component component_OCL =
        LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(layoutWithSizeSpec).build();

    return ComponentWithMeasureCall.create(c)
        .component(component_OCL)
        .widthSpec(widthSpec)
        .heightSpec(heightSpec)
        .shouldCacheResult(true)
        .build();
  }

  /*
          A
            \
              B

    A (ComponentWithSizeSpecMeasureCall) is OCLWSS which calls Component.measure on Component B passed as prop and simply returns the same component
    Component B (LayoutSpecLifecycleTester) is OCL
  */
  @Test
  public void measureLayoutUsingMeasureApiInMeasure() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    final Component root =
        createComponentWithSizeSpecMeasuringOCLComponent(c, stepsInfo, mountableLifecycleTracker);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> steps = getSteps(stepsInfo);

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  private static Component createComponentWithSizeSpecMeasuringOCLComponent(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> stepsInfo,
      final LifecycleTracker mountableLifecycleTracker) {
    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component component_OCL =
        LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(mountable).build();

    return ComponentWithSizeSpecWithMeasureCall.create(c)
        .component(component_OCL)
        .shouldCacheResult(true)
        .build();
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
  public void measureLayoutWithSizeSpecAsRootUsingMeasureApiInMeasure() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    final Component root =
        createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsRoot(
            c, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);

    if (ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure) {
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC); // OCLWSS gets resolved in measure phase, since
      // we are resolving OCLWSS in measure phase it
      // gets resolved only once

      assertThat(mountableLifecycleTracker.getSteps())
          .describedAs("Should call the lifecycle methods in expected order")
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              // Collect results phase
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              // Mount phase
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND);
    } else {
      assertThat(layoutWithSizeSpecSteps)
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE); // Width and height specs are different, cache
      // is not reused

      assertThat(mountableLifecycleTracker.getSteps())
          .describedAs("Should call the lifecycle methods in expected order")
          .containsExactly(
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              // Collect results phase
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              // Mount phase
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND);
    }
  }

  private static Component createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsRoot(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo,
      final LifecycleTracker mountableLifecycleTracker) {
    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(layoutWithSizeSpecStepsInfo)
            .body(mountable)
            .build();

    return ComponentWithSizeSpecWithMeasureCall.create(c)
        .component(layoutWithSizeSpec)
        .shouldCacheResult(true)
        .build();
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
  public void measureLayoutWithSizeSpecAsChildUsingMeasureApiInMeasure() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> stepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    final Component root =
        createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsChild(
            c, stepsInfo, layoutWithSizeSpecStepsInfo, mountableLifecycleTracker);

    int lithoViewWidthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    int lithoViewHeightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    mLegacyLithoViewRule.setRoot(root);
    mLegacyLithoViewRule.setSizeSpecs(lithoViewWidthSpec, lithoViewHeightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    List<LifecycleStep> layoutWithSizeSpecSteps = getSteps(layoutWithSizeSpecStepsInfo);
    List<LifecycleStep> steps = getSteps(stepsInfo);

    // Notice OCL is called only once even if size specs are not compatible because OCL is not
    // affected by size specs and we can simply re-measure LithoNode from cached layout
    assertThat(steps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_ATTACHED,
            ON_EVENT_VISIBLE,
            ON_FULL_IMPRESSION_VISIBLE_EVENT,
            ON_VISIBILITY_CHANGED);

    assertThat(layoutWithSizeSpecSteps)
        .containsExactly(ON_CREATE_INITIAL_STATE, ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    assertThat(mountableLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            // Collect results phase
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            // Mount phase
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);
  }

  private static Component createComponentWithSizeSpecMeasuringComponentWithSizeSpecAsChild(
      final ComponentContext c,
      final List<LifecycleStep.StepInfo> stepsInfo,
      final List<LifecycleStep.StepInfo> layoutWithSizeSpecStepsInfo,
      final LifecycleTracker mountableLifecycleTracker) {
    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final Component layoutWithSizeSpec =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(layoutWithSizeSpecStepsInfo)
            .body(mountable)
            .build();

    final Component component_OCL =
        LayoutSpecLifecycleTester.create(c).steps(stepsInfo).body(layoutWithSizeSpec).build();

    return ComponentWithSizeSpecWithMeasureCall.create(c)
        .component(component_OCL)
        .shouldCacheResult(true)
        .build();
  }
}
