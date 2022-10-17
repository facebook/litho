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

import android.graphics.Rect
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.ComponentContainerWithSize
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class TestItemTest {

  private lateinit var testItem: TestItem

  @get:Rule val lithoViewRule = LithoViewRule()

  @Before
  fun setup() {
    testItem = TestItem()
  }

  @Test
  fun testPositionAndSizeSet() {
    testItem.setBounds(0, 1, 3, 4)
    assertThat(testItem.bounds.left).isEqualTo(0)
    assertThat(testItem.bounds.top).isEqualTo(1)
    assertThat(testItem.bounds.right).isEqualTo(3)
    assertThat(testItem.bounds.bottom).isEqualTo(4)
  }

  @Test
  fun testRectBoundsSet() {
    val bounds = Rect(0, 1, 3, 4)
    testItem.bounds = bounds
    assertThat(testItem.bounds.left).isEqualTo(0)
    assertThat(testItem.bounds.top).isEqualTo(1)
    assertThat(testItem.bounds.right).isEqualTo(3)
    assertThat(testItem.bounds.bottom).isEqualTo(4)
  }

  @Test
  fun whenRootComponentUsesSizes_thenTestItemShouldBeFound() {
    val isEnabled = ComponentsConfiguration.isEndToEndTestRun
    ComponentsConfiguration.isEndToEndTestRun = true
    val view =
        lithoViewRule
            .render {
              ComponentContainerWithSize.create(context).component(ParentComponent()).build()
            }
            .lithoView

    assertThat(LithoViewTestHelper.findTestItem(view, "test-key")).isNotNull
    ComponentsConfiguration.isEndToEndTestRun = isEnabled
  }

  class ParentComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return TextHolderComponent()
    }
  }

  class TextHolderComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Text(text = "hello", style = Style.testKey("test-key"))
    }
  }
}
