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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.StackLayout
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.beginTouchInput
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Golden traces for how a touch is *routed* to a component: hit-test ordering between siblings,
 * multi-pointer, touch expansion, mid-gesture unmount, and non-View (Drawable) content.
 *
 * These are the routing decisions a root dispatcher has to re-derive from scratch, so they are the
 * ones most at risk of silently changing. Expectations here were captured from real runs.
 */
@RunWith(LithoTestRunner::class)
class TouchRoutingGoldenTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  private val trace = TouchContractTrace()

  /** Two same-size children in a stack, so both cover the tap point. */
  private class Overlapping(
      private val firstStyle: Style,
      private val secondStyle: Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = StackLayout {
      child(Column(style = Style.width(SIZE.px).height(SIZE.px) + firstStyle))
      child(Column(style = Style.width(SIZE.px).height(SIZE.px) + secondStyle))
    }
  }

  /** Two disjoint side-by-side targets, so each pointer lands on a different one. */
  private class SideBySide(
      private val leftStyle: Style,
      private val rightStyle: Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Row {
      child(Column(style = Style.width(SIZE.px).height(SIZE.px) + leftStyle))
      child(Column(style = Style.width(SIZE.px).height(SIZE.px) + rightStyle))
    }
  }

  private class WithBackground(private val drawable: Drawable) : KComponent() {
    override fun ComponentScope.render(): Component = Column {
      child(
          Column(
              style = Style.width(SIZE.px).height(SIZE.px).background(drawable).onClick {},
          ),
      )
    }
  }

  @Test
  fun `tap where two siblings overlap`() {
    val testLithoView = lithoTestRule.render {
      Overlapping(firstStyle = trace.styleFor(FIRST), secondStyle = trace.styleFor(SECOND))
    }

    testLithoView.lithoView.performTouchInput { click(CENTER, CENTER) }

    trace.assertMatches(
        """
        second.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        second.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        second.onTouch[UP] view=ComponentHost#1(50,50) -> false
        second.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  @Test
  fun `two pointers on two different targets`() {
    val testLithoView = lithoTestRule.render {
      SideBySide(leftStyle = trace.styleFor(LEFT), rightStyle = trace.styleFor(RIGHT))
    }

    testLithoView.lithoView.performTouchInput {
      down(x = CENTER, y = CENTER, pointerId = 0)
      down(x = SIZE + CENTER, y = CENTER, pointerId = 1)
      up(pointerId = 1)
      up(pointerId = 0)
    }

    trace.assertMatches(
        """
        left.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        left.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        right.onInterceptTouch[DOWN] view=ComponentHost#2(50,50) -> false
        right.onTouch[DOWN] view=ComponentHost#2(50,50) -> false
        left.onTouch[MOVE] view=ComponentHost#1(50,50) -> false
        right.onTouch[UP] view=ComponentHost#2(50,50) -> false
        right.onClick[-] view=ComponentHost#2
        left.onTouch[MOVE] view=ComponentHost#1(50,50) -> false
        left.onTouch[UP] view=ComponentHost#1(50,50) -> false
        left.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  @Test
  fun `tap inside the touch expansion but outside the bounds`() {
    val testLithoView = lithoTestRule.render { ExpandedTarget(trace.styleFor(TARGET)) }

    // Inside the margin band, so outside the target's own bounds but within its expansion.
    testLithoView.lithoView.performTouchInput { click(x = 4f, y = 4f) }

    trace.assertMatches(
        """
        target.onInterceptTouch[DOWN] view=ComponentHost#1(10,10) -> false
        target.onTouch[DOWN] view=ComponentHost#1(10,10) -> false
        target.onTouch[UP] view=ComponentHost#1(10,10) -> false
        target.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  @Test
  fun `tap inside the bounds of an expanded target`() {
    val testLithoView = lithoTestRule.render { ExpandedTarget(trace.styleFor(TARGET)) }

    // Deliberately off-centre: the delegate relocates out-of-bounds taps to the view centre, so a
    // centred point here would match for the wrong reason.
    testLithoView.lithoView.performTouchInput { click(x = MARGIN + 5f, y = MARGIN + 15f) }

    trace.assertMatches(
        """
        target.onInterceptTouch[DOWN] view=ComponentHost#1(5,15) -> false
        target.onTouch[DOWN] view=ComponentHost#1(5,15) -> false
        target.onTouch[UP] view=ComponentHost#1(5,15) -> false
        target.onClick[-] view=ComponentHost#1
        """
            .trimIndent(),
    )
  }

  /**
   * Content is unmounted between DOWN and UP, as incremental mount does when a row scrolls out. The
   * mounted-child assertions keep this from passing because the unmount silently did nothing.
   */
  @Test
  fun `component unmounted mid-gesture`() {
    val testLithoView = lithoTestRule.render { Overlapping(trace.styleFor(FIRST), Style) }

    val gesture = testLithoView.lithoView.beginTouchInput()
    gesture.down(CENTER, CENTER)
    assertThat(testLithoView.lithoView.childCount)
        .describedAs("target must be mounted when the gesture starts")
        .isGreaterThan(0)

    testLithoView.lithoView.unmountAllItems()
    assertThat(testLithoView.lithoView.childCount)
        .describedAs("target must be unmounted before the gesture ends")
        .isEqualTo(0)

    gesture.up()

    // The cancel Android synthesises on target removal carries zeroed coordinates.
    trace.assertMatches(
        """
        first.onInterceptTouch[DOWN] view=ComponentHost#1(50,50) -> false
        first.onTouch[DOWN] view=ComponentHost#1(50,50) -> false
        first.onTouch[CANCEL] view=ComponentHost#1(0,0) -> false
        """
            .trimIndent(),
    )
  }

  /** `MatrixDrawable` routes DOWN to a RippleDrawable background to position its hotspot. */
  @Test
  fun `tap on a component with a ripple background sets the hotspot`() {
    val hotspots = mutableListOf<String>()
    val ripple =
        object :
            RippleDrawable(
                ColorStateList.valueOf(Color.GRAY),
                Color.WHITE.toDrawable(),
                null,
            ) {
          override fun setHotspot(x: Float, y: Float) {
            hotspots.add("(${x.toInt()},${y.toInt()})")
            super.setHotspot(x, y)
          }
        }

    val testLithoView = lithoTestRule.render { WithBackground(ripple) }

    testLithoView.lithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(hotspots).describedAs("captured hotspots").isEqualTo(listOf("(50,50)"))
  }

  companion object {
    private const val FIRST = "first"
    private const val SECOND = "second"
    private const val LEFT = "left"
    private const val RIGHT = "right"
    private const val TARGET = "target"
    private const val SIZE = TouchFixtureSizes.SIZE
    private const val SMALL = TouchFixtureSizes.SMALL
    private const val MARGIN = TouchFixtureSizes.MARGIN
    private const val EXPANSION = TouchFixtureSizes.EXPANSION
    private const val CENTER = TouchFixtureSizes.CENTER
  }
}
