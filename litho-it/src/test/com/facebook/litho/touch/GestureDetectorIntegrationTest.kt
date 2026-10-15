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

package com.facebook.litho.touch

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoGestureDetector
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Litho has no gesture API beyond click/long-click, so anything richer is `Style.onTouch` driving a
 * `GestureDetector`. That composition has a trap worth pinning: `SimpleOnGestureListener.onDown`
 * returns false by default, so the detector declines the DOWN, the host never becomes the touch
 * target, and no further events arrive. The detector then silently detects nothing.
 */
@RunWith(LithoTestRunner::class)
class GestureDetectorIntegrationTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  /** Records which detector callbacks fire, in order. */
  private class RecordingListener(private val consumeDown: Boolean) :
      GestureDetector.SimpleOnGestureListener() {
    val events = mutableListOf<String>()

    override fun onDown(e: MotionEvent): Boolean {
      events.add("onDown")
      return consumeDown
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
      events.add("onSingleTapUp")
      return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
      events.add("onDoubleTap")
      return true
    }

    override fun onLongPress(e: MotionEvent) {
      events.add("onLongPress")
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
      // Collapse repeats: the count is a function of how many MOVEs the test sends.
      if (events.lastOrNull() != "onScroll") {
        events.add("onScroll")
      }
      return true
    }
  }

  private class DetectorComponent(
      private val detector: GestureDetector,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      child(
          Column(
              style =
                  Style.width(SIZE.px).height(SIZE.px).onTouch { event ->
                    detector.onTouchEvent(event.motionEvent)
                  },
          ),
      )
    }
  }

  private fun detectorFor(consumeDown: Boolean): Pair<GestureDetector, RecordingListener> {
    val listener = RecordingListener(consumeDown)
    val detector = LithoGestureDetector(
        lithoTestRule.context.androidContext,
        listener,
    )
    return detector to listener
  }

  /** The default-listener trap: declining DOWN costs the detector the rest of the gesture. */
  @Test
  fun `detector that declines the down sees nothing after it`() {
    val (detector, listener) = detectorFor(consumeDown = false)
    val testLithoView = lithoTestRule.render { DetectorComponent(detector) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 30f, dy = 0f)
      up()
    }

    // Asserting the absence of the post-DOWN callbacks rather than an exact list: the detector
    // posts its long-press timer on DOWN and, because the host never became the touch target, never
    // sees the UP that would cancel it. An exact match would depend on that timer not firing.
    assertThat(listener.events)
        .describedAs("A detector whose onDown returns false is starved of the gesture")
        .contains("onDown")
    assertThat(listener.events).doesNotContain("onScroll", "onSingleTapUp", "onDoubleTap")
  }

  @Test
  fun `detector that consumes the down detects a tap`() {
    val (detector, listener) = detectorFor(consumeDown = true)
    val testLithoView = lithoTestRule.render { DetectorComponent(detector) }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(listener.events).containsExactly("onDown", "onSingleTapUp")
  }

  @Test
  fun `detector that consumes the down detects a scroll`() {
    val (detector, listener) = detectorFor(consumeDown = true)
    val testLithoView = lithoTestRule.render { DetectorComponent(detector) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 40f, dy = 0f)
      moveBy(dx = 40f, dy = 0f)
      up()
    }

    assertThat(listener.events).contains("onScroll")
  }

  @Test
  fun `detector that consumes the down detects a long press`() {
    val (detector, listener) = detectorFor(consumeDown = true)
    val testLithoView = lithoTestRule.render { DetectorComponent(detector) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 100L)
      up()
    }

    assertThat(listener.events).contains("onLongPress")
  }

  /** Double tap is only reachable through a detector; Litho exposes no API for it. */
  @Test
  fun `detector that consumes the down detects a double tap`() {
    val (detector, listener) = detectorFor(consumeDown = true)
    val testLithoView = lithoTestRule.render { DetectorComponent(detector) }

    testLithoView.performTouchInput {
      click(CENTER, CENTER)
      advanceTimeBy(TAP_GAP_MS)
      click(CENTER, CENTER)
    }

    assertThat(listener.events).contains("onDoubleTap")
  }

  companion object {
    private const val SIZE = 100
    private const val CENTER = SIZE / 2f
    // Comfortably inside ViewConfiguration.getDoubleTapTimeout() (300ms).
    private const val TAP_GAP_MS = 50L
  }
}
