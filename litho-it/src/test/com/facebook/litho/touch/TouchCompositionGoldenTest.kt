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

import android.view.MotionEvent
import android.view.ViewConfiguration
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
import com.facebook.litho.view.onLongClick
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Golden traces for touch behavior that emerges from subsystems composing: a parent stealing the
 * stream partway through, `requestDisallowInterceptTouchEvent` blocking that, the long-clickable
 * route to becoming a touch target, and the touch-expansion delegate's own slop machine.
 *
 * These are the subtlest parts of the contract and the least likely to be reproduced by accident.
 */
@RunWith(LithoTestRunner::class)
class TouchCompositionGoldenTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  private val trace = TouchContractTrace()

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

  private class LongClickOnly(private val onStream: (Int) -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      child(
          Column(
              style =
                  Style.width(SIZE.px)
                      .height(SIZE.px)
                      .onLongClick { true }
                      .onTouch { event ->
                        onStream(event.motionEvent.actionMasked)
                        false
                      },
          ),
      )
    }
  }

  /** The parent lets DOWN through, then takes the stream on the first MOVE. */
  @Test
  fun `parent stealing the stream mid-gesture cancels the child`() {
    val testLithoView = lithoTestRule.render {
      Nested(
          outerStyle =
              trace.styleFor(
                  OUTER,
                  interceptWhen = { it.actionMasked != MotionEvent.ACTION_DOWN },
              ),
          innerStyle = trace.styleFor(INNER),
      )
    }

    testLithoView.lithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 5f, dy = 0f)
      up()
    }

    // The CANCEL reaches inner in OUTER's coordinate space: AOSP's cancel branch in
    // dispatchTransformedTouchEvent skips the child offset transform.
    trace.assertMatches(
        """
        outer.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        inner.onInterceptTouch[DOWN] view=ComponentHost#2(40,40) -> false
        inner.onTouch[DOWN] view=ComponentHost#2(40,40) -> false
        outer.onInterceptTouch[MOVE] view=ComponentHost#1(55,50) -> true
        inner.onTouch[CANCEL] view=ComponentHost#2(55,50) -> false
        outer.onTouch[UP] view=ComponentHost#1(55,50) -> false
        """
            .trimIndent(),
    )
  }

  /**
   * The child asks the parent not to intercept, which is how a horizontally-draggable row survives
   * inside a vertically-scrolling list.
   */
  @Test
  fun `child disallowing intercept keeps the stream from the parent`() {
    val testLithoView = lithoTestRule.render {
      Nested(
          outerStyle =
              trace.styleFor(
                  OUTER,
                  interceptWhen = { it.actionMasked != MotionEvent.ACTION_DOWN },
              ),
          innerStyle =
              trace.styleFor(
                  INNER,
                  onDown = { _, view ->
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                  },
              ),
      )
    }

    testLithoView.lithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 5f, dy = 0f)
      up()
    }

    // outer's intercept is never consulted again, so inner keeps the gesture and still clicks.
    trace.assertMatches(
        """
        outer.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        inner.onInterceptTouch[DOWN] view=ComponentHost#2(40,40) -> false
        inner.onTouch[DOWN] view=ComponentHost#2(40,40) -> false
        inner.onTouch[MOVE] view=ComponentHost#2(45,40) -> false
        inner.onTouch[UP] view=ComponentHost#2(45,40) -> false
        inner.onClick[-] view=ComponentHost#2
        """
            .trimIndent(),
    )
  }

  /** `onLongClick` sets isLongClickable, the third route to staying the touch target. */
  @Test
  fun `long-clickable alone keeps a non-consuming onTouch receiving the stream`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render { LongClickOnly { actions.add(it) } }

    testLithoView.lithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(actions)
        .containsExactly(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
  }

  /**
   * Dragging out of the expanded region relocates the event far outside the target, which is how
   * the delegate makes the target drop its pressed state.
   */
  @Test
  fun `dragging out of the expanded region relocates the event away from the target`() {
    val testLithoView = lithoTestRule.render { ExpandedTarget(trace.styleFor(TARGET)) }

    // Unlike the px layout constants, slop is density-derived, so the parked coordinate is computed
    // rather than written in. The slop assertion keeps a configuration change from quietly
    // rewriting the expectation instead of failing.
    val slop = ViewConfiguration.get(testLithoView.lithoView.context).scaledTouchSlop
    assertThat(slop).describedAs("golden captured at this touch slop").isEqualTo(16)
    val parked = -2 * slop

    testLithoView.performTouchInput {
      down(x = 4f, y = 4f)
      moveTo(x = 200f, y = 200f)
      up()
    }

    trace.assertMatches(
        """
        target.onInterceptTouch[DOWN] view=ComponentHost#1(10,10) -> false
        target.onTouch[DOWN] view=ComponentHost#1(10,10) -> false
        target.onTouch[MOVE] view=ComponentHost#1($parked,$parked) -> false
        target.onTouch[UP] view=ComponentHost#1($parked,$parked) -> false
        """
            .trimIndent(),
    )
  }

  companion object {
    private const val OUTER = "outer"
    private const val INNER = "inner"
    private const val TARGET = "target"
    private const val SIZE = TouchFixtureSizes.SIZE
    private const val PAD = TouchFixtureSizes.PAD
    private const val INNER_SIZE = TouchFixtureSizes.INNER_SIZE
    private const val SMALL = TouchFixtureSizes.SMALL
    private const val MARGIN = TouchFixtureSizes.MARGIN
    private const val EXPANSION = TouchFixtureSizes.EXPANSION
    private const val CENTER = TouchFixtureSizes.CENTER
  }
}
