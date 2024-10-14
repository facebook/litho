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
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateViewTooltipTextTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun testInnerComponentHostTooltipText() {
    val tooltipText1 = "Tooltip Text 1"
    val tooltipText2 = "Tooltip Text 2"

    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(
                          Column.create(c)
                              .tooltipText(tooltipText1)
                              .child(SimpleMountSpecTester.create(c))
                              .child(SimpleMountSpecTester.create(c)))
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c).tooltipText(tooltipText2))
                      .build()
            })
    val innerHost1 = lithoView.getChildAt(0)
    val innerHost2 = lithoView.getChildAt(1)
    assertThat(innerHost1.tooltipText).isEqualTo(tooltipText1)
    assertThat(innerHost2.tooltipText).isEqualTo(tooltipText2)
  }

  @Test
  fun testRootHostTooltipText() {
    val tooltipText = "Tooltip Text"
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .tooltipText(tooltipText)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    val innerHost = lithoView.getChildAt(0)
    assertThat(innerHost.tooltipText).isEqualTo(tooltipText)
  }
}
