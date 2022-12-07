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

package com.facebook.litho.testing.api.behavior

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.api.LithoRule
import com.facebook.litho.testing.api.hasText
import com.facebook.litho.testing.api.performClick
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class PerformClickTest {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `performs click if the test node selection is valid`() {
    rule.render { DummyComponent() }
    rule.selectNode(hasText("Number of clicks: 0")).assertExists()
    rule.selectNode(hasText("Increment")).performClick()
    rule.selectNode(hasText("Number of clicks: 1")).assertExists()
  }

  @Test
  fun `performing click in an unexisting node will throw an assertion`() {
    assertThatThrownBy {
          rule.render { DummyComponent() }.selectNode(hasText("I don't exist")).performClick()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `performing click in an existing node without a click handler will throw an assertion`() {
    assertThatThrownBy {
          rule.render { DummyComponent() }.selectNode(hasText("Number of clicks: 0")).performClick()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  private class DummyComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      val numClicks = useState { 0 }

      return Column {
        child(Text("Number of clicks: ${numClicks.value}"))
        child(Text("Increment", Style.onClick { numClicks.update { it + 1 } }))
        child(Text("* 10", Style.onClick { numClicks.update { it * 10 } }))
      }
    }
  }
}
