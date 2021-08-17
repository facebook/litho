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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.useState
import com.facebook.litho.widget.Text
import kotlin.random.Random

class PullToRefreshCollectionKComponent : KComponent() {

  private fun generateRandomNumbers(): List<Int> = List(50) { Random.nextInt(0, 100) }

  override fun ComponentScope.render(): Component? {
    val list = useState { generateRandomNumbers() }
    val idGenerator = useState { generateSequence(0) { it + 1 }.iterator() }
    val handle = Handle()

    return Column(style = Style.padding(16.dp)) {
      child(
          Collection(
              handle = handle,
              style = Style.flex(grow = 1f),
              onPullToRefresh = {
                list.update(generateRandomNumbers())
                Collection.clearRefreshing(context, handle)
              },
          ) { list.value.forEach { child(id = idGenerator.value.next()) { Text("$it") } } })
    }
  }
}
