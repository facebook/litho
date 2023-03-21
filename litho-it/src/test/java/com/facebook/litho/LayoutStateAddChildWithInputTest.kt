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
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutStateAddChildWithInputTest {

  private lateinit var context: ComponentContext

  @JvmField @Rule var legacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    context.setRenderStateContextForTests()
  }

  @Test
  fun testNewEmptyLayout() {
    val component =
        Column.create(context)
            .child(TestLayoutComponent.create(context))
            .child(TestLayoutComponent.create(context))
            .build()
    val node = LegacyLithoViewRule.getRootLayout(legacyLithoViewRule, component)?.node
    assertThat(node?.childCount).isEqualTo(2)
    assertThat(node?.getChildAt(0)?.childCount).isEqualTo(0)
    assertThat(node?.getChildAt(1)?.childCount).isEqualTo(0)
  }
}
