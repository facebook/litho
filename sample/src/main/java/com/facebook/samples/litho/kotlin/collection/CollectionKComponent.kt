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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.collection.LazyList

class CollectionKComponent : KComponent() {

  private val friends = listOf("Ross", "Rachel", "Joey", "Phoebe", "Monica", "Chandler")

  override fun ComponentScope.render(): Component =
      LazyList(style = Style.flex(grow = 1f)) {
        child(Text(text = "Header"))
        friends.forEach { child(id = it, component = Text(it)) }
        child(Text(text = "Footer"))
      }
}
