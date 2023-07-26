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

package com.facebook.litho.testing.api

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * The goal of this set of tests is just to be a health check that basic scenarios of the nodes
 * resolution and verification are working.
 */
@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class NodeResolutionWithAssertionsTest : RunWithDebugInfoTest() {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `one level component hierarchy`() {
    rule.render { SimpleComponent() }.selectNode(hasType<Text>()).assertExists()
  }

  @Test
  fun `multi-level component hierarchy`() {
    rule.render { ParentComponent() }

    rule.selectNode(hasType<Text>()).assertExists()
    rule.selectNode(hasType<ParentComponent>()).assertExists()
    rule.selectNode(hasType<ChildComponent>()).assertExists()
    rule.selectNode(hasType<SimpleComponent>()).assertDoesNotExist()
  }

  @Test
  fun `one level component with collection hierarchy`() {
    rule.render { CollectionComponent() }

    rule.selectNode(hasTestKey("item-#0")).assertExists()
    rule.selectNode(hasTestKey("item-#15")).assertExists()
    rule.selectNode(hasTestKey("item-#30")).assertExists()
  }

  @Test
  fun `root node selection`() {
    rule
        .render { SimpleComponent() }
        .selectNode(isRoot())
        .assertExists()
        .assert(hasType<SimpleComponent>())

    rule
        .render { ParentComponent() }
        .selectNode(isRoot())
        .assertExists()
        .assert(hasType<ParentComponent>())
    rule
        .render { CollectionComponent() }
        .selectNode(isRoot())
        .assertExists()
        .assert(hasType<CollectionComponent>())
  }

  private class SimpleComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Text("Hello")
    }
  }

  private class ParentComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.padding(20.dp)) { child(ChildComponent()) }
    }
  }

  private class ChildComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Text("Hello")
    }
  }

  class CollectionComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.flex(grow = 1f)) {
        child(Row { child(Text("Hello world")) })

        child(
            LazyList(style = Style.flex(grow = 1f)) {
              children(items = (0..30), id = { it }) {
                Text("Item #$it", Style.testKey("item-#$it"))
              }
            })
      }
    }
  }
}
