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

import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.ComponentWithTreeProp
import com.facebook.litho.widget.TextDrawable
import com.facebook.litho.widget.treeprops.SimpleTreeProp
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoViewRuleTest {

  @JvmField @Rule val lithoViewRule = LithoViewRule()

  @Test
  fun onLithoViewRuleWithTreeProp_shouldPropagateTreeProp() {
    val component = ComponentWithTreeProp.create(lithoViewRule.context).build()
    val testLithoView =
        lithoViewRule.setTreeProp(SimpleTreeProp::class.java, SimpleTreeProp("test")).render {
          component
        }

    val item = testLithoView.lithoView.getMountItemAt(0).content
    assertThat(item).isInstanceOf(TextDrawable::class.java)
    assertThat((item as TextDrawable).text).isEqualTo("test")
  }

  @Test(expected = RuntimeException::class)
  fun onLithoViewRuleWithoutTreeProp_shouldThrowException() {
    val component = ComponentWithTreeProp.create(lithoViewRule.context).build()
    lithoViewRule.createTestLithoView().attachToWindow().setRoot(component).measure().layout()
  }

  @Test
  fun onLithoViewRuleExceptionOnBackgroundThread_shouldPropagateExceptionImmediately() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val randomState = useState { false }
        if (randomState.value) {
          throw Exception("Hi There!")
        }
        return Row(style = Style.width(100.px).height(100.px)) {
          child(Text(text = "some_other_text", style = Style.onClick { randomState.update(true) }))
        }
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    val thrown: Throwable =
        Assertions.catchThrowable {
          lithoViewRule.act(testLithoView) { clickOnText("some_other_text") }
        }

    assertThat(thrown).isInstanceOf(RuntimeException::class.java)
    assertThat((thrown.stackTraceToString()).contains("Timed out!")).isFalse
    assertThat(thrown).hasStackTraceContaining("Hi There!")
  }
}
