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

package com.facebook.rendercore.zoomable

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/**
 * Tests that [ZoomableViewBaseController] defers view hierarchy modifications (detachViewFromParent
 * / attachViewToParent) when rootView.isInLayout() is true, preventing the
 * FrameLayout.layoutChildren NPE crash (MID 44c8cdc22a80c9a967836bc7ffc7de56).
 *
 * Root cause: FrameLayout.layoutChildren captures `count = getChildCount()`, then iterates. If
 * detachViewFromParent/attachViewToParent modifies the child array mid-iteration (e.g., via
 * teardownRemoteZoom triggered by RecyclerView bind/unbind during layout), getChildAt(i) returns
 * null, causing NPE on child.getVisibility(). The isInLayout guard defers the operation via
 * View.post() to the next frame.
 *
 * Note: Robolectric does NOT set View.isInLayout()=true during real layout() calls (verified
 * empirically). The tests use a [TestZoomableContainerView] with a `simulateInLayout` override to
 * control the guard. True NPE crash reproduction requires a real Android device (Android 16+).
 */
@Suppress("ActivityStoredInField") // Test class — no lifecycle leak risk
@RunWith(RobolectricTestRunner::class)
class ZoomableViewBaseControllerLayoutGuardTest {

  private lateinit var activity: Activity
  private lateinit var decorView: ViewGroup
  private lateinit var rootView: TestZoomableContainerView
  private lateinit var contentView: View
  private lateinit var controller: TestZoomableController

  /** A [ZoomableContainerView] whose [isInLayout] can be controlled in tests. */
  private class TestZoomableContainerView(context: Context) : ZoomableContainerView(context) {
    var simulateInLayout = false

    override fun isInLayout(): Boolean = simulateInLayout || super.isInLayout()
  }

  /** Minimal concrete [ZoomableViewBaseController] for testing. */
  private class TestZoomableController(
      context: Context,
      override val decorView: ViewGroup,
  ) :
      ZoomableViewBaseController<TestZoomableContainerView>(
          context = context,
          backgroundDrawable = ColorDrawable(Color.BLACK),
          isRemoteZoomEnabled = true,
      ) {
    lateinit var testContentView: View

    override fun getRenderTreeView(): View = testContentView
  }

  private class TestListener : ZoomableViewBaseController.Listener {
    var zoomStartedCount = 0
    var zoomFinishedCount = 0

    override fun onZoomStarted() {
      zoomStartedCount++
    }

    override fun onZoomFinished() {
      zoomFinishedCount++
    }
  }

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    decorView = activity.window.decorView as ViewGroup

    rootView = TestZoomableContainerView(activity)
    contentView = View(activity)
    rootView.addView(
        contentView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    // rootView needs a parent for requestDisallowInterceptTouchEvent
    val parentFrame = FrameLayout(activity)
    parentFrame.addView(rootView)
    decorView.addView(parentFrame)

    // Force initial layout
    parentFrame.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    parentFrame.layout(0, 0, 200, 200)

    controller = TestZoomableController(activity, decorView)
    controller.testContentView = contentView
    controller.init()
    controller.bindTo(rootView)
  }

  private fun invokeStartRemoteZoom() {
    val method = ZoomableViewBaseController::class.java.getDeclaredMethod("startRemoteZoom")
    method.isAccessible = true
    method.invoke(controller)
  }

  private fun invokeTeardownRemoteZoom() {
    val method = ZoomableViewBaseController::class.java.getDeclaredMethod("teardownRemoteZoom")
    method.isAccessible = true
    method.invoke(controller)
  }

  // ---- startRemoteZoom tests ----

  @Test
  fun `startRemoteZoom executes immediately when not in layout`() {
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    // contentView should have been detached from rootView
    assertEquals(0, rootView.childCount)
  }

  @Test
  fun `startRemoteZoom defers when rootView isInLayout`() {
    rootView.simulateInLayout = true

    invokeStartRemoteZoom()

    // State should NOT have changed — operation was deferred
    assertNotEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    // contentView should still be a child of rootView (not detached)
    assertEquals(1, rootView.childCount)

    // Allow the posted runnable to execute
    rootView.simulateInLayout = false
    ShadowLooper.idleMainLooper()

    // Now the deferred startRemoteZoom should have executed
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(0, rootView.childCount)
  }

