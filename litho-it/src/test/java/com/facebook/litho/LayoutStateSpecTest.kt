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
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutStateSpecTest {

  private var widthSpec = 0
  private var heightSpec = 0
  private lateinit var layoutState: LayoutState
  private lateinit var component: Component
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    widthSpec = makeSizeSpec(39, SizeSpec.EXACTLY)
    heightSpec = makeSizeSpec(41, SizeSpec.EXACTLY)
    component = TestLayoutComponent.create(context).build()
    Whitebox.setInternalState(component, "mId", COMPONENT_ID)
    val result =
        ResolveTreeFuture.resolve(context, component, TreeState(), 1, 1, null, null, null, null)
    layoutState =
        LayoutTreeFuture.layout(result, widthSpec, heightSpec, 1, 1, false, null, null, null, null)
  }

  @Test
  fun testCompatibleInputAndSpec() {
    assertThat(layoutState.isCompatibleComponentAndSpec(COMPONENT_ID, widthSpec, heightSpec)).isTrue
  }

  @Test
  fun testIncompatibleInput() {
    assertThat(
            layoutState.isCompatibleComponentAndSpec(COMPONENT_ID + 1_000, widthSpec, heightSpec))
        .isFalse
  }

  @Test
  fun testIncompatibleWidthSpec() {
    assertThat(
            layoutState.isCompatibleComponentAndSpec(COMPONENT_ID, widthSpec + 1_000, heightSpec))
        .isFalse
  }

  @Test
  fun testIncompatibleHeightSpec() {
    assertThat(
            layoutState.isCompatibleComponentAndSpec(COMPONENT_ID, widthSpec, heightSpec + 1_000))
        .isFalse
  }

  companion object {
    private const val COMPONENT_ID = 37
  }
}
