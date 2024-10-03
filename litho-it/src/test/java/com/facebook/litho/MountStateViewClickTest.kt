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
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutWithInnerClickableChildTester
import com.facebook.litho.widget.SimpleLayoutSpecWithClickHandlersTester
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateViewClickTest {

  private lateinit var context: ComponentContext
  private lateinit var componentTree: ComponentTree

  @JvmField @Rule val lithoViewRule = LithoTestRule()

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    componentTree =
        ComponentTree.create(context)
            .componentsConfiguration(
                context.lithoConfiguration.componentsConfig.copy(
                    incrementalMountEnabled = false, visibilityProcessingEnabled = false))
            .build()
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun testInnerComponentHostClickable() {
    val lithoView =
        lithoViewRule
            .render(componentTree = componentTree) {
              LayoutWithInnerClickableChildTester.create(context)
                  .shouldSetClickHandler(true)
                  .build()
            }
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat(lithoView.isClickable).isFalse
    assertThat(lithoView.isLongClickable).isFalse
    val innerHost = lithoView.getChildAt(0) as ComponentHost
    assertThat(innerHost.isClickable).isTrue
    assertThat(innerHost.isLongClickable).isFalse
  }

  @Test
  fun testInnerComponentHostClickableWithLongClickHandler() {
    val lithoView =
        lithoViewRule
            .render(componentTree = componentTree) {
              LayoutWithInnerClickableChildTester.create(context)
                  .shouldSetLongClickHandler(true)
                  .build()
            }
            .lithoView
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat(lithoView.isClickable).isFalse
    assertThat(lithoView.isLongClickable).isFalse
    val innerHost = lithoView.getChildAt(0) as ComponentHost
    assertThat(innerHost.isClickable).isFalse
    assertThat(innerHost.isLongClickable).isTrue
  }

  @Test
  fun testRootHostClickable() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .clickHandler(
                          EventHandlerTestHelper.create(c, 1) as? EventHandler<ClickEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat(lithoView.getChildAt(0).isClickable).isTrue
  }

  @Test
  fun testRootHostClickableWithLongClickHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .longClickHandler(
                          EventHandlerTestHelper.create(c, 1) as? EventHandler<LongClickEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat(lithoView.getChildAt(0).isLongClickable).isTrue
  }

  @Test
  fun testRootHostClickableUnmount() {
    val testLithoView =
        lithoViewRule.render(componentTree = componentTree) {
          SimpleLayoutSpecWithClickHandlersTester.create(context).build()
        }
    val rootHost = testLithoView.lithoView.getChildAt(0)
    assertThat(rootHost.isClickable).isTrue
    assertThat(rootHost.isLongClickable).isTrue
    testLithoView.lithoView.unmountAllItems()
    assertThat(rootHost.isClickable).isFalse
    assertThat(rootHost.isLongClickable).isFalse
  }
}
