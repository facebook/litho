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

package com.facebook.litho

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.assertj.LithoViewAssert.Companion.assertThat
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(LithoTestRunner::class)
class MountStateTestItemTest {

  private lateinit var context: ComponentContext
  private var originalE2ETestRun = false

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    originalE2ETestRun = ComponentsConfiguration.isEndToEndTestRun
    ComponentsConfiguration.isEndToEndTestRun = true
  }

  @After
  fun teardown() {
    ComponentsConfiguration.isEndToEndTestRun = originalE2ETestRun
  }

  @Test
  fun testInnerComponentHostViewTags() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(SimpleMountSpecTester.create(c))
                            .child(SimpleMountSpecTester.create(c))
                            .testKey(TEST_ID_1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c).testKey(TEST_ID_2))
                    .build()
              }
            })
    assertThat(lithoView)
        .containsTestKey(TEST_ID_1)
        .containsTestKey(TEST_ID_2)
        .doesNotContainTestKey(TEST_ID_3)
  }

  @Test
  fun testEndToEndExtensionsWorkWithRenderCoreMountState() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c).testKey(TEST_ID_1).build()
              }
            })
    assertThat(lithoView).containsTestKey(TEST_ID_1)
  }

  @Test
  fun testMultipleIdenticalInnerComponentHostViewTags() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(SimpleMountSpecTester.create(c))
                            .child(SimpleMountSpecTester.create(c))
                            .testKey(TEST_ID_1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c).testKey(TEST_ID_1))
                    .build()
              }
            })
    assertThat(lithoView)
        .containsTestKey(TEST_ID_1, LithoViewAssert.times(2))
        .doesNotContainTestKey(TEST_ID_2)
  }

  @Test
  fun testSkipInvalidTestKeys() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .child(SimpleMountSpecTester.create(c))
                            .child(SimpleMountSpecTester.create(c))
                            .testKey(""))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c).testKey(null))
                    .child(SimpleMountSpecTester.create(c).testKey(TEST_ID_1))
                    .build()
              }
            })
    assertThat(lithoView)
        .doesNotContainTestKey("")
        .doesNotContainTestKey(null)
        .containsTestKey(TEST_ID_1)
  }

  @Test
  fun testTextItemTextContent() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(Text.create(c).text(MY_TEST_STRING_1).testKey(TEST_ID_1))
                    .build()
              }
            })
    assertThat(lithoView).containsTestKey(TEST_ID_1)
    val item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1)
    assertThat(item1?.textContent).isEqualTo(MY_TEST_STRING_1)
  }

  @Test
  fun testMultipleTextItemsTextContents() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(Text.create(c).text(MY_TEST_STRING_1).testKey(TEST_ID_1))
                    .child(Text.create(c).text(MY_TEST_STRING_2).testKey(TEST_ID_2))
                    .build()
              }
            })
    assertThat(lithoView).containsTestKey(TEST_ID_1)
    val item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1)
    assertThat(item1?.textContent).isEqualTo(MY_TEST_STRING_1)
    val item2 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_2)
    assertThat(item2?.textContent).isEqualTo(MY_TEST_STRING_2)
  }

  @Test
  fun testTextItemsWithClickHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component {
                return Column.create(c)
                    .child(
                        Text.create(c)
                            .text(MY_TEST_STRING_1)
                            .clickHandler(mock<EventHandler<ClickEvent>>())
                            .testKey(TEST_ID_1))
                    .child(Text.create(c).text(MY_TEST_STRING_2).testKey(TEST_ID_2))
                    .build()
              }
            })
    assertThat(lithoView).containsTestKey(TEST_ID_1)
    val item1 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_1)
    assertThat(item1?.textContent).isEqualTo(MY_TEST_STRING_1)
    val item2 = LithoViewTestHelper.findTestItem(lithoView, TEST_ID_2)
    assertThat(item2?.textContent).isEqualTo(MY_TEST_STRING_2)
  }

  companion object {
    private const val TEST_ID_1 = "test_id_1"
    private const val TEST_ID_2 = "test_id_2"
    private const val TEST_ID_3 = "test_id_3"
    private const val MY_TEST_STRING_1 = "My test string"
    private const val MY_TEST_STRING_2 = "My second test string"
  }
}
