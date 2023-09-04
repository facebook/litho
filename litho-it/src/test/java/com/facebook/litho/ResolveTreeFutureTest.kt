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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.ResolveTreeFutureTest.BlockedRenderComponent
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ResolveTreeFutureTest {

  private lateinit var componentContext: ComponentContext

  @Before
  fun setup() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    componentContext = ComponentContextUtils.withComponentTree(c, ComponentTree.create(c).build())
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
    val resolveTreeFuture =
        ResolveTreeFuture(
            componentContext,
            component,
            TreeState(),
            null,
            null,
            0,
            true,
            1,
            null,
            RenderSource.SET_ROOT_ASYNC)

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
        backgroundTaskCompleteHolder, 0, "Timeout waiting for background runAndGet thread to start")

    // In the background, wait a short interval, then unblock the BlockedRenderComponent's render
    // function
    object : Thread() {
          override fun run() {
            shortWait()
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
        "Timeout waiting for background runAndGet thread to finish")

    // Ensure the render-result from the background thread is null, meaning it was properly
    // interrupted.
    assertThat(resolveResultHolder[0]).isNull()

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
            .child(Text.create(componentContext).text("Hello World"))
            .build()
    val widthSpec = exactly(100)
    val heightSpec = exactly(100)
    val resolveTreeFuture =
        ResolveTreeFuture(
            componentContext,
            component,
            TreeState(),
            null,
            null,
            0,
            true,
            1,
            null,
            RenderSource.SET_ROOT_SYNC)
    val renderResult = resolveTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(renderResult).isNotNull
    val layoutTreeFuture =
        LayoutTreeFuture(
            renderResult,
            null,
            null,
            null,
            widthSpec,
            heightSpec,
            -1,
            0,
            true,
            RenderSource.SET_ROOT_SYNC)
    val layoutState = layoutTreeFuture.runAndGet(RenderSource.SET_ROOT_SYNC).result
    assertThat(layoutState).isNotNull
    val renderTree = layoutState?.toRenderTree()
    assertThat(renderTree).isNotNull
    assertThat(renderTree?.mountableOutputCount).isEqualTo(2)
  }

  /**
   * Component class that includes a "busy-wait" that blocks the render method. The busy-wait waits
   * until a provided "canRenderBoolean" is set to true.
   */
  internal class BlockedRenderComponent(
      private val canRenderBoolean: BooleanArray,
      private val renderCountHolder: IntArray
  ) : SpecGeneratedComponent("BlockedRenderComponent") {
    override fun render(
        resolveContext: ResolveContext,
        c: ComponentContext,
        widthSpec: Int,
        heightSpec: Int
    ): RenderResult {
      // Hang until the can-render-boolean is set to true
      waitForMilestone(canRenderBoolean, 0, "Timeout while BlockedRenderComponent blocked render")

      // Increment the render count
      renderCountHolder[0]++

      // Return a render result.
      return RenderResult(Row.create(c).child(Text.create(c).text("Hello World")).build())
    }
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
        errorMessage: String
    ) {
      val startTime = System.currentTimeMillis()
      while (!milestoneHolder[milestoneIndex]) {
        shortWait()
        if (System.currentTimeMillis() - startTime > 2_000) {
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
