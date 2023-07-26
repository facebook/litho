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
import com.facebook.litho.view.onClick
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DebugLogTest : RunWithDebugInfoTest() {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `simple component hierarchy`() {
    val actual =
        rule.render { SimpleComponent() }.selectNode(hasType<Text>()).printToString().trim()
    val expected =
        """
        -Text
           isEnabled = false
           text = Hello
           Actions = [OnClick]
        """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `multi-level component hierarchy with max depth`() {
    val actual =
        rule.render { StyledComponent() }.selectNode(isRoot()).printToString(maxDepth = 1).trim()
    val expected =
        """
        -StyledComponent(children=1)
           isEnabled = false
         |-Column(children=1)
            isEnabled = false
          """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `multi-level component hierarchy with partial tree`() {
    val actual =
        rule.render { CollectionComponent() }.selectNode(hasType<Row>()).printToString().trim()
    val expected =
        """
        -Row(children=2)
           isEnabled = false
         |-Text
         |  isEnabled = false
         |  text = Hello world
         |-Text
            isEnabled = false
            text = Info
            Actions = [OnClick]
        """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `multi-level component hierarchy`() {
    val actual = rule.render { StyledComponent() }.selectNode(isRoot()).printToString().trim()
    val expected =
        """
        -StyledComponent(children=1)
           isEnabled = false
         |-Column(children=1)
            isEnabled = false
          |-Text
             isEnabled = false
             text = Hello world
        """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `component with collection hierarchy`() {
    val actual = rule.render { CollectionComponent() }.selectNode(isRoot()).printToString().trim()
    val expected =
        """
        -CollectionComponent(children=1)
           isEnabled = false
         |-Column(children=2)
            isEnabled = false
          |-Row(children=2)
          |  isEnabled = false
           |-Text
           |  isEnabled = false
           |  text = Hello world
           |-Text
              isEnabled = false
              text = Info
              Actions = [OnClick]
          |-LazyCollection(children=1)
             isEnabled = false
           |-CollectionRecycler(children=1)
              isEnabled = false
            |-Recycler(children=10)
               isEnabled = false
             |-Text
             |  testKey = item-#1
             |  isEnabled = false
             |  text = Item #1
             |-Text
             |  testKey = item-#2
             |  isEnabled = false
             |  text = Item #2
             |-Text
             |  testKey = item-#3
             |  isEnabled = false
             |  text = Item #3
             |-Text
             |  testKey = item-#4
             |  isEnabled = false
             |  text = Item #4
             |-Text
             |  testKey = item-#5
             |  isEnabled = false
             |  text = Item #5
             |-Text
             |  testKey = item-#6
             |  isEnabled = false
             |  text = Item #6
             |-Text
             |  testKey = item-#7
             |  isEnabled = false
             |  text = Item #7
             |-Text
             |  testKey = item-#8
             |  isEnabled = false
             |  text = Item #8
             |-Text
             |  testKey = item-#9
             |  isEnabled = false
             |  text = Item #9
             |-Text
                testKey = item-#10
                isEnabled = false
                text = Item #10
          """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `multiple matching nodes hierarchy`() {
    val actual =
        rule.render { CollectionComponent() }.selectNodes(hasType<Text>()).printToString().trim()
    val expected =
        """
        Found 12 matching node(s)
        -Text
           isEnabled = false
           text = Hello world
        -Text
           isEnabled = false
           text = Info
           Actions = [OnClick]
        -Text
           testKey = item-#1
           isEnabled = false
           text = Item #1
        -Text
           testKey = item-#2
           isEnabled = false
           text = Item #2
        -Text
           testKey = item-#3
           isEnabled = false
           text = Item #3
        -Text
           testKey = item-#4
           isEnabled = false
           text = Item #4
        -Text
           testKey = item-#5
           isEnabled = false
           text = Item #5
        -Text
           testKey = item-#6
           isEnabled = false
           text = Item #6
        -Text
           testKey = item-#7
           isEnabled = false
           text = Item #7
        -Text
           testKey = item-#8
           isEnabled = false
           text = Item #8
        -Text
           testKey = item-#9
           isEnabled = false
           text = Item #9
        -Text
           testKey = item-#10
           isEnabled = false
           text = Item #10
          """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `multiple matching nodes with multi-level hierarchy`() {
    val actual =
        rule
            .render { CollectionComponent() }
            .selectNodes(hasChild(hasType<Text>()))
            .printToString(maxDepth = Int.MAX_VALUE)
            .trim()
    val expected =
        """
        Found 2 matching node(s)
        -Row(children=2)
           isEnabled = false
         |-Text
         |  isEnabled = false
         |  text = Hello world
         |-Text
            isEnabled = false
            text = Info
            Actions = [OnClick]
        -Recycler(children=10)
           isEnabled = false
         |-Text
         |  testKey = item-#1
         |  isEnabled = false
         |  text = Item #1
         |-Text
         |  testKey = item-#2
         |  isEnabled = false
         |  text = Item #2
         |-Text
         |  testKey = item-#3
         |  isEnabled = false
         |  text = Item #3
         |-Text
         |  testKey = item-#4
         |  isEnabled = false
         |  text = Item #4
         |-Text
         |  testKey = item-#5
         |  isEnabled = false
         |  text = Item #5
         |-Text
         |  testKey = item-#6
         |  isEnabled = false
         |  text = Item #6
         |-Text
         |  testKey = item-#7
         |  isEnabled = false
         |  text = Item #7
         |-Text
         |  testKey = item-#8
         |  isEnabled = false
         |  text = Item #8
         |-Text
         |  testKey = item-#9
         |  isEnabled = false
         |  text = Item #9
         |-Text
            testKey = item-#10
            isEnabled = false
            text = Item #10
          """
            .trimIndent()
    Assertions.assertThat(actual).isEqualTo(expected)
  }

  private class SimpleComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Text("Hello", style = Style.onClick { /* no-op */})
    }
  }

  private class StyledComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.padding(20.dp)) { child(Text("Hello world")) }
    }
  }

  private class CollectionComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.flex(grow = 1f)) {
        child(
            Row {
              child(Text("Hello world"))
              child(Text("Info", style = Style.onClick { /* no-op */}))
            })

        child(
            LazyList(style = Style.flex(grow = 1f)) {
              children(items = (1..10), id = { it }) {
                Text("Item #$it", Style.testKey("item-#$it"))
              }
            })
      }
    }
  }
}
