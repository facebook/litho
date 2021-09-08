// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.examples

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Examples of LithoViewRule usage */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class LithoViewRuleExampleTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `verify InnerComponent appears when TestComponent is clicked`() {
    class InnerComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.height(100.dp).width(100.dp)) { child(Text(text = "some_text")) }
      }
    }

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val showChild = useState { false }

        return Row(style = Style.viewTag("test_view").onClick { showChild.update(true) }) {
          child(Text(text = "some_text_2"))
          if (showChild.value) {
            child(InnerComponent())
          }
        }
      }
    }

    val lithoView = lithoViewRule.render { TestComponent() }
    LithoViewAssert.assertThat(lithoView).isNotNull
    LithoViewAssert.assertThat(lithoView).hasVisibleText("some_text_2")
    /** can use all of the assertions from: [LithoViewAssert] class */
    val nullComponent = lithoViewRule.findComponent(InnerComponent::class.java)
    Assertions.assertThat(nullComponent).isNull()

    lithoViewRule.act { clickOnTag("test_view") }

    val component = lithoViewRule.findComponent(InnerComponent::class.java)
    Assertions.assertThat(component).isNotNull
  }
}
