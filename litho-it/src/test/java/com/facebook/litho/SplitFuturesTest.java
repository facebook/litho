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
import static com.facebook.litho.LifecycleStep.ON_CREATE_MOUNT_CONTENT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_TREE_PROP;
import static com.facebook.litho.LifecycleStep.ON_MEASURE;
import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_PREPARE;
import static com.facebook.litho.LifecycleStep.ON_UNBIND;
import static com.facebook.litho.LifecycleStep.ON_UNMOUNT;
import static com.facebook.litho.LifecycleStep.SHOULD_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Color;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import com.facebook.litho.widget.RenderAndLayoutCountingTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/**
 * Tests that check multiple scenarios where resolve and layout are handled separately via split
 * futures. If the relevant configs that enable this behaviour are off, the tests will early return
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class SplitFuturesTest {
  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  /**
   * Test the following flow:
   *
   * <p>1. Set root, ensure render lifecycle steps are triggered
   *
   * <p>2. Set size-specs, measure and layout - ensure only measure steps are triggered
   *
   * <p>3. Remeasure and layout with the same specs, ensure no new steps are triggered
   *
   * <p>4. Remeasure with new size specs, ensure only remeasure steps are triggered
   *
   * <p>5. Set new root, ensure render and measure steps are triggered
   */
  @Test
  public void testSyncRendersAndMeasures() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;
    final LifecycleTracker tracker = new LifecycleTracker();

    final Component component1 =
        Column.create(c)
            .child(MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker))
            .build();

    // Setting root, only render steps should occur
    mLegacyLithoViewRule.setRoot(component1);

    assertThat(tracker.getSteps())
        .describedAs("Only render-phase steps are present")
        .containsExactly(
            ON_CREATE_INITIAL_STATE, ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_PREPARE);

    // Reset tracker
    tracker.reset();

    final int widthSpec1 = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec1 = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    // Set sizespecs, and trigger measure and layout. Only measure steps should occur
    mLegacyLithoViewRule.setSizeSpecs(widthSpec1, heightSpec1).measure().layout();

    assertThat(tracker.getSteps())
        .describedAs("Only measure-phase steps are present")
        .containsExactly(
            ON_MEASURE, ON_BOUNDS_DEFINED, ON_ATTACHED, ON_CREATE_MOUNT_CONTENT, ON_MOUNT, ON_BIND);

    // Reset the tracker.
    tracker.reset();

    // Measure and layout again with no change to specs. No new lifecycle steps should occur
    mLegacyLithoViewRule.measure().layout().idle();

    assertThat(tracker.getSteps()).describedAs("no change means no new lifecycle steps").isEmpty();

    // Reset the tracker
    tracker.reset();

    final int widthSpec2 = SizeSpec.makeSizeSpec(150, SizeSpec.EXACTLY);
    final int heightSpec2 = SizeSpec.makeSizeSpec(150, SizeSpec.EXACTLY);

    // Set new size specs. Only measure steps should occur
    mLegacyLithoViewRule.setSizeSpecs(widthSpec2, heightSpec2).measure().layout();

    assertThat(tracker.getSteps())
        .describedAs("Changing width and height triggers only re-measure steps")
        .containsExactly(
            SHOULD_UPDATE, ON_MEASURE, ON_BOUNDS_DEFINED, ON_UNBIND, ON_UNMOUNT, ON_MOUNT, ON_BIND);

    // Reset the tracker
    tracker.reset();

    final Component component2 =
        Column.create(c)
            .backgroundColor(Color.BLUE)
            .child(
                MountSpecPureRenderLifecycleTester.create(c)
                    .backgroundColor(Color.YELLOW)
                    .lifecycleTracker(tracker))
            .build();

    // Set a new root. Since measure already happened, we expect render and measure steps to occur
    mLegacyLithoViewRule.setRoot(component2);

    assertThat(tracker.getSteps())
        .describedAs("Setting new root after measure should render and measure")
        .containsExactly(
            ON_CREATE_TREE_PROP, ON_PREPARE, SHOULD_UPDATE, ON_MEASURE, ON_BOUNDS_DEFINED);
  }

  /** Test multiple set-root async. */
  @Test
  public void testAsyncSetRootWitNoMeasures() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;
    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    final Component component1 =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build();

    // Set root async
    mLegacyLithoViewRule.setRootAsync(component1);

    // Render should not have happened yet
    assertThat(counter.getRenderCount()).isEqualTo(0);

    // Wait for tasks to finish
    mLegacyLithoViewRule.idle();

    // Now render should have happened once.
    assertThat(counter.getRenderCount()).isEqualTo(1);

    final Component component2 =
        Column.create(c)
            .backgroundColor(0xFFFF00)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build();

    final Component component3 =
        Column.create(c)
            .backgroundColor(0xFF0000)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build();

    // Reset the counter
    counter.reset();

    // Queue 2 set-root-asyncs. Only 1 should actually happen
    mLegacyLithoViewRule.setRootAsync(component2);
    mLegacyLithoViewRule.setRootAsync(component3);

    // Run to end of tasks
    mLegacyLithoViewRule.idle();

    // Multiple queued async set roots will only end up running the latest one.
    assertThat(counter.getRenderCount()).isEqualTo(1);

    // No measure should have happened yet.
    assertThat(counter.getMeasureCount()).isEqualTo(0);
  }
}
