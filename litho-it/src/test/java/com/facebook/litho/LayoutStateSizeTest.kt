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
class LayoutStateSizeTest {

  private lateinit var layoutState: LayoutState
  private lateinit var component: Component
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    component = TestLayoutComponent.create(context).build()
    Whitebox.setInternalState(component, "mId", COMPONENT_ID)
    val result =
        ResolveTreeFuture.resolve(context, component, TreeState(), 1, 1, null, null, null, null)
    layoutState =
        LayoutTreeFuture.layout(
            result,
            makeSizeSpec(WIDTH, SizeSpec.EXACTLY),
            makeSizeSpec(HEIGHT, SizeSpec.EXACTLY),
            1,
            1,
            false,
            null,
            null,
            null,
            null)
  }

  @Test
  fun testCompatibleSize() {
    assertThat(layoutState.isCompatibleSize(WIDTH, HEIGHT)).isTrue
  }

  @Test
  fun testIncompatibleWidthSpec() {
    assertThat(layoutState.isCompatibleSize(WIDTH + 1_000, HEIGHT)).isFalse
  }

  @Test
  fun testIncompatibleHeightSpec() {
    assertThat(layoutState.isCompatibleSize(WIDTH, HEIGHT + 1_000)).isFalse
  }

  companion object {
    private const val COMPONENT_ID = 37
    private const val WIDTH = 49
    private const val HEIGHT = 51
  }
}
