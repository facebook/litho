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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.enabled
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Golden traces of the touch contract as implemented by Views today.
 *
 * The dispatch refactor must be semantics-preserving, so a diff here is a semantic change needing
 * an explicit decision, not necessarily a bug. Every expectation below was captured from a real
 * run, not predicted.
 *
 * Three contract details these traces caught that are easy to get wrong when reimplementing:
 * - `onInterceptTouch` fires on the component that ends up handling the touch, not only on
 *   ancestors, because every `ComponentHost` is a `ViewGroup` and `ViewGroup` consults itself.
 * - It fires again on later events only while that host has a child touch target. A leaf host sees
 *   intercept for DOWN alone; a host whose child took the gesture also sees UP.
 * - `Style.enabled(false)` strips the handlers, so a component with nothing else forcing a host is
 *   not hosted at all and sees nothing. Force a host another way and the asymmetry shows: the
 *   listeners stay gated on `isEnabled`, but `onInterceptTouch` has no such check and still fires.
 */
@RunWith(LithoTestRunner::class)
class TouchContractGoldenTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  private val trace = TouchContractTrace()

  private class Single(private val style: Style) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      child(Column(style = Style.width(SIZE.px).height(SIZE.px) + style))
    }
  }

  /** Outer wraps inner with padding, so a tap can land on either. */
  private class Nested(
      private val outerStyle: Style,
      private val innerStyle: Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      child(
          Column(
              style = Style.width(SIZE.px).height(SIZE.px).padding(all = PAD.px) + outerStyle,
          ) {
            child(
                Column(
                    style = Style.width(INNER_SIZE.px).height(INNER_SIZE.px) + innerStyle,
                ),
            )
          },
      )
    }
  }

  @Test
  fun `tap on a fully-handled component`() {
    val testLithoView = lithoTestRule.render { Single(trace.styleFor(TARGET)) }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertTrace(
        """
        touch_target.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[UP] view=ComponentHost#1(50,50) -> false
        touch_target.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  @Test
  fun `drag inside bounds on a fully-handled component`() {
    val testLithoView = lithoTestRule.render { Single(trace.styleFor(TARGET)) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 10f, dy = 0f)
      up()
    }

    assertTrace(
        """
        touch_target.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[MOVE] view=ComponentHost#1(60,50) -> false
        touch_target.onTouch[UP] view=ComponentHost#1(60,50) -> false
        touch_target.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  @Test
  fun `cancel on a fully-handled component`() {
    val testLithoView = lithoTestRule.render { Single(trace.styleFor(TARGET)) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      cancel()
    }

    assertTrace(
        """
        touch_target.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[CANCEL] view=ComponentHost#1(50,50) -> false
        """
            .trimIndent(),
    )
  }

  /** A lone onTouch never sets the clickable flag, so the host is not made a touch target. */
  @Test
  fun `drag with only a non-consuming onTouch`() {
    val testLithoView = lithoTestRule.render { Single(trace.touchOnlyStyleFor(TARGET)) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 10f, dy = 0f)
      up()
    }

    assertTrace("touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false")
  }

  @Test
  fun `drag with only a consuming onTouch`() {
    val testLithoView = lithoTestRule.render {
      Single(trace.touchOnlyStyleFor(TARGET, returns = true))
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 10f, dy = 0f)
      up()
    }

    assertTrace(
        """
        touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> true
        touch_target.onTouch[MOVE] view=ComponentHost#1(60,50) -> true
        touch_target.onTouch[UP] view=ComponentHost#1(60,50) -> true
        """
            .trimIndent(),
    )
  }

  /**
   * Disabling strips the handlers, and handlers are the only thing that would have forced a host
   * here, so the component is not hosted at all and nothing fires.
   */
  @Test
  fun `tap on a disabled component`() {
    val testLithoView = lithoTestRule.render {
      Single(trace.styleFor(TARGET) + Style.enabled(false))
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertTrace("")
  }

  /**
   * With a host forced for an unrelated reason, the asymmetry shows: disabling gates the listeners,
   * which `View.dispatchTouchEvent` checks `isEnabled` before calling, but not the ViewGroup-level
   * intercept override, which has no such check.
   */
  @Test
  fun `tap on a disabled component that is hosted anyway`() {
    val testLithoView = lithoTestRule.render {
      Single(trace.styleFor(TARGET) + Style.enabled(false) + Style.wrapInView())
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertTrace("touch_target.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false")
  }

  @Test
  fun `long press on a fully-handled component`() {
    val testLithoView = lithoTestRule.render { Single(trace.styleFor(TARGET)) }

    testLithoView.performTouchInput { longClick(CENTER, CENTER) }

    assertTrace(
        """
        touch_target.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        touch_target.onLongClick[-] view=ComponentHost#1 -> true
        touch_target.onTouch[UP] view=ComponentHost#1(50,50) -> false
        """
            .trimIndent(),
    )
  }

  /**
   * Both hosts see intercept on the way down, and coordinates are host-relative: a LithoView tap at
   * (50,50) reaches the inner host at (40,40) through PAD px of padding. Outer sees intercept again
   * on UP because it has a child touch target; inner, a leaf, does not.
   */
  @Test
  fun `tap on the inner of two nested handled components`() {
    val testLithoView = lithoTestRule.render {
      Nested(outerStyle = trace.styleFor(OUTER), innerStyle = trace.styleFor(INNER))
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertTrace(
        """
        outer.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        inner.onInterceptTouch[DOWN] view=ComponentHost#2(40,40) -> false
        inner.onTouch[DOWN] view=ComponentHost#2(40,40) -> false
        outer.onInterceptTouch[UP] view=ComponentHost#1(50,50) -> false
        inner.onTouch[UP] view=ComponentHost#2(40,40) -> false
        inner.onClick[-] view=ComponentHost#2
        """
            .trimIndent(),
    )
  }

  /** Once outer intercepts, inner is never consulted and intercept is not re-checked. */
  @Test
  fun `outer intercept steals the gesture from the inner component`() {
    val testLithoView = lithoTestRule.render {
      Nested(
          outerStyle = trace.styleFor(OUTER, onInterceptTouchReturns = true),
          innerStyle = trace.styleFor(INNER),
      )
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertTrace(
        """
        outer.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> true
        outer.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        outer.onTouch[UP] view=ComponentHost#1(50,50) -> false
        outer.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  /**
   * The host under these fixtures exists only because a touch handler forced it, which is exactly
   * the reason a root dispatcher removes. Asserting it here keeps that premise visible: if these
   * stop being hosted, the traces above are describing a different configuration than intended.
   */
  @Test
  fun `a component with only onTouch is hosted for an interaction reason`() {
    val testLithoView = lithoTestRule.render { Single(trace.touchOnlyStyleFor(TARGET)) }

    assertThat(testLithoView.lithoView.innerHostCount())
        .describedAs("onTouch alone forces a ComponentHost today")
        .isEqualTo(1)
  }

  /**
   * The same stream rule when the host exists for a structural reason instead of the handler. This
   * is the configuration that keeps its host after a dispatch change, so it is the control: the
   * completeness rule follows the clickable flag, not the presence of a host.
   */
  @Test
  fun `drag with only a non-consuming onTouch on a component hosted anyway`() {
    val testLithoView = lithoTestRule.render {
      Single(trace.touchOnlyStyleFor(TARGET) + Style.wrapInView())
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 10f, dy = 0f)
      up()
    }

    assertTrace("touch_target.onTouch[DOWN] view=ComponentHost#1(50,50) -> false")
  }

  /** Counts hosts mounted below the root; the LithoView is itself a ComponentHost. */
  private fun android.view.View.innerHostCount(): Int {
    var count = 0
    if (this is android.view.ViewGroup) {
      for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is com.facebook.litho.ComponentHost) {
          count++
        }
        count += child.innerHostCount()
      }
    }
    return count
  }

  /** Prints the observed trace on mismatch, since assertion diffs elide long strings. */
  private fun assertTrace(expected: String) {
    val actual = trace.asText()
    if (actual != expected) {
      println("Observed trace:\n$actual")
    }
    assertThat(actual).isEqualTo(expected)
  }

  companion object {
    private const val TARGET = "touch_target"
    private const val OUTER = "outer"
    private const val INNER = "inner"
    private const val SIZE = 100
    private const val PAD = 10
    private const val INNER_SIZE = SIZE - 2 * PAD
    private const val CENTER = SIZE / 2f
  }
}
