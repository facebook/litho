/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.annotation.Nullable;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import com.facebook.litho.widget.NestedTreeComponentSpec.ExtraProps;
import com.facebook.litho.widget.RootComponentWithTreeProps;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  private boolean originalE2ETestRun;

  @Before
  public void before() {
    originalE2ETestRun = ComponentsConfiguration.isEndToEndTestRun;
    ComponentsConfiguration.isEndToEndTestRun = true;
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            DefaultInternalNode node = spy(new DefaultInternalNode(c));
            node.getYogaNode().setData(node);
            return node;
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            DefaultNestedTreeHolder node = spy(new DefaultNestedTreeHolder(c, props));
            node.getYogaNode().setData(node);
            return node;
          }
        };
  }

  @After
  public void after() {
    ComponentsConfiguration.isEndToEndTestRun = originalE2ETestRun;
    NodeConfig.sInternalNodeFactory = null;
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldContainNestTreeHolderAndNestedProps() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component = RootComponentWithTreeProps.create(c).build();

    mLithoViewRule.attachToWindow().setSizePx(100, 100).measure().setRoot(component).layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);

    DefaultNestedTreeHolder holder = (DefaultNestedTreeHolder) root.getChildAt(1);
    assertThat(holder.mNestedTreePadding.get(YogaEdge.ALL)).isEqualTo(5.0f);
    assertThat(holder.getNestedResult().getPaddingTop()).isEqualTo(5);
  }

  @Test
  public void onRenderComponentWithSizeSpecWithoutReuse_shouldCallOnCreateLayoutTwice() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = true;
    props.steps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    assertThat(props.steps)
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);
    verify(holder.getInternalNode(), times(2)).copyInto(any(InternalNode.class));
  }

  @Test
  public void onRenderComponentWithSizeSpecWithReuse_shouldCallOnCreateLayoutOnlyOnce() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = false;
    props.steps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    assertThat(props.steps).containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);
    verify(holder.getInternalNode()).copyInto(any(InternalNode.class));
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldNotTransferLayoutDirectionIfExplicitlySet() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = true;
    props.steps = new ArrayList<>();
    props.mDirection = YogaDirection.RTL;

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.LTR);
    verify(holder.getNestedResult().getInternalNode()).layoutDirection(YogaDirection.RTL);
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldTransferLayoutDirectionIfNotExplicitlySet() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = true;
    props.steps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.LTR);
    verify(holder.getNestedResult().getInternalNode(), times(1)).layoutDirection(YogaDirection.LTR);
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldInheritLayoutDirectionIfExplicitlySetOnParent() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c)
            .shouldNotUpdateState(true)
            .layoutDirection(YogaDirection.RTL)
            .build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = true;
    props.steps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.RTL);
    verify(holder.getNestedResult().getInternalNode(), times(1)).layoutDirection(YogaDirection.RTL);
  }

  @Test
  public void onReRenderComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final NodeConfig.InternalNodeFactory current = NodeConfig.sInternalNodeFactory;
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            return spy(new InputOnlyInternalNode<>(c));
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            return spy(new InputOnlyNestedTreeHolder(c, props));
          }
        };

    final ComponentContext c = mLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();

    final Component root_0 =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info_0)
                    .body(mountable_0)
                    .shouldReusePreviousLayout(true))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

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
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info_1)
                    .body(mountable_1)
                    .shouldReusePreviousLayout(true))
            .child(Text.create(c).text("Hello World"))
            .build();

    mLithoViewRule.setRoot(root_1);

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

    NodeConfig.sInternalNodeFactory = current;
  }

  @Test
  public void onReRenderSimpleComponentWithSizeSpec_shouldLeverageLayoutDiffing() {
    final NodeConfig.InternalNodeFactory current = NodeConfig.sInternalNodeFactory;
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            return spy(new InputOnlyInternalNode<>(c));
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            return spy(new InputOnlyNestedTreeHolder(c, props));
          }
        };

    final ComponentContext c = mLithoViewRule.getContext();

    final List<LifecycleStep.StepInfo> info_0 = new ArrayList<>();
    final LifecycleTracker tracker_0 = new LifecycleTracker();

    final Component mountable_0 =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker_0).build();

    final Component root_0 =
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_0)
            .body(mountable_0)
            .shouldReusePreviousLayout(false)
            .build();

    mLithoViewRule.setRoot(root_0).attachToWindow().measure().layout();

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
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_1)
            .body(mountable_1)
            .shouldReusePreviousLayout(false)
            .build();

    mLithoViewRule.setRoot(root_1);

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
        LayoutWithSizeSpecLifecycleTester.create(c)
            .steps(info_2)
            .body(mountable_2)
            .shouldReusePreviousLayout(false)
            .build();

    mLithoViewRule.setRoot(root_2);

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

    NodeConfig.sInternalNodeFactory = current;
  }
}
