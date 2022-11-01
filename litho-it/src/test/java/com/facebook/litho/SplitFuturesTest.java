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
import static com.facebook.litho.testing.ThreadTestingUtils.runOnBackgroundThread;
import static com.facebook.litho.testing.ThreadTestingUtils.shortWait;
import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Color;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.TimeOutSemaphore;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import com.facebook.litho.widget.RenderAndLayoutCountingTester;
import com.facebook.litho.widget.RenderAndLayoutCountingTesterSpec;
import com.facebook.litho.widget.Text;
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
    mLegacyLithoViewRule.setRoot(component1).idle();

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

  /**
   * This test covers a scenario wherein two setSizeSpecs are called via a background thread. By the
   * time the 2nd setSizeSpec is called, the 1st setSizeSpec has already queued a future that is
   * being calculated. The test functions in the following manner:
   *
   * <p>1. Setup a component with a listener during onMeasure, and set it as the root.
   *
   * <p>2. Start 2 threads, where each calls setSizeSpec (sync) using the same width / height.
   *
   * <p>3. Before the 2nd thread is started, wait for the 1st setSizeSpec to call onMeasure
   *
   * <p>4. Before onMeasure finishes, ensure both setSizeSpecs have been called.
   *
   * <p>5. Wait until both setSizeSpecs have finished before continuing to assert
   *
   * <p>6. Ensure no "awaits" timed out
   *
   * <p>7. Ensure onMeasure was only called once. This verifies that only 1 future was created, and
   * when the 2nd setSizeSpec was called, the 1st future was reused. For both calls, the output
   * should have the correct values.
   */
  @Test
  public void testBackgroundSyncMeasures_layoutTreeFutureIsReused() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;
    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    // Latch that waits for the 1st measure to start
    final TimeOutSemaphore firstMeasureStartLatch = new TimeOutSemaphore(0);

    // Latch that waits for the 2nd setSizeSpec to get called
    final TimeOutSemaphore secondSizeSpecCalledLatch = new TimeOutSemaphore(0);

    // Listener to be passed into the component.
    final RenderAndLayoutCountingTesterSpec.Listener listener =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {}

          @Override
          public void onMeasure() {
            // inform measure has started
            firstMeasureStartLatch.release();

            // wait for 2nd setSizeSpec to be called
            secondSizeSpecCalledLatch.acquire();

            // Add short delay to ensure other thread can call setSizeSpec before this finishes.
            shortWait();
          }
        };

    // Setup the component with a counter and listener
    final Component component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build();

    // Set root without measuring
    mLegacyLithoViewRule.setRoot(component);

    final ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();
    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(150, SizeSpec.EXACTLY);

    final Size output1 = new Size();
    final Size output2 = new Size();

    // Background thread latch to wait on before asserting. -1 permits to ensure both bg threads
    // finish and release.
    final TimeOutSemaphore bgThreadLatch = new TimeOutSemaphore(-1);

    // Set-size-spec-sync twice in a row with the same size-specs on the background.
    // Wait until the 1st onMeasure kicks in before queuing the 2nd one.
    // This will ensure that the running future is reused rather than queuing another one
    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Call setSizeSpec, which should call onMeasure only once.
            componentTree.setSizeSpec(widthSpec, heightSpec, output1);
          }
        });

    // Wait until 1st measure happens before queuing the next one. This ensures the Layout
    // future is in process, so that the 2nd setSizeSpec just reuses the running one.
    firstMeasureStartLatch.acquire();

    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Inform 2nd setSizeSpec is about to get called
            secondSizeSpecCalledLatch.release();

            // Call setSizeSpec, which should just reuse the 1st setSizeSpec's future
            componentTree.setSizeSpec(widthSpec, heightSpec, output2);
          }
        });

    // Wait for all measures to finish queuing
    bgThreadLatch.acquire();

    // Ensure onMeasure was only called once.
    assertThat(counter.getMeasureCount()).isEqualTo(1);

    // Ensure outputs were properly set
    assertThat(output1.width).isEqualTo(100);
    assertThat(output1.height).isEqualTo(150);
    assertThat(output2.width).isEqualTo(100);
    assertThat(output2.height).isEqualTo(150);
  }

  /**
   * Testing that when setting root via a background thread twice, the older one does not get
   * committed. This test will ensure that the 1st setRoot starts before the 2nd setRoot, but will
   * finish after the 2nd setRoot completes, thus ensuring the version-check when committing the
   * resolution result is working as expected.
   *
   * <p>We assert this is working by waiting for both background setRoots to finish, then triggering
   * a sync measure and ensuring only the 2nd root got measured and mounted.
   */
  @Test
  public void testOlderRootIsNotCommitted() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;
    final RenderAndMeasureCounter counter1 = new RenderAndMeasureCounter();
    final RenderAndMeasureCounter counter2 = new RenderAndMeasureCounter();

    // Latch to force comp1 to wait until comp2 has completed rendering and has been committed.
    final TimeOutSemaphore waitForSecondSetRootToFinishLatch = new TimeOutSemaphore(0);

    // Latch to wait for the 1st setRoot to get called to avoid calling the 2nd setRoot 1st.
    final TimeOutSemaphore waitForFirstSetRootLatch = new TimeOutSemaphore(0);

    // Listener for comp1
    final RenderAndLayoutCountingTesterSpec.Listener listener1 =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {
            // Inform 1st setRoot has been called
            waitForFirstSetRootLatch.release();

            // wait for 2nd setRoot to finish
            waitForSecondSetRootToFinishLatch.acquire();
          }

          @Override
          public void onMeasure() {}
        };

    // Component1 will be set as root 1st, but will finish after Component2
    final Component component1 =
        Column.create(c)
            .child(Text.create(c).text("Comp1"))
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter1)
                    .listener(listener1))
            .build();

    // Component2 will be set as root 2nd, but will finish before Component1
    final Component component2 =
        Column.create(c)
            .child(Text.create(c).text("Comp2"))
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter2))
            .build();

    // Background thread latch with -1 permits to ensure both bg tasks finish
    final TimeOutSemaphore bgThreadLatch = new TimeOutSemaphore(-1);

    // Set size specs so setRoot happens sync
    mLegacyLithoViewRule
        .getComponentTree()
        .setSizeSpec(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Set root1 1st.
            mLegacyLithoViewRule.setRoot(component1);
          }
        });

    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Wait for the 1st setRoot to be called
            waitForFirstSetRootLatch.acquire();

            // Call the 2nd setRoot, will be called after comp1 is set as root, thereby making
            // comp2 the latest.
            mLegacyLithoViewRule.setRoot(component2);

            // inform comp2 has finished render
            waitForSecondSetRootToFinishLatch.release();
          }
        });

    // Wait for both background tasks to finish
    bgThreadLatch.acquire();

    // Now do layout to trigger a measure with the committed resolution result. We expect only the
    // 2nd component to get measured here.
    mLegacyLithoViewRule
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
        .measure()
        .layout()
        .idle();

    // Both components were rendered, so we expect them both to have a render count of 1
    assertThat(counter1.getRenderCount()).isEqualTo(1);
    assertThat(counter2.getRenderCount()).isEqualTo(1);

    // Only the 2nd root should have been committed, so only the 2nd one will have been measured.
    assertThat(counter1.getMeasureCount()).isEqualTo(0);
    assertThat(counter2.getMeasureCount()).isEqualTo(1);

    // Ensure we can find Comp2's Text mounted.
    assertThat(mLegacyLithoViewRule.findViewWithText("Comp2")).isNotNull();
  }

  /**
   * Testing that when setting size-specs via a background thread twice, the older one does not get
   * committed. This test will ensure that the 1st setSizeSpecs starts before the 2nd setSizeSpecs,
   * but will finish after the 2nd setSizeSpecs completes, thus ensuring the version-check when
   * committing the LayoutState is working as expected.
   *
   * <p>We assert this is working by waiting for both background setSizeSpecs to finish, then
   * checking the committed LayoutState has the newest size-specs, despite the older one finishing
   * last.
   */
  @Test
  public void testOlderLayoutIsNotCommitted() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;

    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    // Boolean holder to check if the 1st measurement has happened yet. We want to force the 1st
    // measurement to stall until the 2nd measurement completes.
    final boolean[] didFirstMeasureHappenHolder = new boolean[1];

    // Latch to wait for the 2nd setSizeSpec to finish before the 1st one can complete
    final TimeOutSemaphore waitForSecondSizeSpecToFinishLatch = new TimeOutSemaphore(0);

    // Latch to ensure the 1st setSizeSpec with "old" specs gets called before the 2nd one
    final TimeOutSemaphore waitForFirstSizeSpecToStartLatch = new TimeOutSemaphore(0);

    final RenderAndLayoutCountingTesterSpec.Listener listener =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {}

          @Override
          public void onMeasure() {
            // Waiting should only happen on the 1st measure.
            // The latches below ensure that this condition is only met for the "old" specs.
            boolean isFirst = false;
            synchronized (didFirstMeasureHappenHolder) {
              if (!didFirstMeasureHappenHolder[0]) {
                didFirstMeasureHappenHolder[0] = true;
                isFirst = true;
              }
            }

            if (isFirst) {
              // First measure has started, inform 2nd measure can begin
              waitForFirstSizeSpecToStartLatch.release();

              // First measure must now wait for 2nd measure to complete
              waitForSecondSizeSpecToFinishLatch.acquire();
            }
          }
        };

    final Component component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build();

    // Set the root component once (sync)
    mLegacyLithoViewRule.setRoot(component);

    // "Old" size-specs to be set first, but finish last
    final int oldWidthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int oldHeightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    // "New" size-specs to be set second, but finish 1st
    final int newWidthSpec = SizeSpec.makeSizeSpec(150, SizeSpec.EXACTLY);
    final int newHeightSpec = SizeSpec.makeSizeSpec(150, SizeSpec.EXACTLY);

    final ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();

    // Background thread latch with -1 permits to ensure both bg tasks finish
    final TimeOutSemaphore bgThreadLatch = new TimeOutSemaphore(-1);

    final Size output1 = new Size();
    final Size output2 = new Size();

    // Set the "old" size specs on the background
    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Call setSizeSpec with "old" size specs
            componentTree.setSizeSpec(oldWidthSpec, oldHeightSpec, output1);
          }
        });

    // Set the "new" size specs on the background
    runOnBackgroundThread(
        bgThreadLatch,
        new Runnable() {
          @Override
          public void run() {
            // Wait for 1st setSizeSpec to happen
            waitForFirstSizeSpecToStartLatch.acquire();

            // Call setSizeSpec with "new" size specs
            componentTree.setSizeSpec(newWidthSpec, newHeightSpec, output2);

            // Inform 2nd measure has completed
            waitForSecondSizeSpecToFinishLatch.release();
          }
        });

    // Wait for both background threads to finish
    bgThreadLatch.acquire();

    final int oldWidth = SizeSpec.getSize(oldWidthSpec);
    final int oldHeight = SizeSpec.getSize(oldHeightSpec);
    final int newWidth = SizeSpec.getSize(newWidthSpec);
    final int newHeight = SizeSpec.getSize(newHeightSpec);

    // Ensure older setSizeSpec has the correct output
    assertThat(output1.width).isEqualTo(oldWidth);
    assertThat(output1.height).isEqualTo(oldHeight);

    // Ensure newer setSizeSpec has the correct output
    assertThat(output2.width).isEqualTo(newWidth);
    assertThat(output2.height).isEqualTo(newHeight);

    // Set root only happened once, so we expect only 1 render call.
    assertThat(counter.getRenderCount()).isEqualTo(1);

    // Despite only the newer size-specs getting committed, measure still happens twice
    assertThat(counter.getMeasureCount()).isEqualTo(2);

    final LayoutState committedLayoutState = componentTree.getCommittedLayoutState();

    // Ensure the committed layout-state has the newer sizes
    assertThat(committedLayoutState).isNotNull();
    assertThat(committedLayoutState.getWidth()).isEqualTo(newWidth);
    assertThat(committedLayoutState.getHeight()).isEqualTo(newHeight);
  }

  /**
   * When an async layout is requested while a sync layout is in progress with the same size-specs,
   * the async layout should be completely ignored in favour of the running sync one.
   *
   * <p>This test sets up this scenario by triggering a sync layout in a bg-thread, while an async
   * layout with the same size specs is queued.
   *
   * <p>To force the timing to be right, the async tasks are only drained (i.e., triggered) when the
   * measure from the sync layout has started. The sync layout is then blocked until the async tasks
   * have finished draining.
   *
   * <p>We then wait for the background thread to finish, and ensure measure has only happened once,
   * and the output is populated with the corerect values.
   */
  @Test
  public void testAsyncLayoutIsSkippedWhenEquivalentSyncLayoutInProgress() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;

    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    final TimeOutSemaphore syncMeasureStartedLatch = new TimeOutSemaphore(0);
    final TimeOutSemaphore asyncMeasureTriggeredLatch = new TimeOutSemaphore(0);

    final RenderAndLayoutCountingTesterSpec.Listener listener =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {}

          @Override
          public void onMeasure() {
            // Inform measure has started
            syncMeasureStartedLatch.release();

            // Wait for async tasks to drain
            asyncMeasureTriggeredLatch.acquire();
          }
        };

    final Component component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build();

    // Set the root component once (sync)
    mLegacyLithoViewRule.setRoot(component);

    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    final ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();

    componentTree.setSizeSpecAsync(widthSpec, heightSpec);

    final Size output = new Size();

    final TimeOutSemaphore bgLatch =
        runOnBackgroundThread(
            new Runnable() {
              @Override
              public void run() {
                componentTree.setSizeSpec(widthSpec, heightSpec, output);
              }
            });

    // Wait for sync measure to start
    syncMeasureStartedLatch.acquire();

    // Force async tasks to run
    mLegacyLithoViewRule.idle();

    // Inform async tasks have been drained
    asyncMeasureTriggeredLatch.release();

    // Wait for bg thread to finish
    bgLatch.acquire();

    // Measure should have only happened once
    assertThat(counter.getMeasureCount()).isEqualTo(1);

    // Ensure sync measure output is correct
    assertThat(output.width).isEqualTo(100);
    assertThat(output.height).isEqualTo(100);
  }

  /**
   * When an async setRootAndSizeSpec happens, followed by an equivalent sync call, we expect the
   * async process to be promoted to the main thread, thus ensuring render and measure only happen
   * once.
   *
   * <p>The test verifies this behaviour works correctly during render by triggering an async
   * setRootAndSizeSpec, waiting for render to begin, and then triggering an equivalent sync
   * process.
   */
  @Test
  public void testSyncRenderContinuesAsyncOnMainThread() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;

    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    // Latch to wait for async render to begin before sync render
    final TimeOutSemaphore waitForAsyncRenderToStartLatch = new TimeOutSemaphore(0);

    final RenderAndLayoutCountingTesterSpec.Listener listener =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {
            // Inform async render has started
            waitForAsyncRenderToStartLatch.release();

            // Short wait to let sync render to begin
            shortWait();
          }

          @Override
          public void onMeasure() {}
        };

    final Component component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build();

    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    final ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();

    // Set root and size-spec async
    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec);

    // run to end of tasks on background to avoid blocking here
    runOnBackgroundThread(
        new Runnable() {
          @Override
          public void run() {
            mLegacyLithoViewRule.idle();
          }
        });

    // Wait for async render to start
    waitForAsyncRenderToStartLatch.acquire();

    // Set root and sync-spec sync
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec);

    // Ensure render and measure only happened once
    assertThat(counter.getRenderCount()).isEqualTo(1);
    assertThat(counter.getMeasureCount()).isEqualTo(1);
  }

  /**
   * When an async setRootAndSizeSpec happens, followed by an equivalent sync call, we expect the
   * async process to be promoted to the main thread, thus ensuring render and measure only happen
   * once.
   *
   * <p>The test verifies this behaviour works correctly during layout by triggering an async
   * setRootAndSizeSpec, waiting for layout to begin, and then triggering an equivalent sync
   * process.
   */
  @Test
  public void testSyncLayoutContinuesAsyncOnMainThread() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;

    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    // Latch to wait for async measure to begin before sync render
    final TimeOutSemaphore waitForAsyncMeasureToStartLatch = new TimeOutSemaphore(0);

    final RenderAndLayoutCountingTesterSpec.Listener listener =
        new RenderAndLayoutCountingTesterSpec.Listener() {
          @Override
          public void onPrepare() {}

          @Override
          public void onMeasure() {
            // Inform async measure has started
            waitForAsyncMeasureToStartLatch.release();

            // Short wait to let sync process to begin
            shortWait();
          }
        };

    final Component component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build();

    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    final ComponentTree componentTree = mLegacyLithoViewRule.getComponentTree();

    // Set root and size-spec async
    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec);

    // run to end of tasks on background to avoid blocking here
    runOnBackgroundThread(
        new Runnable() {
          @Override
          public void run() {
            mLegacyLithoViewRule.idle();
          }
        });

    // Wait for async measure to start
    waitForAsyncMeasureToStartLatch.acquire();

    // Set root and sync-spec sync
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec);

    // Ensure render and measure only happened once
    assertThat(counter.getRenderCount()).isEqualTo(1);
    assertThat(counter.getMeasureCount()).isEqualTo(1);
  }

  /**
   * When splitting futures, we include an optimisation to avoid blocking the UI thread when setting
   * root sync when there's no size-specs. Without split futures, nothing would happen here, so when
   * introducing split futures, we want to ensure that we're still not blocking the UI thread.
   *
   * <p>This test verifies this behaviour by calling setRoot (sync) and ensuring that prior to
   * running to the end of tasks, no render has happened. Then we run to the end of tasks and ensure
   * that the render did happen.
   *
   * <p>In all cases, measure should not happen.
   */
  @Test
  public void testSetRootWithNoSizeSpecsHappensAsync() {
    // Only relevant when futures are split
    if (!ComponentsConfiguration.isResolveAndLayoutFuturesSplitEnabled) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.context;

    final RenderAndMeasureCounter counter = new RenderAndMeasureCounter();

    final Component component =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build();

    // Set root here should force the operation to become async
    mLegacyLithoViewRule.setRoot(component);

    // Setting root without size-specs should force the operation to become async, so we verify
    // that render did not happen yet.
    assertThat(counter.getRenderCount()).isEqualTo(0);
    assertThat(counter.getMeasureCount()).isEqualTo(0);

    // Run to end of tasks, forcing async task to complete
    mLegacyLithoViewRule.idle();

    // Now that we've run to end of tasks, we can expect render to have happened (tho no measure)
    assertThat(counter.getRenderCount()).isEqualTo(1);
    assertThat(counter.getMeasureCount()).isEqualTo(0);
  }
}
