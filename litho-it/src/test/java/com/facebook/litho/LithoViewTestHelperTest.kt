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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LithoViewTestHelperTest {

  @get:Rule val lithoViewRule = LithoViewRule()

  @Before
  fun skipIfRelease() {
    Assume.assumeTrue(
        "These tests cover debug functionality and can only be run " + "for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD)
  }

  @Test
  fun testBasicViewToString() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            return SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build()
          }
        }
    val lithoView = LithoView(getApplicationContext<Context>())
    lithoView.setComponent(component)
    lithoView.measure(unspecified(), unspecified())
    val string = LithoViewTestHelper.viewToString(lithoView)
    assertThat(string)
        .containsPattern(
            """litho.InlineLayout\{\w+ V.E..... .. 0,0-100,100\}
  litho.SimpleMountSpecTester\{\w+ V.E..... .. 0,0-100,100\}""")
  }

  @Test
  fun testBasicRootInstanceToStringWithDepth() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            return SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build()
          }
        }
    val lithoView = LithoView(getApplicationContext<Context>())
    lithoView.setComponent(component)
    lithoView.measure(unspecified(), unspecified())
    val root = DebugComponent.getRootInstance(lithoView)
    val string =
        LithoViewTestHelper.rootInstanceToString(root, false /* embedded */, 1 /* string depth */)
    assertThat(string)
        .containsPattern(
            """  litho.InlineLayout\{\w+ V.E..... .. 0,0-100,100\}
    litho.SimpleMountSpecTester\{\w+ V.E..... .. 0,0-100,100\}""")
  }

  @Test
  fun viewToStringWithText() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            return Column.create(c)
                .child(
                    SimpleMountSpecTester.create(c)
                        .testKey("test-drawable")
                        .widthPx(100)
                        .heightPx(100))
                .child(Text.create(c).widthPx(100).heightPx(100).text("Hello, World"))
                .build()
          }
        }
    val lithoView = LithoView(getApplicationContext<Context>())
    lithoView.setComponent(component)
    lithoView.measure(unspecified(), unspecified())
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    val string = LithoViewTestHelper.viewToString(lithoView)
    assertThat(string)
        .containsPattern(
            """litho.InlineLayout\{\w+ V.E..... .. 0,0-100,200\}
  litho.Column\{\w+ V.E..... .. 0,0-100,200\}
    litho.SimpleMountSpecTester\{\w+ V.E..... .. 0,0-100,100 litho:id/test-drawable\}
    litho.Text\{\w+ V.E..... .. 0,100-100,200 text="Hello, World"\}""")
  }

  @Test
  fun viewToStringForE2E_withExtraDescription_componentKeyIsPrinted() {
    val c = lithoViewRule.context
    val component: Component =
        Column.create(c)
            .key("column")
            .child(SimpleMountSpecTester.create(c).key("simple").widthPx(100).heightPx(100).build())
            .child(Text.create(c).key("text").widthPx(100).heightPx(100).text("Hello, World"))
            .build()

    val string =
        LithoViewTestHelper.viewToStringForE2E(
            lithoViewRule.render { component }.lithoView, 0, false) { debugComponent, sb ->
              sb.append(", key=").append(debugComponent.key)
            }
    assertThat(string)
        .containsPattern(
            """litho.Column\{\w+ V.E..... .. 0,0-1080,200, key=column}
  litho.SimpleMountSpecTester\{\w+ V.E..... .. 0,0-100,100, key=simple}
  litho.Text\{\w+ V.E..... .. 0,100-100,200 text="Hello, World", key=text}
""")
  }
}
