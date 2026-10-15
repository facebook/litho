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
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.ComponentsConfiguration.LogicViolationPolicy
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.touchExpansion
import com.facebook.litho.widget.OnClickCallbackComponent
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventBus
import com.facebook.rendercore.debug.DebugEventSubscriber
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests for the automatic minimum touch-target expansion gated by
 * [ComponentsConfiguration.useAutoTouchExpansion]. When enabled, interactive components whose
 * laid-out size is smaller than [ComponentsConfiguration.minTouchTargetSizeDp] have their *touch
 * target* (not their visual bounds) grown to that minimum, mirroring Jetpack Compose's
 * `minimumInteractiveComponentSize`.
 *
 * All expansion math is derived at runtime from the test device's density (via
 * [ComponentContext.resourceResolver]) so the assertions hold regardless of the configured density.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class AutoTouchExpansionTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  private lateinit var context: ComponentContext

  /**
   * The interactive component's laid-out size, in px. Deliberately far below any reasonable minimum
   * touch target so the feature always has work to do regardless of the test's density.
   */
  private val smallPx = 10

  /**
   * Touch-target opportunity reports delivered to [subscriber] during a render; reset before each
   * test. The report is a [LithoDebugEvent.DebugInfo] event whose `name` attribute is the category
   * [TOUCH_TARGET_CATEGORY].
   */
  private val debugEvents = mutableListOf<DebugEvent>()

  private val subscriber =
      object : DebugEventSubscriber(LithoDebugEvent.DebugInfo) {
        override fun onEvent(event: DebugEvent) {
          if (event.attributes[DebugEventAttribute.Name] == TOUCH_TARGET_CATEGORY) {
            debugEvents.add(event)
          }
        }
      }

  @Before
  fun setup() {
    context = lithoTestRule.context
    debugEvents.clear()
    DebugEventBus.subscribe(subscriber)
  }

  @After
  fun tearDown() {
    // These are process-global, so reset them and detach the subscriber to keep tests isolated.
    ComponentsConfiguration.useAutoTouchExpansion = false
    ComponentsConfiguration.minTouchTargetSizeDp = 48
    ComponentsConfiguration.minTouchTargetViolationPolicy = LogicViolationPolicy.NONE
    DebugEventBus.unsubscribeAll()
  }

  private fun minTouchTargetPx(): Int =
      context.resourceResolver.dipsToPixels(ComponentsConfiguration.minTouchTargetSizeDp.toFloat())

  @Test
  fun whenEnabledAndInteractiveSmallerThanMin_growsTouchTarget() {
    ComponentsConfiguration.useAutoTouchExpansion = true
    val minPx = minTouchTargetPx()
    // Padding == minPx guarantees the (centered) expanded region stays at positive coordinates.
    val pad = minPx
    val perEdgeExpansion = (minPx - smallPx) / 2
    val viewSize = smallPx + 2 * pad
    val callback = ClickListenerCallback()

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(smallPx.px).height(smallPx.px),
                ),
            )
          }
        }

    assertThat(testLithoView.lithoView.touchExpansionDelegate?.size())
        .describedAs("an interactive component below the min size should register a touch delegate")
        .isEqualTo(1)

    // Just outside the visual bounds (which start at `pad`) but inside the expanded touch target.
    testLithoView.lithoView.emulateClickEvent(pad - 1, pad - 1)
    assertThat(callback.handled)
        .describedAs("a tap inside the auto-expanded touch target should be handled")
        .isTrue

    callback.reset()
    // Outside the expanded touch target entirely (the expanded edge is at `pad -
    // perEdgeExpansion`).
    val outsideExpanded = pad - perEdgeExpansion - 2
    testLithoView.lithoView.emulateClickEvent(outsideExpanded, outsideExpanded)
    assertThat(callback.handled)
        .describedAs("a tap outside the auto-expanded touch target should not be handled")
        .isFalse
  }

  @Test
  fun whenDisabled_doesNotExpand() {
    // useAutoTouchExpansion defaults to false.
    val minPx = minTouchTargetPx()
    val pad = minPx
    val viewSize = smallPx + 2 * pad
    val callback = ClickListenerCallback()

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(smallPx.px).height(smallPx.px),
                ),
            )
          }
        }

    assertThat(testLithoView.lithoView.touchExpansionDelegate)
        .describedAs("no touch delegate should be created when the feature is disabled")
        .isNull()

    testLithoView.lithoView.emulateClickEvent(pad - 1, pad - 1)
    assertThat(callback.handled)
        .describedAs("a tap outside the visual bounds should not be handled when disabled")
        .isFalse

    // Sanity: tapping inside the visual bounds is still handled.
    testLithoView.lithoView.emulateClickEvent(pad + smallPx / 2, pad + smallPx / 2)
    assertThat(callback.handled)
        .describedAs("a tap inside the visual bounds should still be handled")
        .isTrue
  }

  @Test
  fun whenNotInteractive_doesNotExpand() {
    ComponentsConfiguration.useAutoTouchExpansion = true
    val minPx = minTouchTargetPx()
    val pad = minPx
    val viewSize = smallPx + 2 * pad

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            // No click/touch handler => not interactive.
            child(
                Column(
                    style = Style.width(smallPx.px).height(smallPx.px).backgroundColor(Color.RED),
                ) {},
            )
          }
        }

    assertThat(testLithoView.lithoView.touchExpansionDelegate)
        .describedAs("a non-interactive component should never get a touch delegate")
        .isNull()
  }

  @Test
  fun whenInteractiveLargerThanMin_doesNotExpand() {
    ComponentsConfiguration.useAutoTouchExpansion = true
    val minPx = minTouchTargetPx()
    val largePx = minPx + 20
    val pad = 10
    val viewSize = largePx + 2 * pad
    val callback = ClickListenerCallback()

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(largePx.px).height(largePx.px),
                ),
            )
          }
        }

    assertThat(testLithoView.lithoView.touchExpansionDelegate)
        .describedAs("a component already at/above the min size should not get a touch delegate")
        .isNull()

    // Sanity: it is still clickable within its visual bounds.
    testLithoView.lithoView.emulateClickEvent(pad + largePx / 2, pad + largePx / 2)
    assertThat(callback.handled)
        .describedAs("a tap inside a large interactive component should be handled")
        .isTrue
  }

  @Test
  fun augmentsManualTouchExpansion() {
    ComponentsConfiguration.useAutoTouchExpansion = true
    val minPx = minTouchTargetPx()
    val manual = 2
    val pad = minPx
    val viewSize = smallPx + 2 * pad
    val callback = ClickListenerCallback()

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style =
                        Style.width(smallPx.px).height(smallPx.px).touchExpansion(all = manual.px),
                ),
            )
          }
        }

    // The deficit beyond the manual expansion is distributed symmetrically per edge.
    val finalPerEdge = manual + (minPx - (smallPx + 2 * manual)) / 2
    val expandedEdge = pad - finalPerEdge
    // This tap is beyond the manual-only region (`pad - manual`) but inside the auto-augmented
    // target. With "skip when manual is set" semantics it would NOT be handled.
    testLithoView.lithoView.emulateClickEvent(expandedEdge + 1, expandedEdge + 1)
    assertThat(callback.handled)
        .describedAs("auto-expansion should augment manual expansion to reach the min target")
        .isTrue
  }

  @Test
  fun respectsConfiguredMinTouchTargetSize() {
    ComponentsConfiguration.useAutoTouchExpansion = true
    // Larger than the 48dp default so we can distinguish the two targets.
    ComponentsConfiguration.minTouchTargetSizeDp = 96
    val minPx = minTouchTargetPx()
    val pad = minPx
    val viewSize = smallPx + 2 * pad
    val callback = ClickListenerCallback()

    val testLithoView =
        lithoTestRule.render(widthPx = viewSize, heightPx = viewSize) {
          Column(style = Style.padding(all = pad.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(smallPx.px).height(smallPx.px),
                ),
            )
          }
        }

    // The edge of a default 48dp target; a tap just beyond it must still be handled because the
    // configured target is larger (96dp).
    val defaultExpansion = (context.resourceResolver.dipsToPixels(48f) - smallPx) / 2
    val beyondDefaultTarget = pad - defaultExpansion - 5
    testLithoView.lithoView.emulateClickEvent(beyondDefaultTarget, beyondDefaultTarget)
    assertThat(callback.handled)
        .describedAs("the configured (larger) min touch target size should be honored")
        .isTrue
  }

  // --- Opportunity reporting: minTouchTargetViolationPolicy ---

  private fun renderSmallInteractive(callback: View.OnClickListener) =
      lithoTestRule.render(widthPx = 100, heightPx = 100) {
        Column {
          child(
              OnClickCallbackComponent(
                  callback = callback,
                  style = Style.width(smallPx.px).height(smallPx.px),
              ),
          )
        }
      }

  @Test
  fun logPolicy_emitsReportForSmallInteractiveComponent() {
    ComponentsConfiguration.minTouchTargetViolationPolicy = LogicViolationPolicy.LOG

    renderSmallInteractive(ClickListenerCallback())

    assertThat(debugEvents)
        .describedAs("one report for the single sub-minimum interactive component")
        .hasSize(1)
    val event = debugEvents.single()
    assertThat(event.attributes[DebugEventAttribute.Width]).isEqualTo(smallPx)
    assertThat(event.attributes[DebugEventAttribute.Height]).isEqualTo(smallPx)
    assertThat(event.attributes["minTouchTargetSizeDp"])
        .describedAs("the configured minimum is recorded")
        .isEqualTo(ComponentsConfiguration.minTouchTargetSizeDp)
    assertThat(event.attributes["component"] as? String)
        .describedAs("the reporting component name is recorded")
        .isNotNull()
  }

  @Test
  fun nonePolicy_emitsNoReport() {
    // minTouchTargetViolationPolicy defaults to NONE.
    renderSmallInteractive(ClickListenerCallback())
    assertThat(debugEvents).isEmpty()
  }

  @Test
  fun logPolicy_forNonInteractiveComponent_emitsNoReport() {
    ComponentsConfiguration.minTouchTargetViolationPolicy = LogicViolationPolicy.LOG

    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      Column {
        child(
            Column(
                style = Style.width(smallPx.px).height(smallPx.px).backgroundColor(Color.RED),
            ) {},
        )
      }
    }

    assertThat(debugEvents).isEmpty()
  }

  @Test
  fun logPolicy_forLargeInteractiveComponent_emitsNoReport() {
    ComponentsConfiguration.minTouchTargetViolationPolicy = LogicViolationPolicy.LOG
    val largePx = minTouchTargetPx() + 20

    lithoTestRule.render(widthPx = largePx + 40, heightPx = largePx + 40) {
      Column {
        child(
            OnClickCallbackComponent(
                callback = ClickListenerCallback(),
                style = Style.width(largePx.px).height(largePx.px),
            ),
        )
      }
    }

    assertThat(debugEvents).isEmpty()
  }

  private class ClickListenerCallback : View.OnClickListener {
    var handled = false
    var count = 0

    override fun onClick(v: View) {
      handled = true
      count++
    }

    fun reset() {
      handled = false
      count = 0
    }
  }

  companion object {
    /** The DebugInfoReporter category (the event's `name` attribute) for the opportunity report. */
    private const val TOUCH_TARGET_CATEGORY = "TouchTargetTooSmall"

    private fun View.emulateClickEvent(x: Int, y: Int) {
      val down =
          MotionEvent.obtain(
              SystemClock.uptimeMillis(),
              SystemClock.uptimeMillis(),
              MotionEvent.ACTION_DOWN,
              x.toFloat(),
              y.toFloat(),
              0,
          )
      val up =
          MotionEvent.obtain(
              SystemClock.uptimeMillis() + 10,
              SystemClock.uptimeMillis() + 10,
              MotionEvent.ACTION_UP,
              x.toFloat(),
              y.toFloat(),
              0,
          )
      dispatchTouchEvent(down)
      dispatchTouchEvent(up)
    }
  }
}
