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

package com.facebook.litho.testing

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import java.util.concurrent.TimeUnit
import org.robolectric.shadows.ShadowLooper

/**
 * Dispatches well-formed [MotionEvent] streams through a real [View.dispatchTouchEvent] chain.
 *
 * `View.performClick()` invokes the click listener directly, so it only proves a listener is
 * installed. Use this instead to exercise hit testing, touch slop, press state, long-press
 * scheduling, `TouchDelegate`s, and `ACTION_CANCEL` from an intercepting ancestor.
 *
 * Coordinates are in [target]'s coordinate space.
 */
class TouchInjectionScope internal constructor(private val target: View) {

  private class Pointer(val id: Int, var x: Float, var y: Float)

  private val pointers = mutableListOf<Pointer>()
  private var downTime: Long = UNSET
  private var eventTime: Long = UNSET
  private var dispatchedInGesture: Int = 0

  /**
   * Gap placed between successive events, in milliseconds. Raise it for a slow drag, lower it for a
   * flick; anything measuring velocity or duration reads this.
   */
  var eventIntervalMs: Long = DEFAULT_EVENT_INTERVAL_MS

  /** Whether the most recently dispatched event was consumed by the hierarchy. */
  var lastEventHandled: Boolean = false
    private set

  private val touchSlop: Int
    get() = ViewConfiguration.get(target.context).scaledTouchSlop

  /** Presses [pointerId] down at ([x], [y]). The first pointer down starts the gesture. */
  @JvmOverloads
  fun down(x: Float, y: Float, pointerId: Int = 0): TouchInjectionScope {
    check(pointers.none { it.id == pointerId }) { "Pointer $pointerId is already down." }
    val isFirst = pointers.isEmpty()
    if (isFirst) {
      downTime = SystemClock.uptimeMillis()
      eventTime = downTime
      // Cleared here rather than in reset(), which runs after the final dispatch of a gesture and
      // would discard the result callers read back.
      lastEventHandled = false
      dispatchedInGesture = 0
    }
    pointers.add(Pointer(pointerId, x, y))
    val index = pointers.indexOfFirst { it.id == pointerId }
    val action =
        if (isFirst) {
          MotionEvent.ACTION_DOWN
        } else {
          MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }
    return dispatch(action)
  }

  /** Moves [pointerId] to the absolute position ([x], [y]). */
  @JvmOverloads
  fun moveTo(x: Float, y: Float, pointerId: Int = 0): TouchInjectionScope {
    val pointer = requirePointer(pointerId)
    pointer.x = x
    pointer.y = y
    return dispatch(MotionEvent.ACTION_MOVE)
  }

  /** Moves [pointerId] by ([dx], [dy]) from its current position. */
  @JvmOverloads
  fun moveBy(dx: Float, dy: Float, pointerId: Int = 0): TouchInjectionScope {
    val pointer = requirePointer(pointerId)
    return moveTo(pointer.x + dx, pointer.y + dy, pointerId)
  }

  /**
   * Moves [pointerId] just past [ViewConfiguration.getScaledTouchSlop], the threshold a scrolling
   * ancestor uses to claim the gesture. Note a lone clickable view abandons its tap on leaving its
   * bounds, not on travel distance, so this alone still clicks.
   */
  @JvmOverloads
  fun moveBeyondTouchSlop(pointerId: Int = 0): TouchInjectionScope =
      moveBy(dx = (touchSlop + 1).toFloat(), dy = 0f, pointerId = pointerId)

  /** Lifts [pointerId]. Lifting the last pointer ends the gesture. */
  @JvmOverloads
  fun up(pointerId: Int = 0): TouchInjectionScope {
    requirePointer(pointerId)
    val index = pointers.indexOfFirst { it.id == pointerId }
    val isLast = pointers.size == 1
    val action =
        if (isLast) {
          MotionEvent.ACTION_UP
        } else {
          MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }
    dispatch(action)
    pointers.removeAt(index)
    if (pointers.isEmpty()) {
      reset()
    }
    return this
  }

