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

package com.facebook.samples.litho.kotlin.collection

import android.os.Bundle
import android.util.Log
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlinStyle
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.setContent
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.NavigatableDemoActivity

class CollectionActivity : NavigatableDemoActivity() {

  class CollectionComponent : KComponent() {

    override fun ComponentScope.render(): Component? {
      val counter = useState { 0 }
      val inOrder = useState { true }
      return Column(style = Style.padding(16.dp)) {
        child(staticItemsExample())
        child(dynamicItemsExample(counter, inOrder))
      }
    }

    fun ComponentScope.staticItemsExample(): Component {
      val friends = "Ross Rachel Joey Phoebe Monica Chandler".split(" ")
      return Collection(disablePTR = true, style = Style.flex(grow = 1f), onDataBound = null) {
        item(Text(text = "Header"))
        items(friends.map { Text(text = it) })
        item(Text(text = "Footer"))
      }
    }

    fun ComponentScope.dynamicItemsExample(
        counter: State<Int>,
        inOrder: State<Boolean>
    ): Component {

      val uniqueIds = generateSequence(0) { it + 1 }.iterator()

      data class Person(val name: String, val score: Int) {
        val id: Int = uniqueIds.next()
      }
      val people =
          listOf(
              Person("Jeff Winger", counter.value),
              Person("Annie Edison", 3),
              Person("Britta Perry", 1),
              Person("Abed Nadir", 4))

      val comparator = compareBy<Person> { it.score }.thenBy { it.name }
      val orderedPeople =
          people.sortedWith(if (inOrder.value) comparator else comparator.reversed<Person>())
      return Collection(
          disablePTR = true,
          style = Style.flex(grow = 1f),
          onViewportChangedFunction = { _, _, _, _, _, _ ->
            Log.d("litho-kotlin", "onViewportChangedFunction")
          },
          onDataBound = { Log.d("litho-kotlin", "onDataBound") }) {
        item(
            Row {
              child(Button(text = "Reverse Order", onClick = { inOrder.update { !it } }))
              child(Button(text = "Jeff +1", onClick = { counter.update { (it + 1) % 5 } }))
            })
        items(data = orderedPeople, isSameItem = itemId(Person::id)) {
          Text(text = "${it.name} ${"\uD83D\uDC31".repeat(it.score + 1)}")
        }
      }
    }

    @Suppress("FunctionName")
    inline fun ComponentScope.Button(
        text: String,
        noinline onClick: (ClickEvent) -> Unit,
    ): Text =
        Text.create(context, android.R.attr.buttonStyle, 0)
            .text(text)
            .kotlinStyle(Style.onClick(onClick))
            .build()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent(CollectionComponent())
  }
}
