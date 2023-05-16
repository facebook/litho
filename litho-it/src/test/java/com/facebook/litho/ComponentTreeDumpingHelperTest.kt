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
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentTreeDumpingHelperTest {

  @Before
  fun skipIfRelease() {
    Assume.assumeTrue(
        "These tests cover debug functionality and can only be run for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD)
  }

  @Test
  fun testBasicComponentTreeDumping() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              SimpleMountSpecTester.create(c).widthPx(100).heightPx(100).build()
        }
    var componentContext: ComponentContext =
        ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val componentTree = ComponentTree.create(componentContext, component).build()
    componentContext = ComponentContextUtils.withComponentTree(componentContext, componentTree)
    val lithoView = LithoView(ApplicationProvider.getApplicationContext<Context>())
    lithoView.componentTree = componentTree
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    val string = ComponentTreeDumpingHelper.dumpContextTree(componentTree)
    assertThat(string)
        .containsPattern("InlineLayout\\{V}\n  SimpleMountSpecTester\\{V 100.0x100.0}")
  }
}