  /** Sends `ACTION_CANCEL`, as an intercepting ancestor causes to be delivered to the child. */
  fun cancel(): TouchInjectionScope {
    check(pointers.isNotEmpty()) { "cancel() requires an in-flight gesture; call down() first." }
    dispatch(MotionEvent.ACTION_CANCEL)
    pointers.clear()
    reset()
    return this
  }

  /**
   * Advances the main looper's clock, running anything scheduled in the interval. Long-press
   * detection is a delayed message, so timing-sensitive gestures advance time rather than sleep.
   *
   * Robolectric only, as is anything that calls it ([longClick]). On an instrumentation test there
   * is no shadow clock to move, so drive those gestures with real waits instead.
   */
  fun advanceTimeBy(millis: Long): TouchInjectionScope {
    advanceClock(millis)
    return this
  }

  /** Moves the event stamp and the looper clock together, so a gesture never reads two clocks. */
  private fun advanceClock(millis: Long) {
    if (eventTime != UNSET) {
      eventTime += millis
    }
    ShadowLooper.idleMainLooper(millis, TimeUnit.MILLISECONDS)
  }

  /** A complete tap: down then up, with no intervening movement. */
  @JvmOverloads
  fun click(x: Float, y: Float, pointerId: Int = 0): TouchInjectionScope =
      down(x, y, pointerId).up(pointerId)

  /** A press held past [ViewConfiguration.getLongPressTimeout] and then released. */
  @JvmOverloads
  fun longClick(x: Float, y: Float, pointerId: Int = 0): TouchInjectionScope =
      down(x, y, pointerId)
          .advanceTimeBy(ViewConfiguration.getLongPressTimeout() + LONG_PRESS_SLACK_MS)
          .up(pointerId)

  /** A drag from ([fromX], [fromY]) to ([toX], [toY]) broken into [steps] `ACTION_MOVE`s. */
  @JvmOverloads
  fun swipe(
      fromX: Float,
      fromY: Float,
      toX: Float,
      toY: Float,
      steps: Int = DEFAULT_SWIPE_STEPS,
      durationMs: Long = DEFAULT_SWIPE_DURATION_MS,
      pointerId: Int = 0,
  ): TouchInjectionScope {
    require(steps > 0) { "A swipe needs at least one step." }
    val previousInterval = eventIntervalMs
    // DOWN carries no gap, so the gaps are the ones before each MOVE and before the UP.
    val gaps = steps + 1
    val gap = (durationMs / gaps).coerceAtLeast(1L)
    // Integer division loses up to gaps-1 ms; the last gap absorbs it so DOWN to UP is exactly
    // durationMs.
    val lastGap = gap + (durationMs - gap * gaps).coerceAtLeast(0L)
    try {
      eventIntervalMs = gap
      down(fromX, fromY, pointerId)
      for (step in 1..steps) {
        val fraction = step.toFloat() / steps
        moveTo(fromX + (toX - fromX) * fraction, fromY + (toY - fromY) * fraction, pointerId)
      }
      eventIntervalMs = lastGap
      return up(pointerId)
    } finally {
      eventIntervalMs = previousInterval
    }
  }

  private fun requirePointer(pointerId: Int): Pointer =
      checkNotNull(pointers.firstOrNull { it.id == pointerId }) {
        "Pointer $pointerId is not down. Call down() before moving or lifting it."
      }

  private fun dispatch(action: Int): TouchInjectionScope {
    // Real streams carry increasing timestamps, and the clock moves with them: anything deriving
    // velocity or duration (VelocityTracker, fling detection, tap-vs-hold) reads both the event
    // stamps and the current time, and gets nonsense if they disagree. So advance the looper too,
    // which also lets work the gesture schedules run at the point it would on device. The opening
    // DOWN is excluded because its eventTime equals its downTime there, and the tap and long-press
    // windows are measured from that difference.
    if (dispatchedInGesture > 0) {
      advanceClock(eventIntervalMs)
    }
    dispatchedInGesture++
    val event = obtain(action)
    try {
      lastEventHandled = target.dispatchTouchEvent(event)
    } finally {
      event.recycle()
    }
    return this
  }

