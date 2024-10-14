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
import com.facebook.litho.GetSimpleNameTest.TestWrapperComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class GetSimpleNameTest {

  private class TestWrapperComponent(private val delegate: Component) :
      SpecGeneratedComponent("TestWrapper") {
    override fun getSimpleNameDelegate(): Component = delegate
  }

  private lateinit var context: ComponentContext

  @Before
  fun setUp() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testGetSimpleName() {
    val testComponent = SimpleMountSpecTester.create(context).build()
    assertThat(testComponent.simpleName).isEqualTo("SimpleMountSpecTester")
  }

  @Test
  fun testGetSimpleNameWithOneWrapper() {
    val inner = SimpleMountSpecTester.create(context).build()
    val wrapper = TestWrapperComponent(inner)
    assertThat(wrapper.simpleName).isEqualTo("TestWrapper(SimpleMountSpecTester)")
  }

  @Test
  fun testGetSimpleNameWithMultipleWrapper() {
    val inner = SimpleMountSpecTester.create(context).build()
    val wrapper = TestWrapperComponent(inner)
    val wrapper2 = TestWrapperComponent(wrapper)
    val wrapper3 = TestWrapperComponent(wrapper2)
    assertThat(wrapper3.simpleName).isEqualTo("TestWrapper(SimpleMountSpecTester)")
  }
}
