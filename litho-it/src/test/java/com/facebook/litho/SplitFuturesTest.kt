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
import com.facebook.litho.TreeFuture.FutureExecutionListener
import com.facebook.litho.TreeFuture.FutureExecutionType
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.LithoStatsRule
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.TimeOutSemaphore
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
import com.facebook.litho.widget.RenderAndLayoutCountingTester
import com.facebook.litho.widget.RenderAndLayoutCountingTesterSpec
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests that check multiple scenarios where resolve and layout are handled separately via split
 * futures. If the relevant configs that enable this behaviour are off, the tests will early return
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class SplitFuturesTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @JvmField @Rule val lithoStatsRule: LithoStatsRule = LithoStatsRule()

  /**
   * Test the following flow:
   * 1. Set root, ensure render lifecycle steps are triggered
   * 2. Set size-specs, measure and layout - ensure only measure steps are triggered
   * 3. Remeasure and layout with the same specs, ensure no new steps are triggered
   * 4. Remeasure with new size specs, ensure only remeasure steps are triggered
   * 5. Set new root, ensure render and measure steps are triggered
   */
  @Test
  fun testSyncRendersAndMeasures() {
    val c = legacyLithoViewRule.context
    val tracker = LifecycleTracker()
    val component1 =
        Column.create(c)
            .child(MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker))
            .build()

    // Setting root, only render steps should occur
    legacyLithoViewRule.setRoot(component1).idle()
    assertThat(tracker.steps)
        .describedAs("Only render-phase steps are present")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE)

    // Reset tracker
    tracker.reset()
    val widthSpec1 = exactly(100)
    val heightSpec1 = exactly(100)

    // Set sizespecs, and trigger measure and layout. Only measure steps should occur
    legacyLithoViewRule.setSizeSpecs(widthSpec1, heightSpec1).measure().layout()
    assertThat(tracker.steps)
        .describedAs("Only measure-phase steps are present")
        .containsExactly(
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Reset the tracker.
    tracker.reset()

    // Measure and layout again with no change to specs. No new lifecycle steps should occur
    legacyLithoViewRule.measure().layout().idle()
    assertThat(tracker.steps).describedAs("no change means no new lifecycle steps").isEmpty()

    // Reset the tracker
    tracker.reset()
    val widthSpec2 = exactly(150)
    val heightSpec2 = exactly(150)

    // Set new size specs. Only measure steps should occur
    legacyLithoViewRule.setSizeSpecs(widthSpec2, heightSpec2).measure().layout()
    if (ComponentsConfiguration.enableLayoutCaching) {
      // SHOULD_UPDATE happens after ON_MEASURE with layout caching
      assertThat(tracker.steps)
          .describedAs("Changing width and height triggers only re-measure steps")
          .containsOnly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
    } else {
      assertThat(tracker.steps)
          .describedAs("Changing width and height triggers only re-measure steps")
          .containsExactly(
              LifecycleStep.SHOULD_UPDATE,
              LifecycleStep.ON_MEASURE,
              LifecycleStep.ON_BOUNDS_DEFINED,
              LifecycleStep.ON_UNBIND,
              LifecycleStep.ON_UNMOUNT,
              LifecycleStep.ON_MOUNT,
              LifecycleStep.ON_BIND)
    }

    // Reset the tracker
    tracker.reset()
    val component2 =
        Column.create(c)
            .backgroundColor(Color.BLUE)
            .child(
                MountSpecPureRenderLifecycleTester.create(c)
                    .backgroundColor(Color.YELLOW)
                    .lifecycleTracker(tracker))
            .build()

    // Set a new root. Since measure already happened, we expect render and measure steps to occur
    legacyLithoViewRule.setRoot(component2)
    assertThat(tracker.steps)
        .describedAs("Setting new root after measure should render and measure")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED)
  }

  /** Test multiple set-root async. */
  @Test
  fun testAsyncSetRootWitNoMeasures() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()
    val component1 =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()

    // Set root async
    legacyLithoViewRule.setRootAsync(component1)

    // Render should not have happened yet
    assertThat(counter.renderCount).isEqualTo(0)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(0)

    // Wait for tasks to finish
    legacyLithoViewRule.idle()

    // Now render should have happened once.
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    val component2 =
        Column.create(c)
            .backgroundColor(0xFFFF00)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()
    val component3 =
        Column.create(c)
            .backgroundColor(0xFF0000)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()

    // Reset the counter
    counter.reset()
    lithoStatsRule.resetAllCounters()

    // Queue 2 set-root-asyncs. Only 1 should actually happen
    legacyLithoViewRule.setRootAsync(component2)
    legacyLithoViewRule.setRootAsync(component3)

    // Run to end of tasks
    legacyLithoViewRule.idle()

    // Multiple queued async set roots will only end up running the latest one.
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)

    // No measure should have happened yet.
    assertThat(counter.measureCount).isEqualTo(0)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(0)
  }

  /**
   * This test covers a scenario wherein two setSizeSpecs are called via a background thread. By the
   * time the 2nd setSizeSpec is called, the 1st setSizeSpec has already queued a future that is
   * being calculated. The test functions in the following manner:
   * 1. Setup a component with a listener during onMeasure, and set it as the root.
   * 2. Start 2 threads, where each calls setSizeSpec (sync) using the same width / height.
   * 3. Before the 2nd thread is started, wait for the 1st setSizeSpec to call onMeasure
   * 4. Before onMeasure finishes, ensure both setSizeSpecs have been called.
   * 5. Wait until both setSizeSpecs have finished before continuing to assert
   * 6. Ensure no "awaits" timed out
   * 7. Ensure onMeasure was only called once. This verifies that only 1 future was created, and
   *    when the 2nd setSizeSpec was called, the 1st future was reused. For both calls, the output
   *    should have the correct values.
   */
  @Test
  fun testBackgroundSyncMeasures_layoutTreeFutureIsReused() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()

    // Latch that waits for the 1st measure to start
    val firstMeasureStartLatch = TimeOutSemaphore(0)

    // Latch that waits for the 2nd future pre-execution listener
    val secondFuturePreExecutionLatch = TimeOutSemaphore(0)

    // Listener to be passed into the component.
    val listener: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() = Unit

          override fun onMeasure() {
            // inform measure has started
            firstMeasureStartLatch.release()

            // wait for 2nd future's pre-execution callback
            secondFuturePreExecutionLatch.acquire()
          }
        }

    // Setup the component with a counter and listener
    val component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build()

    // Set root without measuring
    legacyLithoViewRule.setRoot(component)
    val componentTree = legacyLithoViewRule.componentTree
    val widthSpec = exactly(100)
    val heightSpec = exactly(150)
    val output1 = Size()
    val output2 = Size()

    // Background thread latch to wait on before asserting. -1 permits to ensure both bg threads
    // finish and release.
    val bgThreadLatch = TimeOutSemaphore(-1)

    // Set-size-spec-sync twice in a row with the same size-specs on the background.
    // Wait until the 1st onMeasure kicks in before queuing the 2nd one.
    // This will ensure that the running future is reused rather than queuing another one
    ThreadTestingUtils.runOnBackgroundThread(
        bgThreadLatch) { // Call setSizeSpec, which should call onMeasure only once.
          componentTree.setSizeSpec(widthSpec, heightSpec, output1)
        }

    // Wait until 1st measure happens before queuing the next one. This ensures the Layout
    // future is in process, so that the 2nd setSizeSpec just reuses the running one.
    firstMeasureStartLatch.acquire()
    val isReusingFutureHolder = BooleanArray(1)
    val futureExecutionListener: FutureExecutionListener =
        object : FutureExecutionListener {
          override fun onPreExecution(
              version: Int,
              futureExecutionType: FutureExecutionType,
              attribution: String
          ) {
            secondFuturePreExecutionLatch.release()
            isReusingFutureHolder[0] = futureExecutionType == FutureExecutionType.REUSE_FUTURE
          }

          override fun onPostExecution(version: Int, released: Boolean, attribution: String) = Unit
        }
    ThreadTestingUtils.runOnBackgroundThread(
        bgThreadLatch) { // set the pre-execution listener. This will be called after the 1st future
          // has
          // already begun execution, so no concerns over this being triggered for the 1st
          // future.
          componentTree.setFutureExecutionListener(futureExecutionListener)

          // Call setSizeSpec, which should just reuse the 1st setSizeSpec's future
          componentTree.setSizeSpec(widthSpec, heightSpec, output2)
        }

    // Wait for all measures to finish queuing
    bgThreadLatch.acquire()

    // Ensure onMeasure was only called once.
    assertThat(counter.measureCount).isEqualTo(1)

    // Ensure outputs were properly set
    assertThat(output1.width).isEqualTo(100)
    assertThat(output1.height).isEqualTo(150)
    assertThat(output2.width).isEqualTo(100)
    assertThat(output2.height).isEqualTo(150)

    // Ensure the future was reused
    assertThat(isReusingFutureHolder[0]).isTrue
  }

  /**
   * Testing that when setting root via a background thread twice, the older one does not get
   * committed. This test will ensure that the 1st setRoot starts before the 2nd setRoot, but will
   * finish after the 2nd setRoot completes, thus ensuring the version-check when committing the
   * resolution result is working as expected.
   *
   * We assert this is working by waiting for both background setRoots to finish, then triggering a
   * sync measure and ensuring only the 2nd root got measured and mounted.
   */
  @Test
  fun testOlderRootIsNotCommitted() {
    val c = legacyLithoViewRule.context
    val counter1 = RenderAndMeasureCounter()
    val counter2 = RenderAndMeasureCounter()

    // Latch to force comp1 to wait until comp2 has completed rendering and has been committed.
    val waitForSecondSetRootToFinishLatch = TimeOutSemaphore(0)

    // Latch to wait for the 1st setRoot to get called to avoid calling the 2nd setRoot 1st.
    val waitForFirstSetRootLatch = TimeOutSemaphore(0)

    // Listener for comp1
    val listener1: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() {
            // Inform 1st setRoot has been called
            waitForFirstSetRootLatch.release()

            // wait for 2nd setRoot to finish
            waitForSecondSetRootToFinishLatch.acquire()
          }

          override fun onMeasure() = Unit
        }

    // Component1 will be set as root 1st, but will finish after Component2
    val component1 =
        Column.create(c)
            .child(Text.create(c).text("Comp1"))
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter1)
                    .listener(listener1))
            .build()

    // Component2 will be set as root 2nd, but will finish before Component1
    val component2 =
        Column.create(c)
            .child(Text.create(c).text("Comp2"))
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter2))
            .build()

    // Background thread latch with -1 permits to ensure both bg tasks finish
    val bgThreadLatch = TimeOutSemaphore(-1)

    // Set size specs so setRoot happens sync
    legacyLithoViewRule.componentTree.setSizeSpec(exactly(100), exactly(100))
    lithoStatsRule.resetAllCounters()
    ThreadTestingUtils.runOnBackgroundThread(bgThreadLatch) { // Set root1 1st.
      legacyLithoViewRule.setRoot(component1)
    }
    ThreadTestingUtils.runOnBackgroundThread(
        bgThreadLatch) { // Wait for the 1st setRoot to be called
          waitForFirstSetRootLatch.acquire()

          // Call the 2nd setRoot, will be called after comp1 is set as root, thereby making
          // comp2 the latest.
          legacyLithoViewRule.setRoot(component2)

          // inform comp2 has finished render
          waitForSecondSetRootToFinishLatch.release()
        }

    // Wait for both background tasks to finish
    bgThreadLatch.acquire()

    // Both components were rendered, so we expect them both to have a render count of 1
    assertThat(counter1.renderCount).isEqualTo(1)
    assertThat(counter2.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(2)

    // Both measures should have happened, but only the the 2nd component should've been committed
    assertThat(counter1.measureCount).isEqualTo(1)
    assertThat(counter2.measureCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(2)

    // Reset the counters
    counter1.reset()
    counter2.reset()
    lithoStatsRule.resetAllCounters()

    // Now do layout to trigger a measure with the committed resolution result. We expect only the
    // 2nd component to get measured here.
    legacyLithoViewRule.setSizeSpecs(exactly(100), exactly(100)).measure().layout().idle()

    // Ensure no new renders
    assertThat(counter1.renderCount).isEqualTo(0)
    assertThat(counter2.renderCount).isEqualTo(0)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(0)

    // Ensure no new measures
    assertThat(counter1.measureCount).isEqualTo(0)
    assertThat(counter2.measureCount).isEqualTo(0)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(0)

    // Ensure we can find Comp2's Text mounted.
    assertThat(legacyLithoViewRule.findViewWithText("Comp2")).isNotNull
  }

  @Test
  fun ifSyncResolveIsInProgress_thenAsyncResolveShouldWaitForInProgressResolve() {
    val isFirstHolder = booleanArrayOf(false)
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()
    // Latch to wait for the 1st setRoot to get called to avoid calling the 2nd setRoot 1st.
    val latch = TimeOutSemaphore(0)
    val listener: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() {
            var isFirst = false
            synchronized(isFirstHolder) {
              if (!isFirstHolder[0]) {
                isFirstHolder[0] = true
                isFirst = true
              }
            }
            if (isFirst) {
              legacyLithoViewRule.componentTree.setFutureExecutionListener(
                  object : FutureExecutionListener {
                    override fun onPreExecution(
                        version: Int,
                        futureExecutionType: FutureExecutionType,
                        attribution: String
                    ) {
                      latch.release()
                    }

                    override fun onPostExecution(
                        version: Int,
                        released: Boolean,
                        attribution: String
                    ) = Unit
                  })
              legacyLithoViewRule.setRootAsync(legacyLithoViewRule.componentTree.getRoot())
              ThreadTestingUtils.runOnBackgroundThread { legacyLithoViewRule.idle() }
              latch.acquire() // wait for async set root to release latch
            }
          }

          override fun onMeasure() = Unit
        }

    // Component will be set as root 1st, but will finish after Component2
    val component =
        Column.create(c)
            .child(Text.create(c).text("Component"))
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build()

    // required to trigger async set roots
    legacyLithoViewRule.componentTree.setSizeSpec(exactly(100), exactly(100))

    // first set root
    legacyLithoViewRule.setRoot(component)

    // The second resolve should be skipped
    assertThat(counter.renderCount).isEqualTo(1)
  }

  /**
   * Testing that when setting size-specs via a background thread twice, the older one does not get
   * committed. This test will ensure that the 1st setSizeSpecs starts before the 2nd setSizeSpecs,
   * but will finish after the 2nd setSizeSpecs completes, thus ensuring the version-check when
   * committing the LayoutState is working as expected.
   *
   * We assert this is working by waiting for both background setSizeSpecs to finish, then checking
   * the committed LayoutState has the newest size-specs, despite the older one finishing last.
   */
  @Test
  fun testOlderLayoutIsNotCommitted() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()

    // Boolean holder to check if the 1st measurement has happened yet. We want to force the 1st
    // measurement to stall until the 2nd measurement completes.
    val didFirstMeasureHappenHolder = BooleanArray(1)

    // Latch to wait for the 2nd setSizeSpec to finish before the 1st one can complete
    val waitForSecondSizeSpecToFinishLatch = TimeOutSemaphore(0)

    // Latch to ensure the 1st setSizeSpec with "old" specs gets called before the 2nd one
    val waitForFirstSizeSpecToStartLatch = TimeOutSemaphore(0)
    val listener: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() = Unit

          override fun onMeasure() {
            // Waiting should only happen on the 1st measure.
            // The latches below ensure that this condition is only met for the "old" specs.
            var isFirst = false
            synchronized(didFirstMeasureHappenHolder) {
              if (!didFirstMeasureHappenHolder[0]) {
                didFirstMeasureHappenHolder[0] = true
                isFirst = true
              }
            }
            if (isFirst) {
              // First measure has started, inform 2nd measure can begin
              waitForFirstSizeSpecToStartLatch.release()

              // First measure must now wait for 2nd measure to complete
              waitForSecondSizeSpecToFinishLatch.acquire()
            }
          }
        }
    val component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build()

    // Set the root component once (sync)
    legacyLithoViewRule.setRoot(component)
    lithoStatsRule.resetAllCounters()

    // "Old" size-specs to be set first, but finish last
    val oldWidthSpec = exactly(100)
    val oldHeightSpec = exactly(100)

    // "New" size-specs to be set second, but finish 1st
    val newWidthSpec = exactly(150)
    val newHeightSpec = exactly(150)
    val componentTree = legacyLithoViewRule.componentTree

    // Background thread latch with -1 permits to ensure both bg tasks finish
    val bgThreadLatch = TimeOutSemaphore(-1)
    val output1 = Size()
    val output2 = Size()

    // Set the "old" size specs on the background
    ThreadTestingUtils.runOnBackgroundThread(
        bgThreadLatch) { // Call setSizeSpec with "old" size specs
          componentTree.setSizeSpec(oldWidthSpec, oldHeightSpec, output1)
        }

    // Set the "new" size specs on the background
    ThreadTestingUtils.runOnBackgroundThread(bgThreadLatch) { // Wait for 1st setSizeSpec to happen
      waitForFirstSizeSpecToStartLatch.acquire()

      // Call setSizeSpec with "new" size specs
      componentTree.setSizeSpec(newWidthSpec, newHeightSpec, output2)

      // Inform 2nd measure has completed
      waitForSecondSizeSpecToFinishLatch.release()
    }

    // Wait for both background threads to finish
    bgThreadLatch.acquire()
    val oldWidth = SizeSpec.getSize(oldWidthSpec)
    val oldHeight = SizeSpec.getSize(oldHeightSpec)
    val newWidth = SizeSpec.getSize(newWidthSpec)
    val newHeight = SizeSpec.getSize(newHeightSpec)

    // Ensure older setSizeSpec has the correct output
    assertThat(output1.width).isEqualTo(oldWidth)
    assertThat(output1.height).isEqualTo(oldHeight)

    // Ensure newer setSizeSpec has the correct output
    assertThat(output2.width).isEqualTo(newWidth)
    assertThat(output2.height).isEqualTo(newHeight)

    // Set root only happened once, so we expect only 1 render call.
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)

    // Despite only the newer size-specs getting committed, measure still happens twice
    assertThat(counter.measureCount).isEqualTo(2)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(2)
    val committedLayoutState = componentTree.committedLayoutState

    // Ensure the committed layout-state has the newer sizes
    assertThat(committedLayoutState).isNotNull
    assertThat(committedLayoutState?.width).isEqualTo(newWidth)
    assertThat(committedLayoutState?.height).isEqualTo(newHeight)
  }

  /**
   * When an async layout is requested while a sync layout is in progress with the same size-specs,
   * the async layout should be completely ignored in favour of the running sync one.
   *
   * This test sets up this scenario by triggering a sync layout in a bg-thread, while an async
   * layout with the same size specs is queued.
   *
   * To force the timing to be right, the async tasks are only drained (i.e., triggered) when the
   * measure from the sync layout has started. The sync layout is then blocked until the async tasks
   * have finished draining.
   *
   * We then wait for the background thread to finish, and ensure measure has only happened once,
   * and the output is populated with the corerect values.
   */
  @Test
  fun testAsyncLayoutIsSkippedWhenEquivalentSyncLayoutInProgress() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()
    val syncMeasureStartedLatch = TimeOutSemaphore(0)
    val asyncMeasureTriggeredLatch = TimeOutSemaphore(0)
    val listener: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() = Unit

          override fun onMeasure() {
            // Inform measure has started
            syncMeasureStartedLatch.release()

            // Wait for async tasks to drain
            asyncMeasureTriggeredLatch.acquire()
          }
        }
    val component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build()

    // Set the root component once (sync)
    legacyLithoViewRule.setRoot(component)
    lithoStatsRule.resetAllCounters()
    val widthSpec = exactly(100)
    val heightSpec = exactly(100)
    val componentTree = legacyLithoViewRule.componentTree
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    val output = Size()
    val bgLatch: TimeOutSemaphore =
        ThreadTestingUtils.runOnBackgroundThread {
          componentTree.setSizeSpec(widthSpec, heightSpec, output)
        }

    // Wait for sync measure to start
    syncMeasureStartedLatch.acquire()

    // Force async tasks to run
    legacyLithoViewRule.idle()

    // Inform async tasks have been drained
    asyncMeasureTriggeredLatch.release()

    // Wait for bg thread to finish
    bgLatch.acquire()

    // Measure should have only happened once
    assertThat(counter.measureCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)

    // Ensure sync measure output is correct
    assertThat(output.width).isEqualTo(100)
    assertThat(output.height).isEqualTo(100)
  }

  /**
   * When an async setRootAndSizeSpec happens, followed by a main thread layout, the async tasks
   * should be promoted to the main thread, thus ensuring resolve and measure only happen once.
   */
  @Test
  fun testSyncRenderContinuesAsyncOnMainThread() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()

    // latch to wait for the main thread layout
    val onPrepareLatch = TimeOutSemaphore(0)
    val component =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()
    val componentTree = legacyLithoViewRule.componentTree

    // wait in prepare
    onPrepareLatch.release()

    // set root and size-spec async
    componentTree.setRootAndSizeSpecAsync(component, exactly(100), exactly(100))
    componentTree.setFutureExecutionListener(
        object : FutureExecutionListener {
          override fun onPreExecution(
              version: Int,
              futureExecutionType: FutureExecutionType,
              attribution: String
          ) {
            componentTree.setFutureExecutionListener(null)

            // unblock the async resolve
            onPrepareLatch.acquire()
          }

          override fun onPostExecution(version: Int, released: Boolean, attribution: String) = Unit
        })

    // request a main thread layout
    componentTree.measure(exactly(200), exactly(200), intArrayOf(0, 0), false)

    // Ensure render and measure only happened once
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    assertThat(counter.measureCount).isEqualTo(1)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(1)

    // verify that the layout is measured against the new size
    assertThat(legacyLithoViewRule.committedLayoutState?.width).isEqualTo(200)
    assertThat(legacyLithoViewRule.committedLayoutState?.height).isEqualTo(200)
  }

  /**
   * When an async setRootAndSizeSpec happens, followed by an equivalent sync call, we expect the
   * async process to be promoted to the main thread, thus ensuring render and measure only happen
   * once.
   *
   * The test verifies this behaviour works correctly during layout by triggering an async
   * setRootAndSizeSpec, waiting for layout to begin, and then triggering an equivalent sync
   * process.
   */
  @Test
  fun testSyncLayoutContinuesAsyncOnMainThread() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()

    // Latch to wait for async measure to begin before sync render
    val waitForAsyncMeasureToStartLatch = TimeOutSemaphore(0)

    // Latch to wait for 2nd future pre-execution
    val waitForSecondFuturePreExecutionLatch = TimeOutSemaphore(0)
    val listener: RenderAndLayoutCountingTesterSpec.Listener =
        object : RenderAndLayoutCountingTesterSpec.Listener {
          override fun onPrepare() = Unit

          override fun onMeasure() {
            // Inform async measure has started
            waitForAsyncMeasureToStartLatch.release()

            // Wait for 2nd future pre-execution
            waitForSecondFuturePreExecutionLatch.acquire()
          }
        }
    val component =
        Column.create(c)
            .child(
                RenderAndLayoutCountingTester.create(c)
                    .renderAndMeasureCounter(counter)
                    .listener(listener))
            .build()
    val widthSpec = exactly(100)
    val heightSpec = exactly(100)
    val componentTree = legacyLithoViewRule.componentTree

    // Set root and size-spec async
    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec)

    // run to end of tasks on background to avoid blocking here
    val bgThreadLatch: TimeOutSemaphore =
        ThreadTestingUtils.runOnBackgroundThread { legacyLithoViewRule.runToEndOfBackgroundTasks() }

    // Wait for async measure to start
    waitForAsyncMeasureToStartLatch.acquire()
    val isFutureReusedHolder = BooleanArray(1)
    componentTree.setFutureExecutionListener(
        object : FutureExecutionListener {
          override fun onPreExecution(
              version: Int,
              futureExecutionType: FutureExecutionType,
              attribution: String
          ) {
            componentTree.setFutureExecutionListener(null)

            // Inform second future pre-execution has occurred.
            waitForSecondFuturePreExecutionLatch.release()
            isFutureReusedHolder[0] = futureExecutionType == FutureExecutionType.REUSE_FUTURE
          }

          override fun onPostExecution(version: Int, released: Boolean, attribution: String) = Unit
        })

    // Set root and sync-spec sync
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec)

    // Let the bg thread finish
    bgThreadLatch.acquire()

    // Ensure render and measure only happened once
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    assertThat(counter.measureCount).isEqualTo(1)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(1)

    // Ensure 2nd future is reused
    assertThat(isFutureReusedHolder[0]).isTrue
  }

  /**
   * When splitting futures, we include an optimisation to avoid blocking the UI thread when setting
   * root sync when there's no size-specs. Without split futures, nothing would happen here, so when
   * introducing split futures, we want to ensure that we're still not blocking the UI thread.
   *
   * This test verifies this behaviour by calling setRoot (sync) and ensuring that prior to running
   * to the end of tasks, no render has happened. Then we run to the end of tasks and ensure that
   * the render did happen.
   *
   * In all cases, measure should not happen.
   */
  @Test
  fun testSetRootWithNoSizeSpecsHappensAsync() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()
    val component =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()

    // Set root here should force the operation to become async
    legacyLithoViewRule.setRoot(component)
    lithoStatsRule.resetAllCounters()

    // Setting root without size-specs should force the operation to become async, so we verify
    // that render did not happen yet.
    assertThat(counter.renderCount).isEqualTo(0)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(0)
    assertThat(counter.measureCount).isEqualTo(0)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(0)

    // Run to end of tasks, forcing async task to complete
    legacyLithoViewRule.idle()

    // Now that we've run to end of tasks, we can expect render to have happened (tho no measure)
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    assertThat(counter.measureCount).isEqualTo(0)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(0)
  }

  /**
   * The check of whether or not render can be skipped includes a ref-equality check on the
   * tree-props. This test ensures that when tree-props are set once, and the same root is set twice
   * (with different size-specs), render doesn't happen twice.
   *
   * If this test breaks, it's likely due to a change in ComponentTree that makes copies of
   * tree-props.
   */
  @Test
  fun testNoChangeToTreeProps_renderStillHappensOnce() {
    val c = legacyLithoViewRule.context
    val counter = RenderAndMeasureCounter()
    val component =
        Column.create(c)
            .child(RenderAndLayoutCountingTester.create(c).renderAndMeasureCounter(counter))
            .build()
    val output = Size()

    // Define tree-props on the CT
    val treeProps = TreeProps()
    legacyLithoViewRule.componentTree.setRootAndSizeSpecSync(
        component, exactly(100), exactly(100), output, treeProps) // Set new tree props.

    // Ensure render and measure happened once, and the output is set correctly
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(counter.measureCount).isEqualTo(1)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(1)
    assertThat(output.width).isEqualTo(100)
    assertThat(output.height).isEqualTo(100)

    // Set the same root again, with the same tree-props, but different size-specs
    legacyLithoViewRule.componentTree.setRootAndSizeSpecSync(
        component, exactly(150), exactly(150), output, treeProps) // Use the same tree props

    // Same component, so render count should still be 1.
    // Measure should increment, and the output should be equal to the new values.
    assertThat(counter.renderCount).isEqualTo(1)
    assertThat(counter.measureCount).isEqualTo(2)
    assertThat(lithoStatsRule.resolveCount).isEqualTo(1)
    assertThat(lithoStatsRule.layoutCount).isEqualTo(2)
    assertThat(output.width).isEqualTo(150)
    assertThat(output.height).isEqualTo(150)
  }
}
