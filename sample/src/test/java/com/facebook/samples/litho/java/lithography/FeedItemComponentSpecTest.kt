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
import com.facebook.litho.testing.assertj.ComponentConditions.textEquals
import com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat
import com.facebook.litho.testing.assertj.LithoViewSubComponentDeepExtractor.deepSubComponentWith
import com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.subcomponents.SubComponent
import com.facebook.litho.testing.subcomponents.SubComponent.legacySubComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.hamcrest.core.Is.`is`
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class FeedItemComponentSpecTest {
  @get:Rule val lithoTestRule = LithoTestRule()

  private lateinit var component: Component

  @Before
  fun setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        `is`(true),
    )
    val c = lithoTestRule.context
    component =
        FeedItemComponent.create(c)
            .artist(Artist("Sindre Sorhus", "Rockstar Developer", 2010))
            .build()
  }

  @Test
  fun recursiveSubComponentExists() {
    val c = lithoTestRule.context

    assertThat(c, component).extractingSubComponentAt(0).extractingSubComponents(c).hasSize(2)
  }

  @Test
  fun testLithoViewSubComponentMatching() {
    val c = lithoTestRule.context
    val lithoView = ComponentTestHelper.mountComponent(c, component)

    assertThat(lithoView).has(deepSubComponentWith(textEquals("Sindre Sorhus")))
  }

  @Test
  fun subComponentLegacyBridge() {
    val c = lithoTestRule.context

    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                legacySubComponent(
                    SubComponent.of(FooterComponent.create(c).text("Rockstar Developer").build()),
                ),
            ),
        )
  }
}
