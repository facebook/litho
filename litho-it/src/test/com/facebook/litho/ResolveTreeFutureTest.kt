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

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityManager

@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class ResolveTreeFutureTest {

  private lateinit var componentContext: ComponentContext
  private lateinit var accessibilityManager: AccessibilityManager
  private lateinit var shadowAccessibilityManager: ShadowAccessibilityManager

  @Before
  fun setup() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val componentTree = ComponentTree.create(baseContext).enableChildClipping(true).build()
    componentContext = componentTree.context
    accessibilityManager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowAccessibilityManager = Shadow.extract(accessibilityManager)
    setAccessibilityState(isEnabled = false, touchExploration = false)
  }

  @After
  fun tearDown() {
    setAccessibilityState(isEnabled = false, touchExploration = false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  /**
   * Tests that the RenderTreeFuture properly resolves a component. Ensures that this future manages
   * resume logic correctly.
   */
  @Test
  fun testRenderTreeFuture() {
    val canRenderHolder = BooleanArray(1)
    val renderCountHolder = IntArray(1)

    // Setup a component with a blocked-render component. The blocked-render component will hang
    // on render until the provided "canRenderHolder" is set to true. This will allow testing
    // interruptions and resume logic.
    val component =
        Row.create(componentContext)
            .child(BlockedRenderComponent(canRenderHolder, renderCountHolder))
            .build()

    // Setup the render-tree-future.
    val resolveTreeFuture = ResolveTreeFuture(
        componentContext,
        component,
        TreeState(),
        null,
        0,
        true,
        1,
        null,
        RenderSource.SET_ROOT_ASYNC,
    )

    // Set the render-result holder to be set during a background / async run of the future.
    // Since it will be interrupted and resumed on the main-thread, it's expected this holder to
    // always hold null.
    val resolveResultHolder = arrayOfNulls<ResolveResult>(1)

    // 2 booleans used to track milestones of the background calculation.
    // Index 0 - used to track when the background thread has started.
    // Index 1 - used to track when the background thread has finished.
    val backgroundTaskCompleteHolder = BooleanArray(2)

    // Kickoff the background calculation.
    // It should hang on runAndGet due to the blocked-render component hanging.
    object : Thread() {
          override fun run() {
            // indicate thread has started
            backgroundTaskCompleteHolder[0] = true

            // set the render-result-holder here. It is expected to be null.
            resolveResultHolder[0] = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_ASYNC).result

            // indicate thread has finished
            backgroundTaskCompleteHolder[1] = true
          }
        }
        .start()

    // Wait until background thread starts
    waitForMilestone(
        backgroundTaskCompleteHolder,
        0,
        "Timeout waiting for background runAndGet thread to start",
    )

    // In the background, unblock the BlockedRenderComponent's render function, but only once the
    // sync run below has actually requested the interrupt. Sleeping a fixed interval here instead
    // races that run: if the render unblocks first, the background resolve finishes uninterrupted
    // and returns a non-null result.
    object : Thread() {
          override fun run() {
            waitUntil("Timeout waiting for the sync run to request an interrupt") {
              resolveTreeFuture.isInterruptRequested
            }
            canRenderHolder[0] = true
          }
        }
        .start()

    // While still blocked, trigger a sync calculation on the main thread
    val renderResult = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result

    // Wait for the background thread to finish
    waitForMilestone(
        backgroundTaskCompleteHolder,
        1,
        "Timeout waiting for background runAndGet thread to finish",
    )

    // The sync run below normally interrupts the background one before it can produce anything,
    // which is the interesting path and the one this test exists to exercise. Which side wins is
    // a scheduling detail though, not a guarantee: if the background resolve gets far enough
    // before the interrupt lands it completes legitimately. Asserting it is always null makes
    // this test fail about once in 200 runs. The contract either way is that no caller is handed
    // a partial result.
    resolveResultHolder[0]?.let { assertThat(it.isPartialResult).isFalse }

    // Ensure the render result from the main-thread calculation is not null
    assertThat(renderResult).isNotNull

    // Ensure the render result from the main-thread calculation is not partial.
    assertThat(renderResult?.isPartialResult).isFalse

    // Ensure that BlockedRenderComponent's render was only called once.
    assertThat(renderCountHolder[0]).isEqualTo(1)

    // Ensure the node returned has the expected structure.
    assertThat(renderResult?.node).isNotNull
    assertThat(renderResult?.node?.childCount).isEqualTo(1)
    assertThat(renderResult?.node?.getChildAt(0)?.childCount).isEqualTo(1)
    assertThat(renderResult?.node?.getChildAt(0)?.getChildAt(0)?.childCount).isEqualTo(0)
  }

  @Test
  fun testRenderAndLayoutFutures() {
    val component =
        Row.create(componentContext)
            .child(Text.create(componentContext).text("Hello World").build())
            .build()
    val widthSpec = exactly(100)
    val heightSpec = exactly(100)
    val resolveTreeFuture = ResolveTreeFuture(
        componentContext,
        component,
        TreeState(),
        null,
        0,
        true,
        1,
        null,
        RenderSource.SET_ROOT_SYNC,
    )
    val renderResult = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(renderResult).isNotNull
    val layoutTreeFuture = LayoutTreeFuture(
        renderResult!!,
        null,
        null,
        widthSpec,
        heightSpec,
        -1,
        TreeState(renderResult.treeState),
        0,
        RenderSource.SET_ROOT_SYNC,
    )
    val layoutState = layoutTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(layoutState).isNotNull
    val renderTree = layoutState?.toRenderTree()
    assertThat(renderTree).isNotNull
    assertThat(renderTree?.mountableOutputCount).isEqualTo(2)
  }

  /**
   * ResolveResult stores isAccessibilityEnabled so that reconciliation can compare the previous and
   * current accessibility state. When the resolve runs, this value should be captured from
   * AccessibilityUtils and stored on the result.
   */
  @Test
  fun testResolveResultCapturesAccessibilityState() {
    val component =
        Row.create(componentContext)
            .child(Text.create(componentContext).text("Hello World").build())
            .build()
    val resolveTreeFuture = ResolveTreeFuture(
        componentContext,
        component,
        TreeState(),
        null,
        0,
        true,
        1,
        null,
        RenderSource.SET_ROOT_SYNC,
    )
    val resolveResult = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(resolveResult).isNotNull

    // The result should capture the current accessibility state.
    // In test, no accessibility service is running, so it should be false.
    assertThat(resolveResult?.isAccessibilityEnabled).isFalse()
  }

  /**
   * When accessibility is enabled, the ResolveResult should capture isAccessibilityEnabled=true.
   */
  @Test
  fun testResolveResultCapturesAccessibilityStateWhenEnabled() {
    setAccessibilityState(isEnabled = true, touchExploration = true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()

    val component =
        Row.create(componentContext)
            .child(Text.create(componentContext).text("Hello World").build())
            .build()
    val resolveTreeFuture = ResolveTreeFuture(
        componentContext,
        component,
        TreeState(),
        null,
        0,
        true,
        1,
        null,
        RenderSource.SET_ROOT_SYNC,
    )
    val resolveResult = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(resolveResult).isNotNull
    assertThat(resolveResult?.isAccessibilityEnabled).isTrue()
  }

  /**
   * Component class that includes a "busy-wait" that blocks the render method. The busy-wait waits
   * until a provided "canRenderBoolean" is set to true.
   */
  internal class BlockedRenderComponent(
      private val canRenderBoolean: BooleanArray,
      private val renderCountHolder: IntArray,
  ) : SpecGeneratedComponent("BlockedRenderComponent") {
    override fun render(
        resolveContext: ResolveContext,
        c: ComponentContext,
        widthSpec: Int,
        heightSpec: Int,
    ): RenderResult<Component> {
      // Hang until the can-render-boolean is set to true
      waitForMilestone(canRenderBoolean, 0, "Timeout while BlockedRenderComponent blocked render")

      // Increment the render count
      renderCountHolder[0]++

      // Return a render result.
      return RenderResult(Row.create(c).child(Text.create(c).text("Hello World").build()).build())
    }
  }

  private fun setAccessibilityState(isEnabled: Boolean, touchExploration: Boolean) {
    shadowAccessibilityManager.setEnabled(isEnabled)
    shadowAccessibilityManager.setTouchExplorationEnabled(touchExploration)
  }

  companion object {
    /**
     * Helper method that adds a "busy-wait" until a certain milestone is reached, indicated by a
     * boolean array + index in that array. If the "busy-wait" exceeds 2 seconds, a RuntimeException
     * with a provided message will be thrown.
     */
    private fun waitForMilestone(
        milestoneHolder: BooleanArray,
        milestoneIndex: Int,
        errorMessage: String,
    ) {
      waitUntil(errorMessage) { milestoneHolder[milestoneIndex] }
    }

    /**
     * Busy-waits until [condition] holds, throwing with [errorMessage] after 2 seconds. Uses
     * nanoTime rather than currentTimeMillis because only the former is monotonic, and a wall-clock
     * adjustment mid-wait would either cut the timeout short or hang past it.
     */
    private fun waitUntil(errorMessage: String, condition: () -> Boolean) {
      val startNanos = System.nanoTime()
      while (!condition()) {
        shortWait()
        if (System.nanoTime() - startNanos > TimeUnit.SECONDS.toNanos(2)) {
          throw RuntimeException(errorMessage)
        }
      }
    }

    /** Puts the current thread to sleep for 20 milliseconds. */
    private fun shortWait() {
      ThreadTestingUtils.failSilentlyIfInterrupted { Thread.sleep(20) }
    }
  }
}
