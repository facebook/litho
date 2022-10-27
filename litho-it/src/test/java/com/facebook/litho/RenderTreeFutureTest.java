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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.rendercore.RenderTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class RenderTreeFutureTest {
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    mComponentContext = ComponentContext.withComponentTree(c, ComponentTree.create(c).build());
  }

  /**
   * Tests that the RenderTreeFuture properly resolves a component. Ensures that this future manages
   * resume logic correctly.
   */
  @Test
  public void testRenderTreeFuture() {
    final boolean[] canRenderHolder = new boolean[1];
    final int[] renderCountHolder = new int[1];

    // Setup a component with a blocked-render component. The blocked-render component will hang
    // on render until the provided "canRenderHolder" is set to true. This will allow testing
    // interruptions and resume logic.
    final Component component =
        Row.create(mComponentContext)
            .child(new BlockedRenderComponent(canRenderHolder, renderCountHolder))
            .build();

    // Setup the render-tree-future.
    final RenderTreeFuture renderTreeFuture =
        new RenderTreeFuture(mComponentContext, component, new TreeState(), null, null, 0);

    // Set the render-result holder to be set during a background / async run of the future.
    // Since it will be interrupted and resumed on the main-thread, it's expected this holder to
    // always hold null.
    final LithoResolutionResult[] lithoResolutionResultHolder = new LithoResolutionResult[1];

    // 2 booleans used to track milestones of the background calculation.
    // Index 0 - used to track when the background thread has started.
    // Index 1 - used to track when the background thread has finished.
    final boolean[] backgroundTaskCompleteHolder = new boolean[2];

    // Kickoff the background calculation.
    // It should hang on runAndGet due to the blocked-render component hanging.
    new Thread() {
      @Override
      public void run() {
        // indicate thread has started
        backgroundTaskCompleteHolder[0] = true;

        // set the render-result-holder here. It is expected to be null.
        lithoResolutionResultHolder[0] =
            renderTreeFuture.runAndGet(LayoutState.CalculateLayoutSource.SET_ROOT_ASYNC);

        // indicate thread has finished
        backgroundTaskCompleteHolder[1] = true;
      }
    }.start();

    // Wait until background thread starts
    waitForMilestone(
        backgroundTaskCompleteHolder,
        0,
        "Timeout waiting for background runAndGet thread to start");

    // In the background, wait a short interval, then unblock the BlockedRenderComponent's render
    // function
    new Thread() {
      @Override
      public void run() {
        shortWait();
        canRenderHolder[0] = true;
      }
    }.start();

    // While still blocked, trigger a sync calculation on the main thread
    final LithoResolutionResult renderResult =
        renderTreeFuture.runAndGet(LayoutState.CalculateLayoutSource.SET_ROOT_SYNC);

    // Wait for the background thread to finish
    waitForMilestone(
        backgroundTaskCompleteHolder,
        1,
        "Timeout waiting for background runAndGet thread to finish");

    // Ensure the render-result from the background thread is null, meaning it was properly
    // interrupted.
    assertThat(lithoResolutionResultHolder[0]).isNull();

    // Ensure the render result from the main-thread calculation is not null
    assertThat(renderResult).isNotNull();

    // Ensure the render result from the main-thread calculation is not partial.
    assertThat(renderResult.isPartialResult).isFalse();

    // Ensure that BlockedRenderComponent's render was only called once.
    assertThat(renderCountHolder[0]).isEqualTo(1);

    // Ensure the node returned has the expected structure.
    assertThat(renderResult.node).isNotNull();
    assertThat(renderResult.node.getChildCount()).isEqualTo(1);
    assertThat(renderResult.node.getChildAt(0).getChildCount()).isEqualTo(1);
    assertThat(renderResult.node.getChildAt(0).getChildAt(0).getChildCount()).isEqualTo(0);
  }

  @Test
  public void testRenderAndLayoutFutures() {
    final Component component =
        Row.create(mComponentContext)
            .child(Text.create(mComponentContext).text("Hello World"))
            .build();

    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);

    final RenderTreeFuture renderTreeFuture =
        new RenderTreeFuture(mComponentContext, component, new TreeState(), null, null, 0);

    final LithoResolutionResult renderResult =
        renderTreeFuture.runAndGet(LayoutState.CalculateLayoutSource.SET_ROOT_SYNC);

    final LayoutTreeFuture layoutTreeFuture =
        new LayoutTreeFuture(
            renderResult,
            mComponentContext,
            component,
            null,
            null,
            null,
            widthSpec,
            heightSpec,
            mComponentContext.getComponentTree().mId,
            0);

    final LayoutState layoutState =
        layoutTreeFuture.runAndGet(LayoutState.CalculateLayoutSource.SET_ROOT_SYNC);

    final RenderTree renderTree = layoutState.toRenderTree();

    assertThat(renderTree).isNotNull();
    assertThat(renderTree.getMountableOutputCount()).isEqualTo(2);
  }

  /**
   * Helper method that adds a "busy-wait" until a certain milestone is reached, indicated by a
   * boolean array + index in that array. If the "busy-wait" exceeds 2 seconds, a RuntimeException
   * with a provided message will be thrown.
   */
  private static void waitForMilestone(
      final boolean[] milestoneHolder, final int milestoneIndex, final String errorMessage) {
    final long startTime = System.currentTimeMillis();
    while (!milestoneHolder[milestoneIndex]) {
      shortWait();
      if (System.currentTimeMillis() - startTime > 2000) {
        throw new RuntimeException(errorMessage);
      }
    }
  }

  /** Puts the current thread to sleep for 20 milliseconds. */
  private static void shortWait() {
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  /**
   * Component class that includes a "busy-wait" that blocks the render method. The busy-wait waits
   * until a provided "canRenderBoolean" is set to true.
   */
  static class BlockedRenderComponent extends Component {
    private final boolean[] mCanRenderBoolean;
    private final int[] mRenderCountHolder;

    public BlockedRenderComponent(boolean[] canRenderBoolean, int[] renderCountHolder) {
      mCanRenderBoolean = canRenderBoolean;
      mRenderCountHolder = renderCountHolder;
    }

    @Override
    protected RenderResult render(
        RenderStateContext renderStateContext, ComponentContext c, int widthSpec, int heightSpec) {
      // Hang until the can-render-boolean is set to true
      waitForMilestone(mCanRenderBoolean, 0, "Timeout while BlockedRenderComponent blocked render");

      // Increment the render count
      mRenderCountHolder[0]++;

      // Return a render result.
      return new RenderResult(Row.create(c).child(Text.create(c).text("Hello World")).build());
    }
  }
}
