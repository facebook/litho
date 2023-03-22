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

import android.view.View
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewId
import com.facebook.litho.view.viewTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentViewIdTest {

  @get:Rule val rule: LithoViewRule = LithoViewRule()

  @Test
  fun `should set a view id on components correctly`() {
    val lithoView = rule.render { TestComponent() }
    rule.idle()

    val columnView = lithoView.lithoView.findViewById<View>(TestComponent.COLUMN_ID)
    assertThat(columnView).isNotNull
    assertThat(columnView).isInstanceOf(ComponentHost::class.java)
    assertThat((columnView as ComponentHost).childCount).isEqualTo(1)

    val helloView = lithoView.lithoView.findViewById<View>(TestComponent.HELLO_ID)
    assertThat(helloView).isNotNull
    assertThat(helloView).isInstanceOf(ComponentHost::class.java)
    assertThat((helloView as ComponentHost).childCount).isEqualTo(0)
    assertThat(helloView.textContentText).isEqualTo(listOf("Hello"))
  }

  private class TestComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Column(style = Style.viewTag("my-tag").viewId(COLUMN_ID)) {
        child(Text("Hello", style = Style.viewId(HELLO_ID)))
        child(Text("World"))
      }
    }

    companion object {
      val HELLO_ID: Int = 500
      val COLUMN_ID: Int = 501
    }
  }
}
