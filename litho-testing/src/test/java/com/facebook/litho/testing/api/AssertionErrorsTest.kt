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
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class AssertionErrorsTest : RunWithDebugInfoTest() {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `TestNodeSelection assertion with multiple test node result`() {
    Assertions.assertThatThrownBy {
          rule.render { CollectionComponent() }.selectNode(hasType<Text>()).assertExists()
        }
        .hasMessage(
            """
            Failed: assertExists
            Reason: Expected exactly 1 node(s), but found 6
            Node(s) found:
            -Text
               isEnabled = false
               text = Hello world
            -Text
               testKey = item-#0
               isEnabled = false
               text = Item #0
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
            Selector used: is a component of type com.facebook.litho.widget.Text
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeSelection assertion with no test node result`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNode(hasType<Image>())
              .assert(hasAncestor(hasType<CollectionComponent>()))
        }
        .hasMessage(
            """
            Failed assertion: has ancestor that is a component of type com.facebook.litho.testing.api.AssertionErrorsTest.CollectionComponent
            Reason: Could not find any matching node for selection
            Selector used: is a component of type com.facebook.litho.widget.Image
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeSelection generic assertion`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNode(hasType<Row>())
              .assert(hasChild(hasType<Text>()) and hasTestKey("invalid"))
        }
        .hasMessage(
            """
            Failed assertion: has child that is a component of type com.facebook.litho.widget.Text and has test key "invalid"
            -Row(children=1)
               isEnabled = false
             |-Text
                isEnabled = false
                text = Hello world
            Selector used: is a component of type com.facebook.litho.Row
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeCollectionSelection assertExist on empty result`() {
    Assertions.assertThatThrownBy {
          rule.render { CollectionComponent() }.selectNodes(hasTestKey("no-result")).assertExists()
        }
        .hasMessage(
            """
            Failed: assertExists
            Reason: Could not find any matching node for selection
            Selector used: has test key "no-result"
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeSelection assertion with invalid index selection`() {
    // Typically, if a node is not found, we simply show a missing node error
    // However, in the case where the node is not available specifically because it was
    // a selection on a bad index, we should use this information as it'll be way more
    // helpful than simply failing with the typical missing node error
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasType<Text>())
              .selectAtIndex(10)
              .assertExists()
        }
        .hasMessage(
            """
            Failed: assertExists
            Failed selection: index is out of bounds
            Reason: Requested a node at index 10, but only 6 nodes are available
            Node(s) found:
            -Text
               isEnabled = false
               text = Hello world
            -Text
               testKey = item-#0
               isEnabled = false
               text = Item #0
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
            Selector used: is a component of type com.facebook.litho.widget.Text => node at index 10
            
            """
                .trimIndent())
  }

  @Test
  fun `secondary selection with original selection error - v1`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNode(hasType<Image>()) // <- culprit: invalid matcher
              .selectChildren()
              .assertExists()
        }
        .hasMessage(
            """
            Failed selection
            Reason: Expected exactly 1 node(s), but found 0
            Selector used: is a component of type com.facebook.litho.widget.Image
            
            """
                .trimIndent())
  }

  @Test
  fun `secondary selection with original selection error - v2`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasType<Column>())
              .selectAtIndex(2) // <- culprit: invalid index
              .selectChildren()
              .assertExists()
        }
        .hasMessage(
            """
            Failed selection: index is out of bounds
            Reason: Requested a node at index 2, but only 1 node is available
            Node(s) found:
            -Column(children=2)
               isEnabled = false
            Selector used: is a component of type com.facebook.litho.Column => node at index 2
            
            """
                .trimIndent())
  }

  @Test
  fun `select first on an empty collection`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasType<Image>())
              .selectFirst()
              .assertExists()
        }
        .hasMessage(
            """
            Failed: assertExists
            Failed selection
            Reason: Could not find any matching node for selection
            Selector used: is a component of type com.facebook.litho.widget.Image => first node
            
            """
                .trimIndent())
  }

  @Test
  fun `select one with more than one result`() {
    Assertions.assertThatThrownBy {
          // A more compact version of this without the secondary select is simply:
          // selectNode(hasTextContaining("Item"))
          rule
              .render { CollectionComponent() }
              .selectNodes(hasTextContaining("Item"))
              .select(TestNodeMatcher("single node") { true })
              .assertExists()
        }
        .hasMessage(
            """
            Failed: assertExists
            Reason: Expected exactly 1 node(s), but found 5
            Node(s) found:
            -Text
               testKey = item-#0
               isEnabled = false
               text = Item #0
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
            Selector used: has text containing "Item" => single node
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeCollectionSelection assertAll`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasTextContaining("Item"))
              .assertAll(hasTestKey("item-#0"))
        }
        .hasMessage(
            """
            Failed: assertAll(has test key "item-#0")
            Reason: The following nodes do not match the expected condition:
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
            Selector used: has text containing "Item"
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeCollectionSelection assertAny`() {
    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasTextContaining("Item"))
              .assertAny(hasType<Image>())
        }
        .hasMessage(
            """
            Failed: assertAny(is a component of type com.facebook.litho.widget.Image)
            Reason: None of the selected nodes match the expected condition
            Node(s) found:
            -Text
               testKey = item-#0
               isEnabled = false
               text = Item #0
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
            Selector used: has text containing "Item"
            
            """
                .trimIndent())
  }

  @Test
  fun `TestNodeCollectionSelection assertAny on empty collection`() {

    Assertions.assertThatThrownBy {
          rule
              .render { CollectionComponent() }
              .selectNodes(hasType<Image>())
              .assertAny(hasTextContaining("Item"))
        }
        .hasMessage(
            """
            Failed: assertAny(has text containing "Item")
            Reason: Could not find any matching node for selection
            Selector used: is a component of type com.facebook.litho.widget.Image
            
            """
                .trimIndent())
  }

  private class CollectionComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.flex(grow = 1f)) {
        child(Row { child(Text("Hello world")) })

        child(
            LazyList(style = Style.flex(grow = 1f)) {
              children(items = 0 until 5, id = { it }) {
                Text("Item #$it", Style.testKey("item-#$it"))
              }
            })
      }
    }
  }
}
