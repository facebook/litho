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

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The timing a gesture reports, which anything deriving velocity or duration reads.
 *
 * Asserted on a plain `View` rather than a Litho tree: this is the harness's own arithmetic, and a
 * component in the middle would only add ways for it to be wrong.
 */
@RunWith(LithoTestRunner::class)
class GestureTimingTest {

  private class Recorder(context: Context) : View(context) {
    val events: MutableList<MotionEvent> = mutableListOf()

    override fun onTouchEvent(event: MotionEvent): Boolean {
      events.add(MotionEvent.obtain(event))
      return true
    }
  }

  private fun recorder(): Recorder =
      Recorder(ApplicationProvider.getApplicationContext()).apply {
        layout(0, 0, SIZE, SIZE)
        isClickable = true
      }

  private fun Recorder.spanOfLastGesture(): Long {
    val last = events.last()
    return last.eventTime - last.downTime
  }

  @Test
  fun `a swipe takes the duration it was asked for`() {
    val view = recorder()

    view.performTouchInput { swipe(0f, 0f, 90f, 0f, steps = 10, durationMs = 100L) }

    assertThat(view.spanOfLastGesture())
        .describedAs("DOWN to UP, which is what a VelocityTracker measures over")
        .isEqualTo(100L)
  }

  /** The case the arithmetic is easiest to get wrong: few steps, so each gap is large. */
  @Test
  fun `a swipe with few steps still takes the duration it was asked for`() {
    val view = recorder()

    view.performTouchInput { swipe(0f, 0f, 90f, 0f, steps = 3, durationMs = 100L) }

    assertThat(view.spanOfLastGesture()).isEqualTo(100L)
  }

  /** Every gap is equal apart from the last, which absorbs the integer-division remainder. */
  @Test
  fun `a swipe advances the clock monotonically in even steps`() {
    val view = recorder()

    view.performTouchInput { swipe(0f, 0f, 90f, 0f, steps = 4, durationMs = 100L) }

    val stamps = view.events.map { it.eventTime - it.downTime }
    assertThat(stamps).describedAs("DOWN, 4 MOVEs, UP").hasSize(6)
    assertThat(stamps.first()).describedAs("DOWN carries no gap").isEqualTo(0L)
    assertThat(stamps).isSorted
    assertThat(stamps.zipWithNext { a, b -> b - a }.distinct())
        .describedAs("100ms over 5 gaps divides evenly")
        .containsExactly(20L)
  }

  /**
   * A duration shorter than the number of gaps cannot be honoured — the clock has 1ms granularity —
   * so the gesture is as short as it can be rather than collapsing to zero.
   */
  @Test
  fun `a swipe shorter than its step count still advances every step`() {
    val view = recorder()

    view.performTouchInput { swipe(0f, 0f, 90f, 0f, steps = 10, durationMs = 2L) }

    val stamps = view.events.map { it.eventTime - it.downTime }
    assertThat(stamps.zipWithNext().all { (a, b) -> b > a })
        .describedAs("no two events share a timestamp")
        .isTrue()
  }

  @Test
  fun `a click reports no elapsed time`() {
    val view = recorder()
    val before = SystemClock.uptimeMillis()

    view.performTouchInput { click(1f, 1f) }

    assertThat(view.events.first().downTime).isGreaterThanOrEqualTo(before)
    assertThat(view.events.first().eventTime)
        .describedAs("a real DOWN stamps eventTime == downTime")
        .isEqualTo(view.events.first().downTime)
  }

  companion object {
    private const val SIZE = 100
  }
}
