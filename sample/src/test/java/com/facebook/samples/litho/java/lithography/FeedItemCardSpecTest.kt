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
import com.facebook.litho.testing.assertj.ComponentConditions.inspectedTypeIs
import com.facebook.litho.testing.assertj.ComponentConditions.textEquals
import com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat
import com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith
import com.facebook.litho.testing.subcomponents.InspectableComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.PrimitiveCard
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.allOf
import org.assertj.core.api.Condition
import org.assertj.core.data.Index.atIndex
import org.hamcrest.core.Is.`is`
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class FeedItemCardSpecTest {
  @get:Rule val lithoTestRule = LithoTestRule()

  private lateinit var component: Component

  companion object {
    private val ARTIST = Artist("Sindre Sorhus", "JavaScript Rockstar", 2010)
  }

  @Before
  fun setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        `is`(true),
    )

    val c = lithoTestRule.context

    component = FeedItemCard.create(c).artist(ARTIST).build()
  }

  @Test
  fun testShallowSubComponents() {
    val c = lithoTestRule.context
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .extractingSubComponents(c)
        .hasSize(1)
        .has(
            object : Condition<InspectableComponent>() {
              override fun matches(value: InspectableComponent): Boolean {
                return value.componentClass == PrimitiveCard::class.java
              }
            },
            atIndex(0),
        )
  }

  @Test
  fun testDeepSubComponentText() {
    val c = lithoTestRule.context

    assertThat(c, component)
        .has(
            allOf(
                deepSubComponentWith(c, textEquals("JavaScript Rockstar")),
                deepSubComponentWith(c, textEquals("Sindre Sorhus")),
            ),
        )
  }

  @Test
  fun testDeepSubComponentTextType() {
    val c = lithoTestRule.context

    assertThat(c, component).has(deepSubComponentWith(c, inspectedTypeIs(Text::class.java)))
  }

  @Test
  fun testDeepMatcherMatching() {
    val c = lithoTestRule.context

    // You can also test nested sub-components by passing in another Matcher where
    // you would normally provide a Component. In this case we provide a Matcher
    // of the FeedItemComponent to the Card Matcher's content prop.
    assertThat(c, component)
        .has(
            allOf(
                deepSubComponentWith(c, inspectedTypeIs(PrimitiveCard::class.java)),
                deepSubComponentWith(
                    c,
                    TestFeedItemComponent.matcher(c).artist(ARTIST).build(),
                ),
            ),
        )
  }
}
