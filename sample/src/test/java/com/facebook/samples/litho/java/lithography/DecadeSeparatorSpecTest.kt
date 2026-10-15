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

package com.facebook.samples.litho.java.lithography

import com.facebook.litho.Component
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.subcomponents.InspectableComponent
import com.facebook.litho.testing.subcomponents.SubComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import org.assertj.core.api.Condition
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class DecadeSeparatorSpecTest {
  @get:Rule val lithoTestRule = LithoTestRule()

  private lateinit var component: Component

  @Before
  fun setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        `is`(true),
    )
    component = DecadeSeparator.create(lithoTestRule.context).decade(Decade(2010)).build()
  }

  @Test
  fun subComponentsWithManualExtraction() {
    val c = lithoTestRule.context

    assertThat(c, component).extractingSubComponentAt(0).extractingSubComponents(c).hasSize(3)
  }

  @Test
  fun testSubComponentByClass() {
    val c = lithoTestRule.context
    assertThat(c, component).hasSubComponents(SubComponent.of(Text::class.java))
  }

  @Test
  fun subComponentByClassWithExtraction() {
    val c = lithoTestRule.context
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .extractingSubComponents(c)
        .areExactly(
            1,
            object : Condition<InspectableComponent>() {
              override fun matches(value: InspectableComponent): Boolean {
                return value.componentClass == Text::class.java
              }
            },
        )
  }

  @Test
  fun subComponentWithText() {
    val c = lithoTestRule.context
    val testLithoView = lithoTestRule.render { component }
    LithoAssertions.assertThat(testLithoView)
        .hasVisibleText("2010")
        .doesNotHaveVisibleText("2011")
        .hasVisibleTextMatching("10")
  }
}
