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
import static com.facebook.litho.LifecycleStep.ON_CREATE_MOUNT_CONTENT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_TREE_PROP;
import static com.facebook.litho.LifecycleStep.ON_DETACHED;
import static com.facebook.litho.LifecycleStep.ON_MEASURE;
import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_PREPARE;
import static com.facebook.litho.LifecycleStep.ON_UNBIND;
import static com.facebook.litho.LifecycleStep.ON_UNMOUNT;
import static com.facebook.litho.LifecycleStep.SHOULD_UPDATE;
import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stateupdates.BaseIncrementStateCaller;
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndState;
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndStateSpec;
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndState;
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndStateSpec;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/**
 * In this test class, all tests will test for complex component hierarchies of the following form:
 * Root --> Mid --> Bot Where Root, Mid and Bot can either be an OnCreateLayout component, or an
 * OnCreateLayoutWithSizeSpec component.
 *
 * <p>All 3 components hold an int state value, initialized as zero. Each component will render a
 * Text displaying a prefix ("root", "mid", "bot") + the value of the state value as their 1st
 * child. Root will accept Mid as a @Prop component, will call Component.measure on it, and use it
 * as it's 2nd child. Mid will accept Bot as a @Prop component, will call Component.measure on it,
 * and use it as it's 2nd child. Bot will accept a MountSpec component and use it as it's 2nd child.
 *
 * <p>All tests will follow the same flow: 1. Build such a hierarchy, each test will have a
 * different variation of OCL / OCLWSS comps 2. Ensure lifecycle steps for each comp + the MountSpec
 * are as expected 3. Ensure all texts are displaying the correct state 4. Update state on the root
 * comp, and repeat #2 and #3 5. Update state on the mid comp, and repeat #2 and #3 6. Update state
 * on the bot comp, and repeat #2 and #3
 *
 * <p>The name of each test will indicate the hierarchy being tested. For example, a test named
 * "test_OCLWSS_OCL_OCL" will hold the hierarchy of: Root as OCLWSS, Mid as OCL, Bot as OCL
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionWithStateTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void before() {
    ComponentsConfiguration.isEndToEndTestRun = true;
  }

  @Test
  public void test_OCL_OCL_OCL() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    // Generate a component + state-update-callers for OCL->OCL->OCL
    final StateUpdateComponentHolder holder =
        createComponentHierarchySetup(c, widthSpec, heightSpec, true, true, true);

    // Set the root and layout
    mLegacyLithoViewRule.setRoot(holder.component);
    mLegacyLithoViewRule.setSizeSpecs(widthSpec, heightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    // Ensure the root node is not null. We'll need this to extract scoped contexts to simulate
    // state updates.
    assertThat(mLegacyLithoViewRule.getCurrentRootNode()).isNotNull();

    // Ensure the lifecycle steps are as expected before any state updates.
    List<LifecycleStep> rootSteps = getSteps(holder.rootLayoutSpecSteps);
    List<LifecycleStep> midSteps = getSteps(holder.midLayoutSpecSteps);
    List<LifecycleStep> botSteps = getSteps(holder.botLayoutSpecSteps);
    List<LifecycleStep> mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_ATTACHED);

    // Mid and bot steps will be the same
    LifecycleStep[] expectedStepsForMidAndBot =
        new LifecycleStep[] {
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP,
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT,
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP,
          ON_ATTACHED
        };

    assertThat(midSteps).containsExactly(expectedStepsForMidAndBot);
    assertThat(botSteps).containsExactly(expectedStepsForMidAndBot);
    assertThat(mountableSteps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            ON_BOUNDS_DEFINED,
            ON_ATTACHED,
            ON_CREATE_MOUNT_CONTENT,
            ON_MOUNT,
            ON_BIND);

    // Ensure all texts showing initial states are as expected
    assertThat(mLegacyLithoViewRule.findViewWithText("root 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 1/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the root component's context to simulate state updates
    final ComponentContext rootScopedContext =
        mLegacyLithoViewRule.getCurrentRootNode().mNode.getComponentContextAt(1);

    // Simulate the state update for the root.
    holder.rootStateCaller.increment(rootScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .containsExactly(
            ON_CREATE_TREE_PROP, ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT);

    // Mid and bot steps will be the same
    expectedStepsForMidAndBot =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
        };

    assertThat(midSteps).containsExactly(expectedStepsForMidAndBot);
    assertThat(botSteps).containsExactly(expectedStepsForMidAndBot);

    assertThat(mountableSteps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_TREE_PROP,
            ON_PREPARE,
            ON_MEASURE,
            ON_BOUNDS_DEFINED,
            SHOULD_UPDATE,
            ON_UNBIND,
            ON_UNMOUNT,
            ON_MOUNT,
            ON_BIND);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull(); // Updated!
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 2/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the mid component's context to simulate state updates
    final ComponentContext midScopedContext =
        mLegacyLithoViewRule.getCurrentRootNode().mNode.getChildAt(1).getComponentContextAt(0);

    // Simulate the state update for the mid component.
    holder.midStateCaller.increment(midScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps).containsExactly(ON_CREATE_TREE_PROP);
    assertThat(midSteps)
        .containsExactly(
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_DETACHED,
            ON_ATTACHED);

    assertThat(botSteps)
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_DETACHED,
            ON_ATTACHED);

    assertThat(mountableSteps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            ON_MEASURE,
            ON_BOUNDS_DEFINED,
            ON_DETACHED,
            ON_ATTACHED,
            ON_UNBIND,
            ON_UNMOUNT,
            ON_MOUNT,
            ON_BIND);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 1")).isNotNull(); // Updated!
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 3/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the bottom component's context to simulate state updates
    final ComponentContext botScopedContext =
        mLegacyLithoViewRule
            .getCurrentRootNode()
            .mNode
            .getChildAt(1)
            .getChildAt(1)
            .getComponentContextAt(0);

    // Simulate the state update for the bottom component.
    holder.botStateCaller.increment(botScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps).containsExactly(ON_CREATE_TREE_PROP);
    assertThat(midSteps).containsExactly(); // Empty!
    assertThat(botSteps)
        .containsExactly(
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_DETACHED,
            ON_ATTACHED);

    assertThat(mountableSteps)
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_PREPARE,
            SHOULD_UPDATE,
            ON_MEASURE,
            ON_BOUNDS_DEFINED,
            ON_DETACHED,
            ON_ATTACHED,
            ON_UNBIND,
            ON_UNMOUNT,
            ON_MOUNT,
            ON_BIND);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 1")).isNotNull(); // Updated!
  }

  /**
   * Generate a component hierarchy as described above.
   *
   * @param c the component context
   * @param widthSpec the width-spec to use in Component.measure calls
   * @param heightSpec the height-spec to use in Component.measure calls
   * @param isRootOCL true if the root should be OCL (OCLWSS otherwise)
   * @param isMidOCL true if the mid should be OCL (OCLWSS otherwise)
   * @param isBottomOCL true if the bot should be OCL (OCLWSS otherise)
   * @return a holder class containing the root component, state update callers for each comp, and
   *     lifecycle step arrays for each component including the MountSpec held by the bot comp.
   */
  private static StateUpdateComponentHolder createComponentHierarchySetup(
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final boolean isRootOCL,
      final boolean isMidOCL,
      final boolean isBottomOCL) {

    final List<LifecycleStep.StepInfo> rootStepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> midStepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> botStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final BaseIncrementStateCaller rootCaller;
    final BaseIncrementStateCaller midCaller;
    final BaseIncrementStateCaller bottomCaller;

    final Component rootComponent;
    final Component midComponent;
    final Component bottomComponent;

    // bottom comp will set half height to make room for text
    if (isBottomOCL) {
      bottomCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      bottomComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(botStepsInfo)
              .shouldCacheResult(true)
              .widthPx(SizeSpec.getSize(widthSpec) / 2)
              .heightPx(SizeSpec.getSize(heightSpec) / 2)
              .prefix("bot")
              .mountSpec(mountable)
              .build();
    } else {
      bottomCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      bottomComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(botStepsInfo)
              .shouldCacheResult(true)
              .widthPx(SizeSpec.getSize(widthSpec) / 2)
              .heightPx(SizeSpec.getSize(heightSpec) / 2)
              .prefix("bot")
              .mountSpec(mountable)
              .build();
    }

    if (isMidOCL) {
      midCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      midComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(midStepsInfo)
              .shouldCacheResult(true)
              .component(bottomComponent)
              .widthSpec(widthSpec)
              .heightSpec(heightSpec)
              .prefix("mid")
              .build();
    } else {
      midCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      midComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(midStepsInfo)
              .shouldCacheResult(true)
              .component(bottomComponent)
              .prefix("mid")
              .build();
    }

    if (isRootOCL) {
      rootCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      rootComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(rootStepsInfo)
              .shouldCacheResult(true)
              .component(midComponent)
              .widthSpec(widthSpec)
              .heightSpec(heightSpec)
              .prefix("root")
              .build();
    } else {
      rootCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      rootComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(rootStepsInfo)
              .shouldCacheResult(true)
              .component(midComponent)
              .prefix("root")
              .build();
    }

    return new StateUpdateComponentHolder(
        rootComponent,
        rootCaller,
        midCaller,
        bottomCaller,
        rootStepsInfo,
        midStepsInfo,
        botStepsInfo,
        mountableLifecycleTracker);
  }

  /**
   * Holder class for a component hierarchy described above. Holds the root component, state update
   * callers for each 3 components, and step info arrays for each component including the leaf
   * MountSpec.
   */
  public static class StateUpdateComponentHolder {
    public final Component component;
    public final BaseIncrementStateCaller rootStateCaller;
    public final BaseIncrementStateCaller midStateCaller;
    public final BaseIncrementStateCaller botStateCaller;
    public final List<LifecycleStep.StepInfo> rootLayoutSpecSteps;
    public final List<LifecycleStep.StepInfo> midLayoutSpecSteps;
    public final List<LifecycleStep.StepInfo> botLayoutSpecSteps;
    public final LifecycleTracker mountableLifecycleTracker;

    public StateUpdateComponentHolder(
        final Component component,
        final BaseIncrementStateCaller rootStateCaller,
        final BaseIncrementStateCaller midStateCaller,
        final BaseIncrementStateCaller botStateCaller,
        final List<LifecycleStep.StepInfo> rootLayoutSpecSteps,
        final List<LifecycleStep.StepInfo> midLayoutSpecSteps,
        final List<LifecycleStep.StepInfo> botLayoutSpecSteps,
        final LifecycleTracker mountableLifecycleTracker) {
      this.component = component;
      this.rootStateCaller = rootStateCaller;
      this.midStateCaller = midStateCaller;
      this.botStateCaller = botStateCaller;
      this.rootLayoutSpecSteps = rootLayoutSpecSteps;
      this.midLayoutSpecSteps = midLayoutSpecSteps;
      this.botLayoutSpecSteps = botLayoutSpecSteps;
      this.mountableLifecycleTracker = mountableLifecycleTracker;
    }

    /** Resets all lifecycle steps for all components. */
    void clearAllSteps() {
      this.rootLayoutSpecSteps.clear();
      this.midLayoutSpecSteps.clear();
      this.botLayoutSpecSteps.clear();
      this.mountableLifecycleTracker.reset();
    }
  }
}