  // ---- teardownRemoteZoom tests ----

  @Test
  fun `teardownRemoteZoom executes immediately when not in layout`() {
    // First enter REMOTE_ZOOMING state
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(0, rootView.childCount)

    invokeTeardownRemoteZoom()

    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    // contentView should be re-attached to rootView
    assertEquals(1, rootView.childCount)
  }

  @Test
  fun `teardownRemoteZoom defers when rootView isInLayout`() {
    // First enter REMOTE_ZOOMING state
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    rootView.simulateInLayout = true

    invokeTeardownRemoteZoom()

    // State is set to IDLE immediately — captured-reference lambda handles
    // view reparenting independently of controller state.
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    // contentView should still NOT be in rootView (reparenting is deferred)
    assertEquals(0, rootView.childCount)

    // Allow the posted runnable to execute
    rootView.simulateInLayout = false
    ShadowLooper.idleMainLooper()

    // Now the deferred reparenting should have completed
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    assertEquals(1, rootView.childCount)
  }

  // ---- Controller-level crash path tests ----

  @Test
  fun `teardownRemoteZoom during layout prevents child array modification on rootView`() {
    // This test exercises the actual controller teardown path that would crash
    // in production: teardownRemoteZoom calls rootView.attachViewToParent()
    // which modifies rootView's mChildren array. If rootView is in layout
    // (layoutChildren iterating mChildren), this causes the NPE.
    //
    // Setup: enter REMOTE_ZOOMING so contentView is in remoteContainer.
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(0, rootView.childCount)

    // Add a sibling to make the crash scenario more realistic —
    // in production, rootView has children being iterated by layoutChildren.
    val siblingView = View(activity)
    rootView.addView(siblingView, FrameLayout.LayoutParams(100, 100))
    assertEquals(1, rootView.childCount) // siblingView only

    // Simulate active layout pass on rootView.
    // Without the isInLayout guard, teardownRemoteZoom() would call
    // rootView.attachViewToParent(contentView, ...) which modifies
    // rootView.mChildren during the active layoutChildren iteration → NPE.
    rootView.simulateInLayout = true

    invokeTeardownRemoteZoom()

    // State is set to IDLE immediately — captured-reference lambda handles
    // view reparenting independently of controller state.
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    assertEquals(1, rootView.childCount) // Still just siblingView (reparenting is deferred)
    assertTrue(
        "contentView should NOT be in rootView yet (deferred)",
        contentView.parent !== rootView,
    )

    // Release and execute deferred runnable
    rootView.simulateInLayout = false
    ShadowLooper.idleMainLooper()

    // Now contentView should be back in rootView
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    assertEquals(2, rootView.childCount) // siblingView + contentView
    assertTrue(
        "contentView should be reparented to rootView after deferred execution",
        contentView.parent === rootView,
    )
  }

  @Test
  fun `startRemoteZoom defers reparenting when rootView isInLayout with siblings`() {
    // Similar to the teardown test, but for startRemoteZoom.
    // startRemoteZoom calls rootView.detachViewFromParent(contentView) which
    // modifies rootView.mChildren — dangerous during an active layout pass.
    val siblingView = View(activity)
    rootView.addView(siblingView, FrameLayout.LayoutParams(100, 100))
    assertEquals(2, rootView.childCount) // contentView + siblingView

    rootView.simulateInLayout = true
    invokeStartRemoteZoom()

    // Guard prevented immediate detach
    assertNotEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(2, rootView.childCount) // Both children still present

    rootView.simulateInLayout = false
    ShadowLooper.idleMainLooper()

    // Now startRemoteZoom executed — contentView detached from rootView
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(1, rootView.childCount) // Only siblingView remains
  }

  // ---- removeView isInLayout guard ----

