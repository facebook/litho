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

package com.facebook.litho.layout

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.RenderWithConstraints
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.onCleanup
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class RenderWithConstraintsTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private val defaultAnimationDisabled = ComponentsConfiguration.isAnimationDisabled

  @Before
  fun setup() {
    ComponentsConfiguration.isAnimationDisabled = false
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.isAnimationDisabled = defaultAnimationDisabled
  }

  /** This test will fail */
  @Test(expected = AssertionError::class)
  fun `when component renders with constraints then effect should run`() {
    var effectRunCount = 0
    var effectCleanupCount = 0
    val component = TestComponent({ effectRunCount++ }, { effectCleanupCount++ })

    lithoTestRule.render { component }

    assertThat(effectRunCount).isEqualTo(1) // WILL FAIL HERE

    assertThat(effectCleanupCount).isEqualTo(0)

    lithoTestRule.render { EmptyComponent() }
  }

  /** This test will fail */
  @Test(expected = AssertionError::class)
  fun `when component renders with constraints then transitions should be collected`() {
    val component = TestComponent({}, {})

    val handle = lithoTestRule.render { component }

    assertThat(handle.committedLayoutState!!.transitions).hasSize(1) // WILL FAIL HERE
  }

  @Test
  fun `when component renders with constraints then state updates should work correctly`() {
    val component = TestComponent({}, {})

    val handle = lithoTestRule.render { component }

    lithoTestRule.act(handle) { handle.findViewWithText("count0: 0").performClick() }

    handle.findViewWithText("count0: 1")
    handle.findViewWithText("count1: 0")

    lithoTestRule.act(handle) { handle.findViewWithText("count1: 0").performClick() }

    handle.findViewWithText("count0: 1")
    handle.findViewWithText("count1: 1")
  }

  internal class TestComponent(
      val runEffects: () -> Unit,
      val cleanupEffects: () -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      return RenderWithConstraints { constraints ->
        val count0 = useState { 0 }
        val count1 = useState { 0 }

        useEffect(Unit) {
          runEffects()
          onCleanup { cleanupEffects() }
        }

        useTransition(
            Transition.create("render-with-constraints")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .animator(Transition.timing(100)),
        )

        Column {
          child(Text(text = "Hello World"))
          child(
              Text(
                  text = "count0: ${count0.value}",
                  style = Style.viewTag("count0").onClick { count0.update { it + 1 } },
              ),
          )
          child(
              Text(
                  text = "count1: ${count1.value}",
                  style = Style.viewTag("count1").onClick { count1.update { it + 1 } },
              ),
          )
        }
      }
    }
  }
}
