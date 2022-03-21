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

package com.facebook.litho.examples

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Examples of LithoViewRuleList usage */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class LithoViewRuleListExampleTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `test child is present in LazyList`() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return LazyList {
          for (i in 1..10) {
            child(id = i, component = Text("child$i"))
          }
        }
      }
    }

    val testView =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { TestComponent() }

    assertThat(testView).isNotNull()

    val listComponent = testView.findCollectionComponent()
    assertThat(listComponent).isNotNull()

    assertThat(listComponent?.itemCount ?: return).isEqualTo(10)
    assertThat(listComponent.getItemAtIndex(0)).hasFieldOrPropertyWithValue("text", "child1")

    assertThat(listComponent.getItemAtIndex(listComponent.lastVisibleIndex))
        .hasFieldOrPropertyWithValue("text", "child3")

    assertThat(
            listComponent.items.slice(
                listComponent.firstVisibleIndex..listComponent.lastVisibleIndex))
        .hasSize(3)

    val component = listComponent.findFirstItem(Text::class)
    assertThat(component).hasFieldOrPropertyWithValue("text", "child1")
  }

  @Test
  fun `test child is present in LazyList with multiple component types`() {

    data class Item(val id: Int, val name: String, val quantity: Int)

    val items =
        listOf(
            Item(1, "Flour", 2),
            Item(2, "Cinnamon", 3),
            Item(3, "Salt", 100),
            Item(4, "Butter", 1),
        )

    class ItemComponent(val model: Item) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Text("${model.name}: ${model.quantity}")
      }
    }

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return LazyList() {
          child(Text("Item List"))
          for (item in items) {
            child(id = item.id, component = ItemComponent(item))
          }
        }
      }
    }

    val testView =
        lithoViewRule.render(widthPx = exactly(100), heightPx = exactly(100)) { TestComponent() }

    assertThat(testView.findViewWithTextOrNull("Flour: 2")).isNotNull()
    assertThat(testView.findAllComponents(Text::class))
        // this should return empty for now.
        .isEmpty()

    val listComponent = testView.findCollectionComponent()
    assertThat(listComponent).isNotNull()

    val component = listComponent?.findFirstItem(ItemComponent::class) ?: return
    assertThat(component).hasFieldOrPropertyWithValue("model", items[0])

    val foundComponents = listComponent.findItems(ItemComponent::class)
    assertThat(foundComponents).hasSize(4)
  }
}
