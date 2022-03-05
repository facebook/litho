// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.samples.litho.kotlin.collection

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

import android.graphics.Typeface
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList

// start_original_data_example
data class Person(val name: String, val id: Int)

val friends =
    listOf(
        Person("Ross Geller", 1),
        Person("Monica Geller", 2),
        Person("Rachel Green", 3),
        Person("Phoebe Buffay", 4),
        Person("Joey Tribbiani", 5),
        Person("Chandler Bing", 6),
    )
// end_original_data_example

// start_modified_data_example
val friends_added_removed_sorted =
    listOf(
        Person("Chandler Bing", 6),
        Person("Janice Hosenstein", 7),
        Person("Joey Tribbiani", 5),
        Person("Monica Geller", 2),
        Person("Phoebe Buffay", 4),
        Person("Rachel Green", 3),
    )
// end_modified_data_example

val friends_monica_name_updated =
    listOf(
        Person("Chandler Bing", 6),
        Person("Janice Hosenstein", 7),
        Person("Joey Tribbiani", 5),
        Person("Monica Geller-Bing", 2),
        Person("Phoebe Buffay", 4),
        Person("Rachel Green", 3),
    )

class FriendsCollectionKComponent : KComponent() {

  enum class Data(val list: List<Person>) {
    ORIGINAL(friends),
    ITEMS_MODIFIED(friends_added_removed_sorted),
    CONTENTS_MODIFIED(friends_monica_name_updated),
  }

  override fun ComponentScope.render(): Component? {
    val data = useState { Data.ORIGINAL }

    return Column {
      child(
          Button("Update Data") {
            data.update { Data.values()[(it.ordinal + 1) % Data.values().size] }
          })
      child(FriendList(data.value.list))
    }
  }
}

// start_example
class FriendList(val friends: List<Person>) : KComponent() {

  override fun ComponentScope.render(): Component = LazyList {
    child(Text(text = "Friends", textStyle = Typeface.BOLD))
    friends.forEach { (name, id) -> child(id = id, component = Text(name)) }
  }
}
// end_example
