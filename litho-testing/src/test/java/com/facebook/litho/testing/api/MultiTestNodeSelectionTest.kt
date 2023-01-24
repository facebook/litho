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
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MultiTestNodeSelectionTest : RunWithDebugInfoTest() {

  @get:Rule val rule: LithoRule = LithoRule()

  @Test
  fun `should be able to assert the existence of multiple test nodes that respect the selection`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasType<Text>()).assertExists()
  }

  @Test
  fun `should be able to assert the non existence of test nodes that respect the selection`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasType<Image>()).assertDoesNotExist()
  }

  @Test
  fun `should throw an error when there is not a single test node respecting the selection`() {
    rule.render { CollectionComponent() }

    assertThatThrownBy { rule.selectNodes(hasType<Image>()).assertExists() }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `should throw an error when there is at least one test node contradicts the selection`() {
    rule.render { CollectionComponent() }

    assertThatThrownBy { rule.selectNodes(hasType<Text>()).assertDoesNotExist() }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `should be able to select an item of a collection`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasType<Text>()).selectAtIndex(2).assert(hasText("Item #1"))
  }

  @Test
  fun `should be able to assert correct size of component collection`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasTextContaining("Item")).assertCount(30)
  }

  @Test
  fun `should throw an error if count assertion fails`() {
    rule.render { CollectionComponent() }

    assertThatThrownBy { rule.selectNodes(hasTextContaining("Item")).assertCount(25) }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `should assert that all selected nodes match a condition`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasTextContaining("Item")).assertAll(hasType<Text>())
  }

  @Test
  fun `should throw an error if not all of the selected nodes match a condition`() {
    rule.render { CollectionComponent() }

    assertThatThrownBy {
          rule.selectNodes(hasTextContaining("Item")).assertAll(hasTestKey("item-#1"))
        }
        .isInstanceOf(AssertionError::class.java)
        .hasMessageStartingWith("Failed: assertAll")
  }

  @Test
  fun `should assert that any of the selected nodes match a condition`() {
    rule.render { CollectionComponent() }

    rule.selectNodes(hasType<Text>()).assertAny(hasText("Hello world"))
  }

  @Test
  fun `should throw an error if none of the selected nodes match a condition`() {
    rule.render { CollectionComponent() }

    assertThatThrownBy {
          rule.selectNodes(hasType<Text>()).assertAny(hasTextContaining("Goodbye world"))
        }
        .isInstanceOf(AssertionError::class.java)
        .hasMessageStartingWith("Failed: assertAny")
  }

  private class CollectionComponent : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column(style = Style.flex(grow = 1f)) {
        child(Row { child(Text("Hello world")) })

        child(
            LazyList(style = Style.flex(grow = 1f)) {
              children(items = 0 until 30, id = { it }) {
                Text("Item #$it", Style.testKey("item-#$it"))
              }
            })
      }
    }
  }
}
