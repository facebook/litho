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

import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.EventHandler
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat
import com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.core.IsNull
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Demonstrates testing sub components based on [FeedItemComponentSpec]'s [FooterComponentSpec] use.
 */
@RunWith(LithoTestRunner::class)
class FeedItemComponentSpecSubComponentTest {
  @get:Rule val lithoTestRule = LithoTestRule()

  @Before
  fun setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        `is`(true),
    )
  }

  @Test
  fun subComponentWithoutProperties() {
    val c = lithoTestRule.context
    val component = makeComponentWithTextInSubcomponent("Any String")

    // This will match as long as there is a FooterComponent, with any props.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).build()))
  }

  @Test
  fun subComponentWithRawText() {
    val c = lithoTestRule.context
    val component = makeComponentWithTextInSubcomponent("Raw Text")

    // This will match if the component has exactly the specified text as property.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).text("Raw Text").build()))
  }

  @Test
  fun subComponentWithMatcher() {
    val c = lithoTestRule.context
    val component =
        makeComponentWithTextInSubcomponent("Long Text That We Don't Want To Match In Its Entirety")

    // We can pass in any of the default hamcrest matchers here.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                TestFooterComponent.matcher(c).text(containsString("Want To Match")).build(),
            ),
        )
  }

  @Test
  fun subComponentWithRes() {
    val c = lithoTestRule.context
    val string = c.androidContext.resources.getString(android.R.string.cancel)
    val component = makeComponentWithTextInSubcomponent(string)

    // You can also reference resources here directly.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                TestFooterComponent.matcher(c).textRes(android.R.string.cancel).build(),
            ),
        )
  }

  @Test
  fun footerHasNoClickHandler() {
    val c = lithoTestRule.context
    val component = makeComponentWithTextInSubcomponent("Any Text")

    // Components commonly have conditional handlers assigned. Using the clickHandler matcher
    // we can assert whether or not a given component has a handler attached to them.
    @Suppress("UNCHECKED_CAST")
    val nullValueMatcher = IsNull.nullValue() as org.hamcrest.Matcher<EventHandler<ClickEvent>>
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                TestFooterComponent.matcher(c).clickHandler(nullValueMatcher).build(),
            ),
        )
  }

  private fun makeComponentWithTextInSubcomponent(value: String): Component {
    val c = lithoTestRule.context
    return FeedItemComponent.create(c).artist(Artist("Some Name", value, 2001)).build()
  }
}
