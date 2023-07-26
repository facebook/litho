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
package com.facebook.litho.animation

import android.annotation.SuppressLint
import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Row
import com.facebook.litho.StateCaller
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TransitionTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.StateWithTransitionTestComponent
import com.facebook.litho.widget.TestAnimationsComponent
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * We test the different kind of Transitions and how they compose different animations.
 *
 * All transitions are timed to 144 ms which translates to 9 frames (we actually step 10 because
 * * the first one does not count as it only initializes the values in the [*]) based on
 *   [MockTimingSource].FRAME_TIME_MS) which * is 16ms per frame.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@SuppressLint("ColorConstantUsageIssue")
@RunWith(LithoTestRunner::class)
class TransitionsTest {
  @JvmField @Rule val lithoViewRule = LithoViewRule()

  @JvmField @Rule val transitionTestRule = TransitionTestRule()

  private val stateCaller = StateCaller()

  private val testComponent: ComponentScope.(Boolean) -> Component = { state ->
    Column(alignItems = if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END) {
      child(
          Row(
              style =
                  Style.height(40.dp)
                      .width(40.dp)
                      .backgroundColor(Color.parseColor("#ee1111"))
                      .transitionKey(context, RED_TRANSITION_KEY)
                      .viewTag(RED_TRANSITION_KEY)))
      child(
          Row(
              style =
                  Style.height(40.dp)
                      .width(40.dp)
                      .backgroundColor(Color.parseColor("#11ee11"))
                      .transitionKey(context, GREEN_TRANSITION_KEY)
                      .viewTag(GREEN_TRANSITION_KEY)))
      child(
          Row(
              style =
                  Style.height(40.dp)
                      .width(40.dp)
                      .backgroundColor(Color.parseColor("#1111ee"))
                      .transitionKey(context, BLUE_TRANSITION_KEY)
                      .viewTag(BLUE_TRANSITION_KEY)))
    }
  }

  @Test
  fun transitionAnimations_runTransitionsInSequence_elementsShouldAnimateOneAfterTheOtherOnUpdateStateWithTransition() {
    val component = StateWithTransitionTestComponent(stateCaller, testComponent)
    val testLithoView = lithoViewRule.render { component }
    val redView = testLithoView.findViewWithTag(RED_TRANSITION_KEY)
    val greenView = testLithoView.findViewWithTag(GREEN_TRANSITION_KEY)
    val blueView = testLithoView.findViewWithTag(BLUE_TRANSITION_KEY)
    Assertions.assertThat(redView.x)
        .describedAs("redView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(greenView.x)
        .describedAs("greenView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(blueView.x)
        .describedAs("blueView should be at start position")
        .isEqualTo(1040f)
    stateCaller.update()
    lithoViewRule.idle()

    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(1040f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 10 frames").isEqualTo(1040f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 10 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 10 frames").isEqualTo(1040f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 10 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 20 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 30 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 30 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 30 frames").isEqualTo(0f)
  }

  @Test
  fun transitionAnimations_runTransitionsInSequence_elementsShouldAnimateOneAfterTheOther() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.sequence(
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X)),
            testComponent)

    val testLithoView = lithoViewRule.render { component }
    val redView = testLithoView.findViewWithTag(RED_TRANSITION_KEY)
    val greenView = testLithoView.findViewWithTag(GREEN_TRANSITION_KEY)
    val blueView = testLithoView.findViewWithTag(BLUE_TRANSITION_KEY)
    Assertions.assertThat(redView.x)
        .describedAs("redView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(greenView.x)
        .describedAs("greenView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(blueView.x)
        .describedAs("blueView should be at start position")
        .isEqualTo(1040f)
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 10 frames").isEqualTo(1040f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 10 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 20 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 30 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 30 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 30 frames").isEqualTo(0f)
  }

  @Test
  fun transitionAnimations_runTransitionsInParallel_elementsShouldAnimateAtTheSameTime() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.parallel(
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X)),
            testComponent)

    val testLithoView = lithoViewRule.render { component }
    val redView = testLithoView.findViewWithTag(RED_TRANSITION_KEY)
    val greenView = testLithoView.findViewWithTag(GREEN_TRANSITION_KEY)
    val blueView = testLithoView.findViewWithTag(BLUE_TRANSITION_KEY)
    Assertions.assertThat(redView.x)
        .describedAs("redView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(greenView.x)
        .describedAs("greenView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(blueView.x)
        .describedAs("blueView should be at start position")
        .isEqualTo(1040f)
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 10 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 10 frames").isEqualTo(0f)
  }

  @Test
  fun transitionAnimations_runTransitionsInStagger_elementsShouldAnimateStaggered() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.stagger(
                50,
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(GREEN_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X),
                Transition.create(BLUE_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X)),
            testComponent)

    val testLithoView = lithoViewRule.render { component }
    val redView = testLithoView.findViewWithTag(RED_TRANSITION_KEY)
    val greenView = testLithoView.findViewWithTag(GREEN_TRANSITION_KEY)
    val blueView = testLithoView.findViewWithTag(BLUE_TRANSITION_KEY)
    Assertions.assertThat(redView.x)
        .describedAs("redView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(greenView.x)
        .describedAs("greenView should be at start position")
        .isEqualTo(1040f)
    Assertions.assertThat(blueView.x)
        .describedAs("blueView should be at start position")
        .isEqualTo(1040f)
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(5)
    Assertions.assertThat(redView.x).describedAs("redView after 5 frames").isEqualTo(610.2971f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 5 frames").isEqualTo(1040f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 5 frames").isEqualTo(1040f)
    transitionTestRule.step(5)
    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x)
        .describedAs("greenView after 10 frames")
        .isEqualTo(429.70294f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 10 frames").isEqualTo(1008.64014f)
    transitionTestRule.step(5)
    Assertions.assertThat(redView.x).describedAs("redView after 15 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 15 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 15 frames").isEqualTo(259.99988f)
    transitionTestRule.step(5)
    Assertions.assertThat(redView.x).describedAs("redView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(greenView.x).describedAs("greenView after 20 frames").isEqualTo(0f)
    Assertions.assertThat(blueView.x).describedAs("blueView after 20 frames").isEqualTo(0f)
  }

  @Test
  fun transitionAnimations_runDelayedTransition_elementsShouldAnimateAfterDelay() {
    val component =
        TestAnimationsComponent(
            stateCaller,
            Transition.delay(
                144,
                Transition.create(RED_TRANSITION_KEY)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X)),
            testComponent)

    val testLithoView = lithoViewRule.render { component }
    val redView = testLithoView.findViewWithTag(RED_TRANSITION_KEY)
    Assertions.assertThat(redView.x)
        .describedAs("redView should be at start position")
        .isEqualTo(1040f)
    stateCaller.update()
    lithoViewRule.idle()
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 10 frames").isEqualTo(1040f)
    transitionTestRule.step(10)
    Assertions.assertThat(redView.x).describedAs("redView after 20 frames").isEqualTo(0f)
  }

  companion object {
    const val RED_TRANSITION_KEY = "red"
    const val GREEN_TRANSITION_KEY = "green"
    const val BLUE_TRANSITION_KEY = "blue"
  }
}
