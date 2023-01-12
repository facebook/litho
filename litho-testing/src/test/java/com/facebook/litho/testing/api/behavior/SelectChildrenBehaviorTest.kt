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
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.api.LithoRule
import com.facebook.litho.testing.api.hasTestKey
import com.facebook.litho.testing.api.hasText
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class SelectChildrenBehaviorTest {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `select children will select the direct children`() {
    rule
        .render { TopLevelComponent() }
        .selectNode(hasTestKey("mid-level-items-container"))
        .selectChildren()
        .assertCount(2)
  }

  @Test
  fun `select children will select nothing when there is no children`() {
    rule
        .render { TopLevelComponent() }
        .selectNode(hasText("bottom-level"))
        .selectChildren()
        .assertCount(0)
  }

  private class TopLevelComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column {
        child(Text("top-level"))

        child(
            Row(style = Style.testKey("mid-level-items-container")) {
              child(MidLevelComponent())
              child(MidLevel2Component())
            })
      }
    }
  }

  private class MidLevel2Component : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("mid-level-2")
    }
  }

  private class MidLevelComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column {
        child(Text("mid-level"))
        child(BottomLevelComponent())
      }
    }
  }

  private class BottomLevelComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("bottom-level")
    }
  }
}
