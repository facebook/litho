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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.api.LithoRule
import com.facebook.litho.testing.api.hasType
import com.facebook.litho.testing.api.isEnabled
import com.facebook.litho.testing.api.isNotEnabled
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.enabled
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class IsEnabledBehaviorTest {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `selecting via isEnabled will succeed when there is an enabled component`() {
    rule.render { DummyComponent(isEnabled = true) }

    rule.selectNode(hasType<Text>() and isEnabled()).assertExists()
  }

  @Test
  fun `selecting via isEnabled will throw assertion when there is no enabled component`() {
    assertThatThrownBy {
          rule
              .render { DummyComponent(isEnabled = false) }
              .selectNode(hasType<Text>() and isEnabled())
              .assertExists()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `selecting via isNotEnabled will succeed when there is a disabled component`() {
    rule.render { DummyComponent(isEnabled = false) }

    rule.selectNode(hasType<Text>() and isNotEnabled()).assertExists()
  }

  @Test
  fun `selecting via isNotEnabled will throw assertion when there is no disabled component`() {
    assertThatThrownBy {
          rule
              .render { DummyComponent(isEnabled = true) }
              .selectNode(hasType<Text>() and isNotEnabled())
              .assertExists()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  private class DummyComponent(private val isEnabled: Boolean) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("Not enabled", Style.enabled(isEnabled))
    }
  }
}
