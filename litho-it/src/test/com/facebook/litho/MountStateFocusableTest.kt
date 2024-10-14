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
import com.facebook.litho.testing.TestViewComponent
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
class MountStateFocusableTest {

  private lateinit var context: ComponentContext
  private var focusableDefault = false

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    context =
        ComponentContext(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>())
    focusableDefault = ComponentHost(context).isFocusable
  }

  @Test
  fun testInnerComponentHostFocusable() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component? =
                  Column.create(c)
                      .child(Column.create(c).focusable(true).child(TestViewComponent.create(c)))
                      .build()
            })
    assertThat(lithoView.childCount).isEqualTo(1)
    // TODO(T16959291): The default varies between internal and external test runs, which indicates
    // that our Robolectric setup is not actually identical. Until we can figure out why,
    // we will compare against the dynamic default instead of asserting false.
    assertThat(lithoView.isFocusable).isEqualTo(focusableDefault)
    val innerHost = lithoView.getChildAt(0) as ComponentHost
    assertThat(innerHost.isFocusable).isTrue
  }

  @Test
  fun testRootHostFocusable() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component? =
                  Column.create(c).focusable(true).child(SimpleMountSpecTester.create(c)).build()
            })
    assertThat(lithoView.childCount).isEqualTo(1)
    val innerHost = lithoView.getChildAt(0)
    assertThat(innerHost.isFocusable).isTrue
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