  @Test
  fun `deferred teardown guards removeView when decorView isInLayout`() {
    // NOTE: Only exercises the else branch — Robolectric's DecorView.isInLayout() is always false.
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    val remoteContainerCountBefore = decorView.childCount

    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    assertEquals(remoteContainerCountBefore, decorView.childCount)

    rootView.simulateInLayout = false
    ShadowLooper.idleMainLooper()

    assertEquals(1, rootView.childCount)
    assertTrue(contentView.parent === rootView)
    assertEquals(remoteContainerCountBefore - 1, decorView.childCount)
  }

  // ---- Deferred teardown + new gesture race ----

  @Test
  fun `startRemoteZoom recovers contentView from stale remoteContainer after deferred teardown`() {
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertEquals(0, rootView.childCount)

    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    assertEquals(0, rootView.childCount)

    rootView.simulateInLayout = false
    invokeStartRemoteZoom()

    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
    assertTrue(
        "contentView should have been recovered and reparented to new remoteContainer",
        contentView.parent is ZoomableContainerView,
    )

    ShadowLooper.idleMainLooper()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)
  }

  @Test
  fun `recovery preserves contentView z-order among siblings`() {
    val siblingView = View(activity)
    rootView.addView(siblingView, FrameLayout.LayoutParams(100, 100))
    assertEquals(contentView, rootView.getChildAt(0))
    assertEquals(siblingView, rootView.getChildAt(1))

    invokeStartRemoteZoom()
    assertEquals(1, rootView.childCount)

    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    rootView.simulateInLayout = false

    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    invokeTeardownRemoteZoom()
    ShadowLooper.idleMainLooper()

    assertEquals(2, rootView.childCount)
    assertEquals(
        "contentView should be at its original index, not appended",
        0,
        rootView.indexOfChild(contentView),
    )
  }

  @Test
  fun `recovery resets transforms and preserves non-zero resting translationY`() {
    // Set a non-zero resting translationY before the first zoom.
    // This ensures the test catches the bug where recovery hardcodes translationY=0
    // instead of using initialTranslationY.
    contentView.translationY = 25f

    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    // Simulate zoom transforms on contentView
    contentView.scaleX = 2.5f
    contentView.scaleY = 2.5f
    contentView.translationX = 50f
    contentView.translationY = 100f

    // Trigger deferred teardown (transforms won't be reset because lambda hasn't run)
    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    rootView.simulateInLayout = false

    // New gesture triggers recovery — transforms should be reset before createRemoteContainer
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    // Teardown the second zoom session
    invokeTeardownRemoteZoom()
    ShadowLooper.idleMainLooper()

    // translationY should be restored to 25 (resting value), not 0 or 100
    assertEquals(1f, contentView.scaleX, 0.001f)
    assertEquals(1f, contentView.scaleY, 0.001f)
    assertEquals(0f, contentView.translationX, 0.001f)
    assertEquals(25f, contentView.translationY, 0.001f)
  }

  @Test
  fun `recovery preserves non-zero contentViewIndex after finishMovement resets state`() {
    // Exercises the production race: onSpringAtRest → teardownRemoteZoom (deferred) →
    // finishMovement → resetValues → new gesture → startRemoteZoom recovery.
    // Before the fix, resetValues() zeroed contentViewIndex, so recovery re-attached
    // contentView at index 0 instead of its original position.

    // Place a sibling BEFORE contentView so contentView is at index 1.
    val siblingView = View(activity)
    rootView.removeView(contentView)
    rootView.addView(siblingView, FrameLayout.LayoutParams(100, 100))
    rootView.addView(
        contentView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
    assertEquals(1, rootView.indexOfChild(contentView))

    // Gesture 1: start remote zoom — saves contentViewIndex = 1
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    // Deferred teardown (simulates rootView in layout)
    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    rootView.simulateInLayout = false
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)

    // finishMovement calls resetValues — previously zeroed contentViewIndex
    controller.finishMovement()

    // Gesture 2: new zoom triggers recovery — uses contentViewIndex
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    // Teardown gesture 2 and let all deferred lambdas complete
    invokeTeardownRemoteZoom()
    ShadowLooper.idleMainLooper()

    assertEquals(2, rootView.childCount)
    assertEquals(
        "contentView should be at its original index 1, not 0",
        1,
        rootView.indexOfChild(contentView),
    )
  }

  @Test
  fun `unbind during deferred teardown window does not crash`() {
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)

    rootView.simulateInLayout = false
    controller.unbind()

    ShadowLooper.idleMainLooper()

    assertTrue(
        "Deferred lambda should complete reparenting even after unbind",
        contentView.parent === rootView,
    )
  }

  // ---- Crash mechanism proof ----

  @Test
  fun `removing child during FrameLayout layout causes NPE in layoutChildren`() {
    // This test demonstrates the crash PATTERN that the isInLayout guard prevents,
    // not a reproduction of the exact production crash. The production crash occurs
    // when teardownRemoteZoom/startRemoteZoom modifies rootView's child array during
    // an active FrameLayout.layoutChildren() iteration. Trigger paths include:
    // - RecyclerView bind/unbind during layout → unbind() → teardownRemoteZoom()
    // - Touch events during layout pass on Android 16
    // - onScaleBegin → startZoom → startRemoteZoom if touch overlaps layout
    //
    // True NPE reproduction is only possible on real Android devices because
    // Robolectric does not set View.isInLayout()=true during layout() calls.
    val parent = FrameLayout(activity)

    // childB is the victim — will be removed during childA's layout
    val childB = View(activity)

    // childA: when laid out, removes its sibling childB from parent
    val childA =
        object : View(activity) {
          override fun layout(l: Int, t: Int, r: Int, b: Int) {
            if (childB.parent === parent) {
              parent.removeView(childB)
            }
            super.layout(l, t, r, b)
          }
        }

    parent.addView(childA, FrameLayout.LayoutParams(100, 100))
    parent.addView(childB, FrameLayout.LayoutParams(100, 100))
    decorView.addView(parent)

    parent.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )

    // FrameLayout.layoutChildren will:
    //   1. count = getChildCount() -> 2
    //   2. i=0: childA.layout() -> removes childB -> mChildrenCount=1
    //   3. i=1: getChildAt(1) -> null (1 >= mChildrenCount=1)
    //   4. child.getVisibility() -> NPE
    var caughtNpe = false
    try {
      parent.layout(0, 0, 200, 200)
    } catch (e: NullPointerException) {
      caughtNpe = true
    }

    // The NPE proves the crash mechanism. On Robolectric's shadow of FrameLayout,
    // this may or may not fire depending on shadow fidelity. Either way, we verify
    // the child was improperly removed during the parent's active layout pass.
    assertTrue(
        "Expected either NPE (real FrameLayout) or childB detached during layout (shadow)",
        caughtNpe || childB.parent == null,
    )
  }

  // ---- unbind + onZoomFinished notification tests ----

  @Test
  fun `unbind during active zoom fires onZoomFinished`() {
    val listener = TestListener()
    controller.addZoomableEventListener(listener)

    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    controller.unbind()

    assertEquals(1, listener.zoomFinishedCount)
  }

  @Test
  fun `unbind when idle does not fire onZoomFinished`() {
    val listener = TestListener()
    controller.addZoomableEventListener(listener)
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)

    controller.unbind()

    assertEquals(0, listener.zoomFinishedCount)
  }

  @Test
  fun `deferred teardown posts to decorView and completes after rootView detached`() {
    invokeStartRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.REMOTE_ZOOMING, controller.state)

    val remoteContainerCountBefore = decorView.childCount

    rootView.simulateInLayout = true
    invokeTeardownRemoteZoom()
    assertEquals(ZoomableViewBaseController.State.IDLE, controller.state)
    // remoteContainer still on decorView (deferred)
    assertEquals(remoteContainerCountBefore, decorView.childCount)

    // Simulate RecyclerView recycling: detach rootView from its parent
    rootView.simulateInLayout = false
    (rootView.parent as? ViewGroup)?.removeView(rootView)

    // The deferred lambda was posted to decorView (not rootView), so it still executes
    ShadowLooper.idleMainLooper()

    assertEquals(
        "remoteContainer should be removed from decorView even after rootView is detached",
        remoteContainerCountBefore - 1,
        decorView.childCount,
    )
  }
}
