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

package com.facebook.litho.zoomable

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.view.isInvisible
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.zoomable.OverlayZoomController
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer

@OptIn(ExperimentalLithoApi::class)
@RunWith(LithoTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OverlayZoomControllerTest {

  private lateinit var activity: Activity
  private lateinit var first: View
  private lateinit var second: View
  private var visibilityChanges = 0

  private fun setUp(maxScale: Float = 4f): OverlayZoomController {
    // The spring that animates the copy back runs on frame callbacks.
    ShadowChoreographer.setPostFrameCallbackDelay(16)
    activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val root = FrameLayout(activity)
    first =
        object : View(activity) {
              override fun onVisibilityChanged(changedView: View, visibility: Int) {
                super.onVisibilityChanged(changedView, visibility)
                visibilityChanges++
              }
            }
            .apply {
              // A gradient, so a copy moved sideways draws different pixels.
              background =
                  GradientDrawable(
                      GradientDrawable.Orientation.LEFT_RIGHT,
                      intArrayOf(Color.RED, Color.BLUE),
                  )
            }
    second = View(activity)
    root.addView(first, FrameLayout.LayoutParams(300, 300))
    root.addView(second, FrameLayout.LayoutParams(300, 300))
    activity.setContentView(root)
    val decor = activity.window.decorView
    decor.measure(
        View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
    )
    decor.layout(0, 0, 400, 800)
    shadowOf(Looper.getMainLooper()).idle()
    visibilityChanges = 0
    return OverlayZoomController(activity, maxScale = maxScale).also { it.bindTo(first) }
  }

  @Test
  fun hidingTheHostDoesNotTellItsViewsTheirVisibilityChanged() {
    val controller = setUp()
    pinch(controller)
    assertThat(first.isHiddenForZoom).isTrue()
    controller.bindTo(second)
    assertThat(first.isHiddenForZoom).isFalse()
    assertThat(first.visibility).isEqualTo(View.VISIBLE)
    assertThat(visibilityChanges)
        .describedAs("a TextureView told it's invisible stops showing new frames")
        .isEqualTo(0)
  }

  @Test
  fun aNewHostMidZoomShowsTheHiddenOneAgain() {
    val controller = setUp()
    pinch(controller)
    assertThat(first.isHiddenForZoom).describedAs("hidden while zooming").isTrue()

    controller.bindTo(second)

    assertThat(first.isHiddenForZoom).describedAs("the zoomed host is shown again").isFalse()
    assertThat(second.isHiddenForZoom).isFalse()
    assertThat(controller.zoomableTouchListener.isCurrentlyInZoom).isFalse()
  }

  @Test
  fun aClaimedGestureThatNeverEndedDoesNotTakeTheNextOne() {
    val controller = setUp()
    val t = SystemClock.uptimeMillis()
    assertThat(controller.onInterceptTouchEvent(event(t, MotionEvent.ACTION_DOWN, 50f to 150f)))
        .isFalse()
    assertThat(controller.onInterceptTouchEvent(pointerDown(t + 10)))
        .describedAs("claimed")
        .isTrue()
    // The gesture's up never reaches the controller. A new one starts with a single finger.
    val down = event(t + 1000, MotionEvent.ACTION_DOWN, 50f to 150f)
    assertThat(controller.onInterceptTouchEvent(down)).isFalse()
    controller.zoomableTouchListener.onTouchEvent(down)
    val move = event(t + 1010, MotionEvent.ACTION_MOVE, 60f to 150f)
    assertThat(controller.zoomableTouchListener.onTouchEvent(move))
        .describedAs("a single finger's move is not the zoom's")
        .isFalse()
  }

  @Test
  fun aHostThatDoesNotKeepUnhandledTouchesLetsATapGoOnAndStillZoomsAForwardedPinch() {
    val controller = setUp()
    controller.keepsUnhandledTouches = false
    val t = SystemClock.uptimeMillis()
    assertThat(
        controller.zoomableTouchListener.onTouchEvent(
            event(t, MotionEvent.ACTION_DOWN, 50f to 150f),
        ),
    )
        .describedAs("the tap goes on to the host's ancestors")
        .isFalse()

    // An ancestor that took the gesture passes the pinch on.
    controller.zoomableTouchListener.onTouchEvent(pointerDown(t + 10))
    for (step in 1..10) {
      val half = (200f + 200f * step / 10) / 2
      controller.zoomableTouchListener.onTouchEvent(
          event(
              t + 10 + step * 16L,
              MotionEvent.ACTION_MOVE,
              150f - half to 150f,
              150f + half to 150f,
          ),
      )
    }

    assertThat(controller.zoomableTouchListener.isCurrentlyInZoom).isTrue()
    assertThat(first.isHiddenForZoom).isTrue()
  }

  @Test
  fun theWindowsRootViewIsNotZoomedIntoItsOwnOverlay() {
    val controller = setUp()
    val decor = activity.window.decorView
    controller.bindTo(decor)
    pinch(controller)
    assertThat(controller.zoomableTouchListener.isCurrentlyInZoom).isFalse()
    decor.draw(Canvas(createBitmap(decor.width, decor.height)))
  }

  @Test
  fun aHostThatCannotZoomDoesNotTakeTheGesture() {
    val controller = setUp()
    controller.bindTo(activity.window.decorView)
    val t = SystemClock.uptimeMillis()
    controller.onInterceptTouchEvent(event(t, MotionEvent.ACTION_DOWN, 50f to 150f))
    assertThat(controller.onInterceptTouchEvent(pointerDown(t + 10)))
        .describedAs("not claimed: the root view can't be zoomed into its own overlay")
        .isFalse()
    val move = event(t + 20, MotionEvent.ACTION_MOVE, 40f to 150f, 260f to 150f)
    assertThat(controller.zoomableTouchListener.onTouchEvent(move))
        .describedAs("the content keeps the gesture")
        .isFalse()
  }

  @Test
  fun aHostDetachedMidZoomEndsTheZoomAndIsShownAgain() {
    val controller = setUp()
    pinch(controller)
    assertThat(first.isHiddenForZoom).isTrue()

    val looper = shadowOf(Looper.getMainLooper())
    looper.pause()
    (first.parent as android.view.ViewGroup).removeView(first)

    assertThat(first.isHiddenForZoom).isFalse()
    assertThat(controller.zoomableTouchListener.isCurrentlyInZoom).isFalse()
    assertThat(first.hasTransientState())
        .describedAs("not released while the detach is going down the tree")
        .isTrue()
    looper.unPause()
    looper.idle()
    assertThat(first.hasTransientState()).isFalse()
  }

  @Test
  fun aCancelWithThreeFingersDownEndsTheZoom() {
    val controller = setUp()
    val t = pinch(controller)
    controller.touch(
        t + 1,
        MotionEvent.ACTION_POINTER_DOWN,
        2,
        at(0, -50f),
        at(1, 350f),
        at(2, 200f),
    )
    controller.touch(t + 2, MotionEvent.ACTION_CANCEL, 0, at(0, -50f), at(1, 350f), at(2, 200f))
    shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
    assertThat(controller.zoomableTouchListener.isCurrentlyInZoom).isFalse()
    assertThat(first.isHiddenForZoom).isFalse()
  }

  @Test
  fun fingersLandingAndLiftingWithoutMovingDoNotMoveTheCopy() {
    val controller = setUp()
    val t = pinch(controller)
    val before = snapshot()
    controller.touch(
        t + 1,
        MotionEvent.ACTION_POINTER_DOWN,
        2,
        at(0, -50f),
        at(1, 350f),
        at(2, 200f, 300f),
    )
    controller.touch(t + 2, MotionEvent.ACTION_MOVE, 0, at(0, -50f), at(1, 350f), at(2, 200f, 300f))
    controller.touch(
        t + 3,
        MotionEvent.ACTION_POINTER_UP,
        0,
        at(0, -50f),
        at(1, 350f),
        at(2, 200f, 300f),
    )
    controller.touch(t + 4, MotionEvent.ACTION_MOVE, 0, at(1, 350f), at(2, 200f, 300f))
    controller.touch(t + 5, MotionEvent.ACTION_POINTER_UP, 0, at(1, 350f), at(2, 200f, 300f))
    controller.touch(t + 6, MotionEvent.ACTION_MOVE, 0, at(2, 200f, 300f))
    assertThat(snapshot().sameAs(before)).isTrue()
  }

  @Test
  fun aPanFollowsTheFingersWithoutJumpingWhenTheyChange() {
    val controller = setUp()
    val t = pinch(controller)
    controller.touch(t + 1, MotionEvent.ACTION_POINTER_UP, 1, at(0, -50f), at(1, 350f))
    controller.touch(t + 2, MotionEvent.ACTION_MOVE, 0, at(0, -30f))
    val panned = snapshot()
    controller.touch(t + 3, MotionEvent.ACTION_POINTER_DOWN, 1, at(0, -30f), at(3, 300f, 400f))
    controller.touch(t + 4, MotionEvent.ACTION_POINTER_UP, 0, at(0, -30f), at(3, 300f, 400f))
    controller.touch(t + 5, MotionEvent.ACTION_MOVE, 0, at(3, 300f, 400f))
    assertThat(snapshot().sameAs(panned))
        .describedAs("no jump when the panning finger changes")
        .isTrue()
    controller.touch(t + 6, MotionEvent.ACTION_MOVE, 0, at(3, 280f, 400f))
    assertThat(snapshot().sameAs(panned)).describedAs("the new finger pans").isFalse()
  }

  @Test
  fun pastTheMaximumTheCopyHoldsWhereTheFingersHold() {
    val controller = setUp(maxScale = 1.5f)
    val t = pinch(controller)
    // The 300px host, scaled around its center, reaches 150 + 150 * scale below its top: past 380
    // only above 1.53, which the pinch pulls it to, past the maximum.
    val below = hostTop() + 380
    assertThat(isCopy(150, below)).describedAs("past the maximum after the pinch").isTrue()
    for (step in 1..5) {
      controller.touch(t + step, MotionEvent.ACTION_MOVE, 0, at(0, -50f), at(1, 350f))
    }
    assertThat(isCopy(150, below)).describedAs("still there while the fingers hold").isTrue()
  }

  @Test
  fun eventsInAnotherViewsCoordinatesStillZoomAroundTheFingers() {
    val inHostCoordinates = setUp().also { pinch(it) }.let { snapshot() }
    // The same fingers on the screen, sent in the coordinates of a view starting 100px lower, as
    // a listener shared with another view may forward them.
    eventOriginBelowHost = 100f
    val controller = setUp()
    pinch(controller, offsetY = -100f)
    assertThat(snapshot().sameAs(inHostCoordinates)).isTrue()
  }

  @Test
  fun fingersHoldingStillDoNotRedrawTheCopy() {
    val controller = setUp()
    val t = pinch(controller)
    val decor = activity.window.decorView
    snapshot()
    for (step in 1..3) {
      controller.touch(t + step, MotionEvent.ACTION_MOVE, 0, at(0, -50f), at(1, 350f))
    }
    assertThat(decor.isDirty).describedAs("no redraw while the fingers hold").isFalse()
    controller.touch(t + 4, MotionEvent.ACTION_MOVE, 0, at(0, -40f), at(1, 360f))
    assertThat(decor.isDirty).describedAs("a redraw when they move").isTrue()
  }

  @Test
  fun unbindingMidZoomReleasesTheTransientStateAfterTheUnbind() {
    val controller = setUp()
    pinch(controller)
    val looper = shadowOf(Looper.getMainLooper())
    looper.pause()
    controller.unbind()
    assertThat(first.isHiddenForZoom).isFalse()
    assertThat(first.hasTransientState()).describedAs("not released during the unbind").isTrue()
    looper.unPause()
    looper.idle()
    assertThat(first.hasTransientState()).isFalse()
  }

  @Test
  fun aNewHostDoesNotInheritTheLastOnesClaim() {
    val controller = setUp()
    val t = SystemClock.uptimeMillis()
    controller.onInterceptTouchEvent(event(t, MotionEvent.ACTION_DOWN, 50f to 150f))
    assertThat(controller.onInterceptTouchEvent(pointerDown(t + 10))).isTrue()
    controller.bindTo(second)
    val move = event(t + 20, MotionEvent.ACTION_MOVE, 60f to 150f)
    assertThat(controller.zoomableTouchListener.onTouchEvent(move))
        .describedAs("a single finger's move on the new host is not the zoom's")
        .isFalse()
  }

  /** Pinches out around (150, 150) until the fingers are [spreadTo] apart; returns the time. */
  private fun pinch(
      controller: OverlayZoomController,
      spreadTo: Float = 400f,
      offsetY: Float = 0f,
  ): Long {
    val t = SystemClock.uptimeMillis()
    val y = 150f + offsetY
    controller.onInterceptTouchEvent(event(t, MotionEvent.ACTION_DOWN, 50f to y))
    controller.onInterceptTouchEvent(
        event(
            t + 10,
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            50f to y,
            250f to y,
        ),
    )
    for (step in 1..10) {
      val half = (200f + (spreadTo - 200f) * step / 10) / 2
      controller.zoomableTouchListener.onTouchEvent(
          event(
              t + 10 + step * 16L,
              MotionEvent.ACTION_MOVE,
              150f - half to y,
              150f + half to y,
          ),
      )
    }
    return t + 200
  }

  private fun at(id: Int, x: Float, y: Float = 150f): Triple<Int, Float, Float> = Triple(id, x, y)

  /**
   * Sends a touch event with the given pointers, by id; [index] is the pointer the action is for.
   */
  private fun OverlayZoomController.touch(
      time: Long,
      action: Int,
      index: Int,
      vararg pointers: Triple<Int, Float, Float>,
  ) {
    val masked =
        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
          action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        } else {
          action
        }
    val event =
        obtain(time, masked, pointers.map { it.first }, pointers.map { it.second to it.third })
    zoomableTouchListener.onTouchEvent(event)
    event.recycle()
  }

  private fun hostTop(): Int = IntArray(2).also { first.getLocationInWindow(it) }[1]

  /** Whether the pixel shows the copy's gradient rather than the dimmed background. */
  private fun isCopy(x: Int, y: Int): Boolean {
    val pixel = snapshot()[x, y]
    return Color.red(pixel) + Color.blue(pixel) > 200
  }

  private fun snapshot(): Bitmap {
    val decor = activity.window.decorView
    val bitmap = createBitmap(decor.width, decor.height)
    decor.draw(Canvas(bitmap))
    return bitmap
  }

  private fun pointerDown(time: Long): MotionEvent = event(
      time,
      MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
      50f to 150f,
      250f to 150f,
  )

  private fun event(time: Long, action: Int, vararg points: Pair<Float, Float>): MotionEvent =
      obtain(time, action, points.indices.toList(), points.toList())

  // Where the events' coordinates start, below the host's top: 0 for the host's own coordinates.
  private var eventOriginBelowHost = 0f

  private fun obtain(
      time: Long,
      action: Int,
      ids: List<Int>,
      points: List<Pair<Float, Float>>,
  ): MotionEvent {
    val properties =
        Array(points.size) { i ->
          MotionEvent.PointerProperties().apply {
            id = ids[i]
            toolType = MotionEvent.TOOL_TYPE_FINGER
          }
        }
    val coords =
        Array(points.size) { i ->
          MotionEvent.PointerCoords().apply {
            x = points[i].first
            y = points[i].second
            pressure = 1f
            size = 1f
          }
        }
    // Obtained on the screen, then moved to the coordinates of the view the events are for.
    val toScreen = hostTop() + eventOriginBelowHost
    coords.forEach { it.y += toScreen }
    return MotionEvent.obtain(
            time,
            time,
            action,
            points.size,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )
        .apply { offsetLocation(0f, -toScreen) }
  }
}

/** Whether [OverlayZoomController] hid this host from its parent for a zoom. */
internal val View.isHiddenForZoom: Boolean
  get() =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isInvisible
      } else {
        clipBounds?.isEmpty == true
      }
