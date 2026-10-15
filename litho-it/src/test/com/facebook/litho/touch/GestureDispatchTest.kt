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
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.clickable
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onLongClick
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterizes how a real `MotionEvent` stream reaches Litho's touch handlers.
 *
 * Everything asserted here is inherited from `View`/`ViewGroup`, since a component with a touch
 * handler mounts into its own `ComponentHost`. Pins it so a change to touch dispatch has to
 * preserve it.
 */
@RunWith(LithoTestRunner::class)
class GestureDispatchTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  private class TouchTargetComponent(
      private val style: Style = Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = Row {
      child(
          Row(
              style = Style.width(TARGET_SIZE.px).height(TARGET_SIZE.px).plus(style),
          ),
      )
    }
  }

  @Test
  fun `tap dispatches click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { TouchTargetComponent(Style.onClick { clicks++ }) }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(clicks).isEqualTo(1)
  }

  /** A lone clickable host abandons a tap on leaving its bounds, not on travel distance. */
  @Test
  fun `drag that stays inside the bounds still dispatches click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { TouchTargetComponent(Style.onClick { clicks++ }) }

    // The point of the test is a move that exceeds slop yet stays in bounds. If slop ever grows
    // past the target, the gesture would leave the bounds and pass for the opposite reason.
    val slop = ViewConfiguration.get(testLithoView.lithoView.context).scaledTouchSlop
    assertThat(CENTER + slop + 1)
        .describedAs("slop move must stay inside the target")
        .isLessThan(TARGET_SIZE.toFloat())

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBeyondTouchSlop()
      up()
    }

    assertThat(clicks)
        .describedAs("Movement within the target's bounds does not abandon the tap")
        .isEqualTo(1)
  }

  @Test
  fun `drag out of the bounds abandons the tap`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { TouchTargetComponent(Style.onClick { clicks++ }) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveTo(x = TARGET_SIZE * 2f, y = CENTER)
      up()
    }

    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `cancel suppresses click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { TouchTargetComponent(Style.onClick { clicks++ }) }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      cancel()
    }

    assertThat(clicks).describedAs("ACTION_CANCEL must abandon the gesture").isEqualTo(0)
  }

  @Test
  fun `long press dispatches long click`() {
    var longClicks = 0
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.onLongClick {
            longClicks++
            true
          },
      )
    }

    testLithoView.performTouchInput { longClick(CENTER, CENTER) }

    assertThat(longClicks).isEqualTo(1)
  }

  @Test
  fun `consuming long click suppresses the following click`() {
    var clicks = 0
    var longClicks = 0
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.onClick { clicks++ }
              .onLongClick {
                longClicks++
                true
              },
      )
    }

    testLithoView.performTouchInput { longClick(CENTER, CENTER) }

    assertThat(longClicks).isEqualTo(1)
    assertThat(clicks).describedAs("A consumed long click must not also fire a click").isEqualTo(0)
  }

  @Test
  fun `consuming onTouch receives the whole event stream`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.onTouch { event ->
            actions.add(event.motionEvent.actionMasked)
            true
          },
      )
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(actions)
        .containsExactly(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
  }

  /**
   * With no click handler the host isn't clickable, so `onTouchEvent` doesn't consume the DOWN and
   * `ViewGroup` never records it as the touch target for the rest of the gesture.
   */
  @Test
  fun `non-consuming onTouch on a non-clickable component receives only the down`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.onTouch { event ->
            actions.add(event.motionEvent.actionMasked)
            false
          },
      )
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(actions).containsExactly(MotionEvent.ACTION_DOWN)
  }

  /**
   * The predictor is the clickable flag, not the presence of a click handler: `Style.clickable`
   * alone is enough for `onTouchEvent` to consume the DOWN and keep the child as the touch target.
   */
  @Test
  fun `non-consuming onTouch on a clickable-flagged component receives the whole event stream`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.clickable(true).onTouch { event ->
            actions.add(event.motionEvent.actionMasked)
            false
          },
      )
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(actions)
        .containsExactly(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
  }

  /** A click handler makes the host clickable, so `onTouchEvent` consumes the DOWN regardless. */
  @Test
  fun `non-consuming onTouch on a clickable component receives the whole event stream`() {
    val actions = mutableListOf<Int>()
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(
          Style.onClick {}
              .onTouch { event ->
                actions.add(event.motionEvent.actionMasked)
                false
              },
      )
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(actions)
        .containsExactly(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
  }

  @Test
  fun `onTouch returning true consumes the gesture and suppresses click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(Style.onClick { clicks++ }.onTouch { true })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(clicks)
        .describedAs("onTouch consuming the stream must starve the click listener")
        .isEqualTo(0)
  }

  @Test
  fun `onTouch returning false lets the click through`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render {
      TouchTargetComponent(Style.onClick { clicks++ }.onTouch { false })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(clicks).isEqualTo(1)
  }

  @Test
  fun `tap outside the target bounds does not dispatch click`() {
    var clicks = 0
    val testLithoView = lithoTestRule.render { TouchTargetComponent(Style.onClick { clicks++ }) }

    testLithoView.performTouchInput {
      click(x = TARGET_SIZE + 20f, y = TARGET_SIZE + 20f)
    }

    assertThat(clicks).isEqualTo(0)
  }

  companion object {
    private const val TARGET_SIZE = 100
    private const val CENTER = TARGET_SIZE / 2f
  }
}
