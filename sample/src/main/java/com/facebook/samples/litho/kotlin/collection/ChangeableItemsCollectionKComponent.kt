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

package com.facebook.samples.litho.kotlin.collection

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList

class ChangeableItemsCollectionKComponent : KComponent() {

  data class Person(val id: Int, val name: String, val score: Int)

  private val comparator = compareBy<Person> { it.score }.thenBy { it.name }

  override fun ComponentScope.render(): Component {
    val counter = useState { 0 }
    val inOrder = useState { true }

    val people =
        listOf(
            Person(1, "Jeff Winger", counter.value),
            Person(2, "Annie Edison", 3),
            Person(3, "Britta Perry", 1),
            Person(4, "Abed Nadir", 4))

    val orderedPeople =
        people.sortedWith(if (inOrder.value) comparator else comparator.reversed<Person>())

    return Column(style = Style.padding(16.dp)) {
      child(
          Row {
            child(Button("Reverse Order") { inOrder.update { !it } })
            child(Button("Jeff +1") { counter.update { (it + 1) % 5 } })
          })
      child(
          LazyList {
            orderedPeople.forEach {
              child(
                  id = it.id, component = Text("${it.name} ${"\uD83D\uDC31".repeat(it.score + 1)}"))
            }
          })
    }
  }
}
