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
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.api.LithoRule
import com.facebook.litho.testing.api.hasText
import com.facebook.litho.testing.api.hasTextContaining
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class HasTextBehaviorTest {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `hasText will succeed when there is a component with the exact given text`() {
    rule
        .render { DummyComponent("Hello world!") }
        .selectNode(hasText("Hello world!"))
        .assertExists()
  }

  @Test
  fun `hasTextContaining will succeed when there is a component whose text contains the given text`() {
    rule
        .render { DummyComponent("Hello world!") }
        .selectNode(hasTextContaining("wor"))
        .assertExists()
  }

  @Test
  fun `hasText will throw assertion when there is no component with the exact given text`() {
    assertThatThrownBy {
          rule
              .render { DummyComponent("Hello world!") }
              .selectNode(hasText("Hello world!!"))
              .assertExists()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `hasTextContaining will throw assertion when there is no component containing given text`() {
    assertThatThrownBy {
          rule
              .render { DummyComponent("Hello world!") }
              .selectNode(hasTextContaining("word"))
              .assertExists()
        }
        .isInstanceOf(AssertionError::class.java)
  }

  private class DummyComponent(val text: String) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text(text)
    }
  }
}