  private fun obtain(action: Int): MotionEvent {
    val properties =
        Array(pointers.size) { i ->
          MotionEvent.PointerProperties().apply {
            id = pointers[i].id
            toolType = MotionEvent.TOOL_TYPE_FINGER
          }
        }
    val coords =
        Array(pointers.size) { i ->
          MotionEvent.PointerCoords().apply {
            x = pointers[i].x
            y = pointers[i].y
            pressure = 1f
            size = 1f
          }
        }
    return MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        pointers.size,
        properties,
        coords,
        /* metaState= */ 0,
        /* buttonState= */ 0,
        /* xPrecision= */ 1f,
        /* yPrecision= */ 1f,
        /* deviceId= */ 0,
        /* edgeFlags= */ 0,
        // Some View paths branch on isFromSource(); SOURCE_UNKNOWN would not take the same route a
        // finger does.
        /* source= */ InputDevice.SOURCE_TOUCHSCREEN,
        /* flags= */ 0,
    )
  }

  private fun reset() {
    downTime = UNSET
    eventTime = UNSET
  }

  companion object {
    private const val UNSET = -1L
    private const val DEFAULT_SWIPE_STEPS = 10
    // Roughly a deliberate drag rather than a flick.
    private const val DEFAULT_SWIPE_DURATION_MS = 100L
    // Push past the timeout rather than landing exactly on it, so the scheduled callback is due.
    private const val LONG_PRESS_SLACK_MS = 50L
    // Gap between successive events in a gesture, unless a test overrides it.
    private const val DEFAULT_EVENT_INTERVAL_MS = 10L
  }
}

/**
 * Runs [block] against a [TouchInjectionScope] targeting this view. Coordinates inside [block] are
 * in this view's coordinate space.
 */
fun View.performTouchInput(block: TouchInjectionScope.() -> Unit): Boolean {
  val scope = beginTouchInput()
  scope.block()
  return scope.lastEventHandled
}

/**
 * Opens a gesture that outlives a single statement, for tests that mutate the tree mid-gesture (a
 * re-render or unmount between DOWN and UP). Prefer [performTouchInput] when the whole gesture fits
 * in one block.
 */
fun View.beginTouchInput(): TouchInjectionScope = TouchInjectionScope(this)

/**
 * Runs [block] against a [TouchInjectionScope] targeting the rendered `LithoView`, so a gesture
 * enters at the root and is hit-tested down the real hierarchy. Coordinates are in the
 * `LithoView`'s space.
 *
 * Unlike `LithoTestRule.act`, this does NOT idle the loopers afterwards: it dispatches straight
 * into `View.dispatchTouchEvent` and returns. That is what a characterization test wants, since
 * idling would hide ordering. If the gesture's handler schedules work — a state update, a re-render
 * — call `LithoTestRule.idle()` before asserting, or the assertion races it.
 */
fun TestLithoView.performTouchInput(block: TouchInjectionScope.() -> Unit): Boolean =
    lithoView.performTouchInput(block)

/** [beginTouchInput] targeting the rendered `LithoView`. */
fun TestLithoView.beginTouchInput(): TouchInjectionScope = lithoView.beginTouchInput()

/** The centre of this view, in the coordinate space of [ancestor]. */
fun View.centerRelativeTo(ancestor: ViewGroup): Pair<Float, Float> {
  val bounds = Rect()
  getDrawingRect(bounds)
  ancestor.offsetDescendantRectToMyCoords(this, bounds)
  return bounds.exactCenterX() to bounds.exactCenterY()
}
