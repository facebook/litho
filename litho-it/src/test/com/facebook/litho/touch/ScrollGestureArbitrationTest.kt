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
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onTouch
import com.facebook.litho.widget.VerticalScroll
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterizes a component's touch handlers when a scrolling ancestor claims the gesture.
 *
 * "A tap must not fire when the user was scrolling" isn't implemented by Litho; it falls out of the
 * scroll container intercepting, which makes `ViewGroup` deliver `ACTION_CANCEL` to the child.
 */
@RunWith(LithoTestRunner::class)
class ScrollGestureArbitrationTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  /** A tall column of clickable rows inside a [VerticalScroll], so the ancestor can scroll. */
  private class ScrollingList(
      private val onRowClick: () -> Unit = {},
      private val onRowTouch: ((Int) -> Unit)? = null,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = VerticalScroll(
        style = Style.width(VIEWPORT.px).height(VIEWPORT.px),
        child =
            Column {
              repeat(ROW_COUNT) {
                child(
                    Column(
                        style =
                            Style.width(VIEWPORT.px)
                                .height(ROW_HEIGHT.px)
                                .onClick { onRowClick() }
                                .let { style ->
                                  val touch = onRowTouch
                                  if (touch == null) {
                                    style
                                  } else {
                                    style.onTouch { event ->
                                      touch(event.motionEvent.actionMasked)
                                      false
                                    }
                                  }
                                },
                    ),
                )
              }
            },
    )
  }

  @Test
  fun `tap on a row inside a scroll container dispatches click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { ScrollingList(onRowClick = { clicks++ }) }

    testLithoView.performTouchInput { click(x = ROW_CENTER_X, y = ROW_CENTER_Y) }

    assertThat(clicks).isEqualTo(1)
  }

  @Test
  fun `scrolling past touch slop does not fire the row click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { ScrollingList(onRowClick = { clicks++ }) }

    testLithoView.performTouchInput {
      down(ROW_CENTER_X, ROW_CENTER_Y)
      // Two moves clear touch slop while every coordinate stays inside the container, so the
      // scenario is the container claiming the gesture rather than the pointer leaving it.
      moveBy(dx = 0f, dy = -SCROLL_DISTANCE)
      moveBy(dx = 0f, dy = -SCROLL_DISTANCE)
      up()
    }

    assertThat(clicks)
        .describedAs("A scroll must not be delivered as a tap on the row under the finger")
        .isEqualTo(0)
  }

  @Test
  fun `row receives ACTION_CANCEL when the scroll container takes the gesture`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render { ScrollingList(onRowTouch = { actions.add(it) }) }

    testLithoView.performTouchInput {
      down(ROW_CENTER_X, ROW_CENTER_Y)
      moveBy(dx = 0f, dy = -SCROLL_DISTANCE)
      moveBy(dx = 0f, dy = -SCROLL_DISTANCE)
      up()
    }

    assertThat(actions).startsWith(MotionEvent.ACTION_DOWN)
    assertThat(actions)
        .describedAs("The interrupted child must be told the gesture was taken from it")
        .contains(MotionEvent.ACTION_CANCEL)
  }

  companion object {
    private const val VIEWPORT = 300
    private const val ROW_HEIGHT = 100
    private const val ROW_COUNT = 20
    private const val ROW_CENTER_X = VIEWPORT / 2f
    private const val ROW_CENTER_Y = ROW_HEIGHT / 2f
    private const val SCROLL_DISTANCE = 20f
  }
}
