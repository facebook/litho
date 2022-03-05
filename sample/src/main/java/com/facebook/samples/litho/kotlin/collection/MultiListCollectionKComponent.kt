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

import android.graphics.Typeface
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList

class MultiListCollectionKComponent : KComponent() {

  companion object {
    private const val ALL_FRIENDS_TAG = "all_friends_"
    private const val TOP_FRIENDS_TAG = "top_friends_"
  }

  private val friends = listOf("Ross", "Rachel", "Joey", "Phoebe", "Monica", "Chandler")
  private val topFriends = (0..1)
  private val allFriends = (0..5)

  override fun ComponentScope.render(): Component {
    val shouldShowTopFriends = useState { false }

    return Column {
      child(
          Button("Toggle Top Friends") { shouldShowTopFriends.update(!shouldShowTopFriends.value) })
      child(
          LazyList {
            if (shouldShowTopFriends.value) {
              child(
                  id = TOP_FRIENDS_TAG + "title",
                  component = Text("Top Friends", textStyle = Typeface.BOLD))
              topFriends.forEach { child(id = TOP_FRIENDS_TAG + it, component = Text(friends[it])) }
            }

            child(
                id = ALL_FRIENDS_TAG + "title",
                component = Text("All Friends", textStyle = Typeface.BOLD))
            allFriends.forEach { child(id = ALL_FRIENDS_TAG + it, component = Text(friends[it])) }
          })
    }
  }
}
