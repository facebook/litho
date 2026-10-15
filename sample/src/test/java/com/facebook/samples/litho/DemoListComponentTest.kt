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

package com.facebook.samples.litho

import com.facebook.litho.Component
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat
import com.facebook.litho.testing.assertj.SubComponentExtractor.numOfSubComponents
import com.facebook.litho.testing.subcomponents.SubComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.core.Is.`is`
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class DemoListComponentTest {
  @get:Rule val lithoTestRule = LithoTestRule()

  private lateinit var component: Component

  @Before
  fun assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        `is`(true),
    )
  }

  @Before
  fun setUp() {
    component =
        DemoListRootComponent.create(lithoTestRule.context)
            .demos(ArrayList<Demos.DemoGrouping>())
            .previousIndices(null)
            .build()
  }

  @Test
  fun testSubComponents() {
    assertThat(lithoTestRule.context, component)
        .containsOnlySubComponents(SubComponent.of(RecyclerCollectionComponent::class.java))
  }

  @Test
  fun testNumOfSubComponents() {
    assertThat(lithoTestRule.context, component)
        .has(numOfSubComponents(lithoTestRule.context, `is`(1)))

    assertThat(lithoTestRule.context, component)
        .has(numOfSubComponents(lithoTestRule.context, greaterThan(0)))
  }
}
